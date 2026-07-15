package com.medsound.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsound.app.player.PlayerViewModel
import com.medsound.app.ui.MedsoundScreen
import com.medsound.app.ui.theme.MedsoundTheme

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Media controls work best with permission; playback still runs. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()

        setContent {
            MedsoundTheme {
                val state by playerViewModel.uiState.collectAsStateWithLifecycle()
                MedsoundScreen(
                    state = state,
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onSelectSound = { sound ->
                        playerViewModel.onSoundClicked(this@MainActivity, sound)
                    },
                    onSetSleepTimer = playerViewModel::setSleepTimer,
                    onOpenPremium = playerViewModel::openPremiumSheet,
                    onDismissPremium = playerViewModel::dismissPremiumSheet,
                    onPurchasePremium = {
                        playerViewModel.purchasePremium(this@MainActivity)
                    },
                    onRequestNotificationPermission = ::maybeRequestNotificationPermission,
                    notificationsGranted = areNotificationsGranted(),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playerViewModel.connect()
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (areNotificationsGranted()) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun areNotificationsGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
