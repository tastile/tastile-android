package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Static Frame for the Week view (Phase v37 / Task 4).
 *
 * Owns the structural scaffolding of the Week view:
 *   - 7 day-of-week Text header columns (Mon..Sun)
 *   - 7 columns of 25 hour grid lines drawn as one Canvas
 *   - the shared time-gutter header spacer at top-left
 *
 * Holds ZERO references to `PlacedBlock` / `blocks` so Compose's
 * stability pass keeps its Canvas pinned when the timeline list
 * changes. Event chips and the NowIndicator live in [WeekViewTile].
 *
 * Day-of-week labels are emitted as [Text] Composables (not
 * `Canvas.drawText`) so the Semantics tree sees them and tests can
 * assert them via `onNodeWithText`.
 */
@Composable
fun WeekViewFrame(
    weekStart: LocalDate,
    pxPerMin: Float,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val endHour = GridConstants.DAY_END_HOUR
    val dayLabels = remember(Locale.getDefault()) {
        val pattern = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        (0 until GridConstants.WEEK_DAYS).map { offset ->
            weekStart.plusDays(offset.toLong()).format(pattern)
        }
    }
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.testTag("week-view-frame").fillMaxSize()) {
        // Day-of-week header row. Spacer leaves room for the time-gutter
        // header that lives outside this Frame (WeekView.kt hosts the
        // shared gutter), then 7 columns of Text composables.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GridConstants.WEEK_HEADER_HEIGHT)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Spacer(
                    modifier = Modifier.width(GridConstants.TIME_GUTTER_WIDTH),
                )
                for (offset in 0 until GridConstants.WEEK_DAYS) {
                    val day = weekStart.plusDays(offset.toLong())
                    DayHeader(
                        label = dayLabels[offset],
                        day = day.dayOfMonth,
                        isToday = day == java.time.LocalDate.now(),
                        onClick = { onOpenDay(day) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
            HorizontalDivider(
                color = outlineColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.BottomCenter),
            )
        }
        // Grid-lines Canvas. Fills the rest of the Frame's height; the
        // WeekView host constrains the total height to
        // (pxPerMin × totalMinutes).dp so the 25 lines span the 24h
        // scrollable body. Modifier.fillMaxSize() makes the grid
        // cover the full 7 columns.
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(width = 0.5.dp, color = outlineColor)
                .testTag("week-view-frame-grid-lines"),
        ) {
            val pxPerMinPx = pxPerMin * density
            for (h in 0..endHour) {
                val y = h * 60 * pxPerMinPx
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
        }
    }
}

@Composable
private fun DayHeader(
    label: String,
    day: Int,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .testTag("week-view-frame-day-column"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
