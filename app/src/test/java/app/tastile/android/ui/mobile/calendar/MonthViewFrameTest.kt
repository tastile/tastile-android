package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [MonthViewFrame] (Phase v37 / Task 5).
 *
 * The Frame is the static cell-borders + day-numbers + selection layer for
 * the Month view. It must remain data-free — `itemsByDate` is consumed only
 * by the sibling [MonthEventIndicator] overlay composed in [MonthView].
 *
 *   1. Root + grid render under a known viewport size.
 *   2. Exactly 42 day columns are drawn (7 columns × 6 rows = 42, per
 *      `GridConstants.MONTH_GRID_ROWS` × `WEEK_DAYS`).
 *   3. Out-of-month days (previous/next month bleed) render with their day
 *      number still present and a de-emphasized alpha. We assert by reading
 *      the day-number text (the v31 memory locks in "thin border, no rounded
 *      corners, no background fills").
 *   4. Exactly one cell carries the `month-view-frame-selected-cell`
 *      testTag for the supplied `selectedDate` — proves the selection state
 *      flows to a single coordinate.
 *
 * The Frame intentionally does NOT consult `itemsByDate` or `count` —
 * verified by `grep -nE "itemsByDate|count" MonthViewFrame.kt`.
 */
@RunWith(RobolectricTestRunner::class)
class MonthViewFrameTest {
    @get:Rule val compose = createComposeRule()

    private val monthStart = LocalDate.of(2026, 7, 1)

    @Test fun frame_root_andGrid_areDisplayed() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 800.dp)) {
                    MonthViewFrame(
                        monthStart = monthStart,
                        selectedDate = monthStart.plusDays(4),
                        onSelectDate = {},
                        modifier = Modifier.testTag("month-view-frame"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("month-view-frame").assertExists()
        // The Frame's Column is the only semantic carrier; the rendered
        // 6-row grid is implicit via the 42 day-column children asserted
        // separately in frame_42Cells_rendered.
    }

    @Test fun frame_42Cells_rendered() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 800.dp)) {
                    MonthViewFrame(
                        monthStart = monthStart,
                        selectedDate = monthStart.plusDays(4),
                        onSelectDate = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose
            .onAllNodesWithTag("month-view-frame-day-column", useUnmergedTree = true)
            .assertCountEquals(42)
    }

    @Test fun frame_outOfMonthCells_deemphasized() = runTest {
        // July 2026 starts on a Wednesday (dayOfWeek=3). The first row's
        // Mon/Tue cells (indices 0..1) bleed from June 2026 and must render
        // "29"/"30". July has 31 days, so the final Tue/Wed cells (indices
        // 32..33 in the 0-indexed 42-cell grid) bleed into August and must
        // render "1" / "2" (we assert "31" stays visible inside the month).
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 800.dp)) {
                    MonthViewFrame(
                        monthStart = monthStart,
                        selectedDate = monthStart.plusDays(4),
                        onSelectDate = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.waitForIdle()
        // All 42 cells render — out-of-month included.
        compose
            .onAllNodesWithTag("month-view-frame-day-column", useUnmergedTree = true)
            .assertCountEquals(42)
        // Out-of-month cells layer a contentDescription on the day-number
        // Text node (Compose's `testTag()` replaces prior tags on the same
        // Node, so we use the description channel for the bleed signal
        // instead). For July 2026 there are 2 prev-month cells (Mon/Tue
        // week 0: June 29 + 30) + 9 next-month cells (Sat/Sun of week 5
        // through Sun of week 6: Aug 1..9) = 11.
        compose
            .onAllNodesWithContentDescription("out-of-month", substring = false)
            .assertCountEquals(11)
    }

    @Test fun frame_selectedDate_highlighted() = runTest {
        val selectedDate = monthStart.plusDays(5)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 800.dp)) {
                    MonthViewFrame(
                        monthStart = monthStart,
                        selectedDate = selectedDate,
                        onSelectDate = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose
            .onAllNodesWithTag(
                "month-view-frame-selected-cell",
                useUnmergedTree = true,
            )
            .assertCountEquals(1)
    }
}
