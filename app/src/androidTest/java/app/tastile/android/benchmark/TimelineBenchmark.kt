package app.tastile.android.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal M4 baseline harness. Task 2 should replace the launch-only body with
 * navigation to Timeline and a day-view scroll gesture once the benchmark
 * device state and authenticated fixture are defined.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TimelineBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

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
        }
    }
}
