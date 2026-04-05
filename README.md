# HaramVeil

HaramVeil is a privacy-first Android app for Muslims who want help guarding the eyes on-device. It watches only the apps you choose, runs detection locally, and responds by covering the screen, sending you home, and placing the offending app into a temporary lockdown.

This project is GPL-3.0 licensed, written in Kotlin, and designed around a zero-cloud default. Internet access is only used for the one-time optional FOSS OCR model download path. After that setup step, HaramVeil does not need network access for protection.

## What HaramVeil Does

HaramVeil combines three local detection layers:

1. Mode 1 scans the Accessibility UI tree for risky packages, visible text, content descriptions, and blocked keywords.
2. Mode 2 captures HaramClip screen regions and runs OCR using either Google ML Kit or a FOSS ONNX OCR model.
3. Mode 3 captures the same HaramClip regions and runs NudeNet ONNX inference for explicit visual content.

When risky content is confirmed, HaramVeil shows the Veil overlay, sends the user to the launcher, and starts an app lockdown timer that survives reboot.

## Features

- Three-mode on-device detection pipeline with cascading triggers
- Native Veil overlay with bundled ayah and hadith reminders
- Encrypted local PIN gate, recovery questions, and brute-force lockout
- Encrypted local stats database tied to the user PIN
- Device Admin, boot recovery, sticky foreground service, and self-healing checks
- Optional root enhancements on rooted devices
- Local-only app history, keyword blocklist, and lockdown state
- FOSS-first architecture with an optional proprietary ML Kit path

## Build Flavors

| Flavor | ML Kit | Internet | For |
|--------|--------|----------|-----|
| foss | No | Never | F-Droid |
| full | Yes (optional) | Debug only | GitHub Releases |

Build commands:

```bash
./gradlew assembleFossRelease   # F-Droid APK
./gradlew assembleFullRelease   # GitHub APK
```

## Screenshots

Screenshots will be added here for:

- Onboarding
- Dashboard
- Stats
- Settings
- Advanced Settings
- Veil overlay

## Installation

### APK

1. Build or download the unsigned release APK.
2. Install on Android 10 or newer.
3. Complete onboarding.
4. Grant Accessibility, overlay, and Device Admin permissions.

### F-Droid

F-Droid metadata is included in `fastlane/metadata/android/en-US/`.
Use the `foss` flavor for F-Droid submission and reproducible GPL-only builds.

## Permissions Explained

- `SYSTEM_ALERT_WINDOW`
  Needed to place the Veil on top of the offending app before the app is hidden.
- `FOREGROUND_SERVICE`
  Keeps protection alive while HaramVeil is monitoring selected apps.
- `FOREGROUND_SERVICE_SPECIAL_USE`
  Declares the long-running protection and veil runtime on newer Android versions.
- `RECEIVE_BOOT_COMPLETED`
  Restarts protection after reboot.
- `POST_NOTIFICATIONS`
  Shows service health, Device Admin, and setup reminders.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
  Helps the user exempt HaramVeil from aggressive OEM battery killing.
- `REQUEST_INSTALL_PACKAGES`
  Reserved from earlier setup work. Model download itself uses private app storage and should be revisited before public release.
- `USE_BIOMETRIC`
  Reserved for future security options.
- `INTERNET` in `fullDebug` only
  Reserved for local validation of the optional model-setup path and excluded from `foss` builds.

## ONNX Model Sources

- NudeNet v3.4 weights:
  `https://github.com/notAI-tech/NudeNet/releases/tag/v3.4-weights`
- Bundled assets currently used by the app:
  `app/src/main/assets/320n.onnx`
  `app/src/main/assets/640m.onnx`
- FOSS OCR model source:
  ModelScope RapidOCR `latin_PP-OCRv5_rec_mobile_infer.onnx`

## Privacy Policy

All detection, screenshots, OCR, ONNX inference, logs, PIN checks, and lockdown timers stay on the device. HaramVeil does not upload browsing data, screenshots, OCR text, or block history to any server. The only network use in the project is the one-time optional download of the FOSS OCR model during setup; after that, protection runs locally.

## Battery Usage Guide

Expected battery impact on the Samsung Galaxy A13 / Exynos 850 target:

- Mode 1 only:
  designed to stay near idle cost with a 500 ms debounce and package filtering; expected to remain under roughly 2% battery per idle hour.
- Mode 1 + Mode 2:
  moderate impact because OCR only runs after a Mode 1 wake event and only on HaramClip regions.
- Mode 1 + Mode 2 + Mode 3:
  highest cost; use the 320 model for weaker devices and keep the inference interval at 1000-2000 ms.

The app also includes a thermal guard for Mode 3. If three consecutive visual inferences exceed 1500 ms, HaramVeil automatically slows visual scans to 2000 ms.

## Known Limitations

- Android 10 is the minimum supported version.
- Mode 2 screenshot capture requires Android 11 or newer because `takeScreenshot()` is an API 30+ accessibility capability.
- The `full` flavor includes the optional proprietary ML Kit path; F-Droid should use the `foss` flavor.
- The stats store is SQLCipher-backed rather than Room-backed in this branch due AGP 9 / Windows verifier issues encountered during implementation.
- There is no iOS version.

## Accessibility Notes

- Icon-only controls now include content descriptions where appropriate.
- The PIN pad intentionally avoids exposing spoken digit labels to accessibility services. The visual digits still render, but the semantic labels are generic so TalkBack does not read the entered numbers aloud. This is a deliberate privacy tradeoff for the security screen.

## Build

Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat assembleFossRelease
.\gradlew.bat assembleFullRelease
```

Linux/macOS:

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew assembleFossRelease
./gradlew assembleFullRelease
```

## Reproducibility Notes

For F-Droid-style reproducible builds:

- Build with the checked-in Gradle wrapper.
- Use JDK 17.
- Use Android SDK platform 36 and matching build-tools.
- Keep the release build unsigned in CI.
- Submit the `foss` flavor to F-Droid and keep the `full` flavor for GitHub releases.

GitHub Actions in `.github/workflows/build.yml` builds the unsigned release APK and runs unit tests on pushes to `main`.

## Contributing

This started as a solo project, but pull requests are welcome. Please keep changes readable, self-documenting, and aligned with the privacy-first and Islamic-purpose goals of the app. If you introduce a non-FOSS dependency, call it out clearly and offer a FOSS alternative.

## License

HaramVeil is licensed under GPL-3.0-or-later. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
