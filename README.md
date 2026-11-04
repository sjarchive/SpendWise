<div align="center">

<img src="docs/icon.png" width="110" alt="SpendWise app icon"/>

# SpendWise

A modern Android personal-finance app built with **Kotlin** and **Jetpack Compose** (Material 3 Expressive).

Track transactions, manage budgets, gain spending insights, and back up your data — all offline-first with Room.

</div>

## ✨ Features

- **Transactions** — add/edit/delete income & expenses with categories, payment methods, swipe actions
- **Budgets** — monthly budgets per category with progress tracking
- **Insights** — spending trends and category breakdown charts
- **Categories** — full category management with icons
- **Backup & Restore** — CSV export/import and local backup
- **Material 3 Expressive UI** — animated charts, morphing buttons, haptic feedback

## 📲 Install (prebuilt APK)

No build needed — grab an APK straight from GitHub Releases. Both links are
*evergreen*: they always serve the newest build.

- **Recommended:** [`SpendWise-release.apk`](https://github.com/sjarchive/SpendWise/releases/download/stable/SpendWise-release.apk) — R8-minified, signed with the stable upload key
- Testing only: [`SpendWise-debug.apk`](https://github.com/sjarchive/SpendWise/releases/download/stable/SpendWise-debug.apk) — unminified, per-run debug key

Requires a 64-bit ARM (arm64-v8a) device with Android 8.0+. All versioned
releases live on the [Releases page](https://github.com/sjarchive/SpendWise/releases/latest).

## 🏗 Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM (ViewModel + StateFlow), Clean-ish (domain/data/ui layers) |
| Persistence | Room (SQLite) with DAOs & type converters |
| DI | Hilt |

## 🚀 Run Locally (build & install over USB)

Skip CI while testing: build on your computer and install straight to your phone
over USB debugging — the same one-click loop AI Studio provided.

### One-time setup

1. **Install [Android Studio](https://developer.android.com/studio)** — it bundles the
   JDK, Android SDK, and ADB, so nothing else is needed.
2. **Clone this repo:** in Android Studio use `File → New → Project from
   Version Control`, paste the repo URL (`https://github.com/sjarchive/SpendWise`).
   (CLI alternative: `gh repo clone sjarchive/SpendWise`.)
3. **Unlock Developer options on the phone:** Settings → About phone → tap
   **Build number** 7×. Then in Settings → Developer options enable **USB debugging**.
4. **Connect the phone via USB.** On first connect, accept the
   *"Allow USB debugging?"* prompt on the phone (tick *Always allow from this
   computer*). Verify Android Studio shows the device in the device dropdown.
5. **Generate the debug keystore once** in the project root (CI creates one per run;
   locally it is not committed to the repo):
   ```bash
   keytool -genkeypair -v -keystore debug.keystore -storepass android \
     -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 \
     -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   ```
   (On Windows, put the whole command on one line.)

### Day-to-day testing loop

**Android Studio (easiest):** select your phone in the device dropdown → press
**Run ▶**. Studio builds, installs, and launches the app on the device; use the
**Logcat** tab to see logs and crashes.

**Command line (no Studio UI needed):**
```bash
./gradlew installDebug        # builds + installs the debug APK to the connected phone
# optional — launch the app:
adb shell am start -n com.aistudio.spendwise.iuyclr/com.example.MainActivity
# optional — live logs while using the app:
adb logcat --pid=$(adb shell pidof com.aistudio.spendwise.iuyclr)
```

### Notes

- The **debug** build is signed with your local `debug.keystore`, so **no GitHub
  secrets are needed** for testing. A debug build cannot update-over-install the
  **release** APK (different signing keys) — if the release APK is installed,
  uninstall it first.
- Local **release** builds require `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`
  environment variables — leave release/signing to CI.
- Wireless debugging (Android 11+) works too: Developer options →
  **Wireless debugging** → pair with your computer.
- CI (`.github/workflows/build.yml`) remains the source of truth for signed release
  APKs; local builds are for day-to-day testing only.
- **Releases are automatic — no tag pushing:** bump `versionCode`/`versionName` in
  `app/build.gradle.kts`, push to `main`, and the same CI run tests, builds, and
  publishes the GitHub Release (creating the `v*` tag for you) if that version
  doesn't have a release yet.

## 📁 Project Structure

```
app/src/main/java/com/example/
├── MainActivity.kt
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── preferences/    # DataStore preferences
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Domain models
│   └── repository/     # Repository interfaces
├── di/                 # Hilt modules
└── ui/
    ├── components/     # Reusable composables & charts
    ├── navigation/     # Nav graph & routes
    ├── screens/        # Feature screens
    ├── theme/          # Material 3 theming
    ├── util/           # Helpers (CSV, dates, haptics)
    └── viewmodel/      # Feature ViewModels
```

---
