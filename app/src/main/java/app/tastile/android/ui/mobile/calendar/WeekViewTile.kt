package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.core.CoreTimelineItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

/**
 * Per-day columns for the Week view (Phase v37 / Task 4). Owns:
 *   - the 7 per-day columns of [EventChipContent] blocks
 *   - the [NowIndicator] overlay (only the column whose date matches
 *     `LocalDate.now()` is allowed to render one).
 *
 * Holds zero references to time-gutter, day-of-week header, or grid lines.
 * [WeekViewFrame] renders those.
 */
@Composable
fun WeekViewTile(
    weekStart: LocalDate,
    blocksByDay: Map<LocalDate, List<PlacedBlock>>,
    pxPerMin: Float,
    zone: ZoneId,
    scrollState: ScrollState,
    onEditEvent: (CoreTimelineItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val endHour = GridConstants.DAY_END_HOUR
    Row(modifier = modifier.fillMaxSize().testTag("week-view-tile")) {
        for (offset in 0 until GridConstants.WEEK_DAYS) {
            val day = weekStart.plusDays(offset.toLong())
            val blocks = blocksByDay[day].orEmpty()
            val isToday = day == java.time.LocalDate.now()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    .clickable { /* day-column tap is handled in the Frame */ }
                    .testTag("week-view-tile-event-column"),
            ) {
                // Event chips for this day
                for (b in blocks) {
                    val topDp = (b.startMinutes * pxPerMin).dp
                    val heightDp = ((b.endMinutes - b.startMinutes) * pxPerMin)
                        .coerceAtLeast(GridConstants.MIN_EVENT_HEIGHT_DP.value).dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = topDp)
                            .height(heightDp)
                            .padding(horizontal = 1.dp)
                            .testTag("week-view-tile-event-chip"),
                    ) {
                        EventChipContent(b, onEditEvent)
                    }
                }
                // NowIndicator only on today's column. State and per-minute
                // ticker live inside `WeekMinuteTicker` so the surrounding
                // tile does NOT recompose on each tick; only the inner Box
                // (the red dot + line) re-positions. The LaunchedEffect is
                // keyed on the displayed `day` so a week-page switch tears
                // down the previous day's looping coroutine.
                if (isToday) {
                    WeekMinuteTicker(
                        dayKey = day,
                        zone = zone,
                        pxPerMin = pxPerMin,
                        dayRangeEndHour = endHour,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("week-view-tile-now-indicator"),
                    )
                }
            }
        }
    }
}

/**
 * Self-contained now-line overlay for the Week column. Owns its own
 * `nowInstant` state and per-minute ticker so the surrounding
 * [WeekViewTile] / row / column does NOT recompose when the wall clock
 * advances — only the inner [NowIndicator] Box re-positions.
 *
 * The [LaunchedEffect] is keyed on [dayKey] so navigating away from today
 * (or switching the visible week) cancels the per-minute coroutine.
 */
@Composable
private fun WeekMinuteTicker(
    dayKey: LocalDate,
    zone: ZoneId,
    pxPerMin: Float,
    dayRangeEndHour: Int,
    modifier: Modifier = Modifier,
) {
    var nowInstant by remember { mutableStateOf<Instant?>(Instant.now()) }
    LaunchedEffect(dayKey) {
        while (true) {
            delay(60_000L)
            nowInstant = Instant.now()
        }
    }
    NowIndicator(
        nowProvider = { nowInstant },
        zone = zone,
        pxPerMin = pxPerMin,
        dayRangeStartHour = GridConstants.DAY_START_HOUR,
        dayRangeEndHour = dayRangeEndHour,
        modifier = modifier,
    )
}
