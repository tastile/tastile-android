# M3 Baseline — Material 3 Expressive Migration

- **Date:** 2026-09-03
- **Branch:** main
- **Task:** M3 Expressive migration (Phase 0 + Phase 1 + Phase 2 + Phase 3 docs)
- **Plan:** [`docs/superpowers/plans/2026-09-02-m3-expressive.md`](../../plans/2026-09-02-m3-expressive.md)
- **Design spec:** [`docs/superpowers/specs/2026-09-02-m3-expressive-design.md`](../../specs/2026-09-02-m3-expressive-design.md)

## Material 3 pin

- `androidx.compose.material3:material3:1.5.0-alpha27` is the only M3 version
  in this repo. Bumping the alpha must follow the experiment notes in the plan
  and re-capture this baseline.
- Pin location: `app/build.gradle.kts:579` as a literal coordinate string
  (`implementation("androidx.compose.material3:material3:1.5.0-alpha27")`).
  There is **no** `gradle/libs.versions.toml` in this repo — coordinate strings
  are inlined at the module that consumes them.
- This repo is single-module: `settings.gradle.kts` includes only `:app` and
  `:lint-rules` (no `:designsystem` Gradle module). The design system lives
  inside `:app` at `app/src/main/java/app/tastile/android/core/designsystem/`
  and reads the same `1.5.0-alpha27` coordinate via the `:app` build file.
- API stability: `1.5.0-alpha` is an experimental release line. Every import
  that touches `androidx.compose.material3.*` symbols introduced in alpha is
  marked `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)` at the
  design-system component site that introduces the dependency.

## Design system surface (this migration)

- **LoadingWheel rewrite** — `NiaLoadingWheel` / `NiaOverlayLoadingWheel`
  public signatures unchanged; internals rewritten on top of the alpha
  `LoadingIndicator` composable. Phase 1a.
- **MotionScheme injection** — `TastileTheme` provides
  `MaterialTheme(motionScheme = MotionScheme.expressive())`. The injection is
  applied uniformly so any screen reaching for `MaterialTheme.motionScheme`
  gets the expressive defaults. Phase 0.
- **TastileFabMenu** — new M3 Expressive FAB Menu wrapper at
  `app/src/main/java/app/tastile/android/core/designsystem/component/TastileFabMenu.kt`.
  Wired into `TimelineScreen` and `TilesScreen` (Phase 2). Contract: single
  collapsed FAB; tap → `onExpandedChange(!expanded)`. Multi-item expansion
  fan-out is an alpha feature available for follow-up screens.
- **TastileButtonGroup** — new SegmentedButton wrapper at
  `app/src/main/java/app/tastile/android/core/designsystem/component/TastileButtonGroup.kt`.
  Sizes XS through XL; minimum 48dp touch target; consumes `ButtonGroupItem`
  + `TastileButtonGroupTokens` from the design system. Phase 1c.
- **Shape tokens** — expressive corner-family tokens added in Phase 0
  (`TastileShapes`), aligned with the new component geometry.

## Build guards in force

- `verifyDesignSystemImports` — direct `androidx.compose.material3.*` imports
  remain forbidden in `app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}/`
  unless preceded by `// m2-allow:` on the line immediately above. M3 unified
  screens route through the design system instead. M3 Expressive symbols
  must come from `core/designsystem/component/*`, never from the screen tree
  directly.
- `verifyNoEmbeddedServerSecrets` — unchanged; server-only bridge
  credentials stay out of Android sources and the build script.
- `verifyV1ApiCoverage` — unchanged; 22/22 v1 operations still wired.

**Marker delta.** The migration's net `// m2-allow:` marker change is
**+1** (three added, two removed):

- **+3 added** in Phase 0 commit `275eeb1` (alpha27 pin) — one each in
  `AutoCompleteTextField.kt`, `SettingsScreen.kt`, `QuickCreateTaskPanel.kt`.
  Each marker covers a single direct `androidx.compose.material3.*` import
  for the `ExposedDropdownMenu` extension import + no-arg
  `Modifier.menuAnchor()` overload removal that alpha27 introduced. These
  are minimal, single-line escapes, not boundary widenings.
- **-2 removed** in Phase 2 commit `8085e77` — two `// m2-allow: primitive`
  markers deleted from `TimelineScreen.kt` when `NiaFloatingActionButton`
  was replaced by `TastileFabMenu`. The direct M3 imports those markers
  guarded (`Icon`, `LocalContentColor`) became unused.

Pre-existing marker count (pre-migration): 500. Post-migration: 501. To
audit the post-merge state:

```bash
git grep -n "// m2-allow:" app/src/main/java/app/tastile/android/ui/dashboard \
                        app/src/main/java/app/tastile/android/ui/mobile \
                        app/src/main/java/app/tastile/android/ui/account
```

The post-migration count should be 501; the +1 net should be attributable
to the three Phase 0 files above. If you see more growth, it is a new
escape that needs root-fixing under the `verifyDesignSystemImports` guard.

## Compose Compiler Reports — re-capture notice

Compose Compiler Reports are produced under `app/build/compose-reports/` and
`app/build/compose-metrics/` on every build that runs the Compose Compiler
plugin. The pre-migration baseline at `docs/superpowers/m3/before-reports/` is
**not** a captured report file — this README documents the alpha pin and the
re-capture expectation.

After this migration lands, re-run the Compose Compiler Report generator
locally and commit the regenerated trees:

```bash
./gradlew :app:generateDebugComposeMetrics
./gradlew :app:releaseComposeMetrics
```

Commit only the artifacts that changed (compare against the prior tree
under `app/build/compose-reports/` and `app/build/compose-metrics/`); do
**not** check in build outputs by default — they are normally `.gitignore`d.
The Compose Compiler Reports configuration lives in `app/build.gradle.kts`
under the `composeCompiler { reportsDestination.set(...) }` block; see
that file for the on-disk path before re-running.

If a CI run is the only place these reports are captured, document the
baseline path here once a CI artifact is published.

## Device-blocked follow-ups (not in this baseline)

Tasks 3.1 (`gfxinfo` capture on a running emulator) and 3.2 (instrumented
`QuickCreateSmokeTest` via `connectedDebugAndroidTest`) require an attached
ADB device and are out of scope for this baseline document. They will land
separately when the device path is restored; once captured, link the
artifacts here.

**Device-attempt status (2026-09-03):** a XIG03 / Android 15 device was
attached and the tasks were re-attempted. Both remain unfinished for
**new** reasons outside the M3 plan's scope:

- The pre-existing `ExecutionAlarmRescheduleReceiver` crash on its
  five subscribed broadcasts (notably `BOOT_COMPLETED`, queued since
  device boot) was a blocker for the instrumented run. The
  production receiver was rewritten to use
  `EntryPointAccessors.fromApplication(...)` wrapped in try/catch
  on `IllegalStateException` so the broadcast is dropped when Hilt
  is not yet initialized; this fix is out of M3 plan scope but was
  authorized to land Task 3.2 without bypassing auth. See
  [`docs/superpowers/m3/phase-3-deferral.md`](phase-3-deferral.md)
  for the full chain.
- The auth gate still blocks the gfxinfo run from reaching
  TimelineScreen. Cold-launch render only.

The cold-launch gfxinfo capture landed at
[`docs/superpowers/m3/gfxinfo/XIG03-Android-15-2026-09-03.txt`](../gfxinfo/XIG03-Android-15-2026-09-03.txt)
as a toolchain-evidence artifact (not a motion-physics verdict).

**Formal deferral:** see
[`docs/superpowers/m3/phase-3-deferral.md`](phase-3-deferral.md) for the
plan amendment that records these as deferred pending device, quotes the
verbatim acceptance criteria, lists the evidence required to close
them on a device-available runner, and documents the 2026-09-03 device
attempt plus the two new blockers.

## Phase 3 evidence ledger

- Phase 0: `verifyDesignSystemImports` PASS, `verifyNoEmbeddedServerSecrets` PASS,
  `verifyV1ApiCoverage` PASS.
- Phase 1: component-level contract tests pass for `LoadingWheel`,
  `TastileFabMenu`, `TastileButtonGroup`.
- Phase 2: `TimelineScreenFabTest` (2 tests, green) and `TilesScreenFabTest`
  (2 tests, green) commit the post-swap behavior at the screen boundary.
- Phase 3 docs (this file): README "Material 3 Expressive" subsection added;
  alpha pin recorded; re-capture notice filed.

## Deferred follow-ups (whole-branch review)

The whole-branch review at the end of Phase 3 produced four non-blocking
findings. They are tracked here for visibility but were intentionally left
un-fixed in this migration to keep the scope tight:

- **#3 — Phase-2 callback smell.** Both Phase 2 wiring sites
  (`TimelineScreen.kt`, `TilesScreen.kt`) pass
  `onExpandedChange = { newExpanded -> overlay.show(Overlay.QuickCreate) }`
  — the lambda ignores its parameter and unconditionally fires `overlay.show`.
  This works for the current collapsed-only single-item FAB but creates a
  trap for any Phase-3+ PR that flips `expanded` to mutable state: when
  the FAB expands to a real menu, `onExpandedChange(true)` would still fire
  `overlay.show` and bypass the menu. Future fix: gate on `newExpanded`
  (route only on the collapsed → expanded transition) or split the
  dispatch site to the `FabMenuItem.Action.onClick` only.
- **#4 — Commit hygiene.** `bf4b738` and `1da0bb1` share the identical
  message `test(designsystem): pin TastileButtonGroup contracts` despite
  materially different content (one adds the test file, the other adds
  the `@RunWith(RobolectricTestRunner::class)` annotation). Future fix:
  rewrite history with `git rebase -i` to either squash the two commits
  or rename the second to `fix(test): add Robolectric runner to
  TastileButtonGroupTest`. Not done here because rewriting `main` mid-
  migration carries its own risk and the current state is unambiguous
  to anyone reading `git show`.
- **#6 — `fastSpatialSpec()` vs plan's `defaultSpatialSpec()`.**
  `TastileFabMenu.kt:43` uses `MaterialTheme.motionScheme.fastSpatialSpec()`
  rather than the plan's specified `defaultSpatialSpec()`. Both exist in
  `material3:1.5.0-alpha27`. Cosmetic. Future fix: align to plan or update
  the plan to match the implementation.
- **#7 — `TastileButtonGroup` is unused by screens.** Phase 1c shipped a
  new SegmentedButton wrapper, but Phase 2 did not migrate any consumer.
  Pre-existing screens still use raw `androidx.compose.material3.SegmentedButton`
  with `// m2-allow:` markers (`QuickCreateSheet.kt`, `QuickCreateTaskPanel.kt`,
  `TileActionDialogs.kt`). This is **in-spec per the plan's Non-Goal**:
  "既存 `ViewToggle` / `SegmentedButton` の全面置換（当面は残置。新規画面は
  TastileButtonGroup を使う）". Future Phase-3+ work should fold raw M3
  usages into `TastileButtonGroup`.
