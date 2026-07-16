# Android UI Misuse Redesign — Design

**Date:** 2026-07-16
**Status:** Draft
**Scope:** `app/src/main/java/app/tastile/android/ui/mobile/**` (and shared `designsystem/`)

---

## 1. Context

After installing `tastile-android` v0.3.0 on the Redmi XIG03, the user reported that the app feels overwhelmingly text-heavy and hard to use. Closer inspection surfaced two intertwined problems:

1. **MUI / Material 3 components are mis-applied.** Tappable rows render as raw `Row.clickable{}` with no shape, elevation, or ripple — so users cannot tell what is interactive. Important secondary actions use `TextButton` (blue text only) when `FilledTonalButton` is the right M3 affordance. Free-form inputs (`OutlinedTextField`) appear where the value is constrained (time-of-day, reference ID) and should open a picker instead.
2. **Web parity is broken in places.** Hardcoded English strings ("No tiles", "No projects yet", "Next: $next", `scale.name.lowercase()` in user-facing copy) bypass `R.string.*` resources. One screen (`TilesScreen`) concatenates three statistics into a single wall of text.

This is explicitly framed as **incremental improvement, not a full redesign**. The user accepted Approach A: fix the visible misuse patterns and the broken-parity strings; defer deeper navigation changes until parity is solid.

### Constraints

- Web composition parity is binding: control count, order, label text, and i18n keys must match `tastile-web`. Visual style of each control, tap interactions, and mobile layout may vary.
- The legacy `ui/dashboard/` screens (TimelineScreen, DashboardScreens, QuickCreateSheet) remain reachable from the desktop / tablet UI; only the mobile shell under `ui/mobile/**` is in scope here.

---

## 2. Goals

1. **Restore discoverability of tappable surfaces.** Every interactive element has a visible shape, ripple, or M3 standard affordance.
2. **Eliminate text-only CTAs in primary / secondary positions.** Primary actions use `Button`, secondary use `FilledTonalButton`, tertiary use `TextButton`. No more primary action hiding as blue text.
3. **Replace free-form text input with pickers where the value is constrained.** Time-of-day, date, duration, and reference selectors become `OutlinedButton` + sheet picker.
4. **Plug the Web parity leaks.** Hardcoded English / raw enum names in mobile UI move to `R.string.*`. Web parity memory (`feedback_web_composition_parity`) is restored.
5. **Centralise spacing and typography.** All ad-hoc `.dp` literals and inline `fontWeight` become `MobileSpacing` / `MaterialTheme.typography.*` references. No `fontSize` literals (already 0).

### Non-goals

- Bottom-navigation restructure (Approach C) — deferred.
- Iconification of all text buttons (Approach B) — only in narrow cases where the icon is universal.
- Changes to `ui/dashboard/` (Timeline, Dashboard, QuickCreate legacy) — out of scope; desktop UI owns them.
- Changes to `strings.xml` keys — only adding new keys; never rename existing keys (parity contract).
- New dependencies — stay on Material 3 + Compose BOM already in `libs.versions.toml`.

---

## 3. Concrete MUI anti-patterns to fix

Pulled from `grep` of the mobile UI tree. Counts include all `.kt` files under `ui/mobile/`.

### 3.1 `ListItem` for tappable rows (currently 0 uses)

Replace `Row(... .clickable {})` and `Box(... .clickable {})` with `ListItem(headlineContent, leadingContent, trailingContent, modifier = Modifier.clickable(onClick = ...))`. M3's `ListItem` ships built-in minHeight (56.dp single-line, 72.dp two-line), ripple, and shape — the standard "tap me" affordance.

Known call sites to migrate (representative; not exhaustive — sweep all `mobile/` during implementation):
- `mobile/MobileTopBar.kt:126` — ScaleDropdown trigger pill
- `mobile/panels/projects/ProjectsList.kt:77` — project rows
- `mobile/panels/schedule/ProjectsCheckboxSection.kt:62` — schedule project rows
- `mobile/panels/schedule/ScheduleViewToggle.kt:76` — schedule view tabs
- `mobile/panels/timeline/RangePicker.kt:66` — range options
- `mobile/sheets/quickcreate/QuickCreateBasePanel.kt:95,100,125` — QuickCreate subpanel triggers

### 3.2 `FilledTonalButton` for important secondary actions (currently 0 uses)

Reserve `Button` (filled) for one primary action per surface. Use `FilledTonalButton` for the second-most-important action, `OutlinedButton` for the third, `TextButton` for true tertiary actions (dismiss, "learn more"). Adopt the Material 3 button hierarchy:

| Role | M3 component | Use case |
| --- | --- | --- |
| Primary (1× per surface) | `Button` | Save, Create, Confirm |
| Secondary (1-2×) | `FilledTonalButton` | Add another, Apply, Replace |
| Tertiary | `OutlinedButton` | Cancel |
| Quaternary | `TextButton` | Dismiss, "What's this?" |

Migrate the 13 `TextButton` call sites in `mobile/` and the 9 `OutlinedButton` call sites — but only for primary/secondary positions; "Dismiss" stays as `TextButton`. Implementation sweep during plan execution.

### 3.3 `OutlinedTextField` → `OutlinedButton` + sheet picker for constrained values

Three `OutlinedTextField` instances in `QuickCreateSubpanels.kt` accept strings that are constrained to a small set (time-of-day `HH:mm`, date `yyyy-MM-dd`, reference IDs):

- Line 120-121: `timeOfDayStart`, `timeOfDayEnd`
- Line 129, 132: `window.bounds.start`, `window.bounds.end`
- Line 135: `window.referenceId`
- Line 111-112: `referenceId`, `referenceLabel` for tile references

Replace each with an `OutlinedButton` showing the current value (formatted) and a chevron. On click, open a `ModalBottomSheet` containing:
- Time picker → `androidx.compose.material3.TimePickerDialog` (M3, no extra dep)
- Date picker → `DatePickerDialog`
- Reference picker → existing `ReferencesLabelList` (already wired in `mobile/panels/references/`)

`window.referenceLabel` (free-form text the user types) stays as `OutlinedTextField` — that's actual free input. Same for the `Meta` panel's label field.

### 3.4 `Card` with `onClick` (currently 10 Card sites; ~half are decorative)

Audit each `Card(...)` in `mobile/`. If the card represents an action target, replace with `Card(onClick = ..., elevation = CardDefaults.elevatedCardElevation())`. If it's purely decorative container, keep as `Card`. The legacy `ui/dashboard/` use of `Card` is unchanged.

`Surface(onClick = ...)` is an acceptable alternative when `Card`'s default shape/elevation is wrong.

### 3.5 Add `MobileSpacing` tokens

`grep -Eoh '\b[0-9]+\.dp' mobile/` shows `8.dp` (15×), `6.dp` (12×), `4.dp` (11×), `12.dp` (7×), `2.dp` (6×). Promote the canonical 4/8/12/16/24 scale to `MobileTokens.Spacing` so spacing is one consistent vocabulary. Existing `topBarHeight` / `bottomBarHeight` stay where they are; `Spacing` is a new nested object.

```kotlin
object MobileTokens {
    object Spacing {
        val xxs = 2.dp
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
    }
    // … existing topBarHeight / bottomBarHeight / SurfaceAlpha / Status / iconHitTarget
}
```

Migration rule: **any literal `.dp` value used for padding, Spacer, Arrangement, or width between content blocks** migrates to `MobileSpacing.*`. Decorative-only values (icon visual size, sheet corner radius) stay literal — they're not spacing semantics.

### 3.6 Web parity strings — sweep hardcoded English

Move these to `R.string.*`:

| File | Current | New key |
| --- | --- | --- |
| `TimelineScreen.kt:1161` | `"No blocks in this ${scale.name.lowercase()} view. Create a tile to seed the timeline."` | `timeline_empty_${scale.name}` (pluralised) |
| `TilesScreen.kt:170` | `"Open: $tilesCount · Estimated: ${tilesCount * 30}m · Sections: ${grouped.size}"` | three separate keys + visual chips (see §4) |
| `ExecuteScreen.kt:164` | `"Next: $next"` | `execute_next_label` with format arg |
| `TileEditSheet.kt:76` | `"Occurrence: $placementId"` | `tile_occurrence_label` |
| `ScheduleRowList.kt:38` | `"No tiles"` | `schedule_empty_title` |
| `ProjectsCheckboxSection.kt:49` | `"No projects yet"` | `projects_empty_title` |

Empty-state copy also gets a hint string (existing pattern in web). New `R.string.*` keys follow the snake_case convention already established.

### 3.7 Typography — `fontWeight` literals (12 instances)

`MaterialTheme.typography.*` is already used 99× — good. The 12 remaining `fontWeight = FontWeight.X` literals in `mobile/` migrate to the M3 typography role's built-in weight. No new typography tokens needed; just stop overriding weight.

---

## 4. Per-screen fixes (the deliverables)

Each screen has a defined fix scope. Listed in priority order:

### 4.1 QuickCreate sheet (`mobile/sheets/quickcreate/**`)

- Replace 6 `OutlinedTextField` constrained inputs (§3.3) with `OutlinedButton` + sheet picker
- Three `Row(... .clickable{})` subpanel triggers → `ListItem` (§3.1)
- Two `TextButton` "Open subpanel" / "Close" → `FilledTonalButton` / `TextButton` per hierarchy (§3.2)
- Bottom Save / Cancel pair: `Button` (Save) + `OutlinedButton` (Cancel)

### 4.2 Side panel — Calendar / Schedule / Projects / References / Preferences

- List items (today/tomorrow/etc) → `ListItem` with leading icon and trailing chevron
- Section headers (`Calendar`, `Schedule`) → `HeadlineSmall` typography + `HorizontalDivider` underneath
- Item sub-labels → `BodySmall` with `onSurfaceVariant` color
- Spacing tokens applied throughout

### 4.3 Tasks / Projects tabs (`mobile/tabs/ExecuteScreen.kt`, `TilesScreen.kt`)

- `TilesScreen.kt:170` wall of text → three `AssistChip` (existing import already present; just compose them) showing `Open · 12`, `Estimated · 360m`, `Sections · 3`. Each chip uses an icon + the value, with `MaterialTheme.colorScheme.surfaceVariant` background and a contrasting `onSurfaceVariant` foreground.
- "Next: $next" string → `R.string.execute_next_label` with format arg, displayed as `ListItem` with leading icon
- Project rows in `TilesScreen` → `ListItem` with leading project icon + trailing chevron

### 4.4 Account sheet / Notifications sheet / AccountMenu sheet

- Apply button hierarchy (§3.2) to action rows
- Empty-state strings → `R.string.*`
- Section headers → `TitleMedium` + `HorizontalDivider`

### 4.5 Mobile top bar (`MobileTopBar.kt`)

- ScaleDropdown pill: replace raw `Row.clip(pillShape).border().clickable()` with `Surface(shape = pillShape, border = ..., onClick = ..., color = ...)` so the pill gets a ripple
- Status icons keep `IconButton`

### 4.6 Shared: `MobileTokens` extensions

- Add `MobileTokens.Spacing` nested object (§3.5)
- Add `MobileTokens.ButtonHeight` constants for primary (56.dp), compact (40.dp) — used by all `Button` calls

---

## 5. Acceptance criteria

A change passes review when:

1. **Discoverability**: every `clickable` / `onClick` site renders inside a M3 standard affordance (`ListItem`, `Card`, `Button`, `FilledTonalButton`, `OutlinedButton`, `Surface(onClick)`, or `IconButton`). A reviewer can identify every interactive element by silhouette alone.
2. **No free-form input for constrained values**: `OutlinedTextField` only appears for fields the user genuinely types into (title, label, note text, free-form description). Time, date, duration, reference ID, project ID — all picker-driven.
3. **No `fontSize` literals** (already 0), **no `fontWeight` literals** (target 0), **no hardcoded English strings** in `mobile/` `Text(...)` calls. Verified by `rg 'fontWeight\s*=|text\s*=\s*"[A-Z]' app/src/main/java/app/tastile/android/ui/mobile/`.
4. **Spacing tokens used**: any new code touching padding/Spacer/Arrangement in `mobile/` references `MobileSpacing.*`. Existing literals migrate opportunistically when the file is touched for other reasons.
5. **Button hierarchy**: at most one primary `Button` per surface; secondary actions use `FilledTonalButton`; tertiary uses `OutlinedButton`; "Dismiss" / "What's this?" / "Cancel" may use `TextButton` or `OutlinedButton` as appropriate.
6. **i18n parity**: every new user-visible string has an entry in `strings.xml` with a matching key. Existing keys never rename.
7. **Web parity**: control count, order, label text, and i18n key match `tastile-web`'s equivalent surface. Verified per-sheet by diffing against web source if a question arises.
8. **No regressions**: existing `cargo test` (server side) and Android unit / instrumented tests pass. `quick-create-*` test tags still resolve.

---

## 6. Risks

| Risk | Likelihood | Mitigation |
| --- | --- | --- |
| Sheet picker adds latency to common flows (Create / Save) | Medium | Use `ModalBottomSheet` with `skipPartiallyExpanded = true`; cache `remember { mutableStateOf(...) }` for picker value. Don't re-open picker if same value selected. |
| `ListItem` ripple / min-height breaks existing layouts that rely on tight spacing | Medium | Apply `Spacer(MobileSpacing.sm)` after `ListItem` instead of relying on internal padding; verify visually on Redmi XIG03 (440 dpi). |
| Hardcoded English move to strings causes a missing translation on a locale the user runs | Low | Add all keys with English values only (no `values-ja/`); all locale resolution falls through to English until translation work happens. |
| `FilledTonalButton` palette conflicts with status colors (`MobileTokens.Status.*`) | Low | Restrict `FilledTonalButton` usage to action CTAs in dialogs/sheets; status indicators continue using `Status.*` colors directly. |
| Memory notes say "Web composition parity is binding" — accidental web divergence during refactor | Medium | Treat this spec as a parity *repair* spec; preserve i18n keys; verify each modified surface against `tastile-web` if any doubt. |

---

## 7. Out of scope (deferred to a later spec)

- Bottom-navigation restructure (Approach C from brainstorming)
- Iconification of all text buttons (Approach B)
- Migrating `ui/dashboard/` (Timeline, Dashboard, QuickCreate legacy, ManagementScreens)
- Adding `values-ja/strings.xml` translation
- Refactoring `MobileScaffold` / `MobileNavGraph`
- Updating `tastile-brands` iconography

---

## 8. Open questions for the implementer

- `TilesScreen.kt:170` "Estimated: ${tilesCount * 30}m" — is the 30-minute-per-tile estimate still accurate? If not, this needs a real `tiles_estimate_minutes` model field. **Out of scope for this spec**; assume the formula stays for now.
- `ScheduleRowList.kt:38` "No tiles" — does the empty state want a "+ Add tile" CTA inline, or just the hint? Recommendation: just the hint (the existing `+ Create` in the toolbar is the primary entry point per memory `feedback_calendar_create_entry_point`).

---

## 9. Files touched (initial estimate, refined during planning)

- **New / added**: `mobile/designsystem/MobileSpacing.kt` (or extend `MobileTokens.kt`), new entries in `app/src/main/res/values/strings.xml`
- **Modified (core)**: `mobile/MobileTopBar.kt`, `mobile/MobileScaffold.kt`, `mobile/designsystem/MobileTokens.kt`
- **Modified (sheets)**: `mobile/sheets/quickcreate/QuickCreateBasePanel.kt`, `mobile/sheets/quickcreate/QuickCreateSubpanels.kt`, `mobile/sheets/TileEditSheet.kt`, `mobile/sheets/PanelSheet.kt`
- **Modified (panels)**: all `mobile/panels/**` (Calendar / Schedule / Projects / References / Preferences)
- **Modified (tabs)**: `mobile/tabs/ExecuteScreen.kt`, `mobile/tabs/TilesScreen.kt`, `mobile/tabs/SettingsScreen.kt`, `mobile/tabs/IntegrationsScreen.kt`
- **Modified (account / notifications)**: `mobile/account/AccountSheet.kt`, `mobile/account/SubscriptionSheet.kt`, `mobile/account/TokensSheet.kt`, `mobile/sheets/NotificationsSheet.kt`, `mobile/sheets/AccountMenuSheet.kt`
- **Untouched**: `ui/dashboard/**` (legacy), `MainActivity.kt`, navigation graph (no route changes)

Approximate scope: ~35 files modified, ~1 new token file, ~6 new string resources.