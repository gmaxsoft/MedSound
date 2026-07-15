package com.medsound.app.media

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.medsound.app.MainActivity
import com.medsound.app.MedsoundApplication
import com.medsound.app.R
import com.medsound.app.data.SoundCatalog
import com.medsound.app.data.toMediaItem

/**
 * Foreground [MediaSessionService] that owns the looping [ExoPlayer].
 *
 * - System treats this as `mediaPlayback` FGS → survives screen-off.
 * - Media3 posts a media-style notification with Play/Pause automatically.
 * - Local asset playback uses [C.WAKE_MODE_NONE] (no Wi‑Fi lock) for battery.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

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

        val player = ExoPlayer.Builder(this)
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

                val initial = SoundCatalog.default().toMediaItem(packageName)
                setMediaItem(initial)
                prepare()
            }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setId(SESSION_ID)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val SESSION_ID = "medsound_playback"
        const val NOTIFICATION_ID = 1001
    }
}
