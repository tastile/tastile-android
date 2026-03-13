# Tastile Android

Android native companion app for Tastile execution control system.

## Tech Stack
- Kotlin + Jetpack Compose
- Supabase Kotlin SDK (Auth, DB, Realtime)
- Room (offline cache)
- WorkManager (background sync)
- Material 3 + Dynamic Color

## Architecture
- app/src/main/java/app/tastile/android/
  - MainActivity.kt — Entry point
  - ui/theme/ — Material 3 theme
  - ui/screens/ — Compose screens (Now, Prompt, Tiles, Memo)
  - data/ — Supabase client, Room DB, repositories
  - services/ — Notification, sync workers

## Key Responsibilities
- Prompt display and response via push notification
- Active tile visualization
- Quick memo input
- Supabase sync with offline cache

## Setup
Open in Android Studio. Gradle sync will download dependencies.
