# Tastile Android

Android client for Tastile. The app is written in Kotlin with Jetpack Compose, uses AWS Cognito for mobile-facing auth, and integrates with `tastile-core` through Android native libraries built from the sibling Rust repository.

## Current State

- Active Android app with unit-test coverage for core runtime, sync, notifications, and selected view models.
- Native bridge is partially integrated. Some mobile flows still read from daemon API directly while command execution/state projection is moving behind `tastile-core`.
- This repository assumes `tastile-core` is cloned next to it as `../tastile-core` for Android artifact builds.

## Repository Layout

```text
app/                    Android application module
docs/                   Architecture, development, plans, and operations docs
gradle/                 Wrapper files
build.gradle.kts        Root build and verification entrypoints
README.md               High-level orientation
CONTRIBUTING.md         Day-to-day contributor workflow
SECURITY.md             Secret handling and reporting guidance
```

## Prerequisites

- JDK 17 or JDK 21
- Android Studio Hedgehog or newer
- Android SDK with API 35 and NDK installed
- Rust toolchain with Android targets:

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
cargo install cargo-ndk
```

## Quick Start

1. Clone `tastile-android` and `tastile-core` as sibling directories.
2. Ensure `JAVA_HOME` points to JDK 17 or 21.
3. Run `./gradlew verify` to execute the JVM verification suite.
4. Run `./gradlew assembleDebug` to build the debug APK and native libraries.

## Build Modes

- `./gradlew verify`
  Runs the repository verification suite. This is the default pre-push command.
- `./gradlew testDebugUnitTest`
  Runs Android unit tests without requiring a release keystore.
- `./gradlew assembleDebug`
  Builds the Android app and compiles native libraries from `../tastile-core`.
- `./gradlew bundleRelease`
  Requires release signing properties in `~/.gradle/gradle.properties` or `-P...` flags.

Release signing credentials must never be committed. The build fails fast if they are missing for release tasks.

## Documentation

- [Docs Index](./docs/README.md)
- [Architecture](./docs/architecture.md)
- [Development Guide](./docs/development.md)
- [Release Operations](./docs/operations/release-plan.md)

## Notes

- `app/src/main/jniLibs/` is generated output and should not be committed.
- Cognito client values live in BuildConfig fields. Upload keys and machine-local settings belong in user-level Gradle properties instead.
- If `tastile-core` is missing, native build tasks fail with an explicit message instead of a cargo error cascade.
