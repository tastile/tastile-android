package app.tastile.android.ui.dashboard

import app.tastile.android.core.CoreTimelineItem
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Calendar display modes match the Web Scope/Around/Future selector. */
enum class CalendarMode { Scope, Around, Future }

fun canNavigateCalendar(mode: CalendarMode): Boolean = mode == CalendarMode.Scope

fun shiftCalendarAnchor(anchor: LocalDate, scale: TimelineScale, delta: Long): LocalDate = when (scale) {
    TimelineScale.Day, TimelineScale.List -> anchor.plusDays(delta)
    TimelineScale.Week -> anchor.plusWeeks(delta)
    TimelineScale.Month -> anchor.plusMonths(delta)
}

/**
 * Mirrors `tastile-web/src/lib/calendar/layout.ts#getModeRange` using the
 * device zone for Scope and an injected clock for Around/Future.
 */
internal fun calendarRange(
    anchor: LocalDate,
    scale: TimelineScale,
    mode: CalendarMode,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Pair<Instant, Instant> {
    if (scale == TimelineScale.List) {
        return now.minus(Duration.ofDays(14)) to now.plus(Duration.ofDays(17))
    }
    val day = anchor.atStartOfDay(zone).toInstant()
    return when (mode) {
        CalendarMode.Around -> when (scale) {
            TimelineScale.Day -> now.minus(Duration.ofHours(12)) to now.plus(Duration.ofHours(12))
            TimelineScale.Week -> now.minus(Duration.ofDays(3)) to now.plus(Duration.ofDays(4))
            TimelineScale.Month -> now.minus(Duration.ofDays(15)) to now.plus(Duration.ofDays(16))
            TimelineScale.List -> error("handled above")
        }
        CalendarMode.Future -> when (scale) {
            TimelineScale.Day -> now to now.plus(Duration.ofDays(1))
            TimelineScale.Week -> now to now.plus(Duration.ofDays(7))
            TimelineScale.Month -> now to now.plus(Duration.ofDays(31))
            TimelineScale.List -> error("handled above")
        }
        CalendarMode.Scope -> when (scale) {
            TimelineScale.Day -> day to day.plus(Duration.ofDays(1))
            TimelineScale.Week -> {
                val monday = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong()).atStartOfDay(zone).toInstant()
                monday to monday.plus(Duration.ofDays(7))
            }
            TimelineScale.Month -> {
                val month = anchor.withDayOfMonth(1).atStartOfDay(zone).toInstant()
                month to anchor.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant()
            }
            TimelineScale.List -> error("handled above")
        }
    }
}

/** The Web proxy applies min_minutes after reading v1; Android does the same locally. */
internal fun filterCalendarByMinimumDuration(
    items: List<CoreTimelineItem>,
    minimumMinutes: Int,
): List<CoreTimelineItem> {
    if (minimumMinutes <= 0) return items
    return items.filter { item ->
        val start = runCatching { Instant.parse(item.startAt) }.getOrNull() ?: return@filter false
        val end = item.endAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@filter false
        Duration.between(start, end).toMinutes() >= minimumMinutes
    }
}
