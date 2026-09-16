package app.tastile.android.data.timeline

import app.tastile.android.ui.dashboard.TimelineScale
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelinePageModelsTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun dayKey_containsOneLocalDate() {
        val anchor = LocalDate.of(2026, 9, 16)
        val key = TimelinePageKey(
            accountId = "account-a",
            scopeKey = "scope-a",
            zoneId = zone,
            scale = TimelineScale.Day,
            anchor = anchor,
        )

        assertEquals(anchor, key.normalizedAnchor)
        assertEquals(listOf(anchor), key.visibleDates)
    }

    @Test
    fun weekKey_normalizesToMondayAndContainsSevenDays() {
        val key = TimelinePageKey(
            accountId = "account-a",
            scopeKey = "scope-a",
            zoneId = zone,
            scale = TimelineScale.Week,
            anchor = LocalDate.of(2026, 9, 16),
        )

        assertEquals(LocalDate.of(2026, 9, 14), key.normalizedAnchor)
        assertEquals(
            (0L..6L).map { LocalDate.of(2026, 9, 14).plusDays(it) },
            key.visibleDates,
        )
    }

    @Test
    fun monthKey_containsAllDatesInTheVisibleSixWeekGrid() {
        val key = TimelinePageKey(
            accountId = "account-a",
            scopeKey = "scope-a",
            zoneId = zone,
            scale = TimelineScale.Month,
            anchor = LocalDate.of(2026, 9, 16),
        )

        assertEquals(LocalDate.of(2026, 9, 1), key.normalizedAnchor)
        assertEquals(42, key.visibleDates.size)
        assertEquals(LocalDate.of(2026, 8, 31), key.visibleDates.first())
        assertEquals(LocalDate.of(2026, 10, 11), key.visibleDates.last())
        assertEquals(
            (0L until 42L).map { LocalDate.of(2026, 8, 31).plusDays(it) },
            key.visibleDates,
        )
    }

    @Test
    fun dstSpringAndFallDays_keepLocalDateMembership() {
        val springDate = LocalDate.of(2026, 3, 8)
        val fallDate = LocalDate.of(2026, 11, 1)

        val springKey = TimelinePageKey(
            accountId = "account-a",
            scopeKey = "scope-a",
            zoneId = zone,
            scale = TimelineScale.Day,
            anchor = springDate,
        )
        val fallKey = TimelinePageKey(
            accountId = "account-a",
            scopeKey = "scope-a",
            zoneId = zone,
            scale = TimelineScale.Day,
            anchor = fallDate,
        )

        assertEquals(listOf(springDate), springKey.visibleDates)
        assertEquals(listOf(fallDate), fallKey.visibleDates)
    }
}
