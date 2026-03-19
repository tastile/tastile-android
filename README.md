# Tastile Android

Android client for Tastile task management app.

## Architecture

This app uses **tastile-core** (Rust) as native libraries via JNI:

```
Android App (Kotlin/Jetpack Compose)
    ↓ (JNI via JNA or native bindings)
libtastile_core.so (Rust compiled for Android)
    ↓ (relative path at build time)
../tastile-core/
```

## Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 26+
- Rust toolchain with Android targets:
  ```bash
  rustup target add aarch64-linux-android
  rustup target add armv7-linux-androideabi
  rustup target add i686-linux-android
  rustup target add x86_64-linux-android
  ```
- NDK (installed via Android Studio)
- cargo-ndk:
  ```bash
  cargo install cargo-ndk
  ```

## Building

### Automatic Build (Recommended)

The Gradle plugin automatically builds core:

```bash
./gradlew assembleDebug
```

This will:
1. Build tastile-core for all Android architectures
2. Copy `.so` files to `app/src/main/jniLibs/`
3. Build Android app

### Manual Build

If you need to build core separately:

```bash
cd ../tastile-core

# Build all Android targets
cargo ndk -t armeabi-v7a -t arm64-v8a -t x86 -t x86_64 build --release

# Or with the Gradle plugin
cd ../tastile-android
./gradlew cargoNdkBuild
```

## Project Structure

```
tastile-android/
├── app/
│   ├── build.gradle.kts       # App build config with cargoNdk
│   └── src/
│       ├── main/
│       │   ├── jniLibs/       # Auto-generated .so files
│       │   └── java/
│       │       └── ...        # Kotlin source
│       └── test/
├── build.gradle.kts           # Root build config
└── settings.gradle.kts
```

## Core Integration

### build.gradle.kts

```kotlin
plugins {
    id("com.github.willir.rust.cargo-ndk-android")
}

cargoNdk {
    module = "../../tastile-core"  // Relative path to core
    targets = listOf("arm64", "arm", "x86", "x86_64")
    outputDirectory = "src/main/jniLibs"
}
```

### Kotlin JNI Bridge (Planned)

```kotlin
// app/src/main/java/app/tastile/android/core/TastileCore.kt
object TastileCore {
    init {
        System.loadLibrary("tastile_core")
    }
    
    external fun createTile(title: String): String
    external fun getTiles(): List<Tile>
    // ...
}
```

## Important

**Never copy core code into this folder!**

The `jniLibs/` directory is auto-generated and gitignored. Always reference `../tastile-core`.

When core is updated:
1. Rebuild: `./gradlew assembleDebug`
2. Or: `cargo ndk build` then rebuild Android

## Migration Notes

Currently the app uses Supabase directly. Migration plan to core:

1. [ ] Add JNI bridge module in tastile-core
2. [ ] Create `TastileCore` Kotlin wrapper class
3. [ ] Replace `TileRepository` calls with core calls
4. [ ] Replace `AuthRepository` with core auth
5. [ ] Remove direct Supabase dependency

## Dependencies

- Jetpack Compose BOM 2024.12.01
- Hilt 2.56.2
- Kotlinx Serialization 1.7.3
- cargo-ndk-android plugin 0.3.4

## Build Outputs

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
