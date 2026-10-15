package com.medsound.app.player

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.medsound.app.MedsoundApplication
import com.medsound.app.data.RelaxationSound
import com.medsound.app.data.SoundCatalog
import com.medsound.app.data.isLocked
import com.medsound.app.data.toMediaItem
import com.medsound.app.media.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

data class PlayerUiState(
    val sounds: List<RelaxationSound> = SoundCatalog.sounds,
    val selectedId: String = SoundCatalog.default().id,
    val isPlaying: Boolean = false,
    val isConnected: Boolean = false,
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
    private var sleepQueuePending = false
    private var queuedSleepMinutes: Int? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId ?: return
            val sound = SoundCatalog.findById(id) ?: return
            _playerState.update { it.copy(selectedId = sound.id) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            controller?.let { syncFromController(it) }
        }
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            applySleepExtras(extras)
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
        val future = MediaController.Builder(context, token)
            .setListener(controllerListener)
            .buildAsync()
        controllerFuture = future

        viewModelScope.launch {
            try {
                val mediaController = future.await()
                controller = mediaController
                mediaController.addListener(playerListener)
                syncFromController(mediaController)
                applySleepExtras(mediaController.sessionExtras)
                _playerState.update { it.copy(isConnected = true) }
                if (sleepQueuePending) {
                    sendSleepTimer(queuedSleepMinutes)
                }
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
            if (_playerState.value.isFadingOut) {
                sendSleepTimer(null)
            }
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

        val item = sound.toMediaItem(getApplication())

        if (_playerState.value.isFadingOut) {
            sendSleepTimer(null)
        }
        c.volume = 1f
        c.setMediaItem(item)
        c.prepare()
        c.play()

        _playerState.update { it.copy(selectedId = sound.id) }
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
        val safeMinutes = minutes?.coerceIn(1, PlaybackService.MAX_SLEEP_MINUTES)
        _playerState.update {
            it.copy(
                sleepTimerMinutes = safeMinutes,
                sleepRemainingMs = safeMinutes?.times(60_000L),
                isFadingOut = false,
            )
        }
        sendSleepTimer(safeMinutes)
    }

    private fun sendSleepTimer(minutes: Int?) {
        val c = controller
        if (c == null) {
            queuedSleepMinutes = minutes
            sleepQueuePending = true
            return
        }
        sleepQueuePending = false
        val args = Bundle().apply {
            putInt(
                PlaybackService.EXTRA_SLEEP_MINUTES,
                minutes ?: PlaybackService.SLEEP_TIMER_OFF,
            )
        }
        viewModelScope.launch {
            runCatching {
                c.sendCustomCommand(
                    SessionCommand(PlaybackService.ACTION_SET_SLEEP_TIMER, Bundle.EMPTY),
                    args,
                ).await()
            }
        }
    }

    private fun applySleepExtras(extras: Bundle) {
        val minutes = extras.getInt(
            PlaybackService.EXTRA_SLEEP_MINUTES,
            PlaybackService.SLEEP_TIMER_OFF,
        )
        val fading = extras.getBoolean(PlaybackService.EXTRA_SLEEP_FADING, false)
        val remaining = if (extras.containsKey(PlaybackService.EXTRA_SLEEP_REMAINING_MS)) {
            extras.getLong(PlaybackService.EXTRA_SLEEP_REMAINING_MS)
        } else {
            null
        }
        _playerState.update {
            it.copy(
                sleepTimerMinutes = minutes.takeIf { value -> value > 0 },
                sleepRemainingMs = if (minutes > 0) remaining else null,
                isFadingOut = fading && minutes > 0,
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
            )
        }
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        super.onCleared()
    }

}
