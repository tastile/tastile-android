# Android API Token `scopes` Wire Mismatch — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix `V1ApiTokenCreateResponse.scopes` so that minting succeeds when Core returns `"scopes": "all"`, restoring tile/timeline rendering on `tastile-android`.

**Architecture:** One DTO field type change (`List<String>` → `String?`) in `V1ApiToken.kt`. The wire format lives in `tastile-core/crates-v1/storage/src/migrations.rs:1531` (`scopes text NOT NULL DEFAULT 'all'`) and `crates-v1/api/src/handlers/auth.rs:validate_api_token_scopes` only accepts `"all"`. Android never serializes `scopes` outbound — only the mint response carries it — so the fix is read-side only.

**Tech Stack:** Kotlin 2.x, kotlinx.serialization, JUnit4, AGP 9.2.1, Gradle 9.6.0, JDK 17. Xiaomi `pm install --user 0 -r` reinstall.

---

## Background (why this fix is the entire problem)

Smoke gun from on-device logcat (2026-07-23):

```
mintApiToken failed: Unexpected JSON token at offset 99:
  Expected start of the array '[', but had '"' instead at path: $.scopes
JSON input: ....,"scopes":"all","token":"tastile_f7496c3.....
```

Failure chain (already verified end-to-end):

1. `DashboardViewModel.init` → `reloadVisibleTilesAndExecutionControls`
2. → `tileRepository.getTiles(filter)` → `v1ApiClient.getTiles(...)`
3. → `tokenProvider.accessToken()` returns `null` because `ApiTokenManager.mintApiToken` swallowed the deserialization error
4. → `v1ApiClient` throws `V1Error.Auth()`
5. → caught in `TileRepository.getTiles`, replaced with `TilesResponse(emptyList(), source=v1_unavailable)`
6. → UI renders empty state (no tiles, no timeline)

Fixing the DTO restores the mint path → token is non-null → reads succeed → UI renders.

---

## Task 1: Add failing regression test for `scopes: "all"`

**Files:**
- Modify: `app/src/test/java/app/tastile/android/data/api/V1ApiTokenTest.kt`

**Step 1: Append the failing test** to the end of `V1ApiTokenTest.kt` (preserve existing imports and `@Before` setup):

```kotlin
@Test
fun `V1ApiTokenCreateResponse parses Core wire format with scopes string`() {
    val wire = """
        {
          "token": "tastile_f7496c3d9c2a",
          "token_id": "8e6c0e74-1f33-7c8b-9a8e-8c3f4a1d2b6e",
          "label": "android-client",
          "scopes": "all",
          "created_at": "2026-07-23T07:53:01Z",
          "expires_at": null
        }
    """.trimIndent()

    val resp = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }.decodeFromString<V1ApiTokenCreateResponse>(wire)

    assertEquals("all", resp.scopes)
    assertEquals("tastile_f7496c3d9c2a", resp.token)
    assertEquals("8e6c0e74-1f33-7c8b-9a8e-8c3f4a1d2b6e", resp.tokenId)
}
```

If the file does not already import `kotlinx.serialization.json.Json`, `kotlin.test.assertEquals`, `kotlin.test.Test`, add them. Match the existing import style in the file.

**Step 2: Run the new test in isolation to confirm it fails**

Run (from `tastile-android/`):

```bash
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.api.V1ApiTokenTest.V1ApiTokenCreateResponse parses Core wire format with scopes string"
```

Expected: FAIL with `SerializationException: ... Expected start of the array '[', but had '"' instead at path: $.scopes`.

**Step 3: Commit the failing test**

```bash
git add app/src/test/java/app/tastile/android/data/api/V1ApiTokenTest.kt
git commit -m "test(v1): add regression for V1ApiTokenCreateResponse scopes=\"all\" wire"
```

---

## Task 2: Fix DTO field types

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/api/V1ApiToken.kt`

**Step 1: Change `V1ApiTokenCreateResponse.scopes`**

In `V1ApiTokenCreateResponse` (around line 41), replace:

```kotlin
@SerialName("scopes") val scopes: List<String> = emptyList(),
```

with:

```kotlin
@SerialName("scopes") val scopes: String? = null,
```

**Step 2: Change `V1ApiTokenView.scopes` the same way** (any other view that carries `scopes` from the same payload). Open the file, search for `scopes` declarations; apply the same `List<String>` → `String?` swap. Do not touch `V1ApiTokenCreateRequest` — it is unused outbound and not part of this bug.

**Step 3: Run the failing test from Task 1 to confirm it now passes**

```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.api.V1ApiTokenTest"
```

Expected: PASS. Run the full unit-test suite to confirm no regressions:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all green.

**Step 4: Commit**

```bash
git add app/src/main/java/app/tastile/android/data/api/V1ApiToken.kt
git commit -m "fix(v1): V1ApiTokenResponse.scopes is String (Core returns \"all\")"
```

---

## Task 3: Rebuild the debug APK

**Files:**
- Output: `app/build/outputs/apk/debug/app-debug.apk` (regenerated)

**Step 1: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. APK written to `app/build/outputs/apk/debug/app-debug.apk`.

**Step 2: Verify APK mtime is fresh (defends against stale-install false positive)**

```bash
stat app/build/outputs/apk/debug/app-debug.apk
adb shell stat -c '%y' /data/local/tmp/app-debug.apk 2>/dev/null || true
```

The host mtime must be later than the device-resident APK (if any). If a `app-debug.apk` exists on the device under `/data/local/tmp/`, push the new one over it (next task).

---

## Task 4: Reinstall on device (Xiaomi `pm install -r` workaround)

**Files:**
- Push: host APK → device path

**Step 1: Verify device + package**

```bash
adb devices
adb shell pm list packages | grep -i tastile
```

Expected: one device authorized, package `app.tastile.android` (or current applicationId — confirm via `app/build.gradle.kts`).

**Step 2: Push the new APK to a device-side temp location**

```bash
MSYS_NO_PATHCONV=1 adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/app-debug.apk
```

Expected: `... pushed. 1.x MB/s (... bytes in ...s)`.

**Step 3: Install in user 0 (bypasses MIUI signature-mismatch block)**

```bash
adb shell pm install --user 0 -r /data/local/tmp/app-debug.apk
```

Expected: `Success`.

If `Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]` appears, uninstall first then reinstall:

```bash
adb shell pm uninstall app.tastile.android
adb shell pm install --user 0 -r /data/local/tmp/app-debug.apk
```

This drops local session state; the user will need to log in again — that is expected.

**Step 4: Clean up device temp**

```bash
adb shell rm /data/local/tmp/app-debug.apk
```

---

## Task 5: Verify tiles + timeline render on device

**Files:** none (live verification)

**Step 1: Clear logcat, start the app, log in**

```bash
adb logcat -c
adb shell am start -n app.tastile.android/.MainActivity
```

Sign in via Cognito (Hosted UI). Wait until the dashboard reaches the post-login idle state.

**Step 2: Confirm mint no longer fails and reads succeed**

```bash
adb logcat -d V:* | grep -E "ApiTokenManager|TileRepository|V1ApiClient" | head -50
```

Expected log lines:

- No `mintApiToken failed: ...$.scopes`
- `mintApiToken ... ok` (or equivalent success trace)
- `getTiles ... count>0 source=v1` (instead of `count=0 source=v1_unavailable`)
- `readCloudTimeline ... source=v1`

**Step 3: Visual confirmation**

Open the dashboard. Confirm:
- Tiles list renders tile cards (not empty)
- Timeline screen renders time-grid blocks (not empty)

If either is empty, capture another logcat window and re-check the failure chain (most likely another silent swallow in `TileRepository`; do not "fix forward" — stop and re-trace).

**Step 4: Capture verification screenshot + log snippet for the commit message**

```bash
adb shell screencap -p /sdcard/verify.png
adb pull /sdcard/verify.png ./verify-2026-07-23-android-tiles.png
adb logcat -d > ./verify-2026-07-23-android-tiles.log
```

**Step 5: Commit verification artifacts**

```bash
git add verify-2026-07-23-android-tiles.png verify-2026-07-23-android-tiles.log
git commit -m "verify(v1): tiles + timeline render after scopes wire fix"
```

---

## Rollback

If the device verification fails or a regression appears:

```bash
git revert --no-edit HEAD~1..HEAD  # reverts the two fix/test commits
./gradlew :app:assembleDebug
# repeat Task 4 install steps
```

The app falls back to "no tiles rendered" — the pre-fix observable behavior. No data corruption possible; this is read-side DTO only.

---

## Definition of done

- [ ] Task 1 regression test fails before Task 2, passes after
- [ ] Full `:app:testDebugUnitTest` green
- [ ] New APK installed on device
- [ ] On-device: `mintApiToken failed` absent from logcat
- [ ] On-device: tile count > 0 in logcat (`source=v1`, not `v1_unavailable`)
- [ ] On-device: tiles list and timeline grid render non-empty
- [ ] Verification screenshot + log committed
- [ ] `tastile-core/HARNESS.md` §実装履歴 updated per the post-implementation note in the repo `CLAUDE.md`
