package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.designsystem.AppLoading
import app.tastile.android.ui.designsystem.AppTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_GUTTER_WIDTH = 48.dp
private val MIN_EVENT_HEIGHT_DP = 22.dp
private val PX_PER_MIN = 1.8f      // body-row height: 24h × 60 × 1.8 = 2592dp
private val WEEK_HEADER_HEIGHT = 52.dp
private const val PAGER_CENTER = 365
private const val PAGER_TOTAL = 731
private const val ZOOM_MIN = 1f     // base = 24h fit on screen; max = 6x for detail
private const val ZOOM_MAX = 6f

private val sampleTimeline: List<CoreTimelineItem> = run {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    fun at(hour: Int, minute: Int): String =
        today.atTime(hour, minute).atZone(zone).toInstant().toString()
    listOf(
        // Lane split proof: 9:00-10:30 work + 10:00-10:30 email triage overlap
        CoreTimelineItem("demo-1", "t1", "Deep work: design system audit", "work", "done", at(9, 0), at(10, 30)),
        CoreTimelineItem("demo-2", "t2", "Email triage", "work", "done", at(10, 0), at(10, 30)),
        // Sequential after deep work
        CoreTimelineItem("demo-3", "t3", "Short break", "break", "done", at(10, 30), at(10, 45)),
        CoreTimelineItem("demo-4", "t4", "Review pull requests", "work", "done", at(10, 45), at(12, 0)),
        CoreTimelineItem("demo-5", "t5", "Lunch", "fixed", "active", at(12, 0), at(13, 0)),
        // 13:00-14:30 spec + 13:30-14:00 pair review overlap
        CoreTimelineItem("demo-6", "t6", "Write command-event spec", "work", "active", at(13, 0), at(14, 30)),
        CoreTimelineItem("demo-7", "t7", "Pair review: catch-up", "work", "pending", at(13, 30), at(14, 0)),
        CoreTimelineItem("demo-8", "t8", "Team standup", "fixed", "pending", at(15, 0), at(15, 15)),
        CoreTimelineItem("demo-9", "t9", "Flow block: backlog grooming", "work", "pending", at(15, 30), at(17, 0)),
        CoreTimelineItem("demo-10", "t10", "Wind-down break", "break", "pending", at(17, 0), at(17, 15)),
    )
}

private data class PlacedBlock(
    val id: String,
    val title: String,
    val type: String,
    val status: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val laneIndex: Int,
    val laneCount: Int,
)

@Composable
fun TimelineScreen(viewModel: DashboardViewModel) {
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val scale by viewModel.scale.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val zone = remember { ZoneId.systemDefault() }

    val activeTimeline = remember(timeline) {
        if (timeline.isNotEmpty()) timeline else sampleTimeline
    }

    val onOpenDay: (LocalDate) -> Unit = { day ->
        viewModel.setSelectedDay(day)
        viewModel.setScale(TimelineScale.Day)
    }

    // One pagerState shared across scales; each scale interprets page offset as
    // days / weeks / months from PAGER_CENTER. Snapping to center on scale change
    // keeps "today" anchored regardless of how far the user had swiped elsewhere.
    val pagerState = rememberPagerState(
        initialPage = PAGER_CENTER,
        pageCount = { PAGER_TOTAL },
    )
    LaunchedEffect(scale) {
        if (pagerState.currentPage != PAGER_CENTER) {
            pagerState.scrollToPage(PAGER_CENTER)
        }
    }
    // Map current page → selectedDay so the header title reflects the visible range.
    LaunchedEffect(pagerState.currentPage, scale, today) {
        val offset = (pagerState.currentPage - PAGER_CENTER).toLong()
        val target = when (scale) {
            TimelineScale.Day -> today.plusDays(offset)
            TimelineScale.Week -> {
                val ref = today.plusWeeks(offset)
                ref.minusDays((ref.dayOfWeek.value - 1).toLong())
            }
            TimelineScale.Month -> today.plusMonths(offset)
        }
        if (target != selectedDay) viewModel.setSelectedDay(target)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading && timeline.isEmpty() && scale == TimelineScale.Day -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppLoading()
                }
            }
            scale == TimelineScale.Day -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                ) { page ->
                    val pageDay = today.plusDays((page - PAGER_CENTER).toLong())
                    val pageBlocks = remember(activeTimeline, pageDay) {
                        toDayBlocks(activeTimeline, pageDay, zone)
                    }
                    DayGrid(
                        blocks = pageBlocks,
                        day = pageDay,
                    )
                }
            }
            scale == TimelineScale.Week -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                ) { page ->
                    val anchor = today.plusWeeks((page - PAGER_CENTER).toLong())
                    val pageWeekStart = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
                    WeekView(
                        items = activeTimeline,
                        weekStart = pageWeekStart,
                        zone = zone,
                        onOpenDay = onOpenDay,
                    )
                }
            }
            scale == TimelineScale.Month -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                ) { page ->
                    val pageMonthStart = today.plusMonths((page - PAGER_CENTER).toLong()).withDayOfMonth(1)
                    MonthView(
                        items = activeTimeline,
                        monthStart = pageMonthStart,
                        zone = zone,
                        selectedDay = selectedDay,
                        onOpenDay = onOpenDay,
                    )
                }
            }
            else -> EmptyState(scale)
        }
    }
}

@Composable
private fun DayGrid(
    blocks: List<PlacedBlock>,
    day: LocalDate,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Day view always spans the full 24 hours so that (a) the user can scroll through the
    // entire day, (b) the min zoom (pxPerMin = availableHeight / 1440) shows the whole
    // day on one screen without overflowing.
    val startHour = 0
    val endHour = 24
    // 15 min of empty space above and below the labeled 0–24 range so the day has
    // a small scrollable buffer at the min zoom floor (otherwise totalHeight ==
    // availableHeight and maxScroll = 0).
    val SCROLL_BUFFER_MIN = 15
    val totalMinutes = 24 * 60 + SCROLL_BUFFER_MIN * 2

    val isToday = day == LocalDate.now()
    val nowMin = remember(isToday) {
        if (isToday) LocalTime.now().hour * 60 + LocalTime.now().minute else -1
    }
    val showNowLine = nowMin in (startHour * 60)..(endHour * 60)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeightPx: Float = maxHeight.value
        val pxPerMinBase: Float = availableHeightPx / totalMinutes
        // Minimum: 24h fit on screen. We clamp pxPerMin to this floor regardless of
        // zoom, so the cells can never become smaller than the "day on one screen" size.
        val minPxPerMin: Float = pxPerMinBase
        val outlineColor = AppTheme.colors.outlineVariant
        val nowLineColor = Color(0xFFEF5350)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // detectTransformGestures uses touch-slop to defer consumption: a
                    // 1-finger drag is allowed to fall through to the child
                    // verticalScrolls, and only 2-finger pinches are claimed by this
                    // handler. The callback fires for both pan (1-finger drag = scroll)
                    // and zoom (2-finger pinch) with the gesture's centroid, so we can
                    // anchor the pinch to keep the time under the fingers pinned.
                    detectTransformGestures(panZoomLock = false) { centroid, pan, gestureZoom, _ ->
                        if (gestureZoom != 1f) {
                            val oldZoom = zoom
                            val newZoom = (zoom * gestureZoom).coerceIn(ZOOM_MIN, ZOOM_MAX)
                            if (newZoom != oldZoom) {
                                val pxPerMinOld = (pxPerMinBase * oldZoom).coerceAtLeast(minPxPerMin)
                                val pxPerMinNew = (pxPerMinBase * newZoom).coerceAtLeast(minPxPerMin)
                                val maxScroll = (pxPerMinNew * totalMinutes - availableHeightPx).coerceAtLeast(0f)
                                val minutesAtCentroid = (scrollState.value + centroid.y) / pxPerMinOld
                                val newScroll = (minutesAtCentroid * pxPerMinNew - centroid.y)
                                    .coerceIn(0f, maxScroll)
                                zoom = newZoom
                                scope.launch { scrollState.scrollTo(newScroll.toInt()) }
                            }
                        }
                        if (pan.y != 0f) {
                            scope.launch { scrollState.scrollBy(-pan.y) }
                        }
                    }
                },
        ) {
            val pxPerMin: Float = (pxPerMinBase * zoom).coerceAtLeast(minPxPerMin)
            val totalHeight: Dp = (pxPerMin * totalMinutes).dp

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(TIME_GUTTER_WIDTH)
                        .fillMaxHeight()
                        .verticalScroll(scrollState),
                ) {
                    TimeGutterContent(startHour, endHour, pxPerHour = (pxPerMin * 60).dp, totalHeight = totalHeight)
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    val canvasWidth = maxWidth

                    // Layer 1: hour grid lines (background). Wrapped in a verticalScroll
                    // that shares scrollState with the gutter and blocks so the lines
                    // translate together — without this the labels/blocks scroll but the
                    // grid lines stay fixed, causing visible drift on every scroll step.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalHeight)
                            .verticalScroll(scrollState),
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val hours = totalMinutes / 60
                            // pxPerMin is in dp/min; Canvas draws in raw pixels, so multiply
                            // by device density to land each grid line at the same Y as the
                            // labels (which use .toPx() on a Dp value) and the blocks (which
                            // use Modifier.offset(y = Dp)).
                            val pxPerMinPx = pxPerMin * density
                            for (h in 0..hours) {
                                val y = h * pxPerMinPx * 60
                                drawLine(
                                    color = outlineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f,
                                )
                            }
                        }
                    }

                    // Layer 2: event blocks (renders empty when blocks.isEmpty())
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalHeight)
                            .verticalScroll(scrollState),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
                            blocks.forEach { b ->
                                val topDp: Dp = ((b.startMinutes - startHour * 60) * pxPerMin).dp
                                val heightDp: Dp = ((b.endMinutes - b.startMinutes) * pxPerMin)
                                    .coerceAtLeast(MIN_EVENT_HEIGHT_DP.value)
                                    .dp
                                val laneWidth = canvasWidth / b.laneCount.coerceAtLeast(1)
                                val laneX = laneWidth * b.laneIndex

                                Box(
                                    modifier = Modifier
                                        .offset(x = laneX, y = topDp)
                                        .width(laneWidth)
                                        .height(heightDp)
                                        .padding(horizontal = 2.dp),
                                ) {
                                    EventChip(b)
                                }
                            }
                        }
                    }

                    // Layer 3: now-line (drawn ON TOP of tiles)
                    if (showNowLine) {
                        val elapsed = nowMin - startHour * 60
                        val nowY: Dp = (elapsed * pxPerMin).dp
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(totalHeight)
                                .verticalScroll(scrollState),
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = 8.dp, y = nowY - 5.dp)
                                        .size(10.dp)
                                        .background(nowLineColor, CircleShape),
                                )
                                Box(
                                    modifier = Modifier
                                        .offset(y = nowY - 1.dp)
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(nowLineColor),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeGutterContent(startHour: Int, endHour: Int, pxPerHour: Dp, totalHeight: Dp) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = AppTheme.typography.labelSmall.copy(color = AppTheme.colors.onSurfaceVariant)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight),
    ) {
        // Use pxPerHour.toPx() so the label y positions are pixel-aligned with the
        // grid-line y positions in DayGrid (where lines are drawn at h × pxPerMin × 60).
        // drawText centers glyphs at the given y via size.height / 2 in the offset.
        val pxPerHourPx = pxPerHour.toPx()
        val padRight = 6.dp.toPx()
        for (h in startHour..endHour) {
            val yLine = (h - startHour) * pxPerHourPx
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

@Composable
private fun EventChip(b: PlacedBlock) {
    val (bg, fg) = when (b.type.lowercase(Locale.ROOT)) {
        "work" -> AppTheme.colors.primary to AppTheme.colors.onPrimary
        "break" -> AppTheme.colors.tertiary to AppTheme.colors.onTertiary
        "fixed" -> AppTheme.colors.secondary to AppTheme.colors.onSecondary
        else -> AppTheme.colors.surfaceVariant to AppTheme.colors.onSurface
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
                style = AppTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(
                text = "$timeLabel · ${formatDuration(durationMin.toLong())} · $statusLabel",
                style = AppTheme.typography.labelSmall,
                color = fg,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WeekView(
    items: List<CoreTimelineItem>,
    weekStart: LocalDate,
    zone: ZoneId,
    onOpenDay: (LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val eeeFormatter = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
    val endHourGlobal = 24
    val totalMinutesGlobal = endHourGlobal * 60
    val bodyHeightDp: Dp = (totalMinutesGlobal * PX_PER_MIN).dp

    // Pre-compute per-day blocks at the same composition level so all columns share one body row.
    val blocksByDay: List<List<PlacedBlock>> = remember(items, weekStart) {
        (0L until 7L).map { offset ->
            toDayBlocks(items, weekStart.plusDays(offset), zone)
        }
    }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Day header row (above the scrollable body)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WEEK_HEADER_HEIGHT)
                .background(AppTheme.colors.background.copy(alpha = 0.97f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(TIME_GUTTER_WIDTH))
            for (offset in 0L until 7L) {
                val day = weekStart.plusDays(offset)
                val isToday = day == today
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onOpenDay(day) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = day.format(eeeFormatter),
                        style = AppTheme.typography.labelSmall,
                        color = if (isToday) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = AppTheme.typography.titleSmall,
                        color = if (isToday) AppTheme.colors.primary else AppTheme.colors.onSurface,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    )
                }
            }
        }
        HorizontalDivider(color = AppTheme.colors.outlineVariant)

        // Body row: shared vertical scroll across time gutter + 7 day columns.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
        ) {
            // Time gutter (scrolls with body)
            Column(
                modifier = Modifier
                    .width(TIME_GUTTER_WIDTH)
                    .height(bodyHeightDp),
            ) {
                WeekTimeGutter(endHour = endHourGlobal)
            }
            // 7 day columns
            for (offset in 0L until 7L) {
                val day = weekStart.plusDays(offset)
                val blocks = blocksByDay[offset.toInt()]
                WeekDayColumn(
                    blocks = blocks,
                    modifier = Modifier
                        .weight(1f)
                        .height(bodyHeightDp),
                    isToday = day == today,
                    onOpenDay = { onOpenDay(day) },
                )
            }
        }
    }
}

@Composable
private fun WeekTimeGutter(endHour: Int) {
    val pxPerHourDp: Dp = (60f * PX_PER_MIN).dp
    Column(modifier = Modifier.fillMaxHeight()) {
        for (h in 0..endHour) {
            Box(
                modifier = Modifier.height(pxPerHourDp).fillMaxWidth(),
                contentAlignment = Alignment.TopEnd,
            ) {
                Text(
                    text = "%02d".format(h),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp, top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun WeekDayColumn(
    blocks: List<PlacedBlock>,
    modifier: Modifier = Modifier,
    isToday: Boolean,
    onOpenDay: () -> Unit,
) {
    val outlineColor = AppTheme.colors.outlineVariant
    val pxPerHour = 60f * PX_PER_MIN
    val endHour = 24
    val nowLineColor = Color(0xFFEF5350)
    val nowMin = remember(isToday) {
        if (isToday) LocalTime.now().hour * 60 + LocalTime.now().minute else -1
    }

    Box(
        modifier = modifier
            .border(width = 0.5.dp, color = AppTheme.colors.outlineVariant)
            .clickable { onOpenDay() },
    ) {
        // Hour grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (h in 0..endHour) {
                val y = h * pxPerHour
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
        }
        // Event blocks
        blocks.forEach { b ->
            val topDp: Dp = (b.startMinutes * PX_PER_MIN).dp
            val heightDp: Dp = ((b.endMinutes - b.startMinutes) * PX_PER_MIN)
                .coerceAtLeast(MIN_EVENT_HEIGHT_DP.value).dp
            // Full-width (no lane split on mobile; single column per day)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = topDp)
                    .height(heightDp)
                    .padding(horizontal = 1.dp),
            ) {
                EventChip(b)
            }
        }
        // Now-line (drawn last = on top)
        if (isToday && nowMin in 0..(endHour * 60)) {
            val nowY: Dp = (nowMin * PX_PER_MIN).dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = nowY - 1.dp)
                    .height(2.dp)
                    .background(nowLineColor),
            )
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = nowY - 5.dp)
                    .size(10.dp)
                    .background(nowLineColor, CircleShape),
            )
        }
    }
}

@Composable
private fun MonthView(
    items: List<CoreTimelineItem>,
    monthStart: LocalDate,
    zone: ZoneId,
    selectedDay: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
) {
    val dowFormatter = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }

    val itemsByDate: Map<LocalDate, Int> = remember(items, monthStart) {
        val map = mutableMapOf<LocalDate, Int>()
        items.forEach { item ->
            val start = parseInstantOrNull(item.startAt) ?: return@forEach
            val day = start.atZone(zone).toLocalDate()
            if (day.month != monthStart.month || day.year != monthStart.year) return@forEach
            map[day] = (map[day] ?: 0) + 1
        }
        map
    }

    val gridStart = monthStart.minusDays((monthStart.dayOfWeek.value - 1).toLong())
    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxSize()) {
        // Day-of-week header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (col in 0 until 7) {
                val date = gridStart.plusDays(col.toLong())
                Text(
                    text = date.format(dowFormatter),
                    style = AppTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // 6 week rows, each weight(1f) → fills remaining vertical space
        for (weekRow in 0 until 6) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                for (dayCol in 0 until 7) {
                    val cellDate = gridStart.plusDays((weekRow * 7 + dayCol).toLong())
                    MonthDayCell(
                        date = cellDate,
                        inMonth = cellDate.month == monthStart.month,
                        isSelected = cellDate == selectedDay,
                        isToday = cellDate == today,
                        eventCount = itemsByDate[cellDate] ?: 0,
                        onClick = { onOpenDay(cellDate) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    eventCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Simple grid cell — matches Week's day columns: a thin border, no fill,
    // no rounded corners. Identity (selected/today) is conveyed by text weight
    // and color, not by background tints.
    val numberColor = when {
        !inMonth -> AppTheme.colors.onSurfaceVariant.copy(alpha = 0.45f)
        isSelected -> AppTheme.colors.primary
        isToday -> AppTheme.colors.primary
        else -> AppTheme.colors.onSurface
    }
    val dotColor = if (inMonth) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant.copy(alpha = 0.30f)

    Box(
        modifier = modifier
            .border(width = 0.5.dp, color = AppTheme.colors.outlineVariant)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = AppTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = numberColor,
            modifier = Modifier.align(Alignment.TopStart),
        )
        if (eventCount > 0 && inMonth) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(6.dp)
                    .background(dotColor, CircleShape),
            )
        }
    }
}

@Composable
private fun EmptyState(scale: TimelineScale) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No blocks in this ${scale.name.lowercase(Locale.ROOT)} view. Create a tile to seed the timeline.",
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

private fun toDayBlocks(items: List<CoreTimelineItem>, day: LocalDate, zone: ZoneId): List<PlacedBlock> {
    val filtered = items.mapNotNull { item ->
        val s = parseInstantOrNull(item.startAt) ?: return@mapNotNull null
        val e = parseInstantOrNull(item.endAt) ?: s
        val sLocal = s.atZone(zone)
        val eLocal = e.atZone(zone)
        if (sLocal.toLocalDate() != day && eLocal.toLocalDate() != day) return@mapNotNull null
        val sMin = sLocal.hour * 60 + sLocal.minute
        val eMin = (eLocal.hour * 60 + eLocal.minute).coerceAtLeast(sMin + 15)
        PlacedBlock(
            id = item.id,
            title = item.title,
            type = item.type,
            status = item.status,
            startMinutes = sMin,
            endMinutes = eMin,
            laneIndex = 0,
            laneCount = 1,
        )
    }
    return assignLanes(filtered)
}

private fun assignLanes(blocks: List<PlacedBlock>): List<PlacedBlock> {
    val sorted = blocks.sortedBy { it.startMinutes }
    val lanes = mutableListOf<MutableList<PlacedBlock>>()
    val out = mutableListOf<PlacedBlock>()

    for (block in sorted) {
        var placed = false
        for ((i, lane) in lanes.withIndex()) {
            if (lane.maxOf { it.endMinutes } <= block.startMinutes) {
                lane.add(block)
                out.add(block.copy(laneIndex = i))
                placed = true
                break
            }
        }
        if (!placed) {
            lanes.add(mutableListOf(block))
            out.add(block.copy(laneIndex = lanes.size - 1))
        }
    }
    return out.map { block ->
        var concurrent = 0
        for (lane in lanes) {
            val overlaps = lane.any {
                it.startMinutes < block.endMinutes && it.endMinutes > block.startMinutes
            }
            if (overlaps) concurrent++
        }
        block.copy(laneCount = concurrent.coerceAtLeast(1))
    }
}

private fun filterForScale(
    items: List<CoreTimelineItem>,
    scale: TimelineScale,
    today: LocalDate,
    zone: ZoneId,
): List<CoreTimelineItem> {
    return when (scale) {
        TimelineScale.Day -> emptyList()
        TimelineScale.Week -> {
            val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong()).atStartOfDay(zone).toInstant()
            val weekEnd = weekStart.plusSeconds(7 * 24 * 60 * 60L)
            items.filter { item ->
                val start = parseInstantOrNull(item.startAt) ?: return@filter false
                val end = parseInstantOrNull(item.endAt) ?: start
                start.isBefore(weekEnd) && end.isAfter(weekStart)
            }
        }
        TimelineScale.Month -> {
            val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant()
            val monthEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant()
            items.filter { item ->
                val start = parseInstantOrNull(item.startAt) ?: return@filter false
                val end = parseInstantOrNull(item.endAt) ?: start
                start.isBefore(monthEnd) && end.isAfter(monthStart)
            }
        }
    }
}

private fun formatDuration(minutes: Long): String {
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h}h" else "${h}h ${m}m"
}

private fun parseInstantOrNull(value: String?): Instant? {
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
