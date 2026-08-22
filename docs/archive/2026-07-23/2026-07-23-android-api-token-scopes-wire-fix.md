# Android API Token `scopes` Wire Mismatch — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix the two confirmed Android read-path wire mismatches so Core API-token minting and `GET /v1/tiles` decoding both succeed, restoring tile/timeline rendering on `tastile-android`.

**Architecture:** Keep Core unchanged because its live handlers and OpenAPI are canonical: API-token `scopes` is the string `"all"`, and `GET /v1/tiles` returns a bare `TileListView[]`. The first fix changes the token DTO field to `String?`; the second makes `V1ApiClient.getTiles` decode `List<TileListView>` and wrap it in the existing Android-internal `V1ListTilesResponse`, leaving next-actionable metadata null because Core does not return it.

**Tech Stack:** Kotlin 2.x, kotlinx.serialization, JUnit4, AGP 9.2.1, Gradle 9.6.0, JDK 17. Xiaomi `pm install --user 0 -r` reinstall.

---

## Background: first confirmed blocker

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

Fixing the DTO restores token minting. Device verification then exposed a second, independent read-path mismatch documented below.

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

**Step 4: Capture local-only verification evidence**

```bash
adb shell screencap -p /sdcard/verify.png
adb pull /sdcard/verify.png "$TEMP/verify-2026-07-23-android-tiles.png"
adb logcat -d | grep -E "ApiTokenManager|TileRepository|V1ApiClient" > "$TEMP/verify-2026-07-23-android-tiles.log"
```

Inspect the filtered log for tokens or user data and keep both artifacts outside the repository. Do not commit screenshots, raw logcat, API tokens, Cognito tokens, or user identifiers.

---

## Addendum: second confirmed blocker — bare `/v1/tiles` array

Device logcat after the token and URL fixes showed:

```text
v1 getTiles failed: Unexpected JSON token at offset 0:
Expected start of the object '{', but had '[' instead at path: $
JSON input: [{"id":"019f8a33-4825-7c13-9c2.....
```

The contract is confirmed in `tastile-core/crates-v1/api/src/handlers/read.rs`: the handler returns `Json<Vec<TileListView>>`, and its OpenAPI response body is `[TileListView]`. Android currently asks kotlinx.serialization to decode that body as `V1ListTilesResponse`, which requires an object envelope.

### Task 6: Add a failing HTTP contract regression test

**Files:**
- Modify: `app/src/test/java/app/tastile/android/data/api/V1ApiClientTest.kt`

**Step 1:** Add a test backed by JDK `HttpServer` that returns a representative production `TileListView` bare array from `/v1/tiles`, calls `V1ApiClient.getTiles()`, and asserts that the single tile is available through `response.tiles` while both next-actionable fields are null.

**Step 2:** Run only the new test with JDK 17.

```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.api.V1ApiClientTest.getTiles decodes Core bare array"
```

Expected before the implementation: FAIL with `Expected start of the object '{', but had '[' instead at path: $`.

### Task 7: Decode the canonical array and retain the internal wrapper

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/api/V1ApiClient.kt`

Change `getTiles` so the selected path is decoded as `List<TileListView>`, then return `V1ListTilesResponse(tiles = tiles)`. Do not change Core, `V1ListTilesResponse`, repository interfaces, or unrelated next-actionable UI state.

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.api.V1ApiClientTest"
./gradlew :app:testDebugUnitTest
```

Expected: both commands pass. Commit the test and implementation together:

```bash
git add app/src/main/java/app/tastile/android/data/api/V1ApiClient.kt \
  app/src/test/java/app/tastile/android/data/api/V1ApiClientTest.kt
git commit -m "fix(v1): decode tile list bare array response"
```

### Task 8: Rebuild and repeat device verification

Repeat Tasks 3–5 with a fresh APK. The diagnostic pass must show no `$.scopes` failure and no `Expected start of the object` failure. Confirm non-empty tile cards and timeline blocks visually. After confirmation, remove only the temporary diagnostic logging changes, rebuild/reinstall once more, and repeat the visual check against the clean APK.

---

## Addendum 2: third confirmed blocker — chained `TimelineItem` decoder failures

After the bare-array fix is in place, on-device logcat showed a *new* decoder failure:

```text
v1 getTimeline failed: MissingFieldException:
  Fields [start_at, end_at] are required for type Span,
  but they were missing at path: $[0].span
JSON input: [{"placement_id":"...","tile_id":"...","revision":1,
              "content":{"title":"休憩","description":null},
              "visual":{"color":"#3b82f6","icon":"check-circle"},
              "role":0,
              "span":{"start":"2026-07-23T00:00:00Z","end":"2026-07-23T00:30:00Z"},
              "inside":null,
              "source":{"kind":1,"detail":"recurring:..."},
              "resolution":{"state":0,...}, ...}]
```

Tracing the Core wire shape through `tastile-core/crates-v1/domain/src/read.rs` and `tastile-core/crates-v1/domain/src/common.rs`:

| DTO | Core `#[derive(Serialize)]` field shape | Android current shape | Mismatch |
| --- | --- | --- | --- |
| `Span` (`common.rs:274`) | `{ start: Instant, end: Instant }` (no rename) | `start_at`/`end_at` SerialName | **field names** |
| `InsideView` (`read.rs:71`) | `{ parent: PlacementId, scope: ScopeKind:i16 }` | `placement_id` only | **field names + missing scope** |
| `PlacementSourceView` (`read.rs:76`) | `{ kind: PlacementSource:i16, detail: String }` | `value` only | **field names + missing detail** |
| `ResolutionInfo` (`read.rs:82`) | `{ state, resolved_at, resolution_hash, violations[] }` | `{ state, resolved_at?, resolution_hash?, violations[] }` | OK with `ignoreUnknownKeys = true` (Core sends more fields, Android ignores) |

The existing `timeline_item_decodes_the_core_array_shape_and_preserves_tile_id` test in `V1ApiClientTest.kt:224` uses the *wrong* fixture (`start_at`/`end_at`/`value`) and passes only because the DTOs match the wrong fixture. It is replaced here with a fixture that matches Core's actual wire.

`PlacementInsideView` and `PlacementSourceView` are referenced from `TileRepository.toCoreTimelineItem` only via `source.value.toInt()` (`TileRepository.kt:388`). The `inside` field is decoded but unused at the repository boundary because Core sends `inside: null` for top-level `休憩` placements. The fix updates the DTOs to match Core and updates the three consumers in `TileRepository.kt`.

Do not change Core. Do not change `V1ListTilesResponse`, `TileRepository.getTimeline`, `TileRepository.toCoreTimelineItem` shape, or the UI's `CoreTimelineItem` (a separate DTO living in `core/CoreDtos.kt` whose `startAt`/`endAt` field names are unrelated to the wire-level `Span`).

### Task 9: Replace the wire-shape test with Core's actual JSON

**Files:**
- Modify: `app/src/test/java/app/tastile/android/data/api/V1ApiClientTest.kt`

**Step 1:** Replace the `timeline_item_decodes_the_core_array_shape_and_preserves_tile_id` test (line 224) body with a fixture that mirrors Core's actual wire shape:

```kotlin
@Test
fun timeline_item_decodes_the_core_span_inside_source_shapes() {
    val payload = """
        [{
          "placement_id":"placement-1", "tile_id":"tile-1", "revision":1,
          "content":{"title":"Planning"},
          "visual":{"color":"#3b82f6"},
          "role":0,
          "span":{"start":"2026-07-01T09:00:00Z","end":"2026-07-01T10:00:00Z"},
          "inside":null,
          "source":{"kind":1,"detail":"recurring:f3aa"},
          "resolution":{"state":0,"resolved_at":"2026-07-01T09:00:00Z","resolution_hash":"00000000-0000-0000-0000-000000000000","violations":[]},
          "source_tile_id":null,"occurrence_id":null,
          "split_index":null,"split_count":null,"split_group_id":null
        }]
    """.trimIndent()

    val items = Json { ignoreUnknownKeys = true }.decodeFromString<List<TimelineItem>>(payload)

    val only = items.single()
    assertEquals("tile-1", only.tileId)
    assertEquals("placement-1", only.placementId)
    assertEquals("2026-07-01T09:00:00Z", only.span.start)
    assertEquals("2026-07-01T10:00:00Z", only.span.end)
    assertEquals(1.toShort(), only.source.kind)
}
```

**Step 2:** Run only the new test with JDK 17.

```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.api.V1ApiClientTest.timeline_item_decodes_the_core_span_inside_source_shapes"
```

Expected before the DTO fix: FAIL with `MissingFieldException: Fields [start_at, end_at] are required for type Span, but they were missing at path: $[0].span`.

**Step 3:** Commit the failing test:

```bash
git add app/src/test/java/app/tastile/android/data/api/V1ApiClientTest.kt
git commit -m "test(v1): pin Core wire shape for Span/Inside/SourceView on TimelineItem"
```

### Task 10: Fix the three DTOs to match Core's wire

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/api/V1Models.kt`

**Step 1:** Replace the `Span` data class (lines 49-53) with field names that match Core's struct `Span { start, end }`:

```kotlin
@Serializable
data class Span(
    val start: String,
    val end: String
)
```

**Step 2:** Replace the `PlacementInsideView` data class (lines 55-58) with Core's `InsideView { parent: PlacementId, scope: ScopeKind:i16 }` shape:

```kotlin
@Serializable
data class PlacementInsideView(
    val parent: String,
    val scope: Short
)
```

**Step 3:** Replace the `PlacementSourceView` data class (lines 60-63) with Core's `PlacementSourceView { kind: PlacementSource:i16, detail: String }` shape:

```kotlin
@Serializable
data class PlacementSourceView(
    val kind: Short,
    val detail: String
)
```

**Step 4:** Run Task 9's test and the full unit-test suite:

```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.api.V1ApiClientTest.timeline_item_decodes_the_core_span_inside_source_shapes"
./gradlew :app:testDebugUnitTest
```

Expected: both green. The TimelineItem `inside` field is `PlacementInsideView? = null`; the JSON `null` decodes to a Kotlin `null`. The TimelineItem `source` field is non-nullable, so non-null `kind`/`detail` is required.

**Step 5:** Commit:

```bash
git add app/src/main/java/app/tastile/android/data/api/V1Models.kt
git commit -m "fix(v1): align Span/InsideView/SourceView to Core wire shape"
```

### Task 11: Update the TileRepository consumers

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/repository/TileRepository.kt`

Three lines read fields that no longer exist:

- Line 382: `span.startAt` → `span.start`
- Line 383: `span.endAt ?: span.startAt` → `span.end ?: span.start`
- Line 388: `source.value.toInt()` → `source.kind.toInt()`

**Step 1:** Apply the three edits. The block should become:

```kotlin
val startInstant = parseIsoInstant(span.start) ?: return null
val endInstant = parseIsoInstant(span.end ?: span.start)
if (startInstant.isBefore(rangeStart) || !startInstant.isBefore(rangeEnd)) return null
return CoreTimelineItem(
    id = placementId,
    tileId = tileId,
    sourceKind = source.kind.toInt(),
    title = content.title.ifBlank { "Untitled" },
    type = role.toRoleName(),
    ...
```

**Step 2:** Run the full unit-test suite:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all green. No new DTO tests are required here because `TimelineItem` → `CoreTimelineItem` is exercised end-to-end by the on-device run.

**Step 3:** Commit:

```bash
git add app/src/main/java/app/tastile/android/data/repository/TileRepository.kt
git commit -m "fix(v1): TileRepository reads Core Span/source field names"
```

### Task 12: Rebuild and reinstall

Run:

```bash
./gradlew :app:assembleDebug
MSYS_NO_PATHCONV=1 adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/app-debug.apk
adb shell pm install --user 0 -r /data/local/tmp/app-debug.apk
adb shell rm /data/local/tmp/app-debug.apk
```

(Treat this as a continuation of Task 4 — same APK + install flow.)

### Task 13: Verify timeline renders on device

**Files:** none (live verification; continuation of Task 5).

**Step 1:** Clear logcat, start the app, log in.

```bash
adb logcat -c
adb shell am start -n app.tastile.android/.MainActivity
```

**Step 2:** Confirm the previously failing decoders succeed:

```bash
adb logcat -d V:* | grep -E "V1ApiClient|TileRepository|toCoreTimelineItem" | head -80
```

Expected: no `MissingFieldException` for `Span`, no `MissingFieldException` for `PlacementInsideView`, no `MissingFieldException` for `PlacementSourceView`. A trace is acceptable showing the timeline was decoded with `count=N` for `N > 0`.

**Step 3:** Visual confirmation.

Open the dashboard timeline. Confirm:
- Time-grid blocks render (not empty)
- Tile cards render (not empty)
- No `[0].span` MissingFieldException in logcat

If the timeline is still empty, capture another logcat window and re-check the failure chain — there may be additional downstream decoder mismatches (e.g. `ResolutionViolation` if any non-empty `violations[]` appears, or `TimelineOwnerEmbed` if `owner` is non-null). For the default `休憩` seed, neither should be in the response.

**Step 4:** Capture local-only evidence (do not commit screenshots, raw logcat, or tokens):

```bash
adb shell screencap -p /sdcard/verify.png
adb pull /sdcard/verify.png "$TEMP/verify-2026-07-23-timeline-render.png"
adb logcat -d | grep -E "V1ApiClient|TileRepository" > "$TEMP/verify-2026-07-23-timeline-render.log"
```

**Step 5:** Strip diagnostic logs and rebuild once.

Remove the temporary `Log.i` / `Log.w` lines added in `AuthRepository.kt` and `TileRepository.kt` during the diagnostic pass. Run:

```bash
./gradlew :app:assembleDebug
MSYS_NO_PATHCONV=1 adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/app-debug.apk
adb shell pm install --user 0 -r /data/local/tmp/app-debug.apk
adb shell rm /data/local/tmp/app-debug.apk
```

Re-confirm the timeline renders against the clean APK. Commit the cleanup:

```bash
git add app/src/main/java/app/tastile/android/data/repository/TileRepository.kt \
  app/src/main/java/app/tastile/android/data/repository/AuthRepository.kt
git commit -m "chore(v1): strip diagnostic logs after timeline render verified"
```

### Task 6: Add a failing HTTP contract regression test

**Files:**
- Modify: `app/src/test/java/app/tastile/android/data/api/V1ApiClientTest.kt`

**Step 1:** Add a test backed by JDK `HttpServer` that returns a representative production `TileListView` bare array from `/v1/tiles`, calls `V1ApiClient.getTiles()`, and asserts that the single tile is available through `response.tiles` while both next-actionable fields are null.

**Step 2:** Run only the new test with JDK 17.

```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.api.V1ApiClientTest.getTiles decodes Core bare array"
```

Expected before the implementation: FAIL with `Expected start of the object '{', but had '[' instead at path: $`.

### Task 7: Decode the canonical array and retain the internal wrapper

**Files:**
- Modify: `app/src/main/java/app/tastile/android/data/api/V1ApiClient.kt`

Change `getTiles` so the selected path is decoded as `List<TileListView>`, then return `V1ListTilesResponse(tiles = tiles)`. Do not change Core, `V1ListTilesResponse`, repository interfaces, or unrelated next-actionable UI state.

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "app.tastile.android.data.api.V1ApiClientTest"
./gradlew :app:testDebugUnitTest
```

Expected: both commands pass. Commit the test and implementation together:

```bash
git add app/src/main/java/app/tastile/android/data/api/V1ApiClient.kt \
  app/src/test/java/app/tastile/android/data/api/V1ApiClientTest.kt
git commit -m "fix(v1): decode tile list bare array response"
```

### Task 8: Rebuild and repeat device verification

Repeat Tasks 3–5 with a fresh APK. The diagnostic pass must show no `$.scopes` failure and no `Expected start of the object` failure. Confirm non-empty tile cards and timeline blocks visually. After confirmation, remove only the temporary diagnostic logging changes, rebuild/reinstall once more, and repeat the visual check against the clean APK.

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
- [ ] Task 6 HTTP contract test fails against object-envelope decoding, passes after Task 7
- [ ] Full `:app:testDebugUnitTest` green
- [ ] New APK installed on device
- [ ] On-device: `mintApiToken failed` absent from logcat
- [ ] On-device: no bare-array/object-envelope decode failure for `/v1/tiles`
- [ ] On-device: tile count > 0 in logcat (`source=v1`, not `v1_unavailable`)
- [ ] On-device: tiles list and timeline grid render non-empty
- [ ] Clean APK without temporary diagnostic logs is reinstalled and still renders
- [ ] Local-only verification screenshot + sanitized log inspected (not committed)
- [ ] No `tastile-core` source or HARNESS change; the canonical Core wire contract is unchanged
