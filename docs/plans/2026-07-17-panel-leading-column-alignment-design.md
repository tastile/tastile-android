# Panel Leading-Column Alignment — Design

Date: 2026-07-17
Branch: `2026-07-07-android-parity`
Scope: QuickCreate base panel + all routed subpanels

## 1. Mechanism (M3-compliant)

Adopt Material 3 `ListItem`'s `leadingContent` slot width as the single
source of truth for left-column reservation across every panel row.

```kotlin
// app/src/main/java/app/tastile/android/core/designsystem/theme/PanelTokens.kt
object PanelTokens {
    val LeadingColumnWidth: Dp = 56.dp   // = 16.dp ListItemStartPadding + 24.dp IconSize + 16.dp gap
    val LeadingIconSize: Dp    = 24.dp
    val LeadingColumnGap: Dp   = 16.dp
}
```

These values mirror M3 ListItem conventions verbatim, so a row using
`NiaListItem(leadingContent = Icon(...))` naturally renders the icon at
`x = 16.dp` and the text at `x = 56.dp` from the row's left edge. All
non-`ListItem` rows in the panel must reach the same `x = 56.dp`
text/control start position.

## 2. Application rules

### Case A — `NiaListItem` with icon (Time, Duration, Repeat, Completion logic, Completion requires, Behavior)
Already aligned by M3. No change.

### Case B — `NiaListItem` without icon (future-proofing)
Pass `leadingContent = { Box(Modifier.size(24.dp)) }` to reserve the
slot when no icon is present. Text lands at `x = 56.dp`.

### Case C — Text input (`EditableTitleField`)
Current: `Column.padding(horizontal = 16.dp)` → text at `x = 16.dp`.
Fix: `Column.padding(start = 56.dp, end = 16.dp)`.
BasicTextField is unchanged; `FocusRequester` and placeholder tracking
follow automatically.

### Case D — Outlined buttons

**D-1. Icon + text** (`Add condition or group`, `Add task`, `References`)
Current: `contentPadding(start = 16.dp)` + internal
`Icon + Spacer(16.dp) + Text` → icon at `x = 32.dp`.
Fix: `contentPadding(start = 0.dp, top = 8.dp, end = 24.dp, bottom = 8.dp)`.
Icon lands at `x = 16.dp`; `Spacer(16.dp)` pushes text to `x = 56.dp`.

**D-2. Text-only** (`Move up`, `Move down`)
Current: `contentPadding(start = 16.dp)` → text at `x = 32.dp`.
Fix: keep `contentPadding(start = 16.dp)`; prepend
`Box(Modifier.size(24.dp))` then `Spacer(16.dp)` to reserve the
leading column as whitespace.

**D-3. Icon + text with internal trailing** (`Remove`)
Trash icon + text. Same composition as D-1 (icon at `x = 16.dp`,
text at `x = 56.dp`).

### Case E — Chip rows (`FlowRow`)
Current: `padding(horizontal = 16.dp)` → first chip at `x = 16.dp`.
Fix: `padding(start = 56.dp, end = 16.dp)`. The horizontal scroll
modifier is preserved (overflow still scrolls left).

**Targets**:
- `QuickCreateBasePanel.kt` — `quick-create-organize-row` (project
  chip, tag chips, Organize button)
- `QuickCreateSubpanels.kt` — 9 chip rows: when-chips,
  time-of-day-chips, repeat-chips, reference-record-target-kind-chips,
  reference-record-relation-chips, completion-logic-chips,
  condition-term-chips, condition-relation-kind-chips,
  condition-relation-window-chips

### Case F — `OutlinedTextField` inside subpanels (time inputs, etc.)
Current: `padding(horizontal = 16.dp)` → text at `x = 16.dp`.
Fix: `padding(start = 56.dp, end = 16.dp)`.

### Out of scope
- `AppPickerButton` (label + value stacked, trailing dropdown) — its
  internal layout is independent of the leading-column rule.
- Subpanel outer `Column.padding(horizontal = 16.dp, vertical = 12.dp)`
  — that's the panel-edge inset, not a row.
- `ListItem` row-level chip wrappers (none currently exist).

## 3. Verification

### 3-1. Compose UI test
`app/src/test/.../quickcreate/QuickCreateLeadingColumnAlignmentTest.kt`
(Robolectric + Compose UI Test).

Sample x-positions per row class and assert equality within `1.dp`
(density-rounding tolerance):
- `quick-create-title` text
- `quick-create-essential-time` headline
- `quick-create-add-task` text
- `quick-create-when-chips` first chip
- `quick-create-time-of-day-chips` first chip

On failure: print actual x-values per tag for instant pinpointing.

### 3-2. Static guard
`scripts/ci/panel-leading-column-guard.sh` (or equivalent under
`tools/`):

```
rg "padding\(horizontal = 16\.dp\)" app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/
```

Must return 0 hits, with these documented exceptions:
- `EditableTitleField` (Case C — uses `start = 56.dp, end = 16.dp`)
- Outlined buttons D-2 (`contentPadding(start = 16.dp, ...)` for
  text-only buttons)
- The panel outer Column itself

Also:
```
rg "contentPadding.*start = 16\.dp" app/src/main/java/app/tastile/android/ui/mobile/sheets/quickcreate/
```
Must return 0 hits outside D-2.

### 3-3. Visual confirmation
Install rebuilt APK via `adb push + pm install -i com.android.shell -r`,
open the QuickCreate sheet, capture screenshots of the base panel
plus three subpanels (Time, Recurring, Completion) using
`mcp__chrome-devtools__take_screenshot`. Compare against existing
`full_panel.png`. Verify title / `Add task` / chip-start / ListItem
text share the same x-coordinate.

### 3-4. Rollback
Single PR, single commit. `git revert <sha>` restores prior layout.
New file is `PanelTokens.kt` only — no other module import surface
changes.

## 4. Out of scope (explicit)
- Web parity changes (`tastile-web` is its own repo)
- Other panels (Projects, References subpanel — uses AppPickerButton
  pattern, not affected)
- Replacing `NiaListItem` with custom row composables
- Changing button visual styles (outline borders, paddings unrelated
  to x-alignment)