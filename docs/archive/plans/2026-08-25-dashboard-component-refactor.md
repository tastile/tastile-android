# Dashboard Component Refactor + Token Foundations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract repeated dashboard patterns into the design system and introduce design-token keys so Phase 2 can refresh visuals without touching call sites.

**Architecture:** Add five new component wrappers and four token families under `core/designsystem/`, then migrate `ui/dashboard/` callers PR-by-PR. Token values are placeholders that match today's behavior; Phase 2 swaps them.

**Tech Stack:** Kotlin 2.1.0, Jetpack Compose, Material 3, JUnit 4, `androidx.compose.ui.test` (`createComposeRule`), Gradle 9.2.1.

**Spec:** `docs/superpowers/specs/2026-08-25-dashboard-component-refactor-design.md`

## Global Constraints

These apply to every task. Pull them from the spec and project rules; do not restate.

- JDK 17 or 21; Android SDK with API 35.
- `./gradlew verify` and `./gradlew testDebugUnitTest` must pass before claiming PASS / DONE / GREEN / ready to ship on any task that modifies `:app` sources.
- Direct `androidx.compose.material3.*` imports in `app/src/main/java/app/tastile/android/ui/dashboard/**` require an immediately preceding `// m2-allow:` marker; otherwise `verifyDesignSystemImports` fails the build.
- All new identifiers, code comments, and commit messages are English.
- All new tests under `app/src/test/java/app/tastile/android/core/designsystem/**`.
- Token defaults must visually match today's behavior - choose `MaterialTheme.colorScheme.*` slots that already produce the same look.
- Do not commit: keystores, `local.properties`, `google-services.json`, `.env*`, generated `app/src/main/jniLibs/`.
- New components must place `Modifier` as the first optional parameter and avoid boolean shape flags (Slot APIs only).
- New components must include `@ThemePreviews` (Light + Dark variants) plus a FontScale=2.0 variant where the parent class already does so.

## File Structure

```
app/src/main/java/app/tastile/android/
  core/designsystem/
    component/
      TastileStatusCircle.kt          (NEW, PR-A)
      TastileCompactTileRow.kt        (NEW, PR-B)
      TastileCardActions.kt           (NEW, PR-C)
      TastileCardActionRow.kt         (NEW, PR-C)
      TastileDashboardCardShell.kt    (NEW, PR-D)
      TastileTileCard.kt              (NEW, PR-D)
    theme/
      TastileStatusTokens.kt          (NEW, PR-A)
      TastileCardRoleTokens.kt        (NEW, PR-A)
      TastileSurfaceElevationTokens.kt (NEW, PR-A)
      TastileSpacingTokens.kt         (NEW, PR-A)
      ThemeTokenLocals.kt             (NEW, PR-A)
      Theme.kt                        (MODIFY, PR-A)
  ui/dashboard/
    DashboardScreens.kt               (MODIFY, PR-A → PR-E)

app/src/test/java/app/tastile/android/
  core/designsystem/
    component/
      TastileStatusCircleTest.kt      (NEW, PR-A)
      TastileCompactTileRowTest.kt    (NEW, PR-B)
      TastileCardActionRowTest.kt     (NEW, PR-C)
      TastileDashboardCardShellTest.kt (NEW, PR-D)
      TastileTileCardTest.kt          (NEW, PR-D)
    theme/
      TastileStatusTokensTest.kt      (NEW, PR-A)
```

One file per concern, no merged god-components. `TastileStatusTokens` and the other token files live next to `Color.kt`, `Type.kt`, `PanelTokens.kt`.

---

## PR-A: Token Foundations + TastileStatusCircle

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileStatusTokens.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileCardRoleTokens.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileSurfaceElevationTokens.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileSpacingTokens.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/ThemeTokenLocals.kt`
- Modify: `app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileStatusCircle.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt:194-211`
- Test: `app/src/test/java/app/tastile/android/core/designsystem/theme/TastileStatusTokensTest.kt`
- Test: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileStatusCircleTest.kt`

**Interfaces (consumed by later PRs):**
- `LocalTastileStatusTokens: ProvidableCompositionLocal<TastileStatusTokens>`
- `LocalTastileCardRoleTokens: ProvidableCompositionLocal<TastileCardRoleTokens>`
- `LocalTastileSurfaceElevationTokens: ProvidableCompositionLocal<TastileSurfaceElevationTokens>`
- `LocalTastileSpacingTokens: ProvidableCompositionLocal<TastileSpacingTokens>`
- `data class TastileStatusTokens(val ready: TastileStatusColors, val started: TastileStatusColors, val done: TastileStatusColors, val archived: TastileStatusColors)`
- `data class TastileStatusColors(val container: Color, val onContainer: Color, val icon: Color)`
- `@Composable fun TastileStatusCircle(lifecycle: TileLifecycle, onClick: (() -> Unit)?, modifier: Modifier = Modifier)`

### Task 1: Define TastileStatusTokens + status data class

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileStatusTokens.kt`

- [ ] **Step 1: Write the file**

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Color slot for a single lifecycle status. Phase 1 fills slots with
 * Material 3 placeholders that match the current look. Phase 2 will swap
 * these for brand-palette entries without touching call sites.
 */
@Immutable
data class TastileStatusColors(
    val container: Color,
    val onContainer: Color,
    val icon: Color,
)

/**
 * Status tokens keyed by [app.tastile.android.data.model.TileLifecycle].
 * Defaults read from `MaterialTheme.colorScheme` so today's visuals are
 * preserved.
 */
@Immutable
data class TastileStatusTokens(
    val ready: TastileStatusColors,
    val started: TastileStatusColors,
    val done: TastileStatusColors,
    val archived: TastileStatusColors,
) {
    companion object {
        fun default(
            scheme: androidx.compose.material3.ColorScheme,
        ): TastileStatusTokens = TastileStatusTokens(
            ready = TastileStatusColors(
                container = scheme.surfaceVariant,
                onContainer = scheme.onSurfaceVariant,
                icon = scheme.primary,
            ),
            started = TastileStatusColors(
                container = scheme.tertiaryContainer,
                onContainer = scheme.onTertiaryContainer,
                icon = scheme.tertiary,
            ),
            done = TastileStatusColors(
                container = scheme.secondaryContainer,
                onContainer = scheme.onSecondaryContainer,
                icon = scheme.secondary,
            ),
            archived = TastileStatusColors(
                container = scheme.surfaceVariant,
                onContainer = scheme.onSurfaceVariant,
                icon = scheme.outline,
            ),
        )
    }
}
```

- [ ] **Step 2: Run test compile to verify it builds**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

### Task 2: Define remaining token data classes

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileCardRoleTokens.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileSurfaceElevationTokens.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileSpacingTokens.kt`

- [ ] **Step 1: Write `TastileCardRoleTokens.kt`**

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Card role tokens. Phase 1 resolves slots from `MaterialTheme.colorScheme`;
 * Phase 2 injects brand-palette entries through `TastileTheme`.
 */
@Immutable
data class TastileCardRoleColors(
    val container: Color,
    val border: Color,
)

@Immutable
data class TastileCardRoleTokens(
    val neutral: TastileCardRoleColors,
    val actionable: TastileCardRoleColors,
    val completed: TastileCardRoleColors,
) {
    companion object {
        fun default(
            scheme: androidx.compose.material3.ColorScheme,
        ): TastileCardRoleTokens = TastileCardRoleTokens(
            neutral = TastileCardRoleColors(
                container = scheme.surface,
                border = scheme.outlineVariant,
            ),
            actionable = TastileCardRoleColors(
                container = scheme.surfaceContainerLow,
                border = scheme.primary,
            ),
            completed = TastileCardRoleColors(
                container = scheme.surfaceContainerLowest,
                border = scheme.outline,
            ),
        )
    }
}
```

- [ ] **Step 2: Write `TastileSurfaceElevationTokens.kt`**

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation slots. Phase 1 mirrors current ad-hoc values used across
 * `ui/dashboard/` so visuals are preserved; Phase 2 may recalibrate.
 */
@Immutable
data class TastileSurfaceElevationTokens(
    val card: Dp,
    val sheet: Dp,
    val overlay: Dp,
) {
    companion object {
        val Default = TastileSurfaceElevationTokens(
            card = 1.dp,
            sheet = 3.dp,
            overlay = 6.dp,
        )
    }
}
```

- [ ] **Step 3: Write `TastileSpacingTokens.kt`**

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens consumed by dashboard and (later) by other screens.
 * Phase 1 adds the keys; existing literal values continue to live where
 * Phase 2 has not migrated them.
 */
@Immutable
data class TastileSpacingTokens(
    val xs: Dp,
    val s: Dp,
    val m: Dp,
    val l: Dp,
    val xl: Dp,
) {
    companion object {
        val Default = TastileSpacingTokens(
            xs = 4.dp,
            s = 8.dp,
            m = 12.dp,
            l = 16.dp,
            xl = 24.dp,
        )
    }
}
```

- [ ] **Step 4: Run the test compile to verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

### Task 3: Add CompositionLocals for tokens

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/ThemeTokenLocals.kt`

- [ ] **Step 1: Write the file**

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocals for the design-system token families. Resolved once
 * inside [TastileTheme]; consumers read via
 * `LocalTastileStatusTokens.current` etc.
 */
val LocalTastileStatusTokens = staticCompositionLocalOf<TastileStatusTokens> {
    error("TastileStatusTokens not provided. Wrap content in TastileTheme { ... }.")
}

val LocalTastileCardRoleTokens = staticCompositionLocalOf<TastileCardRoleTokens> {
    error("TastileCardRoleTokens not provided. Wrap content in TastileTheme { ... }.")
}

val LocalTastileSurfaceElevationTokens = staticCompositionLocalOf<TastileSurfaceElevationTokens> {
    error("TastileSurfaceElevationTokens not provided. Wrap content in TastileTheme { ... }.")
}

val LocalTastileSpacingTokens = staticCompositionLocalOf<TastileSpacingTokens> {
    error("TastileSpacingTokens not provided. Wrap content in TastileTheme { ... }.")
}
```

- [ ] **Step 2: Run the test compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

### Task 4: Wire token providers into TastileTheme

**Files:**
- Modify: `app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt`

- [ ] **Step 1: Update the existing `CompositionLocalProvider` block**

In `Theme.kt`, replace the entire `TastileTheme` body with:

```kotlin
@Composable
fun TastileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (darkTheme) {
            darkColorScheme()
        } else {
            lightColorScheme()
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
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TastileTypography,
            content = content,
        )
    }
}
```

- [ ] **Step 2: Run the test compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

### Task 5: Add tests for the token defaults

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/theme/TastileStatusTokensTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TastileStatusTokensTest {

    @Test fun `default light tokens resolve to Material3 lightColorScheme slots`() {
        val tokens = TastileStatusTokens.default(lightColorScheme())
        assertEquals(lightColorScheme().surfaceVariant, tokens.ready.container)
        assertEquals(lightColorScheme().onSurfaceVariant, tokens.ready.onContainer)
        assertEquals(lightColorScheme().primary, tokens.ready.icon)
    }

    @Test fun `default tokens differ between light and dark schemes`() {
        val light = TastileStatusTokens.default(lightColorScheme())
        val dark = TastileStatusTokens.default(darkColorScheme())
        assertNotEquals(light.ready.icon, dark.ready.icon)
    }

    @Test fun `all four lifecycle slots are populated`() {
        val tokens = TastileStatusTokens.default(lightColorScheme())
        // Each branch reads at least one slot; assert it is not transparent.
        listOf(
            tokens.ready.icon,
            tokens.started.icon,
            tokens.done.icon,
            tokens.archived.icon,
        ).forEach { color ->
            assertNotEquals(androidx.compose.ui.graphics.Color.Transparent, color)
        }
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.TastileStatusTokensTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

### Task 6: Implement TastileStatusCircle

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileStatusCircle.kt`

- [ ] **Step 1: Write the file**

```kotlin
/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.data.model.TileLifecycle

/**
 * Lifecycle-aware status indicator. Phase 1 renders the same Unicode glyph
 * as `StatusCircle` from `DashboardScreens.kt`. Phase 2 will swap the glyph
 * for an icon and read colors from `LocalTastileStatusTokens`.
 *
 * @param lifecycle The current lifecycle state.
 * @param onClick Optional click handler. When non-null, the indicator
 * becomes clickable.
 * @param modifier Modifier applied to the indicator.
 */
@Composable
fun TastileStatusCircle(
    lifecycle: TileLifecycle,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val glyph = when (lifecycle) {
        TileLifecycle.DONE -> "✓"
        TileLifecycle.STARTED -> "▶"
        TileLifecycle.READY -> "○"
        TileLifecycle.ARCHIVED -> "·"
    }
    val taggedModifier = modifier
        .testTag("tastile_status_circle")
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    Box(
        modifier = taggedModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@ThemePreviews
@Composable
fun TastileStatusCirclePreview() {
    app.tastile.android.core.designsystem.theme.TastileTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            TastileStatusCircle(lifecycle = TileLifecycle.READY)
            TastileStatusCircle(lifecycle = TileLifecycle.STARTED)
            TastileStatusCircle(lifecycle = TileLifecycle.DONE)
            TastileStatusCircle(lifecycle = TileLifecycle.ARCHIVED)
        }
    }
}
```

- [ ] **Step 2: Run the test compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

### Task 7: Add TastileStatusCircle test

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileStatusCircleTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.data.model.TileLifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TastileStatusCircleTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `renders the correct glyph for each lifecycle`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileStatusCircle(lifecycle = TileLifecycle.READY)
                TastileStatusCircle(lifecycle = TileLifecycle.STARTED)
                TastileStatusCircle(lifecycle = TileLifecycle.DONE)
                TastileStatusCircle(lifecycle = TileLifecycle.ARCHIVED)
            }
        }
        composeTestRule.onNodeWithText("○").assertIsDisplayed()
        composeTestRule.onNodeWithText("▶").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
        composeTestRule.onNodeWithText("·").assertIsDisplayed()
    }

    @Test fun `click handler fires when onClick is non-null`() {
        var clicked = 0
        composeTestRule.setContent {
            TastileTheme {
                TastileStatusCircle(
                    lifecycle = TileLifecycle.READY,
                    onClick = { clicked++ },
                )
            }
        }
        composeTestRule.onNodeWithTag("tastile_status_circle").performClick()
        composeTestRule.runOnIdle { assertEquals(1, clicked) }
    }

    @Test fun `no click handler leaves the indicator non-interactive`() {
        var clicked = 0
        composeTestRule.setContent {
            TastileTheme {
                TastileStatusCircle(lifecycle = TileLifecycle.DONE)
            }
        }
        composeTestRule.onNodeWithTag("tastile_status_circle").performClick()
        composeTestRule.runOnIdle { assertEquals(0, clicked) }
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.TastileStatusCircleTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

### Task 8: Replace DashboardScreens `StatusCircle` with `TastileStatusCircle`

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt:194-211`

- [ ] **Step 1: Delete the local `StatusCircle` function**

Remove the entire `@Composable private fun StatusCircle(...)` block from `DashboardScreens.kt`. Do not change its callers (`TileCompactCard`, `TileExpandableCard`) - they remain identical and now resolve to `TastileStatusCircle` once you add the import.

- [ ] **Step 2: Add the `TastileStatusCircle` import**

Add this line to the existing `app.tastile.android.core.designsystem.component.*` import block at the top of `DashboardScreens.kt`:

```kotlin
import app.tastile.android.core.designsystem.component.TastileStatusCircle
```

- [ ] **Step 3: Run the design-system import guard**

Run: `./gradlew :app:verifyDesignSystemImports`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run the verification suite**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/theme \
    app/src/main/java/app/tastile/android/core/designsystem/component/TastileStatusCircle.kt \
    app/src/test/java/app/tastile/android/core/designsystem \
    app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt
git commit -m "feat(designsystem): add token foundations and TastileStatusCircle"
```

---

## PR-B: TastileCompactTileRow

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileCompactTileRow.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt:97-116` (delete `TileCompactCard`)
- Test: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileCompactTileRowTest.kt`

**Interfaces:**
- `@Composable fun TastileCompactTileRow(title: String, lifecycle: TileLifecycle, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier, trailing: @Composable (RowScope.() -> Unit)? = null)`

### Task 9: Implement TastileCompactTileRow

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileCompactTileRow.kt`

- [ ] **Step 1: Write the file**

```kotlin
/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.TastileSpacingTokens
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens
import app.tastile.android.data.model.TileLifecycle

/**
 * Compact single-row tile representation. Mirrors the legacy `TileCompactCard`
 * from `DashboardScreens.kt`. Phase 1 uses raw `8.dp / 10.dp` values that match
 * the original layout; Phase 2 swaps them for `LocalTastileSpacingTokens`.
 */
@Composable
fun TastileCompactTileRow(
    title: String,
    lifecycle: TileLifecycle,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val spacing = LocalTastileSpacingTokens.current
    val baseModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Row(
        modifier = baseModifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        TastileStatusCircle(lifecycle = lifecycle)
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
        if (trailing != null) {
            trailing()
        }
    }
}

@ThemePreviews
@Composable
fun TastileCompactTileRowPreview() {
    app.tastile.android.core.designsystem.theme.TastileTheme {
        TastileCompactTileRow(
            title = "Sample tile",
            lifecycle = TileLifecycle.READY,
        )
    }
}
```

- [ ] **Step 2: Run the test compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

### Task 10: Add TastileCompactTileRow test

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileCompactTileRowTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.data.model.TileLifecycle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TastileCompactTileRowTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `renders title and status glyph`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileCompactTileRow(
                    title = "Read inbox",
                    lifecycle = TileLifecycle.READY,
                )
            }
        }
        composeTestRule.onNodeWithText("Read inbox").assertIsDisplayed()
        composeTestRule.onNodeWithText("○").assertIsDisplayed()
    }

    @Test fun `click handler fires when onClick provided`() {
        var clicked = 0
        composeTestRule.setContent {
            TastileTheme {
                TastileCompactTileRow(
                    title = "Read inbox",
                    lifecycle = TileLifecycle.READY,
                    onClick = { clicked++ },
                    modifier = Modifier.testTag("compact_tile_row"),
                )
            }
        }
        composeTestRule.onNodeWithTag("compact_tile_row").performClick()
        composeTestRule.runOnIdle { assertEquals(1, clicked) }
    }

    @Test fun `trailing slot renders when supplied`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileCompactTileRow(
                    title = "Read inbox",
                    lifecycle = TileLifecycle.READY,
                    trailing = { Box(Modifier.testTag("trailing_slot")) },
                )
            }
        }
        composeTestRule.onNodeWithTag("trailing_slot").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.TastileCompactTileRowTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

### Task 11: Replace `TileCompactCard` callsite

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt:97-116` (delete function, replace callers)

- [ ] **Step 1: Locate the callsites**

In `DashboardScreens.kt`, search for `TileCompactCard(` to find every callsite. There is one within `TilesDashboardScreen`'s `items(cards, key = { it.id })` block (or wherever the codebase invokes it). Record each callsite and the `Tile`, `onStart` lambda it passes.

- [ ] **Step 2: Delete `TileCompactCard` and update callers**

Remove the `@Composable private fun TileCompactCard(...)` block. For each callsite, replace with:

```kotlin
TastileCompactTileRow(
    title = tile.title,
    lifecycle = TileLifecycle.fromString(tile.lifecycle),
    onClick = if (TileLifecycle.fromString(tile.lifecycle) == TileLifecycle.READY) {
        { onStart(tile.id) }
    } else {
        null
    },
)
```

Adjust the call so `tile` is non-null: keep the existing `if (tile == null) { Text(...) }` guard, but call `TastileCompactTileRow` for the non-null branch.

- [ ] **Step 3: Add the import**

Add to the existing `app.tastile.android.core.designsystem.component.*` import block:

```kotlin
import app.tastile.android.core.designsystem.component.TastileCompactTileRow
```

- [ ] **Step 4: Run the verification suite**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/component/TastileCompactTileRow.kt \
    app/src/test/java/app/tastile/android/core/designsystem/component/TastileCompactTileRowTest.kt \
    app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt
git commit -m "refactor(designsystem): extract TastileCompactTileRow"
```

---

## PR-C: TastileCardActions + TastileCardActionRow

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileCardActions.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileCardActionRow.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt:277-317` (delete `CardPrimaryActions`, update callers)
- Test: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileCardActionRowTest.kt`

**Interfaces:**
- `sealed interface TastileCardActions { data object Ready : TastileCardActions; data object Started : TastileCardActions; data object DoneOrArchived : TastileCardActions }`
- `@Composable fun TastileCardActionRow(actions: TastileCardActions, onStart: () -> Unit, onComplete: () -> Unit, onDefer: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier)`

### Task 12: Define TastileCardActions sealed interface

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileCardActions.kt`

- [ ] **Step 1: Write the file**

```kotlin
/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.tastile.android.core.designsystem.component

/**
 * Action set exposed by a dashboard card. Maps to the three branches of the
 * legacy `CardPrimaryActions` `when (status)` block.
 */
sealed interface TastileCardActions {
    data object Ready : TastileCardActions
    data object Started : TastileCardActions
    data object DoneOrArchived : TastileCardActions
}
```

- [ ] **Step 2: Run the test compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

### Task 13: Implement TastileCardActionRow

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileCardActionRow.kt`

- [ ] **Step 1: Write the file**

```kotlin
/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens

/**
 * Action row for a dashboard card. Replaces the legacy `CardPrimaryActions`
 * `when (status)` block. Phase 1 keeps the same button set per branch as
 * the original implementation. Phase 2 may extend the action set.
 *
 * Labels are resolved through the supplied [label] composable so callers stay
 * in control of string resources.
 */
@Composable
fun TastileCardActionRow(
    actions: TastileCardActions,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onDefer: () -> Unit,
    onDelete: () -> Unit,
    startLabel: @Composable () -> Unit,
    completeLabel: @Composable () -> Unit,
    deferLabel: @Composable () -> Unit,
    deleteLabel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalTastileSpacingTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (actions) {
            TastileCardActions.Ready -> {
                NiaButton(onClick = onStart, text = startLabel, modifier = Modifier.testTag("card_action_start"))
                NiaOutlinedButton(onClick = onDelete, text = deleteLabel, modifier = Modifier.testTag("card_action_delete"))
            }
            TastileCardActions.Started -> {
                NiaButton(onClick = onComplete, text = completeLabel, modifier = Modifier.testTag("card_action_complete"))
                NiaFilledTonalButton(onClick = onDefer, text = deferLabel, modifier = Modifier.testTag("card_action_defer"))
            }
            TastileCardActions.DoneOrArchived -> {
                NiaOutlinedButton(onClick = onDelete, text = deleteLabel, modifier = Modifier.testTag("card_action_delete"))
            }
        }
    }
}

@ThemePreviews
@Composable
fun TastileCardActionRowPreview() {
    app.tastile.android.core.designsystem.theme.TastileTheme {
        TastileCardActionRow(
            actions = TastileCardActions.Ready,
            onStart = {},
            onComplete = {},
            onDefer = {},
            onDelete = {},
            startLabel = { Text("Start") },
            completeLabel = { Text("Complete") },
            deferLabel = { Text("Defer") },
            deleteLabel = { Text("Delete") },
        )
    }
}
```

- [ ] **Step 2: Run the test compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

### Task 14: Add TastileCardActionRow test

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileCardActionRowTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.tastile.android.core.designsystem.theme.TastileTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TastileCardActionRowTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `Ready branch exposes Start and Delete buttons`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileCardActionRow(
                    actions = TastileCardActions.Ready,
                    onStart = {},
                    onComplete = {},
                    onDefer = {},
                    onDelete = {},
                    startLabel = { Text("Start") },
                    completeLabel = { Text("Complete") },
                    deferLabel = { Text("Defer") },
                    deleteLabel = { Text("Delete") },
                )
            }
        }
        composeTestRule.onNodeWithTag("card_action_start").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_action_delete").assertIsDisplayed()
    }

    @Test fun `Started branch exposes Complete and Defer buttons`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileCardActionRow(
                    actions = TastileCardActions.Started,
                    onStart = {},
                    onComplete = {},
                    onDefer = {},
                    onDelete = {},
                    startLabel = { Text("Start") },
                    completeLabel = { Text("Complete") },
                    deferLabel = { Text("Defer") },
                    deleteLabel = { Text("Delete") },
                )
            }
        }
        composeTestRule.onNodeWithTag("card_action_complete").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_action_defer").assertIsDisplayed()
    }

    @Test fun `DoneOrArchived branch exposes only Delete`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileCardActionRow(
                    actions = TastileCardActions.DoneOrArchived,
                    onStart = {},
                    onComplete = {},
                    onDefer = {},
                    onDelete = {},
                    startLabel = { Text("Start") },
                    completeLabel = { Text("Complete") },
                    deferLabel = { Text("Defer") },
                    deleteLabel = { Text("Delete") },
                )
            }
        }
        composeTestRule.onNodeWithTag("card_action_delete").assertIsDisplayed()
    }

    @Test fun `clicking the Delete button on Ready fires onDelete`() {
        var deletes = 0
        composeTestRule.setContent {
            TastileTheme {
                TastileCardActionRow(
                    actions = TastileCardActions.Ready,
                    onStart = {},
                    onComplete = {},
                    onDefer = {},
                    onDelete = { deletes++ },
                    startLabel = { Text("Start") },
                    completeLabel = { Text("Complete") },
                    deferLabel = { Text("Defer") },
                    deleteLabel = { Text("Delete") },
                )
            }
        }
        composeTestRule.onNodeWithTag("card_action_delete").performClick()
        composeTestRule.runOnIdle { assertEquals(1, deletes) }
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.TastileCardActionRowTest"`
Expected: BUILD SUCCESSFUL, 4 tests passed.

### Task 15: Replace `CardPrimaryActions` callsite

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt:277-317`

- [ ] **Step 1: Locate the callsites**

In `DashboardScreens.kt`, search for `CardPrimaryActions(`. There are two callsites inside `DashboardCardRenderer` (one for `BaseCard` and one for `TimePriorityCard`). Each callsite passes `card.id`, `card.status`, and `onAction`.

- [ ] **Step 2: Delete the local `CardPrimaryActions` function**

Remove the entire `@Composable private fun CardPrimaryActions(...)` block.

- [ ] **Step 3: Update the two callsites**

Replace each callsite with:

```kotlin
TastileCardActionRow(
    actions = when (status) {
        CardStatus.READY -> TastileCardActions.Ready
        CardStatus.STARTED -> TastileCardActions.Started
        CardStatus.DONE, CardStatus.ARCHIVED -> TastileCardActions.DoneOrArchived
    },
    onStart = { onAction(CardAction.StartTile(tileId)) },
    onComplete = { onAction(CardAction.CompleteTile(tileId)) },
    onDefer = { onAction(CardAction.DeferTile(tileId)) },
    onDelete = { onAction(CardAction.DeleteTile(tileId)) },
    startLabel = { Text(stringResource(R.string.dashboard_card_start)) },
    completeLabel = { Text(stringResource(R.string.dashboard_card_complete)) },
    deferLabel = { Text(stringResource(R.string.dashboard_card_defer)) },
    deleteLabel = { Text(stringResource(R.string.dashboard_card_delete)) },
)
```

- [ ] **Step 4: Add imports**

Add to the existing import block:

```kotlin
import app.tastile.android.core.designsystem.component.TastileCardActions
import app.tastile.android.core.designsystem.component.TastileCardActionRow
```

- [ ] **Step 5: Run the verification suite**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/component/TastileCardActions.kt \
    app/src/main/java/app/tastile/android/core/designsystem/component/TastileCardActionRow.kt \
    app/src/test/java/app/tastile/android/core/designsystem/component/TastileCardActionRowTest.kt \
    app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt
git commit -m "refactor(designsystem): extract TastileCardActionRow"
```

---

## PR-D: TastileDashboardCardShell + TastileTileCard

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileDashboardCardShell.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileTileCard.kt`
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt:119-191` (delete `TileExpandableCard`), `:213-274` (replace `DashboardCardRenderer` body)
- Test: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileDashboardCardShellTest.kt`
- Test: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileTileCardTest.kt`

**Interfaces:**
- `@Composable fun TastileDashboardCardShell(modifier: Modifier = Modifier, header: @Composable RowScope.() -> Unit, content: @Composable ColumnScope.() -> Unit)`
- `@Composable fun TastileTileCard(title: String, lifecycle: TileLifecycle, expanded: Boolean, onToggleExpanded: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, expandedContent: @Composable ColumnScope.() -> Unit = {}, actions: @Composable RowScope.() -> Unit = {})`

### Task 16: Implement TastileDashboardCardShell

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileDashboardCardShell.kt`

- [ ] **Step 1: Write the file**

```kotlin
/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens

/**
 * Outlined card shell for dashboard cards. Wraps [NiaOutlinedCard] with
 * consistent outer padding and renders a header row above arbitrary content.
 */
@Composable
fun TastileDashboardCardShell(
    modifier: Modifier = Modifier,
    header: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalTastileSpacingTokens.current
    NiaOutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("dashboard_card_shell"),
    ) {
        Column {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xs),
            ) {
                header()
            }
            content()
        }
    }
}

@ThemePreviews
@Composable
fun TastileDashboardCardShellPreview() {
    app.tastile.android.core.designsystem.theme.TastileTheme {
        TastileDashboardCardShell(
            header = {
                androidx.compose.material3.Text(
                    text = "Header",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            content = {
                androidx.compose.material3.Text(
                    text = "Body content",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }
}
```

- [ ] **Step 2: Run the test compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

### Task 17: Add TastileDashboardCardShell test

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileDashboardCardShellTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import app.tastile.android.core.designsystem.theme.TastileTheme
import org.junit.Rule
import org.junit.Test

class TastileDashboardCardShellTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `renders header and content slots`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileDashboardCardShell(
                    header = {
                        Box(Modifier.testTag("shell_header")) { Text("Header") }
                    },
                    content = {
                        Box(Modifier.testTag("shell_content")) { Text("Content") }
                    },
                )
            }
        }
        composeTestRule.onNodeWithTag("shell_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shell_content").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.TastileDashboardCardShellTest"`
Expected: BUILD SUCCESSFUL, 1 test passed.

### Task 18: Implement TastileTileCard

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileTileCard.kt`

- [ ] **Step 1: Write the file**

```kotlin
/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens
import app.tastile.android.data.model.TileLifecycle

/**
 * Expandable tile card. Replaces the legacy `TileExpandableCard` from
 * `DashboardScreens.kt`. State hoisting: callers own the [expanded] flag and
 * receive toggles via [onToggleExpanded]. Phase 3 will animate the
 * expand/collapse transition.
 */
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
    expandToggleContentDescription: String? = null,
) {
    val spacing = LocalTastileSpacingTokens.current
    Column(modifier = modifier.fillMaxWidth().testTag("tastile_tile_card")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(horizontal = spacing.m, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TastileStatusCircle(lifecycle = lifecycle)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall)
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = expandToggleContentDescription,
                modifier = Modifier.testTag("tile_card_chevron"),
            )
        }
        if (expanded) {
            HorizontalDivider()
            Column(
                modifier = Modifier.fillMaxWidth().padding(spacing.m),
                verticalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                expandedContent()
                actions()
            }
        }
    }
}

@ThemePreviews
@Composable
fun TastileTileCardPreview() {
    app.tastile.android.core.designsystem.theme.TastileTheme {
        TastileTileCard(
            title = "Sample tile",
            subtitle = "Ready",
            lifecycle = TileLifecycle.READY,
            expanded = true,
            onToggleExpanded = {},
            expandedContent = {
                Text("Detail body", style = MaterialTheme.typography.bodySmall)
            },
            actions = {
                NiaButton(onClick = {}, text = { Text("Start") })
            },
        )
    }
}
```

- [ ] **Step 2: Run the test compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

### Task 19: Add TastileTileCard test

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileTileCardTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.data.model.TileLifecycle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TastileTileCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `expanded false hides expandedContent`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileTileCard(
                    title = "Sample",
                    lifecycle = TileLifecycle.READY,
                    expanded = false,
                    onToggleExpanded = {},
                    expandedContent = {
                        Box(Modifier.testTag("expanded_body")) { Text("body") }
                    },
                )
            }
        }
        composeTestRule.onNodeWithTag("expanded_body").assertDoesNotExist()
    }

    @Test fun `expanded true shows expandedContent`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileTileCard(
                    title = "Sample",
                    lifecycle = TileLifecycle.READY,
                    expanded = true,
                    onToggleExpanded = {},
                    expandedContent = {
                        Box(Modifier.testTag("expanded_body")) { Text("body") }
                    },
                )
            }
        }
        composeTestRule.onNodeWithTag("expanded_body").assertIsDisplayed()
    }

    @Test fun `clicking the header fires onToggleExpanded`() {
        var toggles = 0
        composeTestRule.setContent {
            TastileTheme {
                TastileTileCard(
                    title = "Sample",
                    lifecycle = TileLifecycle.READY,
                    expanded = false,
                    onToggleExpanded = { toggles++ },
                )
            }
        }
        composeTestRule.onNodeWithText("Sample").performClick()
        composeTestRule.runOnIdle { assertEquals(1, toggles) }
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.TastileTileCardTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

### Task 20: Replace `TileExpandableCard` and rewrite `DashboardCardRenderer` body

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt`

- [ ] **Step 1: Delete `TileExpandableCard`**

Remove the entire `@Composable private fun TileExpandableCard(...)` block (lines 119-191 in the original file). Take note of the four callback names (`onStart`, `onComplete`, `onDefer`, `onDelete`) and the lifecycle mapping (`lifecycle == TileLifecycle.READY -> onStart`, etc.) - the caller will supply these directly.

- [ ] **Step 2: Hoist expand state at the callsite**

In the caller (currently inside `items(cards, key = { it.id })` of `ExecuteDashboardScreen` / `TilesDashboardScreen`), introduce per-card expanded state:

```kotlin
items(cards, key = { it.id }) { card ->
    var expanded by remember(card.id) { mutableStateOf(false) }
    DashboardCardRenderer(
        card = card,
        expanded = expanded,
        onToggleExpanded = { expanded = !expanded },
        onAction = viewModel::handleCardAction,
    )
}
```

- [ ] **Step 3: Rewrite `DashboardCardRenderer` body**

Replace the body with a composition that uses the new design-system components. Keep the existing branch on `DashboardCardModel`:

```kotlin
@Composable
private fun DashboardCardRenderer(
    card: DashboardCardModel,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAction: (CardAction) -> Unit,
) {
    val headerActionTileId = when (card) {
        is DashboardCardModel.TimelineCard -> card.items.firstOrNull()?.tileId
        else -> card.id
    }
    val headerTitle = when (card) {
        is DashboardCardModel.TimelineCard -> stringResource(card.titleRes)
        else -> card.title
    }

    val lifecycle = when (card) {
        is DashboardCardModel.BaseCard -> TileLifecycle.fromString(card.status.name)
        is DashboardCardModel.TimePriorityCard -> TileLifecycle.fromString(card.status.name)
        is DashboardCardModel.TimelineCard -> TileLifecycle.READY
    }

    TastileDashboardCardShell(
        header = {
            NiaOutlinedButton(
                text = { Text(stringResource(R.string.dashboard_prompt_button)) },
                onClick = { headerActionTileId?.let { onAction(CardAction.TriggerPrompt(it)) } },
            )
            Icon(
                imageVector = statusIcon(card.status),
                contentDescription = stringResource(R.string.dashboard_status_cd),
            )
            Text(headerTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        },
    ) {
        when (card) {
            is DashboardCardModel.BaseCard,
            is DashboardCardModel.TimePriorityCard -> {
                TastileTileCard(
                    title = headerTitle,
                    lifecycle = lifecycle,
                    expanded = expanded,
                    onToggleExpanded = onToggleExpanded,
                    expandToggleContentDescription = stringResource(R.string.dashboard_expand_cd),
                ) {
                    TastileCardActionRow(
                        actions = when (card.status) {
                            CardStatus.READY -> TastileCardActions.Ready
                            CardStatus.STARTED -> TastileCardActions.Started
                            CardStatus.DONE, CardStatus.ARCHIVED -> TastileCardActions.DoneOrArchived
                        },
                        onStart = { onAction(CardAction.StartTile(card.id)) },
                        onComplete = { onAction(CardAction.CompleteTile(card.id)) },
                        onDefer = { onAction(CardAction.DeferTile(card.id)) },
                        onDelete = { onAction(CardAction.DeleteTile(card.id)) },
                        startLabel = { Text(stringResource(R.string.dashboard_card_start)) },
                        completeLabel = { Text(stringResource(R.string.dashboard_card_complete)) },
                        deferLabel = { Text(stringResource(R.string.dashboard_card_defer)) },
                        deleteLabel = { Text(stringResource(R.string.dashboard_card_delete)) },
                    )
                }
            }
            is DashboardCardModel.TimelineCard -> {
                card.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NiaOutlinedButton(
                            text = { Text(stringResource(R.string.dashboard_prompt_button)) },
                            onClick = { onAction(CardAction.TriggerPrompt(item.tileId)) },
                        )
                        Icon(
                            imageVector = statusIcon(item.status),
                            contentDescription = stringResource(R.string.dashboard_status_cd),
                        )
                        Text(item.timestampIso, style = MaterialTheme.typography.labelSmall)
                        Text("│", style = MaterialTheme.typography.labelSmall)
                        Text(item.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Add imports**

Add to the existing import block:

```kotlin
import app.tastile.android.core.designsystem.component.TastileDashboardCardShell
import app.tastile.android.core.designsystem.component.TastileTileCard
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
```

Some of these may already exist; only add the missing ones.

- [ ] **Step 5: Run the design-system import guard**

Run: `./gradlew :app:verifyDesignSystemImports`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run the verification suite**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/app/tastile/android/core/designsystem/component/TastileDashboardCardShell.kt \
    app/src/main/java/app/tastile/android/core/designsystem/component/TastileTileCard.kt \
    app/src/test/java/app/tastile/android/core/designsystem/component/TastileDashboardCardShellTest.kt \
    app/src/test/java/app/tastile/android/core/designsystem/component/TastileTileCardTest.kt \
    app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt
git commit -m "refactor(designsystem): extract TastileDashboardCardShell and TastileTileCard"
```

---

## PR-E: `// m2-allow:` Marker Audit

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt`

### Task 21: Audit and remove removable `// m2-allow:` markers

- [ ] **Step 1: Inventory the markers**

Run: `rg -n "// m2-allow:" app/src/main/java/app/tastile/android/ui/dashboard/`
Expected output: a list of markers in `DashboardScreens.kt`. Open the file and annotate each marker with one of:

- `REMOVE` - the import is no longer used after PR-A through PR-D.
- `KEEP - primitive` - the import is for a primitive (Icon, Text, etc.) that should be replaced by an internal helper in a follow-up.
- `KEEP - theme-bridge` - the import is for `MaterialTheme` itself and unavoidable.

- [ ] **Step 2: Remove `REMOVE` markers and their imports**

For each `REMOVE` marker, delete the `// m2-allow: ...` comment line and the immediately following `import androidx.compose.material3.<name>` line if it is unused after the new design-system wrappers replace it.

- [ ] **Step 3: Add inline justification for `KEEP` markers**

For each surviving marker, append a single-line comment justifying the keep:

```kotlin
// m2-allow: primitive - Icon currently used for menu/prompt chevrons until TastileIconography lands.
import androidx.compose.material3.Icon
```

- [ ] **Step 4: Run the design-system import guard**

Run: `./gradlew :app:verifyDesignSystemImports`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run the verification suite**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/dashboard/DashboardScreens.kt
git commit -m "chore(designsystem): audit and trim // m2-allow: markers"
```

---

## Self-Review

1. **Spec coverage:**
   - Section 5.1 `TastileCompactTileRow` -> PR-B (Task 9, 10, 11).
   - Section 5.2 `TastileStatusCircle` -> PR-A (Task 6, 7, 8).
   - Section 5.3 `TastileCardActionRow` + sealed interface -> PR-C (Task 12-15).
   - Section 5.4 `TastileDashboardCardShell` -> PR-D (Task 16, 17, 20).
   - Section 5.5 `TastileTileCard` -> PR-D (Task 18, 19, 20).
   - Section 6.1 `TastileStatusTokens` -> PR-A (Task 1, 5).
   - Section 6.2 `TastileCardRoleTokens` -> PR-A (Task 2).
   - Section 6.3 `TastileSurfaceElevationTokens` -> PR-A (Task 2).
   - Section 6.4 `TastileSpacingTokens` -> PR-A (Task 2, 3, 4).
   - Section 6 CompositionLocal wiring -> PR-A (Task 3, 4).
   - Section 7 PR-A through PR-E -> Task 1-21.
   - Section 8 testing strategy -> tests added per component.
   - Section 12 risks: state hoisting change covered in PR-D Task 20 step 2.

2. **Placeholder scan:** No "TBD" or "TODO" remain. Spacing comments in `TastileCompactTileRow` and `TastileDashboardCardShell` document that Phase 2 will swap literals for tokens; this is intentional and consistent with the spec.

3. **Type consistency:**
   - `TastileStatusTokens` keys (`ready`, `started`, `done`, `archived`) used identically in Task 1 definition and Task 6 `TastileStatusCircle` (Phase 2 will read them; Phase 1 uses raw glyph).
   - `TastileCardActions` sealed interface variants (`Ready`, `Started`, `DoneOrArchived`) referenced consistently in Tasks 13, 14, 15, 20.
   - `TastileTileCard` callback names (`title`, `lifecycle`, `expanded`, `onToggleExpanded`, `subtitle`, `expandedContent`, `actions`, `expandToggleContentDescription`, `modifier`) match between definition (Task 18), test (Task 19), and callsite (Task 20).
   - `testTag` constants (`tastile_status_circle`, `compact_tile_row`, `trailing_slot`, `card_action_start`, `card_action_complete`, `card_action_defer`, `card_action_delete`, `dashboard_card_shell`, `tastile_tile_card`, `tile_card_chevron`) match between implementation files and tests.
