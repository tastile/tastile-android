package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.ui.mobile.tabs.topBarTotalHeight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Top-level Month view wrapper (Phase v37 / Task 5).
 *
 * Owns the 7×6 = 42-cell layout, the day-of-week header row, and the
 * `itemsByDate` aggregation. The Frame ([MonthViewFrame]) is data-free;
 * this wrapper resolves per-cell `count` and forwards it to the sibling
 * [MonthEventIndicator] overlay. The Frame + Indicator are siblings inside
 * one `Box` so the cell boundaries and the per-cell dot overlays stay
 * pixel-locked even though each layer recomposes independently.
 *
 * No vertical scroll here — Month fits a single screen by design. The
 * `weight(1f)` chain fills the column's vertical extent with 6 equal rows
 * (per v30 memory).
 *
 * @param monthStart the first day of the month (e.g. `LocalDate.of(2026, 7, 1)`).
 * @param selectedDate the date that the Frame marks as the selected cell
 *   (driven by the dashboard's selectedDay state).
 * @param items timeline events to count per day, partitioned to the visible
 *   month only.
 * @param zone the user/system zone used to project an instant into a
 *   calendar date.
 * @param onOpenDay tap callback fired from any cell.
 */
@Composable
fun MonthView(
    monthStart: LocalDate,
    selectedDate: LocalDate,
    items: List<CoreTimelineItem>,
    zone: ZoneId,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemsByDate: Map<LocalDate, Int> = remember(items, monthStart, zone) {
        countEventsByDate(items, monthStart, zone)
    }
    val gridStart = remember(monthStart) {
        monthStart.minusDays((monthStart.dayOfWeek.value - 1).toLong())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topBarTotalHeight()),
    ) {
        // Day-of-week header row (Mon..Sun, locale-aware).
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (col in 0 until GridConstants.WEEK_DAYS) {
                Text(
                    text = shortDowLabel(gridStart.plusDays(col.toLong())),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // Frame + Indicator are siblings in one Box. Both lay out a
        // 7×6 grid via `weight(1f)` per row/cell, so the cell borders
        // and the indicator dots/pills pixel-lock across the two layers.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("month-view"),
        ) {
            MonthViewFrame(
                monthStart = monthStart,
                selectedDate = selectedDate,
                onSelectDate = onOpenDay,
                modifier = Modifier.fillMaxSize(),
            )
            // Indicator overlay. Sibling Box stacks on top of the Frame.
            Column(modifier = Modifier.fillMaxSize()) {
                for (weekRow in 0 until GridConstants.MONTH_GRID_ROWS) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) {
                        for (dayCol in 0 until GridConstants.WEEK_DAYS) {
                            val cellDate = gridStart.plusDays(
                                (weekRow * GridConstants.WEEK_DAYS + dayCol).toLong(),
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                contentAlignment = Alignment.BottomEnd,
                            ) {
                                MonthEventIndicator(
                                    count = itemsByDate[cellDate] ?: 0,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Count timeline items per day within the visible month. Mirrors the prior
 * inline aggregation that lived inside the old `MonthView` in
 * `TimelineScreen.kt`: parses `startAt` as an [Instant], projects through
 * [zone] to a [LocalDate], and increments the matching day bucket if it
 * falls inside the month. Items outside the month are skipped.
 */
private fun countEventsByDate(
    items: List<CoreTimelineItem>,
    monthStart: LocalDate,
    zone: ZoneId,
): Map<LocalDate, Int> {
    val map = mutableMapOf<LocalDate, Int>()
    items.forEach { item ->
        val start = parseInstantOrNullLocal(item.startAt) ?: return@forEach
        val day = start.atZone(zone).toLocalDate()
        if (day.month != monthStart.month || day.year != monthStart.year) return@forEach
        map[day] = (map[day] ?: 0) + 1
    }
    return map
}

/**
 * Tiny parse helper scoped to Month view. Duplicated here (instead of
 * deduplicating with `TimelineScreen.parseInstantOrNull`) to keep the
 * calendar package self-contained — general helper dedup is T6's job and
 * pulling TimelineScreen's private helper out of scope would require
 * touching unrelated files.
 */
private fun parseInstantOrNullLocal(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        Instant.parse(value)
    } catch (_: Exception) {
        try {
            java.time.OffsetDateTime.parse(value).toInstant()
        } catch (_: Exception) {
            null
        }
    }
}
