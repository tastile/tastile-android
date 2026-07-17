package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId

/**
 * Frame-first rendering regression test (v37 / T7 — design §3 row 5).
 *
 * Pins the "Frame renders even when the timeline list is empty" invariant
 * that motivates the entire Frame / Tile split. Mounts the
 * [DayView] composable with **zero blocks**, asserts the Frame's grid-line
 * Canvas (testTag `day-view-frame-grid-lines`) is displayed.
 *
 * The test is scoped to [DayView] rather than the full `TimelineScreen`
 * because the latter pulls Hilt-injected VMs + auth/profile state —
 * a separate concern with its own test harness. The Frame-first property
 * is owned by the split between [DayViewFrame] and [DayViewTile]; if
 * that boundary ever gets crossed (e.g. someone re-introduces a
 * `LaunchedEffect { timeline.collect { ... } }` that gates the Frame
 * on data arrival), the assertion below will start failing.
 *
 * Matches the sibling Frame/Tile JVM tests: `@RunWith(Robolectric)`.
 */
@RunWith(RobolectricTestRunner::class)
class TimelineScreenLoadingTest {
    @get:Rule val compose = createComposeRule()

    private val zone = ZoneId.of("UTC")
    private val pageDay = LocalDate.now().minusDays(1) // not today → no now-line noise

    @Test fun frame_rendersImmediately_whenTimelineEmpty() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(400.dp, 1440.dp)) {
                    DayView(
                        date = pageDay,
                        zoom = 1f,
                        blocks = emptyList(),
                        zone = zone,
                        onCreateAt = { _, _ -> },
                        onEditEvent = {},
                        modifier = Modifier.testTag("timeline-loading-day-view"),
                    )
                }
            }
        }
        compose.waitForIdle()

        // The Frame's grid-line Canvas must be present (proving the
        // Frame rendered without waiting for timeline data). This is the
        // core v37 invariant the design doc pins as the Frame / Tile
        // split's primary motivation.
        compose
            .onNodeWithTag("day-view-frame-grid-lines", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
