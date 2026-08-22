# Tastile Android — Web→Android Content/Control/API Parity Plan

Date: 2026-07-07
Owner: backend+android parity
Inputs: `2026-07-07-web-controls-api-audit.md`, `2026-07-07-android-controls-api-audit.md`

## 1. Background & Binding Constraint

`tastile-android` is the mobile companion to the same v1 domain (`tastile-core/v1/`)
that `tastile-web` consumes. Both clients are thin presentations over the v1 HTTP
API. The web audit captured 76 controls + 17 endpoints + ~190 i18n keys; the
android audit captured the opposite: many missing/stub surfaces, hardcoded
English strings, and a half-migrated v1 Tile model.

The user's directive (`2026-07-07`):

> **完全移植でいいです。翻訳要素・UIコントロールも完全に一致させてください。
> 変えていいのはモバイル用のパネル展開等の全体構成と、個々のコントロールUI
> (ボタンやドロップダウンなど)のコンポーネントのみです。
> 組み合わせは完全にweb準拠で一致させます**

Translation:
- Full port is fine. Translation elements (labels / i18n keys) and **UI control
  composition** (which controls appear, in what order) must match web.
- Allowed to vary: **overall mobile composition** (e.g. panel → BottomSheet
  expansion), and **individual control UI components**
  (e.g. a Compose `DropdownMenu` instead of HTML `<select>`).
- The **composition** — combinations of controls per surface — must follow web.

Two audits are now in place. This plan is the contract between them.

## 2. Cross-cutting Rules (binding)

| # | Rule | Reason |
|---|---|---|
| R1 | Control **count, order, and labels** per surface must match web verbatim. | User directive on composition. |
| R2 | i18n keys mirror web namespaced structure one-to-one; Android resource id is `R.string.` + dots replaced with underscores (`panels.calendar.day` → `R.string.panels_calendar_day`). | Same content, idiomatic Kotlin namespacing. |
| R3 | Mobile overall layout may use `BottomSheet`, `HorizontalPager`, `LazyColumn`, or a vertical section list where web uses a sidebar/sheet — that is the "panel expansion" variation. | Per user directive. |
| R4 | Individual control widgets may be Compose-native (e.g. `DropdownMenu` vs `<select>`, `Slider` vs `<input type="range">`, `Switch` vs `<input type="checkbox">`). | Per user directive. |
| R5 | API request/response shapes (`GET /v1/tiles`, `POST /v1/api-tokens`,
`/api/account/profile`, etc.) match web **wire shape** — not just REST path. | thin-client parity. |
| R6 | Lints/build/tests pass per chunk before next chunk starts. | Per project policy. |
| R7 | Drop stub surfaces and dead code as we go (no orphans). | Per project surgical-edit rule. |
| R8 | Existing v1 auth (`AuthRepository` + `ApiTokenManager` + `EncryptedTokenStorage`) stays untouched — it already matches web's Cognito+v1-token model. | R5. |

## 3. Naming Conventions

- i18n key namespacing: kebab-free, dots-only. Android `R.string.<namespace>_<sub>_<leaf>`.
- File layout under `app/src/main/java/app/tastile/android/ui/mobile/`:
  - `tabs/` — one composable per mobile tab (`TilesScreen.kt`, `ExecuteScreen.kt`,
    `IntegrationsScreen.kt`, `SettingsScreen.kt`).
  - `sheets/` — overlay/sheet composables
    (`PanelSheet.kt`, `SidePanelSheet.kt`, `SectionPanelContent.kt`,
    `AccountMenuSheet.kt`, `TilesFilterSheet.kt`, …).
  - `panels/` — content for each side-panel section (Calendar, Schedule,
    Projects, References, Preferences). New modules mirroring web panel dirs.
  - `account/` — Account sheet (preferences/account profile/tokens/subscription
    sub-sheets).
- ViewModel: one per surface (`TilesViewModel`, `CalendarSectionViewModel`,
  `ProjectsSectionViewModel`, `ReferencesSectionViewModel`,
  `PreferencesViewModel`, `AccountViewModel`); `DashboardViewModel` continues
  to coordinate shared state (`tiles`, `selectedTileId`, `email`, locale,
  themeMode, lock).
- Repository: existing names stay. New methods added; deprecated stubs removed.
- Tests: per-chunk unit / instrumentation test file names mirror SUT name
  (`TilesViewModelTest.kt`, `TileRepositoryListTest.kt`, …).

## 4. Chunk Specification (12 chunks)

### C1 — v1 Tile data model foundation

**Web source(s)**: `src/lib/utils/map-list-view-to-tile.ts`,
`src/lib/domain/tile.ts`, `src/lib/hooks/use-tile-list.ts`
(`TileListView` shape), `src/lib/i18n/translations.ts` lifecycle codes
under `tiles.lifecycle.*`.

**Android target file(s)**:
- **MODIFY** `data/api/V1Models.kt` — add `TileListView` data class matching
  the wire shape (id, plan_id, title, lifecycle i16, next_action,
  done_definition, worked_minutes, break_minutes, labels [], objective_mode,
  target_work_min, target_rest_min, done_rule, resume_note,
  projected_next_start_at, temporal { release_at, due_at, fixed_start,
  fixed_end, active_start, active_end }, recurrence { step_min,
  window_start_min, window_end_min, expression }).
- **MODIFY** `data/api/V1Mappers.kt` — implement
  `V1ListTilesResponse.toTiles(): List<TileListView>` and
  `TileListView.toTile(userId): Tile` carrying through **all** fields, plus
  add `TileListViewLifecycle` enum mapper
  (`lifecycle` i16 → enum matching web's
  `LIFECYCLE_BY_CODE`).
- **REPLACE** `data/model/Tile.kt` — new shape: domain `Tile` mirrors
  the web `Tile` shape (core, work, temporal, objective, interruption,
  automation, annotation), not the legacy `*Conditions` JSON. Lifecycle
  derivation follows web (`if completedAt != null => done; else if
  startedAt != null => started; else => ready`).
- **DELETE** `data/model/TileConditionsExt.kt` — replaced by direct field
  reads on the new `Tile`.
- **MODIFY** `data/api/V1NumericConstants.kt` — add the lifecycle,
  objective-mode, done-rule i16 registries imported from
  `tastile-core/v1/02-core-entities.md`.

**Composition**: none — pure data shape. Unblocks C3, C4, C6, C10, C11.
**i18n keys**: none.
**API contract**: restates the existing `GET /v1/tiles` response shape
(no request side change). Strip the unused user_id=stub in `V1Mappers`.
**Allowed mobile variations**: none (foundation).

**Verification**:
- Unit tests on `V1Mappers.toTile` for all 4 lifecycle codes + each
  numeric-code mapping (objective_mode, done_rule).
- `gradle :app:compileDebugKotlin :app:testDebugUnitTest` green.
- Existing `TileExtensions.isStarted()` keeps working via new `Tile.let {
  it.lifecycle == TileLifecycle.STARTED }`.
- Diff: `git diff --stat data/ | tee log` shows Net deletion in `Tile.kt`
  (new shape) and growth in `V1Models.kt` / `V1Mappers.kt`.

---

### C2 — Google Calendar removal

**Web source(s)**: `src/app/dashboard/integrations/page.tsx` (34 lines).

**Android target file(s)**:
- **MODIFY** `ui/mobile/tabs/IntegrationsScreen.kt` — replace body with the
  web static-notice: `<h2> "Google Calendar" </h2>` + two paragraphs
  explaining the integration is outside v1 scope. Drop the per-row
  `TextButton "Sync now"` / `TextButton "Disconnect"` controls and the
  `AlertDialog`.
- **DELETE** `ui/mobile/sheets/IntegrationConfigSheet.kt`.
- **DELETE** `data/repository/IntegrationRepository.kt` methods:
  - `getSettings`, `updateGoogleCalendarConnected`,
  - `markGoogleCalendarSyncedNow`, `updateGoogleCalendarIntegration`,
  - `getCalendarSyncPlanPreview`, `getCalendarMonthProjection`,
  - `triggerSync`, `triggerTick`, `resetLocalSyncData`,
  - `redownloadRemoteSyncData`, `getSyncStatus`, `getTileQuota`,
  - `restoreSession`, `startOAuth`, `signInWithOAuth`, `startBrowserAuth`,
  - `checkHealth`, `getEventsRaw`, `streamStateEvents`,
  - `isAuthenticated`, `getSession`.
  Keep `getRuntimePaths()` (delegates to `V1ApiClient.listRuntimePaths`).
  Keep the OAuth helpers used by `AuthRepository` (move them into
  `CognitoAuthStartUrlBuilder.kt`).
- **MODIFY** `data/model/Integration.kt` — reduce to one
  `IntegrationPlaceholder` model or delete entirely (depends on whether
  the integrations tab still needs a placeholder list — it does **not**;
  match web by deleting).
- **MODIFY** `ui/dashboard/DashboardViewModel.kt` — remove
  `integrations`, `googleCalendarIntegration`, `calendarMonthProjection`,
  `calendarSyncPlanPreview`, `syncGoogleCalendarNow`,
  `disconnectGoogleCalendar`, `connectGoogleCalendar`,
  `updateGoogleCalendarPolicy`, `triggerDaemonTick`,
  `resetLocalSyncData`, `redownloadRemoteSyncData`,
  `refreshDaemonStatus`. Drop `_googleCalendarIntegration` and friends.

**Composition**: web Integrations page has **zero controls**; android
Integrations tab must similarly drop its control set entirely (just a
read-only notice).
**i18n keys**: none.
**API contract**: deletes the legacy daemon endpoints
(`/auth/integrations/*`, `/sync/*`, `/views/calendar/*`,
`/auth/tile-quota`, `/health`, `/debug/events`,
`/auth/oauth/*`, `/auth/session/*`, `/commands/tick`). `V1ApiClient` path
(`/v1/runtime/paths`) is the only survivor.
**Allowed mobile variations**: `<h2>` may render as `Text(style=title)`;
paragraphs as `Text(bodyMedium)`.

**Verification**:
- `gradle :app:assembleDebug` green.
- `grep -rn "/auth/integrations\|/sync/trigger\|/commands/tick\|/auth/tile-quota\|/auth/session"` `app/src/main/` returns 0.
- Confirm remaining daemon-side call list:
  `grep -rn "executeDaemonRequest" app/src/main/` shows only the auth
  helpers used by `CognitoAuthStartUrlBuilder`.

---

### C3 — Tiles API contract correction

**Web source(s)**:
`src/lib/hooks/use-tile-list.ts` (HTTP signature + query params),
`src/lib/utils/map-list-view-to-tile.ts`.

**Android target file(s)**:
- **CREATE** `data/repository/TileRepository.kt` — overhaul:
  - `suspend fun getTiles(filter: TileFilter): TilesResponse` —
    composes URL `"$base/v1/tiles?view_mode=...&limit=...&search=...&range=...&granularity=...&exclude_future=...&owner_ids=..."`.
  - `TileFilter` data class with fields:
    `viewMode = "list"`, `lifecycle`, `limit = 20` (default matches web),
    `search`, `excludeFuture: Boolean`, `range`, `granularity`,
    `ownerIds: List<String>`. The composer's defaults match web
    initial state.
  - Return type `TilesResponse(tiles: List<Tile>,
    nextActionableTileId: String?,
    nextActionableStartAt: String?)`.
  - `suspend fun getTile(id: String): TileDetailView?` → `V1ApiClient.readTile(id)`.
  - Delete legacy stubs (getTilesInProgress, getActiveTile, getExecution,
    getExecutionView, getTodayTimelineView).
  - Delete `startBreak` / `endBreak` / `extendTile` (throws).
- **MODIFY** `data/api/V1ApiClient.kt` — add
  `getTiles(filter: TileFilter): V1ListTilesResponse?` that builds the
  query string from the filter and returns the typed payload. Adjust
  `listTiles()` to accept the filter (preserve overload for backward
  compat with internal callers, retire by C12).
- **MODIFY** `ui/dashboard/DashboardViewModel.kt`:
  - `_tiles` Flow fed by `TileRepository.getTiles(filter)` (compose from
    a `MutableStateFlow<TileFilter>` that consumers like `TilesScreen`,
    `ReferencesSectionContent`, `CalendarSectionContent` update).
  - Expose `nextActionableTileId`, `nextActionableStartAt`.
  - Drop `buildTimelineTiles(tiles)` legacy path; replace with
    `tileRepository.getTimeline(start, end) → v1ApiClient.getTimeline()`
    (this is preserved; no change there).

**Composition**: none — repository layer.
**i18n keys**: none directly. (Filters surface in C4.)
**API contract**:
- `GET /v1/tiles?view_mode=&limit=&search=&exclude_future=&range=&granularity=&owner_ids=`
- Response `{tiles: [TileListView], next_actionable_tile_id, next_actionable_start_at}`.
**Allowed mobile variations**: `TileFilter` is a Compose-convenient
data class not present in web. Wire shape must match exactly.

**Verification**:
- Unit test (`TileRepositoryGetTilesTest`): wires a fake `V1ApiClient`,
  asserts the call URL is built per spec when each filter field flips
  (range=today, granularity=min_5m, exclude_future=true, owner_ids=[u1,
  u2], search="meeting", limit=50).
- `gradle :app:testDebugUnitTest` green.
- `diff -u <(...) <(...)` against web's `use-tile-list` query string.

---

### C4 — Tiles tab content/control rewrite

**Web source(s)**:
`src/app/dashboard/tiles/page.tsx` (656 lines), plus
`src/lib/hooks/use-tile-list.ts` semantics.

**Android target file(s)**:
- **REWRITE** `ui/mobile/tabs/TilesScreen.kt` — content matches web in
  order top → bottom:
  1. `AppScreenTitle("Tiles Workspace")` + breadcrumbs (header).
  2. Summary line `Row { Text("Open: ${openCount} · Estimated: ${est} min · Sections: ${sections}") }`
     — sourced from `viewModel.tiles`.
  3. Filter row (`TilesFilterBar`):
     - `OutlinedTextField` (search, label = `R.string.dashboard_tiles_searchPlaceholder`).
     - `DropdownMenu` (range) — values All/Today/Recent/ExcludeFuture.
     - `DropdownMenu` (granularity) — All/No breaks/≥ 5m/≥ 15m/≥ 30m.
     - `DropdownMenu` (limit) — 20/50/100/500/Unlimited.
     - `SegmentedRow` (grouping: state/project/tag, 3 buttons).
     - `SegmentedRow` (view-mode: compact/comfortable/detailed, 3 buttons).
     - 3-tab pill (`RowSegmented` — List/Timeline/Changes).
  4. Per-section block (`TilesSectionColumn` — title row with total
     minutes + "X more ▼" expander + `LazyColumn` of `TileCard` rows
     using view-mode height).
  5. Mounted `DeleteTileDialog` (mirrors web).
  6. Empty state ("No tiles yet").
- **CREATE** `ui/mobile/tabs/TilesTab.kt` (extracts composition logic;
  consumed by `TilesScreen` and any tab-aware re-mounts).
- **CREATE** `ui/mobile/tabs/tiles/TilesFilterBar.kt`,
  `TilesSectionColumn.kt`, `TileCard.kt`,
  `DeleteTileDialog.kt`, `TilesTabSwitcher.kt`.
- **MODIFY** `ui/dashboard/DashboardViewModel.kt` — add
  `tabIndex`, `filter` state, `expandedSectionLimit`, `viewMode`. Wire
  `setFilter`, `setTab`, `setGrouping`, `setViewMode`,
  `expandSection`, etc.

**Composition** (mandatory):
- Tab segmented buttons: list / timeline / changes (3 buttons).
- Search input, range dropdown, granularity dropdown, limit dropdown,
  grouping toggle, view-mode toggle, per-section expander, timeline
  scale dropdown (when tab=timeline — reuses C10 picker but in tab),
  date inputs (scale=custom — 2 date pickers), `TileStatusIcon`,
  `DeleteTileDialog`.
- **Total: 12 controls** (matches web audit row count).

**i18n keys to mirror** (snake_case ids):
| Web key | Android id |
|---|---|
| `dashboard.tiles.title` | `dashboard_tiles_title` |
| `dashboard.tiles.tab.list` | `dashboard_tiles_tab_list` |
| `dashboard.tiles.tab.timeline` | `dashboard_tiles_tab_timeline` |
| `dashboard.tiles.tab.changes` | `dashboard_tiles_tab_changes` |
| `dashboard.tiles.section.main` | `dashboard_tiles_section_main` |
| `dashboard.tiles.section.sub` | `dashboard_tiles_section_sub` |
| `dashboard.tiles.empty.main` | `dashboard_tiles_empty_main` |
| `dashboard.tiles.empty.sub` | `dashboard_tiles_empty_sub` |
| `dashboard.tiles.summary.openCount` | `dashboard_tiles_summary_open_count` |
| `dashboard.tiles.summary.estimated` | `dashboard_tiles_summary_estimated` |
| `dashboard.tiles.summary.sections` | `dashboard_tiles_summary_sections` |
| `dashboard.tiles.searchPlaceholder` | `dashboard_tiles_search_placeholder` |
| `dashboard.tiles.filter.rangeLabel` | `dashboard_tiles_filter_range_label` |
| `dashboard.tiles.filter.range.{all,today,recent,excludeFuture}` | `…_filter_range_{all,today,recent,exclude_future}` |
| `dashboard.tiles.filter.granularityLabel` | `…_filter_granularity_label` |
| `dashboard.tiles.filter.granularity.{all,noBreaks,min5,min15,min30}` | `…_filter_granularity_{all,no_breaks,min_5m,min_15m,min_30m}` |
| `dashboard.tiles.filter.limitLabel` | `…_filter_limit_label` |
| `dashboard.tiles.filter.limit.{20,50,100,500,unlimited}` | `…_filter_limit_{20,50,100,500,unlimited}` |
| `dashboard.tiles.omittedMore` | `dashboard_tiles_omitted_more` |
| `tiles.duration` | `tiles_duration` |
| `tiles.startAt` | `tiles_start_at` |

**API contract**:
- `GET /v1/tiles?...` (via C3).
- `POST /v1/tiles/{id}/start` (StartTile envelope) — wired via
  `DashboardViewModel.startTile`.
- `POST /v1/tiles/{id}/complete` (SetTileLifecycle state=2).
- `POST /v1/tiles/{id}/defer`.
- `DELETE /v1/tiles/{id}`.
- `POST /v1/prompts` (prompt_requested from `TileStatusIcon`).

**Allowed mobile variations**:
- `<input type="text">` → `OutlinedTextField`.
- `<Dropdown size="tiny">` → `ComposeDropdownMenu`.
- `<RowSegmented>` → `SegmentedRow` Compose component (same 3-button pill).
- `DeleteTileDialog` → `AlertDialog` (same control IDs).

**Verification**:
- Snapshot render test (Roborazzi or Paparazzi) of `TilesScreen` at 360×800
  showing 12 controls present, in order. Compare to web `/dashboard/tiles`
  screenshot from a known reference.
- `grep -n "filter.range\|filter.granularity\|filter.limit" app/src/main/res/values/strings.xml` returns 14 (one label + 5+5+5 values).
- `gradle :app:assembleDebug :app:testDebugUnitTest` green.

---

### C5 — Projects side panel CRUD

**Web source(s)**:
`src/components/panels/ProjectsSidePanel.tsx` (214 lines),
`src/lib/hooks/use-projects.ts`.

**Android target file(s)**:
- **REWRITE** `ui/mobile/sheets/SectionPanelContent.kt` —
  `ProjectsSectionContent` (was reusing checkbox stub).
- **CREATE** `ui/mobile/panels/ProjectsSectionContent.kt`.
- **CREATE** `ui/mobile/panels/projects/ProjectsList.kt`,
  `NewProjectForm.kt`, `ProjectRow.kt`.
- **CREATE** `data/repository/WorkspaceRepository.kt` —
  `suspend fun list(): List<Workspace>`,
  `suspend fun create(input: CreateWorkspaceInput): Workspace`,
  `suspend fun delete(id: String)`.
- **CREATE** `ui/mobile/panels/ProjectsViewModel.kt`.

**Composition** (mandatory; mirrors web `ProjectsSidePanel`):
- `Button "+ New"` (open inline create form).
- `OutlinedTextField` (name, `placeholder = "Project name"`,
  `testTag = "project-create-name"`).
- `OutlinedTextField` (slug, `placeholder = "slug (optional)"`,
  testTag = "project-create-slug", regex = `[a-z0-9-]+`).
- `ColorPicker` (`Modifier.testTag("project-create-color")`).
- `Button "Create"` (`disabled when busy || name.isBlank()`,
  `label = "Creating..."` while busy).
- `Button "Cancel"`.
- `Button "All Projects"` (clear owner filter).
- Per-workspace select row (one per workspace).
- Per-workspace delete `×` button (visible only when row long-pressed /
  held; mobile analog of `group-hover:visible`).
- Inline error `Text` (red) beneath Create when validation/API fails.

**i18n keys**: none in web. Mirror: introduce
`panels.projects.{title, allProjects, newButton, namePlaceholder,
slugPlaceholder, slugHint, colorLabel, create, creating, cancel,
deleteHint, deleteConfirmTitle, deleteConfirmBody, errorEmpty,
errorSlug, errorCreate}` — fill ja/en strings verbatim from web's
hardcoded English (mirror to ja).

**API contract**:
- `GET /v1/access/subjects?kind=1` → `{ items: [Workspace], count }`.
- `POST /v1/access/workspaces` body `{ display_name, slug?, color? }`.
- `DELETE /v1/access/subjects/{id}`.

`Workspace` data class:
```
{ id, kind: 1, display_name, slug?, email?, color?, owner_user_id,
  disabled_at?, created_at?, updated_at? }
```

**Allowed mobile variations**:
- `group-hover:visible` → `combinedClickable` (long-press reveals delete).
- Inline `window.confirm` → `AlertDialog` reusing web's confirm text.

**Verification**:
- Unit test: `WorkspaceRepository` builds URL / body / method per spec;
  handles 200 + 4xx (incl. 409 slug conflict).
- UI test: New → fill → Create → row appears; long-press → × visible →
  delete → confirm → row gone.
- `gradle :app:testDebugUnitTest :app:assembleDebug` green.

---

### C6 — References side panel rewrite

**Web source(s)**:
`src/components/panels/ReferencesSidePanel.tsx` (61 lines),
`src/lib/stores/reference-overlay-store.ts`,
`src/lib/hooks/use-tile-list.ts`.

**Android target file(s)**:
- **MODIFY** `ui/mobile/sheets/SectionPanelContent.kt` —
  `ReferencesSectionContent` (was 4 hardcoded URLs; replace).
- **CREATE** `ui/mobile/panels/ReferencesSectionContent.kt`.
- **CREATE** `ui/mobile/panels/references/ReferencesLabelList.kt`.
- **CREATE** `data/repository/ReferenceOverlayStore.kt` — backed by
  DataStore Preferences (or `EncryptedTokenStorage`-aligned shared
  prefs since the keys are not sensitive). Keys:
  `reference_overlay_enabled` (StringSet).
- **MODIFY** `DashboardViewModel.kt` — expose
  `referenceOverlayEnabled: StateFlow<Set<String>>`,
  `toggleReference(label: String)`.

**Composition** (mandatory; mirrors web):
- One `Toggleable` row per unique label (from
  `groupTilesByLabel(viewModel.tiles)`).
- Empty state: "No labels yet. Tiles with labels will appear here."

**i18n keys**: none in web. Mirror to `panels.references.{title,
empty, labelHint}`.

**API contract**:
- `GET /v1/tiles?view_mode=list&limit=500` (via C3) — same hook
  used by Tiles tab; android reuses the cached list.

**Allowed mobile variations**:
- `<button>` toggle → `Switch` row or `Row` with `RadioButton`
  (single-select per label vs additive — pick **Switch** to mirror web's
  additive `enabled: string[]` set).

**Verification**:
- Unit test: `ReferenceOverlayStore.persist(Toggle, label)` round-trips
  through DataStore.
- UI test: 3 tiles each with 2 labels → 6 toggles; toggle 1 → persist
  readback matches.

---

### C7 — Preferences sidebar + sub-sheets

**Web source(s)**:
- `src/components/panels/PreferencesSidePanel.tsx` (85 lines).
- `src/app/dashboard/preferences/account/page.tsx` (297 lines).
- `src/lib/stores/theme-store.ts`, `src/lib/stores/locale-store.ts`,
  `src/lib/security/security-lock-policy.ts`,
  `src/lib/notifications/browser.ts`.

**Android target file(s)**:
- **MODIFY** `ui/mobile/sheets/SectionPanelContent.kt` —
  `PreferencesSectionContent` becomes a **vertical nav list**
  (4 items) mirroring web `PreferencesSidePanel`:
  - "General Preferences" → opens `GeneralPreferencesSheet`
    (renders C9 content via `BottomSheet`).
  - "Profile" → opens `AccountSheet` (renders account page content).
  - "Subscription" → opens `SubscriptionSheet`.
  - "Access Tokens" → opens `TokensSheet`.
- **CREATE** `ui/mobile/account/AccountSheet.kt`,
  `AccountViewModel.kt`, `SubscriptionSheet.kt`,
  `TokensSheet.kt`.
- **CREATE** `data/repository/AccountRepository.kt` —
  `suspend fun getProfile(): Profile`,
  `suspend fun startEmailChange(email: String): EmailCodeResult`,
  `suspend fun verifyEmailChange(code: String): VerifyResult`,
  `suspend fun getTokens(): List<TokenView>`,
  `suspend fun createToken(req: CreateTokenRequest): TokenWithSecret`,
  `suspend fun revokeToken(id: String)`.
- **CREATE** `data/api/CognitoAccountApi.kt` (thin OkHttp or
  `HttpURLConnection` client, since these endpoints are not in
  `V1ApiClient`).

**Composition** (mandatory; mirrors web account page top → bottom):
- **Profile** heading + "Open Settings on web to edit public profile." notice.
- **Account** panel:
  - `IconButton(Refresh)` → `loadProfile()`.
  - Email row + status row (`Verified` / `Unverified`).
  - `Text("Account ID: ${sub}")`.
- **Change email** panel:
  - `OutlinedTextField` (new email).
  - `Button "Send code"` → `startEmailChange(...)`.
  - `OutlinedTextField` (code, `keyboardType = Number`).
  - `Button "Verify code"` → `verifyEmailChange(...)`.
- **Login methods** panel:
  - `LinkButton "Passkey"` → `Intent(ACTION_VIEW,
    "https://app.tastile.app/auth/cognito/login?next=/dashboard/account")`.
  - `LinkButton "Email OTP re-login"` →
    `Intent(ACTION_VIEW, "https://app.tastile.app/auth/email")`.
- **Subscription** panel: mirrors web `SubscriptionSection`
  (free/pro plan card, manage/upgrade buttons — until the web audit
  enumerates its components, treat as a passthrough re-export).

**i18n keys to mirror**: every `preferences.account.*`,
`account.tokens.*`, `account.subscription.*` from web (full list below).

**API contract**:
- `GET /api/account/profile` (Next route, returns
  `{ profile: { username, sub, email, emailVerified, preferredUsername } }`).
- `POST /api/account/email/start` (form: `{ email }`).
- `POST /api/account/email/verify` (form: `{ code }`).
- `GET /v1/api-tokens` (returns `{ tokens: [...] }`).
- `POST /v1/api-tokens` (returns `{ token, tokenId, ... }`).

**Allowed mobile variations**:
- Next-route `/api/account/profile` is a web-only proxy; on Android,
  talk to `tastile-core` directly. **Open question** (flag for user):
  does `/api/account/profile` exist on the v1 daemon, or do we need an
  equivalent (`GET /v1/account/profile`)? Audit verified web maps
  `/api/account/profile` → Cognito AdminGetUser. If v1 daemon lacks the
  equivalent, mint a v1 admin route as part of C7 or fall back to
  parsing the cached Cognito JWT for `email` / `email_verified`.

**Verification**:
- Repository unit tests for each endpoint (URL, method, header).
- UI test: account flow (mock Cognito edge) sends code → verifies →
  profile state refreshes.

---

### C8 — AccountMenuSheet (web menu parity)

**Web source(s)**:
`src/components/shell/*` + `nav.*` i18n keys (account dropdown is
typed in floating header).

**Android target file(s)**:
- **REWRITE** `ui/mobile/sheets/AccountMenuSheet.kt` — body mirrors
  web account dropdown:
  - "Profile" → opens `AccountSheet`.
  - "Subscription" → opens `SubscriptionSheet`.
  - "Access Tokens" → opens `TokensSheet`.
  - "Sign out" → `viewModel.signOut()` (currently a no-op).
- Drop "Memo", "Prompt history", "Billing" rows (web does not have
  those).

**Composition** (mandatory; **4 rows total** + email header):
- Header: `Text(email.ifBlank { "Signed in" })`.
- Row "Profile".
- Row "Subscription".
- Row "Access Tokens".
- Row "Sign out".

**i18n keys to mirror**:
| Web key | Android id |
|---|---|
| `nav.account.profile` | `nav_account_profile` |
| `nav.account.subscription` | `nav_account_subscription` |
| `nav.account.tokens` | `nav_account_tokens` |
| `nav.account.signOut` | `nav_account_sign_out` |
| `shell.account.signedIn` (or fallback) | `shell_account_signed_in` |

**API contract**: `DELETE /v1/api-tokens/{id}` (sign-out clears tokens).
**Allowed mobile variations**: list rows are `AppListRow` (consistent
with android shell).

**Verification**:
- UI test: 4 rows render in order; tapping "Sign out" calls
  `viewModel.signOut()` (mock VM, assert invocation).

---

### C9 — Settings UI: Gray theme + lock/notification

**Web source(s)**:
`src/app/dashboard/preferences/general/page.tsx` (219 lines),
`src/lib/stores/theme-store.ts`, `src/lib/security/security-lock-policy.ts`,
`src/lib/notifications/browser.ts`.

**Android target file(s)**:
- **REWRITE** `ui/mobile/tabs/SettingsScreen.kt`:
  - "Theme" row → 3-option picker ("Dark", "Light", "Gray") — **Gray
    is new on android**.
  - "Language" row → 2-option picker ("日本語", "English").
  - "Notifications" section: `Button "Allow"` +
    `Button "Test"` + (existing) `Button "Full screen"`.
  - "Security Lock" section: `Switch(securityLockEnabled)` + on true,
    a sub-row `Timeout: ${minutes} min  ›` → stepper reusing web's
    -5 / +5 model with clamps 1..240 (replace the existing 5/15/60
    picker).
- **MODIFY** `ui/theme/` (or wherever `AppTheme` lives) — add
  `ThemeMode.GRAY`; wire `AppTheme(colorScheme = GrayColors)` based on
  the user's `themeMode`.
- **MODIFY** `data/repository/UserSettingsRepository.kt` — replace
  `ThemeMode { LIGHT, DARK }` with `{ LIGHT, GRAY, DARK }`; persist
  "system" default. Adjust the secure lock timeout setter to support
  arbitrary minute steps (replace the 5/15/60 preset dialog).

**Composition** (mandatory; **8 controls** total — matches web audit):
- Theme `RowSegmented` 3-option.
- Language `RowSegmented` 2-option.
- Notification "Allow" button.
- Notification "Test" button.
- Security Lock ON/OFF button.
- Timeout `-5` button.
- Timeout `+5` button.
- Timeout display.

**i18n keys to mirror** (web namespace `settings.*`):
| Web key | Android id |
|---|---|
| `settings.theme` | `settings_theme` |
| `settings.themeLight` | `settings_theme_light` |
| `settings.themeGray` | `settings_theme_gray` |
| `settings.themeDark` | `settings_theme_dark` |
| `settings.language` | `settings_language` |
| `settings.languageJa` | `settings_language_ja` |
| `settings.languageEn` | `settings_language_en` |
| `settings.securityLock.on` | `settings_security_lock_on` |
| `settings.securityLock.off` | `settings_security_lock_off` |
| `settings.securityLock.timeoutLabel` | `settings_security_lock_timeout_label` |
| `settings.securityLock.timeoutDecrease` | `…_timeout_decrease` |
| `settings.securityLock.timeoutIncrease` | `…_timeout_increase` |
| `settings.notifications.allow` | `settings_notifications_allow` |
| `settings.notifications.test` | `settings_notifications_test` |
| `settings.notifications.statusAllowed` | `…_status_allowed` |
| `settings.notifications.statusDenied` | `…_status_denied` |
| `settings.notifications.statusUnsupported` | `…_status_unsupported` |

**API contract**: none (local-only; `SharedPreferences("tastile-user-settings")`).
**Allowed mobile variations**:
- `RowSegmented` → same Compose `SegmentedRow` used in C4.

**Verification**:
- Theme test: `GrayColors.surface` matches web's grey palette (`#F4F4F5`)
  — store render tests via Roborazzi.
- Lock test: timeout setter clamps at 1 / 240.

---

### C10 — Timeline mode/list/nav matching web

**Web source(s)**:
`src/app/dashboard/timeline/page.tsx` (227 lines).

**Android target file(s)**:
- **REWRITE** `ui/mobile/sheets/SectionPanelContent.kt` —
  `TimelineSectionContent`.
- **CREATE** `ui/mobile/panels/timeline/RangePicker.kt`,
  `TimelineBlockList.kt`, `TimelineMetaPills.kt`.
- **MODIFY** `DashboardViewModel.kt` — surface
  `timelineBlocks: StateFlow<List<TimelineBlock>>` derived from
  `tileRepository.getTimeline(start, end)`,
  `metaPills: { blockCount, totalWorkMin, totalBreakMin }`.

**Composition** (mandatory; mirrors web timeline):
- Page title (`AppScreenTitle`) + meta pills (`Text("N blocks · ${Nm} work · ${N} breaks")`).
- 4-tab pill scale selector (Day / Week / Month / Custom).
- Optional 2 date inputs (when scale=custom).
- `Loading timeline…` overlay while refreshing.
- Empty state `"No blocks in this range. Create a tile to seed the timeline."`.

**i18n keys to mirror**:
| Web key | Android id |
|---|---|
| `panels.calendar.scale` | `panels_calendar_scale` |
| `panels.calendar.day` | `panels_calendar_day` |
| `panels.calendar.week` | `panels_calendar_week` |
| `panels.calendar.month` | `panels_calendar_month` |
| `panels.calendar.custom` | `panels_calendar_custom` |
| `panels.calendar.projects` | `panels_calendar_projects` |
| `panels.calendar.loadingProjects` | `panels_calendar_loading_projects` |
| `panels.timeline.empty` | `panels_timeline_empty` |
| `panels.timeline.loading` | `panels_timeline_loading` |
| `panels.timeline.meta.blocks` | `panels_timeline_meta_blocks` |
| `panels.timeline.meta.work` | `panels_timeline_meta_work` |
| `panels.timeline.meta.breaks` | `panels_timeline_meta_breaks` |

**API contract**:
- `GET /v1/timeline?start=…&end=…` (already wired via C1's data
  foundation).

**Allowed mobile variations**:
- `<input type="date">` → `DatePickerDialog` (Material3).
- Card boundary → LazyColumn Section (no card per requirement).

**Verification**:
- Timeline block list snapshot test.
- `gradle :app:assembleDebug` green.

---

### C11 — Schedule toggle + view-mode

**Web source(s)**:
`src/components/panels/ScheduleSidePanel.tsx` (130 lines).

**Android target file(s)**:
- **MODIFY** `ui/mobile/sheets/SectionPanelContent.kt` —
  `ScheduleSectionContent`.
- **CREATE** `ui/mobile/panels/schedule/ScheduleViewToggle.kt`,
  `ScheduleRowList.kt`, `ProjectsCheckboxSection.kt` (extracted from
  `SidePanelSheet`).
- **MODIFY** `DashboardViewModel.kt` — `scheduleView: StateFlow<"recurring"|"upcoming">`
  with persisted key `schedule_view` (mobile analog of `?view=` URL).
- **MODIFY** `UserSettingsRepository.kt` — add
  `scheduleView: String` (default "recurring").

**Composition** (mandatory; mirrors web `ScheduleSidePanel`):
- `RowSegmented` 2 buttons: "Recurring Tiles" / "Upcoming Deadlines".
- Project checkboxes section (one per workspace; visually identical to
  web's `ProjectsCheckboxSection`).

**i18n keys to mirror**:
| Web key | Android id |
|---|---|
| `panels.schedule.recurring` | `panels_schedule_recurring` |
| `panels.schedule.upcoming` | `panels_schedule_upcoming` |
| `panels.schedule.projects` | `panels_schedule_projects` |
| (reuses) `panels.calendar.*` for shared scale / mini-cal | n/a (already in C10) |

**API contract**:
- `GET /v1/access/subjects?kind=1` (workspaces).
- `GET /v1/tiles` (filtered for recurrency / due).

**Allowed mobile variations**:
- URL `?view=` → SharedPreferences key.

**Verification**:
- Persistence test: toggle view → process death → reopen → state retained.
- `gradle :app:testDebugUnitTest` green.

---

### C12 — i18n keys strings.xml / values-ja/strings.xml

**Web source(s)**:
`src/lib/i18n/translations.ts` (1979 lines).

**Android target file(s)**:
- **MODIFY** `app/src/main/res/values/strings.xml`.
- **MODIFY** `app/src/main/res/values-ja/strings.xml`.

**Mirrored namespaces** (`R.string.<web-key-with-dots-as-underscores>`):

| Namespace | Source keys |
|---|---|
| `nav.*` | execute, tiles, integrations, settings, new, timeline, tasks, projects, schedule, references, preferences (11) |
| `shell.activityBar.*` | 6 keys |
| `shell.floatingHeader.*` | 13 keys |
| `notifications.*` | 8 kinds |
| `header.*` | sync states + general (6+) |
| `languageToggle.*` | 2 keys |
| `execution.*` | 5 prompt options |
| `sidebar.*` | 4 keys |
| `dashboard.tiles.*` | from C4 (23 keys) |
| `timeline.*` | 4 keys |
| `quickCreate.*` | ~75 keys (create / edit forms). Use when `QuickCreateSheet` is rewritten to web parity (out of scope of this sweep unless user signals). |
| `settings.*` | from C9 (16 keys) |
| `common.*` | save, cancel, delete, edit, close, loading, confirm, back (8) |
| `miniCalendar.*` | prev month, next month (2) |
| `auth.mfaSetup.*` | 13 keys |
| `panels.tasks.*` | 16 keys |
| `panels.calendar.*` | from C10 (7 keys) |
| `panels.schedule.*` | from C11 (3 keys) |
| `panels.projects.*` | from C5 (15 keys) |
| `panels.references.*` | from C6 (3 keys) |
| `preferences.account.*` | from C7 (22 keys) |
| `prompt.actions.*` | 7 keys |
| `tiles.*` | actions, dialogs, metadata (10 keys) |
| `account.tokens.*` | from C7 (25 keys) |
| `account.subscription.*` | from C7 (25 keys) |
| `marketing.*` | landing / pricing / footer (18) |
| `marketingLanding.*` | hero / bento / lifecycle / manifesto / pricing / faq / finalCta (90+) |

**Naming rule**: `R.string.<web-key-with-dots-as-underscores>`.
E.g. `panels.calendar.day` → `R.string.panels_calendar_day`.

**Allowed mobile variations**: pure data — no display logic.

**Verification**:
- `grep -E 'name="[a-z]+\.[a-z]+"' app/src/main/res/values/strings.xml` returns 0 (no dots left in ids).
- `gradle :app:assembleDebug --lint` green.

## 5. Execution Order & Dependencies

```
C1 (data model)
  ├── C2  (Google Calendar removal — independent)
  ├── C3  (Tiles API contract)
  │     ├── C4 (Tiles tab)
  │     ├── C5 (Projects)
  │     ├── C6 (References)
  │     ├── C7 (Preferences) → C8 (AccountMenu)
  │     ├── C10 (Timeline mode/list/nav)
  │     └── C11 (Schedule toggle)
  └── C9 (Settings UI Gray theme)
              ↓
            C12 (i18n strings.xml / values-ja — last; consumes all)
```

C1 is foundational. C2 is independent (cleanup). C3 unblocks the read hooks
that C4 / C5 / C6 / C10 / C11 depend on. C7 / C8 / C9 each depend only on
their own models and the data foundation. C12 is the final consolidation —
only after every control's i18n key set is known to be stable.

## 6. Verification Per Chunk

Every chunk ships with:

1. **Build**: `gradle :app:assembleDebug` exits 0.
2. **Unit tests**: `gradle :app:testDebugUnitTest` exits 0.
3. **Control composition audit**: enumerate the controls in order; verify
   the count matches the row in the chunk's "Composition" section above.
4. **i18n key coverage**: `grep -E '<key-names>' app/src/main/res/values/strings.xml`
   returns ≥ the expected count; each key has a JA counterpart.
5. **Diff scope**: `git diff --stat` shows only files inside this chunk's
   "Android target file(s)" list — no drive-by edits.
6. **Visual smoke** (chunks C4, C5, C7, C9, C10): Roborazzi or Paparazzi
   golden image matches web reference for the same surface.
7. **Single PR** at end of all chunks green.

## 7. Out-of-Scope Flags

- **`useDaemonExecution`** in web: throws today per web audit §7 / open
  question 1. Android C3 will only call the v1 endpoints (`POST
  /v1/tiles/{id}/start`, etc.); does **not** mirror the broken web
  behavior. Flag for user: should android C4 handle prompt decisions
  through `POST /v1/prompts` even though web's tile page currently
  reaches a throw?

- **`/api/account/*` Next routes on Android**: web audit notes these are
  Next-server proxy routes. On Android we hit the v1 daemon or Cognito
  directly. C7 needs user clarification on which path.

- **Marketing/i18n keys** in C12: listed for completeness but `marketing.*`
  and `marketingLanding.*` are public-landing copy; the Android webview /
  app shell does not currently render these. Confirm with user whether
  to translate (likely skip — they belong to the Next.js landing pages).

- **`quickCreate.*` (~75 keys)**: Android's `QuickCreateSheet` exists but
  was not in the audit's scope. Add or skip depending on whether the
  user wants C4 to also rebuild the create sheet to web parity. Default:
  skip for this sweep; revisit if user signals.

## 8. Single Sweep / Single PR

User directive: `PRに分ける必要はない`. After all 12 chunks green on a
fresh branch `2026-07-07-android-parity`, push + open **one** PR
against `main`. Use a single commit per chunk (squashing during merge
as configured) or one cumulative commit, per project policy on
"small, readable commits".
