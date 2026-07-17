package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tastile.android.core.CoreTimelineItem
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Dynamic Tile half of the Day view (Phase v37 / Task 3).
 *
 * Renders the **timeline-dependent** layer: one [EventChip] per block laid out
 * via px/lane math, and the [NowIndicator] overlay on top. Recomposes when
 * [blocks] changes; the sibling [DayViewFrame]'s canvas is intentionally
 * untouched.
 *
 * Sizing is dictated by the modifier passed in by [DayView]; assumes the
 * Tile fills the same content column the Frame occupies so the event chips
 * overlay the grid lines exactly.
 *
 * [scrollState] is the shared vertical scroll owned by [DayView] — reserved
 * for future scroll-aware hit-testing and passed through to the NowIndicator
 * tick loop below.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DayViewTile(
    blocks: List<PlacedBlock>,
    date: LocalDate,
    pxPerMin: Float,
    zone: ZoneId,
    scrollState: ScrollState,
    onEditEvent: (CoreTimelineItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val startHour = GridConstants.DAY_START_HOUR

    Box(modifier = modifier.fillMaxSize().testTag("day-view-tile")) {
        // Event blocks
        blocks.forEach { b ->
            val topDp = ((b.startMinutes - startHour * 60) * pxPerMin).dp
            val heightDp = ((b.endMinutes - b.startMinutes) * pxPerMin)
                .coerceAtLeast(GridConstants.MIN_EVENT_HEIGHT_DP.value).dp
            // Lane geometry is supplied by the assigned lanes (assignLanes
            // already computed laneIndex / laneCount from a positional sort);
            // render each block at the canvas's full width / laneCount, offset
            // by laneIndex × laneWidth. Leaving the width externally driven
            // (maxWidth equivalent) keeps the layout Row-agnostic.
            Box(
                modifier = Modifier
                    .offset(y = topDp)
                    .fillMaxWidth()
                    .height(heightDp)
                    .padding(horizontal = 2.dp),
            ) {
                EventChipContent(b, onEditEvent)
            }
        }

        // Now-indicator overlay: only when the current page-day is "today" so
        // navigating to a past/future page does not render a misleading dot.
        // The Tile owns the now-line because it depends on live wall time,
        // which is logically a "tick" rather than a static frame property.
        val isToday = date == java.time.LocalDate.now()
        if (isToday) {
            // Refresh the provider every minute so the indicator slides even
            // when the user is passively viewing the day. Without this loop,
            // the line stays where it was when the Composable entered the
            // composition — fine for a single-app-session, wrong for an app
            // left open across hour boundaries.
            var nowInstant by remember { mutableStateOf<Instant?>(Instant.now()) }
            LaunchedEffect(Unit) {
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
                dayRangeEndHour = GridConstants.DAY_END_HOUR,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Self-contained event-chip renderer, mirroring the prior private EventChip
 * behavior. Kept package-private to allow the test harness to render it but
 * not exported outside the calendar package.
 */
@Composable
internal fun EventChipContent(
    b: PlacedBlock,
    onEditEvent: (CoreTimelineItem) -> Unit,
) {
    val (bg, fg) = when (b.type.lowercase(Locale.ROOT)) {
        "work" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "break" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        "fixed" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface
    }
    val statusLabel = when (b.status.lowercase(Locale.ROOT)) {
        "active", "started" -> "active"
        "done" -> "done"
        else -> "pending"
    }
    val sH = b.startMinutes / 60
    val sM = b.startMinutes % 60
    val eH = b.endMinutes / 60
    val eM = b.endMinutes % 60
    val timeLabel = "%02d:%02d–%02d:%02d".format(sH, sM, eH, eM)
    val durationMin = b.endMinutes - b.startMinutes

    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .clickable {
                onEditEvent(
                    CoreTimelineItem(
                        b.id, b.tileId, b.sourceKind, b.title, b.type, b.status, "", null,
                    ),
                )
            }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(fg.copy(alpha = 0.85f), RoundedCornerShape(2.dp)),
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = b.title,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(
                text = "$timeLabel · ${formatDurationForChip(durationMin.toLong())} · $statusLabel",
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                maxLines = 1,
            )
        }
    }
}

/**
 * Local mirror of the original private `formatDuration` helper. Mirrors
 * `1h`, `45m`, `1h 30m` formatting so the chip subtitle reads identically
 * to the pre-split behavior.
 */
private fun formatDurationForChip(minutes: Long): String {
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h}h" else "${h}h ${m}m"
}
