# UX Fix: Projects drawer entry now shows a real projects page

Investigation: `docs/ux-investigation-tasks-projects.md` (Priority recommendation
#3 — P1). Replaces the broken "Projects" drawer entry, which was mounting
`TilesScreen` and labelling itself "Projects" while rendering a tile list with
a "Tiles" header.

## Files changed (absolute paths)

| File | Lines | Change |
| --- | --- | --- |
| `app/src/main/java/app/tastile/android/ui/mobile/tabs/ProjectsScreen.kt` | new file | New screen composable: `ProjectsSectionContent` (revived) + filtered tile list below |
| `app/src/main/java/app/tastile/android/ui/mobile/tabs/tiles/TileCard.kt` | 207-220 | Promote `TileLifecycle.glyph()` (was `private`) to a public top-level `glyphChar()` so `ProjectsScreen` can reuse the same lifecycle character map |
| `app/src/main/java/app/tastile/android/ui/mobile/MobileScaffold.kt` | 67, 232-236 | Add `ProjectsScreen` import; route `composable("tiles")` to `ProjectsScreen` instead of `TilesScreen` |
| `app/src/main/res/values/strings.xml` | 58-59 | Add `panels_projects_tiles_section_title` and `panels_projects_filtered_tile_count` |

## New ProjectsScreen routing

`composable("tiles") { ... }` in `MobileScaffold.kt:232-236` now mounts:

```kotlin
ProjectsScreen(viewModel = dashboardViewModel, overlay = overlayViewModel)
```

`ProjectsScreen` (`tabs/ProjectsScreen.kt:65`) is composed of:

1. **`ProjectsSectionContent`** — the previously orphaned panel from
   `ui/mobile/panels/ProjectsSectionContent.kt`. This panel already renders:
   - "Projects" header + "+ New" button
   - Inline create form (name / slug / color / parent)
   - "All Projects" + workspace tree rows (parent-before-child)
   - Long-press × for delete, edit dialog with `ProjectEditForm`
   - Delete confirmation `AlertDialog`
   Wiring at `ProjectsSectionContent.kt:138-140` already routes row selection
   through `dashboardViewModel.setOwnerFilter(id)`, so picking a workspace
   scopes `DashboardViewModel.tileFilter.ownerIds` for the list below.

2. **`HorizontalDivider`** + header row showing the scope label
   ("All Projects" or "N tiles").

3. **Filtered tile list** — renders `DashboardViewModel.tiles` (already
   scoped by the v1 `owner_ids` filter applied in step 1). Each row uses
   `NiaListItem` (the design system wrapper) with a lifecycle glyph and
   trailing chevron. Tapping a row calls `viewModel.selectTile(tile.id)` and
   `overlay.show(Overlay.TileEdit(tile.id))` — identical to the `TilesScreen`
   handler at `TilesScreen.kt:107-108`. The edit sheet contract is preserved.

`ProjectsViewModel` is hoisted as a default `hiltViewModel()` parameter on
`ProjectsScreen` and forwarded into `ProjectsSectionContent` so the panel's
default `hiltViewModel()` instance is the same one the screen reads
`selectedOwnerId` from. This keeps the panel's "All Projects / rows / edit /
delete" state and the screen's filter header label in lockstep.

`viewModel.tiles` is reused (not re-fetched) — the scope change propagates
through `DashboardViewModel.setOwnerFilter → tileFilter → reloadVisibleTilesAndExecutionControls`
(`DashboardViewModel.kt:149-152`, `:777`) which already exists.

## Out-of-scope items preserved

- `V1ApiClient`, `TileRepository`, `WorkspaceRepository`, `DashboardViewModel`'s
  data shape: untouched.
- `TilesScreen.kt` itself: untouched. The unit test at
  `app/src/test/java/app/tastile/android/ui/mobile/tabs/TilesScreenTest.kt`
  still exercises `TilesScreen` directly and is unaffected by the routing
  change. (The file is still reachable from any code that wants to mount it,
  e.g. future debug surfaces.)
- `SectionPanelContent.kt:195-198` (the second `ProjectsSectionContent` symbol
  shim): left in place. The side-panel sheet that mounted it is still
  unreachable from the running app — removal is tracked in the investigation's
  Fix 7 (P2) and is independent of this fix.

## Verification

- `verifyDesignSystemImports`: every M3 import in the new file is preceded by
  a `// m2-allow:` marker (`ProjectsScreen.kt:11, 13, 15`). No M3 imports
  were added to `MobileScaffold.kt`.
- `verifyNoEmbeddedServerSecrets`: not relevant (no credentials touched).
- Compile verification: `./gradlew :app:compileDebugKotlin` could not run in
  this environment because the Android SDK location is not configured
  (`local.properties` is intentionally not committed and `ANDROID_HOME` is not
  exported). All new imports have been manually cross-checked against
  existing call sites in `Tabs/TilesScreen.kt`, `Tabs/ExecuteScreen.kt`,
  `core/designsystem/component/ListItem.kt`, and `ui/mobile/components/AppEmptyState.kt`.

## Caveats / things punted

- The filtered tile list under the project editor uses a simple compact
  glyph + title + lifecycle row rather than the grouped `CompactTileCard` /
  `ComfortableTileCard` / `DetailedTileCard` density dispatch used by
  `TilesScreen`. The investigation called this out as out-of-scope (do not
  redesign the project editor) and the new screen is a minimal port to make
  the project → tile relationship visible at a glance. Density control can
  be added later by reusing `tileCard(...)` from
  `ui/mobile/tabs/tiles/TileCard.kt:193`.
- The "tile count per project" badge called out as Fix 4 in the investigation
  is not implemented. The header row shows a total scope count only;
  per-project counts would need a new projection in `ProjectsViewModel.state`.
- The v1 `view_mode="by_state"` default swap from Fix 6 (P2) is untouched.
  The filtered tile list still uses the default `view_mode="list"` so the
  default `by_state` server bucket is not yet surfaced.
- Long-press on the project row continues to use the `combinedClickable` reveal
  already implemented in `ProjectRow.kt:62-65`. No change.
- `local.properties` is intentionally absent from the working tree; the
  compile verification gate will run on CI where the SDK is provisioned.
