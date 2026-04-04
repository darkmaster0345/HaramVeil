# HaramVeil

HaramVeil is an Android app focused on on-device harmful-content shielding using:
- Accessibility event monitoring
- Text detection (keyword + OCR)
- Image detection (ONNX Runtime)
- Real-time overlay blocking

Everything runs locally on the device. No cloud inference is required for detection.

## Current Build Status (verified on April 4, 2026)

Verified with local Gradle checks:
- `:app:assembleDebug` - PASS
- `:app:assembleRelease` - PASS
- `:app:testDebugUnitTest` - PASS (`NO-SOURCE`, no unit tests yet)
- `:app:lintDebug` - PASS (`0 errors, 49 warnings`)

Generated APKs:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

## Tech Stack

- Language: Kotlin
- UI: Jetpack Compose + Material3
- Storage: Room + DataStore
- Background work: WorkManager
- Security: EncryptedSharedPreferences
- OCR: Google ML Kit Text Recognition
- Image model runtime: ONNX Runtime Android
- Build: Gradle 8.9 wrapper, AGP 8.2.2, Kotlin 1.9.0

## Android Config

- Package: `com.haramveil.app`
- Min SDK: 26
- Target SDK: 34
- Compile SDK: 34
- App version: `1.0` (`versionCode 1`)

## Repository Structure

Main module:
- `app/`

Key folders:
- `app/src/main/java/com/haramveil/app/`
  - `service/` and `services/` - accessibility and foreground services
  - `detector/`, `detection/`, `mlkit/` - ONNX, keyword, OCR detection logic
  - `ui/` - Compose screens and theme
  - `data/` - Room + DataStore
  - `security/` - PIN and encrypted storage
  - `utils/` - helper utilities
- `app/src/main/res/` - Android resources
- `app/src/main/assets/320n.onnx` - bundled base ONNX model

## Detection Pipeline

1. Accessibility events are received by `HaramVeilAccessibilityService`.
2. Screen text is extracted from the active node tree.
3. Keyword detector evaluates extracted text.
4. Screenshot capture is attempted (API 30+) for ONNX image analysis.
5. If harmful content is detected, an overlay event is posted.
6. `MainActivity` observes the event and opens `BlockOverlayScreen`.

Settings consumed by the service:
- Text detection enable/disable
- Image detection enable/disable
- Notifications enable/disable
- Enhanced model toggle

## Models

Base model:
- File: `app/src/main/assets/320n.onnx`
- Loaded by default

Enhanced model:
- File path at runtime: `files/models/640m.onnx`
- Downloaded in-app via `ModelDownloadManager`
- Used automatically when toggle is enabled and file is available
- Falls back to base model if enhanced file is missing or invalid

## Permissions and Components

Important permissions declared in `app/src/main/AndroidManifest.xml`:
- `android.permission.SYSTEM_ALERT_WINDOW`
- `android.permission.RECEIVE_BOOT_COMPLETED`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- Foreground service permissions

Core Android components:
- `MainActivity`
- Accessibility service: `service.HaramVeilAccessibilityService`
- Foreground services:
  - `services.ProtectionForegroundService`
  - `services.HaramVeilForegroundService`
- Boot receiver: `receivers.BootCompletedReceiver`
- Device admin receiver: `admin.HaramVeilAdminReceiver`

Backup and transfer rules are enabled in manifest:
- `@xml/backup_rules`
- `@xml/data_extraction_rules`

## Prerequisites

- Android Studio (recent stable)
- Android SDK installed locally
- JDK 17+ (JDK 21 also works)

If building from CLI, ensure SDK location is set in `local.properties`:
- `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`

## Build and Run

Windows (PowerShell):

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

macOS/Linux:

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Install debug build:

```powershell
.\gradlew.bat :app:installDebug
```

## First Run Setup on Device

1. Install and open the app.
2. Enable Accessibility service for HaramVeil.
3. Grant overlay permission.
4. Grant notification permission (Android 13+).
5. Disable battery optimization for uninterrupted monitoring.
6. Enable protection from onboarding/dashboard.

## Quality and Testing Notes

- Unit tests are not implemented yet (`testDebugUnitTest` is `NO-SOURCE`).
- No connected instrumented verification is possible without a real device/emulator attached.
- Lint currently reports warnings (dependency updates, icon polish, some obsolete API guards), but no blocking errors.

## Known Limitations

- Release build currently uses `minifyEnabled false`.
- Dependency versions are functional but not the newest available.
- Screenshot-based image analysis requires API 30+ for actual capture path.
- For complete runtime validation, test on a real device with accessibility + overlay enabled.

## Troubleshooting

Build fails with "SDK location not found":
- Ensure `local.properties` exists with valid `sdk.dir`.

App installs but does not block:
- Verify Accessibility service is enabled.
- Verify overlay permission is granted.
- Verify protection is turned on in app UI.

Enhanced model not used:
- Check download completed in Settings.
- Ensure enhanced toggle is enabled.
- If file is missing/corrupt, app falls back to base model.

## Security and Privacy

- Content analysis is performed on-device.
- PIN data is stored in encrypted preferences.
- No detection content is sent to remote servers by default.

## License

Project license information should be added in a dedicated `LICENSE` file if distribution is planned.
