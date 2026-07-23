# Android Lint Hard-Blockers (2026-07-23)

> Tracking doc for lint warnings that remain after the
> `fix(android): re-enable 5 lint rules + root-fix @Suppress sites` sweep.
> None of these can be silenced in `lintOptions { disable += ... }` —
> they need real follow-up work tracked here.

---

## 1. `OldTargetApi` — `app/build.gradle.kts:46` `targetSdk = 35`

**Status:** BLOCKED on this Windows host.

**Why:** Lint flags `targetSdk = 35` because Android 36 (Baklava) is the
latest stable API. Bumping `targetSdk` to 36 requires the
`platforms/android-36` SDK package installed on the build host. Per
project memory (commit a2c508c): "local `android-38` was a mislabeled
duplicate of `android-37.0`; real installed API levels are 35 and 37.
`compileSdk=35` is the safe pick". Until the missing platform is
restored, the warning is unavoidable.

**Resolution:** run `sdkmanager "platforms;android-36"` (or
re-extract from a clean Android Studio install that ships 36), then bump
`targetSdk = 36` and `compileSdk = 36` and re-run lint. Add `compileSdk
= 36` only after `platforms;android-36` is on disk.

**Owner:** dev-host setup.

---

## 2. `GradleDependency` — `androidx.compose:compose-bom 2024.12.01`

**Status:** BLOCKED on Compose Compiler / Kotlin compatibility review.

**Locations:** `app/build.gradle.kts:221` (impl), `:284` (androidTest).

**Why:** The lint-recommended bump is `2024.12.01 → 2026.06.01`, a
1.5-year leap in Compose BOM that requires:
- AGP 9.x with Compose Compiler plugin matching Kotlin 2.x (we are on
  Kotlin via `kotlin.plugin.compose` but the BOM-mandated compiler
  artifact still needs verification)
- material3 1.5.0-alpha24 → 1.5.x stable (the BOM pins a compatible
  material3; we pin our own `1.5.0-alpha24` so a BOM bump would
  conflict with the explicit version)

**Resolution:** open a follow-up issue to bump compose-bom together
with material3 and re-run `lintDebug` + `assembleRelease` + a smoke
build against CI `ubuntu-latest` before merging.

**Owner:** Compose migration track.

---

## 3. `NewerVersionAvailable` — `kotlinx-datetime 0.6.1 → 0.8.0`

**Location:** `app/build.gradle.kts:242`.

**Why:** 0.8.0 promoted `kotlinx.datetime.Instant` arithmetic APIs to
`@ExperimentalTime`. Two production files would need `@OptIn` additions:
- `app/src/main/java/app/tastile/android/execution/ExecutionStateProjector.kt:35,111,115`
- `app/src/main/java/app/tastile/android/notifications/ExecutionAlarmPlanner.kt:15,25,34,36,38,40,49,53,57`

**Resolution:** add `@OptIn(kotlin.time.ExperimentalTime::class)` to the
two files (or the relevant functions), then bump to `0.8.0`.

**Owner:** execution track.

---

## 4. `NewerVersionAvailable` — `kotlinx-coroutines-test 1.9.0 → 1.11.0`

**Location:** `app/build.gradle.kts:267`.

**Why:** 1.11.0 also promotes some test-dispatcher APIs to
`@ExperimentalTime`. Bumping breaks `runTest { … advanceTimeBy(...) }`
calls in existing tests without an `@OptIn` migration.

**Resolution:** add `@OptIn` annotations to the affected test files,
then bump to `1.11.0`. Co-bumps with the runtime
`kotlinx-coroutines` dependency in the Compose stack.

**Owner:** test infrastructure.

---

## Acceptance criteria recap (post-sweep)

- `:app:lintDebug` exit code: 0
- Errors: 0
- Warnings: 5 (4 documented above + `ObsoleteSdkInt` resolved during
  the sweep — see commit message)
- Skipped/ignored tests: 1 (the failing regression in
  `V1ApiTokenTest.kt` is intentional — see
  `docs/plans/2026-07-23-android-api-token-scopes-wire-fix.md`).
  The companion `V1ApiToken.scopes: List<String> → String?` model fix
  is not in this commit's scope; tracked by the wire-fix plan.
- `connectedDebugAndroidTest`: BLOCKED — no device/emulator attached
  to the Windows host.