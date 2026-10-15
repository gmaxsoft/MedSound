package com.medsound.app.data

import android.content.Context
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.medsound.app.R

/**
 * Models a single looping relaxation track.
 *
 * Prefer [Asset] for larger catalogs (files under `src/main/assets/`).
 * Prefer [Raw] for small bundled demos compiled into `res/raw`.
 */
sealed class SoundSource {
    data class Asset(val assetPath: String) : SoundSource()
    data class Raw(@RawRes val resId: Int) : SoundSource()
}

enum class SoundIconKind {
    Rain,
    WhiteNoise,
    Ocean,
    Fan,
    BrownNoise,
    Campfire,
}

data class RelaxationSound(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: SoundIconKind,
    val source: SoundSource,
    /** Ekstra content — wymaga Premium (ikona kłódki). */
    val requiresPremium: Boolean = false,
)

object SoundCatalog {

    val sounds: List<RelaxationSound> = listOf(
        RelaxationSound(
            id = "rain",
            titleRes = R.string.sound_rain_title,
            subtitleRes = R.string.sound_rain_subtitle,
            icon = SoundIconKind.Rain,
            source = SoundSource.Asset("sounds/rain.wav"),
        ),
        RelaxationSound(
            id = "white_noise",
            titleRes = R.string.sound_white_noise_title,
            subtitleRes = R.string.sound_white_noise_subtitle,
            icon = SoundIconKind.WhiteNoise,
            source = SoundSource.Asset("sounds/white_noise.wav"),
        ),
        RelaxationSound(
            id = "ocean",
            titleRes = R.string.sound_ocean_title,
            subtitleRes = R.string.sound_ocean_subtitle,
            icon = SoundIconKind.Ocean,
            source = SoundSource.Asset("sounds/ocean.wav"),
            requiresPremium = true,
        ),
        RelaxationSound(
            id = "fan",
            titleRes = R.string.sound_fan_title,
            subtitleRes = R.string.sound_fan_subtitle,
            icon = SoundIconKind.Fan,
            source = SoundSource.Asset("sounds/fan.wav"),
            requiresPremium = true,
        ),
        RelaxationSound(
            id = "brown_noise",
            titleRes = R.string.sound_brown_noise_title,
            subtitleRes = R.string.sound_brown_noise_subtitle,
            icon = SoundIconKind.BrownNoise,
            source = SoundSource.Asset("sounds/white_noise.wav"),
            requiresPremium = true,
        ),
        RelaxationSound(
            id = "campfire",
            titleRes = R.string.sound_campfire_title,
            subtitleRes = R.string.sound_campfire_subtitle,
            icon = SoundIconKind.Campfire,
            source = SoundSource.Asset("sounds/rain.wav"),
            requiresPremium = true,
        ),
    )

    fun findById(id: String): RelaxationSound? = sounds.find { it.id == id }

    fun default(): RelaxationSound = sounds.first { !it.requiresPremium }
}

fun RelaxationSound.toMediaItem(context: Context): MediaItem {
    val uri = when (val src = source) {
        is SoundSource.Asset -> "asset:///${src.assetPath}"
        is SoundSource.Raw -> "android.resource://${context.packageName}/${src.resId}"
    }

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(context.getString(titleRes))
                .setArtist(context.getString(subtitleRes))
                .setAlbumTitle(context.getString(R.string.app_name))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        )
        .build()
}

fun RelaxationSound.isLocked(isPremium: Boolean): Boolean =
    requiresPremium && !isPremium
