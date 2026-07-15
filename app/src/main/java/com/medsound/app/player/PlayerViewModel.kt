package com.medsound.app.player

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.medsound.app.MedsoundApplication
import com.medsound.app.data.RelaxationSound
import com.medsound.app.data.SoundCatalog
import com.medsound.app.data.isLocked
import com.medsound.app.data.toMediaItem
import com.medsound.app.media.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val sounds: List<RelaxationSound> = SoundCatalog.sounds,
    val selectedId: String = SoundCatalog.default().id,
    val isPlaying: Boolean = false,
    val isConnected: Boolean = false,
    val title: String = SoundCatalog.default().title,
    val subtitle: String = SoundCatalog.default().subtitle,
    val sleepTimerMinutes: Int? = null,
    val sleepRemainingMs: Long? = null,
    val isFadingOut: Boolean = false,
    val isPremium: Boolean = false,
    val premiumPriceLabel: String? = null,
    val canPurchasePremium: Boolean = false,
    val showPremiumSheet: Boolean = false,
    val billingError: String? = null,
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MedsoundApplication
    private val billing = app.billingManager
    private val interstitialAds = app.interstitialAdManager

    private val _playerState = MutableStateFlow(
        PlayerUiState(
            // Keep playback bits in this flow; premium merges below.
        ),
    )

    val uiState: StateFlow<PlayerUiState> = combine(
        _playerState,
        billing.state,
    ) { player, billingState ->
        player.copy(
            isPremium = billingState.isPremium,
            premiumPriceLabel = billingState.premiumPriceLabel,
            canPurchasePremium = billingState.canPurchase,
            billingError = billingState.lastError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState(),
    )

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var sleepJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId ?: return
            val sound = SoundCatalog.findById(id) ?: return
            _playerState.update {
                it.copy(
                    selectedId = sound.id,
                    title = sound.title,
                    subtitle = sound.subtitle,
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            controller?.let { syncFromController(it) }
        }
    }

    init {
        connect()
        interstitialAds.ensureInitialized()
    }

    fun connect() {
        if (controller != null || controllerFuture != null) return

        val context = getApplication<Application>()
        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java),
        )
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future

        viewModelScope.launch {
            try {
                val mediaController = future.await()
                controller = mediaController
                mediaController.addListener(playerListener)
                syncFromController(mediaController)
                _playerState.update { it.copy(isConnected = true) }
            } catch (_: Exception) {
                MediaController.releaseFuture(future)
                controllerFuture = null
                _playerState.update { it.copy(isConnected = false) }
            }
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying || _playerState.value.isFadingOut) {
            abortFadeIfNeeded()
            c.volume = 1f
            c.pause()
        } else {
            c.volume = 1f
            if (c.playbackState == Player.STATE_IDLE) {
                c.prepare()
            }
            c.play()
        }
    }

    /**
     * Wybór dźwięku: kłódka → arkusz Premium;
     * free → interstitial (jeśli gotowy), potem odtwarzanie;
     * Premium → od razu play.
     */
    fun onSoundClicked(activity: Activity, sound: RelaxationSound) {
        val isPremium = billing.state.value.isPremium
        if (sound.isLocked(isPremium)) {
            _playerState.update { it.copy(showPremiumSheet = true) }
            return
        }

        if (isPremium) {
            selectSound(sound)
            return
        }

        interstitialAds.showIfAvailable(activity) {
            selectSound(sound)
        }
    }

    fun selectSound(sound: RelaxationSound) {
        val c = controller ?: return
        if (sound.isLocked(billing.state.value.isPremium)) return

        val packageName = getApplication<Application>().packageName
        val item = sound.toMediaItem(packageName)

        abortFadeIfNeeded()
        c.volume = 1f
        c.setMediaItem(item)
        c.prepare()
        c.play()

        _playerState.update {
            it.copy(
                selectedId = sound.id,
                title = sound.title,
                subtitle = sound.subtitle,
            )
        }
    }

    fun openPremiumSheet() {
        _playerState.update { it.copy(showPremiumSheet = true) }
    }

    fun dismissPremiumSheet() {
        _playerState.update { it.copy(showPremiumSheet = false) }
        billing.clearError()
    }

    fun purchasePremium(activity: Activity) {
        billing.launchPremiumPurchase(activity)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        sleepJob = null

        if (minutes == null) {
            controller?.volume = 1f
            _playerState.update {
                it.copy(
                    sleepTimerMinutes = null,
                    sleepRemainingMs = null,
                    isFadingOut = false,
                )
            }
            return
        }

        if (_playerState.value.sleepTimerMinutes == minutes && !_playerState.value.isFadingOut) {
            controller?.volume = 1f
            _playerState.update {
                it.copy(
                    sleepTimerMinutes = null,
                    sleepRemainingMs = null,
                    isFadingOut = false,
                )
            }
            return
        }

        val endAt = SystemClock.elapsedRealtime() + minutes * 60_000L
        _playerState.update {
            it.copy(
                sleepTimerMinutes = minutes,
                sleepRemainingMs = minutes * 60_000L,
                isFadingOut = false,
            )
        }
        controller?.volume = 1f

        sleepJob = viewModelScope.launch {
            while (isActive) {
                val remaining = endAt - SystemClock.elapsedRealtime()
                if (remaining <= 0L) break
                _playerState.update { it.copy(sleepRemainingMs = remaining) }
                delay(1_000L)
            }
            if (!isActive) return@launch
            _playerState.update { it.copy(sleepRemainingMs = 0L) }
            fadeOutAndStop()
        }
    }

    private suspend fun fadeOutAndStop() {
        val c = controller
        if (c == null) {
            clearSleepTimerState()
            return
        }

        _playerState.update { it.copy(isFadingOut = true) }

        val startVolume = c.volume.coerceIn(0.05f, 1f)
        for (i in 1..FADE_STEPS) {
            if (!isActive) return
            c.volume = startVolume * (1f - i.toFloat() / FADE_STEPS)
            delay(FADE_STEP_MS)
        }

        if (!isActive) return
        c.pause()
        c.volume = 1f
        clearSleepTimerState()
    }

    private fun abortFadeIfNeeded() {
        if (!_playerState.value.isFadingOut) return
        sleepJob?.cancel()
        sleepJob = null
        controller?.volume = 1f
        clearSleepTimerState()
    }

    private fun clearSleepTimerState() {
        _playerState.update {
            it.copy(
                sleepTimerMinutes = null,
                sleepRemainingMs = null,
                isFadingOut = false,
            )
        }
    }

    private fun syncFromController(c: MediaController) {
        val mediaId = c.currentMediaItem?.mediaId
        val sound = mediaId?.let(SoundCatalog::findById)
        _playerState.update {
            it.copy(
                isPlaying = c.isPlaying,
                selectedId = sound?.id ?: it.selectedId,
                title = sound?.title ?: c.mediaMetadata.title?.toString() ?: it.title,
                subtitle = sound?.subtitle
                    ?: c.mediaMetadata.artist?.toString()
                    ?: it.subtitle,
            )
        }
    }

    override fun onCleared() {
        sleepJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        super.onCleared()
    }

    companion object {
        private const val FADE_STEPS = 30
        private const val FADE_STEP_MS = 400L

        val SLEEP_TIMER_OPTIONS_MINUTES: List<Int> = listOf(15, 30, 45)
    }
}
