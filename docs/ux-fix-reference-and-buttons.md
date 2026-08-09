# UX Fix: Hide empty "References" entry + recenter off-balance buttons

Scope: two small, well-scoped layout / visibility tweaks in `tastile-android`.
No design-system primitives, dependencies, or click behavior changed.

## Fix 1: Hide the "Reference" drawer item when empty

User report: 「Referenceは現状何も無いから隠すべき」
(Reference is currently empty — hide it.)

### What is "Reference" content?

- The drawer item labeled "References" routes to `composable("integrations")`,
  which renders `IntegrationsScreen`.
- `IntegrationsScreen` only shows a static notice that Google Calendar
  integration is out of v1 scope — there is no live data source to drive it.
- The label-derived `Switch` list in `ReferencesSectionContent` (used by the
  legacy `SidePanelSheet`) is also empty until tiles carry labels.

### Where the change lives

- `app/src/main/java/app/tastile/android/ui/mobile/SidePanelDrawerContent.kt`
  - Added a `hasReferencesContent: Boolean = false` parameter.
  - Filter `drawerRoutes` to drop `"integrations"` when the flag is `false`.
  - The underlying data plumbing (`drawerRoutes`, `IntegrationsScreen`,
    `ReferencesSectionContent`, `ReferenceOverlayStore`) is untouched and
    available for re-enable once the integration surface ships.
- `app/src/main/java/app/tastile/android/ui/mobile/MobileScaffold.kt`
  - Reads `dashboardViewModel.tiles` and derives
    `hasReferencesContent = tiles.any { it.labels.isNotEmpty() }`.
  - Passes the flag into `SidePanelDrawerContent`.
  - Wraps the `composable("integrations") { ... }` registration in an
    `if (hasReferencesContent)` guard, so a deep-link or stale back-stack
    entry into "integrations" can no longer land on the empty screen.

### Caveats

- The drawer ordering stays `timeline / execute / tiles / settings` when the
  flag is `false`; the empty "References" entry simply disappears. With
  labelled tiles the entry reappears at index 3 (between Projects and the
  Settings divider), matching the original `drawerRoutes` order.
- Existing `composable("integrations")` deep-links and saved back-stack state
  now no-op (no destination registered) while the flag is `false`. This is
  intentional: the screen is empty.

## Fix 2: Center off-balance button rows

User report: 「ボタンが左揃え過ぎてバランスが悪い」
(buttons too left-aligned, unbalanced.)

Approach: one-line layout tweaks on the user-facing primary CTAs that visibly
sit at the start (left) of their parent. No behavior change, no new imports
beyond `Alignment.CenterHorizontally`, and no `disable +=` lint entries.

### Changes

- `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt`
  - `NotificationsSection` buttons row: added `Modifier.fillMaxWidth()` and
    switched `Arrangement.spacedBy(8.dp)` to
    `Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)`. The two
    `NiaButton`s ("Allow" / "Test") were previously glued to the leading edge
    of an unconstrained Row; now they sit centered with an 8 dp gap.
- `app/src/main/java/app/tastile\android\ui\mobile\sheets\quickcreate\QuickCreateSubpanels.kt`
  - `MetaPanel` action FlowRow: switched
    `Arrangement.spacedBy(4.dp)` to
    `Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)` so
    "Clear / Cancel / Apply" recenter instead of stacking at the start.
  - `TimePanel` "Add window" `FilledTonalButton`: added `fillMaxWidth()` to
    match the `OutlinedTextField` widths above it.
  - `TimePanel` per-window "Remove window" `FilledTonalButton`: added
    `fillMaxWidth()` so each row's remove action spans the column.
  - `ReferencesPanel` "Add reference" `FilledTonalButton`: added
    `fillMaxWidth()` for the same reason as "Add window".

### Caveats

- `LoginScreen` / `AccountScreen` / `TokensSheet` primary CTAs already use
  `Modifier.fillMaxWidth()` and `horizontalAlignment = Alignment.CenterHorizontally`
  on their parent `Column`, so they were already balanced — left alone.
- The `AccountScreen` "Manage Billing" / "Sign Out" buttons are inside a
  `Column` that lays them out full-width by default; left alone.
- The `SecurityLockSection` timeout row (`Row { NiaButton - Text - NiaButton }`)
  uses `Arrangement.spacedBy(8.dp)` inside `fillMaxWidth()`; the label sits
  between two buttons and the visual gap is acceptable — left alone.
- Recursive `ConditionControls` "Remove" / "Add condition" tonal buttons are
  intentionally left untouched (per the "sample 3–5 screens, only primary
  CTAs" guidance). The user's complaint was about visible imbalance, not
  every action button in the sheet.
- `Arrangement.spacedBy(space, alignment)` is a stock Compose foundation
  overload — no new dependency, and no `verifyDesignSystemImports` impact
  (no new `material3` imports; `Alignment` is already imported in both
  files).

## Verification

- `./gradlew verify` was **not** run per task instructions ("don't run gradle").
- Both files were re-read after each edit to confirm:
  - imports still resolve (no new M3 imports needed — `Alignment` was already
    imported in both files),
  - `Arrangement.spacedBy(Dp, Alignment.Horizontal)` matches the foundation
    signature present in 1.10.x / 1.11.x,
  - `FlowRow` accepts `Arrangement.Horizontal` for `horizontalArrangement`.
- No unit or instrumented test references `SidePanelDrawerContent` directly,
  or expects "References" to be in the drawer / reachable while labels are
  empty. `SidePanelSheetNavigationTest` exercises the legacy
  `SidePanelSheet` (not the drawer) and only taps the "Tasks" entry.
