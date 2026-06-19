package app.tastile.android.ui.dashboard

import app.tastile.android.data.repository.CalendarProjectionBlockResponse
import app.tastile.android.data.repository.CalendarProjectionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthCalendarScreenTest {

    @Test
    fun buildMonthCalendarCells_buildsSixWeekGridFromProjection() {
        val projection = CalendarProjectionResponse(
            view = "month",
            rangeStart = "2026-04-01T00:00:00Z",
            rangeEnd = "2026-05-01T00:00:00Z",
            gridStart = "2026-03-30T00:00:00Z",
            gridEnd = "2026-05-11T00:00:00Z",
            blocks = listOf(
                CalendarProjectionBlockResponse(
                    tileId = "tile-1",
                    title = "Focus",
                    startAt = "2026-04-08T01:00:00Z",
                    endAt = "2026-04-08T02:00:00Z",
                    semanticRole = "work",
                    allDay = false,
                    ownership = "tastile_owned",
                    editable = true,
                    sourceLabel = "tastile"
                )
            )
        )

        val cells = buildMonthCalendarCells(projection)

        assertEquals(42, cells.size)
        assertEquals("2026-03-30", cells.first().date.toString())
        assertEquals("2026-05-10", cells.last().date.toString())
        assertTrue(cells.any { it.date.toString() == "2026-04-08" && it.titles.contains("Focus") })
    }

    @Test
    fun buildMonthCalendarCells_countsOverflowWhenMoreThanThreeItemsPerDay() {
        val dailyBlocks = (1..5).map { index ->
            CalendarProjectionBlockResponse(
                tileId = "tile-$index",
                title = "Task $index",
                startAt = "2026-04-08T0${index}:00:00Z",
                endAt = "2026-04-08T0${index}:30:00Z",
                semanticRole = "work",
                allDay = false,
                ownership = "tastile_owned",
                editable = true,
                sourceLabel = "tastile"
            )
        }
        val projection = CalendarProjectionResponse(
            view = "month",
            rangeStart = "2026-04-01T00:00:00Z",
            rangeEnd = "2026-05-01T00:00:00Z",
            gridStart = "2026-03-30T00:00:00Z",
            gridEnd = "2026-05-11T00:00:00Z",
            blocks = dailyBlocks
        )

        val target = buildMonthCalendarCells(projection).first { it.date.toString() == "2026-04-08" }

        assertEquals(3, target.titles.size)
        assertEquals(2, target.overflowCount)
    }

    @Test
    fun buildMonthCalendarRows_returnsSixRowsOfSevenDays() {
        val projection = CalendarProjectionResponse(
            view = "month",
            rangeStart = "2026-04-01T00:00:00Z",
            rangeEnd = "2026-05-01T00:00:00Z",
            gridStart = "2026-03-30T00:00:00Z",
            gridEnd = "2026-05-11T00:00:00Z"
        )

        val rows = buildMonthCalendarRows(buildMonthCalendarCells(projection))

        assertEquals(6, rows.size)
        assertTrue(rows.all { it.size == 7 })
    }

    @Test
    fun buildMonthCalendarCells_sortsDailyTitlesByStartTime() {
        val projection = CalendarProjectionResponse(
            view = "month",
            rangeStart = "2026-04-01T00:00:00Z",
            rangeEnd = "2026-05-01T00:00:00Z",
            gridStart = "2026-03-30T00:00:00Z",
            gridEnd = "2026-05-11T00:00:00Z",
            blocks = listOf(
                CalendarProjectionBlockResponse(
                    tileId = "tile-2",
                    title = "Lunch",
                    startAt = "2026-04-08T12:00:00Z",
                    endAt = "2026-04-08T12:30:00Z"
                ),
                CalendarProjectionBlockResponse(
                    tileId = "tile-1",
                    title = "Standup",
                    startAt = "2026-04-08T09:00:00Z",
                    endAt = "2026-04-08T09:30:00Z"
                )
            )
        )

        val target = buildMonthCalendarCells(projection).first { it.date.toString() == "2026-04-08" }

        assertEquals(listOf("Standup", "Lunch"), target.titles)
    }
}
