package com.medsound.app.data

import androidx.annotation.RawRes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

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
    val title: String,
    val subtitle: String,
    val icon: SoundIconKind,
    val source: SoundSource,
    /** Ekstra content — wymaga Premium (ikona kłódki). */
    val requiresPremium: Boolean = false,
)

object SoundCatalog {

    val sounds: List<RelaxationSound> = listOf(
        RelaxationSound(
            id = "rain",
            title = "Deszcz",
            subtitle = "Delikatne krople",
            icon = SoundIconKind.Rain,
            source = SoundSource.Asset("sounds/rain.wav"),
        ),
        RelaxationSound(
            id = "white_noise",
            title = "Biały szum",
            subtitle = "Równe spektrum",
            icon = SoundIconKind.WhiteNoise,
            source = SoundSource.Asset("sounds/white_noise.wav"),
        ),
        RelaxationSound(
            id = "ocean",
            title = "Fale oceanu",
            subtitle = "Odległy brzeg",
            icon = SoundIconKind.Ocean,
            source = SoundSource.Asset("sounds/ocean.wav"),
            requiresPremium = true,
        ),
        RelaxationSound(
            id = "fan",
            title = "Dźwięk wentylatora",
            subtitle = "Cichy nadmuch",
            icon = SoundIconKind.Fan,
            source = SoundSource.Asset("sounds/fan.wav"),
            requiresPremium = true,
        ),
        RelaxationSound(
            id = "brown_noise",
            title = "Szum brązowy",
            subtitle = "Głęboki szum · ekstra",
            icon = SoundIconKind.BrownNoise,
            source = SoundSource.Asset("sounds/white_noise.wav"),
            requiresPremium = true,
        ),
        RelaxationSound(
            id = "campfire",
            title = "Ognisko",
            subtitle = "Trzask drewna · ekstra",
            icon = SoundIconKind.Campfire,
            source = SoundSource.Asset("sounds/rain.wav"),
            requiresPremium = true,
        ),
    )

    fun findById(id: String): RelaxationSound? = sounds.find { it.id == id }

    fun default(): RelaxationSound = sounds.first { !it.requiresPremium }
}

fun RelaxationSound.toMediaItem(packageName: String): MediaItem {
    val uri = when (val src = source) {
        is SoundSource.Asset -> "asset:///${src.assetPath}"
        is SoundSource.Raw -> "android.resource://$packageName/${src.resId}"
    }

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(subtitle)
                .setAlbumTitle("Medsound")
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        )
        .build()
}

fun RelaxationSound.isLocked(isPremium: Boolean): Boolean =
    requiresPremium && !isPremium
