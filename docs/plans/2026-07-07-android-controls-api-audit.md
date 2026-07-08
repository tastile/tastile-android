# tastile-android — controls, API, and data model audit

Read-only audit of `app/src/main/` of the tastile-android Kotlin/Compose client,
focused on the mobile UI shell (the four mobile tabs, the side panel, and
overlay/sheets), the repositories + the v1 HTTP client behind them, the
notification/alarm subsystem, and the data model.

Findings are descriptive only. No code was changed.

Conventions used below:
- HTTP paths are relative to the daemon base; the daemon base itself is the
  build-time `TASTILE_CORE_URL` from `ApiModule` / `IntegrationRepository`.
  The IntegrationRepository also falls back to
  `http://127.0.0.1:3140` / `http://10.0.2.2:3140` when the configured URL
  is unreachable.
- "Bearer = Tastile API token (minted by `ApiTokenManager`)" indicates the v1
  token, which is the only `Authorization` header on read/write v1 endpoints.
- i18n note: Most mobile-screen strings are hardcoded English; the few
  Japanese strings live in `ExecutionNotificationCoordinator` /
  `ExecutionAlarmReceiver` and read from `UserSettingsRepository.getLocale()`.
  Settings → Locale toggles between `JA` (`日本語`) and `EN` (`English`),
  but the only consumer is the notification text.

---

## 1. Mobile tab screens

### TilesScreen.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/tabs/TilesScreen.kt`
- **Purpose**: Project-scoped list of all tiles with a local-only filter chip
  set and an Extended FAB to launch `Overlay.QuickCreate`.
- **State read from viewModel**:
  - `tiles: StateFlow<List<Tile>>`
  - `loading: StateFlow<Boolean>`
  - plus local `var filter by remember { mutableStateOf(TileFilter.ALL) }`
- **State written to viewModel**: indirect — only
  `viewModel.selectTile(tile.id)` followed by
  `overlay.show(Overlay.TileEdit(tile.id))` on row tap.
- **Controls**:
  - Filter chips: `FilterChip` × 3 ("All", "Active", "Done")
    (`TileFilter.entries`). The label is derived from the enum's `name`.
    Single-select through local Compose state.
  - Extended FAB: `ExtendedFloatingActionButton`, label = "New", icon =
    `Icons.Outlined.Add`. Click → `overlay.show(Overlay.QuickCreate)`.
  - Per row: `AppListRow` (clickable) → `viewModel.selectTile(id)` +
    `overlay.show(Overlay.TileEdit(id))`.
  - No pull-to-refresh; no swipe actions.
- **HTTP endpoint(s) called**: indirectly via `DashboardViewModel.refreshAll()`
  → `TileRepository.getTiles(userId)` → `V1ApiClient.listTiles()` →
  `GET /v1/tiles`. No screen-local network call.
- **Data fields shown per row**:
  - Lifecycle glyph (✓ / ▶ / ○ / ·) derived from
    `TileLifecycle.fromString(tile.lifecycle)`.
  - `tile.title`.
  - `meta = projectLabel · dueAt · ↻` (built via `tile.projectLabel()`,
    `tile.dueAtDate()`, `tile.isRecurring()`). See `TileConditionsExt.kt`.
- **Locale / i18n**: strings are hardcoded English ("No tiles yet",
  "No active tiles", "No done tiles"). `AppTheme` follows system or
  `ThemeMode` from `UserSettingsRepository`. Locale: JA/EN (no per-locale
  string resources used here).
- **Loading states**: `AppCenteredLoading()` if `loading && tiles.isEmpty()`,
  else `AppEmptyState(message = emptyMessage(filter))`.
- **Notable**: filter state is **not** persisted across process death —
  `remember` only, no `rememberSaveable`.

### ExecuteScreen.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/tabs/ExecuteScreen.kt`
- **Purpose**: Hero card for the currently active tile + list of today-and-ready
  tiles with a `MoreVert` dropdown per row and an inline delete confirmation
  dialog.
- **State read from viewModel**:
  - `tiles: StateFlow<List<Tile>>`
  - `loading: StateFlow<Boolean>`
  - plus local `var deleteCandidate by remember { mutableStateOf<String?>(null) }`
- **State written to viewModel**:
  - `viewModel.startTile(tile.id)` (`MoreVert → Start`, or none inline)
  - `viewModel.completeTile(tile.id)` (`MoreVert → Complete`,
    hero `Button "Complete"`)
  - `viewModel.deferTile(tile.id)` (`MoreVert → Defer`, hero
    `OutlinedButton "Defer"`)
  - `viewModel.deleteTile(id)` (after `AlertDialog` "Delete tile?" confirm)
  - `viewModel.selectTile(tile.id)` + `overlay.show(Overlay.TileEdit(id))`
    on row tap.
- **Controls**:
  - Hero (only when `tiles.firstOrNull { it.isStarted() }` is non-null):
    - "▶ {title}" text + "Next: {nextAction}" if not blank
    - `Button "Complete"` → `completeTile`
    - `OutlinedButton "Defer"` → `deferTile`
  - Per row: lifecycle glyph, `AppListRow`, `IconButton(Icons.Outlined.MoreVert)`
    opening a `DropdownMenu` with `DropdownMenuItem`s:
    "Start" (PlayArrow) → `startTile`,
    "Complete" → `completeTile`,
    "Defer" → `deferTile`,
    "Delete" (Delete icon) → triggers `AlertDialog`.
  - `AlertDialog "Delete tile?"` with `TextButton "Delete"` /
    `TextButton "Cancel"`.
- **HTTP endpoint(s) called**: no direct calls. Through viewModel the
  following all hit `V1CommandDispatcher` → `V1ApiClient.postCommand` /
  `deleteCommand`:
  - `POST /v1/tiles/{id}/start` (`StartTile` command) via
    `dispatchTileStart`
  - `POST /v1/tiles/{id}/complete` (`SetTileLifecycle` state=2) via
    `dispatchTileComplete`
  - `POST /v1/tiles/{id}/defer` (`SetTileLifecycle` state=1, `deferred_until=now+60m`) via
    `dispatchTileDefer`
  - `DELETE /v1/tiles/{id}` via `dispatchTileDelete`
- **Data fields shown**:
  - Hero: `tile.title` + `tile.nextAction`.
  - Row: lifecycle glyph, `tile.title`, semantics "ready/started/done/archived: title".
- **Locale / i18n**: hardcoded English ("Nothing to do — create a tile",
  "Today and ready", "No tiles for today.", "Create", "Complete", "Defer",
  "Delete tile?", etc.). JA/EN envelope only via `theme/locale`.
- **Loading states**: `AppCenteredLoading()` on first load; empty state
  `AppEmptyState("No tiles for today.", actionLabel="Create", onAction=…)`.
- **Notable**: The screen computes "active" as the first tile whose lifecycle
  is `Started`; "today-and-ready" hides `DONE` only (`ARCHIVED` and `READY`
  remain visible).

### IntegrationsScreen.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/tabs/IntegrationsScreen.kt`
- **Purpose**: Two-section list — Connected (live state from viewModel) +
  Available (hardcoded `IntegrationStub` list).
- **State read from viewModel**:
  - `viewModel.integrations: StateFlow<List<Integration>>`
    (always exactly one entry, `id="google_calendar"`, name="Calendar",
    `connected=gc?.connected`).
  - `viewModel.loading: StateFlow<Boolean>`
- **State written to viewModel**:
  - `viewModel.syncGoogleCalendarNow()` (per-row "Sync now" `TextButton`).
  - `viewModel.disconnectGoogleCalendar()` (after `AlertDialog` "Disconnect Google Calendar?").
  - `overlay.show(Overlay.IntegrationConfig(integration.id))` on row tap.
- **Controls**:
  - Connected section header.
  - Per connected integration:
    - `AppStatusDot(connected=true)`
    - `meta = "Connected"`
    - `TextButton "Sync now"` → `syncGoogleCalendarNow()`
    - `TextButton "Disconnect"` → `disconnectCandidate = id`
    - Row click → `overlay.show(Overlay.IntegrationConfig(id))`
  - Available section: hardcoded `IntegrationStub` list
    (outlook/apple/slack/notion). Each row → `AppStatusDot(false)` +
    `AppChevron` + tap → `Overlay.IntegrationConfig(stub.id)`.
  - Confirmation `AlertDialog`: "Disconnect Google Calendar?" →
    `TextButton "Disconnect"` / `TextButton "Cancel"`.
- **HTTP endpoint(s) called**: indirect via viewModel:
  - `GET /auth/integrations/settings` (Google-cal settings) via
    `IntegrationRepository.getSettings()` → `IntegrationRepository.executeDaemonRequest`.
  - `POST /sync/trigger` via `IntegrationRepository.triggerSync()`.
  - `POST /auth/integrations/settings` (with `{google_calendar:{connected:false,account_email:""}}`)
    via `updateGoogleCalendarConnected(false)` →
    `postSettingsPayload(...)`.
- **Data fields shown**: `integration.name`, status dot, meta string.
  Only Google Calendar is read from the backend; the remaining integrations
  are stubs. The disconnect dialog mentions only Google Calendar.
- **Locale / i18n**: hardcoded English. "Cancel" / "Disconnect" / "Sync now"
  / "Connected" / "Available" are English-only.
- **Loading states**: `AppCenteredLoading()` on first load only; otherwise
  inline "No integrations connected".
- **Notable**: The "Available" stub list has no "Connect" action — the only
  effective action comes from opening the `IntegrationConfigSheet`, which
  shows "Coming soon" for anything except `google_calendar`.

### SettingsScreen.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt`
- **Purpose**: User-facing preferences (Locale, Theme, Security lock +
  Timeout) plus notification permission / full-screen-intent management,
  Privacy + About dialogs (with deep-link buttons).
- **State read from viewModel**:
  - `viewModel.locale: StateFlow<AppLocale>`
  - `viewModel.themeMode: StateFlow<ThemeMode>`
  - `viewModel.securityLockEnabled: StateFlow<Boolean>`
  - `viewModel.securityLockTimeoutMinutes: StateFlow<Int>`
  - Local state: notification permission status (`canPostNotifications`),
    full-screen-intent allow (`canUseFullScreenIntent`), ephemeral
    `status` string ("Notifications enabled", "Alarm will open in 3
    seconds", …).
- **State written to viewModel**:
  - `viewModel.setLocale(it)` from `LocalePickerDialog`.
  - `viewModel.setThemeMode(it)` from `ThemePickerDialog`.
  - `viewModel.setSecurityLockEnabled(it)` from `Switch` (in
    `SecurityLockRow`) and from row tap.
  - `viewModel.setSecurityLockTimeoutMinutes(it)` from `TimeoutPickerDialog`.
- **Controls**:
  - `AppListRow "Locale"` (Language icon) → `LocalePickerDialog` (radio
    list "日本語" / "English").
  - `AppListRow "Theme"` (DarkMode icon) → `ThemePickerDialog`
    ("Dark (default)" / "Light").
  - `SecurityLockRow`: lock + meta = "Require biometric to open the app";
    trailing `Switch` for the toggle; if enabled, a sub-row "Timeout:
    {N} min  ›" → `TimeoutPickerDialog` ("5 min" / "15 min" / "60 min").
  - `NotificationSettingsSection`:
    - bell icon + status text ("Alarm ready" / "Limited" / "Blocked"),
    - `Button "Allow"` → requests `POST_NOTIFICATIONS` on Android 13+.
    - `Button "Full screen"` → opens
      `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`.
    - `Button "Test"` → posts a test notification to channel `ALERTS`.
    - `Button "Alarm"` → schedules a `setAlarmClock(...)` 3-second-out test
      alarm (writes PendingIntents for `ExecutionAlarmTestReceiver` and
      `ExecutionAlarmActivity`).
  - `AppListRow "Privacy"` (PrivacyTip icon, chevron) → opens
    `PrivacyDialog` with deep-link button to
    `https://tastile.app/privacy`.
  - `AppListRow "About"` (Info icon, chevron) → opens `AboutDialog`
    showing version + deep-link to `https://github.com/rebuildup/tastile`.
- **HTTP endpoint(s) called**: none directly. Settings write to local
  `SharedPreferences("tastile-user-settings")` via
  `UserSettingsRepository` (no HTTP). The Test/Alarm buttons touch
  `NotificationManagerCompat` and `AlarmManager` directly.
- **Data fields shown**: pretty much every field of `UserSettingsRepository`
  except `KEY_SECURITY_LOCK_LEFT_AT_MILLIS` (which is internal to the
  lock policy). The dialogs reference `versionName` from
  `packageManager.getPackageInfo(packageName, 0).versionName`.
- **Locale / i18n**: hardcoded English UI ("Locale", "Theme", "Security
  lock", "Notifications", "Allow", "Full screen", "Test", "Alarm",
  "Cancel", "OK", "Notifications enabled", etc.). The locales names
  themselves are JA/EN.
- **Loading states**: none. No network calls.
- **Notable**: Security lock is a UI-only switch — there is no BiometricPrompt
  wiring in this file; the switch merely writes to `prefs`. Actual
  enforcement lives in the (separate) `MainActivity` lock flow + the
  `SecurityLockPolicy.shouldRequireUnlock` helper. The TimeoutPickerDialog
  options are 5/15/60 only — the underlying setter stores 1–240.

---

## 2. Side panel content

### SectionPanelContent.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/sheets/SectionPanelContent.kt`
- **Purpose**: Pager page body for each of the five `SidePanelSection`s.
  Mounted by `SidePanelSheet`.
- **State read from viewModel**:
  - `viewModel.selectedDay: StateFlow<LocalDate>` (TimelineSectionContent)
  - `viewModel.scale: StateFlow<TimelineScale>` (TimelineSectionContent)
  - `viewModel.tiles: StateFlow<List<Tile>>` (Timeline, Schedule, Projects)
  - `viewModel.locale / themeMode / securityLockEnabled / securityLockTimeoutMinutes`
    (Preferences section)
- **State written to viewModel**:
  - `viewModel.setSelectedDay(date)` from the mini month calendar.
  - `viewModel.setScale(scale)` from the `ScalePicker`.
  - `viewModel.selectTile(tile.id)` from each `AppListRow` in Schedule.
- **Controls per section**:
  - **Calendar** (`TimelineSectionContent`):
    - `MiniMonthCalendar`: chevron-left/`Previous month` (icon
      `Icons.AutoMirrored.Outlined.KeyboardArrowLeft`) →
      `month = month.minusMonths(1)`; chevron-right → `+1 month`; day cells
      (Box grid 7×variable) → `onSelect(date)` (only clickable cells have
      `date != null`).
    - `ScalePicker`: three Box cells ("Day" / "Week" / "Month") →
      `viewModel.setScale(item)`.
    - `ProjectsCheckboxSection`: see below (reused for the Projects tab).
  - **Schedule** (`ScheduleSectionContent`):
    - Two sections: "Recurring" + "Upcoming (7 days)".
    - Each renders up to 10 `AppListRow`s, each tap → `viewModel.selectTile(id)`.
    - The "recurring" filter is `tiles.filter { it.isRecurring() }`.
    - The "upcoming" filter is `tiles.filter { tile ->
        tile.dueAtDate()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
          ?.let { it in now..now.plusDays(7) } ?: false }`.
  - **References** (`ReferencesSectionContent`):
    - `ReferenceLink` rows: "Help" / "Changelog" / "GitHub" /
      "Send feedback" (4 hardcoded URLs) — each opens the URL with
      `Intent.ACTION_VIEW` + `FLAG_ACTIVITY_NEW_TASK`. No backend call.
  - **Preferences** (`PreferencesSectionContent`):
    - Read-only summary: Locale / Theme / Lock ("On (Nm)" / "Off") rows
      + caption "Open Settings tab to change." No interactive controls.
- **HTTP endpoint(s) called**: none. Reads `viewModel` state only.
- **Data fields shown**:
  - `selectedDay` (LocalDate), `scale` (enum), `tile.title` (recurring
    list + upcoming list), `tile.lifecycle` (semantics), `tile.dueAtDate()`
    (upcoming only — implicit, not displayed as text).
  - `locale.toString()`, `theme.toString()`, `lock` / `timeout` for
    preferences.
- **Locale / i18n**: literal English labels. Days in the mini calendar
  show `"M", "T", "W", "T", "F", "S", "S"` (hardcoded; first day of the
  week is Monday per `DayOfWeek.MONDAY`). Month name uses
  `Locale.ENGLISH` explicitly.
- **Notable**:
  - The Projects section re-uses `ProjectsCheckboxSection` — see the
    entry below.
  - The `checked` set for Projects is a Compose-only `mutableStateOf(setOf<String>())`,
    so project filters **do not persist** and are **not** applied to any
    timeline display — purely presentational state in the panel.

### SidePanelSheet.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/sheets/SidePanelSheet.kt`
- **Purpose**: Bottom-sheet navigation drawer.
- **Structure**:
  - 2-page `HorizontalPager`: page 0 = section tab list, page 1 =
    content preview of the currently selected section.
  - Tab list (from `TabSpec`):
    - `Calendar` → route `"timeline"`, icon `Icons.Outlined.CalendarMonth`,
      label "Timeline".
    - `Schedule` → route `"execute"`, icon
      `Icons.Outlined.AccountCircle` (likely a legacy icon), label "Tasks".
    - `Projects` → route `"tiles"`, icon `Icons.Outlined.Folder`,
      label "Projects".
    - `References` → route `"integrations"`, icon `Icons.Outlined.Bookmark`,
      label "References".
    - `Preferences` → route `"settings"`, icon `Icons.Outlined.Tune`,
      label "Preferences".
  - The label/icon mapping is visibly inconsistent (the `execute` route
    is paired with an `AccountCircle` icon).
- **Controls**:
  - Per row: `AppListRow(label, icon, selected=…, role=Role.Tab, onClick)`.
    Click → `selectedIndex = index`, `onNavigate(tabs[index].route)`,
    `overlay.dismiss()`.
  - HorizontalPager swipe to switch between tab-list page and
    content-preview page.
  - `PagerDots(pageCount = 2)` indicator (decorative).
- **HTTP endpoint(s) called**: none. Pure navigation.
- **Data fields shown**: only the 5 hardcoded `TabSpec`s.
- **Locale / i18n**: hardcoded English.
- **Notable**: `onNavigate` is invoked and the sheet is dismissed before any
  of the underlying tab composables have a chance to render. The header
  text is "Navigation" on page 0 and the selected tab's label on page 1.

---

## 3. Overlays / sheets

### PanelSheet.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/sheets/PanelSheet.kt`
- **Purpose**: Generic `ModalBottomSheet` scaffold used by every overlay.
- **Controls**:
  - Material3 `ModalBottomSheet` (sheet drag-to-dismiss + scrim tap).
  - Title text via `MaterialTheme.typography.titleLarge`.
  - Composable content slot.
- **Visuals**: containerColor = `background.copy(alpha=MobileTokens.SurfaceAlpha.strongSelected)`
  (≈0.95); scrimColor = `Black.copy(alpha=MobileTokens.SurfaceAlpha.scrim)`
  (≈0.28). Matches the `feedback_panel_design.md` memory rule.
- **Other**: no HTTP.

### SearchOverlaySheet.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/sheets/SearchOverlaySheet.kt`
- **Purpose**: Searchable list of `EndpointsCatalog.entries`. Mostly a
  developer / debug affordance, not user-facing functionality.
- **Controls**:
  - `OutlinedTextField(value=query, onValueChange=…, label="Search")`.
  - Up to 8 `Text` rows; tap → `overlay.dismiss()` (no action).
- **HTTP endpoint(s) called**: none. All data is the static
  `EndpointsCatalog` enum (`start_tile`, `complete_tile`, ...,
  `read_runtime_state`).
- **Data fields shown**: `EndpointsCatalog.operationId` and `.label`.
- **Locale / i18n**: hardcoded English.
- **Notable**: tap is wired to dismiss — i.e. the search sheet has no
  execution semantics yet.

### NotificationsSheet.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/sheets/NotificationsSheet.kt`
- **Purpose**: Render pending notifications.
- **Controls**: none interactive. `Text` lines from `repository.pending`.
- **HTTP endpoint(s) called**: none via this sheet. The backing
  `NotificationRepository` is currently empty (`MutableStateFlow(emptyList())`);
  the project's memory note records that wiring to
  `ExecutionNotificationCoordinator` was deferred.
- **Data fields shown**: `NotificationItem.label`. Empty state: "No notifications".
- **Locale / i18n**: hardcoded English.

### TileEditSheet.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/sheets/TileEditSheet.kt`
- **Purpose**: Currently a near-empty viewer. Title + lifecycle badge only.
- **Controls**: drag-to-dismiss + tap-outside-to-dismiss. On dismiss it
  calls `viewModel.clearSelectedTile()` and `overlay.dismiss()`. No other
  inputs, no buttons.
- **HTTP endpoint(s) called**: none. The `selectedTile` is sourced from
  `viewModel.tiles + viewModel._selectedTileId` (local combine), which in
  turn came from `V1ApiClient.listTiles()`.
- **Data fields shown**: `tile.title` (as title), `tile.lifecycle` (as
  small body), fallback title "Tile".
- **Locale / i18n**: hardcoded English ("Tile", "—" for missing lifecycle).
- **Notable**: This sheet has no edit affordance — fields like `nextAction`,
  `doneDefinition`, `dueAt`, `project`, etc. exist on `Tile` but are not
  exposed here. Open question (intentional placeholder vs. incomplete
  wiring) — flagging, no fix proposed.

### IntegrationConfigSheet.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/sheets/IntegrationConfigSheet.kt`
- **Purpose**: Per-integration configuration. Google Calendar gets a
  3-option "Sync mode" radio list; everything else shows "Coming soon".
- **Controls**:
  - `ConfigRow` × 3 (id="sync_all"/"sync_primary"/"sync_selected", labels
    "Sync all calendars" / "Sync primary only" / "Sync selected calendar").
    Click → `selected = opt.id`,
    `viewModel.updateGoogleCalendarPolicy(syncMode=opt.id, selectedCalendarId=null)`.
  - Anything else: a passive "Coming soon" `Text`.
- **HTTP endpoint(s) called**: indirect, only for the Google Calendar
  branch — `POST /auth/integrations/settings` carrying
  `{google_calendar: {sync_mode: …}}` via
  `IntegrationRepository.updateGoogleCalendarIntegration(...)` →
  `postSettingsPayload`. After the write the viewModel also calls
  `getCalendarSyncPlanPreview()`.
- **Data fields shown**: nothing beyond the local "selected" radio state.
- **Locale / i18n**: hardcoded English.

### AccountMenuSheet.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/mobile/sheets/AccountMenuSheet.kt`
- **Purpose**: Side panel-style drawer for the user identity.
- **Controls** (each a clickable `AppListRow`):
  - Title: `email.ifBlank { "Signed in" }`.
  - "Account" → `overlay.dismiss()` (no action).
  - "Subscription" → `overlay.show(Overlay.SidePanel(Preferences))`.
  - "Memo" → `overlay.show(Overlay.SidePanel(Schedule))`.
  - "Prompt history" → `overlay.show(Overlay.SidePanel(References))`.
  - "Billing" → `overlay.show(Overlay.SidePanel(Preferences))`.
  - "Sign out" → `overlay.dismiss()` (no actual sign-out call here; the
    file does not invoke `viewModel.signOut()`).
- **HTTP endpoint(s) called**: none in this sheet. `viewModel.email`
  comes from `AuthRepository.authState`.
- **Data fields shown**: just the email.
- **Locale / i18n**: hardcoded English.

---

## 4. ViewModel and dashboard components

### DashboardViewModel.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/dashboard/DashboardViewModel.kt`
- **Hilt-injected deps**: `AuthRepository`, `ProfileRepository`,
  `TileRepository`, `UserSettingsRepository`, `IntegrationRepository`.
- **State flows exposed** (selects):
  - `tiles: StateFlow<List<Tile>>` (default empty)
  - `selectedTile: StateFlow<Tile?>` (combine of `tiles` +
    `_selectedTileId`)
  - `profile: StateFlow<Profile?>`
  - `email: StateFlow<String>`
  - `avatarUrl: StateFlow<String?>`
  - `loading: StateFlow<Boolean>`
  - `error: StateFlow<String?>`
  - `themeMode: StateFlow<ThemeMode>` (mirror of `UserSettingsRepository`)
  - `locale: StateFlow<AppLocale>`
  - `securityLockEnabled: StateFlow<Boolean>`
  - `securityLockTimeoutMinutes: StateFlow<Int>`
  - `timeline: StateFlow<List<CoreTimelineItem>>`
  - `timelineRange: StateFlow<Pair<Instant, Instant>>` (derived from
    `_selectedDay` + `_scale`)
  - `googleCalendarIntegration: StateFlow<GoogleCalendarIntegrationSettings?>`
  - `selectedDay: StateFlow<LocalDate>`
  - `scale: StateFlow<TimelineScale>`
  - `integrations: StateFlow<List<Integration>>` (derived, always one entry)
  - `statsDiagnostics: StateFlow<String>`
  - `daemonStatusSummary: StateFlow<String>`
  - `calendarMonthProjection: StateFlow<CalendarProjectionResponse?>`
  - `calendarSyncPlanPreview: StateFlow<CalendarSyncPlanPreviewResponse?>`
- **`createTimelineRange()`** (file-private `computeTimelineRange`):
  - `Day`: 24h starting at `day.atStartOfDay(zone)`.
  - `Week`: Monday 00:00 → next Monday 00:00.
  - `Month`: first-of-month 00:00 → next month first-day 00:00.
- **Tile CRUD methods** (each launches a coroutine, catches all
  `Exception`, writes to `_error`, then calls `refreshAll()`):
  - `createTile(draft: CreateTileDraft)` →
    `tileRepository.createTile(userId, buildCreatePayload(draft))`
    (no network call if `draft.title` is blank).
  - `startTile(id)`, `completeTile(id)`, `deferTile(id)`,
    `deleteTile(id)` → straight passthrough.
  - `updateDisplayName(name)`, `signOut()`, `clearError()`.
- **Calendar / integration methods**:
  - `connectGoogleCalendar()` →
    `integrationRepository.updateGoogleCalendarConnected(true)`.
  - `disconnectGoogleCalendar()` →
    `updateGoogleCalendarConnected(false)`.
  - `syncGoogleCalendarNow()` →
    `triggerSync()` + `markGoogleCalendarSyncedNow()`.
  - `updateGoogleCalendarPolicy(syncMode, selectedCalendarId)` →
    `updateGoogleCalendarIntegration(...)` then
    `getCalendarSyncPlanPreview()`.
  - `triggerDaemonTick()` → `integrationRepository.triggerTick()`.
  - `resetLocalSyncData()` →
    `integrationRepository.resetLocalSyncData()`.
  - `redownloadRemoteSyncData()` →
    `integrationRepository.redownloadRemoteSyncData()`.
  - `refreshDaemonStatus()` → `refreshDaemonStatusInternal()`.
- **Settings passthroughs**: `setThemeMode(mode)`,
  `setLocale(locale)`, `setSecurityLockEnabled(enabled)`,
  `setSecurityLockTimeoutMinutes(minutes)` (clamped 1..240).
- **Card action handlers** (`handleCardAction`):
  - `TriggerPrompt → triggerPrompt(id)` → `tileRepository.requestPrompt(id)`.
  - `StartTile / CompleteTile / DeferTile / DeleteTile` → tile passthroughs.
  - `StartBreak` → `tileRepository.startBreak(breakMin=5)` (which throws
    `UnsupportedOperationException` per Step 5).
  - `EndBreak` → `tileRepository.endBreak()` (same).
  - `ExtendTile(minutes)` → `tileRepository.extendTile(minutes)` (same).
- **Timeline drag operations**:
  - `rescheduleTimelineItem(item, minuteOffset, zoomScale)` snaps to the
    next 15/5/1 minute boundary and calls
    `tileRepository.rescheduleTile(tileId, startAtIso, endAtIso)` →
    `dispatchTileReschedule` → `POST /v1/placements/{placementId}/changes`.
  - `createTimelineTile(title, startAtIso, endAtIso)` /
    `deleteTimelineTile(id)` are thin wrappers.
- **`refreshAll()`** orchestrates:
  - `_tiles ← tileRepository.getTiles(userId)` →
    `TileRepository.getTiles` → `V1ApiClient.listTiles()` →
    `GET /v1/tiles`.
  - `_profile ← profileRepository.getProfile(userId)` (no HTTP today —
    `ProfileRepository` is a stub).
  - `_timeline ← tileRepository.getTimeline(start, end)` →
    `V1ApiClient.getTimeline(start, end)` →
    `GET /v1/timeline?start=…&end=…`.
  - `_googleCalendarIntegration ← integrationRepository.getSettings()` →
    `GET /auth/integrations/settings`.
  - `_calendarMonthProjection ← integrationRepository.getCalendarMonthProjection()`
    → `GET /views/calendar/month` (optional `?anchor=…`).
  - `_calendarSyncPlanPreview ← integrationRepository.getCalendarSyncPlanPreview()`
    → `GET /auth/integrations/calendar/sync-plan`.
  - Calls `refreshDaemonStatusInternal()` → `GET /sync/status`,
    `GET /v1/runtime/paths`, `GET /auth/tile-quota`.

### TileExtensions.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/dashboard/TileExtensions.kt`
- **Exposes two extension functions**:
  - `fun Tile.isStarted(): Boolean = lifecycle == "Started"`
  - `fun Tile.isDone(): Boolean = lifecycle == "Done"`
- (The other helpers (`isRecurring`, `dueAtDate`, `projectLabel`) live in
  `data/model/TileConditionsExt.kt` and are importable across both
  packages.)

### TileLifecycle.kt
- **Inline enum** in `Tile.kt`:
  - `READY("Ready")`, `STARTED("Started")`, `DONE("Done")`,
    `ARCHIVED("Archived")`.
  - `fromString(value)` default to `READY` on unknown.

### DashboardCards.kt
- **File**:
  `app/src/main/java/app/tastile/android/ui/dashboard/DashboardCards.kt`
- **Types**:
  - `CardAction` (sealed interface):
    `TriggerPrompt(tileId)`, `StartTile(id)`, `CompleteTile(id)`,
    `DeferTile(id)`, `DeleteTile(id)`, `StartBreak`, `EndBreak`,
    `ExtendTile(minutes=10)`.
  - `CardStatus` (enum): `READY`, `STARTED`, `DONE`, `ARCHIVED`.
  - `DashboardCardModel` (sealed):
    `BaseCard(id, title, status, subtitle?, doneDefinition?)`,
    `TimePriorityCard(id, title, status, durationMinutes?, startAtIso?, endAtIso?)`,
    `TimelineCard(id="timeline", title="Timeline", status, items)`.
  - `TimelineItem(tileId, title, status, timestampIso)`.
  - `DashboardCardMapper.buildExecuteCards(tiles)` and
    `buildTileCards(tiles)`.
- **Screens**: `ExecuteDashboardScreen`, `TilesDashboardScreen` —
  basically `AppScreenTitle` + `LazyColumn` of cards rendered by
  `DashboardCardRenderer`. (These composables are not the ones the mobile
  shell mounts; they live alongside but are referenced elsewhere — they
  read `loading`, `locale`, `viewModel.build*Cards()`, dispatch through
  `viewModel.handleCardAction`.)
- **HTTP**: none directly; depends on what `viewModel.build*Cards()` saw.

### Other dashboard components
- `AutoCompleteTextField.kt` — `ExposedDropdownMenuBox` wrapping an
  `OutlinedTextField` with a suggestions list. Used by `QuickCreateSheet`.
- `DurationInput.kt` — read-only `OutlinedTextField` formatted `HH:MM` plus
  a `IconButton(Icons.Default.Schedule)` that opens `DurationPickerDialog`.
- `DurationPickerDialog.kt` — Material3 `AlertDialog` with `IconButton` ▲/▼
  spinners for hours (0–99, step 1) and minutes (0–59, step 5), four
  preset `FilterChip`s ("15m"/"25m"/"45m"/"1h"), `TextButton OK` /
  `Cancel`.
- `HelpBadge.kt` — `?` `IconButton` revealing a `DropdownMenu` with
  `helpText`. Used by `SectionBlock`.
- `PickerDialogs.kt` — generic `PickerDialog<T>` (radio list inside a
  `selectableGroup`), plus thin helpers `LocalePickerDialog`,
  `ThemePickerDialog`, `TimeoutPickerDialog`.
- `SectionBlock.kt` — vertical section header + slot for
  `ColumnScope` content; optional `helpText` shows a `HelpBadge`.

---

## 5. Data models

### Tile.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/model/Tile.kt`
- **Class**:
  ```kotlin
  @Serializable
  data class Tile(
      val id: String = "",
      @SerialName("user_id") val userId: String = "",
      @SerialName("local_tile_id") val localTileId: String = "",
      val title: String = "",
      @SerialName("next_action") val nextAction: String? = null,
      @SerialName("done_definition") val doneDefinition: String? = null,
      @SerialName("temporal_conditions") val temporalConditions: JsonObject? = null,
      @SerialName("objective_conditions") val objectiveConditions: JsonObject? = null,
      @SerialName("interruption_conditions") val interruptionConditions: JsonObject? = null,
      @SerialName("automation_conditions") val automationConditions: JsonObject? = null,
      val lifecycle: String = "Ready",
      @SerialName("annotation_conditions") val annotationConditions: JsonObject? = null,
      @SerialName("created_at") val createdAt: String? = null,
      @SerialName("updated_at") val updatedAt: String? = null,
      @SerialName("local_created_at") val localCreatedAt: String? = null,
      @SerialName("local_updated_at") val localUpdatedAt: String? = null,
      @SerialName("deleted_at") val deletedAt: String? = null,
  )
  ```
- **Distinct `lifecycle` strings**: `"Ready"`, `"Started"`, `"Done"`,
  `"Archived"` (canonical values via `TileLifecycle` enum).
- **Note**: The model is not what the v1 backend speaks in the live
  `getTiles` flow — `V1ApiClient.listTiles()` returns `TileView`, mapped
  via `V1Mappers.toTile(...)` which only carries through
  `id / userId / localTileId / title / lifecycle`. Most of the
  `*Conditions` slots are unused in the UI today (only `temporal_conditions`
  and `annotation_conditions` are queried by `TileConditionsExt.kt`).

### Integration.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/model/Integration.kt`
- **Class**:
  ```kotlin
  data class Integration(
      val id: String,
      val name: String,
      val connected: Boolean,
  )
  ```
- (Used only as the UI-side projection in
  `DashboardViewModel.integrations`.)

### Profile.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/model/Profile.kt`
- **Class**:
  ```kotlin
  @Serializable
  data class Profile(
      val id: String = "",
      @SerialName("display_name") val displayName: String? = null,
      @SerialName("avatar_url") val avatarUrl: String? = null,
      val plan: String = "free",
  )
  ```
- **Enum** `Plan { FREE("free"), PRO("pro") }` with
  `Plan.fromString(value)`.
- (Note: `ProfileRepository` is currently a stub — no HTTP read returns
  a `Profile`.)

### TileConditionsExt.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/model/TileConditionsExt.kt`
- **Functions**:
  - `Tile.projectLabel(): String?` — looks for `"project:NAME"` token in
    `annotationConditions["project"]` and
    `annotationConditions["labels"]` (Regex
    `"\"project:([^\"]+)\""`).
  - `Tile.dueAtDate(): String?` — returns the YYYY-MM-DD prefix of
    `temporalConditions["due_at"]` or `annotationConditions["due_at"]`.
  - `Tile.isRecurring(): Boolean` — true if either `annotationConditions`
    or `temporalConditions` contains a `"recurrence"` key.

### V1Models.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/api/V1Models.kt`
- Notable bodies:
  - `TileView(id, kind: Byte, ownerId, externalId?, content: TileContentView(title, note), visual: TileVisualView(color?, icon?), revision: Long)`.
  - `V1ListTilesResponse(tiles: List<TileView>)`.
  - `TimelineItem(placementId, revision, content, visual, role: Byte, span: Span(startAt,endAt), inside: PlacementInsideView?, source: PlacementSourceView(value), resolution: ResolutionInfoView)`.
  - `V1TimelineResponse(items: List<TimelineItem>)`.
  - `TileDetailView(id, kind: Byte, ownerId, revision, title, description?, color?, icon?, externalId?, planId?, archivedAt?)` (flat schema used by `GET /v1/tiles/{id}`).
  - `V1PlacementListItem(placementId, tileId, planId?, title, spanStart?, spanEnd?)`.
  - `V1ExecutionView(id, tileId, ownerId, revision, state: Byte, placementId?)`.
  - `RuntimePathView(id, profileName, appDataDir, dbPath, sessionPath, daemonStartupLogPath="", daemonExecutablePath="")` + `V1ListRuntimePathsResponse`.

### V1CommandPayloads.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/api/V1CommandPayloads.kt`
- Notable bodies:
  - `CreateTilePayload(kind: Byte, title, description?, color?, icon?, externalId?, planRole: Byte)`.
  - `ArchiveTilePayload(tileId)`.
  - `UpdateTilePayload(tileId, title?, description?, color?, icon?, externalId?)`.
  - `SetTileLifecyclePayload(tileId, state: Short?, deferredUntil?, completedAt?, bumpExtend: Boolean=false)`.
  - `AttachMemoPayload(tileId, body)`.
  - `StartTilePayload(tileId, planId: String, source: Byte, sourceRef?, baseline: StartTileBaseline(startAt,endAt))`.
  - `PauseExecutionPayload / ResumeExecutionPayload(executionId)`.
  - `AppendChangesPayload(placementId, changeset: JsonObject)`.
  - `CreatePromptRequestPayload(kind: Short, payload: JsonObject)` (raw body, no envelope).

### V1ApiToken.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/api/V1ApiToken.kt`
- `V1ApiTokenCreateRequest(label?, scopes: List<String>)`,
  `V1ApiTokenCreateResponse(token, tokenId, label?, scopes, createdAt?, expiresAt?)`,
  `V1ApiTokenView(id, label?, scopes, createdAt?, lastUsedAt?, expiresAt?, revokedAt?)`,
  `V1ListApiTokensResponse(tokens: List<V1ApiTokenView>)`.

### V1Error.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/api/V1Error.kt`
- `sealed class V1Error { Network(cause), Auth(), Unknown(status, body), Api(kindValue, kindName, message, currentRevision?) }` plus `fromApiBody(V1ApiErrorBody)` mapping numeric `kind` (Short) to a name string.

### V1Mappers.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/api/V1Mappers.kt`
- `TileView.toTile(userId)` and
  `V1ListTilesResponse.toTiles(userId)` — derives `lifecycle` from the
  v1 numeric `kind` byte:
  `EXECUTION → "Started"`, `PLACEMENT / RECURRING / unknown → "Ready"`.

### V1Idempotency.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/api/V1Idempotency.kt`
- Generates UUIDv7-shaped strings (`SecureRandom`-based) used in the
  `idempotencyKey` envelope field and `Idempotency-Key` header.

### V1NumericConstants.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/api/V1NumericConstants.kt`
- Container object holding the byte / Short numeric registries for
  `TileKind`, `PlanRole`, `PlacementSource`, `ExecutionState`,
  `ExecutionSegmentKind`, `CommandResult`, `ApiErrorKind`, `ActorKind`,
  `AggregateKind`, `ResolutionState`, `ChangeLayer`, `ChangeKind`,
  `ChangeSource`, `MergeMode` — all matching the v1 backend's smallint
  registry (per spec v1/10 §2).

---

## 6. Repositories and HTTP entry points

### TileRepository.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/TileRepository.kt`
- **Constructor deps**: `ExecutionNotificationCoordinator`,
  `EventRepository`, `CurrentUserProvider`, `V1ApiClient`,
  `V1CommandDispatcher`.
- **Read methods**:
  - `suspend fun getTiles(userId: String): List<Tile>` —
    v1-first, fails silently to empty list (`latestReadDiagnostics`
    holds the reason string).
  - `suspend fun getTiles(viewMode, lifecycle?, limit?, search?): TilesResponse`
    — client-side filter over `getTiles(userId)`.
  - `suspend fun getTilesInProgress(): TilesInProgressResponse`,
    `getActiveTile(): ActiveTileResponse`,
    `getExecution(): ExecutionResponse`,
    `getExecutionView(): ExecutionViewResponse` — all currently fall
    through to `null` snapshot data because `currentSnapshotOrNull()` is
    permanently null (the v0 core runtime was removed in Step 5; see
    comments inline).
  - `suspend fun getEditableTileById(tileId): Tile?` → `getTileById`.
  - `suspend fun getTileById(tileId): Tile?` — looks in cache, else
    re-issues `v1ApiClient.listTiles()`.
  - `suspend fun getTodayTimelineView(): TimelineTodayResponse`.
  - `suspend fun getTimeline(start, end): List<CoreTimelineItem>` —
    v1 first, then cloud-tiles fallback (`buildTimelineFromTiles(...)`).
- **Command methods** (delegate to `V1CommandDispatcher`):
  - `createTile(userId, title)` / `createTile(userId, payload)`.
  - `startTile(id)` → `dispatchTileStart` (throws if v1 tile lacks
    `plan_id`).
  - `completeTile(id)` → `dispatchTileComplete`.
  - `deferTile(id, reason?, minutes?)` → `dispatchTileDefer`.
  - `deleteTile(id)` → `dispatchTileDelete`.
  - `pauseTile(id)` → `dispatchTilePause`.
  - `continueTile(id)` → `dispatchTileContinue`.
  - `rescheduleTile(id, startAtIso, endAtIso)` →
    `dispatchTileReschedule`.
  - `startBreak(breakMin, insertionMode?)` and `endBreak()` — both throw
    `UnsupportedOperationException` (v1 has no break endpoint).
  - `extendTile(extendMin)` — throws
    `UnsupportedOperationException` (no `tile_id` from v0 callers).
  - `updateTile(id, payload)` → `dispatchTileUpdate`.
  - `requestPrompt(id)` → `dispatchPromptRequest`.
  - `respondStartupRecoveryPrompt(...)` →
    `dispatchStartupRecoveryPrompt`.
  - `attachMemo / saveMemo(tileId, note)` → `dispatchMemoAttach`.
- **Diagnostics**: `latestReadDiagnostics()` — string like
  `source=v1 count=N user_match=true` or
  `source=v1_unavailable count=0 user_match=true`.
- **Error handling**: read path returns empty list on `V1Error` or
  generic `Exception`; command paths throw `IllegalStateException` /
  `UnsupportedOperationException`. The viewModel catches and writes to
  `_error`.

### V1CommandDispatcher.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/command/V1CommandDispatcher.kt`
- **Method / endpoint / HTTP details**:
  - `dispatchTileCreate(payload, userId)` →
    `POST /v1/tiles` with `kind=CreateTile` envelope carrying
    `CreateTilePayload(kind, title, description?, …, planRole)`.
  - `dispatchTileDelete(id)` →
    `DELETE /v1/tiles/{id}` (CommandResponse envelope).
  - `dispatchTileUpdate(id, payload)` →
    `POST /v1/tiles/{id}/update` (`UpdateTile` envelope).
  - `dispatchTileComplete(id, nextTileId?, scope?)` →
    `POST /v1/tiles/{id}/complete` (`SetTileLifecycle` with
    `state=2, completed_at=now`).
  - `dispatchTileDefer(id, reason?, minutes?)` →
    `POST /v1/tiles/{id}/defer` (`SetTileLifecycle` with
    `state=1, deferred_until=now+60min` default).
  - `dispatchTileExtend(deltaMin)` — always returns `null` (Step 5
    placeholder).
  - `dispatchMemoAttach(tileId, body)` →
    `POST /v1/tiles/{id}/memos` (`AttachMemo` envelope).
  - `dispatchTileStart(id)` →
    1. `GET /v1/tiles/{id}` (via `readTile`) — looks up `plan_id`.
    2. `POST /v1/tiles/{id}/start` (`StartTile` envelope with
       `planId, source=MANUAL, sourceRef=null, baseline={start_at:now,end_at:now}`).
    Throws `IllegalStateException` when the tile has no `plan_id`.
  - `dispatchTilePause(id)` /
    `dispatchTileContinue(id)` →
    looks up an active execution (currently always returns `null` per
    inline TODO — `findActiveExecutionIdForTile` cannot translate from
    `tile_id`); else
    `POST /v1/executions/{id}/pause` or
    `/resume` (`PauseExecution` / `ResumeExecution` envelope). Always
    throws today.
  - `dispatchTileReschedule(tileId, startAt, endAt)` →
    looks up placement via `listPlacements()` →
    `POST /v1/placements/{placementId}/changes`
    (`AppendChanges` envelope with a PLACEMENT-layer ChangeSet of two
    SET/Span items).
  - `dispatchPromptRequest(tileId)` →
    `POST /v1/prompts` raw JSON
    (`{kind:0, payload:{tile_id:…}}`), parses the returned
    `{id: <uuidv7>}` into a synthetic `CoreCommandAck`.
  - `dispatchStartupRecoveryPrompt(promptId, tileId, actionId, stopAtIso?)` →
    `POST /v1/prompts/startup-recovery` raw JSON.
- All responses are decoded into `CommandResponse` (envelope with
  `command_id, accepted_at, aggregate, revision, result: Byte,
  pending`). The dispatcher translates `result == 0/1` to `accepted=true`
  in the v0 `CoreCommandAck`.

### V1ApiClient.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/api/V1ApiClient.kt`
- **Auth**: `tokenProvider: suspend () -> String?` is wired in `ApiModule`
  to `ApiTokenManager.getOrMint()` (which mints on first use via the
  "web bridge" headers when `BuildConfig.TASTILE_WEB_BRIDGE_SECRET` is set,
  otherwise it falls back to minting with the Cognito `id_token`).
- **Base URL**: `BuildConfig.TASTILE_CORE_URL` (trimmed trailing slash).
- **Connection details**: `HttpURLConnection`, 15s connect / 15s read
  timeouts, `Accept: application/json` and `Authorization: Bearer $token`.
- **Methods**:
  - `get<T>(path)` — private reified inline helper.
  - `listTiles(): V1ListTilesResponse` → `GET /v1/tiles`.
  - `readTile(tileId): TileDetailView` → `GET /v1/tiles/{id}`.
  - `listPlacements(): List<V1PlacementListItem>` → `GET /v1/placements`.
  - `getExecution(executionId): V1ExecutionView` →
    `GET /v1/executions/{id}`.
  - `getTimeline(start, end): V1TimelineResponse` →
    `GET /v1/timeline?start=…&end=…` (ISO encoded with `URLEncoder`).
  - `listRuntimePaths(): V1ListRuntimePathsResponse` →
    `GET /v1/runtime/paths`.
  - `postCommand(path, commandKind, payload, …, responseSerializer, …, expectedRevision?)`
    — wraps `{expectedRevision, idempotencyKey, occurredAt, payload:{kind,value:…}}`,
    POSTs to `path`. Used by `V1CommandDispatcher` for all v1 typed
    commands. Returns the deserialized `Resp`.
  - `postRawJson(path, body: JsonObject, responseSerializer)` —
    bypasses the envelope, still adds an `Idempotency-Key` header. Used
    for `/v1/prompts` and `/v1/prompts/startup-recovery`.
  - `deleteCommand(path, responseSerializer)` —
    `DELETE {path}` with bearer header; envelope decoder. Used for
    `tile.delete`.
  - `mintApiToken(bootstrapToken, request)` — `POST /v1/api-tokens`
    with `Authorization: Bearer <cognito_id_token>` (the bootstrap
    credential). Returned once, the raw `token` is then used for all
    subsequent calls.
  - `mintApiTokenViaBridge(bridgeSecret, userSub, request)` —
    same path, but with headers
    `x-tastile-web-bridge-secret` + `x-tastile-web-session-user: <cognito_sub>`
    and no `Authorization`. The v1 daemon prefers the bridge headers and
    ignores auth.

### IntegrationRepository.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/IntegrationRepository.kt`
- **Note**: Despite the "IntegrationRepository" name, every method except
  `getRuntimePaths` and `streamStateEvents` actually targets the **v0
  CoreRuntimeService HTTP daemon** (auth daemon, port `3140`), not
  `/v1/...`. This is the legacy daemon-side surface the viewModel talks
  to for calendar plumbing.
- **Connection details**:
  - `executeDaemonRequest(path, method, payload?, withContentType=true,
    requiresAuth=true)`:
    - `Authorization: Bearer <AuthRepository.currentIdToken()>` (the
      Cognito `id_token`, **not** the Tastile API token).
    - `Content-Type: application/json` (toggleable).
    - `runWithDaemonFallback(...)` tries each candidate in
      `daemonBaseCandidates()` (last-successful URL, plus
      `BuildConfig.TASTILE_CORE_URL`, plus
      `http://127.0.0.1:3140`, `http://10.0.2.2:3140`), catching
      `ConnectException`, `SocketTimeoutException`, `UnknownHostException`
      on each.
    - 15s connect / 15s read timeouts.
- **Methods / endpoints** (daemon-side):
  - `getSettings()` → `GET /auth/integrations/settings`.
  - `updateGoogleCalendarConnected(connected)` →
    `POST /auth/integrations/settings`.
  - `markGoogleCalendarSyncedNow()` →
    `POST /auth/integrations/settings` (only `last_synced_at`).
  - `updateGoogleCalendarIntegration(connected?, canRead?, canWrite?,
    accountEmail?, selectedCalendarId?, syncMode?, readPolicy?,
    writePolicy?, lastSyncedAt?)` →
    `POST /auth/integrations/settings`.
  - `getCalendarSyncPlanPreview()` →
    `GET /auth/integrations/calendar/sync-plan`.
  - `getCalendarMonthProjection(anchor?)` →
    `GET /views/calendar/month` (`?anchor=…`).
  - `triggerSync()` → `POST /sync/trigger`.
  - `triggerTick()` → `POST /commands/tick`.
  - `resetLocalSyncData()` → `POST /sync/recovery/reset-local`.
  - `redownloadRemoteSyncData()` → `POST /sync/recovery/redownload-remote`.
  - `getSyncStatus()` → `GET /sync/status`.
  - `getRuntimePaths()` → calls
    `V1ApiClient.listRuntimePaths()` → `GET /v1/runtime/paths` directly.
  - `getTileQuota()` → `GET /auth/tile-quota`.
  - `getSession()` → `GET /auth/session` (`requiresAuth = false`).
  - `restoreSession(...)` → `POST /auth/session/restore`.
  - `startOAuth(provider)` → `POST /auth/oauth/start`.
  - `signInWithOAuth(provider, code, redirectUri?, state?)` →
    `POST /auth/oauth/exchange`.
  - `startBrowserAuth(provider)` → alias of `startOAuth(...).authUrl`.
  - `isAuthenticated()` → checks `getSession()` then
    `authRepository.currentIdToken()`.
  - `checkHealth()` → `GET /health`.
  - `getEventsRaw()` → `GET /debug/events`.
  - `streamStateEvents(): Flow<String>` — internal v1 polling flow
    emitting the JSON of `GET /v1/timeline?start=now-24h&end=now+24h`
    on a 30s cadence.

### ApiTokenManager.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/ApiTokenManager.kt`
- **Constructor deps**: `ApplicationContext`, `Lazy<V1ApiClient>`,
  `CurrentUserProvider`.
- **Storage**: `EncryptedTokenStorage.apiTokenPrefs(context)` —
  `EncryptedSharedPreferences` keyed `tastile_v1_api_tokens` with
  `api_token`, `token_id`, `label`, `minted_at` entries.
- **Method**:
  - `suspend fun getOrMint(): String?` — guarded by a `Mutex`. If
    `BuildConfig.TASTILE_WEB_BRIDGE_SECRET` is set (and the user has a
    `user_sub`), calls
    `V1ApiClient.mintApiTokenViaBridge(secret, userSub, …)`. Otherwise
    falls back to `V1ApiClient.mintApiToken(bootstrap=cognitoIdToken,
    …)`. Persists the response and returns the raw token. Returns `null`
    on any failure.
  - `invalidate()` / `signOut()`.
- **Used by**: `ApiModule.provideV1ApiClient(...)` — i.e. every `GET` /
  `POST` / `DELETE` against the v1 API carries this token.

### AuthRepository.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/AuthRepository.kt`
- **Implements**: `CurrentUserProvider`, `AuthRepositoryContract`.
- **Storage**: `EncryptedTokenStorage.cognitoPrefs(context)` —
  `tastile_cognito_auth` (keys: `pkce_verifier`, `oauth_state`, `id_token`,
  `access_token`, `refresh_token`, `user_id`, `email`).
- **Cognito bootstrap**:
  - `signInWithCognito(context)` /
    `signInWithGoogle(context)` build the hosted-UI URL via
    `CognitoAuthStartUrlBuilder` (PKCE S256, random `state`), write
    `pkce_verifier` / `oauth_state` to prefs, and launch the
    `Intent.ACTION_VIEW` browser flow.
  - `handleDeepLink(intent)` recognises callbacks on
    `BuildConfig.COGNITO_REDIRECT_URI` or
    `https://app.tastile.app/auth/callback`. Accepts either:
    1. **Implicit**: `#id_token=…&access_token=…&state=…` →
       `importCognitoTokenSession` (just parses + stores).
    2. **Authorization code**: `?code=…&state=…` →
       `exchangeCognitoCodeForSession` →
       `POST https://{domain}.auth.{region}.amazoncognito.com/oauth2/token`
       with `grant_type=authorization_code`, `client_id`, `code`,
       `redirect_uri`, `code_verifier`. Stores the returned
       `id_token / access_token / refresh_token / user_id / email` and
       publishes `TastileAuthState.Authenticated(…)`.
- **Refresh**:
  - `currentIdToken()` returns the cached `id_token` if not expired
    (JWT `exp` checked locally) else calls
    `refreshCognitoSessionOrNull()` →
    `POST https://{domain}.auth.{region}.amazoncognito.com/oauth2/token`
    with `grant_type=refresh_token`, `client_id`, `refresh_token`.
    On 400–499 it clears prefs and flips `_authState` to `Unauthenticated`.
- **Token parse**:
  - `isJwtExpired(idToken)` checks `exp <= now + 30s`.
  - `parseIdTokenClaims(idToken)` decodes base64-url-no-padding payload
    for `sub` / `email`.
- **Sign out**: clears Cognito prefs, calls `ApiTokenManager.signOut()`,
  sets `_authState = Unauthenticated`.
- **No JWT auth via Tastile API token** — that path is owned by
  `ApiTokenManager`.

### ProfileRepository.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/ProfileRepository.kt`
- **Methods**:
  - `suspend fun getProfile(userId): Profile?` returns
    `Profile(id = userId)` — no network call, no real lookup.
  - `suspend fun updateDisplayName(userId, displayName): Profile?`
    returns `Profile(id = userId, displayName = displayName)` — local
    echo only.
- (Profile data isn't fetched anywhere; `viewModel.refreshAll()` calls
  this and discards everything except `avatarUrl`.)

### EventRepository.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/EventRepository.kt`
- **Methods** (`@Singleton`, but no-ops today):
  - `suspend fun loadAll(userId): List<EventRow>` → `emptyList()`.
  - `suspend fun append(...) = Unit`.
  - `suspend fun appendEmittedEvent(...) = Unit`.
- The shape exists for future use; nothing actually writes events.

### UserSettingsRepository.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/UserSettingsRepository.kt`
- Backing store: `SharedPreferences("tastile-user-settings")` (regular
  SharedPreferences, not the encrypted one).
- Keys: `theme_mode`, `locale`, `security_lock_enabled`,
  `security_lock_timeout_minutes`, `security_lock_left_at_millis`.
- Methods: `getThemeMode() / setThemeMode(mode)`,
  `getLocale() / setLocale(locale)`,
  `getSecurityLockEnabled() / set(...)`,
  `getSecurityLockTimeoutMinutes() / set(minutes)` (clamped 1..240),
  `recordSecurityLockLeftAt(nowMillis)`,
  `shouldRequireSecurityUnlock(...)` defers to
  `SecurityLockPolicy.shouldRequireUnlock(...)`.
- Defaults: `theme_mode = "dark"`, `locale = "ja"`,
  `security_lock_enabled = true`, `security_lock_timeout_minutes = 10`.
- Enums `ThemeMode { LIGHT, DARK }`, `AppLocale { JA, EN }`.

### EncryptedTokenStorage.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/EncryptedTokenStorage.kt`
- Two `EncryptedSharedPreferences` files:
  - `tastile_cognito_auth` — Cognito tokens (PKCE verifier, state,
    id_token, access_token, refresh_token, user_id, email).
  - `tastile_v1_api_tokens` — Tastile API token (raw `api_token`,
    `token_id`, `label`, `minted_at`).
- Failure mode: if Keystore is unavailable, throws
  (Robolectric-only fallback to plain prefs).
- Excluded from cloud-backup via `res/xml/backup_rules.xml` +
  `android:allowBackup="false"`.

### CurrentUserProvider.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/CurrentUserProvider.kt`
- Interface with `currentUserId()` and `currentIdToken()` (returns `null`
  default).

### TastileAuthState.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/TastileAuthState.kt`
- Sealed: `Loading`, `Unauthenticated`, `Authenticated(userId, email?, idToken, accessToken, refreshToken?)`.

### CognitoAuthStartUrlBuilder.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/CognitoAuthStartUrlBuilder.kt`
- Composes
  `{webAuthBaseUrl}/login?redirect_uri=…&state=…&code_challenge=…&platform=…`
  or
  `{webAuthBaseUrl}/auth/cognito/login?provider=…&…` (with `URLEncoder`).

### AuthRepositoryContract.kt
- **File**:
  `app/src/main/java/app/tastile/android/data/repository/AuthRepositoryContract.kt`
- Interface: `authState`, `signInWithCognito`, `signInWithGoogle`,
  `signOut`, `currentIdToken`.

---

## 7. Background subsystems — alarms + notifications

### ExecutionAlarmScheduler.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/ExecutionAlarmScheduler.kt`
- Uses `AlarmManager`. Shared prefs `tastile-exact-alarms` track currently
  scheduled alarm IDs.
- **Methods**:
  - `suspend fun rescheduleFromCurrentState()` →
    `currentSnapshotOrNull()?.let(::reschedule) ?: cancelAll()`.
    `currentSnapshot()` calls `CoreRuntimeService.currentSnapshot()`
    (the legacy v0 native bridge); if any `CoreBridgeError` is thrown it
    returns `null` and all alarms are cancelled.
  - `suspend fun isAlarmStillRelevant(alarmId)`.
  - `fun reschedule(snapshot)` → `cancelAll()` then `ExecutionAlarmPlanner.plan(snapshot, now)` →
    `schedule(...)`.
  - `fun cancelAll()`.
- **No HTTP endpoints**.

### ExecutionAlarmPlanner.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/ExecutionAlarmPlanner.kt`
- `enum AlarmTriggerType { PROMPT, FIXED_START }`. Builds up to 32
  alarms from a `CoreSnapshot`: `phase-end` (PROMPT) + per-tile
  `fixed-start` (FIXED_START). No HTTP.

### ExecutionAlarmReceiver.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/ExecutionAlarmReceiver.kt`
- `@AndroidEntryPoint BroadcastReceiver`. On receive:
  - reads `alarmId` extras, calls
    `ExecutionAlarmScheduler.isAlarmStillRelevant(...)` (uses
    `CoreRuntimeService.currentSnapshot()`).
  - Looks at `AlarmTriggerType` extras and (fallback) `tileTitle`.
  - Builds the alarm title/body string (JA/EN).
  - Launches `ExecutionAlarmActivity` directly.
  - Calls `scheduler.rescheduleFromCurrentState()`.

### ExecutionAlarmRescheduleReceiver.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/ExecutionAlarmRescheduleReceiver.kt`
- Receives `BOOT_COMPLETED`, `TIME_CHANGED`, `TIMEZONE_CHANGED`,
  `MY_PACKAGE_REPLACED`, exact-alarm permission state changes →
  `scheduler.rescheduleFromCurrentState()`.

### ExecutionAlarmTestReceiver.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/ExecutionAlarmTestReceiver.kt`
- Plain `BroadcastReceiver` started from `postTestAlarm(...)` in
  `SettingsScreen.kt`. Reads `EXTRA_TITLE/BODY/NOTIFICATION_ID` extras and
  launches `ExecutionAlarmActivity` directly.

### ExecutionAlarmActivity.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/ExecutionAlarmActivity.kt`
- Full-screen `ComponentActivity` for the alarm UI: `setShowWhenLocked`,
  `setTurnScreenOn`, `requestDismissKeyguard`, system-bars
  hidden (`WindowInsetsController.hide(systemBars())` with
  `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`).
- Plays looping alarm audio (`MediaPlayer` with default alarm
  `RingtoneManager.TYPE_ALARM`) and a vibration waveform
  (`longArrayOf(0, 600, 350, 600, 900), 0`).
- Renders `AlarmSurface` — a Surface with a Canvas that draws a
  progress circle scaled by drag distance; `detectDragGestures`
  dismisses when `progress >= 0.82f` (or `hypot(dragX,dragY) >= 260f`
  during drag). On dismiss → cancels the system notification + finishes
  the activity.
- Constants: `EXTRA_TITLE`, `EXTRA_BODY`, `EXTRA_NOTIFICATION_ID`,
  `DEFAULT_NOTIFICATION_ID = 492`.

### ExecutionNotificationChannels.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/ExecutionNotificationChannels.kt`
- Three channels (created via `NotificationManager.createNotificationChannel`):
  - `STATUS` = `"execution-status"` (LOW, no sound).
  - `ALERTS` = `"execution-alerts"` (HIGH, prompts).
  - `ALARMS` = `"execution-alarms-v2"` (HIGH + default alarm sound +
    vibration).

### ExecutionNotificationCoordinator.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/ExecutionNotificationCoordinator.kt`
- Used by `TileRepository.refreshCloudCacheAfterCommand(...)` via `syncOnce()`.
- Reads `CoreRuntimeService.currentSnapshot()` (the legacy core bridge).
- Builds notification content with JA/EN branching (kind = `end_break` /
  `start_tile` / default → "Decision required").
- Posts the notification with id `ALERT_NOTIFICATION_ID = 402` to channel
  `ALERTS`.

### ExecutionNotificationPolicy.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/ExecutionNotificationPolicy.kt`
- Pure decision logic: thresholds
  `WORK_GENTLE_THRESHOLD_MIN = 15`, `WORK_INTERVENTION_THRESHOLD_MIN = 25`.
  Maps `(execution, now, emittedMilestones)` →
  `ExecutionNotificationDecision(milestone?, elapsedMinutes, targetMinutes?, statusTitle, statusText, milestoneKey?)`.
- Title / text: `"Break"` / `"Executing"`; status text uses
  `"${tile.title}  ${elapsed}/${target} min"` or
  `"${tile.title}  ${elapsed} min"`.

### NotificationRepository.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/NotificationRepository.kt`
- `@Singleton MutableStateFlow<List<NotificationItem>>` — currently
  always empty. Wired to `NotificationsSheet` via a
  `NotificationsViewModel` (in `OverlayLayer.kt`).
- **Flag**: `pending` is never populated in the production code path; the
  `NotificationsSheet` empty state is the only state observed.

### NotificationItem.kt
- **File**:
  `app/src/main/java/app/tastile/android/notifications/NotificationItem.kt`
- `data class NotificationItem(val label: String)`.

---

## 8. Auth flow summary (where the bearer comes from)

1. User taps the "Sign in with Cognito" affordance → `AuthRepository`
   opens the hosted-UI URL (PKCE S256, random `state`), stored in
   `EncryptedSharedPreferences("tastile_cognito_auth")`.
2. Cognito redirects back to `BuildConfig.COGNITO_REDIRECT_URI` or
   `https://app.tastile.app/auth/callback`. Either:
   - The implicit-grant tokens arrive in the URL fragment → `AuthRepository`
     parses and stores them.
   - Or an authorization code arrives in the query → `AuthRepository`
     posts to
     `https://{domain}.auth.{region}.amazoncognito.com/oauth2/token`
     (`grant_type=authorization_code`) to mint tokens.
3. `AuthRepository.authState` is published as
   `TastileAuthState.Authenticated(userId, email?, idToken,
   accessToken, refreshToken?)`.
4. **First v1 call**: `V1ApiClient` reads the
   `AuthTokenProvider = ApiTokenManager.getOrMint()`. If `cachedToken` is
   blank, `ApiTokenManager` calls
   `V1ApiClient.mintApiTokenViaBridge(secret, cognito_sub, …)` (when the
   `TASTILE_WEB_BRIDGE_SECRET` Gradle property is set) or
   `V1ApiClient.mintApiToken(bootstrapToken = cognitoIdToken, …)`. The
   resulting raw token is cached under
   `EncryptedSharedPreferences("tastile_v1_api_tokens")`.
5. Every subsequent `V1ApiClient.get / postCommand / deleteCommand /
   postRawJson` attaches `Authorization: Bearer $token` (the Tastile
   API token, **not** the Cognito `id_token`).
6. **IntegrationRepository** is the exception: it uses
   `Authorization: Bearer ${authRepository.currentIdToken()}` (the
   Cognito `id_token` directly) against the legacy daemon endpoints.
7. **Token refresh**: `AuthRepository.currentIdToken()` checks JWT
   `exp`. When within 30s of expiry, it
   `POST https://{domain}.auth.{region}.amazoncognito.com/oauth2/token`
   with `grant_type=refresh_token`. On 400–499 it clears the prefs and
   publishes `Unauthenticated`.

---

## 9. Known gaps in the audit

- The `integration` data model (`Integration.kt`) is consumed exclusively
  from `DashboardViewModel.integrations`. Although the
  `IntegrationsScreen` lists "Outlook Calendar", "Apple Calendar", "Slack",
  "Notion", none of those have data backing them. They are
  `IntegrationStub`s hardcoded in `IntegrationsScreen.kt`.
- `TileEditSheet.kt` only displays `title` and `lifecycle`. Fields that
  exist on `Tile` (and on the v1 backend) — `nextAction`,
  `doneDefinition`, due/start/end timestamps, project — are not exposed
  in this sheet (the deeper editing surface, if any, lives in
  `QuickCreateSheet.kt`, which is for creation only).
- The `Calendar create` affordance noted in `feedback_calendar_create_entry_point.md`
  is satisfied in the timeline / monthly calendars, not via the tabs we
  audited here.
- `EndpointsCatalog` exposes 23 endpoint names but `SearchOverlaySheet`
  treats them as developer-facing — there is no per-endpoint
  dispatcher; tapping any of them only dismisses the sheet.
- `AccountMenuSheet`'s "Sign out" row only calls `overlay.dismiss()`. It
  does not actually sign out via `viewModel.signOut()` (search the
  composable — no `signOut` call). Flagged but no editorial comment.
- `NotificationRepository.pending` is wired to `NotificationsSheet` but
  never populated. Production notifications come from
  `ExecutionNotificationCoordinator.notify(... id=402)` with the legacy
  v0 snapshot path, and from `postTestNotification` in `SettingsScreen`.
- `ProfileRepository.updateDisplayName` and `getProfile` are stubs. They
  return local projections without any HTTP call.
- `EventRepository` is a stub (`= Unit` / `emptyList()`).
- `PanelSheet` exposes no controls of its own; it is the shared scaffold
  for every other overlay.
