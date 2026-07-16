package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import app.tastile.android.core.designsystem.component.NiaFloatingActionButton
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.core.designsystem.component.NiaLoadingWheel
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.panels.ProjectsViewModel
import app.tastile.android.ui.mobile.calendar.GridConstants

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MIN_EVENT_HEIGHT_DP = 22.dp
private val WEEK_HEADER_HEIGHT = 52.dp
private const val PAGER_CENTER = 365
private const val PAGER_TOTAL = 731
private const val INITIAL_ZOOM = 1.5f  // day is 1.5× screen → always scrollable

// Total top-bar height the table-control rows must clear:
//   status bar + 56dp content (56.dp).
// MobileScaffold sets contentWindowInsets = WindowInsets(0) and ignores innerPadding,
// so consumers must pad themselves if they want to sit below the top bar.
@Composable
private fun topBarTotalHeight(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp

private data class PlacedBlock(
    val id: String,
    val tileId: String?,
    val sourceKind: Int?,
    val title: String,
    val type: String,
    val status: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val laneIndex: Int,
    val laneCount: Int,
)

@Composable
fun TimelineScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel,
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
) {
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val scale by viewModel.scale.collectAsStateWithLifecycle()
    val calendarMode by viewModel.calendarMode.collectAsStateWithLifecycle()
    val minimumDuration by viewModel.calendarMinimumDurationMinutes.collectAsStateWithLifecycle()
    val tileFilter by viewModel.tileFilter.collectAsStateWithLifecycle()
    val projectsState by projectsViewModel.state.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val zone = remember { ZoneId.systemDefault() }

    val activeTimeline = remember(timeline) {
        timeline
    }

    val onOpenDay: (LocalDate) -> Unit = { day ->
        viewModel.setSelectedDay(day)
        viewModel.setScale(TimelineScale.Day)
    }
    val onEditEvent: (CoreTimelineItem) -> Unit = { item ->
        when (val target = calendarEventTarget(item)) {
            is CalendarEventTarget.RecurringTile -> {
                viewModel.selectTile(target.tileId)
                overlay.show(Overlay.TileEdit(tileId = target.tileId))
            }
            is CalendarEventTarget.Placement -> {
                target.tileId?.let(viewModel::selectTile)
                overlay.show(Overlay.TileEdit(tileId = target.tileId, placementId = target.placementId))
            }
        }
    }

    // Per-scale zoom, hoisted above the HorizontalPager so swiping between
    // days / weeks doesn't reset it. Resets on scale change (Day/Week/Month)
    // because the gesture math is scale-specific.
    var dayZoom by remember { mutableFloatStateOf(INITIAL_ZOOM) }
    var weekZoom by remember { mutableFloatStateOf(INITIAL_ZOOM) }

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
    LaunchedEffect(selectedDay, scale) {
        val pageOffset = when (scale) {
            TimelineScale.Day, TimelineScale.List -> ChronoUnit.DAYS.between(today, selectedDay)
            TimelineScale.Week -> ChronoUnit.WEEKS.between(
                today.minusDays((today.dayOfWeek.value - 1).toLong()),
                selectedDay.minusDays((selectedDay.dayOfWeek.value - 1).toLong()),
            )
            TimelineScale.Month -> ChronoUnit.MONTHS.between(today.withDayOfMonth(1), selectedDay.withDayOfMonth(1))
        }
        val target = (PAGER_CENTER + pageOffset.toInt()).coerceIn(0, PAGER_TOTAL - 1)
        if (scale != TimelineScale.List && pagerState.currentPage != target) pagerState.scrollToPage(target)
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
            TimelineScale.List -> today.plusDays(offset)
        }
        if (target != selectedDay) viewModel.setSelectedDay(target)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading && timeline.isEmpty() && scale == TimelineScale.Day -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NiaLoadingWheel(contentDesc = "Loading")
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
                        zoom = dayZoom,
                        onZoomChange = { dayZoom = it },
                        onCreateAt = { hour, minute ->
                            val start = pageDay.atTime(hour, minute).atZone(zone).toInstant()
                            overlay.show(Overlay.QuickCreateAt(start.toString(), start.plusSeconds(60 * 60).toString()))
                        },
                        onEditEvent = onEditEvent,
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
                        zoom = weekZoom,
                        onZoomChange = { weekZoom = it },
                        onEditEvent = onEditEvent,
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
            scale == TimelineScale.List -> TimelineListView(activeTimeline, zone, onEditEvent)
            else -> EmptyState(scale)
        }

        CalendarToolbar(
            mode = calendarMode,
            minimumDuration = minimumDuration,
            onPrevious = { viewModel.moveCalendar(-1) },
            onNext = { viewModel.moveCalendar(1) },
            onToday = viewModel::goToCalendarToday,
            onMode = viewModel::setCalendarMode,
            onMinimumDuration = viewModel::setCalendarMinimumDuration,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topBarTotalHeight()),
        )

        CalendarFilterPanel(
            selectedDayLabel = selectedDay.toString(),
            workspaces = projectsState.workspaces,
            ownerIds = tileFilter.ownerIds,
            onSelectDate = { value ->
                runCatching { LocalDate.parse(value) }.getOrNull()?.let(viewModel::setSelectedDay)
            },
            onOwnerIdsChange = { ownerIds -> viewModel.setOwnerFilters(ownerIds) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topBarTotalHeight() + 76.dp),
        )

        // Quick-create FAB: bottom-right round + button. Sits on top of every
        // scale (Day / Week / Month) so the entry point is always discoverable
        // regardless of which view the user is on. `navigationBarsPadding` keeps
        // it clear of the system gesture bar on Android 15.
        NiaFloatingActionButton(
            onClick = { overlay.show(Overlay.QuickCreate) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun CalendarToolbar(
    mode: app.tastile.android.ui.dashboard.CalendarMode,
    minimumDuration: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onMode: (app.tastile.android.ui.dashboard.CalendarMode) -> Unit,
    onMinimumDuration: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationEnabled = app.tastile.android.ui.dashboard.canNavigateCalendar(mode)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", modifier = Modifier.testTag("calendar-previous").clickable(enabled = navigationEnabled, onClick = onPrevious))
            Text("Today", modifier = Modifier.testTag("calendar-today").clickable(onClick = onToday))
            Text("›", modifier = Modifier.testTag("calendar-next").clickable(enabled = navigationEnabled, onClick = onNext))
            app.tastile.android.ui.dashboard.CalendarMode.entries.forEach { candidate ->
                Text(
                    text = candidate.name,
                    color = if (candidate == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .testTag("calendar-mode-${candidate.name.lowercase()}")
                        .clickable { onMode(candidate) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Min", color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(0, 5, 15, 30).forEach { minutes ->
                Text(
                    text = if (minutes == 0) "Any" else "${minutes}m",
                    color = if (minutes == minimumDuration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .testTag("calendar-min-$minutes")
                        .clickable { onMinimumDuration(minutes) },
                )
            }
        }
    }
}

@Composable
private fun DayGrid(
    blocks: List<PlacedBlock>,
    day: LocalDate,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onCreateAt: (hour: Int, minute: Int) -> Unit,
    onEditEvent: (CoreTimelineItem) -> Unit,
) {
    val scrollState = rememberScrollState()
    val latestZoom by rememberUpdatedState(zoom)
    var pendingZoomScroll by remember { mutableStateOf<Int?>(null) }
    var pinchZoom by remember { mutableStateOf<Float?>(null) }
    var pinchTranslationY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(zoom, pendingZoomScroll) {
        pendingZoomScroll?.let { target ->
            withFrameNanos { }
            scrollState.scrollTo(target)
            pendingZoomScroll = null
            pinchZoom = null
            pinchTranslationY = 0f
        }
    }

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
    val nowMin = if (isToday) LocalTime.now().hour * 60 + LocalTime.now().minute else -1
    val showNowLine = nowMin in (startHour * 60)..(endHour * 60)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Gesture math (scrollState.value, pointer position.y) is in pixels,
        // so capture density here and convert inside the pinch handler.
        // Drawing layers treat pxPerMin as dp/min and multiply by density
        // inside their own DrawScope, so that path stays untouched.
        val density = LocalDensity.current.density
        val availableHeightPx: Float = maxHeight.value
        val pxPerMinBase: Float = availableHeightPx / totalMinutes
        // Floor: 24h fit on screen. pxPerMin never drops below this.
        val minPxPerMin: Float = pxPerMinBase
        val outlineColor = MaterialTheme.colorScheme.outlineVariant
        val nowLineColor = Color(0xFFEF5350)

        val effectiveZoom = pinchZoom ?: zoom
        val pxPerMin: Float = (pxPerMinBase * effectiveZoom).coerceAtLeast(minPxPerMin)
        val totalHeight: Dp = (pxPerMin * totalMinutes).dp
        val pxPerHour: Dp = (pxPerMin * 60).dp

        // Single scrollable container. The gutter, grid, blocks, and now-line
        // are siblings inside one Row, so they translate as one body — no
        // drift between guide lines, hour labels, and tiles.
        //
        // Gesture split:
        //  • 1-finger vertical drag  → verticalScroll claims, scrolls the day
        //  • 1-finger horizontal drag → falls through to parent HorizontalPager
        //    (page change)
        //  • 2-finger pinch          → pointerInput below claims, anchored on
        //    the pinch centroid
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        // Lock the anchor on the first stable 2-finger frame so
                        // (initialDistance, initialZoom, initialCentroidY) stays
                        // the reference for the whole gesture. With a per-frame
                        // centroid, the axis drifts as fingers move and the
                        // zoom feels unstable; an anchored axis is what the user
                        // expects when they pinch.
                        var initialDistance = 0f
                        var initialZoom = 0f
                        var initialScroll = 0
                        var initialCentroidY = 0f
                        var finalZoom = latestZoom
                        var finalScroll: Int? = null

                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size >= 2) {
                                val first = pressed[0]
                                val second = pressed[1]

                                // Always consume 2-finger events so verticalScroll
                                // and HorizontalPager don't fight the pinch.
                                event.changes.forEach { it.consume() }

                                // Skip the frame where a fresh finger just landed;
                                // its previousPosition is its initial touch point
                                // and any ratio computed against it is bogus.
                                val firstNew = first.changedToDown()
                                val secondNew = second.changedToDown()
                                if (firstNew || secondNew) continue

                                val currentDistance = (first.position - second.position).getDistance()
                                if (currentDistance <= 0f) continue

                                if (initialDistance == 0f) {
                                    // Anchor frame — capture the triplet.
                                    initialDistance = currentDistance
                                    initialZoom = latestZoom
                                    initialScroll = scrollState.value
                                    initialCentroidY = (first.position.y + second.position.y) / 2f
                                    finalZoom = initialZoom
                                    finalScroll = initialScroll
                                    pinchZoom = initialZoom
                                    pinchTranslationY = 0f
                                } else {
                                    // Compute new zoom from the absolute anchor.
                                    val rawFactor = currentDistance / initialDistance
                                    val newZoom = (initialZoom * rawFactor).coerceIn(GridConstants.ZOOM_MIN, GridConstants.ZOOM_MAX)
                                    val targetScroll = anchoredZoomScrollTarget(
                                        currentScrollPx = initialScroll,
                                        anchorYpx = initialCentroidY,
                                        oldPxPerMin = (pxPerMinBase * initialZoom).coerceAtLeast(minPxPerMin) * density,
                                        newPxPerMin = (pxPerMinBase * newZoom).coerceAtLeast(minPxPerMin) * density,
                                        totalMinutes = totalMinutes,
                                        viewportPx = availableHeightPx * density,
                                    )
                                    finalZoom = newZoom
                                    finalScroll = targetScroll
                                    pinchZoom = newZoom
                                    pinchTranslationY = (initialScroll - targetScroll).toFloat()
                                }
                            } else {
                                // A finger lifted — invalidate the anchor so the
                                // next 2-finger touch can establish a fresh one.
                                initialDistance = 0f
                            }
                        } while (event.changes.any { it.pressed })

                        finalScroll?.let { targetScroll ->
                            pendingZoomScroll = targetScroll
                            onZoomChange(finalZoom)
                        } ?: run {
                            pinchZoom = null
                            pinchTranslationY = 0f
                        }
                    }
                }
                .verticalScroll(scrollState),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
                    .graphicsLayer {
                        translationY = pinchTranslationY
                    },
            ) {
                Column(
                    modifier = Modifier
                        .width(GridConstants.TIME_GUTTER_WIDTH)
                        .height(totalHeight),
                ) {
                    TimeGutterContent(startHour, endHour, pxPerHour, totalHeight)
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(totalHeight),
                ) {
                    val canvasWidth = maxWidth
                    DayContentLayer(
                        blocks = blocks,
                        startHour = startHour,
                        pxPerMin = pxPerMin,
                        outlineColor = outlineColor,
                        nowLineColor = nowLineColor,
                        showNowLine = showNowLine,
                        nowMin = nowMin,
                        canvasWidth = canvasWidth,
                        onCreateAt = onCreateAt,
                        onEditEvent = onEditEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayContentLayer(
    blocks: List<PlacedBlock>,
    startHour: Int,
    pxPerMin: Float,
    outlineColor: Color,
    nowLineColor: Color,
    showNowLine: Boolean,
    nowMin: Int,
    canvasWidth: Dp,
    onCreateAt: (hour: Int, minute: Int) -> Unit,
    onEditEvent: (CoreTimelineItem) -> Unit,
) {
    val localDensity = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(pxPerMin, startHour) {
                detectTapGestures { tap ->
                    val minute = ((tap.y / localDensity.density) / pxPerMin).toInt().coerceIn(0, 1439)
                    onCreateAt(minute / 60, (minute % 60 / 15) * 15)
                }
            },
    ) {
        // Layer 1: hour grid lines (background). pxPerMin is in dp/min; the
        // Canvas DrawScope uses raw pixels, so multiply by density. This
        // matches the gutter's drawText and the block Modifier.offset so
        // every hour line lands at the same y on screen.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hours = 24
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

        // Layer 2: event blocks
        blocks.forEach { b ->
            val topDp: Dp = ((b.startMinutes - startHour * 60) * pxPerMin).dp
            val heightDp: Dp = ((b.endMinutes - b.startMinutes) * pxPerMin)
                .coerceAtLeast(MIN_EVENT_HEIGHT_DP.value).dp
            val laneWidth = canvasWidth / b.laneCount.coerceAtLeast(1)
            val laneX = laneWidth * b.laneIndex
            Box(
                modifier = Modifier
                    .offset(x = laneX, y = topDp)
                    .width(laneWidth)
                    .height(heightDp)
                    .padding(horizontal = 2.dp),
            ) {
                EventChip(b, onEditEvent)
            }
        }

        // Layer 3: now-line (drawn ON TOP of tiles)
        if (showNowLine) {
            val elapsed = nowMin - startHour * 60
            val nowY: Dp = (elapsed * pxPerMin).dp
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

@Composable
private fun TimeGutterContent(startHour: Int, endHour: Int, pxPerHour: Dp, totalHeight: Dp) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun EventChip(b: PlacedBlock, onEditEvent: (CoreTimelineItem) -> Unit) {
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
                onEditEvent(CoreTimelineItem(b.id, b.tileId, b.sourceKind, b.title, b.type, b.status, "", null))
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
                text = "$timeLabel · ${formatDuration(durationMin.toLong())} · $statusLabel",
                style = MaterialTheme.typography.labelSmall,
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
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onEditEvent: (CoreTimelineItem) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val eeeFormatter = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
    val endHourGlobal = 24
    // 15 min of empty buffer above and below the labeled 0–24 range so the
    // body has a small scroll cushion at the min-zoom floor (mirrors DayGrid).
    val SCROLL_BUFFER_MIN = 15
    val totalMinutesGlobal = endHourGlobal * 60 + SCROLL_BUFFER_MIN * 2

    // Pre-compute per-day blocks at the same composition level so all columns share one body row.
    val blocksByDay: List<List<PlacedBlock>> = remember(items, weekStart) {
        (0L until 7L).map { offset ->
            toDayBlocks(items, weekStart.plusDays(offset), zone)
        }
    }

    val scrollState = rememberScrollState()
    val latestZoom by rememberUpdatedState(zoom)
    var pendingZoomScroll by remember { mutableStateOf<Int?>(null) }
    var pinchZoom by remember { mutableStateOf<Float?>(null) }
    var pinchTranslationY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(zoom, pendingZoomScroll) {
        pendingZoomScroll?.let { target ->
            withFrameNanos { }
            scrollState.scrollTo(target)
            pendingZoomScroll = null
            pinchZoom = null
            pinchTranslationY = 0f
        }
    }

    // Top-padding clears the status bar + top bar so the day-of-week row hooks
    // under the header rather than disappearing behind it. The scrolling body
    // still occupies the column weight(1f), so anything that scrolls upward can
    // visually slide under the top bar's transparent lower half.
    Column(modifier = Modifier.fillMaxSize().padding(top = topBarTotalHeight())) {
        // Day header row — sits ABOVE the scrollable body, so it stays pinned
        // directly under the app's top bar regardless of vertical scroll.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WEEK_HEADER_HEIGHT)
                .background(MaterialTheme.colorScheme.background),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(GridConstants.TIME_GUTTER_WIDTH))
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
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Body: zoomable + vertically scrollable. Gesture split mirrors DayGrid:
        //  • 1-finger vertical drag  → verticalScroll claims
        //  • 1-finger horizontal drag → falls through to parent HorizontalPager
        //  • 2-finger pinch          → pointerInput below claims, anchored on
        //    the pinch centroid so the time under the fingers stays put
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Gesture math runs in pixels (scrollState.value, position.y);
            // capture density so the pinch handler can convert. Drawing
            // layers treat pxPerMin as dp/min and convert inside DrawScope.
            val density = LocalDensity.current.density
            val availableHeightPx: Float = maxHeight.value
            val pxPerMinBase: Float = availableHeightPx / totalMinutesGlobal
            val minPxPerMin: Float = pxPerMinBase
            val effectiveZoom = pinchZoom ?: zoom
            val pxPerMin: Float = (pxPerMinBase * effectiveZoom).coerceAtLeast(minPxPerMin)
            val totalHeight: Dp = (pxPerMin * totalMinutesGlobal).dp
            val pxPerHour: Dp = (pxPerMin * 60).dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .graphicsLayer {
                        translationY = pinchTranslationY
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            // Same locked-anchor pattern as DayGrid: capture
                            // (initialDistance, initialZoom, initialCentroidY)
                            // on the first stable 2-finger frame so the axis
                            // stays put while fingers move.
                            var initialDistance = 0f
                            var initialZoom = 0f
                            var initialScroll = 0
                            var initialCentroidY = 0f
                            var finalZoom = latestZoom
                            var finalScroll: Int? = null

                            do {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.size >= 2) {
                                    val first = pressed[0]
                                    val second = pressed[1]

                                    event.changes.forEach { it.consume() }

                                    val firstNew = first.changedToDown()
                                    val secondNew = second.changedToDown()
                                    if (firstNew || secondNew) continue

                                    val currentDistance = (first.position - second.position).getDistance()
                                    if (currentDistance <= 0f) continue

                                    if (initialDistance == 0f) {
                                        initialDistance = currentDistance
                                        initialZoom = latestZoom
                                        initialScroll = scrollState.value
                                        initialCentroidY = (first.position.y + second.position.y) / 2f
                                        finalZoom = initialZoom
                                        finalScroll = initialScroll
                                        pinchZoom = initialZoom
                                        pinchTranslationY = 0f
                                    } else {
                                        val rawFactor = currentDistance / initialDistance
                                        val newZoom = (initialZoom * rawFactor).coerceIn(GridConstants.ZOOM_MIN, GridConstants.ZOOM_MAX)
                                        val targetScroll = anchoredZoomScrollTarget(
                                            currentScrollPx = initialScroll,
                                            anchorYpx = initialCentroidY,
                                            oldPxPerMin = (pxPerMinBase * initialZoom).coerceAtLeast(minPxPerMin) * density,
                                            newPxPerMin = (pxPerMinBase * newZoom).coerceAtLeast(minPxPerMin) * density,
                                            totalMinutes = totalMinutesGlobal,
                                            viewportPx = availableHeightPx * density,
                                        )
                                        finalZoom = newZoom
                                        finalScroll = targetScroll
                                        pinchZoom = newZoom
                                        pinchTranslationY = (initialScroll - targetScroll).toFloat()
                                    }
                                } else {
                                    initialDistance = 0f
                                }
                            } while (event.changes.any { it.pressed })

                            finalScroll?.let { targetScroll ->
                                pendingZoomScroll = targetScroll
                                onZoomChange(finalZoom)
                            } ?: run {
                                pinchZoom = null
                                pinchTranslationY = 0f
                            }
                        }
                    },
            ) {
                // Time gutter (scrolls with body)
                Column(
                    modifier = Modifier
                        .width(GridConstants.TIME_GUTTER_WIDTH)
                        .height(totalHeight),
                ) {
                    WeekTimeGutter(
                        endHour = endHourGlobal,
                        pxPerHour = pxPerHour,
                        totalHeight = totalHeight,
                    )
                }
                // 7 day columns
                for (offset in 0L until 7L) {
                    val day = weekStart.plusDays(offset)
                    val blocks = blocksByDay[offset.toInt()]
                    WeekDayColumn(
                        blocks = blocks,
                        modifier = Modifier
                            .weight(1f)
                            .height(totalHeight),
                        isToday = day == today,
                        onOpenDay = { onOpenDay(day) },
                        onEditEvent = onEditEvent,
                        pxPerMin = pxPerMin,
                        endHour = endHourGlobal,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekTimeGutter(endHour: Int, pxPerHour: Dp, totalHeight: Dp) {
    // Canvas with pxPerHour.toPx() so the label y positions are pixel-aligned
    // with the grid-line y positions drawn in WeekDayColumn.
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    Canvas(
        modifier = Modifier
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

@Composable
private fun WeekDayColumn(
    blocks: List<PlacedBlock>,
    modifier: Modifier = Modifier,
    isToday: Boolean,
    onOpenDay: () -> Unit,
    onEditEvent: (CoreTimelineItem) -> Unit,
    pxPerMin: Float,
    endHour: Int,
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val nowLineColor = Color(0xFFEF5350)
    val nowMin = remember(isToday) {
        if (isToday) LocalTime.now().hour * 60 + LocalTime.now().minute else -1
    }

    Box(
        modifier = modifier
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .clickable { onOpenDay() },
    ) {
        // Hour grid lines. pxPerMin is in dp; Canvas DrawScope is in pixels,
        // so multiply by density to keep the lines at the same y as the
        // block Modifier.offset values.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pxPerMinPx = pxPerMin * density
            for (h in 0..endHour) {
                val y = h * pxPerMinPx * 60
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
        }
        // Event blocks (top + height in dp, matches Canvas pixels at this density)
        blocks.forEach { b ->
            val topDp: Dp = (b.startMinutes * pxPerMin).dp
            val heightDp: Dp = ((b.endMinutes - b.startMinutes) * pxPerMin)
                .coerceAtLeast(MIN_EVENT_HEIGHT_DP.value).dp
            // Full-width (no lane split on mobile; single column per day)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = topDp)
                    .height(heightDp)
                    .padding(horizontal = 1.dp),
            ) {
                EventChip(b, onEditEvent)
            }
        }
        // Now-line (drawn last = on top)
        if (isToday && nowMin in 0..(endHour * 60)) {
            val nowY: Dp = (nowMin * pxPerMin).dp
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

    // Top padding puts the day-of-week header row below the top bar
    // (see WeekView's equivalent comment for details).
    Column(modifier = Modifier.fillMaxSize().padding(top = topBarTotalHeight())) {
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
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dotColor = if (inMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)

    Box(
        modifier = modifier
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No blocks in this ${scale.name.lowercase(Locale.ROOT)} view. Create a tile to seed the timeline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun anchoredZoomScrollTarget(
    currentScrollPx: Int,
    anchorYpx: Float,
    oldPxPerMin: Float,
    newPxPerMin: Float,
    totalMinutes: Int,
    viewportPx: Float,
): Int {
    if (oldPxPerMin <= 0f || newPxPerMin <= 0f) return currentScrollPx
    val minutesAtAnchor = (currentScrollPx + anchorYpx) / oldPxPerMin
    val maxScroll = (newPxPerMin * totalMinutes - viewportPx).coerceAtLeast(0f)
    return (minutesAtAnchor * newPxPerMin - anchorYpx)
        .coerceIn(0f, maxScroll)
        .toInt()
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
            tileId = item.tileId,
            sourceKind = item.sourceKind,
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
        TimelineScale.List -> items.sortedBy { it.startAt }
    }
}

@Composable
private fun TimelineListView(items: List<CoreTimelineItem>, zone: ZoneId, onEditEvent: (CoreTimelineItem) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topBarTotalHeight(), start = 16.dp, end = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.sortedBy { it.startAt }.forEach { item ->
            val start = parseInstantOrNull(item.startAt)?.atZone(zone)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onEditEvent(item) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = start?.format(DateTimeFormatter.ofPattern("MMM d HH:mm", Locale.getDefault())) ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(item.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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
