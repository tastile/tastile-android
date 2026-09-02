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
- Pin location: `gradle/libs.versions.toml` (`material3 = "1.5.0-alpha27"`),
  surfaced to `app/build.gradle.kts` and `:designsystem:build.gradle.kts` via
  the version catalog.
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

No new `// m2-allow:` markers were added by this migration. Run

```bash
git grep -n "// m2-allow:" app/src/main/java/app/tastile/android/ui/dashboard \
                        app/src/main/java/app/tastile/android/ui/mobile \
                        app/src/main/java/app/tastile/android/ui/account
```

post-merge to confirm only the pre-existing markers remain.

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

## Phase 3 evidence ledger

- Phase 0: `verifyDesignSystemImports` PASS, `verifyNoEmbeddedServerSecrets` PASS,
  `verifyV1ApiCoverage` PASS.
- Phase 1: component-level contract tests pass for `LoadingWheel`,
  `TastileFabMenu`, `TastileButtonGroup`.
- Phase 2: `TimelineScreenFabTest` (2 tests, green) and `TilesScreenFabTest`
  (2 tests, green) commit the post-swap behavior at the screen boundary.
- Phase 3 docs (this file): README "Material 3 Expressive" subsection added;
  alpha pin recorded; re-capture notice filed.
