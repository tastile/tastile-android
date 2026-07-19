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
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.tabs.TimelineScreen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Two layers are pinned:
 *   1. [frame_rendersImmediately_whenTimelineEmpty] mounts [DayView] with
 *      zero blocks — the Frame/Tile split's own invariant.
 *   2. [frame_rendersImmediately_whenScreenLoadingWithEmptyTimeline] mounts
 *      the real `TimelineScreen` with a mocked VM in the initial-load state
 *      (`loading = true`, empty timeline, `scale = Day`). This is the layer
 *      that regressed: a `when` branch in `TimelineScreen` was swapping the
 *      whole Day pager for a full-screen loading wheel, defeating the split
 *      at the screen level even though [DayView] itself was frame-first.
 *      `TimelineScreen` takes its VMs as parameters, so a mockk double
 *      sidesteps Hilt entirely — no auth/profile harness needed.
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

    @Test fun frame_rendersImmediately_whenScreenLoadingWithEmptyTimeline() = runTest {
        // Initial-load state: a refresh is in flight (`loading = true`) and
        // no timeline data has arrived yet. The Frame must still render.
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.timeline } returns MutableStateFlow<List<CoreTimelineItem>>(emptyList())
        every { vm.loading } returns MutableStateFlow(true)
        every { vm.selectedDay } returns MutableStateFlow(pageDay)
        every { vm.scale } returns MutableStateFlow(TimelineScale.Day)
        val overlayVm = mockk<OverlayViewModel>(relaxed = true)

        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(400.dp, 1440.dp)) {
                    TimelineScreen(viewModel = vm, overlay = overlayVm)
                }
            }
        }
        compose.waitForIdle()

        // The Day pager (→ DayView → DayViewFrame) must be on screen even
        // while loading. If the screen-level loading-wheel gate comes back,
        // the pager is replaced by a spinner and this node disappears.
        compose
            .onNodeWithTag("day-view-frame-grid-lines", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
