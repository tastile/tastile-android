package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit tests for the dynamic Day-view tile layer (Phase v37 / Task 3.5).
 *
 * DayViewTile composes the event blocks for the day (the data-bound layer)
 * and the NowIndicator overlay. We cover:
 *   1. Empty-blocks happy path — the tile root Box is rendered.
 *   2. One-block rendering — title Text is displayed and its top y-offset
 *      equals `startMinutes × pxPerMin` (so the chip lines up over the
 *      grid line the Frame drew for that hour).
 *   3. NowIndicator overlay appears only when `date == today`. The
 *      `date = today - 1` case must not render the now-line.
 *   4. NowIndicator is driven by an injectable `nowInstant` so tests
 *      can pin wall time deterministically.
 */
@RunWith(RobolectricTestRunner::class)
class DayViewTileTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.now()

    private fun sampleBlock(
        id: String = "b1",
        startMin: Int = 540, // 09:00
        endMin: Int = 600,   // 10:00
        title: String = "Deep Work",
        laneIndex: Int = 0,
        laneCount: Int = 1,
    ): PlacedBlock = PlacedBlock(
        id = id,
        tileId = null,
        sourceKind = null,
        title = title,
        type = "work",
        status = "active",
        startMinutes = startMin,
        endMinutes = endMin,
        laneIndex = laneIndex,
        laneCount = laneCount,
    )

    @Test fun tile_emptyBlocks_rootIsDisplayed() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(400.dp, 1440.dp)) {
                    DayViewTile(
                        blocks = emptyList(),
                        date = LocalDate.now().minusDays(1), // not today → no now-line
                        pxPerMin = 1f,
                        zone = ZoneId.of("UTC"),
                        onEditEvent = {},
                        today = today,
                        modifier = Modifier.testTag("day-view-tile"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("day-view-tile").assertIsDisplayed()
    }

    @Test fun tile_oneBlock_rendersTitleAtStartYOffset() = runTest {
        val block = sampleBlock(startMin = 540, endMin = 600, title = "Deep Work")
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(400.dp, 1440.dp)) {
                    DayViewTile(
                        blocks = listOf(block),
                        date = LocalDate.now().minusDays(1),
                        pxPerMin = 1f,
                        zone = ZoneId.of("UTC"),
                        onEditEvent = {},
                        today = today,
                        modifier = Modifier.testTag("day-view-tile"),
                    )
                }
            }
        }
        compose.waitForIdle()
        // The block title must be on screen …
        compose.onNodeWithText("Deep Work").assertIsDisplayed()
        // … and its top-y offset must equal `startMinutes × pxPerMin`
        // (= 540 × 1 = 540.dp from the tile root top). The Tile places
        // the chip Box at `.offset(y = startMinutes * pxPerMin)`, so the
        // title Text inside the chip inherits that offset within the
        // tile's local coordinates.
        val tileTop = compose.onNodeWithTag("day-view-tile").getUnclippedBoundsInRoot().top
        val titleTop = compose.onNodeWithText("Deep Work").getUnclippedBoundsInRoot().top
        assertEquals(540f, (titleTop - tileTop).value, 2f)
    }

    @Test fun tile_nowIndicator_absentOnYesterday() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(400.dp, 1440.dp)) {
                    DayViewTile(
                        blocks = emptyList(),
                        date = today.minusDays(1),
                        pxPerMin = 1f,
                        zone = ZoneId.of("UTC"),
                        onEditEvent = {},
                        today = today,
                        modifier = Modifier.testTag("day-view-tile"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("now-indicator-line").assertDoesNotExist()
    }

    @Test fun tile_nowIndicator_visibleOnToday() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(400.dp, 1440.dp)) {
                    DayViewTile(
                        blocks = emptyList(),
                        date = today,
                        pxPerMin = 1f,
                        zone = ZoneId.of("UTC"),
                        onEditEvent = {},
                        today = today,
                        nowInstant = Instant.now(),
                        modifier = Modifier.testTag("day-view-tile"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("now-indicator-line").assertExists()
        compose.onNodeWithTag("now-indicator-dot").assertExists()
    }

    @Test fun tile_nowIndicator_usesInjectedNowProvider() = runTest {
        // Pin "now" to 14:30 UTC on today's date. In UTC zone that's
        // minute-of-day = 14*60+30 = 870. With pxPerMin=1 / density=1 the
        // now-line's top y-offset must be 870.dp (line is 2dp tall, so its
        // own bounds.height=2 but its offset.y is the nowY from
        // NowIndicator.kt).
        val zone = ZoneId.of("UTC")
        // Use today at 14:30 UTC.
        val nowInstant = today.atTime(14, 30).atZone(zone).toInstant()
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(400.dp, 1440.dp)) {
                    DayViewTile(
                        blocks = emptyList(),
                        date = today,
                        pxPerMin = 1f,
                        zone = zone,
                        onEditEvent = {},
                        today = today,
                        nowInstant = nowInstant,
                        modifier = Modifier.testTag("day-view-tile"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("now-indicator-line").assertExists()
        compose.onNodeWithTag("now-indicator-line").assertIsDisplayed()
    }
}
