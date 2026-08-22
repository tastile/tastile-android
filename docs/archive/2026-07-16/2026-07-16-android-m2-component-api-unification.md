# Android M3 Optimization — Phase M2 (Component API Unification) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Lock the M3 component API conventions. Audit and bring every existing wrapper in `ui/designsystem/AppComponents.kt` and `ui/mobile/designsystem/MobileComponents.kt` into compliance with L0 conventions (Modifier first, `@Stable` / `@Immutable`, no `Color(0x…)` literals, semantic-role annotations). Extend the `verifyDesignSystemImports` Gradle guard from one file to the full `ui/{dashboard, mobile, account}/**` trees. Add two custom lint rules (`WrapperParameterOrder`, `WrapperStability`) so future PRs cannot regress the conventions.

**Architecture:** Code-only changes to two existing files (`AppComponents.kt`, `MobileComponents.kt`) and one Gradle script (`app/build.gradle.kts`). New lint rules live in a new module `app/lint-rules/` (a Kotlin-only Gradle subproject; no Android dependencies) so they can be applied to `:app` from a single source of truth. The guard extension is a one-shot widening commit — the first commit of this phase fixes its own violations; subsequent phases operate on a clean baseline.

**Tech Stack:** Kotlin 2.x, Compose Material3 BOM 2024.12.01, AGP 9.2.1, JUnit 4.13.2, Robolectric 4.14, lint API `com.android.tools.lint:lint:31.x` (matched to AGP).

**Spec reference:** `tastile-android/docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md` §4 (Phase M2).

**Out-of-scope reminder:** Component tactical fixes that the sibling MUI Misuse spec handles (ListItem adoption, FilledTonalButton adoption, sheet-picker conversion, `Card(onClick)` replacement) — see `docs/superpowers/specs/2026-07-16-android-ui-misuse-redesign-design.md` §3.1–§3.4. This plan only enforces the conventions; it does not perform those substitutions.

---

## File structure

### New files

| Path | Responsibility |
|---|---|
| `app/lint-rules/build.gradle.kts` | Java/Kotlin library subproject; lint-rule APIs on `compileOnly` |
| `app/lint-rules/src/main/java/app/tastile/android/lint/WrapperParameterOrderDetector.kt` | Reports when `Modifier` is not the first parameter (rule C1) or `enabled` is not the last (rule C2) |
| `app/lint-rules/src/main/java/app/tastile/android/lint/WrapperStabilityDetector.kt` | Reports when a `@Composable` wrapper receiving a lambda or `Color` is missing `@Stable` (rule C3) |
| `app/lint-rules/src/main/java/app/tastile/android/lint/AOSP` IssueRegistry and `lint.xml` stubs |
| `app/lint-rules/src/test/java/app/tastile/android/lint/WrapperParameterOrderDetectorTest.kt` | Lint-rule unit test (positive + negative cases) |
| `app/lint-rules/src/test/java/app/tastile/android/lint/WrapperStabilityDetectorTest.kt` | Lint-rule unit test |
| `settings.gradle.kts` (edit) | Include `:app:lint-rules` |

### Modified files

| Path | Change |
|---|---|
| `app/build.gradle.kts` | Apply `:app:lint-rules`; widen `designSystemGuardFiles`; add `tokenGuardedFiles` for the regex check |
| `app/src/main/java/app/tastile/android/ui/designsystem/AppComponents.kt` | C1–C5 audit + `@Stable` annotations + Role/contentDescription |
| `app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt` | Same audit |
| `app/src/test/java/app/tastile/android/ui/designsystem/TokenTest.kt` | Asserts no `Color(0x...` literal outside `BrandColors.kt` (and the legacy `GrayColors` removal in M1) |

---

## Tasks

### Task 1: Widening commit — extend `verifyDesignSystemImports` (TDD red on guard task)

**Files:**
- Edit: `app/build.gradle.kts`

- [ ] **Step 1: Replace the existing one-file allowlist with a directory globs allowlist**

```kotlin
// app/build.gradle.kts (replace val designSystemGuardFiles)
val designSystemGuardRoots = listOf(
    "app/src/main/java/app/tastile/android/ui/dashboard",
    "app/src/main/java/app/tastile/android/ui/mobile",
    "app/src/main/java/app/tastile/android/ui/account",
)
val designSystemGuardFiles: List<File> =
    designSystemGuardRoots.flatMap { root ->
        fileTree(root) { include("**/*.kt") }.files
    }
```

- [ ] **Step 2: Run the guard locally**

Run: `./gradlew :app:verifyDesignSystemImports`
Expected: FAIL with a list of files importing one of the forbidden prefixes (`androidx.compose.material3.Color`, `ColorScheme`, `Button`, `Card`, `FilledTonalButton`, `OutlinedButton`, `TextButton`, `AssistChip`, `ElevatedButton`, `ElevatedCard`, `OutlinedCard`, `FilterChip`, `InputChip`, `SuggestionChip`, `Surface(onClick = ...)`).

- [ ] **Step 3: Commit the failing guard widening (no fixes yet)**

```bash
git add app/build.gradle.kts
git commit -m "ci(android): widen DesignSystem guard to ui/{dashboard,mobile,account}/** (red)"
```

This commit intentionally fails the guard. Subsequent tasks fix the violations.

---

### Task 2: Fix guard violations from Task 1

**Files:** Each file listed in the Task 1 output.

- [ ] **Step 1: Inventory violations**

```bash
./gradlew :app:verifyDesignSystemImports 2>&1 | tee logs/m2-guard-violations.txt
```

- [ ] **Step 2: Per-file replacement strategy**

For each violation, pick one:

- **Replace direct M3 import with the wrapper**. e.g. `import androidx.compose.material3.Button` → `import app.tastile.android.ui.designsystem.AppPrimaryButton` (or `MobileComponents.PrimaryButton` for mobile-only). The wrapper handles the M3 concerns internally.
- **Move the call site to the design system layer** if it's a wrapper definition (imports must be in the design system file, not in `ui/dashboard/`).
- **Whitelist with a per-line comment** only if the import is one of: `Icon`, `Text`, `HorizontalDivider`, `VerticalDivider`, `CircularProgressIndicator`, `LinearProgressIndicator`. Other names must use a wrapper.

- [ ] **Step 3: Run `./gradlew :app:verifyDesignSystemImports`, expect PASS**

- [ ] **Step 4: Run `./gradlew :app:assembleDebug`, expect BUILD SUCCESSFUL**
- [ ] **Step 5: Run `./gradlew :app:testDebugUnitTest`, expect green**
- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/app/tastile/android/
git commit -m "refactor(android): route ui/{dashboard,mobile,account} through wrappers, satisfy extended guard"
```

---

### Task 3: Wrapper audit — apply C1–C5 to existing wrappers

**Files:**
- Edit: `app/src/main/java/app/tastile/android/ui/designsystem/AppComponents.kt`
- Edit: `app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt`

- [ ] **Step 1: Read the inventory**

For each wrapper in `AppComponents.kt` and `MobileComponents.kt`, check:

- C1: `Modifier` is the first optional parameter (after any required label params).
- C2: `enabled: Boolean = true` is the last optional parameter.
- C3: Composable that takes a `() -> Unit` lambda or `Color` directly is annotated `@Stable`. Data classes are `@Immutable`.
- C4: No `Color(0x...` literals (the only exception is `BrandColors.kt` from M1).
- C5: Each interactive wrapper sets `Modifier.semantics { contentDescription = ...; role = Role.X }` (this is also picked up by Phase M5 audit).

Existing wrappers in scope: `AppPrimaryButton`, `AppSecondaryButton`, `AppTonalButton`, `AppTextButton`, `AppListItem`, `AppPickerButton`, `AppPickerButtonCompact`, `AppSectionHeader`, `AppStatChip`, `AppEmptyState`, plus mobile-only equivalents in `MobileComponents.kt`. Use this snippet as the canonical pattern:

```kotlin
@Stable
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = Button(
    onClick = onClick,
    modifier = modifier.semantics { role = Role.Button },
    enabled = enabled,
) { Text(text) }
```

- [ ] **Step 2: Apply changes per wrapper.** Use a single commit per wrapper family (buttons, lists, pickers, chips).

```bash
git add app/src/main/java/app/tastile/android/ui/designsystem/AppComponents.kt
git commit -m "refactor(android): apply L0 conventions to AppComponents.kt buttons"
git add app/src/main/java/app/tastile/android/ui/mobile/designsystem/MobileComponents.kt
git commit -m "refactor(android): apply L0 conventions to MobileComponents.kt chips"
# … continue per family
```

- [ ] **Step 3: Run `./gradlew :app:assembleDebug :app:testDebugUnitTest`, expect green after each commit**

---

### Task 4: Lint-rules subproject — skeleton + `WrapperParameterOrder`

**Files:**
- Edit: `settings.gradle.kts`
- Create: `app/lint-rules/build.gradle.kts`
- Create: `app/lint-rules/src/main/java/app/tastile/android/lint/WrapperParameterOrderDetector.kt`
- Create: `app/lint-rules/src/main/java/app/tastile/android/lint/IssueRegistry.kt`
- Create: `app/src/test/java/app/tastile/android/lint/WrapperParameterOrderDetectorTest.kt`

- [ ] **Step 1: Add `:lint-rules` to `settings.gradle.kts`**

```kotlin
// settings.gradle.kts (add after include(":app"))
include(":lint-rules")
```

- [ ] **Step 2: `app/lint-rules/build.gradle.kts`**

```kotlin
plugins {
    `java-library`
    kotlin("jvm")
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
    compileOnly("com.android.tools.lint:lint-api:31.4.2")
    compileOnly("com.android.tools.lint:lint-checks:31.4.2")
    testImplementation("junit:junit:4.13.2")
}
```

(Use the AGP-matched lint version: 31.4.2 is conservative for AGP 9.2.1. Adjust if mismatch.)

- [ ] **Step 3: `IssueRegistry.kt`**

```kotlin
package app.tastile.android.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

class IssueRegistry : IssueRegistry() {
    override val issues = listOf(
        WrapperParameterOrderDetector.ISSUE,
    )
    override val api = CURRENT_API
    override val minApi = 14
    override val vendor = Vendor(
        vendorName = "Tastile",
        identifier = "app.tastile.android.lint",
        feedbackUrl = "https://github.com/tastile/tastile-android/issues",
    )
}
```

- [ ] **Step 4: `WrapperParameterOrderDetector.kt`**

```kotlin
package app.tastile.android.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UMethod

class WrapperParameterOrderDetector : Detector(), Detector.UastScanner {
    override fun visitMethod(context: JavaContext, method: UMethod) {
        if (!method.isComposable()) return
        if (!method.isInDesignSystemPackage()) return

        val psi = method.sourcePsi as? PsiMethod ?: return
        val params = psi.parameterList.parameters

        // Rule C1: Modifier must be first optional parameter.
        val firstModifierIndex = params.indexOfFirst { it.type.canonicalText.contains("Modifier") }
        // Rule C2: `enabled` must be the last optional parameter.
        val enabledIndex = params.indexOfFirst { it.name == "enabled" }

        // (Implementation elided — straightforward; compares against @JvmDefault
        // positional indexes and reports at the source line.)
    }

    companion object {
        val ISSUE = Issue.create(
            id = "WrapperParameterOrder",
            briefDescription = "Wrapper parameter order violates L0 conventions",
            explanation = "C1: Modifier must be the first optional parameter. " +
                "C2: `enabled: Boolean = true` must be the last optional parameter.",
            category = Category.API,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                WrapperParameterOrderDetector::class.java,
                com.android.tools.lint.detector.api Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
```

(Implementation details for the actual parameter-order assertion are deferred to the executor; the public contract is `Issue`, the detector class, and the test that exercises positive/negative cases.)

- [ ] **Step 5: `WrapperParameterOrderDetectorTest.kt`**

```kotlin
package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import org.junit.Test

class WrapperParameterOrderDetectorTest : LintDetectorTest() {
    override fun getDetector() = WrapperParameterOrderDetector()
    override fun getIssues() = listOf(WrapperParameterOrderDetector.ISSUE)

    @Test fun `Modifier-first reports positive`() {
        lint().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Wrong(text: String, modifier: Modifier = Modifier) {}
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    @Test fun `Modifier-first reports negative when Modifier is first optional`() {
        lint().files(kotlin(
            """
            package app.tastile.android.ui.designsystem
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            @Composable
            fun Right(text: String, modifier: Modifier = Modifier) {}
            """.trimIndent()
        )).run().expectWarningCount(0)
    }
}
```

- [ ] **Step 6: Wire `:lint-rules` into `:app`**

In `app/build.gradle.kts`:

```kotlin
dependencies {
    lintRules(project(":lint-rules"))
}
```

(Make sure the `lint-rules` project has a `Lint-Registry` manifest entry pointing to `app.tastile.android.lint.IssueRegistry`. Generated from the `IssueRegistry` class.)

- [ ] **Step 7: Run, expect PASS**

Run: `./gradlew :lint-rules:test :app:lint`
Expected: 2 tests PASS; `:app:lint` reports 0 new issues for files already C1-conformant, otherwise issues only in `ui/designsystem/` and `ui/mobile/designsystem/`.

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts app/lint-rules/ app/build.gradle.kts
git commit -m "feat(android): add WrapperParameterOrder lint rule (C1, C2)"
```

---

### Task 5: Add `WrapperStabilityDetector` (rule C3)

**Files:**
- Create: `app/lint-rules/src/main/java/app/tastile/android/lint/WrapperStabilityDetector.kt`
- Create: `app/lint-rules/src/test/java/app/tastile/android/lint/WrapperStabilityDetectorTest.kt`
- Edit: `app/lint-rules/src/main/java/app/tastile/android/lint/IssueRegistry.kt`

- [ ] **Step 1: Detector**

```kotlin
class WrapperStabilityDetector : Detector(), Detector.UastScanner {
    override fun visitMethod(context: JavaContext, method: UMethod) {
        if (!method.isComposable()) return
        if (!method.isInDesignSystemPackage()) return
        if (method.hasAnnotation("@Stable") || method.hasAnnotation("@Immutable")) return

        val receivesLambda = method.uastParameters.any { it.type.canonicalText.contains("Function") }
        val receivesColor  = method.uastParameters.any { it.type.canonicalText == "androidx.compose.ui.graphics.Color" }
        if (receivesLambda || receivesColor) {
            context.report(
                WrapperStabilityDetector.ISSUE,
                method,
                context.getLocation(method),
                "@Composable wrapper accepting a lambda or Color must be @Stable or @Immutable",
            )
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "WrapperStability",
            briefDescription = "Wrapper stability annotation required",
            explanation = "Wrappers that accept a lambda or Color directly must be annotated `@Stable` " +
                "(or `@Immutable` if they are a data class).",
            category = Category.PERFORMANCE,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(
                WrapperStabilityDetector::class.java,
                com.android.tools.lint.detector.api.Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
```

- [ ] **Step 2: Add to `IssueRegistry.issues`**

```kotlin
override val issues = listOf(
    WrapperParameterOrderDetector.ISSUE,
    WrapperStabilityDetector.ISSUE,
)
```

- [ ] **Step 3: Test** (mirrors Task 4 Step 5 — kotlin test fixtures with one wrapper `@Stable` and one without; expect 1 warning).

- [ ] **Step 4: Commit**

```bash
git add app/lint-rules/
git commit -m "feat(android): add WrapperStability lint rule (C3)"
```

---

### Task 6: TokenTest — assert no `Color(0x…` outside `BrandColors.kt` (rule C4 test)

**Files:**
- Create: `app/src/test/java/app/tastile/android/ui/designsystem/TokenTest.kt`

- [ ] **Step 1: TokenTest**

```kotlin
package app.tastile.android.ui.designsystem

import org.junit.Test
import java.io.File

class TokenTest {
    @Test fun `no Color(0x literal outside BrandColors.kt and Theme.kt`() {
        val root = File("src/main/java/app/tastile/android/ui")
        val allowList = setOf(
            "designsystem/BrandColors.kt",
            "designsystem/Theme.kt",
        )
        val violation = root.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { it.readText().contains(Regex("""Color\(0x[0-9A-Fa-f]+""")) }
            .filter { f -> allowList.none { f.path.endsWith(it) } }
            .toList()
        assert(violation.isEmpty()) {
            "Found forbidden Color(0x literals in:\n" +
                violation.joinToString("\n") { " - " + it.path }
        }
    }
}
```

- [ ] **Step 2: Run, expect PASS** (`./gradlew :app:testDebugUnitTest --tests '*TokenTest*'`)
- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/app/tastile/android/ui/designsystem/TokenTest.kt
git commit -m "test(android): TokenTest rejects Color(0x literals outside BrandColors/Theme"
```

---

### Task 7: Compile-time regressions check + open PR

- [ ] **Step 1: Full local sweep**

```bash
./gradlew :app:lint :app:verifyDesignSystemImports :app:testDebugUnitTest
```

Expected: green.

- [ ] **Step 2: Compose Compiler Reports**

```bash
./gradlew :app:assembleDebug -PenableComposeCompilerReports=true
ls app/build/reports/compiler-reports/ 2>/dev/null | head
```

Archive to `logs/m2-compiler-reports/before.txt` (used by Phase M3 to compare).

- [ ] **Step 3: Open PR**

Title: `feat(android): unify component wrappers + extend DesignSystem guard`
Body:

```
### Phase M2: Component API Unification
- Widened verifyDesignSystemImports to ui/{dashboard,mobile,account}/**.
- New :lint-rules subproject with WrapperParameterOrder (C1, C2) and
  WrapperStability (C3) detectors.
- Audited AppComponents.kt + MobileComponents.kt to L0 conventions.
- TokenTest asserts no `Color(0x…` outside the allow-list.
- Parity safety: no new controls; no new i18n keys; no reorder.
- Tests: TokenTest, WrapperParameterOrderDetectorTest, WrapperStabilityDetectorTest.

Spec: docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md §4
Sprint status: this plan is independent of M3–M5 but unblocks state work in M3.
```

---

## Completion KPI recap

- [ ] `verifyDesignSystemImports` runs green against widened glob.
- [ ] `:lint-rules:test` — both detector tests PASS.
- [ ] `TokenTest` PASS.
- [ ] `./gradlew :app:lint` — 0 warnings outside `ui/designsystem/` and `ui/mobile/designsystem/`.
- [ ] `app/build/reports/compiler-reports/before.txt` archived under `logs/m2-compiler-reports/`.
- [ ] PR opened: `feat(android): unify component wrappers + extend DesignSystem guard`.

---

## Out of scope for this plan

- Tactical ListItem / FilledTonalButton / sheet-picker adoption — see MUI Misuse spec.
- LazyColumn keys / state stability — Phase M3.
- Timeline perf — Phase M4.
- DC settings toggle + contrast audit — Phase M5.

If a Task above surfaces a defect outside Phase M2 scope, file an issue and defer — do not expand the PR.
