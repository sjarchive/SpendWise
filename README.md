<div align="center">

# 💰 SpendWise V3

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

## 🏗 Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM (ViewModel + StateFlow), Clean-ish (domain/data/ui layers) |
| Persistence | Room (SQLite) with DAOs & type converters |
| DI | Hilt |
| AI | Gemini API (server-side key via `.env`) |

## 🚀 Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose this project directory
3. Allow Android Studio to fix any incompatibilities while importing
4. Create a file named `.env` in the project root and set your Gemini API key (see `.env.example`):
   ```
   GEMINI_API_KEY=your_key_here
   ```
5. Remove this line from `app/build.gradle.kts` (only needed for signed AI Studio builds):
   ```
   signingConfig = signingConfigs.getByName("debugConfig")
   ```
6. Run the app on an emulator or physical device

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

<div align="center">
<sub>Built with Google AI Studio — <a href="https://ai.studio/apps/eb33357d-190e-4cb6-ac96-fe56ec5523a4">View original app</a></sub>
</div>
