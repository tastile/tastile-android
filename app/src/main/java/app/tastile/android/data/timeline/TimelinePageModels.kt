package app.tastile.android.data.timeline

import androidx.compose.runtime.Immutable
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.ui.dashboard.TimelineScale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Identifies one locally cached timeline page.
 *
 * The supplied [anchor] is accepted from navigation code as-is, while
 * [normalizedAnchor] and [normalized] provide the canonical page identity.
 * Repository reads always use [normalized], so a non-canonical navigation
 * anchor cannot create a second cache entry for the same page.
 */
data class TimelinePageKey(
    val accountId: String,
    val scopeKey: String,
    val zoneId: ZoneId,
    val scale: TimelineScale,
    val anchor: LocalDate,
) {
    /** The stable anchor used by this scale's page. */
    val normalizedAnchor: LocalDate
        get() = normalizeTimelineAnchor(scale, anchor)

    /** Every local date rendered by this page, in render order. */
    val visibleDates: ImmutableList<LocalDate>
        get() = timelinePageDates(scale, normalizedAnchor)

    /** Alias useful to callers that refer to page membership as dates. */
    val dates: ImmutableList<LocalDate>
        get() = visibleDates

    /** Returns this key with its anchor canonicalized for [scale]. */
    fun normalized(): TimelinePageKey =
        if (anchor == normalizedAnchor) this else copy(anchor = normalizedAnchor)
}

/**
 * Coverage state for one local calendar date. Absence of a Room coverage row
 * is represented explicitly as [NeverFetched], rather than as an empty item
 * list.
 */
enum class TimelineCoverageState {
    NeverFetched,
    Available,
    Stale,
}

/** Immutable, page-scoped read state consumed by the timeline UI. */
@Immutable
data class TimelinePageSnapshot(
    val key: TimelinePageKey,
    val items: ImmutableList<CoreTimelineItem> = persistentListOf(),
    val coverage: ImmutableMap<LocalDate, TimelineCoverageState> = persistentMapOf(),
    val lastUpdatedAt: Instant? = null,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
)

/** Canonical anchor for a timeline page. */
fun normalizeTimelineAnchor(scale: TimelineScale, anchor: LocalDate): LocalDate = when (scale) {
    TimelineScale.Day,
    TimelineScale.List,
    -> anchor

    TimelineScale.Week -> anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    TimelineScale.Month -> anchor.withDayOfMonth(1)
}

/**
 * Returns the exact Monday-first dates rendered by the current timeline
 * calendar surfaces. MonthViewFrame renders a six-week grid, including cells
 * from adjacent months, so its page contains 42 dates.
 */
fun timelinePageDates(scale: TimelineScale, anchor: LocalDate): ImmutableList<LocalDate> {
    val pageAnchor = normalizeTimelineAnchor(scale, anchor)
    val pageStart = when (scale) {
        TimelineScale.Month -> pageAnchor.minusDays((pageAnchor.dayOfWeek.value - 1).toLong())
        else -> pageAnchor
    }
    val dayCount = when (scale) {
        TimelineScale.Day,
        TimelineScale.List,
        -> 1

        TimelineScale.Week -> 7
        TimelineScale.Month -> MONTH_GRID_DAY_COUNT
    }

    return (0 until dayCount)
        .asSequence()
        .map { pageStart.plusDays(it.toLong()) }
        .toPersistentList()
}

/** Convenience form for callers that already hold a page key. */
fun TimelinePageKey.pageDates(): ImmutableList<LocalDate> = visibleDates

private const val MONTH_GRID_DAY_COUNT = 42
