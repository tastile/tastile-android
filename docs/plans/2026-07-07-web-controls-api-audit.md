# Tastile Web — Controls, API Calls, and Data Model Audit

Audit date: 2026-07-07
Scope: read-only catalog of controls, API endpoints, and data model fields
for the dashboard pages, side panels, hooks, and stores of the `tastile-web`
Next.js 16 codebase (`C:\Users\rebui\Desktop\tastile\tastile-web\src\`).

The web client is a thin v1 API client against `tastile-core`. Most read calls
go via the shared `CoreClient` (`src/lib/api/endpoints.ts`) under the
`/v1/...` namespace; some legacy `/api/...` Next.js routes remain for
account, events, Stripe, and dashboard proxy endpoints.

All API calls hit the `tastile-core` v1 API behind
`Authorization: Bearer <cognito_id_token>` (Cognito JWT), or for some
Next-internal routes `tastile_uid` cookie + bridge secret headers.

---

## 1. Dashboard Pages

### `/dashboard/tiles`
File: `src/app/dashboard/tiles/page.tsx` (656 lines)

**Content shown** (top → bottom):
- `<h1>` title
- Section "main" + "sub" placeholder cards (`DesktopStyleTileRow`)
- Summary line: openCount, estimated minutes, sections count
- Filter row (search, range, granularity, limit, grouping toggle, view toggle)
- Per-group section: title (with total minutes), rows of tiles, "X more ▼" expander
- Tabs: List / Timeline / Changes (timeline shows `TimelineAxis`; changes shows a list)
- Empty-state "No tiles yet" message at the bottom
- `DeleteTileDialog` mounted (opened via dialog store)

**Controls** (in order):
- **Tab segmented buttons**: `list | timeline | changes` (3 buttons in a pill container)
- **Search input** `<input type="text">` — live-bound to `searchTerm`, no debounce
- **Range dropdown** (`<Dropdown size="tiny">`): all / today / recent / exclude_future
- **Granularity dropdown** (`<Dropdown size="tiny">`): all / no_breaks / min_5m / min_15m / min_30m
- **Limit dropdown** (`<Dropdown size="tiny">`): 20 / 50 / 100 / 500 / 0(=unlimited)
- **Grouping toggle button group**: state / project / tag (3 buttons)
- **View-mode toggle button group**: compact / comfortable / detailed (3 buttons)
- **Per-section expander button**: group title button cycles `nextTileSectionLimit`
- **Timeline tab scale dropdown**: day / week / month / custom
- **Timeline tab date inputs** (visible when scale=custom): two `<input type="date">`
- **TileStatusIcon** click → triggers `handlePromptSuggested` command
- **TileCardExpandable** → `onStart` (status icon) + `onDelete` (opens `DeleteTileDialog`)
- Dialog "DeleteTileDialog" mounted at the bottom — no controls until opened

**API calls**:
- `getCoreClient().call("getTiles", { query: { view_mode, limit, search, exclude_future, range, granularity, owner_ids } })`
  → maps to `GET /v1/tiles?view_mode=…&limit=…&search=…&…` via
  `toV1CorePath("/read/tiles")` → `/v1/tiles`
- `useExecutionEngineContext().execute(command, Actor.human("self"))` issues
  Command-pattern events:
  - `request_prompt` (via `TileStatusIcon` click) — routes through legacy
    `useDaemonExecution` which is currently a `throw` (see Open Questions).
  - `delete_tile` (from `handleDeleteConfirm`) — same legacy path.

**Data model fields referenced** (after `mapListViewToTile` mapper):
- `tile.core.id` (`TileId`)
- `tile.core.title`, `tile.core.nextAction`, `tile.core.startedAt?`,
  `tile.core.completedAt?`, `tile.core.lifecycle`
- `tile.work.segments[]`
- `tile.temporal.tz`, `tile.temporal.fixedStart`, `tile.temporal.activeStart`,
  `tile.temporal.releaseAt`
- `tile.temporal.fixedStart`, `tile.temporal.dueAt`, `tile.temporal.fixedEnd`,
  `tile.temporal.activeEnd`
- `tile.objective.targetWorkMin`, `tile.objective.targetRestMin`
- `tile.objective.objectiveMode` (mapped from `objective_mode` numeric code:
  finish_once | recurring | maximize_within_interval | label_only)
- `tile.objective.doneRule` (from `done_rule`)
- `tile.annotation.labels`, `tile.annotation.semanticRole`
- `tileListView.temporal.{release_at,due_at,fixed_start,fixed_end,active_start,active_end}`
- `tileListView.recurrence.{step_min,window_start_min,window_end_min,expression}`
- Change-event fields: `event.tileTitle`, `event.eventType`, `event.createdAt`,
  `event.tz`

**i18n keys used**:
- `dashboard.tiles.title`
- `dashboard.tiles.tab.list`, `dashboard.tiles.tab.timeline`, `dashboard.tiles.tab.changes`
- `dashboard.tiles.section.main`, `dashboard.tiles.section.sub`
- `dashboard.tiles.empty.main`, `dashboard.tiles.empty.sub`
- `dashboard.tiles.summary.openCount`, `dashboard.tiles.summary.estimated`, `dashboard.tiles.summary.sections`
- `dashboard.tiles.searchPlaceholder`
- `dashboard.tiles.filter.rangeLabel`
- `dashboard.tiles.filter.range.all`, `today`, `recent`, `excludeFuture`
- `dashboard.tiles.filter.granularityLabel`
- `dashboard.tiles.filter.granularity.all`, `noBreaks`, `min5`, `min15`, `min30`
- `dashboard.tiles.filter.limitLabel`
- `dashboard.tiles.filter.limit.20`, `50`, `100`, `500`, `unlimited`
- `dashboard.tiles.omittedMore`
- `tiles.duration`, `tiles.startAt`

---

### `/dashboard/execute`
File: `src/app/dashboard/execute/page.tsx` (15 lines — essentially a thin shell)

**Content shown**:
- Suspended `CalendarMain` view with `initialView="day"`
- Loading fallback "Loading execution..."

**Controls**:
- All controls live inside `CalendarMain` (this file has none itself).

**API calls**:
- None directly; control is delegated to `CalendarMain`, which calls
  `useEvents(range)` against `/api/events/occurrences?start=&end=&min_minutes=&include_recurring=`.

**Data model fields referenced**: none directly.

**i18n keys used**: none directly.

---

### `/dashboard/timeline`
File: `src/app/dashboard/timeline/page.tsx` (227 lines)

**Content shown**:
- `PageContainer` with sticky header (eyebrow "views · timeline", title, description, meta pills)
- Meta pills: `N blocks`, `Nm work`, `N breaks`
- Action area: 4-tab pill scale selector (day / week / month / custom)
- Optional custom-range card (two date inputs) only when scale = custom
- Main timeline list/canvas in `Card`: list of blocks (type chip + title + start/end/duration + status dot)
- Loading/empty states ("Loading timeline…" / "No blocks in this range. Create a tile to seed the timeline.")
- Side panel: `TimelineSidePanel` (mini-calendar + scale list + projects checkbox list)

**Controls**:
- **Scale segmented buttons** (4 buttons): Day / Week / Month / Custom
- **Date input** (start) — `<input type="date">` shown when scale=custom
- **Date input** (end) — `<input type="date">` shown when scale=custom
- **Mini-calendar day cells** (in side panel) — click selects a date
- **Side-panel "Scale" row of buttons** (Day / Week / Month / Custom) inside `TimelineSidePanel`
- **Project checkboxes** (one per workspace) in the shared
  `ProjectsCheckboxSection` inside `TimelineSidePanel` (see CalendarSidePanel
  for shape).
- `Loader2` overlays while refreshing

**API calls**:
- `useExecutionEngineContext()` returns the local engine's `state.timeline`
  (in-memory, no fetch in this file).
- The page does not issue any HTTP call directly; data comes from
  `use-daemon-execution.ts` which is currently a legacy throw stub —
  see Open Questions.
- Side panel `ProjectsCheckboxSection` fires `getCoreClient().call("listMyWorkspaces")`
  → `GET /v1/access/subjects?kind=1`.

**Data model fields referenced**:
- `b.type` ("work" | "break" | "fixed"), `b.id`, `b.title`, `b.startLabel`,
  `b.endLabel`, `b.durationLabel`, `b.status` ("active" | "done" | "pending")
- `state.timeline` (Block[] array; sourced from `useExecutionEngineContext`)
- `state.timeline` items shown via `buildTimelineView`

**i18n keys used**:
- `panels.calendar.day`, `panels.calendar.week`, `panels.calendar.month`, `panels.calendar.custom`
- `panels.calendar.scale`
- `panels.calendar.loadingProjects`, `panels.calendar.projects`

---

### `/dashboard/integrations`
File: `src/app/dashboard/integrations/page.tsx` (34 lines)

**Content shown**:
- Static page (no data fetching, no client state):
  - `<h1>` "Integrations"
  - `<h2>` "Google Calendar"
  - Two paragraphs explaining the integration is outside v1 scope

**Controls**: none — informational page only.

**API calls**: none.

**Data model fields referenced**: none.

**i18n keys used**: none.

---

### `/dashboard/preferences/general`
File: `src/app/dashboard/preferences/general/page.tsx` (219 lines)

**Content shown** (top → bottom):
- `<h1>` "General Preferences"
- Section "Theme Settings" → `RowSegmented` 3-option picker
- Section "Language Settings" → `RowSegmented` 2-option picker
- Section "Notifications" → status text + "Allow" and "Test" buttons
- Section "Security Lock" → ON/OFF button, +/-5 minute timeout buttons

**Controls**:
- **Theme segmented row** (`<RowSegmented icon={Palette}>`): light / gray / dark (3 options)
- **Language segmented row** (`<RowSegmented icon={Languages}>`): ja / en (2 options)
- **"Allow" button** — calls `requestNotificationPermissionOnce()` → browser Notification.requestPermission
- **"Test" button** — calls `simulateNotification()` → may show browser notification + local preview
- **Security Lock ON/OFF button** — toggles `setSecurityLockEnabled(localStorage, !securityLock)`
- **-5 button** — `updateSecurityLockMinutes(securityLockMinutes - 5)` (clamped 1–240)
- **+5 button** — `updateSecurityLockMinutes(securityLockMinutes + 5)` (clamped 1–240)

**API calls**:
- No HTTP requests. All writes go to `localStorage` via
  `src/lib/security/security-lock-policy.ts`. Notification flows use the
  browser Notification API (`Notification.requestPermission()`,
  `new Notification(...)`). Theme persists to `localStorage["tastile-theme"]`,
  locale to `localStorage["tastile-locale"]`.

**Data model fields referenced**: none.

**i18n keys used**:
- `settings.theme`
- `settings.themeLight`, `settings.themeGray`, `settings.themeDark`
- `settings.language`
- `settings.languageJa`, `settings.languageEn`

---

### `/dashboard/preferences/account`
File: `src/app/dashboard/preferences/account/page.tsx` (297 lines)

**Content shown**:
- `<h1>` title
- Tab-routed content via `?tab=profile|subscription|tokens`:
  - **profile**: heading, notice, "Account" panel (email, emailVerified, accountId, refresh button), "Change email" panel (2 forms), "Login methods" panel (passkey link, email OTP re-login link)
  - **subscription**: heading, guide, `<SubscriptionSection />`
  - **tokens**: `<AccessTokenSection />`

**Controls**:
- **Refresh button** (`<RefreshCw>` icon button) — re-invokes `loadProfile()`
- **Email form**: `<input name="email" type="email">` + "Send code" submit button (`POST /api/account/email/start`)
- **Verification form**: `<input name="code" inputMode="numeric">` + "Verify code" submit button (`POST /api/account/email/verify`)
- **Passkey link** (`<Link href="/auth/cognito/login?next=/dashboard/account">`)
- **Email OTP re-login link** (`<Link href="/auth/email">`)

**API calls**:
- `GET /api/account/profile` → returns `{ profile: { username, sub, email, emailVerified, preferredUsername } }`
- `POST /api/account/email/start` (form multipart, body: `{ email }`) — sends a verification code
- `POST /api/account/email/verify` (form multipart, body: `{ code }`) — verifies the code; on success re-fetches profile
- After verification: `loadProfile()` again → `GET /api/account/profile`
- Subscription and Tokens content live in external components (`SubscriptionSection`, `AccessTokenSection`); endpoint catalog not in this file.

**Data model fields referenced**:
- `Profile = { username, sub, email, emailVerified, preferredUsername }` (returned from `/api/account/profile`)
- `pendingEmail` (string), `verificationCode` (string), `submitting` (bool), `notice: { tone, text } | null`

**i18n keys used**:
- `preferences.account.title`
- `preferences.account.profileHeading`, `profileGuide`
- `preferences.account.notice.loadFailed`, `emailStartFailed`, `emailStartSent`,
  `emailVerifyFailed`, `emailUpdated`
- `preferences.account.accountHeading`, `refresh`, `loading`
- `preferences.account.email`, `emailVerified`, `verified`, `unverified`, `accountId`
- `preferences.account.changeEmailHeading`, `newEmail`, `sendCode`, `code`, `verifyCode`
- `preferences.account.loginMethods`, `passkey`, `emailOtpRelogin`
- `preferences.account.subscriptionHeading`, `subscriptionGuide`

---

### `/dashboard/references`
File: `src/app/dashboard/references/page.tsx` (32 lines)

**Content shown**:
- Suspended `ReferencesMain` (contents unknown to this audit; not in scope)
- Loading fallback "Loading references..."
- A side panel `<ReferencesSidePanel />`

**Controls**:
- All controls live inside `ReferencesMain` and `ReferencesSidePanel` (see panel section).

**API calls**:
- `useTrackVisit("/dashboard/references")` — pageview tracker (assumed analytics).
- Indirect: `ReferencesSidePanel` fires `useTileList({ viewMode: "list", limit: 500 })`
  → `GET /v1/tiles?view_mode=list&limit=500`.

**Data model fields referenced**:
- Tracks `tileListView.id` and `tileListView.labels[]` (via `groupTilesByLabel`)
- `reference-overlay-store.enabled[]` and `reference-overlay-store.toggle(label)`

**i18n keys used**: none in this file directly.

---

## 2. Side Panels

### `src/components/panels/CalendarSidePanel.tsx`
(265 lines — exports `CalendarSidePanel` and `TimelineSidePanel`)

**Purpose**: Shared mini-calendar + project filter for `Calendar` views; the
`TimelineSidePanel` variant additionally exposes a Day/Week/Month/Custom scale
selector.

**Controls**:
- **MiniCalendar**: day-grid picker (click selects a date; emits `onSelectDate(date)`); `disabled` when mode = "around" or "future".
- **MiniCal date picker** (in `TimelineSidePanel`): clicking a date also flips scale to "custom" via `onScaleChange?.("custom")`.
- **Timeline scale buttons** (`<button aria-pressed>` × 4): Day / Week / Month / Custom.
- **Project checkboxes** (`ProjectsCheckboxSection`): one `<input type="checkbox">` per workspace, plus a `selected.size / workspaces.length` counter. `data-testid="panel-project-{id}"`.
  Toggling writes `?projects=u1,u2,…` to the URL via `router.replace`. When
  selection equals all, the query param is removed.

**API calls**:
- `useProjects()` → `getCoreClient().call("listMyWorkspaces")` → `GET /v1/access/subjects?kind=1`.

**i18n keys used**:
- `panels.calendar.day`, `panels.calendar.week`, `panels.calendar.month`, `panels.calendar.custom`
- `panels.calendar.scale`
- `panels.calendar.loadingProjects`
- `panels.calendar.projects`

---

### `src/components/panels/PreferencesSidePanel.tsx`
(85 lines)

**Purpose**: Vertical nav for the `/dashboard/preferences/{general,account}*` routes.

**Controls**:
- **Section header labels** (read-only text).
- **Nav links** (`<Link>` × 3) to:
  - `/dashboard/preferences/general` ("General")
  - `/dashboard/preferences/account` ("Profile", active only when no `?tab=`)
  - `/dashboard/preferences/account?tab=tokens` ("Access Tokens")
  - `/dashboard/preferences/account?tab=subscription` ("Subscription")
  - All entries compute `isActive` from `usePathname()` and `?tab=` query.

**API calls**: none.

**Data model fields referenced**: none.

**i18n keys used**: none (hard-coded English labels).

---

### `src/components/panels/ProjectsSidePanel.tsx`
(214 lines)

**Purpose**: Workspace (project) manager — list/select/create/delete.

**Controls**:
- **"+ New" button** — opens inline create form (toggles `creating` state).
- **"Project name" text input** (`<Input>` with `data-testid="project-create-name"`).
- **"slug (optional)" text input** (`<Input data-testid="project-create-slug">`, regex `[a-z0-9-]+`).
- **Color picker** (`<input type="color">` with `data-testid="project-create-color"`).
- **"Create" submit button** — calls `createWorkspace(...)` (disabled when busy or empty name; label switches to "Creating...").
- **"Cancel" button** — resets form.
- **"All Projects" button** — clears `?owner=` query.
- **Per-workspace select button** — writes `?owner={id}` to URL.
- **Per-workspace delete "×" button** — fires `window.confirm`, then `deleteWorkspace(id)`; visible only on hover (`group-hover:visible`).
- **Inline error text** (red) appears beneath Create button when validation/API fails.

**API calls**:
- `useProjects()` → `GET /v1/access/subjects?kind=1`
- `createWorkspace({ display_name, slug?, color? })` → `POST /v1/access/workspaces`
- `deleteWorkspace(id)` → `DELETE /v1/access/subjects/{id}`

**Data model fields referenced**:
- `Workspace = { id, kind, display_name, slug, email, color, owner_user_id, disabled_at, created_at, updated_at }` (v1 access subject).

**i18n keys used**: none.

---

### `src/components/panels/ReferencesSidePanel.tsx`
(61 lines)

**Purpose**: Label toggle panel for the `/dashboard/references` page.
Shows tiles' label groups and toggles a persistent "overlay enabled" set.

**Controls**:
- **Reference Label buttons** (one per unique label) — toggles `reference-overlay-store.enabled` for that label (additive).

**API calls**:
- `useTileList({ viewMode: "list", limit: 500 })` → `GET /v1/tiles?view_mode=list&limit=500`.

**Data model fields referenced**:
- `TileListView.id`, `TileListView.labels[]`
- `groupTilesByLabel` reduces into `groups = [{ name, tileIds }]`
- `reference-overlay-store: { enabled: string[], toggle(label), enable(label), disable(label) }`
- `labels-store: { labels: Record<name, { color }>, ensureLabel(name) }`

**i18n keys used**: none.

---

### `src/components/panels/ScheduleSidePanel.tsx`
(130 lines)

**Purpose**: Two-mode schedule view switcher (Recurring Tiles | Upcoming
Deadlines) plus a project checkbox filter, for pages like
`/dashboard/schedule*` (this audit did not enumerate those pages).

**Controls**:
- **"Recurring Tiles" / "Upcoming Deadlines" buttons** (`SCHEDULE_VIEWS` array) — writes `?view=recurring|upcoming` to URL.
- **Project checkboxes** (`ProjectsCheckboxSection`) — one checkbox per workspace; toggling writes `?projects=u1,u2,…`.

**API calls**:
- `useProjects()` → `GET /v1/access/subjects?kind=1`.

**Data model fields referenced**:
- `Workspace` (id, display_name, color) — see ProjectsSidePanel.

**i18n keys used**: none.

---

### `src/components/panels/TasksSidePanel.tsx`
(344 lines)

**Purpose**: Filter sidebar for a Tasks-style list — search, time range, min
duration, and priority switches; writes to URL params only (no fetch in this
file).

**Controls**:
- **Search input** (`<input type="text">` with placeholder) — writes `?q=…`
- **Time-range numeric input** (`<Input type="number" min=1 max=365>`) — bound to `rangeVal`.
- **Time-range unit dropdown** (`<Dropdown>` size=small): days / weeks / months.
- **Time-range slider** (`<input type="range" min=1 max=90|12|6>` depending on unit).
- **Min Duration numeric input** (`<Input type="number" min=0 max=240>`) — bound to `minDuration`.
- **Min Duration slider** (`<input type="range" min=0 max=120 step=5>`).
- **"High Priority Only" toggle switch** (`sr-only` checkbox + visual div).
- **"Exclude Low Priority" toggle switch** (`sr-only` checkbox + visual div).
- **"Reset to Defaults" button** — restores `range=7d`, `granularity=no_breaks,min_0m`.

Internally these compose into URL params:
- `?q=<query>`
- `?range=<val><unit>`  (e.g. `?range=7d`)
- `?granularity=no_breaks,min_<val>m[,important_only][,no_low_priority]`

**API calls**: none in this file. Consumers read the URL params and pass to
`useTileList` or similar.

**Data model fields referenced**: none in this file.

**i18n keys used**:
- `panels.tasks.search`
- `panels.tasks.searchPlaceholder`
- `panels.tasks.timeRange`
- `panels.tasks.days`, `panels.tasks.weeks`, `panels.tasks.months`
- `panels.tasks.minDuration`, `panels.tasks.minUnit`, `panels.tasks.minutes`
- `panels.tasks.priorityFilter`
- `panels.tasks.highPriorityOnly`
- `panels.tasks.excludeLowPriority`
- `panels.tasks.resetToDefaults`

---

## 3. Hooks, Stores, Utilities

### `src/lib/hooks/use-tile-list.ts`
(162 lines)

**Purpose**: React hook that lists v1 tiles (`GET /v1/tiles`) with optional
client-side filters; auto-refreshes on a `tastile:tiles-changed` browser
event.

**Inputs**: `UseTileListArgs = {
viewMode?: string;   // e.g. "list"
lifecycle?: string;
limit?: number;
search?: string;
excludeFuture?: boolean;
range?: string;      // "all"|"today"|"recent"|"exclude_future"
granularity?: string; // "all"|"no_breaks"|"min_5m"|"min_15m"|"min_30m"
ownerIds?: string[];
}`

**Returns**: `{ tiles: TileListView[], nextActionableTileId: string | null, nextActionableStartAt: string | null, loading: boolean, error: Error | null, refresh: () => void }`

**HTTP call**:
- `getCoreClient().call("getTiles", { query: { view_mode, lifecycle, limit, search, exclude_future, range, granularity, owner_ids } })`
  → `GET /v1/tiles?<querystring>` (via `toV1CorePath`)
  Response: `{ tiles: TileListView[], next_actionable_tile_id: string|null, next_actionable_start_at: string|null }`

**`TileListView` shape** (mirrors OpenAPI):
```
{
  id, plan_id, title,
  lifecycle: number,            // i16 code mapped to TileLifecycle by mapListViewToTile
  next_action, done_definition,
  worked_minutes, break_minutes, labels: string[],
  objective_mode: number,       // mapped to ObjectiveMode
  target_work_min, target_rest_min,
  done_rule: number|null,
  resume_note,
  projected_next_start_at,
  temporal: { release_at, due_at, fixed_start, fixed_end, active_start, active_end } | null,
  recurrence: { step_min, window_start_min, window_end_min, expression } | null
}
```

**Refresh trigger**: window event `tastile:tiles-changed`; also re-runs on
inputs change (useEffect with full dep array).

**Used by**: `dashboard/tiles/page.tsx` (List and Changes panels),
`components/panels/ReferencesSidePanel.tsx`, and components shown elsewhere
in the dashboard (not enumerated here).

---

### `src/lib/hooks/use-projects.ts`
(94 lines)

**Purpose**: Fetches workspaces (`GET /v1/access/subjects?kind=1`).

**Inputs**: none.

**Returns**: `{ workspaces: Workspace[], loading: boolean, error: Error | null, refresh: () => Promise<void> }`

**HTTP call**:
- `getCoreClient().call("listMyWorkspaces")` → `GET /v1/access/subjects?kind=1`
  → `{ items: Workspace[], count: number }`

**`Workspace` shape**:
```
{ id, kind, display_name, slug, email, color, owner_user_id, disabled_at, created_at, updated_at }
```

**Exports** (helpers used by `ProjectsSidePanel`):
- `createWorkspace(input: CreateWorkspaceInput)` → `POST /v1/access/workspaces`
- `updateWorkspace(id, input: UpdateWorkspaceInput)` → `PATCH /v1/access/subjects/{id}`
- `deleteWorkspace(id)` → `DELETE /v1/access/subjects/{id}`

**Used by**: `components/panels/ProjectsSidePanel.tsx`,
`components/panels/CalendarSidePanel.tsx` (via `ProjectsCheckboxSection`),
`components/panels/ScheduleSidePanel.tsx`.

---

### `src/lib/hooks/use-events.ts`
File does **not exist** at `src/lib/hooks/use-events.ts`. The closest match is
`src/lib/hooks/calendar/use-events.ts` — covered next.

---

### `src/lib/hooks/calendar/use-events.ts`
(170 lines)

**Purpose**: CRUD against the calendar proxy (`/api/events/*`), backed by
v1 placements via `/v1/timeline`. Auto-refreshes on
`tastile:events-changed` window event.

**Inputs**: `UseEventsRange = { start: ISO, end: ISO, minMinutes?: number=6, includeRecurring?: boolean=true }`

**Returns**: `{ events: CalendarEvent[], loading: boolean, error: Error | null, reload: () => Promise<void>, create(input): Promise<CalendarEvent>, update(id, patch): Promise<CalendarEvent>, remove(id): Promise<void> }`

**HTTP calls**:
- `GET /api/events/occurrences?start=<ISO>&end=<ISO>&min_minutes=6&include_recurring=true`
  → `{ events: CalendarEvent[] }` or `{ occurrences: CalendarEvent[] }`
- `POST /api/events` (body: `{ title, description?, start, end, color, icon? }`) → `{ event: CalendarEvent }`
- `POST /api/events/tiles/{tileId}/update` (body: `{ title?, description?, color?, icon? }`)
- `DELETE /api/events/tiles/{tileId}`

**`CalendarEvent` shape** (from `src/lib/domain/calendar.ts`):
```
{
  id, title, description?, location?,
  start: ISO, end: ISO, allDay: boolean, color: EventColor,
  recurrence: { frequency, until?, count? },
  attendees?, icon?, project?, tags?, memo?,
  source?: { kind: 0..3, detail? } | null,    // MANUAL/RECURRING/FLOW/IMPORT
  tileId?: string | null,
  createdAt: ISO, updatedAt: ISO
}
```
`EventColor` is constrained to the 12-color enum in
`EVENT_COLOR_HEX`. `RecurrenceFrequency = "none" | "daily" | "weekly" | "monthly"`.

**Event constants**:
- `EVENTS_CHANGED_EVENT = "tastile:events-changed"`
- `notifyEventsChanged()` dispatches the event.

**Used by**: `dashboard/execute/page.tsx` (indirectly via `CalendarMain`),
and any consumer that needs the live event horizon.

---

### `src/lib/hooks/execution-engine-context.tsx`
(23 lines — note the `.tsx` extension)

**Purpose**: Thin React context wrapper around the (currently legacy-stub)
`useDaemonExecution()`. Throws if used outside `<ExecutionEngineProvider>`.

**Inputs / returns**: same as `useDaemonExecution`.

**Behavior under v1 (very important — see Open Questions)**:
`useDaemonExecution` returns an `execute` function that **throws** with the
message: `"useDaemonExecution is removed in v1; use the dedicated v1 read
hooks and the v1 command helpers (see app/api/v1/tile-commands.ts)."`

The `state` is a fixed `AppState.initial()` and `loading=false`. So callers
like `tiles/page.tsx` `execute({type:"request_prompt", ...})` and
`execute({type:"delete_tile", ...})` will throw at runtime if the user
actually invokes them. This is intentional per the docstring but is the
current production state.

**Used by**: `dashboard/tiles/page.tsx` and `dashboard/timeline/page.tsx`.

---

### `src/lib/stores/reference-overlay-store.ts`
(28 lines)

**Purpose**: Persisted set of enabled reference labels (`"tastile.reference-overlay"` in localStorage).

**Inputs / returns**:
```
{ enabled: string[], toggle(label), enable(label), disable(label) }
```

**API calls**: none.

**Used by**: `components/panels/ReferencesSidePanel.tsx`,
`components/references/*` (not enumerated here).

---

### `src/lib/stores/theme-store.ts`
(41 lines)

**Purpose**: Persisted theme selection (`"tastile-theme"` in localStorage).
Stores `"light" | "gray" | "dark"`; maps to `applyThemeMode(mode)` from
`@/lib/theme-mode`.

**Inputs / returns**:
```
{ theme: Theme, setTheme(theme) }
```

**API calls**: none. Side-effect: writes the applied theme to the DOM
root element.

**Used by**: `app/dashboard/preferences/general/page.tsx`,
`components/shell/*`, root layout.

---

### `src/lib/stores/locale-store.ts`
(21 lines)

**Purpose**: Persisted UI locale (`"tastile-locale"` in localStorage).
`"ja" | "en"`.

**Inputs / returns**:
```
{ locale: Locale, setLocale(locale) }
```

**API calls**: none.

**Used by**: `lib/i18n/use-translation.ts`,
`app/dashboard/preferences/general/page.tsx`.

---

### `src/lib/stores/quick-create-store.ts`
(590 lines — zustand store; typed extensively with v1 domain types)

**Purpose**: Single source of truth for the QuickTileCreate form (BasePanel
→ 6 SubPanels → Editors). Holds create/edit state for tile creation/editing.

**Notable state**:
- `isOpen: boolean`, `mode: "create" | "edit"`, `editingId: string|null`,
  `editingTileId: string|null`, `loadError: string|null`,
  `submitBlocked: boolean`, `initialAllDay: boolean`
- `identity: { kind: TileKindValue, title, description, externalId, visual: { color, icon } }`
- `plan: Plan` (typed from `@/lib/domain/v1/tile`)
- `time: { span: Span, durationMinMax: DurationRange }`
- `windows: Window[]`
- `recurring: RecurringSlice` (`life`, `frameRules`, `rules`)
- `recurrence: RecurrenceModel | RecurrenceTemplateRecurrence | null`
- `advanced: { changeSets, rules }`
- `meta: { ownerSubjectId, tags, memo }`

**Actions**:
- `open`, `openCreate({initialAllDay?})`, `openEdit(eventId, tileId?)`, `close`, `toggle`
- `setField(path, value)` (deep path setter)
- `loadFromEvent(event)` — hydrates from a `CalendarEvent`
- `loadFromRecurringTile(tileId)` — opens + fetches via
  `getCoreClient().call("getTile", { pathParams: { id } })`
  → `GET /v1/tiles/{id}`. On non-OK response, sets `submitBlocked=true` and
  `loadError`.
- `loadFromTemplate(template)` — seeds create mode from a starter recurring
  template (no HTTP).
- `reset()` — clears fields but preserves `isOpen`.

**Defaults (excerpts)**:
- `identity.kind` = `TileKind.PLACEMENT` (`0`) for create; `TileKind.RECURRING`
  (`0` in constants) for edit.
- `time.span.start` = next half-hour rounded.
- `time.durationMinMax.minMs = 30*60_000`, `maxMs = 90*60_000`.
- `plan.role` = `PlanRole.EXECUTABLE`, `plan.completion.tasks = [task_default]`.
- `recurring.life.state` = `RecurringState.ACTIVE`, `window.weekday_mask = 0b0011111` (Mon–Fri).

**API calls**: only inside `loadFromRecurringTile`: `getTile` → `GET /v1/tiles/{id}`.

**Used by**: `components/quick-create/*`, `components/panels/CalendarSidePanel.tsx`
via `useQuickCreateStore` in upstream callers; not directly imported by any
of the dashboard pages in this audit, but reachable through sub-panels.

---

### `src/lib/security/security-lock-policy.ts`
(36 lines)

**Purpose**: Reads/writes the security-lock policy in localStorage.

**Constants**:
- `SECURITY_LOCK_ENABLED_KEY = "tastile.securityLock.enabled"`
- `SECURITY_LOCK_TIMEOUT_MINUTES_KEY = "tastile.securityLock.timeoutMinutes"`
- `SECURITY_LOCK_LEFT_AT_KEY = "tastile.securityLock.leftAt"`
- `SECURITY_LOCK_CREDENTIAL_ID_KEY = "tastile.securityLock.credentialId"`

**API**:
- `getSecurityLockEnabled(storage)` → boolean (default `true`)
- `setSecurityLockEnabled(storage, enabled)`
- `getSecurityLockTimeoutMinutes(storage)` → number (default `10`, clamp 1–240)
- `setSecurityLockTimeoutMinutes(storage, minutes)`
- `shouldRequireSecurityUnlock({ enabled, timeoutMinutes, lastLeftAt, now })`

**HTTP calls**: none.

**Used by**: `app/dashboard/preferences/general/page.tsx`,
the layout's idle/lock handler (not in this audit's file list).

---

### `src/lib/notifications/browser.ts`
(70 lines)

**Purpose**: Thin wrapper around the browser `Notification` API for in-app
state alerts.

**API**:
- `notificationsSupported()` → boolean (checks `window.isSecureContext`)
- `requestNotificationPermissionOnce()` → `NotificationPermission | "unsupported"`
- `showNotification({ kind, title, body, tag })` — closes after 6s; consolidates
  multiple alerts via `tag`.

**Notification kind enum**:
```
"tile_started" | "tile_completed" | "prompt_pending"
```

**HTTP calls**: none directly; no browser-native fetch.

**Used by**: `app/dashboard/preferences/general/page.tsx`
(`simulateNotification`), and the in-app prompt/notification layer (out of audit scope).

---

### `src/lib/i18n/use-translation.ts`
(23 lines)

**Purpose**: Returns a `t(key)` function and the current locale.

**Inputs / returns**: `{ t: (key: string) => string, locale: Locale }`

**Lookup**: dots-split keys dig into the `translations[locale]` table; if
any segment is missing, the key string is returned verbatim (no error).

**Used by**: all dashboard pages and panels that use `t("...")` /
`useTranslation()`.

---

### `src/lib/utils/map-list-view-to-tile.ts`
(125 lines)

**Purpose**: Project a wire-format `TileListView` row (returned by
`GET /v1/tiles`) into the app-domain `Tile` shape used by consumers like
the tiles page and `buildTileListSections`.

**Inputs**: `item: TileListView`

**Returns**: `Tile` (the in-memory model from `@/lib/domain/tile`)

**Notable mapping rules**:
- `lifecycle` numeric code → `TileLifecycle` via `LIFECYCLE_BY_CODE`
  (READY / STARTED / DONE / CLOSED).
- `objective_mode` numeric code → `ObjectiveMode` via `OBJECTIVE_MODE_BY_CODE`
  (FINISH_ONCE / RECURRING / MAXIMIZE_WITHIN_INTERVAL / LABEL_ONLY).
- `done_rule` numeric code → `DoneRule` (MANUAL / TIME_REACHED / INTERVAL_END).
- `temporal.active_start` → `core.startedAt`; `temporal.active_end` → `core.completedAt`.
- `semantic_role` is **not** on the wire (forbidden by v1/10 §9); a UI-only
  role is projected from `objective_mode` (`label_only → "label"`, else `"work"`).
- `recurrence.expression` round-trips `unknown | null` through.

**Used by**: `app/dashboard/tiles/page.tsx` (the only direct importer in
this audit).

---

## 4. Summary Table

| Page / Hook / Panel                 | # controls | # HTTP endpoints | # data model fields | # i18n keys |
| ----------------------------------- | ---------- | ---------------- | ------------------- | ----------- |
| `/dashboard/tiles`                  | 12         | 1 (getTiles) + 2 legacy throw commands | 16 | 23 |
| `/dashboard/execute`                | 0 (shell)  | 0 in this file; deleg. to `useEvents` (1 endpoint) | 0 | 0 |
| `/dashboard/timeline`               | 4 + side   | 0 in this file; side-panel uses listMyWorkspaces | 4 (`b.type/id/title/status` + summary) | 6 |
| `/dashboard/integrations`           | 0          | 0                | 0                   | 0           |
| `/dashboard/preferences/general`    | 8 (theme 3 + lang 2 + 2 notif + 3 sec-lock) | 0 (localStorage only) | 0 | 8 |
| `/dashboard/preferences/account`    | 4 (refresh, send-code form, verify-code form, 2 links) | 3 (`/api/account/profile`, `/email/start`, `/email/verify`) | 4 (Profile fields) | 22 |
| `/dashboard/references`             | 0 (shell)  | 0 direct; side-panel uses `useTileList` (1 endpoint) | 0 direct | 0 |
| `CalendarSidePanel`                 | 4 (mini-cal cell-click, optional scale buttons 4, project checkboxes N) | 1 (`listMyWorkspaces`) | 1 (`Workspace`) | 6 |
| `PreferencesSidePanel`              | 4 nav links | 0 | 0 | 0 |
| `ProjectsSidePanel`                 | 7 (new, name, slug, color picker, create, cancel, all/projects/n + delete ×/N) | 3 (`listMyWorkspaces` GET, `createWorkspace` POST, `deleteWorkspace` DELETE) | 11 (`Workspace`) | 0 |
| `ReferencesSidePanel`               | 1 per label (toggle) | 1 (`getTiles`) | 3 (`TileListView.id/labels`, `enabled`) | 0 |
| `ScheduleSidePanel`                 | 2 + N project checkboxes | 1 (`listMyWorkspaces`) | 1 (`Workspace`) | 0 |
| `TasksSidePanel`                    | 9 (search, range-num, range-unit dropdown, range-slider, dur-num, dur-slider, hi-pri switch, lo-pri switch, reset) | 0 direct | 0 | 16 |
| `use-tile-list`                     | n/a        | 1 (`GET /v1/tiles`) | 14 (`TileListView` fields) | 0 |
| `use-projects`                      | n/a        | 1 GET, 1 POST, 1 PATCH, 1 DELETE | 11 (`Workspace`) | 0 |
| `use-events` (`calendar/use-events.ts`) | n/a    | 4 (`GET /api/events/occurrences`, `POST /api/events`, `POST /api/events/tiles/{id}/update`, `DELETE /api/events/tiles/{id}`) | 16 (`CalendarEvent`) | 0 |
| `execution-engine-context`          | n/a        | 0 (legacy throw stub) | n/a | n/a |
| `reference-overlay-store`           | n/a        | 0 | 1 (`enabled`) | 0 |
| `theme-store`                       | n/a        | 0 | 1 (`theme`) | 0 |
| `locale-store`                      | n/a        | 0 | 1 (`locale`) | 0 |
| `quick-create-store`                | n/a        | 1 (`getTile`) used in `loadFromRecurringTile` | ~30 (Plan slices, span, durations, recurrence, windows, identity, meta) | 0 |
| `security-lock-policy`              | n/a        | 0 | 0 (4 localStorage keys) | 0 |
| `notifications/browser`             | n/a        | 0 (browser Notification API only) | 0 | 0 |
| `i18n/use-translation`              | n/a        | n/a | n/a | (aggregator) |
| `utils/map-list-view-to-tile`       | n/a        | n/a | 16 (`Tile` + 4 numeric-code mappings) | n/a |

---

## 5. Data Model Reference (src/lib/domain/*)

### `tile.ts` — `Tile`
```
{
  core: { id: TileId, title, nextAction, doneDefinition,
          startedAt, completedAt, lifecycle?: "ready"|"started"|"done"|"closed" },
  work: { segments: Segment[] },
  temporal: { tz, releaseAt, dueAt, fixedStart, fixedEnd, activeStart, activeEnd },
  objective: { objectiveMode, targetWorkMin, targetRestMin, doneRule, recurrence },
  interruption: { interruptPenalty, resumePenalty, breakSplitsWork, externalInterruptOnly },
  automation: { promptOnStart, promptOnEnd, autoStartAllowed, autoEndAllowed },
  annotation: { semanticRole, labels, timedLabels }
}
```
Helper: `getTileLifecycle(tile): TileLifecycle` (derived from
`lifecycle`/`completedAt`/`startedAt`).

### `actor.ts` — `Actor`
```
{ type: "system" | "human" | "agent", id: string }
```
Factories: `Actor.system()`, `Actor.human(id)`, `Actor.agent(id)`.

### `ids.ts` — Branded UUIDs
`TileId`, `EventId`, `CommandId`, `RequestId`, `SegmentId` (UUIDv4 via
`uuid`). Includes `normalizeTileId(value: string | null)` to strip a
"urn:uuid:" prefix.

### `calendar.ts` — `CalendarEvent`
Detailed shape in the `use-events.ts` section above. `EventColor` is a
12-color literal union; `RecurrenceFrequency = "none"|"daily"|"weekly"|"monthly"`.

### `v1/` (subfolder)
Domain types for the v1 backend: `condition.ts`, `completion.ts`,
`constants.ts`, `envelope.ts`, `execution.ts`, `metric.ts`, `placement.ts`,
`reference.ts`, `tile.ts`, `timeline-item.ts`, `window.ts`. These are
referenced by `quick-create-store.ts` but not exercised by the pages in
this audit. Numeric code constants for `TileKind`, `PlanRole`,
`RecurringState` and related enums live in `v1/constants.ts` (no enum
strings allowed by v1/10 §9).

### `tile-list-view-constants.ts`
Maps the i16 numeric codes to the human-readable enums used by
`mapList-view-to-tile.ts`. Removed from the wire: `semantic_role`
(forbidden by v1/10 §9).

---

## 6. HTTP Endpoints Catalog (for these pages)

All `getCoreClient()` calls route through `toV1CorePath()` and prefix with
the configured base URL (defaults to `http://localhost:31400` in tests;
production uses `https://api.tastile.app` via `CLOUD_API_BASE` per the
project memory).

| Method | Path                                                     | Source                        |
| ------ | -------------------------------------------------------- | ----------------------------- |
| GET    | `/v1/tiles?view_mode=&limit=&search=&range=&granularity=&owner_ids=` (`/v1/tiles`) | `use-tile-list` → tiles page, ReferencesSidePanel |
| GET    | `/v1/tiles/{id}`                                         | `quick-create-store.loadFromRecurringTile` |
| GET    | `/v1/access/subjects?kind=1`                             | `useProjects` → panels (Calendar, Schedule, Projects) |
| POST   | `/v1/access/workspaces`                                  | `createWorkspace` |
| PATCH  | `/v1/access/subjects/{id}`                               | `updateWorkspace` (not used by these pages) |
| DELETE | `/v1/access/subjects/{id}`                               | `deleteWorkspace` |
| POST   | `execute({type:"request_prompt", ...})`                  | `tiles/page.tsx` `handlePromptSuggested` (currently throws — see Open Questions) |
| POST   | `execute({type:"delete_tile", ...})`                     | `tiles/page.tsx` `handleDeleteConfirm` (currently throws — see Open Questions) |
| GET    | `/api/account/profile`                                   | `account/page.tsx` `loadProfile` |
| POST   | `/api/account/email/start`                               | `account/page.tsx` `handleEmailStart` |
| POST   | `/api/account/email/verify`                              | `account/page.tsx` `handleEmailVerify` |
| GET    | `/api/events/occurrences?start=&end=&min_minutes=&include_recurring=` | `use-events` (calendar) → execute page |
| POST   | `/api/events`                                            | `use-events.create` |
| POST   | `/api/events/tiles/{tileId}/update`                      | `use-events.update` |
| DELETE | `/api/events/tiles/{tileId}`                             | `use-events.remove` |
| GET    | `/api/proxy/[...path]` (server-side; tracks via `useTrackVisit`) | references page |

(`Account → Subscription → Tokens` content is delegated to
`SubscriptionSection` / `AccessTokenSection`, which are not enumerated here.)

---

## 7. Open Questions / Not-Confirmed

1. **`tiles/page.tsx` → `execute({type:"request_prompt"|"delete_tile"})`**
   `useExecutionEngineContext().execute` is supplied by `useDaemonExecution`,
   which currently `throw`s on every call (docstring: "useDaemonExecution
   is removed in v1"). The doc comment points the caller at
   `app/api/v1/tile-commands.ts`. This audit did not open that route —
   the runtime behaviour of the status icon and delete confirm on
   `/dashboard/tiles` is therefore undetermined. Flag for verification.

2. **`timeline/page.tsx` consumes `state.timeline` from the same legacy
   engine.** As above, `useDaemonExecution` returns an empty initial
   AppState; the timeline page will show an empty list unless
   `state.timeline` is hydrated elsewhere (e.g. by `useEvents` / proxy
   reads). Flag for verification — the visible behaviour of
   `/dashboard/timeline` may depend on unverified upstream hydration.

3. **`use-events.ts` at `src/lib/hooks/use-events.ts` does not exist.** The
   audited file lives at `src/lib/hooks/calendar/use-events.ts`. The task
   spec asked for both — the missing root-level hook is hereby noted.

4. **Integrations** is a static "this is out-of-scope" notice (one `<a>`
   link wrapped in `<code>` is the only interactive element; it's text
   inside a `<code>` element, not a real link).

5. **`PreferencesSidePanel`** uses `/dashboard/preferences/...` URLs as
   hard-coded navigation links. No HTTP call from this component.

6. **Account tabs `subscription` and `tokens`** render external components
   (`<SubscriptionSection />`, `<AccessTokenSection />`). Their endpoints
   are out of this audit's scope; not enumerated.

7. **`quick-create-store.ts`** performs `GET /v1/tiles/{id}` only inside
   `loadFromRecurringTile`. Per the docstring, Submit is gated on success
   (`submitBlocked` flips). Submission endpoints (`/v1/tiles/{id}/update`,
   `/v1/placements/{id}/changes`, `POST /v1/tiles`) were not located
   within this audit's files — likely under `app/api/v1/*` (not in scope).

8. **`TileListView.recurrence`** on the wire still uses the legacy shape
   `{ step_min, window_start_min, window_end_min, expression }` rather
   than the v1 `RecurrenceModel` discriminated union. The map in
   `mapListViewToTile.ts` synthesizes a partial v1 model from the legacy
   fields; `weekday_mask` is hardcoded to `0`.
