# Medsound

Nowoczesna aplikacja Android (Kotlin + Jetpack Compose) do odtwarzania dźwięków relaksacyjnych w pętli — z pełnym wsparciem tła przez **Media3 ExoPlayer** i **MediaSessionService**.

## Co dostajesz

- Odtwarzanie w pętli (`Player.REPEAT_MODE_ONE`)
- Praca przy wyłączonym ekranie jako foreground service typu `mediaPlayback`
- Media Notification z Play/Pause (Media3)
- Katalog lokalnych plików z `assets/` (oraz gotowa ścieżka pod `res/raw`)
- UI Compose z wyborem dźwięku i kontrolą odtwarzania

## Architektura odtwarzania

```
UI (Compose) ──MediaController──► MediaSessionService (PlaybackService)
                                         │
                                         └── ExoPlayer (loop, audio focus, noisy)
```

| Element | Rola |
|---------|------|
| `PlaybackService` | `MediaSessionService` — właściciel playera, FGS + powiadomienie |
| `PlayerViewModel` | łączy UI z `MediaController` |
| `SoundCatalog` | lista ścieżek (`Asset` / `Raw`) → `MediaItem` |

### Bateria

- `WAKE_MODE_NONE` — brak Wi‑Fi lock (pliki lokalne)
- kanał powiadomień `IMPORTANCE_LOW` bez dźwięku/wibracji
- audio focus + `setHandleAudioBecomingNoisy(true)`
- ExoPlayer dekoduje tylko lokalne assety (bez sieci)

## Dodawanie własnych dźwięków

### Assets (zalecane)

1. Wrzuć plik do `app/src/main/assets/sounds/` (np. `brown_noise.ogg`)
2. Dopisz wpis w `SoundCatalog`:

```kotlin
RelaxationSound(
    id = "brown_noise",
    titleRes = R.string.sound_brown_noise_title,
    subtitleRes = R.string.sound_brown_noise_subtitle,
    source = SoundSource.Asset("sounds/brown_noise.ogg"),
)
```

Dopisz te same klucze w `values/strings.xml` (angielski, język domyślny) i w `values-pl/strings.xml`. Aplikacja bierze język z ustawień telefonu.

### Raw resources

1. Plik do `app/src/main/res/raw/` (nazwa: małe litery, bez myślników), np. `brown_noise.ogg`
2. Wpis:

```kotlin
source = SoundSource.Raw(R.raw.brown_noise)
```

Demo WAV-y (`white_noise`, `rain`, `ocean`) są już wygenerowane procedurally w `assets/sounds/` — zamień je na własne nagrania produkcyjne, gdy będziesz gotów.

## Uruchomienie

1. Otwórz folder projektu w **Android Studio** (Ladybug / nowszy).
2. Poczekaj na sync Gradle (wrapper pobierze się automatycznie, jeśli brakuje JARa).
3. Uruchom na urządzeniu/emulatorze (API 26+).
4. Na Android 13+ zaakceptuj uprawnienie powiadomień — dzięki temu widać Play/Pause przy zgaszonym ekranie.

### CLI (gdy masz JDK 17+)

```bash
./gradlew :app:assembleDebug
```

## Struktura

```
app/src/main/java/com/medsound/app/
  MainActivity.kt
  MedsoundApplication.kt
  data/SoundCatalog.kt
  media/PlaybackService.kt
  player/PlayerViewModel.kt
  ui/MedsoundScreen.kt
  ui/theme/Theme.kt
app/src/main/assets/sounds/   ← lokalne audio
app/src/main/res/raw/         ← opcjonalnie short clips
```
