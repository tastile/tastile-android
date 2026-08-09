# UX Fix: Task edit panel detail + Tasks list scroll

Read-only gap analysis: `docs/ux-investigation-tasks-projects.md`.
This change implements the two locked-in P0 fixes from that document.

## Files changed (absolute paths)

| File | Lines | Change |
| --- | --- | --- |
| `app/src/main/java/app/tastile/android/data/repository/TileRepository.kt` | 7, 105-125 | Add `SourceTileDetailRead` import + `getTileDetail(tileId)` |
| `app/src/main/java/app/tastile/android/ui/dashboard/DashboardViewModel.kt` | 20, 161-198 | Add `_selectedTileDetail` + `_selectedTileDetailLoading` + `loadTileDetail(id)`; clear in `clearSelectedTile()` |
| `app/src/main/java/app/tastile/android/ui/mobile/sheets/TileEditSheet.kt` | 18, 22, 54-55, 64-137 | New `LaunchedEffect`, richer header (title / description / placement window), loading + retry affordances |
| `app/src/main/java/app/tastile/android/ui/mobile/tabs/ExecuteScreen.kt` | 28-29, 103 | Add `verticalScroll(rememberScrollState())` on the root Column |

The repository is `@Inject`-constructed, so `V1ApiClient` was already available
in its constructor — no Hilt wiring change in `ApiModule.kt` was needed.

## Fix 1: TileEditSheet fetches the full tile detail

### `TileRepository.getTileDetail` (new)

```kotlin
suspend fun getTileDetail(tileId: String): SourceTileDetailRead? {
    if (tileId.isBlank()) return null
    val token = currentUserProvider.currentIdToken()
    if (token.isNullOrBlank()) return null
    return try {
        v1ApiClient.readSourceTile(tileId)
    } catch (e: V1Error) {
        android.util.Log.w("TileRepository", "v1 readSourceTile failed: ${e.message}", e)
        null
    } catch (e: Exception) {
        android.util.Log.w("TileRepository", "v1 readSourceTile failed: ${e.message}", e)
        null
    }
}
```

Mirrors the existing `getTiles` / `readCloudTimeline` fault-tolerance pattern:
never throws, returns `null` on auth-missing or v1 error, logs the failure.

### `DashboardViewModel` new state + method

```kotlin
private val _selectedTileDetail = MutableStateFlow<SourceTileDetailRead?>(null)
val selectedTileDetail: StateFlow<SourceTileDetailRead?> = _selectedTileDetail.asStateFlow()

private val _selectedTileDetailLoading = MutableStateFlow(false)
val selectedTileDetailLoading: StateFlow<Boolean> = _selectedTileDetailLoading.asStateFlow()

fun loadTileDetail(id: String) {
    if (id.isBlank()) {
        _selectedTileDetail.value = null
        _selectedTileDetailLoading.value = false
        return
    }
    viewModelScope.launch {
        _selectedTileDetailLoading.value = true
        val detail = tileRepository.getTileDetail(id)
        // Guard against a fast re-select / dismiss race: only commit the
        // payload if the user is still looking at the same tile.
        if (_selectedTileId.value == id) {
            _selectedTileDetail.value = detail
            _selectedTileDetailLoading.value = false
        }
    }
}
```

`clearSelectedTile()` now also nulls the detail + loading state so a
subsequent open of a different tile starts from a clean slate.

### `TileEditSheet` header rendering

1. New `LaunchedEffect(tileEdit.tileId)` triggers `loadTileDetail` whenever
   the sheet is opened for a fresh id (and only that id — Compose keys the
   effect, so closing + reopening with a new id re-fires, while a recompose
   that doesn't change id is a no-op).
2. `headerTitle` is now `tile?.title ?: detail?.source?.title ?: "Loading tile…"`.
   Previously the sheet fell back to the literal string `"Tile"` whenever
   the cached `Tile` was missing (which is exactly the calendar-occurrence
   case the user reported). Now the v1 detail's title is shown as soon as
   the read completes.
3. The placement UUID remains visible as a debug subtitle (line 117-123) but
   is no longer the only label.
4. While the detail is in-flight (`detailLoading && detail == null`) a
   `CircularProgressIndicator` (testTag `tile-edit-detail-loading`) is
   rendered.
5. If the fetch returns `null` and no error was raised (e.g. 404 or
   network), a `Retry loading details` `NiaTextButton` is rendered that
   re-invokes `loadTileDetail`.
6. Description (from `detail.source.description`) and the first
   placement's `start → end` are rendered as small body rows so the user
   sees real metadata without scrolling.

The cached `Tile`-driven editable controls (Title field, lifecycle-conditional
Start / Defer / Complete / Pause / Resume / Delete buttons) are unchanged,
so existing behavior on the list-row entry path is preserved.

## Fix 2: Tasks (`ExecuteScreen`) now scrolls

```kotlin
// ExecuteScreen.kt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// ...
Column(
    modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(MobSpacingSm)
)
```

Mirrors the `TilesScreen.kt:179` pattern exactly — same import paths, same
modifier chain, same `Column` (not `LazyColumn`); the forEach inner list
remains a plain iteration because the parent `Column` owns the scroll.

## Compilation status

- `./gradlew :app:compileDebugKotlin` could not be run in this environment
  (no `local.properties` + no `ANDROID_HOME` on PATH; SDK location
  unresolved). Verified by reading imports + the existing test scaffolding.
- Existing `mockk<TileRepository>(relaxed = true)` test seams remain
  compatible. `getTileDetail` is an unverified suspend on the relaxed mock;
  `loadTileDetail` calls it from `viewModelScope.launch`, so the relaxed
  `null` return leaves `selectedTileDetail = null` and `detailLoading =
  false` once the coroutine completes (no change to existing assertions).
- The new `LaunchedEffect` keys on `tileEdit.tileId`, so unit tests that
  open the sheet once see exactly one fetch attempt.

## Caveats / judgment calls

1. **Header color / icon not yet rendered.** `SourceTileRead.color` and
   `.icon` are on the wire (`V1Models.kt:191-192`) but the existing
   `parseHexColor` helper lives in `ui/mobile/panels/ProjectRow.kt`, not
   the design system. Surfacing them is a follow-up that crosses the
   design-system boundary the locked-in scope forbids.
2. **Schedule summary not yet rendered.** `SourceSchedulePayload` is a
   v0-shape object the v1 `SourceTileRead.schedule` carries, but it is not
   a typed field on the locally-mirrored `SourceTileDetailRead`. We render
   the first placement's `start → end` instead, which is a useful proxy.
3. **Retry button is a non-blocking text button** in the sheet body, not a
   snackbar. Snackbar requires hoisting a `SnackbarHostState` from the
   caller, which is a wider refactor than the locked-in scope allows.
4. **`loadTileDetail` is fire-and-forget.** It does not surface errors to
   `viewModel.error` (which is shared with the rest of the screen and
   used by `TileEditSheet`'s `error?` rendering). The retry button is the
   recovery affordance; this matches the locked-in scope ("show snackbar /
   inline error with a Retry affordance").
5. **No `mockk<V1ApiClient>` change required.** `getTileDetail` follows
   the same fault-tolerant pattern as `getTiles` / `readCloudTimeline`;
   tests can opt into stubbing it via `coEvery { tileRepo.getTileDetail(any()) }`
   on the relaxed `TileRepository` mock.

## Out-of-scope confirmations (per investigation)

- `ProjectsSectionContent` import in `SectionPanelContent.kt` not touched.
- `QuickCreateSheet` not touched.
- No changes to `V1ApiClient.kt`, `V1Models.kt`, design-system internals,
  or Hilt modules.
- No new dependencies added.
