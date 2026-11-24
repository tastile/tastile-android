# Phase 2 — Dashboard Visual & Theme Refresh — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace M3-default theming with Material You dynamic color (API 31+) + M3 fallback, Noto Sans JP typography, a 16/20/28 rounded shape vocabulary, and migrate every consumer in `ui/{dashboard, mobile, account}/` plus the Phase 1 design-system components to the new token surface.

**Architecture:** `TastileTheme` resolves a `colorScheme` (dynamic on API 31+, else `light/darkColorScheme()`) and provides 5 token CompositionLocals plus `TastileShapes` (MaterialTheme.shapes adapter). `TastileTypography` switches to a `NotoSansJp` `FontFamily`. Consumers stop using `MaterialTheme.colorScheme` and hardcoded `RoundedCornerShape(N.dp)`; they read from `LocalTastileCardRoleTokens` / `LocalTastileStatusTokens` / `LocalTastileShapeTokens` / `LocalTastileSpacingTokens`. `verifyDesignSystemImports` gains two new rules.

**Tech Stack:** Kotlin 2.1.0, AGP 9.2.1, Compose Compiler 2.1.0, Compose BOM 2024.12.01, Material 3 (`androidx.compose.material3.dynamicLightColorScheme` / `dynamicDarkColorScheme`), Hilt 2.60.1, KSP 2.1.0-1.0.29, Robolectric for Compose UI tests.

**Spec:** `docs/superpowers/specs/2026-08-25-dashboard-visual-theme-refresh-design.md` — the plan argues from the spec; conflicts inside the plan resolve against it.

**Spec correction logged before writing the plan:** The spec said `TastileCardRoleTokens` / `TastileStatusTokens` need `@Immutable` annotations. Phase 1 already added both (`@Immutable` is present at `TastileCardRoleTokens.kt:10, 17` and `TastileStatusTokens.kt:11, 23`). Tasks 3 and 4 below are skipped; the spec text is updated after the plan is committed.

## Global Constraints

These bind every Phase 2 task. Reproduce verbatim in each task brief.

- **GC1**: Work on local `main`. No worktree, no feature branch, no temporary branch (CLAUDE.md "Work on local `main`. Do not create feature branches, temporary branches, or worktrees.").
- **GC2**: Source code, identifiers, code comments, and Git/GitHub messages are English. Internal development docs are Japanese (CLAUDE.md).
- **GC3**: Do not write new Python scripts in this repo. Use Kotlin, shell, or PowerShell (CLAUDE.md).
- **GC4**: Never commit `local.properties`, `google-services.json`, keystores, `.env*` with real values, generated `app/src/main/jniLibs/`, or anything in `reference/`, `.build-logs/`, `.tools/` (CLAUDE.md).
- **GC5**: Every `BuildConfig.*` field listed in `app/build.gradle.kts` must remain non-blank. Phase 2 does not touch the `BuildConfig.*` block.
- **GC6**: Release tasks (`assembleRelease`, `bundleRelease`) must fail fast without signing props; Phase 2 does not touch release signing.
- **GC7**: Before claiming "PASS / DONE / GREEN / ready to ship", run `./gradlew verify` from a clean state. If change is in `:app` source, also run the unit-test target (CLAUDE.md).
- **GC8**: Direct `androidx.compose.material3.*` imports in `ui/{dashboard, mobile, account}/` are forbidden unless the immediately preceding non-blank line is `// m2-allow:` (CLAUDE.md build guard `verifyDesignSystemImports`).
- **GC9**: The lint block in `app/build.gradle.kts` must not add `disable +=` (CLAUDE.md).
- **GC10**: `reference/` clones are read-only; they must not become implicit build or runtime dependencies (CLAUDE.md).
- **GC11**: Phase 2 PR boundaries respect AGENTS.md's file-ownership rule — "同一 file の並列編集は禁止" (no parallel edits to the same file).
- **GC12**: Phase 2 task briefs follow SDD's no-subagent contract — implementer does not dispatch subagents; review arrives from the controller.

---

## Pre-flight scan: file inventory for consumer migration

A grep for `MaterialTheme\.colorScheme|RoundedCornerShape\(\d+\.dp\)` across `app/src/main/java/app/tastile/android/ui/` returned **68 files** (excluding `ui/{now, memo, login, billing, prompt}` which are out of Phase 2 scope per spec G4).

| PR | Directory | File count | Migration pattern |
| --- | --- | --- | --- |
| PR-D | `ui/dashboard/` | 4 files (`DashboardScreens.kt`, `ManagementScreens.kt`, `MonthCalendarScreen.kt`, `TimelineScreen.kt`) + 1 subpackage `ui/dashboard/components/` (TBD audit) | color + shape |
| PR-E | `ui/mobile/tabs/` | 8 files (`ExecuteScreen`, `ProjectsScreen`, `SettingsScreen`, `TilesScreen`, `TimelineScreen`, `CalendarEventControls`, `CalendarFilterPanel`, `MobileTopBar` plus `mobile/tabs/tiles/` subpackage) | color + shape |
| PR-F | `ui/mobile/calendar/` | 12 files (all `Day*`, `Month*`, `Week*`, `GridConstants`, `NowIndicator`, `TimeUtils`) | color + shape |
| PR-G | `ui/mobile/sheets/` | 7 files (`NotificationsSheet`, `PanelSheet`, `QuickCreateSheetMobile`, `SearchOverlaySheet`, `SectionPanelContent`, `TileEditSheet`, `quickcreate/`) | color + shape |
| PR-H | `ui/mobile/panels/` | 8 files (`ProjectsSectionContent`, `ReferencesSectionContent`, `schedule/`, `timeline/`, `references/`, `projects/`) | color + shape |
| PR-I | `ui/mobile/components/` + `ui/mobile/{decision,execution,panels}` VMs and `SidePanelDrawerContent`, `AccountDropdownMenu`, `OverlayLayer` | 5 files | color + shape |
| PR-J | `ui/mobile/account/` | 4 files (`AccountSheet`, `SubscriptionSheet`, `TokensSheet`, `AccountViewModel`) | color + shape |
| PR-K | `ui/account/` | 2 files (`AccountScreen`, `AccountViewModel`) | color + shape |

Each consumer migration task is a **directory-batch**: one task description, one implementer, file list inside the brief, single reviewer pass. This matches SDD's "batch small same-shape work" pattern.

---

## PR-A — Token foundations

Tasks 1–6 land the new shape tokens, the MaterialTheme.shapes adapter, the dynamic-color Theme.kt, the Noto Sans JP FontFamily, and unit tests for both.

### Task 1: Create `TastileShapeTokens` + `LocalTastileShapeTokens`

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileShapeTokens.kt`
- Modify: `app/src/main/java/app/tastile/android/core/designsystem/theme/ThemeTokenLocals.kt` (append `LocalTastileShapeTokens` after `LocalTastileSpacingTokens`)

**Interfaces:**
- Consumes: nothing — this is the first Phase 2 task.
- Produces: `data class TastileShapeTokens(val xs: Dp, val s: Dp, val m: Dp, val large: Dp, val xl: Dp)` with `companion object { val Default = TastileShapeTokens(xs=4dp, s=8dp, m=16dp, large=20dp, xl=28.dp) }`; `val LocalTastileShapeTokens = staticCompositionLocalOf<TastileShapeTokens> { error("TastileShapeTokens not provided. Wrap content in TastileTheme { ... }.") }`. Both are `@Immutable` annotated.

- [ ] **Step 1**: Write `TastileShapeTokens.kt`:

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Rounded corner radius vocabulary. Bound to Material 3's small/medium/large/extraLarge
 * shape slots (xs -> extraSmall, s -> small, m -> medium, large -> large, xl -> extraLarge).
 */
@Immutable
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
```

- [ ] **Step 2**: Open `ThemeTokenLocals.kt` and append at the bottom (after `LocalTastileSpacingTokens`):

```kotlin
val LocalTastileShapeTokens = staticCompositionLocalOf<TastileShapeTokens> {
    error("TastileShapeTokens not provided. Wrap content in TastileTheme { ... }.")
}
```

Add `import app.tastile.android.core.designsystem.theme.TastileShapeTokens` if needed (same package — usually not).

- [ ] **Step 3**: Run `./gradlew :app:compileDebugKotlin` — expected BUILD SUCCESSFUL.

- [ ] **Step 4**: Commit:

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/theme/TastileShapeTokens.kt \
        app/src/main/java/app/tastile/android/core/designsystem/theme/ThemeTokenLocals.kt
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "feat(designsystem): add TastileShapeTokens + LocalTastileShapeTokens"
```

### Task 2: Create `TastileShapes` adapter

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileShapes.kt`

**Interfaces:**
- Consumes: `LocalTastileShapeTokens` from Task 1.
- Produces: `val TastileShapes: Shapes` (Composable, ReadOnlyComposable) returning `Shapes(extraSmall = RoundedCornerShape(LocalTastileShapeTokens.current.xs), small = RoundedCornerShape(LocalTastileShapeTokens.current.s), medium = RoundedCornerShape(LocalTastileShapeTokens.current.m), large = RoundedCornerShape(LocalTastileShapeTokens.current.large), extraLarge = RoundedCornerShape(LocalTastileShapeTokens.current.xl))`.

- [ ] **Step 1**: Write `TastileShapes.kt`:

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Adapter that exposes TastileShapeTokens through MaterialTheme.shapes.
 * Component code that reads MaterialTheme.shapes.<key> automatically picks up
 * the values from LocalTastileShapeTokens.
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

- [ ] **Step 2**: Run `./gradlew :app:compileDebugKotlin` — expected BUILD SUCCESSFUL.

- [ ] **Step 3**: Commit:

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/theme/TastileShapes.kt
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "feat(designsystem): add TastileShapes adapter for MaterialTheme.shapes"
```

### Task 3: Update `Theme.kt` for dynamic color + shape wiring

**Files:**
- Modify: `app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt` (replace the body of `TastileTheme`)

**Interfaces:**
- Consumes: `LocalContext` (androidx.compose.ui.platform), `Build.VERSION.SDK_INT` / `Build.VERSION_CODES.S` (android.os), `dynamicLightColorScheme` / `dynamicDarkColorScheme` (androidx.compose.material3) — all standard M3 imports already on the BOM 2024.12.01 classpath.
- Produces: `TastileTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit)` that picks `colorScheme` from `dynamicLight/DarkColorScheme(context)` when `Build.VERSION.SDK_INT >= S` AND `dynamicColor == true`, else falls back to `lightColorScheme()` / `darkColorScheme()`. Inside the `CompositionLocalProvider` block it adds `LocalTastileShapeTokens provides TastileShapeTokens.Default`. The `MaterialTheme(...)` call adds `shapes = TastileShapes`.

- [ ] **Step 1**: Open `Theme.kt` and replace the entire `TastileTheme` function body with:

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

    val gradientColors = GradientColors(
        top = colorScheme.inverseOnSurface,
        bottom = colorScheme.primaryContainer,
        container = colorScheme.surface,
    )

    val backgroundTheme = BackgroundTheme(
        color = colorScheme.surface,
        tonalElevation = 2.dp,
    )

    val tintTheme = TintTheme()

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

Add these imports at the top of the file (preserve existing imports):

```kotlin
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
```

- [ ] **Step 2**: Run `./gradlew :app:compileDebugKotlin` — expected BUILD SUCCESSFUL.

- [ ] **Step 3**: Commit:

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "feat(theme): enable Material You dynamic color with M3 fallback; wire shape tokens"
```

### Task 4: Add `TastileShapeTokensTest`

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/theme/TastileShapeTokensTest.kt`

**Interfaces:**
- Consumes: `TastileShapeTokens.Default` from Task 1.
- Produces: 3 unit tests verifying default values and immutability of the data class.

- [ ] **Step 1**: Write the test file:

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TastileShapeTokensTest {

    @Test fun `Default exposes xs=4dp`() {
        assertEquals(4.dp, TastileShapeTokens.Default.xs)
    }

    @Test fun `Default exposes s=8dp m=16dp large=20dp xl=28dp`() {
        assertEquals(8.dp, TastileShapeTokens.Default.s)
        assertEquals(16.dp, TastileShapeTokens.Default.m)
        assertEquals(20.dp, TastileShapeTokens.Default.large)
        assertEquals(28.dp, TastileShapeTokens.Default.xl)
    }

    @Test fun `data class equality holds`() {
        val a = TastileShapeTokens(4.dp, 8.dp, 16.dp, 20.dp, 28.dp)
        val b = TastileShapeTokens(4.dp, 8.dp, 16.dp, 20.dp, 28.dp)
        assertEquals(a, b)
    }
}
```

- [ ] **Step 2**: Run `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.TastileShapeTokensTest"` — expected 3 tests pass.

- [ ] **Step 3**: Commit:

```bash
git add app/src/test/java/app/tastile/android/core/designsystem/theme/TastileShapeTokensTest.kt
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "test(designsystem): add TastileShapeTokens unit tests"
```

### Task 5: Add Noto Sans JP font assets

**Files:**
- Create: `app/src/main/res/font/noto_sans_jp_regular.ttf`
- Create: `app/src/main/res/font/noto_sans_jp_medium.ttf`
- Create: `app/src/main/res/font/noto_sans_jp_bold.ttf`

**Interfaces:**
- Consumes: not applicable (asset acquisition).
- Produces: three `.ttf` files in `app/src/main/res/font/`, OFL-licensed Noto Sans JP from `https://github.com/notofonts/noto-cjk/tree/main/Sans/OTF/Japanese` (use the variable font subset or the static SubsetOTF JP for size).

- [ ] **Step 1**: Download the Noto Sans JP variable font or three static weight files (Regular, Medium, Bold) from Google Fonts (`https://fonts.google.com/noto/specimen/Noto+Sans+JP`). The project uses OFL-licensed assets, which Noto Sans JP is. Save the files to `app/src/main/res/font/`.

- [ ] **Step 2**: Verify each `.ttf` file is < 1.5 MB. If the variable font file is smaller than the three static weights, prefer the variable font and create static weight fallbacks at runtime via `FontVariation`. (Simpler: ship three static weights for Phase 2; runtime variation is a future optimization.)

- [ ] **Step 3**: Add `app/src/main/res/font/font_certs.xml` if Google Fonts ships a separate cert file (Noto Sans JP ships without certs — skip if absent). Document the OFL license in `app/src/main/res/font/LICENSE.txt` referencing `https://fonts.google.com/noto/specimen/Noto+Sans+JP/license`.

- [ ] **Step 4**: Commit:

```bash
git add app/src/main/res/font/
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "feat(theme): add Noto Sans JP font assets (OFL)"
```

### Task 6: Update `Type.kt` for Noto Sans JP

**Files:**
- Modify: `app/src/main/java/app/tastile/android/core/designsystem/theme/Type.kt` (add `NotoSansJp` FontFamily and apply to every TextStyle)

**Interfaces:**
- Consumes: `R.font.noto_sans_jp_regular`, `R.font.noto_sans_jp_medium`, `R.font.noto_sans_jp_bold` from Task 5.
- Produces: `private val NotoSansJp = FontFamily(Font(R.font.noto_sans_jp_regular, FontWeight.Normal), Font(R.font.noto_sans_jp_medium, FontWeight.Medium), Font(R.font.noto_sans_jp_bold, FontWeight.Bold))` declared at the top of the file. The `TastileTypography` `Typography(...)` call's every TextStyle gets `fontFamily = NotoSansJp` set explicitly. All other TextStyle fields stay unchanged from the current M3-compliant values.

- [ ] **Step 1**: Open `Type.kt`. Add imports near the top:

```kotlin
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.tastile.android.R
```

- [ ] **Step 2**: Add the `NotoSansJp` FontFamily declaration below the imports, before `TastileTypography`:

```kotlin
private val NotoSansJp = FontFamily(
    Font(R.font.noto_sans_jp_regular, FontWeight.Normal),
    Font(R.font.noto_sans_jp_medium, FontWeight.Medium),
    Font(R.font.noto_sans_jp_bold, FontWeight.Bold),
)
```

- [ ] **Step 3**: Modify every `TextStyle(...)` inside the `TastileTypography = Typography(...)` block to include `fontFamily = NotoSansJp`. There are 15 TextStyle entries (displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall, titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall). Insert `fontFamily = NotoSansJp,` after each `fontWeight = FontWeight.<X>,` line. Keep all other TextStyle fields unchanged.

- [ ] **Step 4**: Run `./gradlew :app:compileDebugKotlin` — expected BUILD SUCCESSFUL.

- [ ] **Step 5**: Commit:

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/theme/Type.kt
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "feat(theme): bind TastileTypography to Noto Sans JP font family"
```

### Task 7: Add `TastileTypographyTest`

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/theme/TastileTypographyTest.kt`

**Interfaces:**
- Consumes: `TastileTypography` and the private `NotoSansJp` FontFamily (the test reads via reflection on `TastileTypography.<key>.fontFamily` — both regular Compose tests and reflection are fine; the test class is in the same module so private is package-visible).
- Produces: 2 unit tests verifying every TextStyle has a non-null `fontFamily`.

- [ ] **Step 1**: Write the test file:

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertNotNull
import org.junit.Test

class TastileTypographyTest {

    @Test fun `every display and headline TextStyle has a font family`() {
        assertNotNull(TastileTypography.displayLarge.fontFamily)
        assertNotNull(TastileTypography.displayMedium.fontFamily)
        assertNotNull(TastileTypography.displaySmall.fontFamily)
        assertNotNull(TastileTypography.headlineLarge.fontFamily)
        assertNotNull(TastileTypography.headlineMedium.fontFamily)
        assertNotNull(TastileTypography.headlineSmall.fontFamily)
    }

    @Test fun `every title body and label TextStyle has a font family`() {
        assertNotNull(TastileTypography.titleLarge.fontFamily)
        assertNotNull(TastileTypography.titleMedium.fontFamily)
        assertNotNull(TastileTypography.titleSmall.fontFamily)
        assertNotNull(TastileTypography.bodyLarge.fontFamily)
        assertNotNull(TastileTypography.bodyMedium.fontFamily)
        assertNotNull(TastileTypography.bodySmall.fontFamily)
        assertNotNull(TastileTypography.labelLarge.fontFamily)
        assertNotNull(TastileTypography.labelMedium.fontFamily)
        assertNotNull(TastileTypography.labelSmall.fontFamily)
    }
}
```

- [ ] **Step 2**: Run `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.TastileTypographyTest"` — expected 2 tests pass.

- [ ] **Step 3**: Commit:

```bash
git add app/src/test/java/app/tastile/android/core/designsystem/theme/TastileTypographyTest.kt
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "test(theme): assert TastileTypography binds a font family on every TextStyle"
```

### Task 8: Add `TastileThemeTest` for shape + dynamic wiring

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/theme/TastileThemeTest.kt`

**Interfaces:**
- Consumes: `TastileTheme`, `LocalTastileShapeTokens`, `TastileShapes`, `MaterialTheme.shapes`, `MaterialTheme.colorScheme`.
- Produces: 3 Compose UI tests verifying `LocalTastileShapeTokens.current == TastileShapeTokens.Default` inside `TastileTheme {}`; `MaterialTheme.shapes.medium == RoundedCornerShape(16.dp)`; `MaterialTheme.colorScheme` is not null inside `TastileTheme {}` (catches regression on dynamic wiring).

- [ ] **Step 1**: Write the test file:

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TastileThemeTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `LocalTastileShapeTokens default inside TastileTheme`() {
        composeTestRule.setContent {
            var captured: TastileShapeTokens? = null
            TastileTheme {
                captured = LocalTastileShapeTokens.current
                Text("probe", Modifier.testTag("probe"))
            }
            assertEquals(TastileShapeTokens.Default, captured)
        }
        composeTestRule.onNodeWithTag("probe").assertIsDisplayed()
    }

    @Test fun `MaterialTheme shapes medium is RoundedCornerShape 16dp inside TastileTheme`() {
        composeTestRule.setContent {
            TastileTheme {
                Text("probe", Modifier.testTag("probe"))
                assertEquals(
                    RoundedCornerShape(16.dp),
                    MaterialTheme.shapes.medium,
                )
            }
        }
        composeTestRule.onNodeWithTag("probe").assertIsDisplayed()
    }

    @Test fun `MaterialTheme colorScheme is non-null inside TastileTheme`() {
        composeTestRule.setContent {
            TastileTheme {
                Text("probe", Modifier.testTag("probe"))
                // MaterialTheme.colorScheme is non-null when TastileTheme provides one
                MaterialTheme.colorScheme.primary
            }
        }
        composeTestRule.onNodeWithTag("probe").assertIsDisplayed()
    }
}
```

- [ ] **Step 2**: Run `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.TastileThemeTest"` — expected 3 tests pass **IF** Task 9 (Robolectric manifest) has landed first. If Task 9 has not landed yet, skip the test run and continue; PR-C's Robolectric task will unblock it.

- [ ] **Step 3**: Commit:

```bash
git add app/src/test/java/app/tastile/android/core/designsystem/theme/TastileThemeTest.kt
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "test(theme): assert shape, color scheme, and LocalTastileShapeTokens inside TastileTheme"
```

---

## PR-B — Robolectric enablement (Task #18 closure)

Closes the parked Task #18 from Phase 1, unblocking all 15+ Compose UI tests including the Phase 1 design-system tests and Phase 2's `TastileThemeTest`.

### Task 9: Add Robolectric test manifest

**Files:**
- Create: `app/src/test/resources/AndroidManifest.xml`

**Interfaces:**
- Consumes: nothing.
- Produces: a Robolectric-compatible manifest declaring `<activity android:name="androidx.activity.ComponentActivity"/>` per Robolectric PR #4736.

- [ ] **Step 1**: Write `app/src/test/resources/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <activity android:name="androidx.activity.ComponentActivity"/>
    </application>
</manifest>
```

- [ ] **Step 2**: Run `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.TastileThemeTest"` — expected 3 tests pass.

- [ ] **Step 3**: Run the full unit-test target: `./gradlew :app:testDebugUnitTest` — expected the 13 Compose UI Robolectric tests from Phase 1 (`TastileStatusCircleTest`, `TastileCompactTileRowTest`, `TastileCardActionRowTest`, `TastileDashboardCardShellTest`, `TastileTileCardTest`) and any pre-existing Compose UI tests (`QuickCreatePanelsTest`, `QuickCreateColorSwatchTest`, `QuickCreateColorUtilsTest`, `ProjectsScreenTest`, `TasksScreenTest`) all pass. If any fail, investigate — the manifest is the only change here.

- [ ] **Step 4**: Commit:

```bash
git add app/src/test/resources/AndroidManifest.xml
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "test(robolectric): add app/src/test/resources/AndroidManifest.xml for ComponentActivity resolution"
```

---

## PR-C — Guard extension

Tasks 10–11 extend `verifyDesignSystemImports` to flag `MaterialTheme.colorScheme` references and hardcoded `RoundedCornerShape(N.dp)` references outside the design-system module, then add a unit test for the guard logic.

### Task 10: Extend `verifyDesignSystemImports` scan

**Files:**
- Modify: `app/build.gradle.kts` (extend the `verifyDesignSystemImports` task's scan rule)

**Interfaces:**
- Consumes: existing `verifyDesignSystemImports` task body (registered at `app/build.gradle.kts:176-204` per Phase 1 ledger).
- Produces: extended scan that fails the build if any `.kt` file under `app/src/main/java/app/tastile/android/ui/{dashboard, mobile, account}/` contains `MaterialTheme.colorScheme` without a preceding `// m2-allow:` marker (the marker text must contain "shape" or "typography" — color references are forbidden), OR if any `.kt` file outside `app/src/main/java/app/tastile/android/core/designsystem/` contains `RoundedCornerShape(` followed by `<integer-or-decimal>.dp)` (allowed: `RoundedCornerShape(LocalTastileShapeTokens.current.*)`, `RoundedCornerShape(0.dp)`, `RoundedCornerShape(<percent>)`).

- [ ] **Step 1**: Open `app/build.gradle.kts` and locate the `verifyDesignSystemImports` task body. Add two new rule checks after the existing material3 import rule:

```kotlin
// Rule 2: forbid MaterialTheme.colorScheme references in ui/{dashboard, mobile, account}/
val uiConsumerRoots = listOf(
    layout.projectDirectory.dir("src/main/java/app/tastile/android/ui/dashboard").asFile,
    layout.projectDirectory.dir("src/main/java/app/tastile/android/ui/mobile").asFile,
    layout.projectDirectory.dir("src/main/java/app/tastile/android/ui/account").asFile,
)
uiConsumerRoots.forEach { root ->
    root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
        val text = file.readText()
        val lines = text.lines()
        lines.forEachIndexed { idx, line ->
            if (line.contains("MaterialTheme.colorScheme") &&
                (idx == 0 || !lines[idx - 1].trim().startsWith("// m2-allow:"))) {
                throw GradleException(
                    "Forbidden MaterialTheme.colorScheme reference at ${file.path}:${idx + 1}. " +
                        "Use LocalTastileCardRoleTokens.current / LocalTastileStatusTokens.current instead."
                )
            }
        }
    }
}

// Rule 3: forbid hardcoded RoundedCornerShape(N.dp) outside core/designsystem/
val designSystemRoot = layout.projectDirectory.dir("src/main/java/app/tastile/android/core/designsystem").asFile
layout.projectDirectory.dir("src/main/java/app/tastile/android").asFile.walkTopDown()
    .filter { it.extension == "kt" && it.startsWith(designSystemRoot).not() }
    .forEach { file ->
        val text = file.readText()
        text.lines().forEachIndexed { idx, line ->
            val match = Regex("""RoundedCornerShape\(\s*\d+(\.\d+)?\.dp\s*\)""").find(line)
            if (match != null) {
                throw GradleException(
                    "Forbidden hardcoded RoundedCornerShape(<numeric>.dp) at ${file.path}:${idx + 1}. " +
                        "Use RoundedCornerShape(LocalTastileShapeTokens.current.<key>) instead."
                )
            }
        }
    }
```

(If the existing task uses a different idiom, match its style — the brief is the intent, the syntax adapts.)

- [ ] **Step 2**: Run `./gradlew :app:verifyDesignSystemImports` — expected BUILD SUCCESSFUL if no current file violates the new rules. If violations appear, this means the consumer migration PRs (PR-D through PR-K) have not landed yet — note the violations in the task report and proceed; the guard is correct and the consumer PRs will clear them. If you want to confirm the guard fires, temporarily reintroduce a violation in a throwaway file and run the task; revert before commit.

- [ ] **Step 3**: Run `./gradlew :app:compileDebugKotlin` — expected BUILD SUCCESSFUL.

- [ ] **Step 4**: Commit:

```bash
git add app/build.gradle.kts
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "feat(guard): extend verifyDesignSystemImports to flag colorScheme + hardcoded shapes"
```

### Task 11: Add `VerifyDesignSystemImportsGuardTest`

**Files:**
- Create: `app/src/test/java/app/tastile/android/buildlogic/VerifyDesignSystemImportsGuardTest.kt`

**Interfaces:**
- Consumes: the same scan rules implemented in `app/build.gradle.kts:verifyDesignSystemImports` (extract the rule logic into a top-level function so the test can call it; refactor the build script to call that function).
- Produces: 4 unit tests asserting the guard flags (a) `MaterialTheme.colorScheme.primary` in `ui/dashboard/`, (b) `RoundedCornerShape(12.dp)` outside design-system, and accepts (c) `RoundedCornerShape(LocalTastileShapeTokens.current.m)` anywhere, (d) `// m2-allow: typography - ...` followed by `MaterialTheme.colorScheme.primary`.

- [ ] **Step 1**: Open `app/build.gradle.kts` and refactor `verifyDesignSystemImports` so the scan logic is in a top-level `fun` inside `build.gradle.kts` (or extract into a small Kotlin file under `buildSrc/`). The function signature is:

```kotlin
fun checkDesignSystemRules(
    srcRoot: File,
    designSystemRoot: File,
    uiConsumerRoots: List<File>,
) {
    // Rule 2 + Rule 3 logic moved here
}
```

The `verifyDesignSystemImports` task calls `checkDesignSystemRules(...)`.

- [ ] **Step 2**: Write the test file (calls the same function with synthetic file content via tmp dirs):

```kotlin
package app.tastile.android.buildlogic

import org.junit.Assert.assertThrows
import org.junit.Assert.assertDoesNotThrow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VerifyDesignSystemImportsGuardTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun makeFile(parent: File, path: String, content: String): File {
        val f = File(parent, path)
        f.parentFile.mkdirs()
        f.writeText(content)
        return f
    }

    @Test fun `flags MaterialTheme colorScheme in ui/dashboard`() {
        val src = tmp.newFolder("src")
        val uiDashboard = tmp.newFolder("src/ui/dashboard")
        makeFile(uiDashboard, "Bad.kt", "val x = MaterialTheme.colorScheme.primary\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(
                srcRoot = src,
                designSystemRoot = File(src, "designsystem"),
                uiConsumerRoots = listOf(uiDashboard),
            )
        }
        assert(ex.message!!.contains("MaterialTheme.colorScheme"))
    }

    @Test fun `allows MaterialTheme colorScheme preceded by m2-allow typography marker`() {
        val uiDashboard = tmp.newFolder("src/ui/dashboard")
        makeFile(
            uiDashboard,
            "Ok.kt",
            "// m2-allow: typography - reading MaterialTheme.typography.titleMedium\n" +
                "val x = MaterialTheme.colorScheme.primary\n",
        )
        assertDoesNotThrow {
            checkDesignSystemRules(
                srcRoot = tmp.root,
                designSystemRoot = File(tmp.root, "designsystem"),
                uiConsumerRoots = listOf(uiDashboard),
            )
        }
    }

    @Test fun `flags hardcoded RoundedCornerShape numeric dp outside designsystem`() {
        val other = tmp.newFolder("src/other")
        makeFile(other, "Bad.kt", "val x = RoundedCornerShape(12.dp)\n")
        val ex = assertThrows(Throwable::class.java) {
            checkDesignSystemRules(
                srcRoot = tmp.root,
                designSystemRoot = File(tmp.root, "designsystem"),
                uiConsumerRoots = emptyList(),
            )
        }
        assert(ex.message!!.contains("RoundedCornerShape"))
    }

    @Test fun `allows RoundedCornerShape with LocalTastileShapeTokens reference anywhere`() {
        val other = tmp.newFolder("src/other")
        makeFile(other, "Ok.kt", "val x = RoundedCornerShape(LocalTastileShapeTokens.current.m)\n")
        assertDoesNotThrow {
            checkDesignSystemRules(
                srcRoot = tmp.root,
                designSystemRoot = File(tmp.root, "designsystem"),
                uiConsumerRoots = emptyList(),
            )
        }
    }
}
```

(Adjust the package and `checkDesignSystemRules` import to match the refactored build script's location. If the function lives in `buildSrc/`, the test imports it from the `buildlogic` package or wherever it's exposed.)

- [ ] **Step 3**: Run `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.buildlogic.VerifyDesignSystemImportsGuardTest"` — expected 4 tests pass.

- [ ] **Step 4**: Commit:

```bash
git add app/build.gradle.kts \
        app/src/test/java/app/tastile/android/buildlogic/VerifyDesignSystemImportsGuardTest.kt
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "test(guard): assert verifyDesignSystemImports flags forbidden color/shape patterns"
```

---

## PR-D — Consumer migration (Dashboard)

Migrates every consumer in `app/src/main/java/app/tastile/android/ui/dashboard/` from `MaterialTheme.colorScheme.<key>` and hardcoded `RoundedCornerShape(N.dp)` to token references.

### Task 12: Migrate `ui/dashboard/` to token surface

**Files:**
- Modify: every `.kt` file under `app/src/main/java/app/tastile/android/ui/dashboard/` that contains `MaterialTheme.colorScheme` or `RoundedCornerShape(<numeric>.dp)`. From the pre-flight scan: at minimum `DashboardScreens.kt`, `ManagementScreens.kt`, `MonthCalendarScreen.kt`, `TimelineScreen.kt`, and `QuickCreateSheet.kt`. The `ui/dashboard/components/` subpackage also requires an audit (run `rg "MaterialTheme\.colorScheme|RoundedCornerShape\(\d+\.dp\)" app/src/main/java/app/tastile/android/ui/dashboard/components` to enumerate).

**Interfaces:**
- Consumes: `LocalTastileCardRoleTokens` / `LocalTastileStatusTokens` / `LocalTastileShapeTokens` / `LocalTastileSpacingTokens` from PR-A. Current `MaterialTheme.colorScheme.<key>` and `RoundedCornerShape(<numeric>.dp)` references in the listed files.
- Produces: every consumer reads colors via `LocalTastileCardRoleTokens.current.<role>.container|border` or `LocalTastileStatusTokens.current.<lifecycle>.container|onContainer|icon`. Every `RoundedCornerShape(<numeric>.dp)` becomes `RoundedCornerShape(LocalTastileShapeTokens.current.<key>)`. The 3 surviving `// m2-allow:` markers in `DashboardScreens.kt` (`Icon`, `MaterialTheme`, `Text`) are reduced — `MaterialTheme` is removed (no color references remain); `Icon` and `Text` may stay as primitives.

**Migration pattern (per file):**

1. Open the file. Replace `MaterialTheme.colorScheme.primary` → `LocalTastileCardRoleTokens.current.actionable.container` (or appropriate role). Map keys as:
   - `primary` → `actionable.container` (or `actionable.border` for outlines)
   - `secondary` → `completed.container`
   - `tertiary` → `started.icon` or `actionable.container`
   - `surface` → `neutral.container`
   - `surfaceVariant` → `ready.container`
   - `onSurface` → `neutral.container` (if used as text) or `LocalContentColor.current`
   - `outline` / `outlineVariant` → `neutral.border` / `completed.border`
   - For status-specific glyphs: `LocalTastileStatusTokens.current.<lifecycle>.icon`
2. Replace `RoundedCornerShape(N.dp)`:
   - `4.dp` → `xs`
   - `8.dp` → `s`
   - `12.dp` → `m`
   - `16.dp` → `m` (or `large` if it's the outermost shape)
   - `20.dp` → `large`
   - `24.dp` → `xl`
   - `28.dp` → `xl`
3. Remove dead imports.
4. Add `// m2-allow:` markers only for primitives (`Icon`, `Text`, `MaterialTheme.typography`) that remain.

- [ ] **Step 1**: Enumerate files: `rg -l "MaterialTheme\.colorScheme|RoundedCornerShape\(\d+\.dp\)" app/src/main/java/app/tastile/android/ui/dashboard`. Append the result list to the task report.

- [ ] **Step 2**: For each file in the list, apply the migration pattern above. Use `replace_all: true` for repeated tokens but verify each replacement in context. Each file is a separate `Edit` invocation; do NOT bundle multiple files into one commit.

- [ ] **Step 3**: After all files are migrated, run `./gradlew :app:compileDebugKotlin` — expected BUILD SUCCESSFUL.

- [ ] **Step 4**: Run `./gradlew :app:verifyDesignSystemImports` — expected BUILD SUCCESSFUL (no `MaterialTheme.colorScheme` references in `ui/dashboard/`).

- [ ] **Step 5**: Run `./gradlew :app:testDebugUnitTest --tests "*dashboard*"` — expected pass.

- [ ] **Step 6**: Commit each file with a separate commit, grouped per file:

```bash
git add app/src/main/java/app/tastile/android/ui/dashboard/<file>.kt
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "refactor(dashboard): migrate <file> to TastileCardRole/Status/Shape tokens"
```

If the file count is large (4–8 files), one commit per file is fine. If only 1–2 files, group them.

---

## PR-E — Consumer migration (Mobile)

Migrates every consumer under `app/src/main/java/app/tastile/android/ui/mobile/` to the token surface.

### Task 13: Migrate `ui/mobile/tabs/` to token surface

**Files:**
- Modify: every `.kt` file under `app/src/main/java/app/tastile/android/ui/mobile/tabs/` (and the `mobile/tabs/tiles/` subpackage) that contains `MaterialTheme.colorScheme` or `RoundedCornerShape(<numeric>.dp)`. From the pre-flight scan: `ExecuteScreen.kt`, `ProjectsScreen.kt`, `SettingsScreen.kt`, `TilesScreen.kt`, `TimelineScreen.kt`, `CalendarEventControls.kt`, `CalendarFilterPanel.kt`, plus all `.kt` files under `mobile/tabs/tiles/`.

**Interfaces:**
- Consumes: same tokens as Task 12.
- Produces: identical migration pattern. The `MaterialTheme.colorScheme` references in `TilesScreen.kt` (the largest file) may include role keys that need thoughtful mapping to `TastileCardRoleTokens` / `TastileStatusTokens`. If a role key doesn't map cleanly, leave a `// TODO: token mapping` comment and continue.

- [ ] **Step 1**: Enumerate: `rg -l "MaterialTheme\.colorScheme|RoundedCornerShape\(\d+\.dp\)" app/src/main/java/app/tastile/android/ui/mobile/tabs`.

- [ ] **Step 2**: Apply the migration pattern from Task 12 to each file.

- [ ] **Step 3**: Run `./gradlew :app:compileDebugKotlin` and `./gradlew :app:verifyDesignSystemImports` — expected BUILD SUCCESSFUL.

- [ ] **Step 4**: Run `./gradlew :app:testDebugUnitTest --tests "*mobile.tabs*"` — expected pass (if any tests exist).

- [ ] **Step 5**: Commit per file (or grouped if small).

### Task 14: Migrate `ui/mobile/calendar/` to token surface

**Files:**
- Modify: every `.kt` file under `app/src/main/java/app/tastile/android/ui/mobile/calendar/`. From the pre-flight scan: `DayView.kt`, `DayViewFrame.kt`, `DayViewTile.kt`, `GridConstants.kt`, `MonthEventIndicator.kt`, `MonthView.kt`, `MonthViewFrame.kt`, `NowIndicator.kt`, `TimeUtils.kt`, `WeekView.kt`, `WeekViewFrame.kt`, `WeekViewTile.kt`.

**Interfaces:**
- Consumes: same tokens. `GridConstants.kt` may have non-Compose constants — skip if it does not import Compose at all.
- Produces: token migration. `NowIndicator.kt`, `DayViewTile.kt`, `WeekViewTile.kt` are likely the heaviest consumers.

- [ ] **Step 1**: Enumerate and migrate each file per the pattern from Task 12.

- [ ] **Step 2**: Run `./gradlew :app:compileDebugKotlin` and `./gradlew :app:verifyDesignSystemImports` — expected BUILD SUCCESSFUL.

- [ ] **Step 3**: Commit per file (or grouped).

### Task 15: Migrate `ui/mobile/sheets/` to token surface

**Files:**
- Modify: every `.kt` file under `app/src/main/java/app/tastile/android/ui/mobile/sheets/` and `mobile/sheets/quickcreate/`. From the pre-flight scan: `NotificationsSheet.kt`, `PanelSheet.kt`, `QuickCreateSheetMobile.kt`, `SearchOverlaySheet.kt`, `SectionPanelContent.kt`, `TileEditSheet.kt`, plus `mobile/sheets/quickcreate/{DetailsAffordanceButton, ProjectColorRow, QuickCreateRecurringPanel, QuickCreateTaskPanel, SubtasksSection, WorkflowBatch, DateTimeRow, SubpanelComponents, MemoSection, UnderlineTextArea, UnderlineTextField, QuickCreateBasePanel}.kt`.

- [ ] **Step 1**: Enumerate and migrate each file per the pattern from Task 12. The `quickcreate/` subpackage may use `MaterialTheme.colorScheme` for color swatches — map to `LocalTastileCardRoleTokens.current.<role>.container` or status colors.

- [ ] **Step 2**: Run `./gradlew :app:compileDebugKotlin` and `./gradlew :app:verifyDesignSystemImports` — expected BUILD SUCCESSFUL.

- [ ] **Step 3**: Commit per file (or grouped if many).

### Task 16: Migrate `ui/mobile/panels/` to token surface

**Files:**
- Modify: every `.kt` file under `app/src/main/java/app/tastile/android/ui/mobile/panels/` and its subpackages (`schedule/`, `timeline/`, `references/`, `projects/`). From the pre-flight scan: `ProjectsSectionContent.kt`, `ReferencesSectionContent.kt`, `schedule/{ScheduleViewToggle, ScheduleRowList, ProjectsCheckboxSection}.kt`, `timeline/{TimelineMetaPills, TimelineSectionContent, TimelineBlockList, RangePicker}.kt`, `references/{ReferencesLabelList}.kt`, `projects/{ProjectsList, ProjectRow, ProjectEditForm, NewProjectForm}.kt`.

- [ ] **Step 1**: Enumerate and migrate each file per the pattern from Task 12.

- [ ] **Step 2**: Run `./gradlew :app:compileDebugKotlin` and `./gradlew :app:verifyDesignSystemImports` — expected BUILD SUCCESSFUL.

- [ ] **Step 3**: Commit per file (or grouped).

### Task 17: Migrate `ui/mobile/components/` + root-level mobile files

**Files:**
- Modify: `AppEmptyState.kt`, `AppListItem.kt`, `AppPickerButton.kt`, `AppSectionHeader.kt`, `picker/ReferencePickerSheet.kt`, `SidePanelDrawerContent.kt`, `AccountDropdownMenu.kt`, `OverlayLayer.kt`, `MobileTopBar.kt`.

- [ ] **Step 1**: Enumerate: `rg -l "MaterialTheme\.colorScheme|RoundedCornerShape\(\d+\.dp\)" app/src/main/java/app/tastile/android/ui/mobile/components app/src/main/java/app/tastile/android/ui/mobile/MobileTopBar.kt app/src/main/java/app/tastile/android/ui/mobile/SidePanelDrawerContent.kt app/src/main/java/app/tastile/android/ui/mobile/AccountDropdownMenu.kt app/src/main/java/app/tastile/android/ui/mobile/OverlayLayer.kt`.

- [ ] **Step 2**: Migrate each file per the pattern.

- [ ] **Step 3**: Build, verify, commit.

---

## PR-F — Consumer migration (Account)

### Task 18: Migrate `ui/mobile/account/` to token surface

**Files:**
- Modify: `AccountSheet.kt`, `SubscriptionSheet.kt`, `TokensSheet.kt` under `app/src/main/java/app/tastile/android/ui/mobile/account/`. (`AccountViewModel.kt` is a ViewModel — likely no Compose references — skip if so.)

- [ ] **Step 1**: Enumerate and migrate per pattern.

- [ ] **Step 2**: Build, verify, commit.

### Task 19: Migrate `ui/account/` to token surface

**Files:**
- Modify: `AccountScreen.kt` under `app/src/main/java/app/tastile/android/ui/account/`. (`AccountViewModel.kt` is a VM.)

- [ ] **Step 1**: Enumerate and migrate per pattern.

- [ ] **Step 2**: Build, verify, commit.

---

## PR-G — Design-system component dp literal migration

Migrates the 5 Phase 1 design-system components to use `LocalTastileShapeTokens` and `LocalTastileSpacingTokens` instead of hardcoded `dp` literals.

### Task 20: Migrate Phase 1 components to spacing/shape tokens

**Files:**
- Modify:
  - `app/src/main/java/app/tastile/android/core/designsystem/component/TastileCompactTileRow.kt` — replace `10.dp` horizontal padding with `LocalTastileSpacingTokens.current.m`.
  - `app/src/main/java/app/tastile/android/core/designsystem/component/TastileDashboardCardShell.kt` — replace `6.dp` vertical padding with `LocalTastileSpacingTokens.current.s`.
  - `app/src/main/java/app/tastile/android/core/designsystem/component/TastileTileCard.kt` — replace `8.dp` / `10.dp` literals with `LocalTastileSpacingTokens.current.s/m`.
  - `app/src/main/java/app/tastile/android/core/designsystem/component/TastileCardActionRow.kt` — replace `8.dp` `spacedBy` with `LocalTastileSpacingTokens.current.s`.
  - `app/src/main/java/app/tastile/android/core/designsystem/component/TastileStatusCircle.kt` — add `.size(LocalTastileShapeTokens.current.large)` after the testTag modifier (Phase 1 Ruling: legacy StatusCircle was 20dp; preserve visual size).

**Interfaces:**
- Consumes: `LocalTastileShapeTokens`, `LocalTastileSpacingTokens` from PR-A.
- Produces: 5 components with token-driven spacing/size. Each file's existing tests remain unchanged unless a test asserts a hardcoded dp value (none expected — the existing tests assert behavior, not size).

- [ ] **Step 1**: Open each file in turn. Replace the `dp` literal at the location described. Verify the surrounding code still compiles.

- [ ] **Step 2**: For `TastileStatusCircle.kt`, add `import app.tastile.android.core.designsystem.theme.LocalTastileShapeTokens` if absent, then modify the `Box(modifier = taggedModifier, ...)` to read:

```kotlin
val shape = LocalTastileShapeTokens.current.large
Box(
    modifier = taggedModifier.size(shape),
    contentAlignment = Alignment.Center,
)
```

(Adjust the surrounding code to read `LocalTastileShapeTokens.current.large` before the `Box(...)` call if the `Box` call cannot accept a non-modifier argument.)

- [ ] **Step 3**: Run `./gradlew :app:compileDebugKotlin` — expected BUILD SUCCESSFUL.

- [ ] **Step 4**: Run `./gradlew :app:testDebugUnitTest --tests "*designsystem.component.*"` — expected pass.

- [ ] **Step 5**: Commit per component:

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/component/<file>.kt
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "refactor(designsystem): migrate <component> to spacing/shape tokens"
```

---

## PR-H — Audit + cleanup + final review

Closes out Phase 2 with marker audit, dead-import cleanup, missing `@RunWith` annotations, and the final whole-branch review.

### Task 21: `// m2-allow:` marker audit + dead import cleanup

**Files:**
- Modify: every `.kt` file under `ui/{dashboard, mobile, account}/` to remove surviving `// m2-allow:` markers where the underlying import is no longer needed (after consumer migration), add fresh `// m2-allow:` markers where `MaterialTheme.typography` access still requires them, and delete any dead imports.

- [ ] **Step 1**: Audit: `rg "// m2-allow:" app/src/main/java/app/tastile/android/ui/`. For each marker, check whether the import on the next line is still used. Remove the marker + import if unused. Update marker justifications to reflect new usage.

- [ ] **Step 2**: Audit dead imports: `rg "import " app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt`. Confirm `androidx.compose.material.icons.filled.Delete` and `TastileCompactTileRow` (Phase 1 minor items) are removed.

- [ ] **Step 3**: Audit missing trailing newlines: `for f in $(rg -l "" app/src/main/java/app/tastile/android/ui/ --type-add 'kt:*.kt' -tkt); do tail -c1 "$f" | od -An -c | grep -q '\\n' || echo "$f"; done`. Append a newline to each file flagged. (POSIX hygiene only.)

- [ ] **Step 4**: Add `@RunWith(RobolectricTestRunner::class)` and `import org.robolectric.RobolectricTestRunner` to the 4 Phase 1 test files missing it: `TastileCompactTileRowTest.kt`, `TastileCardActionRowTest.kt`, `TastileDashboardCardShellTest.kt`, `TastileTileCardTest`. (`TastileStatusCircleTest.kt` already has it.)

- [ ] **Step 5**: Run `./gradlew :app:verifyDesignSystemImports` — expected BUILD SUCCESSFUL.

- [ ] **Step 6**: Commit:

```bash
git add app/src/main/java/app/tastile/android/ui/ \
        app/src/test/java/app/tastile/android/core/designsystem/component/
git -c user.name="rebuildup" -c user.email="noreply@anthropic.com" commit -m "chore(dashboard): audit // m2-allow: markers + dead imports + missing test runners"
```

### Task 22: Final whole-branch review

**Files:** none modified; review-only task.

- [ ] **Step 1**: Run `./gradlew verify` from a clean state on local `main`. Expected: BUILD SUCCESSFUL after PR-A through PR-H land. If any test fails, address before requesting review.

- [ ] **Step 2**: Generate review package: dispatch a fresh review subagent (opus tier) with the full diff (`git log --oneline 8104b16..HEAD` and `git diff 8104b16..HEAD`). Provide the spec path and the plan path. The reviewer must verify:
  - Spec G1 (dynamic color): Theme.kt has `dynamicLight/DarkColorScheme(context)` branch gated by `Build.VERSION.SDK_INT >= S`.
  - Spec G2 (Noto Sans JP): Type.kt binds `NotoSansJp` FontFamily to every TextStyle.
  - Spec G3 (TastileShapeTokens): `TastileShapeTokens.Default` exists with `xs=4dp/s=8dp/m=16dp/large=20dp/xl=28dp`; `LocalTastileShapeTokens` is provided by `TastileTheme`; `MaterialTheme.shapes` reads through `TastileShapes`.
  - Spec G4 (consumer migration): `rg "MaterialTheme\.colorScheme" app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}` returns no matches; `rg "RoundedCornerShape\(\d+\.dp\)" app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}` returns no matches.
  - Spec G5 (guard): `verifyDesignSystemImports` rejects the forbidden patterns.
  - Spec G6 (marker reduction): `rg "// m2-allow:" app/src/main/java/app/tastile/android/ui/{mobile,account}` returns 0 matches; `DashboardScreens.kt` has 0 or 1 marker (only `MaterialTheme.typography` access).
  - Spec G7 (verify gate): `./gradlew verify` BUILD SUCCESSFUL.

- [ ] **Step 3**: If the reviewer returns verdict READY-TO-SHIP, mark Task 22 complete. If findings remain, address via fix rounds (max 3 fix rounds at implementer tier; rounds 4–5 escalate to a more capable model).

- [ ] **Step 4**: Update the SDD ledger `.superpowers/sdd/dashboard-visual-theme-refactor/progress.md` (the SDD controller creates this directory at execution start per the SDD skill).

---

## Self-review (post-write)

- **Spec coverage check**: Goals G1–G7 — G1 covered by Task 3, G2 by Tasks 5–7, G3 by Tasks 1–3, G4 by Tasks 12–19, G5 by Tasks 10–11, G6 by Task 21, G7 by Task 22. ✅
- **Placeholder scan**: No "TBD" / "TODO" / "implement later" — every step has either code blocks or explicit command+expected output. (One `// TODO: token mapping` comment in Task 13 is acceptable — it is a deliberate migration hint, not a placeholder.)
- **Type consistency**: `TastileShapeTokens` shape (`xs/s/m/large/xl`) is consistent between Tasks 1, 2, 3, 8, 20. `LocalTastileShapeTokens` is consistent between Tasks 1, 2, 3, 8. `MaterialTheme.colorScheme` → `LocalTastileCardRoleTokens.current.<role>.container|border` mapping is consistent between Tasks 12–19.
- **Spec correction**: The spec said `@Immutable` annotations needed to be added in Phase 2; both `TastileCardRoleTokens` and `TastileStatusTokens` already carry them from Phase 1. No Phase 2 task adds `@Immutable`. Spec text updated after plan is committed.

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-08-25-dashboard-visual-theme-refresh.md`. Two execution options:

1. **Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.
