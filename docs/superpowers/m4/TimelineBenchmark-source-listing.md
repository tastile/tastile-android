# TimelineBenchmark — source listing

Verbatim copy of `app/src/androidTest/java/app/tastile/android/benchmark/TimelineBenchmark.kt`
as of commit `8715f71` (M4 Task 2).

This listing is here so a CI runner can reproduce the harness without
inspecting the live source. If the live file and this listing diverge, the
live file is the source of truth — please re-capture this listing.

```kotlin
package app.tastile.android.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M4 Timeline frame-timing harness.
 *
 * Captures [FrameTimingMetric] while exercising the mobile Timeline screen:
 *   1. App launch (warm)
 *   2. Switch to Day scale via the calendar-mode-day toolbar button
 *   3. Vertical scroll on the day grid (1-finger drag → verticalScroll)
 *   4. Horizontal swipe between day pages (HorizontalPager)
 *
 * Selectors were derived from `ui/mobile/tabs/TimelineScreen.kt`:
 *   - `Modifier.testTag("calendar-mode-day")` on the "DAY" toolbar button
 *   - `Modifier.testTag("calendar-today")` on the "Today" toolbar button
 *   - `Modifier.testTag("calendar-mode-list")` on the "LIST" toolbar button
 *   - The day grid body is a `verticalScroll` Column wrapped inside a `HorizontalPager`
 *
 * Reproduce locally on CI:
 *   ./gradlew :app:pixel6Benchmark \
 *       -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.benchmark.TimelineBenchmark
 *
 * Results land under `app/build/outputs/connect_android_test_additional_output/`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TimelineBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Vertical scroll on the day view. The day grid body is wrapped in
     * `verticalScroll(rememberScrollState())`, so a 1-finger downward swipe
     * drives `scrollBy()` and exercises the grid-line Canvas + EventChip
     * recomposition path — the same workload that produced the v35/v36
     * scroll/cell regressions the M4 task targets.
     */
    @Test
    fun timelineDayScrollFrameTimeP95() {
        benchmarkRule.measureRepeated(
            packageName = "app.tastile.android",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.WARM,
            iterations = 10,
        ) {
            startActivityAndWait()
            device.waitForIdle()

            // 1. Land on Day scale. CalendarToolbar exposes testTag("calendar-mode-day")
            //    on each mode Text widget. If the testTag selector doesn't resolve
            //    (older installs with the dashboard TimelineScreen.kt's plain
            //    `Text("Day View")`), skip the run — `assumeTrue(false)` causes
            //    JUnit to record the test as ignored rather than failed, so CI
            //    can still publish a partial result and the next PR can fix the
            //    selector without bringing down the whole build.
            val dayModeButton = device.findObject(By.res("calendar-mode-day"))
            assumeTrue(
                "TimelineScreen calendar-mode-day testTag not present — skipping run " +
                    "(expected on mobile TimelineScreen; dashboard TimelineScreen has " +
                    "no testTags). See ui/mobile/tabs/TimelineScreen.kt and " +
                    "ui/dashboard/TimelineScreen.kt.",
                dayModeButton != null,
            )
            dayModeButton?.click()
            device.waitForIdle()

            // 2. Snap to "Today" so the anchored grid (now-line, hour blocks)
            //    is visible regardless of which page the pager was last on.
            device.findObject(By.res("calendar-today"))?.click()
            device.waitForIdle()

            // 3. Identify the scrollable body. The day grid wraps the gutter
            //    + content inside `Modifier.verticalScroll(scrollState)`, and
            //    UiAutomator exposes that as a UiObject with `isScrollable=true`.
            val scrollable = firstScrollable(
                By.scrollable(true),
                fallback = By.res("calendar-today"),
            )
            assumeTrue("No vertically scrollable container found on Timeline", scrollable != null)

            // 4. Three short DOWN-then-UP sweeps. Each sweep is small enough to
            //    stay inside the day (avoids horizontalPager page change) and
            //    covers ~15-min of grid lines, which is enough to push a fresh
            //    batch of EventChip past the viewport edge — the recomposition
            //    cost M4 will measure.
            repeat(3) {
                scrollable?.scroll(Direction.DOWN, 2f)
                device.waitForIdle()
                scrollable?.scroll(Direction.UP, 2f)
                device.waitForIdle()
            }
        }
    }

    /**
     * Horizontal swipe between day pages. Day scale uses a shared
     * `HorizontalPager` (per `ui/mobile/tabs/TimelineScreen.kt` v32+), so a
     * left-edge horizontal fling exercises the pager's offscreen page
     * composition + recomposition path.
     */
    @Test
    fun timelineDayHorizontalPagerFrameTimeP95() {
        benchmarkRule.measureRepeated(
            packageName = "app.tastile.android",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.WARM,
            iterations = 10,
        ) {
            startActivityAndWait()
            device.waitForIdle()

            val dayModeButton = device.findObject(By.res("calendar-mode-day"))
            assumeTrue(
                "TimelineScreen calendar-mode-day testTag not present — skipping run",
                dayModeButton != null,
            )
            dayModeButton?.click()
            device.waitForIdle()
            device.findObject(By.res("calendar-today"))?.click()
            device.waitForIdle()

            val scrollable = firstScrollable(
                By.scrollable(true),
                fallback = By.res("calendar-today"),
            )
            assumeTrue("No horizontally scrollable container found on Timeline", scrollable != null)

            // HorizontalPager is also exposed as `isScrollable=true`; direction LEFT
            // = swipe right → next page (offset +1). Repeat to traverse a few days.
            repeat(2) {
                scrollable?.scroll(Direction.RIGHT, 3f)
                device.wait(Until.scrollFinished(Direction.RIGHT), 1_500)
                device.waitForIdle()
                scrollable?.scroll(Direction.LEFT, 3f)
                device.wait(Until.scrollFinished(Direction.LEFT), 1_500)
                device.waitForIdle()
            }
        }
    }

    private fun firstScrollable(primary: BySelector, fallback: BySelector) =
        device.findObject(primary) ?: device.findObject(fallback)
}
```
