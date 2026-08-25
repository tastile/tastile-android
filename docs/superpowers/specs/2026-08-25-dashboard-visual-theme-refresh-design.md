# Phase 2 — Dashboard Visual & Theme Refresh — Design Spec

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current M3-default theming in `tastile-android` with a token-driven theme that supports Material You dynamic color on Android 12+, falls back to M3 defaults on pre-12, wires Noto Sans JP typography, defines a rounded shape vocabulary (16/20/28 dp), and migrates every consumer in `ui/{dashboard, mobile, account}/` to the new token surface so no raw `MaterialTheme.colorScheme` or hardcoded corner radii remain.

**Architecture:** `TastileTheme` becomes the single Compose entry point for both color and shape; `MaterialTheme` continues to host color scheme and typography globally. New `TastileShapeTokens` (CompositionLocal) plus `TastileShapes` (MaterialTheme.shapes adapter) carry corner radius values. `TastileTypography` is rewritten to use `FontFamily(Font(R.font.noto_sans_jp_*))`. Consumer files read from `LocalTastileCardRoleTokens` / `LocalTastileStatusTokens` / `LocalTastileShapeTokens` instead of `MaterialTheme.colorScheme` / hardcoded dp. `verifyDesignSystemImports` gains two new rules.

**Tech Stack:** Kotlin 2.1.0, AGP 9.2.1, Compose Compiler 2.1.0, Compose BOM 2024.12.01, Material 3, Hilt 2.60.1, KSP 2.1.0-1.0.29.

**Spec authority:** This spec overrides Phase 1's `docs/superpowers/specs/2026-08-25-dashboard-component-refactor-design.md` for any conflict on token wiring, `// m2-allow:` audit policy, and `TastileTheme` API. Phase 1 components (file paths, signatures, test contracts) remain unchanged.

**Plan:** `docs/superpowers/plans/2026-08-25-dashboard-visual-theme-refresh.md` (produced after spec sign-off).

## Context (Phase 1 outcomes)

Phase 1 (commit range `228f300..8104b16`) established:

- 5 new design-system components in `app/src/main/java/app/tastile/android/core/designsystem/component/`: `TastileStatusCircle`, `TastileCompactTileRow`, `TastileCardActions` (sealed), `TastileCardActionRow`, `TastileDashboardCardShell`, `TastileTileCard`.
- 4 token families in `app/src/main/java/app/tastile/android/core/designsystem/theme/`: `TastileStatusTokens`, `TastileCardRoleTokens`, `TastileSurfaceElevationTokens`, `TastileSpacingTokens` — each with a `default(...)` or `.Default` factory and a `staticCompositionLocalOf` provider in `ThemeTokenLocals.kt`.
- `TastileTheme` in `Theme.kt` wires all 4 token families via `CompositionLocalProvider` and wraps `MaterialTheme(colorScheme = colorScheme, typography = TastileTypography, content)`.
- `verifyDesignSystemImports` build guard forbids direct `androidx.compose.material3.*` imports in `ui/{dashboard, mobile, account}/` unless preceded by an `// m2-allow:` marker.
- 3 `// m2-allow:` markers survive in `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt` (`Icon`, `MaterialTheme`, `Text`) with site-specific justifications.

Phase 1 used M3 defaults for `colorScheme` (`darkColorScheme()` / `lightColorScheme()`), kept M3 default typography sizes, and did not define custom shape tokens.

## Goals

1. **G1**: `TastileTheme` honors Material You dynamic color when `Build.VERSION.SDK_INT >= S` and the host supports it; pre-12 / unsupported devices keep M3 defaults.
2. **G2**: `TastileTypography` uses Noto Sans JP for Japanese glyphs (Latin falls back to M3 default family, which is Roboto on Android).
3. **G3**: A new `TastileShapeTokens` CompositionLocal defines `xs=4dp / s=8dp / m=16dp / large=20dp / xl=28dp` and is wired into `MaterialTheme.shapes` via `TastileShapes(MaterialShapes)`.
4. **G4**: Every `MaterialTheme.colorScheme.<key>` reference in `ui/dashboard/`, `ui/mobile/`, `ui/account/` is replaced by an equivalent `LocalTastileCardRoleTokens.current.<role>.<slot>` or `LocalTastileStatusTokens.current.<lifecycle>.<slot>` lookup. Every `RoundedCornerShape(N.dp)` reference in those packages is replaced by `RoundedCornerShape(LocalTastileShapeTokens.current.<key>)`.
5. **G5**: `verifyDesignSystemImports` rejects (a) `MaterialTheme.colorScheme` references in `ui/{dashboard, mobile, account}/` and (b) `RoundedCornerShape(<numeric>.dp)` references anywhere outside `app/src/main/java/app/tastile/android/core/designsystem/`.
6. **G6**: After Phase 2 lands, `// m2-allow:` markers in `ui/dashboard/DashboardScreens.kt` reduce to 0 or 1 (only `MaterialTheme.typography` access may remain if no token wrapper exists). All other `ui/` files have 0 markers.
7. **G7**: `./gradlew verify` is green on local `main` after Task #18 (Robolectric `app/src/test/resources/AndroidManifest.xml`) lands.

## Non-goals

- NG1: Defining a brand palette (e.g., `TastilePalette`). Brand fallback on pre-12 is `lightColorScheme()` / `darkColorScheme()`. Dynamic color is the brand on 12+.
- NG2: Custom typography sizes. M3 default size scale stays.
- NG3: Adding `TastileTextStyles` wrapper around `MaterialTheme.typography`. Phase 2 keeps typography accessed via `MaterialTheme.typography.*`; a typography token layer is deferred to a later phase.
- NG4: Animations / motion / transitions. That is Phase 3.
- NG5: Migrating components outside `ui/{dashboard, mobile, account}/` (e.g., `core/designsystem/component/Button.kt`, `Card.kt`, `TopAppBar.kt`).
- NG6: Changing `colors.xml` or `themes.xml`. Window-level theming remains unchanged.
- NG7: Locale-specific font weight or letterSpacing overrides. Noto Sans JP is the family; size/weight/letterSpacing per M3 default scale.

## Architecture decisions

| # | Decision | Source |
| --- | --- | --- |
| AD1 | Material You dynamic + M3 default fallback | Q1 |
| AD2 | Scope = `ui/dashboard/ + ui/mobile/ + ui/account/` | Q2 |
| AD3 | Typography = Noto Sans JP | Q3 |
| AD4 | Shape = 16 / 20 / 28 dp rounded | Q4 |
| AD5 | Brand fallback = M3 default (no TastilePalette) | Q5 |
| AD6 | Approach = Full token migration (Theme + Token + Consumer + Guard) | Q6 |

## Global Constraints

These constraints bind every Phase 2 task. They reproduce the binding requirements from CLAUDE.md / AGENTS.md.

- **GC1**: Work on local `main`. No worktree, no feature branch, no temporary branch (CLAUDE.md "Work on local `main`. Do not create feature branches, temporary branches, or worktrees.").
- **GC2**: Source code, identifiers, code comments, and Git/GitHub messages are English. Internal development docs are Japanese (CLAUDE.md).
- **GC3**: Do not write new Python scripts in this repo. Use Kotlin, shell, or PowerShell (CLAUDE.md).
- **GC4**: Never commit `local.properties`, `google-services.json`, keystores, `.env*` with real values, generated `app/src/main/jniLibs/`, or anything in `reference/`, `.build-logs/`, `.tools/` (CLAUDE.md).
- **GC5**: Every `BuildConfig.*` field listed in `app/build.gradle.kts` must remain non-blank. The Phase 2 plan does not touch the `BuildConfig.*` block.
- **GC6**: Release tasks (`assembleRelease`, `bundleRelease`) must fail fast without signing props; Phase 2 does not touch release signing.
- **GC7**: Before claiming "PASS / DONE / GREEN / ready to ship", run `./gradlew verify` from a clean state. If change is in `:app` source, also run the unit-test target (CLAUDE.md).
- **GC8**: Direct `androidx.compose.material3.*` imports in `ui/{dashboard, mobile, account}/` are forbidden unless the immediately preceding non-blank line is `// m2-allow:` (CLAUDE.md build guard `verifyDesignSystemImports`).
- **GC9**: The lint block in `app/build.gradle.kts` must not add `disable +=` (CLAUDE.md).
- **GC10**: `reference/` clones are read-only; they must not become implicit build or runtime dependencies (CLAUDE.md).
- **GC11**: Phase 2 PR boundaries respect AGENTS.md's file-ownership rule — "同一 file の並列編集は禁止" (no parallel edits to the same file).
- **GC12**: Phase 2 task briefs follow SDD's no-subagent contract — implementer does not dispatch subagents; review arrives from the controller.

## Token layer — `TastileShapeTokens`

### File: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileShapeTokens.kt` (NEW)

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Rounded corner radius vocabulary. Bound to Material 3's small/medium/large/extralarge
 * shape slots (xs -> extraSmall, s -> small, m -> medium, large -> large, xl -> extraLarge).
 */
@androidx.compose.runtime.Immutable
data class TastileShapeTokens(
    val xs: Dp,
    val s: Dp,
    val m: Dp,
    val large: Dp,
    val xl: Dp,
) {
    companion object {
        val Default = TastileShapeTokens(
            xs = 4.dp,
            s = 8.dp,
            m = 16.dp,
            large = 20.dp,
            xl = 28.dp,
        )
    }
}

val LocalTastileShapeTokens = staticCompositionLocalOf {
    error("TastileShapeTokens not provided. Wrap your content in TastileTheme.")
}
```

### File: `app/src/main/java/app/tastile/android/core/designsystem/theme/ThemeTokenLocals.kt` (MODIFY)

Add the `LocalTastileShapeTokens` declaration alongside the four existing locals. The error message mirrors the existing four ("TastileShapeTokens not provided. Wrap your content in TastileTheme.").

### File: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileShapes.kt` (NEW)

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Adapter that exposes TastileShapeTokens through MaterialTheme.shapes.
 * Component code that still uses MaterialTheme.shapes.<key> will read from these values.
 */
val TastileShapes: Shapes
    @Composable
    @ReadOnlyComposable
    get() = Shapes(
        extraSmall = RoundedCornerShape(LocalTastileShapeTokens.current.xs),
        small = RoundedCornerShape(LocalTastileShapeTokens.current.s),
        medium = RoundedCornerShape(LocalTastileShapeTokens.current.m),
        large = RoundedCornerShape(LocalTastileShapeTokens.current.large),
        extraLarge = RoundedCornerShape(LocalTastileShapeTokens.current.xl),
    )
```

### File: `app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt` (MODIFY)

- Read `LocalContext.current` and `Build.VERSION.SDK_INT` to pick dynamic vs static color scheme.
- Wrap the existing `CompositionLocalProvider` block in a function that also provides `LocalTastileShapeTokens provides TastileShapeTokens.Default`.
- Pass `shapes = TastileShapes` to the `MaterialTheme(...)` call.

### File: `app/src/main/java/app/tastile/android/core/designsystem/theme/Type.kt` (MODIFY)

Replace the `internal val TastileTypography = Typography(...)` with:

- A `private val NotoSansJp = FontFamily(...)` referencing `R.font.noto_sans_jp_regular` / `medium` / `bold`.
- A `TastileTypography` that sets each `TextStyle.fontFamily = NotoSansJp` while keeping every other TextStyle field identical to the current M3-compliant values.

`R.font.*` resources live in `app/src/main/res/font/noto_sans_jp_regular.ttf` (and `medium.ttf`, `bold.ttf`). The OFL font files are added by the plan's "Asset acquisition" task.

## Theme layer — Material You dynamic color

### `Theme.kt` modification

```kotlin
@Composable
fun TastileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamicColor && supportsDynamic && darkTheme  -> dynamicDarkColorScheme(context)
        dynamicColor && supportsDynamic && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme                                     -> darkColorScheme()
        else                                          -> lightColorScheme()
    }
    // ... existing gradientColors / backgroundTheme / tintTheme ...
    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
        LocalTastileStatusTokens provides TastileStatusTokens.default(colorScheme),
        LocalTastileCardRoleTokens provides TastileCardRoleTokens.default(colorScheme),
        LocalTastileSurfaceElevationTokens provides TastileSurfaceElevationTokens.Default,
        LocalTastileSpacingTokens provides TastileSpacingTokens.Default,
        LocalTastileShapeTokens provides TastileShapeTokens.Default,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TastileTypography,
            shapes = TastileShapes,
            content = content,
        )
    }
}
```

## Token layer — `TastileCardRoleTokens` / `TastileStatusTokens` extension

The current Phase 1 token families already derive from `colorScheme`, so dynamic color propagates automatically. Phase 2 does not modify the `default(scheme)` body but adds the following structural improvements:

- `TastileCardRoleTokens` gains explicit `accent`, `onAccent`, `container`, `onContainer` slots per role (already present in Phase 1 but no consumers yet in `ui/mobile/` or `ui/account/`).
- `TastileStatusTokens` gains explicit `border` and `glyph` slots for the four lifecycles (already implicit in Phase 1).
- Both `data class` definitions get an `@Immutable` annotation.

These are documented in spec but the actual color slots already exist; only the `@Immutable` annotation is new code.

## Consumer migration — file inventory

The following files contain `MaterialTheme.colorScheme.<key>` or `RoundedCornerShape(<numeric>.dp)` references that must be migrated.

### `ui/dashboard/`

| File | Expected changes |
| --- | --- |
| `DashboardScreens.kt` | 3 `// m2-allow:` markers (lines 16, 18, 20) reduced to 1 (only `MaterialTheme.typography` if needed). `MaterialTheme.colorScheme.primary/secondary/tertiary/surface/onSurface` references in `DashboardCardRenderer` rewritten via `LocalTastileCardRoleTokens.current.<role>.<slot>`. Hardcoded `2.dp` / `6.dp` / `8.dp` / `10.dp` literals (the `padding(horizontal = 2.dp)` at `TimelineCard` branch and `Arrangement.spacedBy(6.dp)` line) move to `LocalTastileSpacingTokens.current.xs/s`. |
| `DashboardViewModel.kt` | If it touches `MaterialTheme.colorScheme` (none expected — VM holds state only). |
| Any other dashboard file | Per audit. |

### `ui/mobile/`

| File | Expected changes |
| --- | --- |
| (files to be enumerated by plan's audit task) | Each `MaterialTheme.colorScheme.<key>` replaced; each `RoundedCornerShape(N.dp)` replaced; `// m2-allow:` markers removed. |

### `ui/account/`

| File | Expected changes |
| --- | --- |
| (files to be enumerated by plan's audit task) | Same rules as `ui/mobile/`. |

### `core/designsystem/component/` (Phase 1 components)

| File | Expected changes |
| --- | --- |
| `TastileCompactTileRow.kt` | Hardcoded `10.dp` horizontal padding → `LocalTastileSpacingTokens.current.m`. |
| `TastileDashboardCardShell.kt` | Hardcoded `6.dp` vertical padding → `LocalTastileSpacingTokens.current.s`. |
| `TastileTileCard.kt` | Hardcoded `8.dp` / `10.dp` literals → `LocalTastileSpacingTokens.current.s/m`. |
| `TastileCardActionRow.kt` | Hardcoded `8.dp` spacedBy → `LocalTastileSpacingTokens.current.s`. |
| `TastileStatusCircle.kt` | Reviewer-flagged: legacy `StatusCircle` was 20.dp; add `.size(LocalTastileShapeTokens.current.large)` after the testTag modifier to preserve visual size (Ruling from Phase 1 final review). |

These are within the design-system module so they are NOT subject to the `// m2-allow:` audit but they MUST use the new tokens because they are the visible consumers.

## Guard — `verifyDesignSystemImports` extension

`app/build.gradle.kts` registers `verifyDesignSystemImports`. Phase 2 extends its file scan to:

1. For each Kotlin file under `app/src/main/java/app/tastile/android/ui/{dashboard, mobile, account}/`, fail if the file contains `MaterialTheme.colorScheme` AND the immediately preceding non-blank line is not `// m2-allow:`. The marker is permitted only if the marker text contains "shape" or "typography" — color references must move to tokens.
2. For each Kotlin file outside `app/src/main/java/app/tastile/android/core/designsystem/`, fail if the file contains `RoundedCornerShape(` followed by a numeric literal and `dp)` within the same argument list. Allowed: `RoundedCornerShape(LocalTastileShapeTokens.current.<key>)` and `RoundedCornerShape(0.dp)` (full radius use) and `RoundedCornerShape(percent)` (percent-based shape).

The extension uses the same scanning approach already in place (string match across file content). Implementation detail deferred to the plan.

## Testing strategy

### Robolectric enablement (Task #18 first)

`app/src/test/resources/AndroidManifest.xml` is added before any Phase 2 task that depends on `createComposeRule()`. The manifest lists `<activity android:name="androidx.activity.ComponentActivity"/>` per Robolectric PR #4736. This unblocks all 15+ Compose UI tests including Phase 1's 5 new test files.

### Token unit tests

- `TastileShapeTokensTest`: assert default values `xs=4dp / s=8dp / m=16dp / large=20dp / xl=28dp` and immutability.
- `TastileTypographyTest`: assert every TextStyle in `TastileTypography` has `fontFamily = NotoSansJp` (Latin fallback uses M3 default Roboto).
- `TastileThemeTest`: Robolectric, assert `LocalTastileShapeTokens.current` returns `Default` inside `TastileTheme {}`; assert `MaterialTheme.shapes.medium == RoundedCornerShape(16.dp)`; assert dynamic color branch produces `dynamicLightColorScheme(context)` on API 31+ (using `RuntimeEnvironment.setApiLevel(33)`).

### Consumer migration tests

- `@ThemePreviews` updates: every new Tastile component gets `lightTheme()` + `darkTheme()` + `dynamicOn_theme()` previews (the third uses a stub `colorScheme` to avoid Robolectric runtime).
- Existing tests that call `TastileTheme { ... }` directly continue to compile (token wiring is additive).

### Build verification

- `./gradlew verify` must pass on local `main` after all waves land.
- `./gradlew :app:verifyDesignSystemImports` must pass after the consumer migration wave (zero `MaterialTheme.colorScheme` references, zero hardcoded `RoundedCornerShape(N.dp)` outside design-system).
- `./gradlew :app:compileDebugKotlin` must pass.

## Wave decomposition (proposed)

The plan produces the following PR boundaries. Each PR lands a self-contained reviewable change set.

### PR-A — Token foundations

- Task 1 (NEW): `TastileShapeTokens.kt` + `LocalTastileShapeTokens` in `ThemeTokenLocals.kt`.
- Task 2 (NEW): `TastileShapes.kt` (MaterialTheme.shapes adapter).
- Task 3 (MODIFY): `Theme.kt` — wire `LocalTastileShapeTokens` and `shapes = TastileShapes`; add `dynamicColor` parameter and `dynamicLight/DarkColorScheme(context)` branch on API >= 31.
- Task 4 (NEW): `TastileShapeTokensTest.kt`.
- Task 5 (MODIFY): `Type.kt` — introduce `NotoSansJp` FontFamily; assign to every `TextStyle.fontFamily` in `TastileTypography`.
- Task 6 (NEW): `TastileTypographyTest.kt` (assert font family binding).
- Task 7 (NEW): Asset acquisition — `app/src/main/res/font/noto_sans_jp_{regular,medium,bold}.ttf` from Google Fonts (OFL).

### PR-B — Guard extension

- Task 8 (MODIFY): `app/build.gradle.kts` — extend `verifyDesignSystemImports` scan to forbid `MaterialTheme.colorScheme` references and hardcoded `RoundedCornerShape(N.dp)` outside design-system.
- Task 9 (NEW): `app/src/test/java/.../VerifyDesignSystemImportsTest.kt` — asserts the guard flags forbidden patterns.

### PR-C — Consumer migration (Dashboard)

- Task 10 (MODIFY): `ui/dashboard/DashboardScreens.kt` — token migration, `// m2-allow:` marker reduction.
- Task 11 (NEW): `app/src/test/java/.../ui/dashboard/DashboardScreensTokensTest.kt` — assert no `MaterialTheme.colorScheme` references in dashboard file under test (lint-style assertion).

### PR-D — Consumer migration (Mobile)

- Task 12 (MODIFY): `ui/mobile/*.kt` files — same rules as PR-C.

### PR-E — Consumer migration (Account)

- Task 13 (MODIFY): `ui/account/*.kt` files — same rules as PR-C.

### PR-F — Design-system component dp literal migration

- Task 14 (MODIFY): 5 Phase 1 components (`TastileStatusCircle`, `TastileCompactTileRow`, `TastileDashboardCardShell`, `TastileTileCard`, `TastileCardActionRow`) — hardcoded dp literals → `LocalTastileSpacingTokens.current.*` and `LocalTastileShapeTokens.current.large` for `TastileStatusCircle.size`.

### PR-G — Audit + cleanup

- Task 15 (MODIFY): `ui/dashboard/mobile/account/**/*.kt` — `// m2-allow:` marker audit + dead import cleanup (carry-over from Phase 1 minor issues: 2 dead imports, 8 missing trailing newlines, 4 missing `@RunWith(RobolectricTestRunner::class)` annotations).
- Task 16 (MODIFY): Pre-existing dashboard test files (QuickCreatePanels, ProjectsScreen, TasksScreen) — `@RunWith(RobolectricTestRunner::class)` annotation add (carried from Phase 1 final review Minor item).

### PR-H — Final whole-branch review

- Task 17 (NEW): Final whole-branch review (opus tier), spec compliance + visual diff verification.

## Risk register

| Risk | Probability | Mitigation |
| --- | --- | --- |
| Material You dynamic color breaks an existing UI test that assumes M3 defaults | Medium | All token tests use `TastileTheme(dynamicColor = false)` explicitly. |
| Noto Sans JP font file adds 1-2 MB to APK | High (this is known) | Acceptable; document in PR description. If size-constrained, fall back to system font subset via `FontFamily(Typeface.create("sans-serif-medium", Typeface.NORMAL))` and ship OFL files as runtime download (out of scope for Phase 2). |
| Robolectric env gap (Task #18) blocks all 13 Compose UI tests | High | Land Task #18 in PR-A before any test work. |
| `verifyDesignSystemImports` extension has false positives | Medium | Test against the existing 13 Compose UI test files in `app/src/test/java/.../designsystem/component/`. They are inside `core/designsystem/` so they pass. |
| Visual regression on legacy StatusCircle size | Low | Phase 1 reviewer flagged this; Task 14 explicitly sets `.size(LocalTastileShapeTokens.current.large)` to preserve 20.dp. |
| `MaterialTheme.shapes` consumers break because `TastileShapes` adds `shapes =` parameter | Low | `MaterialTheme(shapes = ...)` is the correct API; existing components that read `MaterialTheme.shapes.<key>` automatically pick up the new values. |

## Rollback

Phase 2 lands PR-by-PR on local `main`. Each PR is self-contained and revertible via `git revert <sha>`. If a PR breaks `./gradlew verify`, the controller reverts that PR before continuing.

If PR-A (Token foundations) needs to back out: revert commits in reverse order; `TastileTheme` falls back to current M3-default behavior.

If PR-B (Guard) is too strict: relax the `MaterialTheme.colorScheme` blocklist to allow specific keys (`primary`, `onPrimary`, etc.) before any consumer migration lands.

## Open questions for the implementation plan

These are deferred to plan-writing; they are spec-level concerns the plan must address but do not change the spec.

1. **Plan**: which exact files in `ui/mobile/` and `ui/account/` contain `MaterialTheme.colorScheme` references? The plan's audit task enumerates them before dispatching migration tasks.
2. **Plan**: are there any Compose UI tests in `ui/mobile/` or `ui/account/` that require Robolectric before they can run? (Likely no; these directories are state holders.)
3. **Plan**: how does the plan keep `TastileTileCard`'s trailing-lambda foot-gun (Phase 1 Ruling #7) intact while migrating the action row's `8.dp` literal? (Reorder is Phase 3; Phase 2 only touches literals.)

## Self-review checklist (pre-commit)

Before committing this spec, the author must verify:

- [x] No "TBD" / "TODO" / "fill in details" placeholders.
- [x] Internal consistency — token names match between Theme.kt, ThemeTokenLocals.kt, TastileShapes.kt, and consumer examples.
- [x] Scope check — single plan boundary, single deliverable set (visual/theme refresh + consumer migration).
- [x] Ambiguity check — every consumer rule is binary ("must be replaced with X" or "may remain Y").
- [x] Global Constraints list reproduces CLAUDE.md / AGENTS.md binding requirements verbatim.
