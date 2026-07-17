# Panel M3 + architecture-recommendations migration

**Date:** 2026-07-17
**Branch:** `2026-07-07-android-parity`
**Trigger:** user said panels must comply with M3 + Android architecture recommendations.

## Context

- The NiA migration (`2a85ed5`) rewrote the new `core.designsystem.component.*`
  primitives and committed them, but **panels were never migrated** —
  they still reference the deleted `ui.designsystem.AppTheme`,
  `ui.designsystem.AppSpacing/AppCorner`, `ui.mobile.designsystem.MobileSpacing`,
  `AppPrimaryButton`, `AppTertiaryButton`, `AppPickerButton`, `SectionHeader`,
  `AppEmptyState`, `AppListItem`, `AppListRow`.
- `compileDebugKotlin` reports **573 errors** (panel files: 131). The
  previous "0 errors" claim was wrong.
- `ui.mobile.components.AppListItem`, `AppEmptyState`, etc. that were
  briefly on disk got deleted. We will recreate only what we need.

## Architecture rules (Android developer docs)

| Rule | Applied as |
|---|---|
| Unidirectional Data Flow (UDF) | VMs expose `StateFlow<UiState>`; UI calls method-only actions |
| Lifecycle-aware state collection | `collectAsStateWithLifecycle()` everywhere (no `collectAsState()`) |
| ViewModels at screen level only | Reusable composables take plain holders / lambdas |
| Keep ViewModels free of `Activity/Context/Resources` | Already true |
| Use coroutines + `viewModelScope` for actions | Already true |
| Plain state holders for reusable composables | `mutableStateOf` for trivial flags; ViewModel for shared |

## Conversion rules (mechanical)

| From | To |
|---|---|
| `AppTheme.colors.X` | `MaterialTheme.colorScheme.X` |
| `AppTheme.typography.X` | `MaterialTheme.typography.X` |
| `AppSpacing.X` / `MobileSpacing.X` | `X.dp` (xxs=2, xs=4, sm=8, md=12, lg=16, xl=24, xxl=32) |
| `AppCorner.mediumShape` | `MaterialTheme.shapes.medium` |
| `AppCorner.smallShape` | `MaterialTheme.shapes.small` |
| `MobileTokens.SurfaceAlpha.selected/subtle/strongSelected/started` | `0.16f / 0.08f / 0.24f / 0.12f` |
| `AppPrimaryButton(text=..., onClick=..., enabled=..., modifier=..., leadingIcon=...)` | `NiaButton(onClick=..., modifier=..., enabled=..., leadingIcon=...) { Text(text) }` |
| `AppTertiaryButton(text=..., onClick=..., enabled=...)` | `NiaTextButton(onClick=..., enabled=..., modifier=...) { Text(text) }` |
| `collectAsState()` | `collectAsStateWithLifecycle()` |
| `AppEmptyState(icon = { Icon(...) }, ...)` | inline M3 Column/Icon/Text (used 2-3 times only) |
| `AppListItem(headline, leading, selected, onClick, modifier)` | `NiaListItem(headlineContent = { Text(headline, ...) }, leadingContent = { Icon(leading, ...) }, modifier = Modifier.clickable { onClick() }, colors = ...)` — recreate a thin `AppListItem` in `ui.mobile.components` |
| `AppPickerButton(label, value, onClick, leadingIcon, modifier)` | create thin wrapper `AppPickerButton` in `ui.mobile.components` using M3 `OutlinedTextField` (read-only) + trailing icon |
| `SectionHeader(title)` | create thin wrapper `AppSectionHeader` in `ui.mobile.components` |
| `LaunchedEffect(key) { vm.action() }` | replace with direct callback in `onSelect`/`onClick` |

## New shared wrappers

- `app/src/main/java/app/tastile/android/ui/mobile/components/AppSectionHeader.kt` —
  `Text(title, style = titleSmall, color = onSurfaceVariant)`.
- `app/src/main/java/app/tastile/android/ui/mobile/components/AppListItem.kt` —
  thin wrapper over `NiaListItem` with a `String headline + ImageVector leading
  + onClick + selected` signature.
- `app/src/main/java/app/tastile/android/ui/mobile/components/AppEmptyState.kt` —
  thin wrapper over M3 Column/Icon/Text (no lambda for icon).
- `app/src/main/java/app/tastile/android/ui/mobile/components/AppPickerButton.kt` —
  M3 `OutlinedTextField(readOnly = true) + trailing Icon + Modifier.clickable`.

## Architecture-compliance changes (per file)

### `ProjectsSectionContent.kt`
- Move `deleteCandidate` / `editCandidate` from `mutableStateOf` in composable
  into `ProjectsViewModel` as `MutableStateFlow<Workspace?>`. Add
  `requestDelete(ws)`, `requestEdit(ws)`, `cancelDelete()`, `cancelEdit()`.
- Replace `LaunchedEffect(selectedOwnerId) { dashboardViewModel?.setOwnerFilter(...) }`
  with passing an `onOwnerSelected: (String?) -> Unit` to `ProjectsList` and
  invoking both `projectsViewModel.selectOwner(id)` and
  `dashboardViewModel.setOwnerFilter(id)` from the call site.
- Drop the `dashboardViewModel: DashboardViewModel?` nullable parameter
  (callers always pass non-null in practice).

### `ReferencesSectionContent.kt`
- Replace `collectAsState()` with `collectAsStateWithLifecycle()`.
- The `groupTilesByLabel` derivation stays in the composable — it's a pure
  derivation over state, not a side effect.

### `TimelineSectionContent.kt`
- Replace `collectAsStateWithLifecycle()` (already done).
- Drop the broken `CustomDateRow` stub that prints `onStartChange.hashCode()`
  for an action — replace with two `NiaOutlinedTextField` showing the active
  ISO strings (read-only) plus a TODO for the actual date pickers.

### `ProjectsViewModel.kt`
- Already exposes `state`, `creating`, `selectedOwnerId` as `StateFlow`. Add
  `deleteCandidate` and `editCandidate` `StateFlow<Workspace?>` + their
  setters. `selectOwner` should NOT take a callback; the composable wires it.

## Plan of execution (4 parallel agents)

1. **Agent A — `panels/timeline/*`** (4 files: TimelineSectionContent,
   TimelineMetaPills, RangePicker, TimelineBlockList). Also creates
   `AppSectionHeader` wrapper.
2. **Agent B — `panels/schedule/*` + `panels/references/*` + `panels/ReferencesSectionContent`**
   (4 files). Also creates `AppListItem` and `AppEmptyState` wrappers.
3. **Agent C — `panels/projects/*` + `ProjectsViewModel.kt`** (5 files).
   Architecture-compliance changes. Also creates `AppPickerButton` wrapper.
4. **Agent D — verify** (after A/B/C complete): build → 0 errors → install →
   screenshot Timeline/Schedule/Projects/References tabs → commit.

## Verification

- `./gradlew :app:compileDebugKotlin` reports 0 errors.
- APK builds, installs via `adb push + pm install -i com.android.shell -r`,
  launches without crash.
- Side-panel tabs render M3 (large title, rounded outlined fields, two-line
  list rows, leading icons, trailing chevrons), no leftover `AppTheme.*`,
  no `mutableStateOf<Workspace?>` in screen composables.

## Out of scope

- The 442 non-panel errors (TimelineScreen, DashboardScreens, etc.) are
  out of scope for this PR — the user's ask was panels. A separate plan
  will cover the rest.
