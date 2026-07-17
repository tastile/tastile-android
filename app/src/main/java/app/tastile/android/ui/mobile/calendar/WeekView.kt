package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tastile.android.core.CoreTimelineItem
import java.time.LocalDate
import java.time.ZoneId

/**
 * Top-level Week view (Phase v37 / Task 4).
 *
 * Owns the single shared vertical scroll state and routes data to:
 *   - [WeekViewFrame] (static 7-column DOW header + grid lines)
 *   - [WeekViewTile] (per-day event chips + today's NowIndicator)
 *
 * Week has no pinch zoom in v37; the body height is computed from
 * [GridConstants.DAY_END_HOUR] and the caller's `zoom` value.
 *
 * Layout shape:
 *   Column {
 *     Frame (DOW header row, no scroll)
 *     Row(verticalScroll) {
 *       TimeGutter (scrolls with body)
 *       Box {
 *         (Frame grid lines — overlaid so the grid + chips translate together)
 *         Tile (per-day columns)
 *       }
 *     }
 *   }
 */
@Composable
fun WeekView(
    items: List<CoreTimelineItem>,
    weekStart: LocalDate,
    zone: ZoneId,
    onOpenDay: (LocalDate) -> Unit,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onEditEvent: (CoreTimelineItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val blocksByDay = remember(items, weekStart, zone) {
        (0 until GridConstants.WEEK_DAYS).associate { offset ->
            val day = weekStart.plusDays(offset.toLong())
            day to toDayBlocks(items, day, zone)
        }
    }
    val totalMinutes = GridConstants.DAY_END_HOUR * 60 + GridConstants.SCROLL_BUFFER_MIN * 2
    val pxPerMin = computeWeekPxPerMin(zoom = zoom, totalMinutes = totalMinutes)
    val totalHeight: Dp = (pxPerMin * totalMinutes).dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Header (DOW + day number) lives in the Frame. It does not scroll.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GridConstants.WEEK_HEADER_HEIGHT),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Spacer(
                    modifier = Modifier
                        .width(GridConstants.TIME_GUTTER_WIDTH)
                        .height(GridConstants.WEEK_HEADER_HEIGHT)
                        .background(MaterialTheme.colorScheme.background),
                )
                // 7 header text composables — slotted here to keep them
                // visually aligned with the body columns. The
                // `WeekViewFrame` composable separately owns the
                // `week-view-frame` testTag for unit tests; the visible
                // headers here are equivalent Composables.
                WeekHeaderRow(
                    weekStart = weekStart,
                    onOpenDay = onOpenDay,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // Body: gutter + Frame grid + Tile columns share one scroll.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
        ) {
            WeekTimeGutter(
                endHour = GridConstants.DAY_END_HOUR,
                pxPerHour = (pxPerMin * 60).dp,
                totalHeight = totalHeight,
                modifier = Modifier.width(GridConstants.TIME_GUTTER_WIDTH),
            )
            // Frame: 7-column hour grid (sibling of Tile in one scroll)
            WeekViewFrame(
                weekStart = weekStart,
                pxPerMin = pxPerMin,
                onOpenDay = onOpenDay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight),
            )
            // Tile: 7 columns of event chips + today's NowIndicator.
            // The Tile is overlaid via a sibling Box so the chips
            // translate with the Frame's grid under the same scroll.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight),
            ) {
                WeekViewTile(
                    weekStart = weekStart,
                    blocksByDay = blocksByDay,
                    pxPerMin = pxPerMin,
                    zone = zone,
                    scrollState = scrollState,
                    onEditEvent = onEditEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // Suppress unused-onZoomChange warning: signature mirrors DayView
        // even though Week currently has no pinch zoom.
        @Suppress("UNUSED_EXPRESSION")
        onZoomChange
    }
}

/**
 * The DOW header row (7 day-of-week Text columns). Mirrors the
 * private header `Row` that lived in the inline `WeekView` from
 * TimelineScreen.kt — same shape, just lifted to a reusable function
 * so both the production view and the test render the same widget.
 */
@Composable
private fun WeekHeaderRow(
    weekStart: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        for (offset in 0 until GridConstants.WEEK_DAYS) {
            val day = weekStart.plusDays(offset.toLong())
            val isToday = day == java.time.LocalDate.now()
            val label = remember(day) {
                day.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.getDefault()))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onOpenDay(day) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isToday) FontWeight.Bold
                    else FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Compute the week body's `pxPerMin` (dp/min) from the caller's zoom
 * value. Week has no pinch zoom in v37, so this is a passthrough
 * clamped to [GridConstants.ZOOM_MIN] floor; kept as a named
 * function so T7's planned PxPerMinTest can target it later.
 */
internal fun computeWeekPxPerMin(zoom: Float, totalMinutes: Int): Float {
    val base = 1f
    return (base * zoom.coerceIn(GridConstants.ZOOM_MIN, GridConstants.ZOOM_MAX))
        .coerceAtLeast(0.1f)
}

/**
 * Shared time-gutter Composable for the Week body. Mirrors the prior
 * private `WeekTimeGutter` from TimelineScreen.kt so the labels stay
 * pixel-aligned with the grid lines drawn by [WeekViewFrame]. Kept
 * `internal` and colocated inside WeekView.kt so the v37 split has
 * exactly 3 new production files (per the design's file tree).
 */
@Composable
internal fun WeekTimeGutter(
    endHour: Int,
    pxPerHour: Dp,
    totalHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
    ) {
        val pxPerHourPx = pxPerHour.toPx()
        val padRight = 6.dp.toPx()
        for (h in 0..endHour) {
            val yLine = h * pxPerHourPx
            val label = "%02d".format(h)
            val measured = textMeasurer.measure(label, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(
                    x = size.width - measured.size.width - padRight,
                    y = yLine - measured.size.height / 2f,
                ),
                style = labelStyle,
            )
        }
    }
}
