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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.Dry
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Water
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medsound.app.R
import com.medsound.app.data.RelaxationSound
import com.medsound.app.data.SoundCategory
import com.medsound.app.data.SoundIconKind
import com.medsound.app.data.isLocked
import com.medsound.app.player.PlayerUiState
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
            state.sounds.groupBy { it.category }.forEach { (category, sounds) ->
                item(key = "category-${category.name}") {
                    CategoryHeader(category)
                }
                items(sounds, key = { it.id }) { sound ->
                    val locked = sound.isLocked(state.isPremium)
                    SoundRow(
                        sound = sound,
                        selected = sound.id == state.selectedId,
                        locked = locked,
                        onClick = { onSelectSound(sound) },
                    )
                }
            }
        }

        SleepTimerSection(
            selectedMinutes = state.sleepTimerMinutes,
            remainingMs = state.sleepRemainingMs,
            isFadingOut = state.isFadingOut,
            onSelect = onSetSleepTimer,
        )

        val selectedSound = state.sounds.find { it.id == state.selectedId }
            ?: state.sounds.first()
        BottomPlayerBar(
            title = stringResource(selectedSound.titleRes),
            subtitle = stringResource(selectedSound.subtitleRes),
            icon = selectedSound.icon,
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
    val chipColor = if (isPremium) {
        MedsoundColors.PremiumActive
    } else {
        MedsoundColors.SeaAccent.copy(alpha = 0.45f)
    }
    val chipContent = if (isPremium) MedsoundColors.NightDeep else MedsoundColors.Foam
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandLockup(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(chipColor)
                    .clickable(enabled = !isPremium, onClick = onOpenPremium)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = chipContent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.premium),
                        style = MaterialTheme.typography.labelLarge,
                        color = chipContent,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MedsoundColors.FoamDim,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun BrandLockup(modifier: Modifier = Modifier) {
    val name = stringResource(R.string.app_name)
    Row(
        modifier = modifier.semantics { contentDescription = name },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_brand_pulse),
            contentDescription = null,
            tint = MedsoundColors.Foam,
            modifier = Modifier.size(width = 78.dp, height = 32.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)) {
                    append("MED")
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Light)) {
                    append("Sound")
                }
            },
            color = MedsoundColors.Foam,
            fontFamily = FontFamily.SansSerif,
            fontSize = 26.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
        SoundIconBadge(kind = sound.icon, selected = selected, dimmed = locked && !selected)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(sound.titleRes),
                style = MaterialTheme.typography.titleLarge,
                color = if (locked && !selected) {
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
                    stringResource(sound.subtitleRes)
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
    var showPicker by remember { mutableIntStateOf(0) }
    val pickerOpen = showPicker > 0

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
                selected = selectedMinutes == null && !isFadingOut,
                onClick = { onSelect(null) },
                modifier = Modifier.width(96.dp),
            )
            val durationLabel = if (selectedMinutes != null) {
                formatDuration(selectedMinutes)
            } else {
                stringResource(R.string.sleep_timer_pick)
            }
            TimerChip(
                label = durationLabel,
                selected = selectedMinutes != null || isFadingOut,
                onClick = { showPicker++ },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (pickerOpen) {
        SleepDurationSheet(
            initialMinutes = selectedMinutes ?: 60,
            onDismiss = { showPicker = 0 },
            onConfirm = { totalMinutes ->
                showPicker = 0
                onSelect(totalMinutes)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepDurationSheet(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var hours by remember { mutableIntStateOf((initialMinutes / 60).coerceIn(0, 12)) }
    var minutes by remember { mutableIntStateOf((initialMinutes % 60).coerceIn(0, 59)) }
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
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.sleep_timer),
                style = MaterialTheme.typography.titleLarge,
                color = MedsoundColors.Foam,
            )
            Text(
                text = formatDuration(hours * 60 + minutes),
                style = MaterialTheme.typography.displayLarge,
                color = MedsoundColors.Foam,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.sleep_timer_hours),
                style = MaterialTheme.typography.labelLarge,
                color = MedsoundColors.FoamDim,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0..12).toList()) { hour ->
                    TimerChip(
                        label = "%02d".format(Locale.getDefault(), hour),
                        selected = hour == hours,
                        onClick = { hours = hour },
                        contentDescription = stringResource(R.string.sleep_hour_cd, hour),
                        modifier = Modifier.width(56.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.sleep_timer_minutes_label),
                style = MaterialTheme.typography.labelLarge,
                color = MedsoundColors.FoamDim,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp),
            ) {
                gridItems((0..59).toList()) { minute ->
                    TimerChip(
                        label = "%02d".format(Locale.getDefault(), minute),
                        selected = minute == minutes,
                        onClick = { minutes = minute },
                        contentDescription = stringResource(R.string.sleep_minute_cd, minute),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onConfirm(hours * 60 + minutes) },
                enabled = hours > 0 || minutes > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MedsoundColors.Mist,
                    contentColor = MedsoundColors.NightDeep,
                    disabledContainerColor = MedsoundColors.NightElevated,
                    disabledContentColor = MedsoundColors.FoamDim,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.sleep_timer_start),
                    style = MaterialTheme.typography.labelLarge,
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
    contentDescription: String? = null,
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
            .clickable(onClick = onClick)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
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

@Composable
private fun CategoryHeader(category: SoundCategory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp, start = 4.dp),
    ) {
        Text(
            text = stringResource(category.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MedsoundColors.Mist,
        )
        category.detailRes?.let { detail ->
            Text(
                text = stringResource(detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MedsoundColors.FoamDim,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun SoundIconKind.toImageVector(): ImageVector = when (this) {
    SoundIconKind.Snow -> Icons.Rounded.AcUnit
    SoundIconKind.Storm -> Icons.Rounded.Thunderstorm
    SoundIconKind.Rain -> Icons.Rounded.WaterDrop
    SoundIconKind.Waves -> Icons.Rounded.Waves
    SoundIconKind.Water -> Icons.Rounded.Water
    SoundIconKind.Engine -> Icons.Rounded.PrecisionManufacturing
    SoundIconKind.Fan -> Icons.Rounded.Air
    SoundIconKind.Factory -> Icons.Rounded.Factory
    SoundIconKind.Dryer -> Icons.Rounded.Dry
    SoundIconKind.Ship -> Icons.Rounded.DirectionsBoat
    SoundIconKind.Train -> Icons.Rounded.Train
    SoundIconKind.Vacuum -> Icons.Rounded.CleaningServices
    SoundIconKind.Washer -> Icons.Rounded.LocalLaundryService
    SoundIconKind.Plane -> Icons.Rounded.Flight
    SoundIconKind.Planet -> Icons.Rounded.Public
}

private fun formatRemaining(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

private fun formatDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
}
