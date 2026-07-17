package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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

@RunWith(RobolectricTestRunner::class)
class WeekViewHeaderTest {
    @get:Rule val compose = createComposeRule()

    private fun renderWeek() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 1200.dp)) {
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

    @Test fun tile_sevenColumnsDisplayed() = runTest {
        renderWeek()
        compose.onAllNodesWithTag("week-view-tile-event-column").assertCountEquals(7)
    }
}
