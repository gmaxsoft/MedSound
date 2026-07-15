# Keep Media3 reflection / service discovery intact.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep MediaSessionService entry point.
-keep class com.medsound.app.media.PlaybackService { *; }

# Play Billing
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.api.** { *; }

# AdMob / GMS
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
