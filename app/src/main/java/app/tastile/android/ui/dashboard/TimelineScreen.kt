package app.tastile.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.repository.CalendarProjectionResponse
import app.tastile.android.ui.designsystem.AppBodyText
import app.tastile.android.ui.designsystem.AppScreenTitle
import app.tastile.android.ui.designsystem.AppSecondaryButton
import app.tastile.android.ui.designsystem.AppTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

enum class CalendarViewMode {
    DAY,
    WEEK,
    MONTH
}

@Composable
fun TimelineScreen(
    viewModel: DashboardViewModel,
    mode: CalendarViewMode = CalendarViewMode.MONTH
) {
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    when (mode) {
        CalendarViewMode.MONTH -> MonthCalendarScreen(
            projection = null,
            fallbackTimeline = timeline,
            modifier = Modifier.fillMaxSize()
        )
        CalendarViewMode.DAY -> DayAgendaScreen(null, timeline, Modifier.fillMaxSize())
        CalendarViewMode.WEEK -> WeekAgendaScreen(null, timeline, Modifier.fillMaxSize())
    }
}

@Composable
private fun DayAgendaScreen(
    projection: CalendarProjectionResponse?,
    timeline: List<CoreTimelineItem>,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val todayBlocks = (projection?.blocks ?: timelineToProjectionBlocks(timeline))
        .filter { block -> parseInstant(block.startAt)?.atZone(zone)?.toLocalDate() == today }
        .sortedBy { parseInstant(it.startAt) }

    Column(
        modifier = modifier
            .background(AppTheme.colors.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AppScreenTitle("Day View")
        AppBodyText(today.toString())
        if (todayBlocks.isEmpty()) {
            AppBodyText(
                text = "No scheduled blocks today",
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            todayBlocks.forEach { block ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    AppBodyText(
                        text = parseInstant(block.startAt)?.atZone(zone)?.format(
                            DateTimeFormatter.ofPattern("HH:mm", Locale.US)
                        ) ?: "--:--",
                        modifier = Modifier.width(56.dp)
                    )
                    AppBodyText(block.title)
                }
            }
        }
    }
}

@Composable
private fun WeekAgendaScreen(
    projection: CalendarProjectionResponse?,
    timeline: List<CoreTimelineItem>,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val blocks = projection?.blocks ?: timelineToProjectionBlocks(timeline)

    Column(
        modifier = modifier
            .background(AppTheme.colors.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AppScreenTitle("Week View")
        weekDays.forEach { day ->
            val count = blocks.count { block ->
                parseInstant(block.startAt)?.atZone(zone)?.toLocalDate() == day
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                AppBodyText(day.dayOfWeek.name.take(3), modifier = Modifier.width(56.dp))
                AppBodyText("$count items")
            }
        }
        AppSecondaryButton(
            text = "Today",
            onClick = {},
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun TimelineEventBlock(
    block: ArrangedBlock,
    totalWidthPx: Int,
    pxPerMinute: Float
) {
    val durationMin = max(1, block.endMinute - block.startMinute)
    val topPx = block.startMinute * pxPerMinute
    val heightPx = durationMin * pxPerMinute
    val columnWidthPx = if (block.columnCount <= 0) totalWidthPx.toFloat() else totalWidthPx.toFloat() / block.columnCount.toFloat()
    val leftPx = block.columnIndex * columnWidthPx

    Box(
        modifier = Modifier
            .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
            .width((columnWidthPx / androidx.compose.ui.platform.LocalDensity.current.density).dp)
            .height((heightPx / androidx.compose.ui.platform.LocalDensity.current.density).dp)
            .background(AppTheme.colors.primary.copy(alpha = 0.16f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = statusIcon(block.item.status),
                contentDescription = "Status",
                modifier = Modifier.width(18.dp).height(18.dp)
            )
            AppBodyText(block.item.title, modifier = Modifier.weight(1f))
        }
    }
}

internal data class ArrangedBlock(
    val item: CoreTimelineItem,
    val startMinute: Int,
    val endMinute: Int,
    val columnIndex: Int,
    val columnCount: Int
)

private data class TimelineWindow(
    val item: CoreTimelineItem,
    val start: Instant,
    val end: Instant,
    val startMinute: Int,
    val endMinute: Int
)

internal fun arrangeVisibleBlocks(
    items: List<CoreTimelineItem>,
    zoneId: ZoneId
): List<ArrangedBlock> {
    val sorted = items.mapNotNull { item ->
        val start = parseInstant(item.startAt) ?: return@mapNotNull null
        val parsedEnd = parseInstant(item.endAt ?: item.startAt)
        val end = when {
            parsedEnd == null -> start.plusSeconds(60)
            parsedEnd.isAfter(start) -> parsedEnd
            else -> start.plusSeconds(60)
        }
        val startMinute = minuteOfDay(start, zoneId)
        val endMinute = minuteOfDay(end, zoneId).coerceAtLeast(startMinute + 1)
        TimelineWindow(
            item = item,
            start = start,
            end = end,
            startMinute = startMinute,
            endMinute = endMinute
        )
    }.sortedBy { it.start }
    val groups = mutableListOf<MutableList<TimelineWindow>>()
    sorted.forEach { window ->
        val targetGroup = groups.firstOrNull { group ->
            group.any { existing ->
                window.start < existing.end && existing.start < window.end
            }
        }
        if (targetGroup == null) groups += mutableListOf(window) else targetGroup += window
    }

    val result = mutableListOf<ArrangedBlock>()
    groups.forEach { group ->
        val placed = mutableListOf<Pair<TimelineWindow, Int>>()
        group.forEach { window ->
            var col = 0
            while (placed.any { (existing, c) -> c == col && overlaps(existing, window) }) {
                col++
            }
            placed += window to col
        }
        val totalCols = (placed.maxOfOrNull { it.second } ?: 0) + 1
        placed.forEach { (window, col) ->
            result += ArrangedBlock(
                item = window.item,
                startMinute = window.startMinute,
                endMinute = window.endMinute,
                columnIndex = col,
                columnCount = totalCols
            )
        }
    }
    return result
}

private fun overlaps(a: TimelineWindow, b: TimelineWindow): Boolean {
    return a.start < b.end && b.start < a.end
}

private fun minuteOfDay(instant: Instant, zoneId: ZoneId): Int {
    val dt = instant.atZone(zoneId)
    return dt.hour * 60 + dt.minute
}

internal fun parseInstant(iso: String): Instant? {
    return try {
        Instant.parse(iso)
    } catch (_: Exception) {
        try {
            ZonedDateTime.parse(iso, DateTimeFormatter.ISO_DATE_TIME).toInstant()
        } catch (_: Exception) {
            null
        }
    }
}

private fun statusIcon(status: String) = when (status.lowercase()) {
    "done" -> Icons.Default.Check
    "active" -> Icons.Default.PlayArrow
    else -> Icons.Default.RadioButtonUnchecked
}

private fun timelineToProjectionBlocks(items: List<CoreTimelineItem>): List<app.tastile.android.data.repository.CalendarProjectionBlockResponse> {
    return items.map { item ->
        app.tastile.android.data.repository.CalendarProjectionBlockResponse(
            tileId = item.tileId,
            title = item.title,
            startAt = item.startAt,
            endAt = item.endAt ?: item.startAt
        )
    }
}
