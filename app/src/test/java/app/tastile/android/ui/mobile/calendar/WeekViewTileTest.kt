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
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeekViewTileTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tile_emptyColumns_rootIsDisplayed() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 600.dp)) {
                    WeekViewTile(
                        weekStart = LocalDate.of(2026, 7, 13),
                        blocksByDay = emptyMap(),
                        pxPerMin = 1f,
                        zone = ZoneId.of("UTC"),
                        scrollState = rememberScrollState(),
                        onEditEvent = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.onNodeWithTag("week-view-tile").assertIsDisplayed()
    }

    @Test fun tile_oneBlockInOneColumn_rendersChip() = runTest {
        val monday = LocalDate.of(2026, 7, 13)
        val today = LocalDate.now()
        val blocks = mapOf(
            monday to listOf(
                PlacedBlock(
                    id = "evt-1",
                    tileId = "tile-1",
                    sourceKind = 0,
                    title = "Standup",
                    type = "work",
                    status = "active",
                    startMinutes = 9 * 60,
                    endMinutes = 9 * 60 + 30,
                    laneIndex = 0,
                    laneCount = 1,
                ),
            ),
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 1440.dp)) {
                    WeekViewTile(
                        weekStart = monday,
                        blocksByDay = blocks,
                        pxPerMin = 1f,
                        zone = ZoneId.of("UTC"),
                        scrollState = rememberScrollState(),
                        onEditEvent = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.onAllNodesWithTag("week-view-tile-event-chip", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test fun tile_nowIndicator_onTodayOnly() = runTest {
        val today = LocalDate.now()
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 600.dp)) {
                    WeekViewTile(
                        weekStart = weekStart,
                        blocksByDay = emptyMap(),
                        pxPerMin = 1f,
                        zone = ZoneId.of("UTC"),
                        scrollState = rememberScrollState(),
                        onEditEvent = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.onAllNodesWithTag("now-indicator-line", useUnmergedTree = true).assertCountEquals(1)
    }
}
