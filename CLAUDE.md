# Tastile Android

Android client for Tastile task management app.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Hilt DI
- **Backend**: Supabase (Auth + PostgREST)
- **Build System**: Gradle with Kotlin DSL

## Project Structure

```
app/src/main/java/app/tastile/android/
├── TastileApp.kt              # Application class with Hilt
├── MainActivity.kt            # Main entry point with deep link handling
├── navigation/
│   └── TastileNavGraph.kt     # Navigation graph with bottom nav
├── data/
│   ├── SupabaseClient.kt      # Supabase client factory
│   ├── model/
│   │   ├── Tile.kt            # Tile data model
│   │   └── Profile.kt         # Profile data model
│   └── repository/
│       ├── AuthRepository.kt  # Authentication repository
│       ├── TileRepository.kt  # Tile CRUD operations
│       └── ProfileRepository.kt # Profile operations
├── di/
│   └── AppModule.kt           # Hilt dependency injection module
└── ui/
    ├── theme/
    │   ├── Theme.kt           # Material 3 theme (light/dark)
    │   ├── Color.kt           # Lifecycle colors
    │   └── Type.kt            # Typography
    ├── login/
    │   ├── LoginScreen.kt     # Google OAuth login
    │   └── LoginViewModel.kt
    ├── now/
    │   ├── NowScreen.kt       # Main tile list with CRUD
    │   └── NowViewModel.kt
    ├── memo/
    │   ├── MemoScreen.kt      # Quick memo for tiles
    │   └── MemoViewModel.kt
    ├── prompt/
    │   ├── PromptScreen.kt    # 25min+ tile prompts
    │   └── PromptViewModel.kt
    ├── account/
    │   ├── AccountScreen.kt   # Profile & settings
    │   └── AccountViewModel.kt
    └── billing/
        └── BillingScreen.kt   # WebView for billing
```

## Setup

1. **Gradle Sync**: Sync project after opening
2. **Supabase Config**: Already configured in `gradle.properties`
   - URL: `https://cltymfzdhdnebazmayxd.supabase.co`
   - Anon Key: `sb_publishable_55l8G7Jg9F0rgw-vi4bs-Q_ibtaCbkZ`

## Features

- **Authentication**: Google OAuth via Supabase Auth
- **Tiles**: Create, start, complete, delete tiles
- **Lifecycle**: Ready → Started → Done
- **Prompts**: Notification after 25min of active work
- **Memos**: Quick notes on recent tiles
- **Billing**: WebView integration with Stripe

## Build

### Debug Build

```bash
./gradlew assembleDebug
```

### Release Build (AAB for Play Store)

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Release Signing

Release builds are signed with `tastile-release.jks`:
- Keystore: `tastile-release.jks` (in project root, gitignored)
- Alias: `tastile`
- Configured in: `gradle.properties`

⚠️ **Never commit the keystore file or passwords to git.**

## Deep Link

OAuth callback: `tastile://auth/callback`

## Dependencies

- Supabase BOM 3.0.3
- Hilt 2.56.2
- Compose BOM 2024.12.01
- Material 3
- Navigation Compose
- Lifecycle ViewModel Compose

## Known Issues

- **OAuth Deep Link**: `handleDeeplinks` API changed in Supabase 3.x. Current implementation uses `auth.exchangeCodeForSession()` workaround. See `MainActivity.kt` for TODO.
