# Material 3 Expressive Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate `tastile-android`'s design system to Material 3 Expressive (`material3:1.5.0-alpha27`): rewrite `NiaLoadingWheel` internals using `LoadingIndicator`, adopt `MaterialTheme.motionScheme = MotionScheme.expressive()`, introduce `TastileFabMenu` for QuickCreate entry points, and add `TastileButtonGroup` with XS-XL sizing. All work is contained in `designsystem/` plus two mobile FAB call sites. Existing public APIs and call sites are preserved.

**Architecture:** Phased migration with parallel sub-agents where file sets are disjoint. Phase 0 (sequential): dependency pin + theme injection. Phase 1 (parallel): 3 sub-agents work on `LoadingWheel`, `TastileFabMenu`, `TastileButtonGroup` independently. Phase 2 (sequential): wire `TastileFabMenu` into the two FAB sites. Phase 3: integration verification.

**Tech Stack:** Kotlin 2.1.0, AGP 9.2.1, Compose BOM 2024.12.01, `androidx.compose.material3:material3:1.5.0-alpha27`, Compose Compiler plugin 2.1.0, Hilt 2.60.1, JUnit 4.13.2, Robolectric 4.16.1, MockK 1.14.11, Compose UI Test (junit4).

**Spec:** `docs/superpowers/specs/2026-09-02-m3-expressive-design.md`

---

## Global Constraints

These constraints apply to every task. Every implementer MUST read this section first.

- **Material3 version:** `1.5.0-alpha27` (pinned in Phase 0 Task 0.1). Do not introduce any other material3 version.
- **Guard rail:** Direct `androidx.compose.material3.*` imports are forbidden in `app/src/main/java/app/tastile/android/ui/{dashboard,mobile,account}/` unless the immediately preceding non-blank line is `// m2-allow:`. `designsystem/` is exempt — Expressive APIs are absorbed in `designsystem/`, UI screens consume wrappers.
- **`// m3e-allow:` marker:** Do NOT introduce this marker (removed per spec §5.1). Use existing `// m2-allow:` only.
- **`RoundedCornerShape(<n>.dp)` hardcoding:** forbidden outside `designsystem/`. Inside `designsystem/`, only allowed for adding new `TastileShapes` constants.
- **No new modules / files outside the inventory** in spec §3.1 unless explicitly justified.
- **LoadingWheel call sites (8):** `DashboardScreens.kt:48, 75`, `ExecuteScreen.kt:235`, `TokensSheet.kt:154`, `AccountSheet.kt:172`, `ProjectsSectionContent.kt:121`, `AppComponents.kt:333, 356`. The public `NiaLoadingWheel(contentDesc, modifier, wheelSize=48.dp)` and `NiaOverlayLoadingWheel(contentDesc, modifier)` signatures MUST remain unchanged. Implementation change only.
- **LoadingWheel semantics preservation:** `contentDescription`, `progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate`, `testTag("loadingWheel")` MUST survive.
- **Coverage threshold:** 80% lines / branches / methods / instructions. Do not lower the threshold.
- **Verification gate:** `./gradlew verify` MUST be green at the end of each Phase.
- **No emojis in source / commit messages / PR titles.** English only for code identifiers and commit messages. Internal docs are Japanese (not relevant here).
- **One concern per commit** (or per logical group). Commit early and often.

---

## File Structure

| Path | Phase | Role |
|---|---|---|
| `app/build.gradle.kts` | 0 | material3 1.5.0-alpha27 pin |
| `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileShapes.kt` | 0 | Add LargeIncreased / ExtraLargeIncreased / ExtraExtraLarge |
| `app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt` | 0 | Inject `motionScheme = MotionScheme.expressive()` |
| `app/src/main/java/app/tastile/android/core/designsystem/component/LoadingWheel.kt` | 1a | Rewrite internals (signature preserved) |
| `app/src/test/java/app/tastile/android/core/designsystem/component/LoadingWheelTest.kt` | 1a | New unit/Compose UI test |
| `app/src/main/java/app/tastile/android/core/designsystem/component/TastileFabMenu.kt` | 1b | New FAB Menu wrapper |
| `app/src/main/java/app/tastile/android/core/designsystem/component/FabMenuItem.kt` | 1b | New sealed class |
| `app/src/test/java/app/tastile/android/core/designsystem/component/TastileFabMenuTest.kt` | 1b | New unit/Compose UI test |
| `app/src/main/java/app/tastile/android/core/designsystem/component/TastileButtonGroup.kt` | 1c | New ButtonGroup wrapper |
| `app/src/main/java/app/tastile/android/core/designsystem/component/ButtonGroupSize.kt` | 1c | New enum |
| `app/src/main/java/app/tastile/android/core/designsystem/component/ButtonGroupItem.kt` | 1c | New data class |
| `app/src/test/java/app/tastile/android/core/designsystem/component/TastileButtonGroupTest.kt` | 1c | New unit/Compose UI test |
| `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt` | 2 | Wire TastileFabMenu (line 205 area) |
| `app/src/main/java/app/tastile/android/ui/mobile/tabs/TilesScreen.kt` | 2 | Wire TastileFabMenu (line 124 area) |
| `app/src/test/java/app/tastile/android/ui/mobile/tabs/TimelineScreenFabTest.kt` | 2 | New Robolectric FAB integration test |
| `app/src/test/java/app/tastile/android/ui/mobile/tabs/TilesScreenFabTest.kt` | 2 | New Robolectric FAB integration test |
| `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileButtonGroupTokens.kt` | 1c | New token (height/padding/icon-size/text-style) |

**Disjoint file sets for Phase 1 parallelism:**
- **Phase 1a** touches only `LoadingWheel.kt` + `LoadingWheelTest.kt`
- **Phase 1b** touches only `TastileFabMenu.kt`, `FabMenuItem.kt`, `TastileFabMenuTest.kt`
- **Phase 1c** touches only `TastileButtonGroup.kt`, `ButtonGroupSize.kt`, `ButtonGroupItem.kt`, `TastileButtonGroupTokens.kt`, `TastileButtonGroupTest.kt`

No overlap → safe to dispatch 3 sub-agents in parallel after Phase 0 completes.

---

## Phase 0: Dependency Pin + Theme Injection (Sequential)

### Task 0.1: Pin material3 to 1.5.0-alpha27 + add ExposedDropdownMenu extension-function imports

**Files:**
- Modify: `app/build.gradle.kts` line 579
- Modify: `app/src/main/java/app/tastile/android/ui/dashboard/components/AutoCompleteTextField.kt` (add 1 import)
- Modify: `app/src/main/java/app/tastile/android/ui/memo/MemoScreen.kt` (add 1 import)
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt` (add 1 import)

**Interfaces:**
- Consumes: existing `androidx.compose.material3:material3:1.5.0-alpha24`
- Produces: `androidx.compose.material3:material3:1.5.0-alpha27` available transitively; `ExposedDropdownMenu` resolves as an extension function on `ExposedDropdownMenuBoxScope`

**Background (alpha24 → alpha27 breaking change):**
In `1.5.0-alpha26`, `ExposedDropdownMenu` was moved from a member function to an extension function on `ExposedDropdownMenuBoxScope`. Existing call sites that resolved it via implicit member access now require an explicit import:
```kotlin
import androidx.compose.material3.ExposedDropdownMenuBoxScope.ExposedDropdownMenu
```
The "No value passed for parameter 'type'" error reported by an earlier pre-flight verify was a cascading compile error downstream of this unresolved reference — Kotlin type inference fails when a referenced symbol is unresolved, surfacing secondary errors that don't reflect a real API change. Adding the import resolves both the unresolved reference AND the cascading type errors.

- [ ] **Step 1: Verify the existing dependency declaration**

Open `app/build.gradle.kts` and confirm line 579 reads:
```
implementation("androidx.compose.material3:material3:1.5.0-alpha24")
```

- [ ] **Step 2: Replace the version**

Edit `app/build.gradle.kts:579`:
```
implementation("androidx.compose.material3:material3:1.5.0-alpha27")
```

Save the file.

- [ ] **Step 3: Add the ExposedDropdownMenu extension-function import**

In each of the 3 files below, add the following import line directly after the existing `ExposedDropdownMenuBox` / `ExposedDropdownMenuDefaults` import block, **before** the `@OptIn(ExperimentalMaterial3Api::class)` annotation if present:

```kotlin
import androidx.compose.material3.ExposedDropdownMenuBoxScope.ExposedDropdownMenu
```

Files:
- `app/src/main/java/app/tastile/android/ui/dashboard/components/AutoCompleteTextField.kt` — add after the `ExposedDropdownMenuAnchorType` import (line 13)
- `app/src/main/java/app/tastile/android/ui/memo/MemoScreen.kt` — add after the existing `ExposedDropdownMenu*` imports
- `app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt` — add after the existing `ExposedDropdownMenu*` imports

If any file already has a `// m2-allow:` marker on the existing `ExposedDropdownMenu` import (none currently do — the file uses it as a member), do not add a marker to the new import; the extension import is the new normal usage pattern.

- [ ] **Step 4: Run pre-flight verify**

Run:
```
cd app && ../gradlew :app:verifyDesignSystemImports :app:testDebugUnitTest 2>&1 | tail -50
```

Expected: PASS (no compile errors, all unit tests pass). If FAIL with new errors, the most likely cause is something not yet observed in this alpha range — escalate to controller before continuing.

- [ ] **Step 5: Commit**

```
git add app/build.gradle.kts \
        app/src/main/java/app/tastile/android/ui/dashboard/components/AutoCompleteTextField.kt \
        app/src/main/java/app/tastile/android/ui/memo/MemoScreen.kt \
        app/src/main/java/app/tastile/android/ui/mobile/tabs/SettingsScreen.kt
git commit -m "build(gradle): pin androidx.compose.material3 to 1.5.0-alpha27

In material3:1.5.0-alpha26, ExposedDropdownMenu moved from a member
function to an extension function on ExposedDropdownMenuBoxScope.
Add the new import to the 3 files that call it; alpha27's other
breaking changes (LocalMotionScheme removal, TimePicker renames) do
not affect this codebase.

Pre-flight verify: :app:verifyDesignSystemImports and
:app:testDebugUnitTest both PASS.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 0.2: Add LargeIncreased / ExtraLargeIncreased / ExtraExtraLarge shape tokens

**Files:**
- Modify: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileShapes.kt`
- Create: `app/src/test/java/app/tastile/android/core/designsystem/theme/TastileShapesTest.kt`

**Interfaces:**
- Consumes: existing `TastileShapes` object (with `ExtraSmall` / `Small` / `Medium` / `Large` / `ExtraLarge` constants)
- Produces: three new shape constants `LargeIncreased`, `ExtraLargeIncreased`, `ExtraExtraLarge` on the same `TastileShapes` object

- [ ] **Step 1: Read TastileShapes.kt to understand its current shape**

Open `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileShapes.kt`. The file currently exposes a `TastileShapes` object (or top-level vals) wrapping the M3 `Shapes` builder. Note the naming convention used (`ExtraSmall`, `ExtraLarge`, etc.) and any companion-object pattern.

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/app/tastile/android/core/designsystem/theme/TastileShapesTest.kt`:

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TastileShapesTest {

    @Test
    fun `LargeIncreased matches M3 expressive 20dp spec`() {
        val expected = RoundedCornerShape(20.dp)
        assertEquals(expected, TastileShapes.LargeIncreased)
    }

    @Test
    fun `ExtraLargeIncreased matches M3 expressive 32dp spec`() {
        val expected = RoundedCornerShape(32.dp)
        assertEquals(expected, TastileShapes.ExtraLargeIncreased)
    }

    @Test
    fun `ExtraExtraLarge matches M3 expressive 48dp spec`() {
        val expected = RoundedCornerShape(48.dp)
        assertEquals(expected, TastileShapes.ExtraExtraLarge)
    }
}
```

- [ ] **Step 3: Run the test to confirm it fails**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.TastileShapesTest"
```

Expected: FAIL with `Unresolved reference: TastileShapes.LargeIncreased` (or similar) — the constants do not exist yet.

- [ ] **Step 4: Add the three shape constants**

In `TastileShapes.kt`, locate the file-scoped or companion-object block where existing shape constants live. Add the three new entries following the same pattern. Example (adjust to actual style in your file):

```kotlin
val LargeIncreased: CornerBasedShape = RoundedCornerShape(20.dp)
val ExtraLargeIncreased: CornerBasedShape = RoundedCornerShape(32.dp)
val ExtraExtraLarge: CornerBasedShape = RoundedCornerShape(48.dp)
```

Use the same visibility / package-level / object-pattern style that `ExtraSmall` and `ExtraLarge` already use in this file. Do NOT introduce a new wrapper class.

- [ ] **Step 5: Run the test to confirm it passes**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.TastileShapesTest"
```

Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```
git add app/src/main/java/app/tastile/android/core/designsystem/theme/TastileShapes.kt \
        app/src/test/java/app/tastile/android/core/designsystem/theme/TastileShapesTest.kt
git commit -m "feat(designsystem): add LargeIncreased / ExtraLargeIncreased / ExtraExtraLarge shape tokens

Adds three M3 Expressive corner-radius tokens (20dp / 32dp / 48dp) to
TastileShapes, matching the M3 spec values for large-increased,
extra-large-increased, and extra-extra-large shape slots.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 0.3: Inject MotionScheme.expressive() into MaterialTheme

**Files:**
- Modify: `app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt`

**Interfaces:**
- Consumes: existing `TastileTheme` composable (wraps `MaterialTheme(colorScheme = ..., shapes = ..., typography = ...)`)
- Produces: `TastileTheme` that also passes `motionScheme = MotionScheme.expressive()`. M3 components read it via `MaterialTheme.motionScheme`.

- [ ] **Step 1: Read Theme.kt to understand current TastileTheme signature**

Open `app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt`. Note:
- The function name (`TastileTheme`).
- Its current `MaterialTheme(...)` call site and parameters.
- Existing imports.

- [ ] **Step 2: Add file-level OptIn**

At the top of `Theme.kt`, after the `package` declaration, add:

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
```

- [ ] **Step 3: Add the MotionScheme import**

Add the import alongside existing material3 imports:

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
```

- [ ] **Step 4: Pass motionScheme to MaterialTheme**

Modify the `MaterialTheme(...)` call inside `TastileTheme` to add `motionScheme`:

```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    shapes = shapes,
    typography = typography,
    motionScheme = MotionScheme.expressive(),
    content = content,
)
```

Use the exact parameter names already in use in this file (e.g. `shapes` vs `shapes = TastileShapes`). Do not introduce new local variables.

- [ ] **Step 5: Verify compile and visual smoke**

Run:
```
cd app && ../gradlew :app:compileDebugKotlin
```

Expected: PASS — `MotionScheme.expressive()` is a stable factory in 1.5.0-alpha27 and `motionScheme` parameter exists on `MaterialTheme`.

If the parameter name differs in your installed alpha (e.g. `motionScheme = ...` vs being merged into a `theme` object), refer to the official reference at `https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialTheme` for the exact parameter name and adjust accordingly.

- [ ] **Step 6: Run the full unit-test suite**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest
```

Expected: PASS — no existing test should regress because we only added a parameter to `MaterialTheme`.

- [ ] **Step 7: Commit**

```
git add app/src/main/java/app/tastile/android/core/designsystem/theme/Theme.kt
git commit -m "feat(designsystem): inject MotionScheme.expressive() into TastileTheme

Passes motionScheme = MotionScheme.expressive() to MaterialTheme so
that built-in M3 components pick up spring-physics animations
transitively. The Expressive opt-in is scoped to Theme.kt via
@file:OptIn so consumers don't need to opt in individually.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 0.4: Phase 0 verification gate

**Files:**
- (No file changes — verification only)

- [ ] **Step 1: Run verify**

Run:
```
cd app && ../gradlew :app:verify
```

Expected: PASS. This includes `verifyDesignSystemImports`, `verifyNoEmbeddedServerSecrets`, `verifyV1ApiCoverage`, `testDebugUnitTest`, and JaCoCo coverage verification.

- [ ] **Step 2: Confirm no `// m3e-allow:` references exist**

Run:
```
git grep -n "// m3e-allow:" || echo "no m3e-allow references"
```

Expected: `no m3e-allow references` (the marker was removed per spec §5.1).

- [ ] **Step 3: Mark Phase 0 complete**

Do not commit. Report Phase 0 green to the orchestrator before dispatching Phase 1 sub-agents.

---

## Phase 1: Component Implementation (3 Parallel Sub-Agents)

**Execution model:** After Phase 0 is green, dispatch three sub-agents in parallel — one each for Phase 1a, 1b, 1c. Each sub-agent works on a disjoint file set (see File Structure above) and merges to `main` independently. No coordination needed between them.

**Pre-conditions for each sub-agent:**
- Phase 0 (Tasks 0.1–0.4) is committed and `verify` is green on `main`.
- Sub-agent checks out a fresh worktree from `main` and works only on the files listed for its phase.

---

### Phase 1a — `NiaLoadingWheel` Rewrite (sub-agent A)

#### Task 1a.1: Write failing tests for rewritten LoadingWheel

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/component/LoadingWheelTest.kt`

**Interfaces:**
- Consumes: existing public API — `NiaLoadingWheel(contentDesc: String, modifier: Modifier = Modifier, wheelSize: Dp = 48.dp)` and `NiaOverlayLoadingWheel(contentDesc: String, modifier: Modifier = Modifier)`.
- Produces: passing tests that pin down a11y semantics (`contentDescription`, `progressBarRangeInfo.Indeterminate`) and `testTag("loadingWheel")`.

- [ ] **Step 1: Check existing test pattern in `designsystem/component/`**

Read `app/src/test/java/app/tastile/android/core/designsystem/component/TastileCardActionRowTest.kt` to mirror the package layout and Compose-test setup. Note any `@RunWith` / Robolectric configuration.

- [ ] **Step 2: Create the test file**

Create `app/src/test/java/app/tastile/android/core/designsystem/component/LoadingWheelTest.kt`:

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.tastile.android.core.designsystem.theme.TastileTheme
import org.junit.Rule
import org.junit.Test

class LoadingWheelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `NiaLoadingWheel exposes contentDescription`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface { NiaLoadingWheel(contentDesc = "loading-data") }
            }
        }
        composeTestRule
            .onNodeWithTag("loadingWheel")
            .assertContentDescriptionEquals("loading-data")
    }

    @Test
    fun `NiaLoadingWheel reports indeterminate progress`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface { NiaLoadingWheel(contentDesc = "loading-data") }
            }
        }
        composeTestRule
            .onNodeWithTag("loadingWheel")
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            ))
    }

    @Test
    fun `NiaOverlayLoadingWheel renders with contentDescription`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface { NiaOverlayLoadingWheel(contentDesc = "overlay-loading") }
            }
        }
        composeTestRule
            .onNodeWithTag("loadingWheel", useUnmergedTree = true)
            .assertContentDescriptionEquals("overlay-loading")
    }
}
```

- [ ] **Step 3: Run the new tests**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.LoadingWheelTest"
```

Expected: Currently, the tests should PASS because the existing `NiaLoadingWheel` already sets `contentDescription`, `progressBarRangeInfo`, and `testTag("loadingWheel")`. This step's purpose is to **pin** those contracts so the rewrite cannot regress them. If tests fail, stop — Phase 0 broke the public API, escalate.

- [ ] **Step 4: Commit the test file (no production change yet)**

```
git add app/src/test/java/app/tastile/android/core/designsystem/component/LoadingWheelTest.kt
git commit -m "test(designsystem): pin LoadingWheel a11y and testTag contracts

Captures the public semantics of NiaLoadingWheel and NiaOverlayLoadingWheel
that the upcoming LoadingIndicator-based rewrite must preserve:
  - contentDescription set from contentDesc parameter
  - progressBarRangeInfo = Indeterminate
  - testTag(\"loadingWheel\")

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

#### Task 1a.2: Rewrite NiaLoadingWheel internals using LoadingIndicator

**Files:**
- Modify: `app/src/main/java/app/tastile/android/core/designsystem/component/LoadingWheel.kt`

**Interfaces:**
- Consumes: existing public signatures `NiaLoadingWheel(contentDesc, modifier, wheelSize=48.dp)` and `NiaOverlayLoadingWheel(contentDesc, modifier)`.
- Produces: same public signatures. Internal implementation backed by `androidx.compose.material3.LoadingIndicator` (Experimental).

- [ ] **Step 1: Add file-level OptIn**

At the top of `LoadingWheel.kt`, after the `package` declaration, replace the existing `// m2-allow:` markers (if any are needed for the imports we add) and add:

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
```

Add the import (with appropriate `// m2-allow:` line if the import is `androidx.compose.material3.*` and the file is inside `designsystem/` — designsystem is guard-exempt, so no marker is needed):

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
```

- [ ] **Step 2: Rewrite `NiaLoadingWheel` body**

Replace the body of `NiaLoadingWheel` with:

```kotlin
@Composable
fun NiaLoadingWheel(
    contentDesc: String,
    modifier: Modifier = Modifier,
    wheelSize: androidx.compose.ui.unit.Dp = 48.dp,
) {
    LoadingIndicator(
        modifier = modifier
            .size(wheelSize)
            .semantics(mergeDescendants = true) {
                contentDescription = contentDesc
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .testTag("loadingWheel"),
        color = MaterialTheme.colorScheme.onBackground,
    )
}
```

Keep the same parameter names, types, defaults, and `@Composable` annotations.

- [ ] **Step 3: Keep `NiaOverlayLoadingWheel` intact**

Do not change `NiaOverlayLoadingWheel`. It still wraps `NiaLoadingWheel` in a `Surface` and must keep the existing signature.

- [ ] **Step 4: Remove now-unused imports**

After the rewrite, the Canvas / Animatable / animation imports may become unused. Remove them to keep `verifyDesignSystemImports` happy and `lintDebug` clean. **Do not remove** `MaterialTheme`, `Surface`, `Dp`, `dp`, `contentDescription`, `progressBarRangeInfo`, `ProgressBarRangeInfo`, `testTag`, `Modifier`, `Composable`, `size`, `semantics` — these are still in use.

- [ ] **Step 5: Run the pinning tests from Task 1a.1**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.LoadingWheelTest"
```

Expected: PASS — the rewrite preserves the pinned semantics.

- [ ] **Step 6: Run the full unit-test suite**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest
```

Expected: PASS — 8 LoadingWheel call sites (`DashboardScreens.kt`, `ExecuteScreen.kt`, `TokensSheet.kt`, `AccountSheet.kt`, `ProjectsSectionContent.kt`, `AppComponents.kt`) continue to compile and behave correctly with the new internals.

- [ ] **Step 7: Commit**

```
git add app/src/main/java/app/tastile/android/core/designsystem/component/LoadingWheel.kt
git commit -m "refactor(designsystem): rewrite NiaLoadingWheel internals using LoadingIndicator

Replace the Canvas-based spinning-wheel implementation with the M3
Expressive LoadingIndicator. Public API (signatures, parameter names,
defaults) is preserved — the 8 call sites in DashboardScreens.kt,
ExecuteScreen.kt, TokensSheet.kt, AccountSheet.kt,
ProjectsSectionContent.kt, and AppComponents.kt remain unchanged.

Pinned semantics (verified by LoadingWheelTest):
  - contentDescription set from contentDesc
  - progressBarRangeInfo = Indeterminate
  - testTag(\"loadingWheel\")

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

#### Task 1a.3: Phase 1a verification gate

- [ ] **Step 1: Run verify**

Run:
```
cd app && ../gradlew :app:verify
```

Expected: PASS.

- [ ] **Step 2: Mark Phase 1a complete**

Do not commit. Report green to the orchestrator.

---

### Phase 1b — `TastileFabMenu` (sub-agent B)

#### Task 1b.1: Define `FabMenuItem` sealed class with test

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/FabMenuItem.kt`
- Create: `app/src/test/java/app/tastile/android/core/designsystem/component/FabMenuItemTest.kt`

**Interfaces:**
- Consumes: `androidx.compose.ui.graphics.vector.ImageVector`
- Produces: `sealed class FabMenuItem { abstract val icon: ImageVector; abstract val label: String; data class Action(...) : FabMenuItem() }`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/app/tastile/android/core/designsystem/component/FabMenuItemTest.kt`:

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FabMenuItemTest {

    @Test
    fun `Action exposes icon and label`() {
        val action = FabMenuItem.Action(
            icon = Icons.Outlined.Add,
            label = "Add event",
            onClick = {},
        )
        assertEquals(Icons.Outlined.Add, action.icon)
        assertEquals("Add event", action.label)
    }

    @Test
    fun `Action invokes onClick when triggered`() {
        var captured = 0
        val action = FabMenuItem.Action(
            icon = Icons.Outlined.Add,
            label = "tap",
            onClick = { captured += 1 },
        )
        action.onClick()
        assertEquals(1, captured)
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.FabMenuItemTest"
```

Expected: FAIL — `FabMenuItem` doesn't exist yet.

- [ ] **Step 3: Create the sealed class**

Create `app/src/main/java/app/tastile/android/core/designsystem/component/FabMenuItem.kt`:

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.ui.graphics.vector.ImageVector

sealed class FabMenuItem {
    abstract val icon: ImageVector
    abstract val label: String

    data class Action(
        override val icon: ImageVector,
        override val label: String,
        val onClick: () -> Unit,
    ) : FabMenuItem()
}
```

- [ ] **Step 4: Run the test to confirm it passes**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.FabMenuItemTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/app/tastile/android/core/designsystem/component/FabMenuItem.kt \
        app/src/test/java/app/tastile/android/core/designsystem/component/FabMenuItemTest.kt
git commit -m "feat(designsystem): add FabMenuItem sealed class

Defines the menu item contract consumed by TastileFabMenu. Currently
exposes a single Action variant carrying an icon, label, and onClick.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

#### Task 1b.2: Write failing tests for TastileFabMenu

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileFabMenuTest.kt`

**Interfaces:**
- Consumes: `FabMenuItem.Action` from Task 1b.1, `androidx.compose.material3.ExperimentalMaterial3Api` (for `BackHandler`), `BackHandler` from `androidx.activity.compose.BackHandler`.
- Produces: tests pinning expand/collapse behavior, BackHandler dismissal, onClick propagation, and the empty-list fail-fast.

- [ ] **Step 1: Check existing designsystem test patterns**

Re-read `TastileCardActionRowTest.kt` for Compose-test conventions. Also check `MaterialTheme` invocation pattern used in other tests.

- [ ] **Step 2: Create the failing test file**

Create `app/src/test/java/app/tastile/android/core/designsystem/component/TastileFabMenuTest.kt`:

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.tastile.android.core.designsystem.theme.TastileTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

class TastileFabMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `expanded false shows only main fab`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TastileFabMenu(
                        mainIcon = Icons.Outlined.Add,
                        mainLabel = "Add",
                        expanded = false,
                        onExpandedChange = {},
                        items = listOf(
                            FabMenuItem.Action(Icons.Outlined.Add, "Event") {},
                        ),
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fab-menu-item-0").assertDoesNotExist()
    }

    @Test
    fun `expanded true shows each item`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TastileFabMenu(
                        mainIcon = Icons.Outlined.Add,
                        mainLabel = "Add",
                        expanded = true,
                        onExpandedChange = {},
                        items = listOf(
                            FabMenuItem.Action(Icons.Outlined.Add, "Event") {},
                            FabMenuItem.Action(Icons.Outlined.Add, "Task") {},
                        ),
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("fab-menu-item-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fab-menu-item-1").assertIsDisplayed()
    }

    @Test
    fun `clicking item invokes its onClick`() {
        var invoked = 0
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TastileFabMenu(
                        mainIcon = Icons.Outlined.Add,
                        mainLabel = "Add",
                        expanded = true,
                        onExpandedChange = {},
                        items = listOf(
                            FabMenuItem.Action(Icons.Outlined.Add, "Event") { invoked += 1 },
                        ),
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("fab-menu-item-0").performClick()
        assertEquals(1, invoked)
    }

    @Test
    fun `clicking main fab invokes onExpandedChange true`() {
        var lastExpanded: Boolean? = null
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TastileFabMenu(
                        mainIcon = Icons.Outlined.Add,
                        mainLabel = "Add",
                        expanded = false,
                        onExpandedChange = { lastExpanded = it },
                        items = listOf(
                            FabMenuItem.Action(Icons.Outlined.Add, "Event") {},
                        ),
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        assertEquals(true, lastExpanded)
    }

    @Test
    fun `empty items throws IllegalArgumentException`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    try {
                        TastileFabMenu(
                            mainIcon = Icons.Outlined.Add,
                            mainLabel = "Add",
                            expanded = false,
                            onExpandedChange = {},
                            items = emptyList(),
                        )
                    } catch (e: IllegalArgumentException) {
                        // surface a marker node so the test can assert below
                        Box(modifier = Modifier.testTag("empty-throws"))
                    }
                }
            }
        }
        composeTestRule.onNodeWithTag("empty-throws").assertIsDisplayed()
    }
}
```

> **Implementation note for the executor:** The exact exception-class assertion pattern above is a placeholder for "the empty list case must fail-fast." The real implementation may choose to fail in `TastileFabMenu`'s body via `require(items.isNotEmpty()) { ... }`. The test's job is to pin the contract — feel free to replace the "empty-throws" tag with a more idiomatic assertion if your codebase uses a pattern like Robolectric's `@Test(expected = ...)`.

- [ ] **Step 3: Run the test to confirm it fails (compile-time)**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.TastileFabMenuTest"
```

Expected: FAIL — `TastileFabMenu` is unresolved.

- [ ] **Step 4: Commit the failing test (TDD red)**

```
git add app/src/test/java/app/tastile/android/core/designsystem/component/TastileFabMenuTest.kt
git commit -m "test(designsystem): pin TastileFabMenu behavior contracts

Captures the expected behavior of the upcoming TastileFabMenu:
  - main fab visible at all times with contentDescription = mainLabel
  - menu items render with testTag(\"fab-menu-item-N\") when expanded
  - item click invokes the item's onClick
  - main fab click invokes onExpandedChange(true)
  - empty items list is rejected (fail-fast)

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

#### Task 1b.3: Implement TastileFabMenu

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileFabMenu.kt`

**Interfaces:**
- Consumes: `FabMenuItem.Action` (from Task 1b.1), `MaterialTheme.motionScheme.defaultSpatialSpec` (set up in Task 0.3), `androidx.activity.compose.BackHandler`.
- Produces: `@Composable fun TastileFabMenu(mainIcon, mainLabel, expanded, onExpandedChange, items, modifier)`.

- [ ] **Step 1: Add file-level OptIn**

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
```

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
```

- [ ] **Step 2: Implement the composable**

Create `app/src/main/java/app/tastile/android/core/designsystem/component/TastileFabMenu.kt`:

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package app.tastile.android.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

private const val MAX_MENU_ITEMS = 6

@Composable
fun TastileFabMenu(
    mainIcon: ImageVector,
    mainLabel: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<FabMenuItem>,
    modifier: Modifier = Modifier,
) {
    require(items.isNotEmpty()) { "TastileFabMenu requires at least one item" }
    val clippedItems = items.take(MAX_MENU_ITEMS)
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "fab-rotation",
    )
    val stateDesc = if (expanded) "expanded" else "collapsed"

    BackHandler(enabled = expanded) { onExpandedChange(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                clippedItems.forEachIndexed { index, item ->
                    ExtendedFloatingActionButton(
                        onClick = { item.onClick() },
                        modifier = Modifier
                            .testTag("fab-menu-item-$index")
                            .semantics { role = Role.Button },
                    ) {
                        Icon(item.icon, contentDescription = null)
                        Text(item.label)
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.semantics {
                role = Role.Button
                stateDescription = stateDesc
            },
        ) {
            Icon(
                imageVector = mainIcon,
                contentDescription = mainLabel,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
    }
}
```

The above is a starter shape that satisfies the pinned tests in 1b.2. After running the tests, you may need to:
- Adjust the exact `testTag` placement so `onNodeWithTag("fab-menu-item-0")` resolves.
- Adjust the `contentDescription` of the main FAB so `onNodeWithContentDescription("Add")` resolves. (If you keep `Icon`'s `contentDescription = mainLabel` and `IconButton` passes it through, this is fine.)

If the implementation needs additional internal helper composables (e.g., a private `TastileFabMenuItem`), put them in the same file as `private` functions.

- [ ] **Step 3: Run the pinning tests**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.TastileFabMenuTest"
```

Expected: PASS. If any test fails because of an interaction detail (e.g., test tag scope), fix the implementation to match the pinned contract — **do not change the tests**.

- [ ] **Step 4: Run the full unit-test suite**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/app/tastile/android/core/designsystem/component/TastileFabMenu.kt
git commit -m "feat(designsystem): add TastileFabMenu wrapper around FAB Menu

Provides a TastileFabMenu composable that:
  - Renders a main FAB (always visible) with a 45° rotation when expanded
  - Reveals up to 6 ExtendedFloatingActionButton menu items when expanded
  - Animates with MaterialTheme.motionScheme.fastSpatialSpec
  - Registers a BackHandler that dismisses the menu on system back
  - Sets a11y stateDescription = \"expanded\" / \"collapsed\" on the main FAB
  - Test-tags each item as \"fab-menu-item-N\" for instrumentation
  - Fail-fasts on an empty items list

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

#### Task 1b.4: Phase 1b verification gate

- [ ] **Step 1: Run verify**

Run:
```
cd app && ../gradlew :app:verify
```

Expected: PASS.

- [ ] **Step 2: Mark Phase 1b complete**

Do not commit. Report green to the orchestrator.

---

### Phase 1c — `TastileButtonGroup` (sub-agent C)

#### Task 1c.1: Define `ButtonGroupSize` enum, `ButtonGroupItem` data class, and `TastileButtonGroupTokens`

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/ButtonGroupSize.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/ButtonGroupItem.kt`
- Create: `app/src/main/java/app/tastile/android/core/designsystem/theme/TastileButtonGroupTokens.kt`
- Create: `app/src/test/java/app/tastile/android/core/designsystem/component/ButtonGroupTypesTest.kt`
- Create: `app/src/test/java/app/tastile/android/core/designsystem/theme/TastileButtonGroupTokensTest.kt`

**Interfaces:**
- Consumes: `androidx.compose.ui.graphics.vector.ImageVector`, `androidx.compose.ui.unit.Dp`, `androidx.compose.ui.unit.dp`, `androidx.compose.material3.Typography` token names.
- Produces: `enum class ButtonGroupSize { Xs, S, M, L, Xl }`, `data class ButtonGroupItem(icon, label, enabled)`, and `TastileButtonGroupTokens` data class (height/padding/icon-size/text-style per size).

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/app/tastile/android/core/designsystem/component/ButtonGroupTypesTest.kt`:

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonGroupTypesTest {

    @Test
    fun `ButtonGroupItem defaults enabled to true`() {
        val item = ButtonGroupItem(icon = null, label = "Tap")
        assertEquals("Tap", item.label)
        assertTrue(item.enabled)
    }

    @Test
    fun `ButtonGroupItem can be disabled`() {
        val item = ButtonGroupItem(icon = Icons.Outlined.Add, label = "Tap", enabled = false)
        assertFalse(item.enabled)
    }

    @Test
    fun `ButtonGroupSize has five members in increasing order`() {
        assertEquals(
            listOf(ButtonGroupSize.Xs, ButtonGroupSize.S, ButtonGroupSize.M, ButtonGroupSize.L, ButtonGroupSize.Xl),
            ButtonGroupSize.values().toList(),
        )
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.ButtonGroupTypesTest"
```

Expected: FAIL.

- [ ] **Step 3: Create `ButtonGroupSize.kt`**

```kotlin
package app.tastile.android.core.designsystem.component

enum class ButtonGroupSize { Xs, S, M, L, Xl }
```

- [ ] **Step 4: Create `ButtonGroupItem.kt`**

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.ui.graphics.vector.ImageVector

data class ButtonGroupItem(
    val icon: ImageVector? = null,
    val label: String,
    val enabled: Boolean = true,
)
```

- [ ] **Step 5: Run the test to confirm it passes**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.ButtonGroupTypesTest"
```

Expected: PASS.

- [ ] **Step 6: Write the failing tokens test**

Create `app/src/test/java/app/tastile/android/core/designsystem/theme/TastileButtonGroupTokensTest.kt`:

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TastileButtonGroupTokensTest {

    private val tokens = TastileButtonGroupTokens.Default

    @Test
    fun `Xs size is 32dp height with 8dp horizontal padding`() {
        assertEquals(32.dp, tokens.height(ButtonGroupSize.Xs))
        assertEquals(8.dp, tokens.horizontalPadding(ButtonGroupSize.Xs))
    }

    @Test
    fun `M size is 48dp height with 16dp horizontal padding`() {
        assertEquals(48.dp, tokens.height(ButtonGroupSize.M))
        assertEquals(16.dp, tokens.horizontalPadding(ButtonGroupSize.M))
    }

    @Test
    fun `Xl size is 64dp height with 24dp horizontal padding`() {
        assertEquals(64.dp, tokens.height(ButtonGroupSize.Xl))
        assertEquals(24.dp, tokens.horizontalPadding(ButtonGroupSize.Xl))
    }

    @Test
    fun `icon size scales with button size`() {
        assertEquals(16.dp, tokens.iconSize(ButtonGroupSize.Xs))
        assertEquals(20.dp, tokens.iconSize(ButtonGroupSize.M))
        assertEquals(28.dp, tokens.iconSize(ButtonGroupSize.Xl))
    }
}
```

(You will need to add `import app.tastile.android.core.designsystem.component.ButtonGroupSize` at the top of this test file.)

- [ ] **Step 7: Run the tokens test to confirm it fails**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.TastileButtonGroupTokensTest"
```

Expected: FAIL.

- [ ] **Step 8: Create `TastileButtonGroupTokens.kt`**

```kotlin
package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.component.ButtonGroupSize

@Immutable
data class TastileButtonGroupTokens(
    val heights: Map<ButtonGroupSize, Dp>,
    val horizontalPaddings: Map<ButtonGroupSize, Dp>,
    val iconSizes: Map<ButtonGroupSize, Dp>,
) {
    fun height(size: ButtonGroupSize): Dp = heights.getValue(size)
    fun horizontalPadding(size: ButtonGroupSize): Dp = horizontalPaddings.getValue(size)
    fun iconSize(size: ButtonGroupSize): Dp = iconSizes.getValue(size)

    companion object {
        val Default = TastileButtonGroupTokens(
            heights = mapOf(
                ButtonGroupSize.Xs to 32.dp,
                ButtonGroupSize.S to 40.dp,
                ButtonGroupSize.M to 48.dp,
                ButtonGroupSize.L to 56.dp,
                ButtonGroupSize.Xl to 64.dp,
            ),
            horizontalPaddings = mapOf(
                ButtonGroupSize.Xs to 8.dp,
                ButtonGroupSize.S to 12.dp,
                ButtonGroupSize.M to 16.dp,
                ButtonGroupSize.L to 20.dp,
                ButtonGroupSize.Xl to 24.dp,
            ),
            iconSizes = mapOf(
                ButtonGroupSize.Xs to 16.dp,
                ButtonGroupSize.S to 18.dp,
                ButtonGroupSize.M to 20.dp,
                ButtonGroupSize.L to 24.dp,
                ButtonGroupSize.Xl to 28.dp,
            ),
        )
    }
}
```

- [ ] **Step 9: Run the tokens test to confirm it passes**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.theme.TastileButtonGroupTokensTest"
```

Expected: PASS.

- [ ] **Step 10: Commit**

```
git add app/src/main/java/app/tastile/android/core/designsystem/component/ButtonGroupSize.kt \
        app/src/main/java/app/tastile/android/core/designsystem/component/ButtonGroupItem.kt \
        app/src/main/java/app/tastile/android/core/designsystem/theme/TastileButtonGroupTokens.kt \
        app/src/test/java/app/tastile/android/core/designsystem/component/ButtonGroupTypesTest.kt \
        app/src/test/java/app/tastile/android/core/designsystem/theme/TastileButtonGroupTokensTest.kt
git commit -m "feat(designsystem): add ButtonGroup types and TastileButtonGroupTokens

Introduces the foundational types for TastileButtonGroup:
  - ButtonGroupSize enum (Xs / S / M / L / Xl)
  - ButtonGroupItem data class (icon, label, enabled=true)
  - TastileButtonGroupTokens data class with explicit height, horizontal
    padding, and icon size per size slot (32..64dp / 8..24dp / 16..28dp)

Tokens are stored as maps keyed by ButtonGroupSize so future sizes can
be added without changing the call-site API.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

#### Task 1c.2: Write failing tests for TastileButtonGroup

**Files:**
- Create: `app/src/test/java/app/tastile/android/core/designsystem/component/TastileButtonGroupTest.kt`

**Interfaces:**
- Consumes: `ButtonGroupSize`, `ButtonGroupItem` (from Task 1c.1).
- Produces: tests that pin selection behavior, range-out-of-bounds fail-fast, empty-list fail-fast, disabled item, and 48dp touch target.

- [ ] **Step 1: Create the failing test file**

```kotlin
package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.TastileTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TastileButtonGroupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `selected item exposes Role Tab and selected true`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    TastileFabMenu_caller { /* unused */ }
                }
            }
        }
        // The actual call:
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    TastileButtonGroup(
                        items = listOf(
                            ButtonGroupItem(icon = null, label = "One"),
                            ButtonGroupItem(icon = null, label = "Two"),
                        ),
                        selectedIndex = 1,
                        onItemSelected = {},
                        size = ButtonGroupSize.M,
                    )
                }
            }
        }
        composeTestRule
            .onAllNodesWithTag("button-group-item-1", useUnmergedTree = true)
            .onFirst()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test
    fun `clicking an item invokes onItemSelected with its index`() {
        var captured = -1
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    TastileButtonGroup(
                        items = listOf(
                            ButtonGroupItem(label = "One"),
                            ButtonGroupItem(label = "Two"),
                        ),
                        selectedIndex = 0,
                        onItemSelected = { captured = it },
                        size = ButtonGroupSize.M,
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("button-group-item-1").performClick()
        assertEquals(1, captured)
    }

    @Test
    fun `M size button is 48dp tall`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    TastileButtonGroup(
                        items = listOf(ButtonGroupItem(label = "X")),
                        selectedIndex = 0,
                        onItemSelected = {},
                        size = ButtonGroupSize.M,
                    )
                }
            }
        }
        composeTestRule
            .onNodeWithTag("button-group-item-0")
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun `Xs size has minimum 48dp touch target even when height is 32dp`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    TastileButtonGroup(
                        items = listOf(ButtonGroupItem(label = "X")),
                        selectedIndex = 0,
                        onItemSelected = {},
                        size = ButtonGroupSize.Xs,
                    )
                }
            }
        }
        // Implementation detail: the outer touch target should be ≥ 48dp
        // even when the visual height is 32dp. Use MinimumInteractiveComponentSize
        // to expand the hit area.
        composeTestRule
            .onNodeWithTag("button-group-item-0-touch")
            .assertHeightIsEqualTo(48.dp)
    }
}

// Helper composable to satisfy the unused reference above (delete this stub)
@androidx.compose.runtime.Composable
private fun TastileFabMenu_caller(content: @androidx.compose.runtime.Composable () -> Unit) {
    content()
}
```

The redundant `TastileFabMenu_caller` helper exists only because of the two `setContent` calls in the first test — if you refactor that test into a single setContent, remove the helper.

You will also need to add:

```kotlin
import androidx.compose.ui.semantics.SemanticsProperties
```

at the top of the file.

- [ ] **Step 2: Run the test to confirm it fails (compile-time)**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.TastileButtonGroupTest"
```

Expected: FAIL — `TastileButtonGroup` is unresolved.

- [ ] **Step 3: Commit the failing test (TDD red)**

```
git add app/src/test/java/app/tastile/android/core/designsystem/component/TastileButtonGroupTest.kt
git commit -m "test(designsystem): pin TastileButtonGroup contracts

Pins behavior of the upcoming TastileButtonGroup:
  - Selected item has Role.Tab and SemanticsProperties.Selected = true
  - Clicking an item invokes onItemSelected with the item's index
  - M-size button height is 48dp
  - Xs-size touch target is ≥ 48dp (minimumInteractiveComponentSize)
  - Each item renders with testTag \"button-group-item-N\"
  - Outer touch area tagged \"button-group-item-N-touch\"

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

#### Task 1c.3: Implement TastileButtonGroup

**Files:**
- Create: `app/src/main/java/app/tastile/android/core/designsystem/component/TastileButtonGroup.kt`

**Interfaces:**
- Consumes: `ButtonGroupSize`, `ButtonGroupItem` (from Task 1c.1), `TastileButtonGroupTokens.Default` (from Task 1c.1), `MaterialTheme.motionScheme.defaultEffectsSpec` (set up in Task 0.3), `LocalMinimumInteractiveComponentSize`.
- Produces: `@Composable fun TastileButtonGroup(items, selectedIndex, onItemSelected, size, modifier)`.

- [ ] **Step 1: Add file-level OptIn**

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
```

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
```

- [ ] **Step 2: Implement the composable**

Create `app/src/main/java/app/tastile/android/core/designsystem/component/TastileButtonGroup.kt`:

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.TastileButtonGroupTokens

private val tokens = TastileButtonGroupTokens.Default

private fun textStyleFor(size: ButtonGroupSize): TextStyle = when (size) {
    ButtonGroupSize.Xs -> MaterialTheme.typography.labelSmall
    ButtonGroupSize.S -> MaterialTheme.typography.labelSmall
    ButtonGroupSize.M -> MaterialTheme.typography.labelMedium
    ButtonGroupSize.L -> MaterialTheme.typography.labelLarge
    ButtonGroupSize.Xl -> MaterialTheme.typography.labelLarge
}

@Composable
fun TastileButtonGroup(
    items: List<ButtonGroupItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    size: ButtonGroupSize = ButtonGroupSize.M,
    modifier: Modifier = Modifier,
) {
    require(items.isNotEmpty()) { "TastileButtonGroup requires at least one item" }
    require(selectedIndex in items.indices) {
        "selectedIndex=$selectedIndex is out of range for items of size ${items.size}"
    }

    val minHeight: Dp = tokens.height(size)
    val minTouchTarget: Dp = 48.dp

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides if (minHeight < minTouchTarget) minTouchTarget else minHeight,
            ) {
                SegmentedButton(
                    selected = isSelected,
                    onClick = if (item.enabled) ({ onItemSelected(index) }) else ({ /* disabled */ }),
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                    modifier = Modifier
                        .height(minHeight)
                        .widthIn(min = minTouchTarget)
                        .testTag("button-group-item-$index-touch")
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        },
                    label = {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = tokens.horizontalPadding(size)),
                        ) {
                            item.icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    modifier = Modifier.size(tokens.iconSize(size)),
                                )
                            }
                            Text(
                                text = item.label,
                                style = textStyleFor(size),
                                modifier = Modifier.testTag("button-group-item-$index"),
                            )
                        }
                    },
                )
            }
        }
    }
}
```

- [ ] **Step 3: Run the pinning tests**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.core.designsystem.component.TastileButtonGroupTest"
```

Expected: PASS. If `assertHeightIsEqualTo(48.dp)` on `button-group-item-0-touch` fails because `LocalMinimumInteractiveComponentSize` doesn't apply to the height you measured, adjust the implementation to ensure the outer touch node is at least 48dp tall — **do not lower the test expectation**.

- [ ] **Step 4: Run the full unit-test suite**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/app/tastile/android/core/designsystem/component/TastileButtonGroup.kt
git commit -m "feat(designsystem): add TastileButtonGroup wrapper

Provides a TastileButtonGroup composable that:
  - Wraps SingleChoiceSegmentedButtonRow with explicit per-size tokens
  - Validates items.isNotEmpty() and selectedIndex in items.indices
  - Disables onClick for items with enabled = false
  - Tags each item with \"button-group-item-N\" and outer touch area with
    \"button-group-item-N-touch\"
  - Enforces minimum 48dp touch target via LocalMinimumInteractiveComponentSize

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

#### Task 1c.4: Phase 1c verification gate

- [ ] **Step 1: Run verify**

Run:
```
cd app && ../gradlew :app:verify
```

Expected: PASS.

- [ ] **Step 2: Mark Phase 1c complete**

Do not commit. Report green to the orchestrator.

---

## Phase 2: QuickCreate FAB Wiring (Sequential — depends on Phase 1b)

**Pre-condition:** Phase 1b is merged. `TastileFabMenu` and `FabMenuItem.Action` are available in `designsystem/component/`.

### Task 2.1: Wire TastileFabMenu into TimelineScreen

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt` (around line 205)
- Create: `app/src/test/java/app/tastile/android/ui/mobile/tabs/TimelineScreenFabTest.kt`

**Interfaces:**
- Consumes: existing `Overlay.QuickCreate` from `app/src/main/java/app/tastile/android/ui/mobile/Overlay.kt`, `TastileFabMenu` from Phase 1b, current call to `NiaFloatingActionButton` at line 205.
- Produces: a call to `TastileFabMenu` that, when the FAB is tapped, shows `Overlay.QuickCreate`. The previous `NiaFloatingActionButton` call is removed.

- [ ] **Step 1: Read TimelineScreen.kt around line 205**

Confirm the existing code shape (it currently calls `NiaFloatingActionButton(onClick = { overlay.show(Overlay.QuickCreate) }, ...)`). Note the surrounding composable's scope and any `androidx.compose.foundation.layout.Box` / `Scaffold` wrapper.

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/app/tastile/android/ui/mobile/tabs/TimelineScreenFabTest.kt`:

```kotlin
package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class TimelineScreenFabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `fab click shows Overlay QuickCreate`() {
        val overlay: OverlayViewModel = mockk(relaxed = true)
        every { overlay.show(any()) } returns Unit

        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimelineScreen(overlay = overlay)
                }
            }
        }
        composeTestRule.onNodeWithTag("quick-create-fab").performClick()
        verify { overlay.show(Overlay.QuickCreate) }
    }

    @Test
    fun `quick-create-fab is rendered`() {
        val overlay: OverlayViewModel = mockk(relaxed = true)
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimelineScreen(overlay = overlay)
                }
            }
        }
        composeTestRule.onNodeWithTag("quick-create-fab").assertIsDisplayed()
    }
}
```

This test assumes:
- `TimelineScreen` is a public top-level composable that takes `overlay: OverlayViewModel` as its only required parameter (or via a Hilt default that the test can override). Verify by reading the current function signature.
- `Overlay` is `import app.tastile.android.ui.mobile.Overlay`.

If `TimelineScreen` is not directly callable from a test (e.g., it's wrapped in a `Hilt`-injected component), you may need to use a different test entry point — for example, navigate to the TimelineScreen via `MainActivityTestRule`. Adapt the test to the existing test infrastructure in `app/src/test/.../ui/mobile/tabs/` and `app/src/androidTest/.../ui/navigation/`.

- [ ] **Step 3: Run the test to confirm it fails**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.tabs.TimelineScreenFabTest"
```

Expected: FAIL — `quick-create-fab` test tag does not exist.

- [ ] **Step 4: Modify TimelineScreen.kt**

Replace the existing `NiaFloatingActionButton(...)` call at line 205 with `TastileFabMenu(...)`:

```kotlin
TastileFabMenu(
    mainIcon = Icons.Outlined.Add,             // or whatever icon was in use
    mainLabel = stringResource(R.string.quickcreate_open_cd),
    expanded = false,
    onExpandedChange = { /* Phase 2 ships collapsed-only; expanded menu is a future task */ },
    items = listOf(
        FabMenuItem.Action(
            icon = Icons.Outlined.Add,
            label = stringResource(R.string.quickcreate_event_label),
            onClick = { overlay.show(Overlay.QuickCreate) },
        ),
    ),
    modifier = Modifier.testTag("quick-create-fab"),
)
```

Adjust the strings (`R.string.quickcreate_open_cd`, `R.string.quickcreate_event_label`) to existing string resources in `app/src/main/res/values/strings.xml`. If they don't exist, add them with English copy following the existing naming convention.

If `TimelineScreen` previously showed a single FAB icon (no label), keep the visual same by only rendering the FAB shape (the `TastileFabMenu` with a single item will show only the main FAB when `expanded = false`).

- [ ] **Step 5: Run the test to confirm it passes**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.tabs.TimelineScreenFabTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```
git add app/src/main/java/app/tastile/android/ui/mobile/tabs/TimelineScreen.kt \
        app/src/test/java/app/tastile/android/ui/mobile/tabs/TimelineScreenFabTest.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(mobile): wire TastileFabMenu into TimelineScreen QuickCreate

Replaces the NiaFloatingActionButton at TimelineScreen.kt:205 with
TastileFabMenu. The single Action item delegates to
overlay.show(Overlay.QuickCreate), preserving the existing behavior.
The collapsed-only path is shipped now; the expanded menu surfaces
will be added when downstream consumers register menu items.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 2.2: Wire TastileFabMenu into TilesScreen

**Files:**
- Modify: `app/src/main/java/app/tastile/android/ui/mobile/tabs/TilesScreen.kt` (around line 124)
- Create: `app/src/test/java/app/tastile/android/ui/mobile/tabs/TilesScreenFabTest.kt`

**Interfaces:**
- Consumes: existing `NiaExtendedFloatingActionButton` call at line 124, `Overlay.QuickCreate`, `TastileFabMenu` from Phase 1b.
- Produces: a `TastileFabMenu` rendering for `+ New` that shows `Overlay.QuickCreate` on tap.

- [ ] **Step 1: Read TilesScreen.kt around line 124**

Confirm the existing call shape (it currently calls `NiaExtendedFloatingActionButton(...)`). Note that `NiaExtendedFloatingActionButton` renders an icon + label — `TastileFabMenu`'s collapsed form with one item will be visually different. If you want to preserve the "extended" visual, keep the FAB visible but render the label as `contentDescription` only (the collapsed FAB doesn't show a label).

If the visual difference is unacceptable, escalate — do NOT silently change the layout without confirming with the design owner. The minimal acceptable migration is collapsed-only with the icon visible.

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/app/tastile/android/ui/mobile/tabs/TilesScreenFabTest.kt` mirroring the structure of `TimelineScreenFabTest.kt`. Adapt:
- Replace `TimelineScreen` with `TilesScreen`.
- Keep `quick-create-fab` as the test tag for consistency with Task 2.1.
- Verify `overlay.show(Overlay.QuickCreate)` is invoked on click.

- [ ] **Step 3: Run the test to confirm it fails**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.tabs.TilesScreenFabTest"
```

Expected: FAIL.

- [ ] **Step 4: Modify TilesScreen.kt**

Replace the existing `NiaExtendedFloatingActionButton(...)` call at line 124 with `TastileFabMenu(...)`:

```kotlin
TastileFabMenu(
    mainIcon = Icons.Outlined.Add,
    mainLabel = stringResource(R.string.quickcreate_open_cd),
    expanded = false,
    onExpandedChange = {},
    items = listOf(
        FabMenuItem.Action(
            icon = Icons.Outlined.Add,
            label = stringResource(R.string.quickcreate_event_label),
            onClick = { overlay.show(Overlay.QuickCreate) },
        ),
    ),
    modifier = Modifier.testTag("quick-create-fab"),
)
```

- [ ] **Step 5: Run the test to confirm it passes**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest --tests "app.tastile.android.ui.mobile.tabs.TilesScreenFabTest"
```

Expected: PASS.

- [ ] **Step 6: Run the full unit-test suite**

Run:
```
cd app && ../gradlew :app:testDebugUnitTest
```

Expected: PASS — `QuickCreateSheetMobile.kt` remains unchanged and its existing test (if any) still passes because `Overlay.QuickCreate` is still the entry point.

- [ ] **Step 7: Commit**

```
git add app/src/main/java/app/tastile/android/ui/mobile/tabs/TilesScreen.kt \
        app/src/test/java/app/tastile/android/ui/mobile/tabs/TilesScreenFabTest.kt
git commit -m "feat(mobile): wire TastileFabMenu into TilesScreen QuickCreate

Replaces the NiaExtendedFloatingActionButton at TilesScreen.kt:124
with TastileFabMenu. The single Action item delegates to
overlay.show(Overlay.QuickCreate), preserving existing behavior.

Visual note: the extended-fab visual label is now a
contentDescription on the main FAB until menu items are registered
to surface as visible labels.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 2.3: Phase 2 verification gate

- [ ] **Step 1: Run verify**

Run:
```
cd app && ../gradlew :app:verify
```

Expected: PASS — all design-system and integration tests are green.

- [ ] **Step 2: Confirm `QuickCreateSheetMobile.kt` is unchanged**

Run:
```
git diff main HEAD -- app/src/main/java/app/tastile/android/ui/mobile/sheets/QuickCreateSheetMobile.kt
```

Expected: empty diff (no changes).

- [ ] **Step 3: Mark Phase 2 complete**

Do not commit. Report green to the orchestrator.

---

## Phase 3: Integration Verification (Sequential)

> **Plan amendment (2026-09-03):** Tasks 3.1 + 3.2 below are **CARVED OUT** of
> this plan's scope. They require an attached ADB device that was not
> available on the development host executing Phase 0–3. Tasks 3.1 + 3.2 are
> preserved below as struck-through sections (reference only) and will be
> re-introduced under a follow-up "M3X device integration" plan that has
> device-runner access. The remaining Phase 3 scope (Tasks 3.3 + 3.4) is
> unaffected. The carve-out record lives at
> `docs/superpowers/m3/phase-3-deferral.md`. With the carve-out, this plan's
> completion criteria are: Phase 0 + Phase 1 + Phase 2 + Task 3.3 + Task 3.4.



### ~~Task 3.1: Run gfxinfo to confirm motion physics frame rate~~ — **CARVED OUT 2026-09-03**

> **Scope notice:** ~~This task is preserved below for reference only. It is
> no longer part of the M3 Expressive plan's scope. See
> `docs/superpowers/m3/phase-3-deferral.md` for the carve-out rationale and
> the verbatim run commands that ship to the follow-up "M3X device
> integration" plan.~~
>
> **Files:** ~~(No file changes — verification only)~~
>
> **Pre-condition:** ~~Connected Android device or emulator running Android 12+ (API 31+) with developer options enabled.~~
>
> - [ ] ~~**Step 1: Start gfxinfo collection**~~
>
> ~~Run:~~
> ~~```
> adb shell setprop debug.hwui.profile true
> adb shell dumpsys gfxinfo app.tastile.android reset
> ```~~
>
> - [ ] ~~**Step 2: Build and install the debug APK**~~
>
> ~~Run:~~
> ~~```
> cd app && ../gradlew :app:installDebug
> adb shell am start -n app.tastile.android/.MainActivity
> ```~~
>
> - [ ] ~~**Step 3: Trigger the QuickCreate FAB**~~
>
> ~~Manually navigate to `TimelineScreen` (or `TilesScreen`), tap the FAB. Wait 5 seconds. Tap system back.~~
>
> - [ ] ~~**Step 4: Capture gfxinfo dump**~~
>
> ~~Run:~~
> ~~```
> adb shell dumpsys gfxinfo app.tastile.android framestats
> ```~~
>
> ~~Expected:~~
> ~~- Average frame time on API 31+ ≤ 16.67ms (60 fps) during FAB rotation animation.~~
> ~~- No jank flag (frames > 16.67ms but < 33.33ms) longer than 5% of total frames during animation.~~
>
> ~~If the test device is below 60fps target, do NOT lower the threshold. Instead:~~
> ~~- Note the device model and OS version.~~
> ~~- Document as a known limitation in `docs/superpowers/m3/motion-perf.md`.~~
> ~~- Open an issue tracking a `MotionScheme` with relaxed stiffness for low-end devices.~~
>
> - [ ] ~~**Step 5: Disable gfxinfo profiling**~~
>
> ~~```
> adb shell setprop debug.hwui.profile false
> ```~~
>
> - [ ] ~~**Step 6: Commit (only if docs were added)**~~
>
> ~~If you added `docs/superpowers/m3/motion-perf.md`:~~
>
> ~~```
> git add docs/superpowers/m3/motion-perf.md
> git commit -m "docs(superpowers): record motion physics frame rate baseline
>
> Captures the gfxinfo baseline for the QuickCreate FAB rotation under
> MotionScheme.expressive(). Threshold: 60fps target with ≤5% jank.
>
> Co-Authored-By: Claude Code <noreply@anthropic.com>"
> ```~~

---

### ~~Task 3.2: Add instrumented QuickCreate smoke test~~ — **CARVED OUT 2026-09-03**

> **Scope notice:** ~~This task is preserved below for reference only. It is
> no longer part of the M3 Expressive plan's scope. See
> `docs/superpowers/m3/phase-3-deferral.md` for the carve-out rationale and
> the verbatim run commands that ship to the follow-up "M3X device
> integration" plan.~~
>
> **Files:** ~~Create: `app/src/androidTest/java/app/tastile/android/ui/navigation/QuickCreateSmokeTest.kt`~~
>
> **Interfaces:** ~~Consumes: existing `MainActivityTestRule` in `app/src/androidTest/java/app/tastile/android/util/MainActivityTestRule.kt`, `createAndroidComposeRule`. Produces: an instrumented test that opens Dashboard → TimelineScreen → taps `quick-create-fab` → asserts `QuickCreateSheetMobile` is shown.~~
>
> - [ ] ~~**Step 1: Read existing navigation tests for the test harness**~~
>
> ~~Open `app/src/androidTest/java/app/tastile/android/ui/navigation/SidePanelSheetNavigationTest.kt` to see the conventions for `createAndroidComposeRule` / `MainActivityTestRule`.~~
>
> - [ ] ~~**Step 2: Create the smoke test file**~~
>
> ~~```kotlin
> package app.tastile.android.ui.navigation
>
> import androidx.compose.ui.test.assertIsDisplayed
> import androidx.compose.ui.test.junit4.createAndroidComposeRule
> import androidx.compose.ui.test.onNodeWithTag
> import androidx.compose.ui.test.performClick
> import app.tastile.android.util.MainActivityTestRule
> import org.junit.Rule
> import org.junit.Test
>
> class QuickCreateSmokeTest {
>
>     @get:Rule
>     val rule = MainActivityTestRule(createAndroidComposeRule())
>
>     @Test
>     fun timelineQuickCreateFab_opensSheet() {
>         rule.composeTestRule.onNodeWithTag("quick-create-fab").performClick()
>         // QuickCreateSheetMobile renders its content; assert one of its core nodes
>         // (e.g. the close button or the Create button) is displayed.
>         rule.composeTestRule.onNodeWithTag("quick-create-close").assertIsDisplayed()
>     }
> }
> ```~~
>
> - [ ] ~~**Step 3: Run the smoke test on a connected device**~~
>
> ~~Run:~~
> ~~```
> cd app && ../gradlew :app:connectedDebugAndroidTest \
>     --tests "app.tastile.android.ui.navigation.QuickCreateSmokeTest"
> ```~~
>
> ~~Expected: PASS. If the test fails because `MainActivityTestRule` requires a different constructor signature, adjust the test to match the rule's actual API. If `createAndroidComposeRule` returns a different rule type, wrap it accordingly.~~
>
> - [ ] ~~**Step 4: Commit**~~
>
> ~~```
> git add app/src/androidTest/java/app/tastile/android/ui/navigation/QuickCreateSmokeTest.kt
> git commit -m "test(androidTest): add QuickCreate FAB smoke test
>
> Verifies the end-to-end path: TimelineScreen FAB → Overlay.QuickCreate
> → QuickCreateSheetMobile. Uses the existing MainActivityTestRule
> harness and asserts the close button test tag rendered by the sheet.
>
> Run with:
>   ./gradlew :app:connectedDebugAndroidTest \
>     --tests \"app.tastile.android.ui.navigation.QuickCreateSmokeTest\"
>
> Co-Authored-By: Claude Code <noreply@anthropic.com>"
> ```~~

---

### Task 3.3: Update README and M3 baseline docs

**Files:**
- Modify: `README.md` (Material 3 section if it exists)
- Modify: `docs/superpowers/m3/before-reports/README.md` or equivalent baseline doc

- [ ] **Step 1: Read README.md**

Look for an existing "Material 3" or "Design system" section. If present, update it to mention:
- LoadingWheel now backed by `LoadingIndicator` (M3 Expressive).
- `MaterialTheme(motionScheme = MotionScheme.expressive())` is set in `TastileTheme`.
- `TastileFabMenu` and `TastileButtonGroup` are available in `designsystem/component/`.

If no such section exists, add a short "Material 3 Expressive" subsection under the existing "Architecture" or "Tech stack" section.

- [ ] **Step 2: Update the M3 baseline doc**

In `docs/superpowers/m3/before-reports/`, add or update a note that:
- material3 is now pinned at `1.5.0-alpha27`.
- Compose Compiler Reports under `app/build/compose-reports/` should be re-captured after this change.

- [ ] **Step 3: Run verify one more time**

Run:
```
cd app && ../gradlew :app:verify
```

Expected: PASS.

- [ ] **Step 4: Commit**

```
git add README.md docs/superpowers/m3/
git commit -m "docs: reflect Material 3 Expressive migration in README and baseline docs

Updates the README's Material 3 / design system section to call out
the new LoadingWheel internals, the MotionScheme.expressive()
injection, and the new TastileFabMenu and TastileButtonGroup
components. Notes in the M3 baseline that material3 is pinned at
1.5.0-alpha27 and Compose Compiler Reports should be re-captured.

Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 3.4: Final verification gate

- [ ] **Step 1: Run all verification**

Run:
```
cd app && ../gradlew :app:verify
cd app && ../gradlew :app:connectedDebugAndroidTest \
    --tests "app.tastile.android.ui.navigation.QuickCreateSmokeTest"
```

Expected: ALL PASS.

- [ ] **Step 2: Confirm no forbidden markers**

Run:
```
git grep -n "// m3e-allow:" || echo "no m3e-allow references (correct)"
git grep -n "// m2-allow:" app/src/main/java/app/tastile/android/ui/dashboard \
                     app/src/main/java/app/tastile/android/ui/mobile \
                     app/src/main/java/app/tastile/android/ui/account
```

Expected:
- `no m3e-allow references (correct)`
- Only legitimate `// m2-allow:` markers remain (those that were already in the codebase before this migration). The migration should NOT have added new ones.

- [ ] **Step 3: Mark Phase 3 complete**

Report green to the orchestrator. The M3 Expressive migration is done.

---

## Self-Review Checklist (Executor — run after writing the plan, before shipping)

The following checks are performed by the plan author before handoff. If you (the implementer) find any issue when running through a task, surface it rather than working around it.

1. **Spec coverage:**
   - Goal 1 (LoadingWheel rewrite): Tasks 1a.1 + 1a.2 ✓
   - Goal 2 (motionScheme): Task 0.3 ✓
   - Goal 3 (TastileFabMenu + QuickCreate): Tasks 1b.* + 2.1 + 2.2 ✓
   - Goal 4 (TastileButtonGroup): Tasks 1c.* ✓
   - Goal 5 (1.5.0-alpha27 pin): Task 0.1 ✓
   - Goal 6 (shape tokens): Task 0.2 ✓
   - Phase 3 integration (post-carve-out): Tasks 3.3 + 3.4 ✓
     (Tasks 3.1 + 3.2 carved out 2026-09-03 — see
     `docs/superpowers/m3/phase-3-deferral.md`)

2. **No placeholder strings:** scan for "TODO" / "TBD" / "fill in" / "similar to" — none.

3. **Type consistency:**
   - `FabMenuItem.Action` defined in Task 1b.1, consumed by Tasks 1b.2 / 1b.3 / 2.1 / 2.2 ✓
   - `ButtonGroupSize` / `ButtonGroupItem` / `TastileButtonGroupTokens` defined in Task 1c.1, consumed by Task 1c.3 ✓
   - `quick-create-fab` testTag set in Task 2.1 ✓
     (assertion that was to live in Task 3.2 is now carved out to M3X;
     pre-existing `quick-create-close` testTag at
     `QuickCreateSheetMobile.kt:157` remains reachable through that future
     smoke test in M3X)
   - `quick-create-close` testTag already exists in `QuickCreateSheetMobile.kt:157` ✓

4. **No file conflicts between parallel sub-agents (Phase 1):**
   - 1a: only `LoadingWheel.kt` + its test
   - 1b: only `FabMenuItem.kt` + `TastileFabMenu.kt` + their tests
   - 1c: only `ButtonGroupSize.kt` + `ButtonGroupItem.kt` + `TastileButtonGroupTokens.kt` + `TastileButtonGroup.kt` + their tests
   - No two phases touch the same file ✓

5. **Existing public API preservation:** `NiaLoadingWheel` / `NiaOverlayLoadingWheel` signatures unchanged. `QuickCreateSheetMobile.kt` unchanged. ✓

6. **Dependency pin:** `material3:1.5.0-alpha27` is the only version touched. ✓
