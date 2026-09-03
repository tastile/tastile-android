# Tastile Android

Android client for Tastile. The app is written in Kotlin with Jetpack Compose, uses AWS Cognito for mobile-facing auth, and integrates with `tastile-core` through Android native libraries built from the sibling Rust repository.

## Current State

- Active Android app with unit-test coverage for core runtime, sync, notifications, and selected view models.
- Native bridge is partially integrated. Some mobile flows still read from daemon API directly while command execution/state projection is moving behind `tastile-core`.
- This repository assumes `tastile-core` is cloned next to it as `../tastile-core` for Android artifact builds.

## Material 3 Expressive

The Compose UI is on the Material 3 Expressive track (`material3:1.5.0-alpha27`,
pinned in `gradle/libs.versions.toml`). The design-system module is the
only place that talks to alpha APIs directly; screen-level Compose stays on
the stable design-system surface and is enforced by `verifyDesignSystemImports`.

What the design system currently exposes from M3 Expressive:

- **`LoadingWheel`** (`core/designsystem/component/LoadingWheel.kt`) — the
  spinning indicator is now backed by M3 Expressive's `LoadingIndicator`.
  Public signatures (`NiaLoadingWheel`, `NiaOverlayLoadingWheel`) are
  unchanged, so existing call sites need no edits.
- **`MotionScheme.expressive()` injection** (`TastileTheme.kt`) — every
  screen receives `MaterialTheme(motionScheme = …)` automatically, so
  alpha defaults flow through the standard M3 motion APIs without
  per-screen opt-in.
- **`TastileFabMenu`** (`core/designsystem/component/TastileFabMenu.kt`) —
  M3 Expressive FAB Menu wrapper used by `TimelineScreen` and `TilesScreen`
  for the QuickCreate action. Collapsed-state contract: tap on the main FAB
  routes through `onExpandedChange`.
- **`TastileButtonGroup`** (`core/designsystem/component/TastileButtonGroup.kt`) —
  SegmentedButton wrapper with XS–XL sizes and a 48 dp touch target. Used
  by callers that previously rolled their own segmented controls.

Expressive alpha APIs are marked `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)`
at the design-system component that introduces them. The migration's net
`// m2-allow:` marker change is **+1** (three added by the alpha27 pin for
`ExposedDropdownMenu` extension imports and the no-arg `Modifier.menuAnchor()`
overload removal; two removed when `TimelineScreen.kt` swapped its direct
M3 FAB imports for `TastileFabMenu`). Every added marker covers a single
direct M3 import, not a permanent widening of the boundary.

The M3 baseline is tracked at
[`docs/superpowers/m3/before-reports/README.md`](docs/superpowers/m3/before-reports/README.md).
After bumping the `material3` alpha, regenerate the Compose Compiler
Reports under `app/build/compose-reports/` and re-capture that baseline.

**Phase 3 device-side verifications (Tasks 3.1 + 3.2) remain deferred.**
A 2026-09-03 attempt on a XIG03 / Android 15 device uncovered two
pre-existing app blockers:

- The instrumented run was blocked by `ExecutionAlarmRescheduleReceiver`
  crashing on `BOOT_COMPLETED` (queued since device boot) because the
  Hilt-generated wrapper calls `inject()` before
  `Application.onCreate()` finishes initializing the graph. The
  production receiver was rewritten to tolerate Hilt-not-yet-ready via
  `EntryPointAccessors.fromApplication(...)` + try/catch. After the
  fix, the instrumented test starts cleanly (verified via logcat
  `TestRunner: started: timelineQuickCreateFab_opensSheet(...)`) but
  hangs on `LoginScreen` because the auth gate is in front of
  TimelineScreen and the FAB testTag is on TimelineScreen, not
  LoginScreen.
- The gfxinfo run still cannot reach TimelineScreen because the auth
  gate intercepts cold launch.

Neither blocker is in the M3 plan's scope. The cold-launch gfxinfo
capture and the smoke-test file are checked in as evidence; full
details and the follow-up unblock recipe live in
[`docs/superpowers/m3/phase-3-deferral.md`](docs/superpowers/m3/phase-3-deferral.md).

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

## WSLC Development Container

`.wslc/` に開発用コンテナ定義を保持。バージョンは `app/build.gradle.kts` から自動抽出。

```powershell
# ビルド（バージョン自動同期）
.wslc/wslc-build.ps1

# 再ビルド（キャッシュ無効）
.wslc/wslc-build.ps1 -NoCache

# 開発環境起動（ADB + Gradle）
.wslc/wslc-dev.ps1

# ワイヤレス ADB 接続付き起動
.wslc/wslc-dev.ps1 -DeviceIp 192.168.1.100
```

- コンテナイメージ: `tastile-android-dev:latest`
- ADB: デバイスへのアプリインストール・デバッグ対応
- Gradle: ビルド実行可能
- ポート: ADB 5037

### ADB コマンド例

```powershell
# コンテナ内でデバイス一覧表示
wslc exec tastile-android-dev adb devices

# APK インストール
wslc exec tastile-android-dev adb install app/build/outputs/apk/debug/app-debug.apk

# ワイヤレス ADB 接続
wslc exec tastile-android-dev adb connect <device-ip>:5555

# デバッグログ確認
wslc exec tastile-android-dev adb logcat
```
