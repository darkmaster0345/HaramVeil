# HaramVeil Project State

## Snapshot

| Field | Value |
|-------|-------|
| App Name | HaramVeil |
| Package | `com.haramveil` |
| Version | `1.0.0` (`versionCode 1`) |
| Min SDK | 29 |
| Target / Compile SDK | 36 |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| License | GPL-3.0-or-later |
| Build | Gradle 9.3.1, AGP 9.1.0, Kotlin Compose plugin 2.3.10 |

## Current Architecture

- `accessibility/`
  Accessibility service entrypoint and service-status helpers
- `detection/mode1/`
  UI tree scanning, keyword matching, risk classification
- `detection/mode2/`
  OCR pipeline with ML Kit or FOSS ONNX OCR selection
- `detection/mode3/`
  NudeNet ONNX preprocessing, model selection, inference, and pacing
- `overlay/`
  Native Veil overlay service, passage loading, and overlay control
- `security/`
  PIN manager, recovery questions, lockdown manager, Device Admin receiver
- `service/`
  Sticky foreground service, boot recovery, alarm backup, WorkManager health checks
- `data/local/`
  DataStore repositories, SQLCipher-backed stats store, cleanup worker
- `ui/`
  Onboarding, dashboard, stats, settings, advanced settings, and security gates

## Implemented Protection Flow

1. Accessibility watches monitored packages only.
2. Mode 1 scans the active UI tree and publishes detection events.
3. Mode 2 and Mode 3 subscribe independently through `DetectionBus`.
4. The Veil overlay service responds to `VeilRequested`.
5. HaramVeil sends the user home and stores a persistent lockdown timer.
6. Block history is written to the encrypted local stats database.

## Storage

- DataStore preferences for protection settings and onboarding state
- EncryptedSharedPreferences for PIN, recovery-question lockout state, and lockdown timers
- SQLCipher-backed local stats database keyed from the stored PIN hash

## Release Readiness Notes

- Release metadata lives under `fastlane/metadata/android/en-US/`
- GitHub Actions release build lives at `.github/workflows/build.yml`
- `LICENSE` contains the full GPL-3.0 text
- `NOTICE` lists third-party libraries, licenses, and compatibility notes

## Open Release Caveats

- The optional ML Kit dependency is proprietary and should be excluded from any F-Droid submission build.
- The project currently uses `net.zetetic:android-database-sqlcipher`, which upstream has deprecated in favor of the newer `sqlcipher-android` line. Migration is recommended before wider public distribution.
- Runtime verification is strongest on a real device; emulator coverage depends on locally installed system images.
