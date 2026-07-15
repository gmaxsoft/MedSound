package com.medsound.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medsound.app.R
import com.medsound.app.data.RelaxationSound
import com.medsound.app.data.SoundIconKind
import com.medsound.app.data.isLocked
import com.medsound.app.player.PlayerUiState
import com.medsound.app.player.PlayerViewModel
import com.medsound.app.ui.ads.AdMobBanner
import com.medsound.app.ui.theme.MedsoundColors
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedsoundScreen(
    state: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSelectSound: (RelaxationSound) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onOpenPremium: () -> Unit,
    onDismissPremium: () -> Unit,
    onPurchasePremium: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    notificationsGranted: Boolean,
) {
    LaunchedEffect(state.isPremium, state.showPremiumSheet) {
        if (state.isPremium && state.showPremiumSheet) {
            onDismissPremium()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MedsoundColors.NightDeep,
                        Color(0xFF0D131A),
                        MedsoundColors.NightDeep,
                    ),
                ),
            )
            .statusBarsPadding(),
    ) {
        Header(
            isPremium = state.isPremium,
            onOpenPremium = onOpenPremium,
        )

        if (!notificationsGranted) {
            TextButton(
                onClick = onRequestNotificationPermission,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.grant_notifications),
                    color = MedsoundColors.MistSoft,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            items(state.sounds, key = { it.id }) { sound ->
                val locked = sound.isLocked(state.isPremium)
                SoundRow(
                    sound = sound,
                    selected = sound.id == state.selectedId,
                    locked = locked,
                    onClick = { onSelectSound(sound) },
                )
            }
        }

        SleepTimerSection(
            selectedMinutes = state.sleepTimerMinutes,
            remainingMs = state.sleepRemainingMs,
            isFadingOut = state.isFadingOut,
            onSelect = onSetSleepTimer,
        )

        BottomPlayerBar(
            title = state.title,
            subtitle = state.subtitle,
            icon = state.sounds.find { it.id == state.selectedId }?.icon
                ?: SoundIconKind.Rain,
            isPlaying = state.isPlaying || state.isFadingOut,
            enabled = state.isConnected,
            onTogglePlayPause = onTogglePlayPause,
        )

        if (!state.isPremium) {
            AdMobBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MedsoundColors.NightMid)
                    .navigationBarsPadding()
                    .padding(vertical = 4.dp),
            )
        } else {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }

    if (state.showPremiumSheet && !state.isPremium) {
        PremiumSheet(
            priceLabel = state.premiumPriceLabel,
            canPurchase = state.canPurchasePremium,
            error = state.billingError,
            onDismiss = onDismissPremium,
            onPurchase = onPurchasePremium,
        )
    }
}

@Composable
private fun Header(
    isPremium: Boolean,
    onOpenPremium: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = MedsoundColors.Foam,
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MedsoundColors.FoamDim,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (isPremium) {
            Text(
                text = stringResource(R.string.premium_active),
                style = MaterialTheme.typography.labelMedium,
                color = MedsoundColors.Mist,
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MedsoundColors.SeaAccent.copy(alpha = 0.45f))
                    .clickable(onClick = onOpenPremium)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = MedsoundColors.Foam,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.premium),
                        style = MaterialTheme.typography.labelLarge,
                        color = MedsoundColors.Foam,
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundRow(
    sound: RelaxationSound,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected && !locked) {
            MedsoundColors.NightElevated
        } else {
            MedsoundColors.NightMid.copy(alpha = 0.55f)
        },
        animationSpec = tween(220),
        label = "soundBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected && !locked) {
            MedsoundColors.Mist.copy(alpha = 0.45f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(220),
        label = "soundBorder",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoundIconBadge(kind = sound.icon, selected = selected && !locked, dimmed = locked)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sound.title,
                style = MaterialTheme.typography.titleLarge,
                color = if (locked) {
                    MedsoundColors.Foam.copy(alpha = 0.55f)
                } else if (selected) {
                    MedsoundColors.Foam
                } else {
                    MedsoundColors.Foam.copy(alpha = 0.92f)
                },
            )
            Text(
                text = if (locked) {
                    stringResource(R.string.premium_locked)
                } else {
                    sound.subtitle
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MedsoundColors.FoamDim,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (locked) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = stringResource(R.string.locked_content_cd),
                tint = MedsoundColors.MistSoft.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SoundIconBadge(
    kind: SoundIconKind,
    selected: Boolean,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val iconTint = when {
        dimmed -> MedsoundColors.MistSoft.copy(alpha = 0.55f)
        selected -> MedsoundColors.NightDeep
        else -> MedsoundColors.MistSoft
    }
    val badgeBg = when {
        dimmed -> MedsoundColors.NightElevated.copy(alpha = 0.7f)
        selected -> MedsoundColors.Mist
        else -> MedsoundColors.NightElevated
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(badgeBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = kind.toImageVector(),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SleepTimerSection(
    selectedMinutes: Int?,
    remainingMs: Long?,
    isFadingOut: Boolean,
    onSelect: (Int?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = MedsoundColors.MistSoft,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.sleep_timer),
                style = MaterialTheme.typography.titleMedium,
                color = MedsoundColors.FoamDim,
            )
            Spacer(modifier = Modifier.weight(1f))
            when {
                isFadingOut -> Text(
                    text = stringResource(R.string.sleep_fading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MedsoundColors.Mist,
                )
                remainingMs != null -> Text(
                    text = formatRemaining(remainingMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MedsoundColors.Mist,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TimerChip(
                label = stringResource(R.string.sleep_off),
                selected = selectedMinutes == null,
                onClick = { onSelect(null) },
                modifier = Modifier.weight(1f),
            )
            PlayerViewModel.SLEEP_TIMER_OPTIONS_MINUTES.forEach { minutes ->
                TimerChip(
                    label = stringResource(R.string.sleep_minutes, minutes),
                    selected = selectedMinutes == minutes,
                    onClick = { onSelect(minutes) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TimerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MedsoundColors.SeaAccent.copy(alpha = 0.55f) else MedsoundColors.NightMid,
        animationSpec = tween(200),
        label = "chipBg",
    )
    val fg = if (selected) MedsoundColors.Foam else MedsoundColors.FoamDim

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) MedsoundColors.Mist.copy(alpha = 0.35f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
        )
    }
}

@Composable
private fun BottomPlayerBar(
    title: String,
    subtitle: String,
    icon: SoundIconKind,
    isPlaying: Boolean,
    enabled: Boolean,
    onTogglePlayPause: () -> Unit,
) {
    HorizontalDivider(
        color = MedsoundColors.NightElevated,
        thickness = 1.dp,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MedsoundColors.NightMid.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoundIconBadge(kind = icon, selected = isPlaying)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MedsoundColors.Foam,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isPlaying) {
                    stringResource(R.string.now_playing)
                } else {
                    subtitle
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MedsoundColors.FoamDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MedsoundColors.Mist.copy(alpha = if (enabled) 0.9f else 0.35f))
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTogglePlayPause,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) R.string.pause else R.string.play,
                ),
                tint = MedsoundColors.NightDeep,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumSheet(
    priceLabel: String?,
    canPurchase: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MedsoundColors.NightMid,
        contentColor = MedsoundColors.Foam,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = MedsoundColors.Mist,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.premium_title),
                style = MaterialTheme.typography.titleLarge,
                color = MedsoundColors.Foam,
            )
            Text(
                text = stringResource(R.string.premium_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MedsoundColors.FoamDim,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "• ${stringResource(R.string.premium_benefits_ads)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MedsoundColors.MistSoft,
            )
            Text(
                text = "• ${stringResource(R.string.premium_benefits_extra)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MedsoundColors.MistSoft,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD08080),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onPurchase,
                enabled = canPurchase || priceLabel == null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MedsoundColors.Mist,
                    contentColor = MedsoundColors.NightDeep,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = if (priceLabel != null) {
                        stringResource(R.string.premium_buy_priced, priceLabel)
                    } else {
                        stringResource(R.string.premium_buy)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.premium_close),
                    color = MedsoundColors.FoamDim,
                )
            }
        }
    }
}

private fun SoundIconKind.toImageVector(): ImageVector = when (this) {
    SoundIconKind.Rain -> Icons.Rounded.WaterDrop
    SoundIconKind.WhiteNoise -> Icons.Rounded.GraphicEq
    SoundIconKind.Ocean -> Icons.Rounded.Waves
    SoundIconKind.Fan -> Icons.Rounded.Air
    SoundIconKind.BrownNoise -> Icons.Rounded.GraphicEq
    SoundIconKind.Campfire -> Icons.Rounded.LocalFireDepartment
}

private fun formatRemaining(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
