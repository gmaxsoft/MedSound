package com.medsound.app.monetization

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.medsound.app.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ładuje i pokazuje interstitial po wyborze dźwięku (tylko dla użytkowników bez Premium).
 */
class InterstitialAdManager(context: Context) {

    private val appContext = context.applicationContext
    private var interstitial: InterstitialAd? = null
    private val isLoading = AtomicBoolean(false)
    private var initialized = false

    fun ensureInitialized(onReady: (() -> Unit)? = null) {
        if (initialized) {
            onReady?.invoke()
            return
        }
        MobileAds.initialize(appContext) {
            initialized = true
            preload()
            onReady?.invoke()
        }
    }

    fun preload() {
        if (isLoading.get() || interstitial != null) return
        isLoading.set(true)

        InterstitialAd.load(
            appContext,
            BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    isLoading.set(false)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    isLoading.set(false)
                }
            },
        )
    }

    /**
     * Pokazuje reklamę jeśli gotowa; zawsze wywołuje [onFinished] (po zamknięciu / błędzie / braku ads).
     */
    fun showIfAvailable(activity: Activity, onFinished: () -> Unit) {
        val ad = interstitial
        if (ad == null) {
            preload()
            onFinished()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                preload()
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                preload()
                onFinished()
            }

            override fun onAdShowedFullScreenContent() {
                interstitial = null
            }
        }

        ad.show(activity)
    }
}
