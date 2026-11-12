# LiquidOS Android Launcher

Kotlin + Jetpack Compose launcher project. `minSdk 26`, `targetSdk 34`, `compileSdk 34`, JDK 17, Gradle 8.7, Android Gradle Plugin 8.6.1, Kotlin 2.0.20.

## Build from this project directory

Prerequisites: Android SDK Platform 34, Android Build Tools 34.x, JDK 17, Gradle 8.7 installed on PATH for the one-time wrapper bootstrap, and Android platform-tools (`adb`).

```bash
gradle wrapper --gradle-version 8.7 --distribution-type bin
./gradlew --version
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The first command generates `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`. The wrapper properties file is already included. On Windows, use `gradlew.bat` instead of `./gradlew`.

Expected build artifact:
`app/build/outputs/apk/debug/app-debug.apk`

Expected successful Gradle terminal ending:
`BUILD SUCCESSFUL`

Expected successful ADB install ending:
`Success`

To set LiquidOS as the default launcher, press Home and choose LiquidOS in Android's launcher chooser, or open Android Settings > Apps > Default apps > Home app.

## Included features

- Home-screen launcher activity and installed launchable-app discovery
- Adjustable icon size and grid columns, labels toggle, page dots and multi-page app grid
- App icon launching, long-press app menu and App info shortcut
- Glass-styled widgets, dock and visual Control Center overlay
- App Library with alphabetical app listing, recently-used-style row and app search
- Spotlight-style app search dialog
- Light/dark theme toggle, glass intensity setting, dock and label options
- JSON configuration export via Android share sheet
- Wi-Fi and Bluetooth tiles open Android system settings

## Platform constraints

Android restricts third-party launchers from silently toggling some protected radios and system features. Wi-Fi/Bluetooth shortcuts therefore open system settings. The sliders are interface controls, not privileged system brightness/audio controls. The widget cards are local visual cards, not live weather/calendar/music integrations. This project does not replace Android's lock screen or implement true system-wide refraction, depth wallpaper, or icon-pack protocols. No foreground service is needed for a HOME launcher.
