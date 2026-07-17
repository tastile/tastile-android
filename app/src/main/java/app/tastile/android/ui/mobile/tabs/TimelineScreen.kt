package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import app.tastile.android.core.designsystem.component.NiaFloatingActionButton
// m2-allow: primitive
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.tastile.android.ui.mobile.calendar.DayView
import app.tastile.android.ui.mobile.calendar.MonthView
import app.tastile.android.ui.mobile.calendar.WeekView
import app.tastile.android.ui.mobile.calendar.toDayBlocks
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private const val PAGER_CENTER = 365
private const val PAGER_TOTAL = 731
private const val INITIAL_ZOOM = 1.5f  // day is 1.5× screen → always scrollable

/**
 * Top-level timeline screen. Hosts the HorizontalPager that switches
 * between Day / Week / Month views, the toolbar / filter panel, and the
 * quick-create FAB. The actual per-scale renderers live in the
 * `ui.mobile.calendar` package; this file only owns scale dispatch and
 * shell chrome.
 */
@Composable
fun TimelineScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel,
) {
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val scale by viewModel.scale.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val zone = remember { ZoneId.systemDefault() }
    val activeTimeline = remember(timeline) { timeline }

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
            scale == TimelineScale.Day -> HorizontalPager(
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
                        overlay.show(
                            Overlay.QuickCreateAt(
                                start.toString(),
                                start.plusSeconds(60 * 60).toString(),
                            ),
                        )
                    },
                    onEditEvent = onEditEvent,
                )
            }
            scale == TimelineScale.Week -> HorizontalPager(
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
            scale == TimelineScale.Month -> HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
            ) { page ->
                val pageMonthStart = today.plusMonths((page - PAGER_CENTER).toLong()).withDayOfMonth(1)
                MonthView(
                    monthStart = pageMonthStart,
                    selectedDate = selectedDay,
                    items = activeTimeline,
                    zone = zone,
                    onOpenDay = onOpenDay,
                )
            }
        }

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

/**
 * Pure math helper. The same implementation lives as a `private` copy in
 * DayView.kt (used by the gesture handler); this public copy exists for
 * [app.tastile.android.ui.mobile.tabs.TimelineZoomMathTest] which calls
 * it from the same package and verifies the anchor math invariants. Kept
 * verbatim so the test fixture does not change.
 */
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
