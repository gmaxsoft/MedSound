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
 * Audio files are seamless loops under the project `audio/` folder, packaged as assets.
 * Display names come from the file name with the `loop` suffix removed.
 */
sealed class SoundSource {
    data class Asset(val assetPath: String) : SoundSource()
    data class Raw(@RawRes val resId: Int) : SoundSource()
}

enum class SoundCategory(
    @StringRes val titleRes: Int,
    @StringRes val detailRes: Int? = null,
) {
    Weather(R.string.category_weather),
    Nature(R.string.category_nature),
    Industrial(R.string.category_industrial),
    Space(R.string.category_space, R.string.category_space_detail),
    Plane(R.string.category_plane),
}

enum class SoundIconKind {
    Snow,
    Storm,
    Rain,
    Waves,
    Water,
    Engine,
    Fan,
    Factory,
    Dryer,
    Ship,
    Train,
    Vacuum,
    Washer,
    Plane,
    Planet,
}

data class RelaxationSound(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: SoundIconKind,
    val category: SoundCategory,
    val source: SoundSource,
    /** Ekstra content — wymaga Premium (ikona kłódki). */
    val requiresPremium: Boolean = false,
)

object SoundCatalog {

    val sounds: List<RelaxationSound> = listOf(
        track(
            id = "blizzard-snow",
            file = "weather/blizzard-snow-loop.ogg",
            category = SoundCategory.Weather,
            titleRes = R.string.sound_blizzard_snow_title,
            icon = SoundIconKind.Snow,
        ),
        track(
            id = "thunderstorm",
            file = "weather/burza-z-deszczem-loop.ogg",
            category = SoundCategory.Weather,
            titleRes = R.string.sound_thunderstorm_title,
            icon = SoundIconKind.Storm,
        ),
        track(
            id = "heavy-rain",
            file = "weather/heavy-rain-loop.ogg",
            category = SoundCategory.Weather,
            titleRes = R.string.sound_heavy_rain_title,
            icon = SoundIconKind.Rain,
        ),
        track(
            id = "beach-with-waves",
            file = "nature/beach-with-waves-loop.wav",
            category = SoundCategory.Nature,
            titleRes = R.string.sound_beach_waves_title,
            icon = SoundIconKind.Waves,
        ),
        track(
            id = "small-cascade",
            file = "nature/small-cascade-loop.ogg",
            category = SoundCategory.Nature,
            titleRes = R.string.sound_small_cascade_title,
            icon = SoundIconKind.Water,
        ),
        track(
            id = "small-waterfall",
            file = "nature/small-waterfall-loop.ogg",
            category = SoundCategory.Nature,
            titleRes = R.string.sound_small_waterfall_title,
            icon = SoundIconKind.Water,
        ),
        track(
            id = "engine-room",
            file = "industrial/engine-room-loop.ogg",
            category = SoundCategory.Industrial,
            titleRes = R.string.sound_engine_room_title,
            icon = SoundIconKind.Engine,
        ),
        track(
            id = "factory-fan",
            file = "industrial/factory-fan-loop.ogg",
            category = SoundCategory.Industrial,
            titleRes = R.string.sound_factory_fan_title,
            icon = SoundIconKind.Fan,
        ),
        track(
            id = "factory-sounds",
            file = "industrial/factory-sounds-loop.ogg",
            category = SoundCategory.Industrial,
            titleRes = R.string.sound_factory_sounds_title,
            icon = SoundIconKind.Factory,
        ),
        track(
            id = "fan-noise",
            file = "industrial/fan-noise-loop.ogg",
            category = SoundCategory.Industrial,
            titleRes = R.string.sound_fan_noise_title,
            icon = SoundIconKind.Fan,
        ),
        track(
            id = "hair-dryer",
            file = "industrial/hair-dryer-loop.ogg",
            category = SoundCategory.Industrial,
            titleRes = R.string.sound_hair_dryer_title,
            icon = SoundIconKind.Dryer,
        ),
        track(
            id = "ship-engine",
            file = "industrial/ship-engine-loop.ogg",
            category = SoundCategory.Industrial,
            titleRes = R.string.sound_ship_engine_title,
            icon = SoundIconKind.Ship,
        ),
        track(
            id = "train-blizzard",
            file = "industrial/train-sounds-and-blizzard-howling-loop.ogg",
            category = SoundCategory.Industrial,
            titleRes = R.string.sound_train_blizzard_title,
            icon = SoundIconKind.Train,
        ),
        track(
            id = "vacuum-cleaner",
            file = "industrial/vacuum-cleaner-loop.ogg",
            category = SoundCategory.Industrial,
            titleRes = R.string.sound_vacuum_cleaner_title,
            icon = SoundIconKind.Vacuum,
        ),
        track(
            id = "vacuum-cleaner-2",
            file = "industrial/vacuum-cleaner-loop-2.ogg",
            category = SoundCategory.Industrial,
            titleRes = R.string.sound_vacuum_cleaner_2_title,
            icon = SoundIconKind.Vacuum,
        ),
        track(
            id = "washing-machine",
            file = "industrial/washing-machine-loop.ogg",
            category = SoundCategory.Industrial,
            titleRes = R.string.sound_washing_machine_title,
            icon = SoundIconKind.Washer,
        ),
        track(
            id = "jupiter",
            file = "space/jupiter-loop-created-with-tunepocket-loop-maker.wav",
            category = SoundCategory.Space,
            titleRes = R.string.sound_jupiter_title,
            icon = SoundIconKind.Planet,
        ),
        track(
            id = "mars",
            file = "space/mars-loop-created-with-tunepocket-loop-maker.wav",
            category = SoundCategory.Space,
            titleRes = R.string.sound_mars_title,
            icon = SoundIconKind.Planet,
        ),
        track(
            id = "mercury",
            file = "space/mercury-loop-created-with-tunepocket-loop-maker.wav",
            category = SoundCategory.Space,
            titleRes = R.string.sound_mercury_title,
            icon = SoundIconKind.Planet,
        ),
        track(
            id = "neptune",
            file = "space/neptune-loop-created-with-tunepocket-loop-maker.wav",
            category = SoundCategory.Space,
            titleRes = R.string.sound_neptune_title,
            icon = SoundIconKind.Planet,
        ),
        track(
            id = "uranus",
            file = "space/uranus-loop-created-with-tunepocket-loop-maker.wav",
            category = SoundCategory.Space,
            titleRes = R.string.sound_uranus_title,
            icon = SoundIconKind.Planet,
        ),
        track(
            id = "venus",
            file = "space/venus-loop.ogg",
            category = SoundCategory.Space,
            titleRes = R.string.sound_venus_title,
            icon = SoundIconKind.Planet,
        ),
        track(
            id = "airbus",
            file = "plane/aribus-szum-samolotu-loop.ogg",
            category = SoundCategory.Plane,
            titleRes = R.string.sound_airbus_title,
            icon = SoundIconKind.Plane,
            requiresPremium = true,
        ),
        track(
            id = "b-17-bomber",
            file = "plane/b-17-bomber-loop.ogg",
            category = SoundCategory.Plane,
            titleRes = R.string.sound_b17_title,
            icon = SoundIconKind.Plane,
            requiresPremium = true,
        ),
        track(
            id = "b-29-superfortress",
            file = "plane/b-29-superfortress-loop.ogg",
            category = SoundCategory.Plane,
            titleRes = R.string.sound_b29_title,
            icon = SoundIconKind.Plane,
            requiresPremium = true,
        ),
    )

    fun findById(id: String): RelaxationSound? = sounds.find { it.id == id }

    fun default(): RelaxationSound = sounds.first { !it.requiresPremium }

    private fun track(
        id: String,
        file: String,
        category: SoundCategory,
        titleRes: Int,
        icon: SoundIconKind,
        requiresPremium: Boolean = false,
    ) = RelaxationSound(
        id = id,
        titleRes = titleRes,
        subtitleRes = category.titleRes,
        icon = icon,
        category = category,
        source = SoundSource.Asset(file),
        requiresPremium = requiresPremium || category == SoundCategory.Plane,
    )
}

fun RelaxationSound.toMediaItem(context: Context, clipEndMs: Long? = null): MediaItem {
    val uri = when (val src = source) {
        is SoundSource.Asset -> "asset:///${src.assetPath}"
        is SoundSource.Raw -> "android.resource://${context.packageName}/${src.resId}"
    }

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .apply {
            if (clipEndMs != null) {
                setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setEndPositionMs(clipEndMs)
                        .build(),
                )
            }
        }
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
