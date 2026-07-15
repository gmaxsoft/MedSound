package com.medsound.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.medsound.app.monetization.BillingManager
import com.medsound.app.monetization.InterstitialAdManager
import com.medsound.app.monetization.PremiumPreferences

class MedsoundApplication : Application() {

    lateinit var premiumPreferences: PremiumPreferences
        private set

    lateinit var billingManager: BillingManager
        private set

    lateinit var interstitialAdManager: InterstitialAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        ensurePlaybackNotificationChannel()

        premiumPreferences = PremiumPreferences(this)
        billingManager = BillingManager(this, premiumPreferences)
        interstitialAdManager = InterstitialAdManager(this)
        interstitialAdManager.ensureInitialized()
    }

    private fun ensurePlaybackNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            PLAYBACK_CHANNEL_ID,
            getString(R.string.playback_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.playback_notification_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "medsound_playback"
    }
}
