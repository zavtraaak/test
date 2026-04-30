# NoTube (from scratch)

Minimal **YouTube-styled** Android client — written from zero, no NewPipe code, no ads.

This branch contains a small Kotlin / Jetpack Compose Android app that talks to public [Piped](https://github.com/TeamPiped/Piped) instances to fetch YouTube data and plays HLS streams via Media3 ExoPlayer.

## Features

- Trending feed on the home tab (YouTube-style cards: thumbnail + duration + channel avatar + title + meta)
- Full-text search
- Full-screen video player (Media3 ExoPlayer, HLS)
- Material 3 theme in YouTube red on white/black backgrounds
- Bottom navigation: Home / Library
- Adaptive launcher icon (red play badge on white)
- No ads, no Google API, no tracking

## Tech

- Kotlin 2.0.21, Jetpack Compose, Material 3
- Retrofit 2.11 + kotlinx.serialization (custom converter)
- Coil 3 for image loading
- Media3 ExoPlayer 1.4.1 (HLS / DASH)
- Multi-instance Piped fallback (5 public instances)

## Build

Requires JDK 17 and Android SDK with platform 35 / build-tools 35.

```
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk` (~14 MB), signed with `notube-release.jks` (password `notube123`).

## Install

Copy the APK to an Android 8+ device, allow "Install from unknown sources", open the APK.

## Caveats

- Depends on the availability of public Piped instances. If all 5 are down, content will not load (extremely rare).
- v1 has no comments, subscriptions, downloads, library or login. Add later if needed.
- Violates YouTube ToS — personal sideload only, not for Play Store.
