# Dashboard Component Refactor + Design Token Foundations (Phase 1)

Status: Draft for review
Owner: tastile-android UI maintainers
Date: 2026-08-25
Related workspace contract: `../AGENTS.md`
Related canonical orientation: `../../README.md`, `../../architecture.md`

## 1. Background and Motivation

The Tastile Android app is migrating toward a single Material 3 (M3) unified
design system. The current `core/designsystem/` already ships 19 component
wrappers (Button, Card, DatePicker, IconButton, ListItem, ModalBottomSheet,
Navigation, OutlinedTextField, SegmentedButton, Switch, Tabs, Tag, TimePicker,
TopAppBar, ViewToggle, LoadingWheel, Background, AppComponents) plus theme and
icon layers.

Within `ui/dashboard/`, however, screen-level composables still host repeated
patterns that should live in the design system:

- `TileCompactCard` and `TileExpandableCard` reimplement list-item primitives.
- `StatusCircle` is a hand-rolled Box-with-glyph component duplicated implicitly
  in `QuickCreateSheet` flows.
- `CardPrimaryActions` repeats a `Row { when (status) { ... } }` branching block
  per `CardStatus`.
- `DashboardCardRenderer` mixes card shell, header row, status icon, and
  body content in one large composable, making it hard to vary for new card
  kinds.
- Spacing values (`2.dp`, `6.dp`, `8.dp`, `10.dp`, `12.dp`) are scattered as
  raw literals, blocking any later spacing-token overhaul.

Meanwhile, the project has no design tokens for `TileLifecycle` status colors,
card roles, surface elevations, or spacing scales. A future visual refresh
(Phase 2) would have to touch every screen to apply a palette change unless
those tokens exist first.

## 2. Goals and Non-Goals

### Goals

- Extract five repeated dashboard patterns into the design system with Slot APIs.
- Introduce four families of design tokens that downstream phases can fill in
  without touching call sites.
- Keep observable behavior identical during Phase 1: no copy change, no visual
  change, no new feature.
- Ship the change as a sequence of small PRs that each pass
  `./gradlew verify` and `testDebugUnitTest`.
- Add unit/component tests for the new design system components.

### Non-Goals (out of scope for Phase 1)

- Account and Mobile screen refactors (Phase 2+).
- Brand palette, typography refresh, dynamic color (Phase 2).
- Animation, shared element transitions, motion design (Phase 3).
- TalkBack semantics, focus order, accessibility audit (Phase 4).
- Replacing the `verifyDesignSystemImports` guard logic itself.
- Changes to `tastile-core` boundary, data layer, notifications, sync, or DI.

## 3. Phasing Context

Phase 1 of a four-phase UI improvement program:

1. Phase 1 - Component design system strengthening (this document).
2. Phase 2 - Visual and theme refresh (palette, typography, shape).
3. Phase 3 - Motion and transitions.
4. Phase 4 - Accessibility.

Phase 1 establishes the token keys and component APIs that Phases 2-4 will
consume. Token **values** are intentionally deferred to Phase 2, but the
**keys and shape** must be stable enough that filling in values later does
not require call-site edits.

## 4. Architecture

```
ui/dashboard/
  DashboardScreens.kt          <-- consumes design system components only
  DashboardCards.kt
  ManagementScreens.kt
  QuickCreateSheet.kt
  ...
    |
    v
core/designsystem/component/   <-- new components added here
  TastileTileCard.kt
  TastileCompactTileRow.kt
  TastileStatusCircle.kt
  TastileCardActionRow.kt
  TastileDashboardCardShell.kt
  (+ existing 19 wrappers)
    |
    v
core/designsystem/theme/      <-- token keys (values filled in Phase 2)
  PanelTokens.kt               <-- TastileStatusTokens / TastileCardRoleTokens
  ThemeExtensions.kt           <-- TastileSurfaceElevationTokens / TastileSpacingTokens
  Color.kt                     <-- unchanged in Phase 1; Phase 2 fills values
  Type.kt                      <-- unchanged in Phase 1
```

Rules:

- `ui/dashboard/` continues to forbid raw `androidx.compose.material3.*`
  imports except under `// m2-allow:` markers. The new wrappers expose
  everything dashboard code needs.
- New design system components consume tokens via the CompositionLocals
  defined in section 6 (e.g. `LocalTastileStatusTokens.current.ready.icon`),
  so Phase 2 can swap palette entries without editing consumers.
- Token keys are declared as `data object`s or `val` properties on a
  CompositionLocal-backed interface. Default values are placeholders that
  Phase 1 will visually fall back to `MaterialTheme.colorScheme.*` and
  Phase 2 will replace.

## 5. New Design System Components

### 5.1 `TastileCompactTileRow`

Replaces `TileCompactCard`.

Signature:

```kotlin
@Composable
fun TastileCompactTileRow(
    title: String,
    lifecycle: TileLifecycle,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
)
```

Behavior:

- `onClick == null` -> renders a non-interactive row.
- `onClick != null` -> row consumes `Modifier.clickable` on the outer container.
- Renders `TastileStatusCircle` + title + optional trailing.

### 5.2 `TastileStatusCircle`

Replaces `StatusCircle`. Domain-aware component is acceptable for lifecycle
status.

Signature:

```kotlin
@Composable
fun TastileStatusCircle(
    lifecycle: TileLifecycle,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

Phase 1 renders the same Unicode glyph as today ("✓", "▶", "○", "·"). Phase 2
will replace the glyph with an icon sourced from `TastileIcons` and color
sourced from `TastileStatusTokens`.

### 5.3 `TastileCardActionRow`

Replaces `CardPrimaryActions`. Replaces boolean flags with a sealed
`TastileCardActions` value.

Signature:

```kotlin
sealed interface TastileCardActions {
    data object Ready : TastileCardActions
    data object Started : TastileCardActions
    data object DoneOrArchived : TastileCardActions
}

@Composable
fun TastileCardActionRow(
    actions: TastileCardActions,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onDefer: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Phase 1 keeps the same button set per branch as the existing
`CardPrimaryActions`. Phase 2 may extend the sealed interface with
customizable labels and icons.

### 5.4 `TastileDashboardCardShell`

Replaces the outer wrapper of `DashboardCardRenderer`.

Signature:

```kotlin
@Composable
fun TastileDashboardCardShell(
    modifier: Modifier = Modifier,
    header: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
)
```

Behavior:

- Wraps content in `NiaOutlinedCard` with consistent padding.
- Renders `header` row at the top and `content` below.
- No internal logic beyond layout; consumers pass any header / content they
  want.

### 5.5 `TastileTileCard`

Replaces `TileExpandableCard`.

Signature:

```kotlin
@Composable
fun TastileTileCard(
    title: String,
    lifecycle: TileLifecycle,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    expandedContent: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
)
```

Behavior:

- Header row: `TastileStatusCircle`, title, optional subtitle, expand chevron.
- Tapping the header invokes `onToggleExpanded`.
- When `expanded == true`, renders a divider and `expandedContent`.
- The action row is rendered unconditionally at the bottom of
  `expandedContent`; consumers compose it where appropriate.
- Phase 3 will animate the expand/collapse; Phase 1 introduces the boolean
  state holder for forward compatibility.

All five components must:

- Place `Modifier` as the first optional parameter.
- Use Slot APIs (lambdas / sealed types) in place of boolean shape flags.
- Provide `@Preview` composables in Light, Dark, and FontScale=2.0 variants.
- Live under `core/designsystem/component/` next to existing wrappers.

## 6. New Design Tokens

Place under `core/designsystem/theme/PanelTokens.kt` and
`ThemeExtensions.kt`. Phase 1 declares **keys with placeholder defaults**
that visually match today's behavior.

### 6.1 `TastileStatusTokens`

Four lifecycle states, three slots each (12 color entries).

| Lifecycle | container | onContainer | icon |
|-----------|-----------|-------------|------|
| READY     | `colorScheme.surfaceVariant` (placeholder) | `colorScheme.onSurfaceVariant` | `colorScheme.primary` |
| STARTED   | `colorScheme.tertiaryContainer` (placeholder) | `colorScheme.onTertiaryContainer` | `colorScheme.tertiary` |
| DONE      | `colorScheme.secondaryContainer` (placeholder) | `colorScheme.onSecondaryContainer` | `colorScheme.secondary` |
| ARCHIVED  | `colorScheme.surfaceVariant` (placeholder, lower alpha) | `colorScheme.onSurfaceVariant` | `colorScheme.outline` |

Exposed via CompositionLocal `LocalTastileStatusTokens`.

### 6.2 `TastileCardRoleTokens`

Three card roles, two slots each.

| Role         | container | border |
|--------------|-----------|--------|
| Neutral      | `colorScheme.surface` | `colorScheme.outlineVariant` |
| Actionable   | `colorScheme.surfaceContainerLow` | `colorScheme.primary` |
| Completed    | `colorScheme.surfaceContainerLowest` | `colorScheme.outline` |

Exposed via CompositionLocal `LocalTastileCardRoleTokens`.

### 6.3 `TastileSurfaceElevationTokens`

Three elevation slots: `Card`, `Sheet`, `Overlay`. Each is a `Dp` value.
Phase 1 mirrors current ad-hoc values (1.dp / 3.dp / 6.dp). Phase 2 may
recalibrate.

Exposed via CompositionLocal `LocalTastileSurfaceElevationTokens`.

### 6.4 `TastileSpacingTokens`

Five spacing slots: `xs = 4.dp`, `s = 8.dp`, `m = 12.dp`, `l = 16.dp`,
`xl = 24.dp`. Phase 1 introduces them; Phase 2 standardizes their use across
screens.

Exposed via CompositionLocal `LocalTastileSpacingTokens`.

CompositionLocal provisioning:

```kotlin
@Composable
fun TastileTheme(...) {
    CompositionLocalProvider(
        LocalGradientColors provides ...,
        LocalBackgroundTheme provides ...,
        LocalTintTheme provides ...,
        LocalTastileStatusTokens provides TastileStatusTokens.default(),
        LocalTastileCardRoleTokens provides TastileCardRoleTokens.default(),
        LocalTastileSurfaceElevationTokens provides TastileSurfaceElevationTokens.default(),
        LocalTastileSpacingTokens provides TastileSpacingTokens.default(),
    ) { MaterialTheme(...) { content() } }
}
```

`default()` resolves placeholders through `MaterialTheme.colorScheme.*` so
today's look is unchanged.

## 7. Migration Plan (Dashboard Side)

Five small PRs, each independently buildable and testable.

### PR-A: Tokens foundation + `TastileStatusCircle`

- Add `TastileStatusTokens`, `TastileCardRoleTokens`,
  `TastileSurfaceElevationTokens`, `TastileSpacingTokens` keys with
  placeholder defaults.
- Wire CompositionLocals into `TastileTheme`.
- Add `TastileStatusCircle` to `core/designsystem/component/`.
- Replace `ui/dashboard/DashboardScreens.kt::StatusCircle` callsite with
  `TastileStatusCircle`.
- Add unit/component tests for `TastileStatusCircle`.
- `./gradlew verify` + `testDebugUnitTest` pass.

### PR-B: `TastileCompactTileRow`

- Add `TastileCompactTileRow` to `core/designsystem/component/`.
- Replace `ui/dashboard/DashboardScreens.kt::TileCompactCard` with
  `TastileCompactTileRow`.
- Add component test (Light/Dark/FontScale=2.0).
- `./gradlew verify` + `testDebugUnitTest` pass.

### PR-C: `TastileCardActionRow`

- Add `TastileCardActions` sealed interface and `TastileCardActionRow` to
  `core/designsystem/component/`.
- Replace `ui/dashboard/DashboardScreens.kt::CardPrimaryActions` callsite.
- Add component test covering the three action states.
- `./gradlew verify` + `testDebugUnitTest` pass.

### PR-D: `TastileDashboardCardShell` + `TastileTileCard`

- Add `TastileDashboardCardShell` and `TastileTileCard` to
  `core/designsystem/component/`.
- Replace `DashboardCardRenderer` shell and `TileExpandableCard` body.
- Add component tests.
- `./gradlew verify` + `testDebugUnitTest` pass.

### PR-E: `// m2-allow:` marker cleanup

- After the prior PRs land, audit the remaining `// m2-allow:` markers in
  `ui/dashboard/`.
- Remove any that can be deleted now that the new wrappers exist.
- Keep only markers with a documented justification (e.g. genuinely
  primitive escapes that should move to the design system later).

Each PR must:

- Run `./gradlew verify` from a clean state before opening.
- Run `./gradlew testDebugUnitTest` to confirm regression coverage.
- Reference this spec in the PR description.
- Include a commit message in the form `feat(dashboard): ...` or
  `refactor(designsystem): ...`.

## 8. Testing Strategy

Existing tests that must keep passing:

- `DashboardViewModel` unit tests: loading flag, `buildExecuteCards`,
  `buildTileCards`, `handleCardAction`.
- Repository tests that exercise dashboard data flows.

New tests to add per PR:

- `TastileStatusCircle` - four lifecycle states, clickable vs non-clickable.
- `TastileCompactTileRow` - non-interactive, clickable, with trailing slot.
- `TastileCardActionRow` - all three `TastileCardActions` branches.
- `TastileTileCard` - collapsed, expanded, click on header toggles state.
- `TastileDashboardCardShell` - renders passed header and content slots.

Use `createComposeRule()` per `compose-ui-testing-patterns` skill. Snapshot
tests are optional and only if `androidx.compose.ui.test` + Paparazzi / Roborazzi
are already wired in.

Verification commands (run before claiming "PASS / DONE / GREEN / ready to ship"):

```bash
./gradlew verify
./gradlew testDebugUnitTest
./gradlew lintDebug
```

## 9. Data Flow (unchanged in Phase 1)

Dashboard data flow is preserved:

```
DashboardViewModel
  - loading: StateFlow<Boolean>
  - buildExecuteCards() / buildTileCards() -> List<DashboardCardModel>
  - handleCardAction(action: CardAction)
    |
    v
DashboardScreens  --(after refactor)-->  design system wrappers
    |
    v
TastileTileCard / TastileCardActionRow / TastileStatusCircle
```

No changes to repositories, `tastile-core` bridge, notifications, or sync.

## 10. Error Handling

Existing error handling in dashboard cards (loading wheel, empty state) is
preserved. No new error paths are introduced in Phase 1.

## 11. Out-of-Scope Items Carried Forward

- Phase 2 will fill `TastileStatusTokens` and `TastileCardRoleTokens`
  values with the brand palette, swap Unicode glyphs for icons, and refresh
  typography.
- Phase 3 will animate `TastileTileCard.expanded` transitions and add
  shared element transitions between dashboard and detail screens.
- Phase 4 will add `Modifier.semantics`, `CollectionInfo`, focus order,
  and TalkBack descriptions.
- Account and Mobile screens adopt the same components after Phase 1
  validates them on dashboard.

## 12. Risks

- **Risk**: Replacing `TileExpandableCard` (which currently uses an internal
  `remember { mutableStateOf(false) }`) with `TastileTileCard` requires the
  caller to own the `expanded` state. **Mitigation**: caller-side state is
  hoisted in the new API; this is the standard pattern and matches the rest
  of the design system.
- **Risk**: Token defaults chosen as `MaterialTheme.colorScheme.*` may not
  visually match today's ad-hoc colors exactly. **Mitigation**: pre-PR visual
  diff against current `main` build on emulator; if mismatch, choose a closer
  placeholder in `default()`.
- **Risk**: Build guard `verifyDesignSystemImports` could reject the new
  exports. **Mitigation**: the new files live in `core/designsystem/` which
  the guard allows; dashboard callsites are checked in PR-E.

## 13. Open Questions

None at draft time. Resolved during brainstorming 2026-08-25:

- Scope is dashboard only.
- Phase 2/3/4 are separate specs.
- Token values are deferred to Phase 2.
- Approach C (refactor + token extension) selected over A and B.
