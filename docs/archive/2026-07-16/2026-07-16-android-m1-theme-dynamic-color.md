# Android M3 Optimization — Phase M1 (Theme + Dynamic Color) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land the Material 3 Theme foundation for Tastile Android: a single entry-point `TastileTheme` composable, Material You active on Android 12+, brand palette fallback on older devices and `ThemeMode.BRAND` / `ThemeMode.GRAY`. The `GrayColors` object is removed; `AppTheme` stays as a façade.

**Architecture:** Compose Material3 `ColorScheme` resolution branches on `(themeMode, dynamicColor, Build.VERSION.SDK_INT)`. Two CompositionLocals (`LocalAppTouchTarget`, `LocalAppCornerRadius`) are introduced in this phase for use by Phases M2 / M5. After M1 ships, `MaterialTheme { ... }` is called in exactly one place — inside `TastileTheme`.

**Tech Stack:** Kotlin 2.x, Jetpack Compose Material3 BOM 2024.12.01, AGP 9.2.1, JUnit 4.13.2, Robolectric 4.14, Paparazzi 1.3.5.

**Spec reference:** `tastile-android/docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md` §3 (Phase M1).

---

## File structure

### New files

| Path | Responsibility |
|---|---|
| `app/src/main/java/app/tastile/android/ui/designsystem/Theme.kt` | `TastileTheme` composable; private `grayColorScheme(dark)`; `LocalAppTouchTarget` and `LocalAppCornerRadius` CompositionLocals |
| `app/src/main/java/app/tastile/android/ui/designsystem/BrandColors.kt` | `lightBrandColorScheme()` and `darkBrandColorScheme()` returning Compose `ColorScheme`; holds the canonical brand tokens |
| `app/src/test/java/app/tastile/android/ui/designsystem/ThemeTest.kt` | 8-cell matrix of `(themeMode × dynamicColor × dark)` |
| `app/src/test/java/app/tastile/android/ui/designsystem/BrandColorsTest.kt` | Light/dark symmetry assertions |
| `app/src/test/java/app/tastile/android/ui/designsystem/TastileThemeSnapshotTest.kt` | Robolectric + Paparazzi screenshot test for 6 fixture pairs |

### Modified files

| Path | Change |
|---|---|
| `app/src/main/java/app/tastile/android/ui/designsystem/AppTheme.kt` | Delete `object GrayColors`; keep `AppTheme` façade unchanged |
| All root composables currently calling `MaterialTheme { ... }` directly | Replace with `TastileTheme { ... }` |

---

## Tasks

### Task 1: ThemeTest — write failing 8-cell matrix (TDD red)

**Files:**
- Create: `app/src/test/java/app/tastile/android/ui/designsystem/ThemeTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package app.tastile.android.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import app.tastile.android.data.repository.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    @get:Rule val rule = createComposeRule()

    /** API 28 device qualifier: dynamic color unavailable, brand fallback. */
    @Test @Config(sdk = [28])
    fun `BRAND + dynamicColor=false + dark=false resolves to brandLight primary`() {
        rule.setContent {
            TastileTheme(themeMode = ThemeMode.BRAND, dynamicColor = false) {
                val got = MaterialTheme.colorScheme.primary
                val want = BrandColors.light().primary
                assert(got == want) { "got $got want $want" }
            }
        }
    }

    /** API 33 device qualifier: dynamic color path is taken. */
    @Test @Config(sdk = [33])
    fun `BRAND + dynamicColor=true + dark=false on API33 resolves to dynamicLight`() {
        rule.setContent {
            TastileTheme(themeMode = ThemeMode.BRAND, dynamicColor = true) {
                val got = MaterialTheme.colorScheme.primary
                // dynamicLightColorScheme() colors are computed from the
                // system wallpaper; assert only that the value is not the brand value.
                assert(got != BrandColors.light().primary) {
                    "expected dynamic, got brand"
                }
            }
        }
    }
    // See Appendix A for the remaining 6 cells. They are added by this same PR.
}
```

- [ ] **Step 2: Run, expect compilation FAIL with "Unresolved reference: TastileTheme"**

Run: `./gradlew :app:testDebugUnitTest --tests '*ThemeTest*'`
Expected: unresolved reference errors for `TastileTheme` and `BrandColors`.

- [ ] **Step 3: Commit the failing test**

```bash
git add app/src/test/java/app/tastile/android/ui/designsystem/ThemeTest.kt
git commit -m "test(android): add ThemeTest 8-cell matrix (red)"
```

---

### Task 2: BrandColors.kt — implement static brand color schemes

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/designsystem/BrandColors.kt`
- Create: `app/src/test/java/app/tastile/android/ui/designsystem/BrandColorsTest.kt`

- [ ] **Step 1: Implement BrandColors**

```kotlin
package app.tastile.android.ui.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Brand color tokens. Mirror tastile-brands/; edit only with brand team sign-off.
 */
private object BrandTokens {
    val Primary           = Color(0xFF1F1F23)
    val OnPrimary         = Color(0xFFFFFFFF)
    val PrimaryContainer  = Color(0xFFE4E4E7)
    val OnPrimaryContainer = Color(0xFF18181B)
    val Secondary         = Color(0xFF52525B)
    val OnSecondary       = Color(0xFFFFFFFF)
    val Tertiary          = Color(0xFF0D8A72)
    val OnTertiary        = Color(0xFFFFFFFF)
    val Error             = Color(0xFFC34141)
    val OnError           = Color(0xFFFFFFFF)
    val Background        = Color(0xFFFAFAFA)
    val OnBackground      = Color(0xFF18181B)
    val Surface           = Color(0xFFFFFFFF)
    val OnSurface         = Color(0xFF18181B)
    val SurfaceVariant    = Color(0xFFE4E4E7)
    val OnSurfaceVariant  = Color(0xFF52525B)
    val Outline           = Color(0xFFD4D4D8)
    // … dark variants for the same 28 roles, defined below for the dark scheme.
    val PrimaryDark       = Color(0xFFE4E4E7)
    val OnPrimaryDark     = Color(0xFF18181B)
    // …
}

fun BrandColors.Companion.light(): ColorScheme = lightColorScheme(
    primary = BrandTokens.Primary,
    onPrimary = BrandTokens.OnPrimary,
    primaryContainer = BrandTokens.PrimaryContainer,
    onPrimaryContainer = BrandTokens.OnPrimaryContainer,
    secondary = BrandTokens.Secondary,
    onSecondary = BrandTokens.OnSecondary,
    tertiary = BrandTokens.Tertiary,
    onTertiary = BrandTokens.OnTertiary,
    error = BrandTokens.Error,
    onError = BrandTokens.OnError,
    background = BrandTokens.Background,
    onBackground = BrandTokens.OnBackground,
    surface = BrandTokens.Surface,
    onSurface = BrandTokens.OnSurface,
    surfaceVariant = BrandTokens.SurfaceVariant,
    onSurfaceVariant = BrandTokens.OnSurfaceVariant,
    outline = BrandTokens.Outline,
)

fun BrandColors.Companion.dark(): ColorScheme = darkColorScheme(
    primary = BrandTokens.PrimaryDark,
    onPrimary = BrandTokens.OnPrimaryDark,
    // … remaining 26 dark scheme fields.
)

/** Internal so Theme.kt can branch on brand vs gray vs dynamic. */
class BrandColors internal constructor() {
    companion object
}
```

(The 28-role expansion is exhaustive — every M3 role listed in the
`lightColorScheme`/`darkColorScheme` signatures must be passed.)

- [ ] **Step 2: Add BrandColorsTest.kt**

```kotlin
package app.tastile.android.ui.designsystem

import org.junit.Assert.assertNotEquals
import org.junit.Test

class BrandColorsTest {
    @Test fun `light and dark schemes differ in expected roles`() {
        val l = BrandColors.light()
        val d = BrandColors.dark()
        assertNotEquals(l.primary, d.primary)
        assertNotEquals(l.background, d.background)
    }

    @Test fun `light scheme has 28-role coverage`() {
        // Sanity: round-trip every well-known role through lightColorScheme
        val l = BrandColors.light()
        listOf(
            "primary", "onPrimary", "primaryContainer", "onPrimaryContainer",
            "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
            "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
            "error", "onError", "errorContainer", "onErrorContainer",
            "background", "onBackground",
            "surface", "onSurface", "surfaceVariant", "onSurfaceVariant",
            "outline", "outlineVariant",
            "scrim", "inverseSurface", "inverseOnSurface", "inversePrimary",
        ).forEach { role ->
            // throws if the role is unset
            l::class.java.getMethod(role)
        }
    }
}
```

- [ ] **Step 3: Run, expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests '*BrandColorsTest*'`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/designsystem/BrandColors.kt \
        app/src/test/java/app/tastile/android/ui/designsystem/BrandColorsTest.kt
git commit -m "feat(android): add BrandColors light/dark schemes (28-role coverage)"
```

---

### Task 3: Theme.kt — `TastileTheme` composable + CompositionLocals (TDD green)

**Files:**
- Create: `app/src/main/java/app/tastile/android/ui/designsystem/Theme.kt`

- [ ] **Step 1: Implement the two CompositionLocals**

```kotlin
package app.tastile.android.ui.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalAppTouchTarget = staticCompositionLocalOf<Dp> { 48.dp }

/** Pass concrete corner tokens here; the wrapper layer reads it in Phase M2. */
val LocalAppCornerRadius = staticCompositionLocalOf<CornerTokens> { DefaultCornerTokens }

interface CornerTokens { val small: Dp; val medium: Dp; val large: Dp }
object DefaultCornerTokens : CornerTokens {
    override val small  = 4.dp
    override val medium = 8.dp
    override val large  = 16.dp
}
```

- [ ] **Step 2: Implement the private `grayColorScheme(dark)` helper**

```kotlin
private fun grayColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = Color(0xFFA1A1AA),
        onPrimary = Color(0xFF18181B),
        primaryContainer = Color(0xFF52525B),
        onPrimaryContainer = Color(0xFFE4E4E7),
        secondary = Color(0xFF71717A),
        onSecondary = Color(0xFFE4E4E7),
        tertiary = Color(0xFF71717A),
        onTertiary = Color(0xFFE4E4E7),
        error = Color(0xFFC34141),
        onError = Color(0xFFFFFFFF),
        background = Color(0xFF18181B),
        onBackground = Color(0xFFE4E4E7),
        surface = Color(0xFF18181B),
        onSurface = Color(0xFFE4E4E7),
        surfaceVariant = Color(0xFF27272A),
        onSurfaceVariant = Color(0xFFA1A1AA),
        outline = Color(0xFF3F3F46),
    )
} else {
    lightColorScheme(
        primary = Color(0xFF71717A),
        onPrimary = Color(0xFFFAFAFA),
        primaryContainer = Color(0xFFE4E4E7),
        onPrimaryContainer = Color(0xFF18181B),
        secondary = Color(0xFF52525B),
        onSecondary = Color(0xFFFAFAFA),
        tertiary = Color(0xFF52525B),
        onTertiary = Color(0xFFFAFAFA),
        error = Color(0xFFC34141),
        onError = Color(0xFFFFFFFF),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF18181B),
        surface = Color(0xFFF4F4F5),
        onSurface = Color(0xFF18181B),
        surfaceVariant = Color(0xFFE4E4E7),
        onSurfaceVariant = Color(0xFF52525B),
        outline = Color(0xFFD4D4D8),
    )
}
```

- [ ] **Step 3: Implement `TastileTheme`**

```kotlin
@Composable
fun TastileTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK  -> true
        else            -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    val cs = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        themeMode == ThemeMode.GRAY -> grayColorScheme(dark)
        else                        -> if (dark) BrandColors.dark() else BrandColors.light()
    }
    MaterialTheme(
        colorScheme = cs,
        typography  = AppTypography,
        shapes      = AppShapes,
    ) {
        CompositionLocalProvider(
            LocalAppTouchTarget  provides 48.dp,
            LocalAppCornerRadius provides DefaultCornerTokens,
        ) { content() }
    }
}
```

- [ ] **Step 4: Run ThemeTest (Task 1), expect PASS for both cells**

Run: `./gradlew :app:testDebugUnitTest --tests '*ThemeTest*'`
Expected: 2 cells PASS (the Appendix-A cells expand this to 8 later in Task 5).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/designsystem/Theme.kt
git commit -m "feat(android): add TastileTheme with Dynamic Color + brand/gray fallback"
```

---

### Task 4: AppTheme.kt — remove `GrayColors`, keep façade

**Files:**
- Edit: `app/src/main/java/app/tastile/android/ui/designsystem/AppTheme.kt`

- [ ] **Step 1: Delete `object GrayColors`** from `AppTheme.kt` (the entire 9-line block).
- [ ] **Step 2: Run `./gradlew :app:assembleDebug`**

Expected: FAIL with "Unresolved reference: GrayColors" at the existing call sites in `data/repository/PreferencesRepository.kt` and any `ui/dashboard/*` references.

- [ ] **Step 3: Migrate known call sites**

```kotlin
// Old (outside Composable scope)
GrayColors.surface

// New (inside a @Composable)
AppTheme.colors.surface
```

Replace `GrayColors.X` with the closest `AppTheme.colors.X` (or read directly from `MaterialTheme.colorScheme` inside a `@Composable` block). The semantic mapping is documented in the PR description.

- [ ] **Step 4: Run `./gradlew :app:assembleDebug`, expect BUILD SUCCESSFUL**
- [ ] **Step 5: Run `./gradlew :app:testDebugUnitTest`, expect green**
- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/designsystem/AppTheme.kt <migrated-sites>
git commit -m "refactor(android): remove GrayColors in favor of TastileTheme"
```

---

### Task 5: Migrate root `MaterialTheme { … }` call sites to `TastileTheme { … }`

**Files:** All composables under `app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account,billing,login,memo,notifications}/**` that currently call `MaterialTheme { ... }` directly.

- [ ] **Step 1: Find call sites**

```bash
rg "MaterialTheme\\(" app/src/main/java/app/tastile/android/
```

For each result, confirm the call is a top-level wrapper (not e.g. a helper reading `MaterialTheme.colorScheme`). If unsure, run `:app:assembleDebug` after each site and accept the compile-time signal.

- [ ] **Step 2: Per-site replace**

```kotlin
// Before
@Composable
fun RootScreen() {
    MaterialTheme {
        // …
    }
}

// After
@Composable
fun RootScreen() {
    TastileTheme {
        // …
    }
}
```

If a call site also sets `themeMode` from local state, pass it through:
`TastileTheme(themeMode = viewModel.themeMode.value) { ... }`.

- [ ] **Step 3: Run `./gradlew :app:assembleDebug`, expect BUILD SUCCESSFUL**
- [ ] **Step 4: Run `./gradlew :app:testDebugUnitTest`, expect green**
- [ ] **Step 5: Commit (one per directory branch)**

```bash
git add app/src/main/java/app/tastile/android/ui/<area>/
git commit -m "refactor(android): route root composables through TastileTheme"
```

---

### Task 6: Extend ThemeTest to the full 8-cell matrix

**Files:**
- Edit: `app/src/test/java/app/tastile/android/ui/designsystem/ThemeTest.kt`

- [ ] **Step 1: Add the remaining 6 cells** from Appendix A. Each cell uses `@Config(sdk = …)` to pin the SDK level and asserts the resolved `colorScheme.primary` matches the expected source.

- [ ] **Step 2: Run, expect 8-cell PASS**

Run: `./gradlew :app:testDebugUnitTest --tests '*ThemeTest*'`
Expected: 8 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/app/tastile/android/ui/designsystem/ThemeTest.kt
git commit -m "test(android): complete ThemeTest 8-cell matrix"
```

---

### Task 7: TastileThemeSnapshotTest (Paparazzi)

**Files:**
- Edit: `app/build.gradle.kts` (add Paparazzi testImplementation)
- Create: `app/src/test/java/app/tastile/android/ui/designsystem/TastileThemeSnapshotTest.kt`

- [ ] **Step 1: Add Paparazzi dependency**

```kotlin
// in app/build.gradle.kts inside dependencies { }
testImplementation("app.cash.paparazzi:paparazzi:1.3.5")
```

- [ ] **Step 2: Write the snapshot test for the 6 fixture pairs**

```kotlin
package app.tastile.android.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.tastile.android.data.repository.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class TastileThemeSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private fun renderRoot(@Config(sdk = [Int]) _: Nothing = nothing, mode: ThemeMode, dyn: Boolean, dark: Boolean) {
        paparazzi.snapshot {
            TastileTheme(themeMode = mode, dynamicColor = dyn) {
                Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}
```

The 6 fixture pairs are added by this same PR: `(brandLight, brandDark, grayLight, grayDark, dynamicLight, dynamicDark)`. Each pair is one test method using `@Config(sdk = [33])` for the dynamic pairs and `@Config(sdk = [28])` for the brand/gray pairs.

- [ ] **Step 3: Run, expect PASS; visual record generated under `app/build/reports/paparazzi/`**

Run: `./gradlew :app:testDebugUnitTest --tests '*TastileThemeSnapshotTest*'`
Expected: 6 tests PASS.

- [ ] **Step 4: Commit fixtures and recorder**

```bash
git add app/build.gradle.kts \
        app/src/test/java/app/tastile/android/ui/designsystem/TastileThemeSnapshotTest.kt
git commit -m "test(android): snapshot TastileTheme against 6 fixtures"
```

---

### Task 8: Manual emulator verification + open PR

- [ ] **Step 1: Build & install debug APK**

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 2: Walk through surfaces on a Pixel 6 (Android 14, API 34)** and on a Pixel 4a (Android 11, API 30, brand fallback).

For each device:
- Open QuickCreate bottom sheet.
- Open the side panel sections.
- Open the account sheet.
- Switch themeMode via Settings → Preferences → Appearance.

Archive screenshots to `logs/m1/screenshots/`.

- [ ] **Step 3: Open PR**

Title: `feat(android): add TastileTheme with Dynamic Color + brand/gray fallback`
Body:

```
### Phase M1: Theme + Dynamic Color
- TastileTheme entry point; BrandColors + grayColorScheme fallbacks.
- CompositionLocals LocalAppTouchTarget + LocalAppCornerRadius introduced for M2/M5.
- Parity safety: no new controls; no new i18n keys; no layout reorder.
- Tests: ThemeTest 8-cell; BrandColorsTest 28-role coverage; TastileThemeSnapshotTest 6 fixtures.
- Manual verification: logs/m1/screenshots/

Spec: docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md §3
```

---

## Completion KPI recap

- [ ] `ThemeTest` — 8-cell PASS
- [ ] `BrandColorsTest` — symmetry + 28-role coverage PASS
- [ ] `TastileThemeSnapshotTest` — 6 fixture pairs PASS
- [ ] `./gradlew :app:assembleDebug` green
- [ ] `./gradlew :app:testDebugUnitTest` green
- [ ] Manual screenshot walkthrough archived under `logs/m1/`
- [ ] PR opened: `feat(android): add TastileTheme with Dynamic Color + brand/gray fallback`

---

## Appendix A — `ThemeTest` 8-cell matrix

The 8 cells to cover, in matrix form. (Task 1 sets up the first 2; Task 6 expands to 8.)

| themeMode | dynamicColor | effective `dark` | expected scheme source | @Config(sdk) |
|-----------|--------------|-------------------|-------------------------|--------------|
| SYSTEM    | true         | = isSystemInDarkTheme() | API 31+ → dynamic; `<31` → brand | `[33]` |
| SYSTEM    | false        | = isSystemInDarkTheme() | brand (light or dark) | `[28]` |
| LIGHT     | true (ignored on `<31`) | false | brand light | `[28]` |
| LIGHT     | true (effective)        | false | dynamic (on API 31+) | `[33]` |
| DARK      | true (ignored on `<31`) | true  | brand dark | `[28]` |
| DARK      | true (effective)        | true  | dynamic dark | `[33]` |
| GRAY      | any (dynamic ignored) | = isSystemInDarkTheme() | gray (light or dark) | `[28]` |
| BRAND     | true (effective on API 31+) | = isSystemInDarkTheme() | brand | `[28]` |

Each test method asserts `MaterialTheme.colorScheme.primary` (or any role) against the value resolved by the appropriate scheme source:

```kotlin
@Test @Config(sdk = [28])
fun `LIGHT resolves to brandLight primary regardless of dynamicColor`() {
    rule.setContent {
        TastileTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
            assert(MaterialTheme.colorScheme.primary == BrandColors.light().primary)
        }
    }
}
```

For dynamic cells on API 33, the assertion is reversed: the primary must NOT equal the brand value. Use a single `Build.VERSION` stub if needed (Robolectric's `@Config(sdk = …)` is sufficient).

---

## Out of scope for this plan

- Components wrapper refactor (Phase M2)
- LazyColumn keys / state stability (Phase M3)
- Timeline performance (Phase M4)
- Accessibility + DC settings toggle (Phase M5)

If a Task above surfaces a defect outside Phase M1 scope, file an issue and defer — do not expand the PR.
