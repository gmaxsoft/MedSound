package com.medsound.app.media

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.medsound.app.MainActivity
import com.medsound.app.MedsoundApplication
import com.medsound.app.R
import com.medsound.app.data.SoundCatalog
import com.medsound.app.data.toMediaItem
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground [MediaSessionService] that owns the looping [ExoPlayer].
 *
 * - System treats this as `mediaPlayback` FGS → survives screen-off.
 * - Media3 posts a media-style notification with Play/Pause automatically.
 * - The sleep timer runs here, so it keeps counting while the screen is off.
 * - Local asset playback uses [C.WAKE_MODE_NONE] (no Wi‑Fi lock) for battery.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepJob: Job? = null

    private val callback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(ACTION_SET_SLEEP_TIMER, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_SET_SLEEP_TIMER) {
                val minutes = args.getInt(EXTRA_SLEEP_MINUTES, SLEEP_TIMER_OFF)
                if (minutes <= 0) {
                    cancelSleepTimer()
                } else {
                    startSleepTimer(minutes)
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onCreate() {
        super.onCreate()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(MedsoundApplication.PLAYBACK_CHANNEL_ID)
                .setChannelName(R.string.playback_notification_channel)
                .setNotificationId(NOTIFICATION_ID)
                .build()
                .apply {
                    setSmallIcon(R.drawable.ic_notification)
                },
        )

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            // Local files: no network wake lock. CPU stays awake only while decoding.
            .setWakeMode(C.WAKE_MODE_NONE)
            .build()
            .apply {
                // Seamless loop of the current track (white noise / rain / ocean).
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = false
                volume = 1f

                val initial = SoundCatalog.default().toMediaItem(this@PlaybackService)
                setMediaItem(initial)
                prepare()
            }
        player = exoPlayer

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setId(SESSION_ID)
            .setCallback(callback)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val activePlayer = mediaSession?.player
        if (activePlayer == null || !activePlayer.playWhenReady || activePlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        sleepJob?.cancel()
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        val safeMinutes = minutes.coerceIn(1, MAX_SLEEP_MINUTES)
        val endAt = SystemClock.elapsedRealtime() + safeMinutes * 60_000L
        player?.volume = 1f
        publishSleep(safeMinutes, safeMinutes * 60_000L, fading = false)

        sleepJob = scope.launch {
            while (isActive) {
                val remaining = endAt - SystemClock.elapsedRealtime()
                if (remaining <= 0L) break
                publishSleep(safeMinutes, remaining, fading = false)
                delay(1_000L)
            }
            if (!isActive) return@launch
            fadeOutAndStop(safeMinutes)
        }
    }

    private fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        player?.volume = 1f
        publishSleep(minutes = null, remainingMs = null, fading = false)
    }

    private suspend fun fadeOutAndStop(minutes: Int) {
        val activePlayer = player
        if (activePlayer == null) {
            publishSleep(minutes = null, remainingMs = null, fading = false)
            return
        }

        publishSleep(minutes, remainingMs = 0L, fading = true)
        val startVolume = activePlayer.volume.coerceIn(0.05f, 1f)
        for (step in 1..FADE_STEPS) {
            if (!coroutineContext.isActive) return
            activePlayer.volume = startVolume * (1f - step.toFloat() / FADE_STEPS)
            delay(FADE_STEP_MS)
        }
        if (!coroutineContext.isActive) return
        activePlayer.pause()
        activePlayer.volume = 1f
        sleepJob = null
        publishSleep(minutes = null, remainingMs = null, fading = false)
    }

    private fun publishSleep(minutes: Int?, remainingMs: Long?, fading: Boolean) {
        val extras = Bundle().apply {
            putInt(EXTRA_SLEEP_MINUTES, minutes ?: SLEEP_TIMER_OFF)
            if (remainingMs != null) {
                putLong(EXTRA_SLEEP_REMAINING_MS, remainingMs)
            }
            putBoolean(EXTRA_SLEEP_FADING, fading)
        }
        mediaSession?.setSessionExtras(extras)
    }

    companion object {
        const val SESSION_ID = "medsound_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SET_SLEEP_TIMER = "com.medsound.app.SET_SLEEP_TIMER"
        const val EXTRA_SLEEP_MINUTES = "sleep_minutes"
        const val EXTRA_SLEEP_REMAINING_MS = "sleep_remaining_ms"
        const val EXTRA_SLEEP_FADING = "sleep_fading"
        const val SLEEP_TIMER_OFF = -1
        const val MAX_SLEEP_MINUTES = 12 * 60 + 59

        private const val FADE_STEPS = 30
        private const val FADE_STEP_MS = 400L
    }
}
