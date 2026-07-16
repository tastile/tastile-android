# Android M3 Optimization — Phase M4 (Drawing Performance — Timeline-first) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate the user-reported jank on `TimelineScreen` (Day / Week / Month). Measure `frameTimeP95` on the Day view before and after; drive an audit of `Modifier.drawBehind` call sites; preserve Timeline v34–v36's Canvas-based grid/scroll architecture. Other surfaces are audited opportunistically with the same checklist.

**Architecture:** A single Macrobenchmark test (added inside `:app`, no separate module) scripts a 2-second pinch + scroll gesture on `TimelineScreen` Day view and reports `frameTimeP95`. Each call to `Modifier.drawBehind` is audited and triaged as `keep` / `replace with Surface(tonalElevation = ...)` / `remove`. Modifier-ordering rules are enforced on touched files.

**Tech Stack:** Kotlin 2.x, Compose Material3 BOM 2024.12.01, AGP 9.2.1, `androidx.benchmark:benchmark-macro-junit4:1.3.3`, `androidx.benchmark:benchmark-junit4:1.3.3`, uiautomator.

**Spec reference:** `tastile-android/docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md` §6 (Phase M4).

**Hotspot reminder:** `TimelineScreen.kt` Day / Week / Month was reported janky on 2026-07-16. Day view is the primary test surface; Week and Month are deferred follow-up unless they also report jank.

---

## File structure

### New files

| Path | Responsibility |
|---|---|
| `app/src/androidTest/java/app/tastile/android/benchmark/TimelineScreenDayScrollBenchmark.kt` | `MacrobenchmarkRule` + uiautomator scripted pinch + scroll; measures `frameTimeP95` |
| `logs/m4-perf/before.txt` and `after.txt` | P95 measurements archived as plain text |
| `logs/m4-drawbehind.md` | Triage table for every `Modifier.drawBehind` call site |

### Modified files

| Path | Change |
|---|---|
| `app/build.gradle.kts` | Add `androidTestImplementation` for benchmark deps; add `androidx.benchmark` plugin if needed for Baseline Profile generation |
| `app/src/main/java/app/tastile/android/ui/dashboard/TimelineScreen.kt` | Targeted tweaks from the audit (e.g. Modifier ordering, `drawBehind` removal where audited) |
| Other `Modifier.drawBehind` sites as identified by `rg` | Triaged per `logs/m4-drawbehind.md` |

---

## Tasks

### Task 1: Add benchmark dependencies to `:app`

**Files:**
- Edit: `app/build.gradle.kts`

- [ ] **Step 1: Add dependencies**

```kotlin
// app/build.gradle.kts inside android { defaultConfig { … } } or dependencies { }
androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.3.3")
androidTestImplementation("androidx.benchmark:benchmark-junit4:1.3.3")
androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
```

(Use the AGP-matched benchmark version. 1.3.3 is conservative for AGP 9.2.x. If a different version is required, surface that to the user.)

- [ ] **Step 2: Allow `androidTestBenchmark` configuration if needed**

Some `androidx.benchmark` macro-rules live in a separate source set. If the build complains, add to `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // benchmark-junit4 looks for tests under androidTestBenchmark source set
        // when benchmark plugin is applied; without the plugin, macro tests
        // live under androidTest.
    }
}
```

(This plan keeps benchmark tests in `androidTest`; the `androidTestBenchmark` source set is only required if the MacroBenchmark plugin is applied, which this plan deliberately avoids to minimize CI graph churn.)

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugAndroidTestSources`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build(android): add androidx.benchmark macro + uiautomator to androidTest"
```

---

### Task 2: Capture `before` Timeline frame P95

**Files:**
- Create: `app/src/androidTest/java/app/tastile/android/benchmark/TimelineScreenDayScrollBenchmark.kt`
- Create: `logs/m4-perf/before.txt` (after running)

- [ ] **Step 1: Write the benchmark skeleton**

```kotlin
package app.tastile.android.benchmark

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileMacrobenchmarkRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import app.tastile.android.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalBaselineProfilesApi::class)
@RunWith(AndroidJUnit4::class)
class TimelineScreenDayScrollBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    @Test fun timelineDayScroll_frameTimeP95() {
        rule.measureRepeated(
            packageName = InstrumentationRegistry.getArguments().getString("targetPackage") ?: "app.tastile.android",
            metrics = listOf(androidx.benchmark.macro.FrameTimingMetric()),
            compilationMode = androidx.benchmark.macro.CompilationMode.DEFAULT,
            startupMode = androidx.benchmark.macro.StartupMode.COLD,
            iterations = 5,
            setupBlock = { device ->
                device.wakeUp()
                device.pressMenu()
            },
            measureBlock = { device: UiDevice ->
                val ctx = InstrumentationRegistry.getInstrumentation().targetContext
                ctx.startActivity(
                    android.content.Intent(ctx, MainActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                device.waitForIdle()
                // Navigate to Timeline Day if not already there; assume launching
                // from a saved state lands on Day. Adjust as the app's launcher does.
                device.findObject(By.res("timeline_day_view"))
                // Two-finger pinch + scroll, 2 seconds.
                device.executeShellCommand("input swipe 500 500 700 700 1000")
                device.executeShellCommand("input swipe 500 500 300 1500 1000")
                device.waitForIdle()
            },
        )
    }
}
```

(The exact gesture sequence and resource ids depend on the app's actual surfaces; they are filled in by the executor from the live `TimelineScreen.kt` state.)

- [ ] **Step 2: Run on a connected device/emulator**

```bash
./gradlew :app:connectedBenchmarkAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.targetPackage=app.tastile.android \
    -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.benchmark.TimelineScreenDayScrollBenchmark
```

Expected: 5 iterations complete; `frameTimeP95` reported under `app/build/reports/benchmark/`.

- [ ] **Step 3: Archive**

```bash
mkdir -p logs/m4-perf
grep -A3 "FrameTimingMetric\|P95\|median" app/build/reports/benchmark/*.txt \
    | head -200 > logs/m4-perf/before.txt
```

(Exact output format depends on the benchmark plugin version. The pattern above is best-effort; verify manually that `frameTimeP95` is captured. If the output format differs, save the full benchmark report and link to it.)

- [ ] **Step 4: Commit (artefacts only)**

```bash
git add -f logs/m4-perf/before.txt \
        app/src/androidTest/java/app/tastile/android/benchmark/TimelineScreenDayScrollBenchmark.kt
git commit -m "perf(android): capture Timeline Day frame P95 baseline (M4 before)"
```

---

### Task 3: `Modifier.drawBehind` audit + Modifier-ordering sweep

**Files:**
- Create: `logs/m4-drawbehind.md`
- Edit: Each file surfaced by the audit with a `keep` / `replace` / `remove` decision

- [ ] **Step 1: Inventory**

```bash
rg -n 'Modifier\.drawBehind|\.drawBehind\(' app/src/main/java/ > logs/m4-drawbehind-raw.txt
wc -l logs/m4-drawbehind-raw.txt
```

- [ ] **Step 2: Triage table**

For each result, fill the audit:

| File:Line | Reason | Verdict | Author | Sign-off |
|---|---|---|---|---|
| `ui/dashboard/TimelineScreen.kt:42` | draws hour-grid lines | `keep` | <name> | <date> |
| `ui/dashboard/components/SectionBlock.kt:11` | decorative surface tint | `replace with Surface(tonalElevation = 1.dp)` | <name> | <date> |
| `ui/mobile/components/Foo.kt:23` | ad-hoc ripple-only | `remove` | <name> | <date> |

Save the table to `logs/m4-drawbehind.md`.

- [ ] **Step 3: Per-row action**

For `replace` verdicts, refactor the call site:

```kotlin
// Before
Box(modifier = Modifier.drawBehind { /* manual surface tint */ })

// After
Surface(tonalElevation = 1.dp, modifier = Modifier) { /* contents */ }
```

For `remove` verdicts, delete the `drawBehind` modifier chain. If it was important, file a follow-up — do not silently drop functionality.

- [ ] **Step 4: Modifier-ordering sweep**

For every `Modifier.padding(...).background(...)` in the touched files, reorder to `.padding().background()`. Most reorders are safe; if a layout shifts, leave a code comment and surface in the PR.

- [ ] **Step 5: Run `./gradlew :app:assembleDebug :app:testDebugUnitTest`, expect green**
- [ ] **Step 6: Commit per audit batch**

```bash
git add app/src/main/java/ logs/m4-drawbehind.md logs/m4-drawbehind-raw.txt
git commit -m "perf(android): Modifier.drawBehind audit + reorder sweep"
```

---

### Task 4: Targeted Timeline tweaks (Day view)

**Files:**
- Edit: `app/src/main/java/app/tastile/android/ui/dashboard/TimelineScreen.kt`

This task is intentionally open-ended — the precise tweak depends on what the `before` P95 reveals. Common wins to try first:

- [ ] **Step 1: Reduce `Modifier.drawBehind` work inside TimelineScreen**

If the grid-line Canvas or v34–v36 day-scroll setup allocates per-frame, hoist to `remember`. Example:

```kotlin
// Before (per-frame paint closure)
val painter = Modifier.drawBehind { /* paint lines */ }

// After (hoisted)
val painter = remember { Modifier.drawBehind { /* paint lines */ } }
```

If no `drawBehind` exists in TimelineScreen, skip this step.

- [ ] **Step 2: Replace `LazyColumn` items without `key` if any remain** (Phase M3 lint should have caught these, but verify locally).

- [ ] **Step 3: Verify scroll path**

Re-read TimelineScreen v36 (the `detectTransformGestures(panZoomLock=false)` scroll path from memory). Confirm `Modifier.verticalScroll(scrollState)` (or the equivalent) wraps `Canvas` so grid lines / labels / blocks translate together. If not, fix.

- [ ] **Step 4: Re-run benchmark to confirm improvement**

```bash
./gradlew :app:connectedBenchmarkAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.targetPackage=app.tastile.android \
    -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.benchmark.TimelineScreenDayScrollBenchmark
```

Expected: `frameTimeP95` lower than `before.txt`. Target ≥ 10% improvement, or absolute < 16.7 ms (60 fps median).

If no improvement, file a follow-up issue with the diff and stop; do not expand scope.

- [ ] **Step 5: Archive `after.txt`**

```bash
grep -A3 "FrameTimingMetric\|P95\|median" app/build/reports/benchmark/*.txt \
    | head -200 > logs/m4-perf/after.txt
diff logs/m4-perf/before.txt logs/m4-perf/after.txt > logs/m4-perf.diff || true
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tastile/android/ui/dashboard/TimelineScreen.kt \
        logs/m4-perf/after.txt logs/m4-perf.diff
git commit -m "perf(android): TimelineScreen tweaks from P95 audit"
```

---

### Task 5: Compose Compiler Reports re-snapshot + open PR

- [ ] **Step 1: Re-build with reports**

```bash
./gradlew :app:clean
./gradlew :app:assembleDebug -PenableComposeCompilerReports=true
mkdir -p logs/m4-skippable
cp -r app/build/reports/compiler-reports/ logs/m4-skippable/post-m4/
```

- [ ] **Step 2: PR**

Title: `perf(android): Timeline Macrobenchmark + drawBehind audit`
Body:

```
### Phase M4: Drawing Performance (Timeline-first)
- Frame P95 measurement on Timeline Day view.
- logs/m4-perf/{before,after}.txt archived.
- Modifier.drawBehind audit + Modifier-ordering sweep; logs/m4-drawbehind.md.
- Targeted TimelineScreen tweaks per the audit.
- Parity safety: no new controls; no new i18n keys; no reorder.
- Spec §6 KPI: ≥ 10% frameTimeP95 improvement, or absolute < 16.7 ms.

Spec: docs/superpowers/specs/2026-07-16-tastile-android-m3-optimization-design.md §6
```

---

## Completion KPI recap

- [ ] `frameTimeP95` baseline + post-improvement archived under `logs/m4-perf/`.
- [ ] `logs/m4-drawbehind.md` signed off (every call site triaged).
- [ ] Modifier-ordering sweep complete on touched files.
- [ ] `./gradlew :app:assembleDebug :app:testDebugUnitTest` green.
- [ ] PR opened: `perf(android): Timeline Macrobenchmark + drawBehind audit`.

---

## Out of scope for this plan

- Week / Month view (deferred; user only reported Day jank).
- `androidx.profileinstaller` Baseline Profile generation as a separate PR.
- General surface optimization (Phase M4 audits opportunistic, not exhaustive).
- Accessibility semantics + DC toggle — Phase M5.

If a Task above finds that Timeline tweaks alone don't reach the KPI, file a follow-up issue describing the remaining cause and stop. Do not expand the PR to unrelated areas.
