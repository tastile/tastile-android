package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Status bar + 56dp top bar: the column header must sit at least this far
 * below the WeekView's top edge. Mirrors the production offset used by the
 * Scaffold + MobileTopBar combination.
 */
private val TOP_BAR_OFFSET = 56.dp

@RunWith(RobolectricTestRunner::class)
class WeekViewHeaderTest {
    @get:Rule val compose = createComposeRule()

    private fun renderWeek() {
        compose.setContent {
            MaterialTheme {
                Box(
                    Modifier
                        .requiredSize(800.dp, 1200.dp)
                        .testTag("week-view-test-root"),
                ) {
                    WeekView(
                        items = emptyList(),
                        weekStart = LocalDate.of(2026, 7, 13),
                        zone = ZoneId.of("UTC"),
                        onOpenDay = {},
                        zoom = 1f,
                        onZoomChange = {},
                        onEditEvent = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun pinHeader_rendersSevenDayColumns() = runTest {
        renderWeek()
        compose.onAllNodesWithTag("week-view-pin-header-day-column")
            .assertCountEquals(7)
    }

    @Test fun pinHeader_dowLabelsExist() = runTest {
        renderWeek()
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
            compose.onNodeWithText(it, useUnmergedTree = true).assertExists()
        }
    }

    @Test fun frame_gridLinesCanvasDisplayed() = runTest {
        renderWeek()
        compose.onNodeWithTag("week-view-frame-grid-lines").assertIsDisplayed()
    }

    @Test fun body_extendsBehindTopBar() = runTest {
        renderWeek()
        val rootTop = compose.onNodeWithTag("week-view-test-root")
            .getBoundsInRoot().top
        val bodyTop = compose.onNodeWithTag("week-view-frame-grid-lines")
            .getBoundsInRoot().top
        val offset = (bodyTop - rootTop).value
        assert(offset <= 0.5f) {
            "expected Week body to start at the test root, got ${offset}dp top offset"
        }
    }

    @Test fun tile_sevenColumnsDisplayed() = runTest {
        renderWeek()
        compose.onAllNodesWithTag("week-view-tile-event-column").assertCountEquals(7)
    }

    @Test fun pinHeader_sitsBelowTopBarOffset() = runTest {
        renderWeek()
        // WeekHeaderRow renders one Column per weekday, all sharing this
        // testTag — pick the first sibling (the leftmost Column) to verify
        // the row's y position. All 7 siblings are weighted-and-laid-out
        // identically, so any one of them is representative.
        //
        // `getBoundsInRoot()` returns bounds in the activity-window
        // coordinate space, which Robolectric lays out at a negative
        // offset (status bar + action bar above the ComposeView). Compare
        // the column's top to the test Box's top instead of a literal 0 so
        // the assertion is robust to wherever the test surface lands in
        // the window — what matters is the local offset, which mirrors
        // the production layout where `Modifier.padding(top = TOP_BAR_TOTAL_HEIGHT())`
        // pushes the header below the top bar.
        val rootTop = compose.onNodeWithTag("week-view-test-root")
            .getBoundsInRoot().top
        val colTop = compose.onAllNodesWithTag("week-view-pin-header-day-column")
            .onFirst()
            .getBoundsInRoot().top
        val offsetPx = (colTop - rootTop).value
        val expectedPx: Float = compose.density.run { TOP_BAR_OFFSET.toPx() }
        assert(offsetPx >= expectedPx) {
            "expected column top ≥ $TOP_BAR_OFFSET ($expectedPx px) below test root, got $offsetPx px"
        }
    }
}
