package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
 * Unit tests for the static Day-view frame (Phase v37 / Task 3).
 *
 * DayViewFrame is the layout shell that does NOT depend on `blocks` —
 * its only timeline-independent inputs are `date`, `pxPerMin`, `zone`,
 * `scrollState`, and the `onCreateAt` slot-click callback. We lock down:
 *   1. The frame root renders inside a Box with `testTag("day-view-frame")`.
 *   2. The Canvas drawing the 25 hour grid lines is present and findable
 *      via `testTag("day-view-frame-grid-lines")`.
 *
 * No NowIndicator assertion lives here — that overlay belongs to DayViewTile
 * and is covered by `NowIndicatorTest`.
 */
@RunWith(RobolectricTestRunner::class)
class DayViewFrameTest {
    @get:Rule val compose = createComposeRule()

    @Test fun frame_root_andCanvas_areDisplayed() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 1200.dp)) {
                    DayViewFrame(
                        date = LocalDate.parse("2026-07-17"),
                        pxPerMin = 1f,
                        zone = ZoneId.of("UTC"),
                        scrollState = rememberScrollState(),
                        onCreateAt = { _, _ -> },
                        modifier = Modifier.testTag("day-view-frame"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("day-view-frame").assertIsDisplayed()
        compose
            .onNodeWithTag("day-view-frame-grid-lines", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
