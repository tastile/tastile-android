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
import app.tastile.android.ui.mobile.calendar.DayView
import app.tastile.android.ui.mobile.calendar.EventChipContent
import app.tastile.android.ui.mobile.calendar.GridConstants
import app.tastile.android.ui.mobile.calendar.NowIndicator
import app.tastile.android.ui.mobile.calendar.PlacedBlock
import app.tastile.android.ui.mobile.calendar.WeekView
import app.tastile.android.ui.mobile.calendar.toDayBlocks

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                    DayView(
                        date = pageDay,
                        zoom = dayZoom,
                        blocks = pageBlocks,
                        zone = zone,
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
