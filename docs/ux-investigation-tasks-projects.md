# UX Investigation: Tasks (Tile Edit Panel), Tasks list scroll, Projects screen

Read-only gap analysis. No source files modified. All paths absolute.

Repo map at-a-glance: The bottom-nav drawer (`SidePanelDrawerContent.kt`) labels
four routes, but only three are visible to the user as top-level destinations:

- `timeline`     → `TimelineScreen`
- `execute`      → `ExecuteScreen` (route label = **"Tasks"** in `MobileScaffold.kt:153`)
- `tiles`        → `TilesScreen`     (route label = **"Projects"** in `MobileScaffold.kt:154`)
- `integrations` → `IntegrationsScreen`

So when the user says "Tasks", they mean either the drawer route "Tasks" → `ExecuteScreen.kt`,
or the drawer route "Projects" → `TilesScreen.kt`. The "edit panel when a task is tapped"
is the same overlay sheet (`TileEditSheet`) for both routes, because both list rows call
`overlay.show(Overlay.TileEdit(tile.id))`. The user-visible confusion is reasonable;
both list screens have the same edit panel.

There is also a "ProjectsSectionContent" living inside the now-retired side panel
(`ui/mobile/sheets/SectionPanelContent.kt` still imports it, but
`MobileScaffold.kt:34` says "Phase 1: SidePanelSheet removed — primary nav now lives
in the ModalNavigationDrawer"). So there are TWO project UIs in the tree: the
drawer-mounted "Projects" tab (`TilesScreen`) and the orphaned drawer-pane
`ProjectsSectionContent` (which only renders if something still calls it).

---

## Section 1: Tasks screen current state

### What "Tasks" maps to

- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\MobileScaffold.kt:153`
  declares `"execute" -> "Tasks"`. So `MobileScaffold.kt:226-230` mounts `ExecuteScreen.kt`
  for the Tasks drawer entry.
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\tabs\ExecuteScreen.kt`
  - `ExecuteScreen` body at lines 101-164
  - `active` hero card (lines 108-117)
  - `showable` (line 97: filters out DONE tiles only)
  - "Today and ready" header at line 119-124
  - Empty-state column at lines 126-141 — calls out "create a tile" / "No tiles for today"
  - Tile row list at lines 142-163 — `TileActionRow` for each non-DONE non-ACTIVE tile
  - Tapping a row calls `viewModel.selectTile(tile.id)` + `overlay.show(Overlay.TileEdit(tile.id))` (lines 146-149)

### Why the user can't scroll

The list rendering is in a `Column` at `ExecuteScreen.kt:101` (`Modifier.fillMaxWidth().padding(MobSpacingSm)`)
that contains a non-scrollable inner `showable.forEach { tile -> TileActionRow(...) }` (line 143).
There is no `Modifier.verticalScroll(rememberScrollState())` on the column, and the parent
`MobileScaffold.kt:227` wraps the screen in `Box(modifier = Modifier.padding(top = topPad))`
with no scroll fallback.

Concrete impact:
- `ExecuteScreen.kt:101` — `Column(modifier = Modifier.fillMaxWidth().padding(MobSpacingSm))`
  no scroll modifier
- `ExecuteScreen.kt:143` — `showable.forEach { tile -> TileActionRow(...) }` inside that
  non-scrollable Column
- Compare to the v2 web equivalent in
  `C:\Users\rebui\Desktop\tastile\tastile-web\src\app\dashboard\tasks\tasks-page-client.tsx:15`
  which wraps `<TasksMain />` in `<div className="h-full overflow-y-auto">`

Compare with the **`TilesScreen`** ("Projects") route:
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\tabs\TilesScreen.kt:179`
  applies `Modifier.verticalScroll(rememberScrollState())` to the list body. So the
  Projects tab scrolls correctly while the Tasks tab does not.

This is the same `DashboardViewModel.tiles` list, rendered differently per route, and
only the "Projects" path added scroll plumbing.

---

## Section 2: Task edit panel — UUID-only root cause

### What is shown

`C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\sheets\TileEditSheet.kt`
is the only sheet that mounts when a tile row is tapped. Rendered body (lines 71-167):

- Header `Text(tile?.title ?: "Tile", style = titleLarge)` (line 75-78)
- Lifecycle label (line 80)
- `tile_occurrence_label` formatted with `placementId` (line 89) — this is the only
  place a UUID-like value is rendered for the user. Example: `"Occurrence: 4d2f9e3a-..."`.
  In `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\res\values\strings.xml:147`
  it's `Occurrence: %1$s`.
- Editable Title field with save button (lines 97-109) — only field that updates anything.
- Lifecycle-conditional Start / Defer / Request prompt / Complete / Pause / Resume / Finish buttons (lines 110-152).
- Delete / Delete-occurrence button (lines 154-165).

That's it. No description, no project, no schedule, no labels, no next-action text, no done-definition.

### Why "UUID only"

The user's complaint that the edit panel "just shows a UUID" almost certainly refers
to **`TileEditSheet.kt:87-93`**, which prints the placement UUID via:

```kotlin
(current as Overlay.TileEdit).placementId?.let { placementId ->
    Text(
        text = stringResource(R.string.tile_occurrence_label, placementId),
        ...
    )
}
```

When `Overlay.TileEdit(tileId, placementId)` is dispatched from a *calendar*
placement (not from the list rows), `placementId != null` and the only meaningful
identifying label is the UUID. From the list rows (`TilesScreen.kt:108` and
`ExecuteScreen.kt:148`) `placementId` is null so the line is skipped — the title
renders as expected. So:

- Tapping a row on the **Tasks** list (`ExecuteScreen`) → opens the sheet with
  `placementId=null` → shows the title.
- Tapping a row on the **Projects/Tiles** list (`TilesScreen`) → same, shows the title.
- Tapping an occurrence from the **calendar** → `placementId="<uuid>"` → the only
  identifying label printed is the placement UUID, with no title, lifecycle, or other
  detail. THIS is the "UUID-only" the user sees.

### Root cause: detail payload is not fetched

The deeper issue is that **`TileEditSheet.kt` only consumes the `Tile` cached in
`DashboardViewModel.tiles`** — it never calls `V1ApiClient.readSourceTile(id)` to
load the v1 `SourceTileDetailRead`. The full detail payload (which carries the
fields the user expects to see) is already defined and reachable:

- `C:\Users\rebui\Desktop\tastile\tastile-android\app\build\generated\openapi\v1\src\main\kotlin\app\tastile\android\data\api\generated\v1\models\SourceTileDetailRead.kt`
  - `source: SourceTileRead` (title, description, color, icon, plan_id, owner_id, schedule)
  - `occurrences: List<SourceOccurrenceRead>`
  - `placements: List<PlacementTileRead>`
  - `relations: List<RelationDefinitionRead>`
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\data\api\V1ApiClient.kt:251`
  exposes `suspend fun readSourceTile(sourceTileId: String): SourceTileDetailRead`
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\data\repository\TileRepository.kt`
  has no equivalent: `getTileById`/`getEditableTileById` only consult the in-memory
  `_tiles` list. **There is no `getTileDetail`/`getSourceTileDetail` call.**
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\build\generated\openapi\v1\src\main\kotlin\app\tastile\android\data\api\generated\v1\models\SourceTileRead.kt`
  - `createdAt`, `ownerId`, `planId`, `planRole`, `revision`, `schedule`,
    `sourceState`, `sourceTileId`, `title`, `updatedAt`, `color?`, `description?`,
    `externalId?`, `icon?`
- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\data\api\V1Models.kt:184-198`
  mirrors the same shape with `description`, `color`, `icon`, `externalId`,
  `schedule`, `createdAt`, `updatedAt`.

The edit panel therefore has no way to surface description, project, schedule,
labels, next-action, done-definition, color, or icon — they exist on the wire but
the panel never fetches them.

### What "edit / cancel / reschedule" controls are present vs missing

Present (in `TileEditSheet.kt`):
- Edit title (line 97-103) + Save (line 104-109)
- Lifecycle-conditional Start / Defer / Request prompt (lines 110-123)
- Complete (line 124-128)
- Pause / Resume / Start-execution / Finish-execution (lines 129-152)
- Delete / Delete-occurrence (lines 154-165)

Missing (the user explicitly asked about these):
- **Reschedule** — no field for `fixed_start` / `fixed_end` / `release_at` /
  `due_at` / `projected_next_start_at`. The v1 `SourceTileDetailRead.schedule`
  block (`SourceScheduleDefinitionSchema`) is never fetched. The web
  `TaskDetailSubPanel.tsx` and the v1 endpoint
  `POST /v1/tiles/{id}/reschedule` (not currently surfaced in
  `V1CommandPayloads.kt` search results) imply this is a v1 capability Android
  hasn't wired.
- **Cancel** — `CancelSourceTileRequest` exists in
  `app/build/generated/openapi/v1/src/.../CancelSourceTileRequest.kt` but no
  Android UI surfaces it.
- **Project / labels edit** — `tile.labels` is read-only on the Android side.
  No picker / chips.
- **Description** — `Tile.description` doesn't exist on the legacy v0
  `data/model/Tile.kt:21-59`. Adding it would be a model + mapper change.

---

## Section 3: Projects screen — current vs ideal

### What's wired up today

- The drawer route labeled "Projects" is `MobileScaffold.kt:154` (`"tiles"`) which
  mounts `TilesScreen.kt`. **TilesScreen is the "Projects" tab** even though the
  file name says "tiles" (see `TilesScreen.kt:53-61` doc comment: "Mobile Tiles tab.
  Mirrors web's `/dashboard/tiles` composition").
- `TilesScreen.kt:99-120` has three sub-tabs: `LIST`, `TIMELINE`, `CHANGES`.
  Default tab is `LIST` (`DashboardViewModel.kt:267` — `_activeTilesTab = TilesTab.LIST`).
  The list is grouped by `ListGroupingMode.STATE`/`PROJECT`/`TAG`
  (`DashboardViewModel.kt:282-286` + `TilesFilterBar.kt:298-300`).
- `TilesScreen.kt:79` shows the screen header "Tiles" (`R.string.dashboard_tiles_title`).
  So inside the route labeled "Projects" the user sees the title "Tiles" on first paint
  and the list is grouped by `STATE` by default.
- The orphaned `ProjectsSectionContent` (`ui/mobile/panels/ProjectsSectionContent.kt:1-202`)
  is still wired in
  `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\sheets\SectionPanelContent.kt:195-198`
  but the side-panel sheet that calls it is removed (`MobileScaffold.kt:34` comment
  "SidePanelSheet removed — primary nav now lives in the ModalNavigationDrawer").

### What it shows per row

- `C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\ui\mobile\panels\ProjectRow.kt:68-83`
  renders ONLY `workspace.displayName` and a folder icon. No color chip, no slug,
  no tile count, no created date.
- `parseHexColor` is defined at `ProjectRow.kt:119-127` but **never called** —
  the workspace has a `color` field that the row never paints.

### What v1 offers for "Projects"

- `GET /v1/access/subjects?kind=1` — `WorkspaceRepository.list()` returns a flat
  list (`V1Models.kt:273-285`):
  `id, kind, display_name, slug?, email?, parent_subject_id?, color?, owner_user_id?, disabled_at?, created_at?, updated_at?`
- The web side
  (`C:\Users\rebui\Desktop\tastile\tastile-web\src\features\manage-projects\ui\ProjectsMain.tsx:33-39`)
  renders a real edit form (name, slug, color picker, tile count, created date,
  Save) above a list of tiles in the project. Android never renders this
  full-page edit form because `ProjectsMain.tsx` is web-only.

### "ownerScopes" / "by_state" — what the v1 API offers

- `GET /v1/tiles?owner_ids=...&view_mode=by_state&range=7d&granularity=no_breaks,min_0m&...`
  per `app/openapi/v1.json:487-575`. This endpoint exists and supports both
  `view_mode=by_state` (group tiles by lifecycle, the web default) and
  `view_mode=flat`. The default for Android
  `DashboardViewModel.kt:538` (`viewMode = "list"`) is **not** `by_state` —
  so the Projects tab never gets the lifecycle-bucketed view web shows by default.
- `TilesScreen.kt` has its own custom `ListGroupingMode.STATE/PROJECT/TAG`
  segmentation that runs locally on whatever the list returns. It's a client-side
  bucket, not the server's `by_state` projection.
- `TileFilter.kt:43` already serializes `owner_ids` correctly — but no caller
  wires it from a real user-selected workspace in `TilesScreen.kt`. The
  `ProjectsViewModel.selectOwner` callback does call
  `dashboardViewModel.setOwnerFilter(id)` (`ProjectsSectionContent.kt:138-140`),
  but `ProjectsSectionContent` is in the orphaned panel.

### Where the existing Projects UI gets messy

- The drawer mounts "Projects" → `TilesScreen`. `TilesScreen` opens with sub-tab
  LIST, mode COMPACT/STATE-grouped, default `view_mode="list"` (server default),
  showing a header that says "Tiles". So users see:
  - Top bar: "Projects" (from `MobileScaffold.kt:154`)
  - Screen header: "Tiles" (`R.string.dashboard_tiles_title`)
  - Pill row: LIST / TIMELINE / CHANGES
  - Filter row: 6 controls (search, range, granularity, limit, grouping, view mode)
  - Stat chips: "Open · N" + "Estimated N*30" + "Sections N"
  - Then sections of tiles grouped by lifecycle (READY/STARTED/DONE/ARCHIVED)
- The `ProjectsSectionContent` panel would render a tidy "All Projects + N rows"
  list at 1:1 with web, but it lives behind a removed side-sheet.

---

## Section 4: v1 source-tile fields available (exhaustive)

From `app/build/generated/openapi/v1/.../SourceTileRead.kt` and
`SourceTileDetailRead.kt` and the locally-mirrored
`C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\java\app\tastile\android\data\api\V1Models.kt:184-198`:

`SourceTileRead`:
- `sourceTileId: UUID` (required)
- `title: String` (required)
- `description: String?`
- `color: String?`
- `icon: String?`
- `externalId: String?`
- `ownerId: UUID` (required)
- `planId: UUID` (required)
- `planRole: Int` (required)
- `schedule: SourceScheduleDefinitionSchema` (required)
- `sourceState: Int` (required; 0 ACTIVE .. 3 CANCELLED)
- `revision: Long` (required)
- `createdAt: OffsetDateTime` (required)
- `updatedAt: OffsetDateTime` (required)

`SourceTileDetailRead` (the response of `GET /v1/source-tiles/{id}`):
- `source: SourceTileRead`
- `occurrences: List<SourceOccurrenceRead>`
  - `occurrenceId, sourceTileId, sequenceNo, nominalAt, windowStart, windowEnd, requiredDurationMs, state, revision`
- `placements: List<PlacementTileRead>`
  - `placementId, sourceTileId, occurrenceId, splitIndex/splitCount, splitGroupId, start, end, closed, closedAt?, revision`
- `relations: List<RelationDefinitionRead>`

`SourceTileSummary` (returned inline in `TileListView.source`):
- `generationKind, priority, requiredDurationMs, sourceState, splitKind, windowStart/EndOffsetMs, color?, externalId?, icon?, weekdayMask?`

`TileListView` (list endpoint payload):
- `breakMinutes, id: UUID, labels: List<String>, lifecycle: Int, objectiveMode: Int, title, workedMinutes`
- `doneDefinition?, doneRule?, nextAction?, planId?, projectedNextStartAt?, recurrence?, resumeNote?, source: SourceTileSummary?, targetRestMin?, targetWorkMin?, temporal: TemporalView?`

Commands / write payload:
- `CreateSourceTilePayloadSchema`, `UpdateSourceTilePayloadSchema`
- `CancelSourceTileRequest` / `CancelSourceTilePayloadSchema` (CANCEL operation exists)
- `ReflowSourceTileRequest` / `ReflowSourceTilePayloadSchema` (reschedule / reflow)

---

## Section 5: Web equivalent (what works there)

### Tasks list

- `C:\Users\rebui\Desktop\tastile\tastile-web\src\features\manage-tasks\ui\TasksMain.tsx`
  - Uses `useTileList({ viewMode: "by_state", limit: 200, range, granularity, search })`
  - Wrapped in `tasks-page-client.tsx:15` with `h-full overflow-y-auto` (the
    missing scroll plumbing on Android Tasks)
  - Renders each row via `TileCardCompact` with status icon, title, label badges,
    source-kind chip (BREAK/SLEEP/legacy), duration chip, friendly start date,
    inline Edit pencil button
- The Edit pencil calls `loadFromRecurringTile(id)` which:
  - Opens the QuickCreate store in edit mode
  - Calls `getCoreClient().call("getTile", { pathParams: { id: tileId } })`
    (i.e. fetches the **full tile detail**, not the list cache)
  - Surfaces hydration failure as an in-panel banner (`editingError`)
  - Renders `TaskDetailSubPanel` with note textarea, order rules (BEFORE/AFTER),
    Remove/Apply/Cancel buttons, cycle detection

### Task edit detail (`TaskDetailSubPanel.tsx`)

- Note textarea (4 rows)
- Task-order list with target selector + BEFORE/AFTER segmented + Add/Remove
- Apply / Cancel / Remove

### Projects page

- `C:\Users\rebui\Desktop\tastile\tastile-web\src\features\manage-projects\ui\ProjectsMain.tsx`
  - Reads `?owner=` URL param → selects workspace
  - Shows full `ProjectEditForm` (name, slug, color picker, tile count, created date, Save)
  - Below the form: tiles in this project (uses `useTileList({ ownerIds, limit: 500 })`)
  - Header chip: `owner_id: <first 8 chars> · slug: <slug>`
- This is the canonical full-page projects UI Android does not have.

### Mobile parity side-panel

- `C:\Users\rebui\Desktop\tastile\tastile-web\src\components\panels\ProjectsSidePanel.tsx`
  - "All Projects" row + ordered tree (parent-before-child)
  - `+ New` inline form (name + slug + color + parent picker)
  - Long-press × to reveal Delete
  - This is mirrored by `ProjectsSectionContent.kt` on Android, but that
    screen is orphaned behind the removed side-panel sheet.

---

## Section 6: Priority recommendations

1. **P0 — Wire the tile detail fetch into `TileEditSheet` so the UUID is replaced
   with a useful title and metadata.** Root cause is that
   `TileRepository.getTileById` only consults the list cache and
   `readSourceTile` is never called. The fix is one repository method
   + one `StateFlow<SourceTileDetailRead?>` + a few Compose rows.

2. **P0 — Add scroll to the Tasks (`ExecuteScreen`) column.** One line: add
   `.verticalScroll(rememberScrollState())` to `ExecuteScreen.kt:101`. Without
   it, even a single tile with a long title can be clipped on small phones.

3. **P1 — Re-route the "Projects" drawer entry to a real projects page**, not
   `TilesScreen`. The web app's `ProjectsMain.tsx` shows an inline edit form +
   tile list scoped to `owner_id`. Android can port that pattern using the
   existing `WorkspaceRepository` + `TileFilter.ownerIds`.

4. **P1 — Render the workspace color chip + slug + tile count on each
   `ProjectRow`.** The `parseHexColor` helper already exists at
   `ProjectRow.kt:119-127` and the data is on `Workspace.color/slug/...`.

5. **P1 — Surface reschedule / cancel controls in `TileEditSheet`.** Wire
   `V1CommandPayloads.kt`-style commands to `CancelSourceTileRequest` and a
   v1 `reflow`/`reschedule` endpoint. Currently invisible to the user.

6. **P2 — Switch default `viewMode` to `"by_state"` in `DashboardViewModel`**
   (`DashboardViewModel.kt:538`) so the Projects tab matches web.

7. **P2 — Delete the orphaned `ProjectsSectionContent` import path**
   (`SectionPanelContent.kt:195-198`) or remount it as the Projects page. Dead
   code paths rot quickly.

---

## Section 7: Concrete file:line fixes

### Fix 1 (P0): Edit panel renders a UUID with no other info

- Root cause: `TileEditSheet.kt:87-93` prints only `placementId` (the UUID) when
  the sheet was opened from a calendar occurrence, because no detail payload is
  fetched.
- Fix path:
  - `TileRepository.kt:89-117` — add `suspend fun getTileDetail(id: String): SourceTileDetailRead?`
    that calls `v1ApiClient.readSourceTile(id)` and returns null on 404 / V1Error.
  - `DashboardViewModel.kt:154-166` — extend `selectedTile` to combine the cached
    `Tile` with a freshly fetched `SourceTileDetailRead?` and expose it as
    `selectedTileDetail: StateFlow<SourceTileDetailRead?>`.
  - `TileEditSheet.kt:71-167` — when `current is Overlay.TileEdit` and
    `tile == null` BUT the detail is non-null, fall back to `detail.source.title`;
    render `description`, `color`, `icon`, lifecycle, project label, plan id, and
    scheduled window from `detail.source.schedule` and `detail.placements.firstOrNull()`.
  - `TileEditSheet.kt:88` — replace the UUID-only Text with
    `tile?.title ?: detail?.source?.title ?: "Tile"` (and only print the placement
    id as a debug subtitle).

### Fix 2 (P0): Tasks list cannot scroll

- `ExecuteScreen.kt:101` — wrap the column in `.verticalScroll(rememberScrollState())`:
  ```kotlin
  Column(
      modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(MobSpacingSm)
  ) {
  ```
- Import `androidx.compose.foundation.rememberScrollState` and
  `androidx.compose.foundation.verticalScroll`.
- Mirror web's `tasks-page-client.tsx:15` (`overflow-y-auto`).

### Fix 3 (P1): "Projects" drawer entry shows tile list, not projects

- `MobileScaffold.kt:231-235` — change `composable("tiles") { ... TilesScreen ... }`
  to mount a new `ProjectsScreen` composable (port `ProjectsMain.tsx` from web).
- New file `app/src/main/java/app/tastile/android/ui/mobile/tabs/ProjectsScreen.kt`:
  - Uses `ProjectsViewModel` for workspace list (already exists)
  - Uses `DashboardViewModel.setOwnerFilter` + `TileFilter.copy(ownerIds = listOf(id))`
    to scope tiles to the selected workspace (already wired through
    `setOwnerFilters` / `TileFilter.toQueryParameters`)
  - Renders `ProjectEditForm` (already exists in
    `ui/mobile/panels/projects/ProjectEditForm.kt:41-106`) inline
  - Below it, a list of `TileCardCompact`-equivalent rows
- Deprecate or remove the orphaned `ProjectsSectionContent` import in
  `SectionPanelContent.kt:195-198` once `MobileScaffold.kt:34` confirms the side
  panel is gone.

### Fix 4 (P1): Project rows don't show color / slug / tile count

- `ProjectRow.kt:68-83` — extend the `ListItem` `content` to:
  ```kotlin
  Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
          parseHexColor(workspace.color)?.let { color ->
              Box(Modifier.size(10.dp).background(color, CircleShape))
          }
          Text(workspace.displayName, ...)
      }
      workspace.slug?.let { slug ->
          Text(slug, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
      }
      Text("$tileCount tiles", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
  }
  ```
- Lift `tileCount` from `ProjectsViewModel.state` (new field keyed by workspace id,
  computed from a `DashboardViewModel.tiles` query scoped to that workspace).

### Fix 5 (P1): Reschedule / cancel controls

- `TileEditSheet.kt:154-165` — add buttons next to "Delete":
  - "Reschedule" → opens a date-picker dialog (Material3 `DatePicker` already used
    in `MobileScaffold.kt:271-309`) → dispatch a `ReflowSourceTileRequest`.
  - "Cancel" → calls `CancelSourceTileRequest` (already a generated model in
    `app/build/generated/openapi/v1/.../CancelSourceTileRequest.kt`).
- New methods on `TileRepository` and `V1CommandDispatcher` are needed; the wire
  shape already exists.

### Fix 6 (P2): Default Projects view mode

- `DashboardViewModel.kt:538` — change `viewMode = "list"` to
  `viewMode = if (grouping == ListGroupingMode.STATE) "by_state" else "list"`
  so the server-side lifecycle bucket matches web's default.

### Fix 7 (P2): Remove orphaned side-panel code

- `SectionPanelContent.kt:62` and `:195-198` — drop the `SidePanelSection.Projects`
  branch once `MobileScaffold.kt:34` confirms the side sheet is gone.
- Or, if `ProjectsSectionContent` is to be promoted to the new Projects tab
  (Fix 3), migrate it before deleting the orphaned import.

---

## Appendix — file paths

Android: `MobileScaffold.kt`, `SidePanelDrawerContent.kt`, `OverlayLayer.kt`, `OverlayState.kt`,
`tabs/ExecuteScreen.kt`, `tabs/TilesScreen.kt`, `tabs/tiles/TilesFilterBar.kt`, `tabs/tiles/TilesSectionColumn.kt`,
`tabs/tiles/TileCard.kt`, `tabs/tiles/TilesTabSwitcher.kt`, `sheets/TileEditSheet.kt`, `sheets/SectionPanelContent.kt`,
`panels/ProjectsSectionContent.kt`, `panels/ProjectsViewModel.kt`, `panels/projects/{ProjectsList,ProjectRow,NewProjectForm,ProjectEditForm}.kt`,
`dashboard/DashboardViewModel.kt`, `data/api/V1ApiClient.kt`, `data/api/V1Mappers.kt`, `data/api/V1Models.kt`,
`data/repository/{TileRepository,TileFilter,WorkspaceRepository}.kt`, `data/model/{Tile,TileConditionsExt}.kt`,
`res/values/strings.xml:147` (tile_occurrence_label).

Generated v1 (under `app/build/generated/openapi/v1/.../models/`):
`SourceTileRead.kt`, `SourceTileDetailRead.kt`, `SourceTileSummary.kt`, `TileListView.kt`,
`CancelSourceTileRequest.kt`, `ReflowSourceTileRequest.kt`.

Web (for parity comparison): `tastile-web/src/app/dashboard/{tasks,projects}/{tasks,projects}-page-client.tsx`,
`features/manage-tasks/ui/TasksMain.tsx`, `features/manage-projects/ui/ProjectsMain.tsx`,
`features/create-tile/ui/TaskDetailSubPanel.tsx`, `shared/stores/quick-create-store.ts`,
`tile/ui/TileCardCompact.tsx`.
