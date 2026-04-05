# HaramVeil

> "Veil the Haram. Guard Your Gaze."

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://android.com)
[![F-Droid](https://img.shields.io/badge/F--Droid-Pending-blue.svg)](https://f-droid.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-29-orange.svg)](https://developer.android.com)

HaramVeil is a privacy-first, Islamic-values-aligned,
on-device content filtering app for Android.
It uses a 3-layer AI detection system to block
harmful visual content, with zero cloud dependency,
zero tracking, and zero internet in production.

---

## Screenshots

<!-- Add screenshots here after device testing -->
*Screenshots coming soon*

---

## Features

### 3-Layer Detection System
- **Mode 1 - Static Node Scanning**: Reads the Android UI tree. Near-zero CPU. Always on.
- **Mode 2 - OCR Intelligence**: Fires when Mode 1 detects a risky app. Reads text inside images using ML Kit or the FOSS ONNX OCR engine.
- **Mode 3 - Visual AI**: Uses the NudeNet v3.4 ONNX model to detect explicit visual content with no text at all.

### The Veil
When harmful content is detected:
- Full screen overlay fires instantly
- Displays a random Quranic ayah or hadith (Arabic + English) from a bundled collection
- Offending app is force-closed silently
- User lands on home screen
- App enters a configurable lockdown timer

### Security
- PIN protection with bcrypt hashing
- Brute force lockout (3 attempts -> 20 min)
- Forgot PIN via security questions
- DeviceAdmin anti-uninstall protection
- Direct Boot compatibility (active before unlock)
- EncryptedSharedPreferences + SQLCipher database
- Clock-bypass resistant lockout (`elapsedRealtime`)

### Privacy
- Zero internet permission in production
- Zero cloud inference
- Zero analytics or crash reporting
- All data stored locally and encrypted
- `allowBackup` disabled

### Performance (Samsung A13 / Exynos 850)
- Mode 1: ~0% CPU (UI tree reading only)
- Mode 2: Medium (fires only on risk events)
- Mode 3: Max 1 inference per 1-2 seconds
- HaramClip scans only the top 30% + middle 40% of the screen to cut AI processing load
- Total service RAM target: under 80MB

---

## Build Flavors

| Flavor | ML Kit | Internet | For |
|--------|--------|----------|-----|
| foss | No | Never | F-Droid / de-Googled ROMs |
| full | Yes (optional) | Debug only | GitHub Releases |

---

## Download

| Version | Flavor | Link |
|---------|--------|------|
| v1.0.0 | FOSS | [Release pending](https://github.com/darkmaster0345/HaramVeil/releases) |
| v1.0.0 | Full | [Release pending](https://github.com/darkmaster0345/HaramVeil/releases) |
| F-Droid | FOSS | Submission pending |

---

## Build from Source

### Prerequisites
- Android Studio (latest stable)
- Android SDK 36
- JDK 17+
- Kotlin 2.x

### Clone and build

```bash
git clone https://github.com/darkmaster0345/HaramVeil.git
cd HaramVeil

# FOSS build (for F-Droid)
./gradlew assembleFossRelease

# Full build (with ML Kit)
./gradlew assembleFullRelease
```

---

## ONNX Models

| Model | Source | Size | Use |
|-------|--------|------|-----|
| 320n.onnx | NudeNet v3.4 | ~6MB | Default (all devices) |
| 640m.onnx | NudeNet v3.4 | ~15MB | Enhanced (capable devices) |

Source: https://github.com/notAI-tech/NudeNet/releases/tag/v3.4-weights
License: MIT

---

## Permissions Explained

| Permission | Why |
|-----------|-----|
| BIND_ACCESSIBILITY_SERVICE | Read the UI tree for Mode 1 detection |
| SYSTEM_ALERT_WINDOW | Show the Veil overlay on top of all apps |
| FOREGROUND_SERVICE | Keep protection alive in the background |
| RECEIVE_BOOT_COMPLETED | Auto-start after device reboot |
| BIND_DEVICE_ADMIN | Prevent unauthorized uninstallation |
| POST_NOTIFICATIONS | Show protection status notifications |

**No `INTERNET` permission in production builds.**

---

## Privacy Policy

HaramVeil performs all content analysis on-device.
No detection data, usage statistics, or personal
information is transmitted to any server.
All user data, including the PIN, keywords, and block history,
is stored locally in encrypted storage and never leaves
the device.

---

## Battery Usage Guide

| Mode | Expected Battery Impact |
|------|-------------------------|
| Mode 1 only | < 1% per hour |
| Mode 1 + 2 | ~1-2% per hour |
| Mode 1 + 2 + 3 | ~2-4% per hour |

*Tested on Samsung Galaxy A13. Results vary by device.*

---

## Known Limitations

- Android 10+ required (API 29)
- `takeScreenshot()` requires Android 11+; the Mode 2 screenshot path is gracefully skipped on Android 10 / API 29
- No iOS version (Android Accessibility API only)
- Device Admin uninstall protection can still be bypassed by factory reset
- Samsung Knox may interfere on some devices

---

## Contributing

This is a solo FOSS project. PRs are welcome.
Please read the GPL-3.0 license before contributing.
All contributions must be compatible with GPL-3.0.

---

## License

Copyright (C) 2026 Ubaid ur Rehman

This program is free software: you can redistribute
it and/or modify it under the terms of the GNU General
Public License as published by the Free Software
Foundation, either version 3 of the License, or
(at your option) any later version.

See [LICENSE](LICENSE) for full terms.

---

*"Tell the believing men to lower their gaze
and guard their private parts. That is purer
for them." - Quran 24:30*

---
