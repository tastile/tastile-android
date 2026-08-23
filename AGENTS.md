# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Canonical Contract

This is the `tastile-android` child repository of the Tastile workspace. The workspace contract is `../AGENTS.md` — read it first, then this repo's `README.md` (orientation) and `docs/architecture.md` (layer breakdown) before any non-trivial work.

Project-local Skills live in `.agents/skills/`. Claude Code skill adapter mirrors the canonical skills at `.claude/skills/`. Agent-specific settings and hooks inherit from the workspace root; this repo does not redefine them. Do not duplicate workspace-wide rules here.

## Build and Verify

All commands run from this repo root. JDK 17 or 21, Android SDK with API 35, NDK, and the Rust toolchain with `cargo-ndk` are required.

| Goal | Command |
| --- | --- |
| Full verification suite (default pre-push gate) | `./gradlew verify` |
| JVM unit tests only (no release keystore needed) | `./gradlew testDebugUnitTest` |
| Single unit test class | `./gradlew testDebugUnitTest --tests "app.tastile.android.<package>.<ClassName>"` |
| Single unit test method | `./gradlew testDebugUnitTest --tests "app.tastile.android.<package>.<ClassName>.<methodName>"` |
| Debug APK + native libs (needs `../tastile-core`) | `./gradlew assembleDebug` |
| Release build (fails fast without signing props) | `./gradlew bundleRelease` |
| Instrumented tests (Hilt + Espresso) | `./gradlew connectedDebugAndroidTest` |
| Lint only | `./gradlew lintDebug` |

`./gradlew verify` depends on `:app:check`, which itself depends on the project guard tasks `verifyDesignSystemImports` and `verifyNoEmbeddedServerSecrets` registered in `app/build.gradle.kts`.

## Build-Time Hard Requirements

These guards fail the build rather than silently degrading — they exist to prevent environment drift from shipping to users.

- `gradle.projectsEvaluated` in `app/build.gradle.kts` requires every `BuildConfig.*` field listed in `app/build.gradle.kts` (Cognito client/region/hosted-ui/redirect/web-auth base, `TASTILE_CORE_URL`, `GOOGLE_WEB_CLIENT_ID`) to be non-blank. Set them in `gradle.properties` (CI), `~/.gradle/gradle.properties` (local dev), or `-PKEY=value`.
- Release tasks (`assembleRelease`, `bundleRelease`) fail fast if `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` are not provided via the same paths. Never commit keystore or `google-services.json`.
- `verifyDesignSystemImports`: direct `androidx.compose.material3.*` imports are forbidden in `app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}/` unless the immediately preceding non-blank line is `// m2-allow:`. M3 unified screens must go through the design system.
- `verifyNoEmbeddedServerSecrets`: rejects `TASTILE_WEB_BRIDGE_SECRET` / `x-tastile-web-bridge-secret` from Android sources and the build script. Server-only bridge credentials must not enter Android artifacts.
- The lint block in `app/build.gradle.kts` must not add `disable +=`. Every lint rule surfaces; unaddressable rules go in a tracking doc with a hard BLOCKED rationale.
- Native artifact builds require `../tastile-core` to be a sibling checkout. If missing, the build fails with an explicit message rather than a cargo error cascade.

## Architecture (Quick Map)

Compose UI → ViewModels → Repositories → Cognito auth and/or `tastile-core` native bridge.

- `ui/` — Compose screens, state holders, presentation helpers
- `data/` — Auth repository, data models, repository interfaces
- `domain/` — Domain layer (audit baseline recommends, treat as required)
- `core/` — Native bridge, runtime persistence, DTO mapping for `tastile-core`
- `sync/` — Session handoff and event synchronization into the core runtime
- `notifications/` — Alarm scheduling, notification policy, delivery
- `di/` — Hilt modules
- `execution/` — Execution state projection

Auth and server-backed reads go through Cognito + daemon API. Command execution, replay, and projected execution state are moving behind `tastile-core`; keep that boundary explicit until migration completes.

## Toolchain

Authoritative versions live in `build.gradle.kts` (plugins) and `app/build.gradle.kts` (deps + sdk). The table below is a snapshot for orientation; if it disagrees with the build script, the build script wins.

- AGP 9.3.1, Kotlin 2.2.10, Compose Compiler plugin 2.2.10, Hilt plugin 2.59.2 / Hilt 2.60.1, KSP 2.3.6
- Compose BOM 2024.12.01, Navigation Compose 2.9.8
- `minSdk` 26, `targetSdk` 35, `compileSdk` 37, `versionCode` 33, `versionName` 0.4.0
- `kotlinx-datetime` is pinned at 0.6.1 and `kotlinx-coroutines-test` at 1.9.0 — bumping either surfaces an `ExperimentalTime` opt-in requirement. See `docs/plans/`.
- Compose Compiler Reports land in `app/build/compose-reports/` and `app/build/compose-metrics/`; baseline at `docs/superpowers/m3/before-reports/`.

## WSLC Dev Container

`.wslc/` holds the Windows + WSL Container definitions; the version is auto-extracted from `app/build.gradle.kts`.

- Build: `.wslc/wslc-build.ps1` (add `-NoCache` to bust caches)
- Dev shell: `.wslc/wslc-dev.ps1` (add `-DeviceIp <ip>` for wireless ADB)
- ADB inside container: `wslc exec tastile-android-dev adb devices`

## Working Rules

- Work on local `main`. Do not create feature branches, temporary branches, or worktrees.
- Source code, identifiers, code comments, and Git/GitHub messages are English. Internal development docs are Japanese.
- Do not write new Python scripts in this repo. Use Kotlin, shell, or PowerShell as appropriate.
- Search with `rg` / `rg --files`; prefer semantic navigation via the Kotlin language tooling already in `.tools/`.
- Never commit: `local.properties`, `google-services.json`, keystores, `.env*` with real values, generated `app/src/main/jniLibs/`, or anything in `reference/`, `.build-logs/`, `.tools/`.
- `reference/` clones are read-only; they must not become implicit build or runtime dependencies.
- Before claiming "PASS / DONE / GREEN / ready to ship", run `./gradlew verify` from a clean state. If your change is in `:app` source, also run the unit-test target to catch regression coverage gaps.

## Related Workspace Siblings

- `../tastile-core/` — Rust core, produces Android native libs via `cargo-ndk`. Required for artifact builds.
- `../tastile-web/` — Next.js sibling; shares Cognito config values with this repo.
- `../AGENTS.md` — workspace contract. Read it before any cross-repo change.
