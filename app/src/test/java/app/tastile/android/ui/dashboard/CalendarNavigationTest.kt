package app.tastile.android.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CalendarNavigationTest {
    @Test
    fun scope_navigation_moves_by_the_selected_view_unit() {
        assertEquals(LocalDate.parse("2026-07-08"), shiftCalendarAnchor(LocalDate.parse("2026-07-01"), TimelineScale.Week, 1))
        assertEquals(LocalDate.parse("2026-08-01"), shiftCalendarAnchor(LocalDate.parse("2026-07-01"), TimelineScale.Month, 1))
        assertEquals(LocalDate.parse("2026-07-02"), shiftCalendarAnchor(LocalDate.parse("2026-07-01"), TimelineScale.List, 1))
    }

    @Test
    fun non_scope_modes_pin_navigation_and_calculate_web_windows_from_now() {
        val now = Instant.parse("2026-07-15T12:00:00Z")
        assertFalse(canNavigateCalendar(CalendarMode.Around))
        assertTrue(canNavigateCalendar(CalendarMode.Scope))
        assertEquals(
            Instant.parse("2026-07-15T12:00:00Z") to Instant.parse("2026-07-16T12:00:00Z"),
            calendarRange(LocalDate.parse("2026-01-01"), TimelineScale.Day, CalendarMode.Future, now, ZoneOffset.UTC),
        )
        assertEquals(
            Instant.parse("2026-06-30T12:00:00Z") to Instant.parse("2026-07-31T12:00:00Z"),
            calendarRange(LocalDate.parse("2026-01-01"), TimelineScale.Month, CalendarMode.Around, now, ZoneOffset.UTC),
        )
    }

    @Test
    fun minimum_duration_filters_short_calendar_occurrences_client_side() {
        val items = listOf(
            app.tastile.android.core.CoreTimelineItem("short", title = "Short", type = "work", status = "scheduled", startAt = "2026-07-15T09:00:00Z", endAt = "2026-07-15T09:04:00Z"),
            app.tastile.android.core.CoreTimelineItem("long", title = "Long", type = "work", status = "scheduled", startAt = "2026-07-15T09:00:00Z", endAt = "2026-07-15T09:20:00Z"),
        )

        assertEquals(listOf("long"), filterCalendarByMinimumDuration(items, 5).map { it.id })
    }
}
