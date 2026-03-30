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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TimelineScreen(viewModel: DashboardViewModel) {
    val rawItems by viewModel.timeline.collectAsStateWithLifecycle()
    var zoomScale by remember { mutableFloatStateOf(1.6f) }
    val pxPerMinute = 1.2f * zoomScale
    val hourHeightDp = (pxPerMinute * 60f).dp
    val timelineHeightDp = (hourHeightDp.value * 24f).dp
    val arranged = remember(rawItems) { layoutOverlaps(rawItems) }
    val scrollState = rememberScrollState()
    var contentWidthPx by remember { mutableStateOf(0) }
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
                    text = "%02d:00".format(hour),
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
    val startMinute = parseMinuteOfDay(block.item.startAt)
    val endMinute = parseMinuteOfDay(block.item.endAt ?: block.item.startAt)
    val durationMin = max(1, endMinute - startMinute)
    val topPx = startMinute * pxPerMinute
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
            IconButton(onClick = {}, modifier = Modifier.width(22.dp).height(22.dp)) {
                Icon(imageVector = statusIcon(block.item.status), contentDescription = "Status")
            }
            Text(block.item.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
    }
}

private data class ArrangedBlock(
    val item: CoreTimelineItem,
    val columnIndex: Int,
    val columnCount: Int
)

private fun layoutOverlaps(items: List<CoreTimelineItem>): List<ArrangedBlock> {
    val sorted = items.sortedBy { it.startAt }
    val groups = mutableListOf<MutableList<CoreTimelineItem>>()
    sorted.forEach { item ->
        val itemStart = parseInstant(item.startAt) ?: return@forEach
        val itemEnd = parseInstant(item.endAt ?: item.startAt)?.plusSeconds(60) ?: itemStart.plusSeconds(60)
        val targetGroup = groups.firstOrNull { group ->
            group.any { existing ->
                val aStart = parseInstant(existing.startAt) ?: return@any false
                val aEnd = parseInstant(existing.endAt ?: existing.startAt)?.plusSeconds(60) ?: aStart.plusSeconds(60)
                itemStart < aEnd && aStart < itemEnd
            }
        }
        if (targetGroup == null) groups += mutableListOf(item) else targetGroup += item
    }

    val result = mutableListOf<ArrangedBlock>()
    groups.forEach { group ->
        val placed = mutableListOf<Pair<CoreTimelineItem, Int>>()
        group.forEach { item ->
            var col = 0
            while (placed.any { (existing, c) -> c == col && overlaps(existing, item) }) {
                col++
            }
            placed += item to col
        }
        val totalCols = (placed.maxOfOrNull { it.second } ?: 0) + 1
        placed.forEach { (item, col) ->
            result += ArrangedBlock(item = item, columnIndex = col, columnCount = totalCols)
        }
    }
    return result
}

private fun overlaps(a: CoreTimelineItem, b: CoreTimelineItem): Boolean {
    val aStart = parseInstant(a.startAt) ?: return false
    val aEnd = parseInstant(a.endAt ?: a.startAt)?.plusSeconds(60) ?: aStart.plusSeconds(60)
    val bStart = parseInstant(b.startAt) ?: return false
    val bEnd = parseInstant(b.endAt ?: b.startAt)?.plusSeconds(60) ?: bStart.plusSeconds(60)
    return aStart < bEnd && bStart < aEnd
}

private fun parseMinuteOfDay(iso: String): Int {
    val instant = parseInstant(iso) ?: return 0
    val dt = instant.atZone(ZoneOffset.UTC)
    return dt.hour * 60 + dt.minute
}

private fun parseInstant(iso: String): Instant? = try {
    Instant.parse(iso)
} catch (_: Exception) {
    null
}

private fun statusIcon(status: String) = when (status.lowercase()) {
    "done" -> Icons.Default.Check
    "active" -> Icons.Default.PlayArrow
    else -> Icons.Default.RadioButtonUnchecked
}

