package app.tastile.android.data.timeline

import app.tastile.android.ui.dashboard.TimelineScale
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    @Test
    fun scopeFingerprint_normalizesOwnerOrderAndWhitespace() {
        val first = timelineScopeFingerprint(
            ownerIds = listOf(" owner-b ", "owner-a", "owner-b"),
        )
        val second = timelineScopeFingerprint(ownerIds = listOf("owner-a", "owner-b"))

        assertEquals(second, first)
        assertEquals(
            listOf("owner-a", "owner-b"),
            normalizeTimelineOwnerIds(listOf(" owner-b ", "owner-a", "owner-b")),
        )
    }

    @Test
    fun scopeFingerprint_changesWhenOwnerOrIncludeChanges() {
        val ownerA = timelineScopeFingerprint(ownerIds = listOf("owner-a"))
        val ownerB = timelineScopeFingerprint(ownerIds = listOf("owner-b"))
        val withLabels = timelineScopeFingerprint(
            ownerIds = listOf("owner-a"),
            include = TimelineIncludeFlags(labels = true),
        )

        assertNotEquals(ownerA, ownerB)
        assertNotEquals(ownerA, withLabels)
    }

    @Test
    fun forScope_keyCarriesFingerprintAndNormalizedOwnerIds() {
        val key = TimelinePageKey.forScope(
            accountId = "account-a",
            ownerIds = listOf(" owner-b ", "owner-a"),
            zoneId = zone,
            scale = TimelineScale.Day,
            anchor = LocalDate.of(2026, 9, 16),
        )

        assertEquals(listOf("owner-a", "owner-b"), key.ownerIds)
        assertEquals(timelineScopeFingerprint(key.ownerIds), key.scopeFingerprint)
        assertEquals(key.scopeFingerprint, key.scopeKey)
    }
}
