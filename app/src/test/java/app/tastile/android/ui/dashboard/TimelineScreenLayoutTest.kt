package app.tastile.android.ui.dashboard

import app.tastile.android.core.CoreTimelineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class TimelineScreenLayoutTest {

    @Test
    fun parseInstant_parsesIsoWithOffset() {
        val parsed = parseInstant("2026-04-05T09:00:00+09:00")
        assertNotNull(parsed)
        assertEquals("2026-04-05T00:00:00Z", parsed?.toString())
    }

    @Test
    fun arrangeVisibleBlocks_keepsItemsOutsideCurrentDay() {
        val now = Instant.parse("2026-04-05T12:00:00Z")
        val items = listOf(
            CoreTimelineItem(
                id = "a",
                tileId = "tile-a",
                title = "Overnight",
                type = "work",
                status = "active",
                startAt = "2026-04-04T23:30:00Z",
                endAt = "2026-04-05T00:30:00Z"
            ),
            CoreTimelineItem(
                id = "b",
                tileId = "tile-b",
                title = "Tomorrow",
                type = "work",
                status = "scheduled",
                startAt = "2026-04-06T01:00:00Z",
                endAt = "2026-04-06T02:00:00Z"
            )
        )

        val blocks = arrangeVisibleBlocks(items, now, ZoneOffset.UTC)

        assertEquals(2, blocks.size)
    }

    @Test
    fun arrangeVisibleBlocks_assignsDifferentLanesForOverlap() {
        val now = Instant.parse("2026-04-05T12:00:00Z")
        val items = listOf(
            CoreTimelineItem(
                id = "a",
                tileId = "tile-a",
                title = "A",
                type = "work",
                status = "active",
                startAt = "2026-04-05T10:00:00Z",
                endAt = "2026-04-05T11:00:00Z"
            ),
            CoreTimelineItem(
                id = "b",
                tileId = "tile-b",
                title = "B",
                type = "work",
                status = "scheduled",
                startAt = "2026-04-05T10:30:00Z",
                endAt = "2026-04-05T11:30:00Z"
            )
        )

        val blocks = arrangeVisibleBlocks(items, now, ZoneOffset.UTC)

        assertEquals(2, blocks.size)
        assertEquals(2, blocks[0].columnCount)
        assertEquals(2, blocks[1].columnCount)
        assertEquals(setOf(0, 1), blocks.map { it.columnIndex }.toSet())
    }
}

