package app.tastile.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.core.CoreTimelineItem
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TimelineScreen(viewModel: DashboardViewModel) {
    val rawItems by viewModel.timeline.collectAsStateWithLifecycle()
    var zoomScale by remember { mutableFloatStateOf(1.6f) }
    val pxPerMinute = 1.2f * zoomScale
    val hourHeightDp = (pxPerMinute * 60f).dp
    val timelineHeightDp = (hourHeightDp.value * 24f).dp
    val arranged = remember(rawItems) { arrangeVisibleBlocks(rawItems, Instant.now(), ZoneId.systemDefault()) }
    val scrollState = rememberScrollState()
    var contentWidthPx by remember { mutableIntStateOf(0) }
    val timeLabelWidth = 56.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.8f, 3.2f)
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(timeLabelWidth))
            Text("Timeline", style = MaterialTheme.typography.titleMedium)
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            TimeLabelsColumn(
                width = timeLabelWidth,
                hourHeight = hourHeightDp,
                modifier = Modifier.height(timelineHeightDp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(timelineHeightDp)
                    .onSizeChanged { contentWidthPx = it.width }
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                HourGrid(hourHeight = hourHeightDp, modifier = Modifier.fillMaxSize())
                arranged.forEach { block ->
                    TimelineEventBlock(
                        block = block,
                        totalWidthPx = contentWidthPx,
                        pxPerMinute = pxPerMinute
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeLabelsColumn(width: androidx.compose.ui.unit.Dp, hourHeight: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(width)) {
        repeat(24) { hour ->
            Box(modifier = Modifier.height(hourHeight)) {
                Text(
                    text = "%02d:00".format(Locale.US, hour),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun HourGrid(hourHeight: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        repeat(24) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight)
                    .background(MaterialTheme.colorScheme.surface)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
        }
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
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = statusIcon(block.item.status),
                contentDescription = "Status",
                modifier = Modifier.width(18.dp).height(18.dp)
            )
            Text(block.item.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
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

@Suppress("UNUSED_PARAMETER")
internal fun arrangeVisibleBlocks(
    items: List<CoreTimelineItem>,
    now: Instant,
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

