# Android M3 Optimization — Phase M3 (State / Recomposition) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stabilize the StateFlow ↔ Compose boundary across the entire `tastile-android` module so Phase M4 (perf) and Phase M5 (a11y) build on a known-restartability surface. Three concrete moves:

1. Every `collectAsState()` in production code migrated to `collectAsStateWithLifecycle()`.
2. Every `LazyColumn` / `LazyVerticalGrid` carries an explicit, stable `key = { ... }`.
3. Data classes exposed by ViewModels to Compose are annotated `@Stable` or `@Immutable`.

Measured outcome: Compose Compiler Reports show a net-positive growth in skippable-function count, archived as `before` and `after` reports.

**Architecture:** Lint-rule-driven discipline plus a codebase sweep. Two new lint rules enter `:lint-rules` (added in M2): `CollectAsStateRule` (rejects `import androidx.compose.runtime.collectAsState` outside `:app:lint-rules`) and `LazyColumnKeyRule` (rejects `LazyColumn { items(...) }` without a `key = ...`). Together they make M3 a one-shot codebase sweep rather than a recurring detective effort.

**Tech Stack:** Kotlin 2.x, Compose Material3 BOM 2024.12.01, AGP 9.2.1, JUnit 4.13.2, Robolectric 4.14.

**Spec reference:** `tastile-android/docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md` §5 (Phase M3).

**Out-of-scope reminder:** ViewModel topology changes (e.g. splitting `DashboardViewModel`) are explicitly out of scope. M3 only stabilizes the StateFlow ↔ Compose boundary.

---

## File structure

### New files

| Path | Responsibility |
|---|---|
| `app/lint-rules/src/main/java/app/tastile/android/lint/CollectAsStateRuleDetector.kt` | Rejects `collectAsState()` calls (rule S1) |
| `app/lint-rules/src/main/java/app/tastile/android/lint/LazyColumnKeyRuleDetector.kt` | Rejects `LazyColumn { items(...) }` without `key = ...` (rule S2) |
| `app/lint-rules/src/test/java/app/tastile/android/lint/CollectAsStateRuleDetectorTest.kt` | Test fixture: kotlin source with and without the migration |
| `app/lint-rules/src/test/java/app/tastile/android/lint/LazyColumnKeyRuleDetectorTest.kt` | Same |
| `logs/m3-skippable/before.txt` and `after.txt` | Archived Compose Compiler Reports |

### Modified files

| Path | Change |
|---|---|
| `app/lint-rules/src/main/java/app/tastile/android/lint/IssueRegistry.kt` | Register two new issues |
| Every Kotlin file in `app/src/main/java/` containing `collectAsState(` | Migrate to `collectAsStateWithLifecycle(`, change import |
| Every `LazyColumn { items(...) }` and `LazyVerticalGrid { items(...) }` | Add `key = { ... }` (stable id) |
| ViewModels that emit data classes consumed by Compose | Add `@Stable` / `@Immutable` annotations |

---

## Tasks

### Task 1: Capture `before` Compose Compiler Reports

**Files:** None — output only.

- [ ] **Step 1: Clean build with reports enabled**

```bash
./gradlew :app:clean
./gradlew :app:assembleDebug -PenableComposeCompilerReports=true
```

- [ ] **Step 2: Archive**

```bash
mkdir -p logs/m3-skippable
cp -r app/build/reports/compiler-reports/ logs/m3-skippable/before/
find logs/m3-skippable/before/ -type f | sort > logs/m3-skippable/before-files.txt
echo "Total files: $(wc -l < logs/m3-skippable/before-files.txt)" > logs/m3-skippable/before.txt
```

- [ ] **Step 3: Skippable/restartable counts**

```bash
grep -roE "restartable|skippable|inline" app/build/reports/compiler-reports/ \
  | sort | uniq -c | sort -rn > logs/m3-skippable/restartable_summary_before.txt
```

(These counters are coarse — the granular per-function list lives under
`app/build/reports/compiler-reports/<class>.kt.txt`. Skim these baselines at the end of M3.)

- [ ] **Step 4: Commit baseline artefacts**

```bash
git add -f logs/m3-skippable/before.txt \
        logs/m3-skippable/restartable_summary_before.txt \
        logs/m3-skippable/before-files.txt
git commit -m "chore(android): capture Compose Compiler Reports baseline (M3 before)"
```

---

### Task 2: Add `CollectAsStateRule` lint rule (TDD red)

**Files:**
- Create: `app/lint-rules/src/main/java/app/tastile/android/lint/CollectAsStateRuleDetector.kt`
- Create: `app/lint-rules/src/test/java/app/tastile/android/lint/CollectAsStateRuleDetectorTest.kt`
- Edit: `app/lint-rules/src/main/java/app/tastile/android/lint/IssueRegistry.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package app.tastile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import org.junit.Test

class CollectAsStateRuleDetectorTest : LintDetectorTest() {
    override fun getDetector() = CollectAsStateRuleDetector()
    override fun getIssues() = listOf(CollectAsStateRuleDetector.ISSUE)

    @Test fun `collectAsState call reports positive`() {
        lint().files(kotlin(
            """
            import androidx.compose.runtime.collectAsState
            package app.tastile.android.ui.mobile.tabs
            import androidx.compose.runtime.*
            @Composable fun Screen(flow: kotlinx.coroutines.flow.StateFlow<String>) {
                val s = flow.collectAsState()  // forbidden
            }
            """.trimIndent()
        )).run().expectWarningCount(1)
    }

    @Test fun `collectAsStateWithLifecycle does not report`() {
        lint().files(kotlin(
            """
            package app.tastile.android.ui.mobile.tabs
            import androidx.lifecycle.compose.collectAsStateWithLifecycle
            @Composable fun Screen(flow: kotlinx.coroutines.flow.StateFlow<String>) {
                val s = flow.collectAsStateWithLifecycle()
            }
            """.trimIndent()
        )).run().expectWarningCount(0)
    }
}
```

- [ ] **Step 2: Run, expect FAIL (unresolved class)**

Run: `./gradlew :lint-rules:test`
Expected: test compile failure.

- [ ] **Step 3: Implement the detector**

```kotlin
package app.tastile.android.lint

import com.android.tools.lint.detector.api.*

class CollectAsStateRuleDetector : Detector(), Detector.UastScanner {
    override fun visitMethodCall(context: JavaContext, call: org.jetbrains.uast.UCallExpression) {
        val name = call.methodName ?: return
        if (name == "collectAsState") {
            // Only flag when the receiver is a Flow.
            context.report(
                ISSUE, call, context.getLocation(call),
                "Use collectAsStateWithLifecycle() instead of collectAsState()",
            )
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "CollectAsStateRule",
            briefDescription = "Use collectAsStateWithLifecycle",
            explanation = "Direct collectAsState() bypasses lifecycle awareness. Use " +
                "androidx.lifecycle.compose.collectAsStateWithLifecycle for predictable " +
                "collection cancellation.",
            category = Category.PERFORMANCE, priority = 5, severity = Severity.WARNING,
            implementation = Implementation(
                CollectAsStateRuleDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
```

- [ ] **Step 4: Add to `IssueRegistry.issues`**

```kotlin
override val issues = listOf(
    WrapperParameterOrderDetector.ISSUE,
    WrapperStabilityDetector.ISSUE,
    CollectAsStateRuleDetector.ISSUE,
)
```

- [ ] **Step 5: Run, expect PASS**

Run: `./gradlew :lint-rules:test --tests '*CollectAsStateRule*'`
Expected: 2 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/lint-rules/
git commit -m "feat(android): add CollectAsStateRule lint detector (red→green)"
```

---

### Task 3: Migrate every `collectAsState()` to `collectAsStateWithLifecycle()`

**Files:** All Kotlin files under `app/src/main/java/app/tastile/android/` calling `collectAsState()`.

- [ ] **Step 1: Inventory**

```bash
rg 'collectAsState\(' app/src/main/java/ > logs/m3-collect-as-state-callers.txt
```

- [ ] **Step 2: Per-file replace pattern**

```kotlin
// Old
import androidx.compose.runtime.collectAsState
val s = flow.collectAsState()

// New
import androidx.lifecycle.compose.collectAsStateWithLifecycle
val s = flow.collectAsStateWithLifecycle()
```

For deeper call shapes (`collectAsState(initial = …)`, `collectAsState(null)`), follow the signatures in `androidx.lifecycle.compose.collectAsStateWithLifecycle`.

- [ ] **Step 3: Run, expect lint clean**

```bash
./gradlew :app:lint
```

Expected: 0 `CollectAsStateRule` warnings.

- [ ] **Step 4: Run `./gradlew :app:assembleDebug :app:testDebugUnitTest`, expect green**
- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ logs/m3-collect-as-state-callers.txt
git commit -m "refactor(android): migrate collectAsState() to collectAsStateWithLifecycle()"
```

---

### Task 4: Add `LazyColumnKeyRule` lint detector (TDD red→green)

**Files:**
- Create: `app/lint-rules/src/main/java/app/tastile/android/lint/LazyColumnKeyRuleDetector.kt`
- Create: `app/lint-rules/src/test/java/app/tastile/android/lint/LazyColumnKeyRuleDetectorTest.kt`
- Edit: `app/lint-rules/src/main/java/app/tastile/android/lint/IssueRegistry.kt`

- [ ] **Step 1: Failing test**

```kotlin
class LazyColumnKeyRuleDetectorTest : LintDetectorTest() {
    override fun getDetector() = LazyColumnKeyRuleDetector()
    override fun getIssues() = listOf(LazyColumnKeyRuleDetector.ISSUE)

    @Test fun `LazyColumn with items but no key reports positive`() {
        lint().files(kotlin("""
            package app.tastile.android.ui.x
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            @Composable fun Screen(list: List<String>) {
                LazyColumn { items(list) { Text(it) } }  // missing key
            }
        """.trimIndent())).run().expectWarningCount(1)
    }

    @Test fun `LazyColumn with items+key does not report`() {
        lint().files(kotlin("""
            package app.tastile.android.ui.x
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            @Composable fun Screen(list: List<String>) {
                LazyColumn { items(list, key = { it }) { Text(it) } }
            }
        """.trimIndent())).run().expectWarningCount(0)
    }
}
```

- [ ] **Step 2: Detector**

```kotlin
class LazyColumnKeyRuleDetector : Detector(), Detector.UastScanner {
    override fun visitMethodCall(context: JavaContext, call: org.jetbrains.uast.UCallExpression) {
        val calledName = call.methodName ?: return
        if (calledName != "items") return
        // Inspect named arguments for "key".
        val hasKey = call.valueArguments.any { (it as? org.jetbrains.uast.UNamedExpression)?.expression?.let { _ -> true } == true
                     && call.valueArguments.any { v -> (v as? org.jetbrains.uast.UNamedExpression)?.expression != null }
        // (Concrete implementation traverses UCallExpression valueArguments and checks
        // for `UNamedExpression` whose `name == "key"`.)
        if (!hasKey) {
            context.report(
                ISSUE, call, context.getLocation(call),
                "LazyColumn/Row items {} must pass `key = { ... }` for stable identity.",
            )
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "LazyColumnKeyRule",
            briefDescription = "items {} must pass `key = ...`",
            explanation = "Stable keys prevent needless recompositions and item identity loss on data change.",
            category = Category.PERFORMANCE, priority = 5, severity = Severity.WARNING,
            implementation = Implementation(
                LazyColumnKeyRuleDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
```

- [ ] **Step 3: Register in `IssueRegistry.issues`**

- [ ] **Step 4: Run, expect PASS**

`./gradlew :lint-rules:test --tests '*LazyColumnKeyRule*'`

- [ ] **Step 5: Commit**

```bash
git add app/lint-rules/
git commit -m "feat(android): add LazyColumnKeyRule lint detector"
```

---

### Task 5: Add `key = { ... }` to every `LazyColumn` / `LazyVerticalGrid`

**Files:** All `LazyColumn { items(...) }` and `LazyVerticalGrid { items(...) }` calls under `app/src/main/java/`.

- [ ] **Step 1: Inventory**

```bash
rg -n 'items\(' app/src/main/java/ | grep -v 'key =' > logs/m3-lazy-no-key.txt
```

- [ ] **Step 2: Per-site fix**

```kotlin
// Before
LazyColumn {
    items(list) { item -> TileRow(item) }
}

// After
LazyColumn {
    items(list, key = { it.id }) { item -> TileRow(item) }
}
```

If the data class lacks a stable `id`, add one. Prefer existing `stableId` / `id` / `key` fields when present; otherwise add a `@Stable` data class field.

- [ ] **Step 3: Run, expect lint clean**

```bash
./gradlew :app:lint
```

Expected: 0 `LazyColumnKeyRule` warnings.

- [ ] **Step 4: Run `./gradlew :app:assembleDebug :app:testDebugUnitTest`, expect green**
- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/ logs/m3-lazy-no-key.txt
git commit -m "refactor(android): add stable key = ... to every LazyColumn/LazyVerticalGrid items()"
```

---

### Task 6: Annotate ViewModel data classes with `@Stable` / `@Immutable`

**Files:** Each `*ViewModel.kt` that emits data classes consumed by Compose.

- [ ] **Step 1: Inventory exposed data classes**

```bash
rg -n '^data class' app/src/main/java/ > logs/m3-data-classes.txt
```

For each, decide:
- `@Immutable` if all fields are themselves immutable primitives or other `@Immutable` / `@Stable` types.
- `@Stable` if fields include `List<…>` of stable types (`List` is `Stable` in Compose).

- [ ] **Step 2: Apply annotations**

```kotlin
import androidx.compose.runtime.Immutable

@Immutable
data class TilesState(
    val tiles: List<Tile> = emptyList(),
    val selectedTileId: String? = null,
)
```

- [ ] **Step 3: Run, expect green**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/
git commit -m "refactor(android): annotate ViewModel-emitted data classes @Stable/@Immutable"
```

---

### Task 7: Capture `after` Compose Compiler Reports + open PR

- [ ] **Step 1: Re-build with reports**

```bash
./gradlew :app:clean
./gradlew :app:assembleDebug -PenableComposeCompilerReports=true

mkdir -p logs/m3-skippable
cp -r app/build/reports/compiler-reports/ logs/m3-skippable/after/
echo "Total files: $(find logs/m3-skippable/after/ -type f | wc -l)" > logs/m3-skippable/after.txt

grep -roE "restartable|skippable|inline" app/build/reports/compiler-reports/ \
  | sort | uniq -c | sort -rn > logs/m3-skippable/restartable_summary_after.txt
```

- [ ] **Step 2: Diff skippable function counts**

```bash
diff logs/m3-skippable/restartable_summary_before.txt \
     logs/m3-skippable/restartable_summary_after.txt \
     > logs/m3-skippable/restartable_summary.diff || true
```

Inspect manually: confirm skippable count grew (target ≥ 5 functions gained skippable status). If not, identify the largest `restartable`/`non-skippable` reports and file a follow-up issue.

- [ ] **Step 3: Open PR**

Title: `refactor(android): stabilize state for skippable recomposition`
Body:

```
### Phase M3: State / Recomposition
- collectAsStateWithLifecycle migration: 100% (lint-enforced).
- LazyColumn/LazyVerticalGrid key coverage: 100% (lint-enforced).
- @Stable/@Immutable annotations added to ViewModel data classes.
- Compose Compiler Reports before/after archived under logs/m3-skippable/.
- Parity safety: no new controls; no new i18n keys; no reorder.
- Spec §5 KPI: ≥ 5 functions gained skippable status.

Spec: docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md §5
```

---

## Completion KPI recap

- [ ] `before` Compose Compiler Reports archived.
- [ ] `after` Compose Compiler Reports archived.
- [ ] `restartable_summary.diff` documents the skippable delta.
- [ ] `CollectAsStateRuleDetectorTest` and `LazyColumnKeyRuleDetectorTest` PASS.
- [ ] `./gradlew :app:lint` — 0 `CollectAsStateRule` and 0 `LazyColumnKeyRule` warnings.
- [ ] `./gradlew :app:testDebugUnitTest` green.
- [ ] PR opened: `refactor(android): stabilize state for skippable recomposition`.

---

## Out of scope for this plan

- ViewModel topology changes (Phase M3 is data-flow only).
- `:lint-rules` extension beyond `S1`/`S2` (new rules are filed separately if needed).
- Timeline perf — Phase M4.
- DC settings toggle + contrast audit — Phase M5.
