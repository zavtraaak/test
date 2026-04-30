# Zavtra Keyboard

A modern, privacy-friendly Android keyboard (IME) built with Kotlin + Jetpack Compose. Targets Android 8.0+ (minSdk 26), compiled against SDK 35 (Android 15).

## Features

### Input
- **English (QWERTY)** and **Russian (ЙЦУКЕН)** layouts, switch with the globe key.
- **Symbols** and **More symbols** layers (`?123` / `=\<`).
- **Long-press for accents / extras** — `a → àáâäæãåā`, `e → èéêëēėę`, `е → ё`, etc.
- **Long-press top row for digits** when the number row is hidden.
- **Swipe space bar** to move the cursor by character.
- **Swipe ⌫** to delete the previous word.
- **Auto-capitalize** sentences (respects `TYPE_TEXT_FLAG_CAP_SENTENCES`).
- **Smart punctuation** — double-space adds `". "`.
- **Word suggestions** in the strip, **autocorrect** on space (Levenshtein distance ≤ 2).
- Built-in **dictionary** (~450 EN words, ~270 RU words, frequency-weighted) with optional **on-device learning**.
- **Caps lock** via double-tap or long-press on Shift.

### UI
- **Material You** dynamic colors on Android 12+.
- **8 theme presets:** Default, Neon, Glass, Sunset, Mono, Pastel, Forest, Ocean.
- **Light / Dark / AMOLED / System** modes.
- **12 accent colors** (and the active one applies everywhere).
- **Adjustable** key corner radius, key height, font size, background opacity, optional borders.
- **Optional rows:** number row, arrow / cursor / clipboard row.
- **Emoji picker** with category tabs and persistent recents.
- **Clipboard manager** with pinning, swipe-out, and incognito-aware capture.

### Ergonomics & feedback
- **One-handed mode** (left or right shift).
- **Split keyboard** flag (intended for landscape/tablet — wired in settings).
- **Floating keyboard** flag.
- **Haptic feedback** with 4 strengths (Off, Light, Medium, Strong).
- **Key sounds** with volume slider.
- **Incognito mode** — disables word learning and clipboard history.

### Settings app
- Onboarding shows live status of "Enabled" and "Default" with one-tap deeplinks to system settings.
- Full Compose UI to tweak every option.
- **Export / Import** all settings as JSON.
- **Reset to defaults**.

## Building

Requirements:
- JDK 17
- Android SDK 35 + Build Tools 35.0.0 (auto-installed by Gradle if you accept licenses)
- Internet access for the first run (downloads Gradle distribution + Android dependencies)

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
```

Release build (unsigned):

```bash
./gradlew :app:assembleRelease
```

## Installing

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then on the device:
1. Open **Zavtra Keyboard** from the launcher.
2. Tap **Enable** → toggle "Zavtra Keyboard" in the system list.
3. Tap **Choose** → pick "Zavtra Keyboard" as the default IME.
4. Type anywhere — the strip + 4-row keyboard appears.

## Architecture

```
app/
├── data/
│   ├── AppSettings.kt           Single immutable settings model
│   └── SettingsRepository.kt    DataStore<Preferences> wrapper, JSON import/export
├── ime/
│   ├── KeyboardService.kt       InputMethodService + view orchestration
│   ├── KeyboardView.kt          Custom View, Canvas-rendered, touch + long-press
│   ├── KeyPopup.kt              Floating alt-character popup
│   ├── Layouts.kt               Static layout data (English / Russian / Symbols)
│   ├── Dictionary.kt            Suggestions + Levenshtein autocorrect
│   ├── WordsEn.kt / WordsRu.kt  Built-in word lists with frequencies
│   ├── ThemeMapper.kt           AppSettings → KeyboardPalette
│   ├── SuggestionStripView.kt   3-slot suggestion bar
│   ├── EmojiPanelView.kt        Categorized emoji grid + recents
│   └── ClipboardPanelView.kt    Persistent clipboard with pin/unpin
├── ui/
│   ├── AppRoot.kt               Compose settings UI + onboarding + live preview
│   └── SettingsViewModel.kt     AndroidViewModel wrapping the repository
└── MainActivity.kt              Single Activity, hosts AppRoot
```

The IME never blocks: settings are observed as a `Flow` and applied to the keyboard view live, so toggles in the settings app reflect immediately the next time the keyboard is shown.

## Privacy

- No network code. The app does not declare `INTERNET` permission.
- Word learning, recents, and clipboard history live in the app's private storage and are skipped entirely in **Incognito mode**.
- Everything is on-device.

## Roadmap

- Glide / swipe typing
- Voice input integration
- GIF / sticker picker
- More layouts (QWERTZ, AZERTY, Dvorak, Colemak)
- Theme editor with image backgrounds
- Per-language autocorrect dictionaries from compressed assets
