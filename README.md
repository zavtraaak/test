# NoTube

Ad-free YouTube client for Android, based on [NewPipe](https://github.com/TeamNewPipe/NewPipe).

This is a rebranded fork of NewPipe with:

- Application ID: `com.notube.app`
- App name: `NoTube`
- `minSdk` raised to **26** (Android 8.0+) — targets modern Android only
- `targetSdk` 35, `compileSdk` 36
- Release builds signed with a self-signed keystore (`notube-release.jks`) so you can sideload directly without going through Play

NewPipe pulls videos from YouTube without using the official Google API and without showing ads — all of NewPipe's features (search, playback, background play, subscriptions, downloads, popup mode) are preserved.

## Building

Requires JDK 17 + JDK 21 (for the Checkstyle task) and the Android SDK with platform 36 / build-tools 35+.

```
./gradlew assembleRelease   # signed release APK
./gradlew assembleDebug     # debug APK
```

Output:

- `app/build/outputs/apk/release/app-release.apk` — installable signed APK (~10 MB)
- `app/build/outputs/apk/debug/app-debug.apk` — debug APK (~26 MB)

## Installation

1. Copy the APK to your Android 8+ device.
2. Enable "Install from unknown sources" for your file manager.
3. Open the APK and install.

## License

GPL-3.0 (inherited from NewPipe). See [LICENSE](LICENSE).

## Credits

All credit for the upstream codebase goes to the [NewPipe team](https://github.com/TeamNewPipe). This fork only changes branding, package id and minSdk.
