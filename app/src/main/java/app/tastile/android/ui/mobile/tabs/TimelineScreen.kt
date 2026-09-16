package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.core.designsystem.component.FabMenuItem
import app.tastile.android.core.designsystem.component.TastileFabMenu
import app.tastile.android.data.timeline.TimelinePageKey
import app.tastile.android.data.timeline.TimelinePageSnapshot
import app.tastile.android.data.timeline.TimelineRefreshDirection
import app.tastile.android.data.timeline.normalizeTimelineAnchor
import app.tastile.android.data.timeline.normalizeTimelineOwnerIds
import app.tastile.android.data.timeline.timelineScopeFingerprint
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.calendar.DayView
import app.tastile.android.ui.mobile.calendar.MonthView
import app.tastile.android.ui.mobile.calendar.WeekView
import app.tastile.android.ui.mobile.calendar.toDayBlocks
import app.tastile.android.ui.timeline.TimelinePageUiState
import app.tastile.android.ui.timeline.TimelinePageViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.atomic.AtomicLong

private const val PAGER_CENTER = 365
private const val PAGER_TOTAL = 731
private const val INITIAL_ZOOM = 1.5f // day is 1.5x screen -> always scrollable

/**
 * Top-level timeline screen. It observes page-local snapshots rather than
 * the dashboard's legacy aggregate timeline list. The optional ViewModel
 * parameter is a test seam; the production path resolves it from Hilt.
 */
@Composable
fun TimelineScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel,
    pageViewModel: TimelinePageViewModel? = null,
) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val requestedScale by viewModel.scale.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    val resolvedPageViewModel = resolveTimelinePageViewModel(pageViewModel)

    if (resolvedPageViewModel == null) {
        // Plain ComponentActivity hosts used by isolated Compose tests do not
        // have a Hilt ViewModel factory. Keep the frame-first preview path
        // deterministic there; the app's @AndroidEntryPoint always resolves
        // the page ViewModel above.
        TimelineScreenContent(
            viewModel = viewModel,
            overlay = overlay,
            pageViewModel = null,
            pageState = TimelinePageUiState.initial(today = selectedDay),
            selectedDay = selectedDay,
            requestedScale = requestedScale,
            zone = zone,
        )
        return
    }

    val pageState by resolvedPageViewModel.uiState.collectAsStateWithLifecycle()
    val accountId = viewModel.timelineAccountId
    val tileFilter by viewModel.tileFilter.collectAsStateWithLifecycle()
    val selectedOwnerIds = normalizeTimelineOwnerIds(tileFilter.ownerIds)
    val scopeFingerprint = timelineScopeFingerprint(selectedOwnerIds)

    // Dashboard still owns the navigation shell's selected day and scale.
    // The page ViewModel owns the page cache/anchor state. A scale transition
    // deliberately calls setScale without copying selectedDay so each scale
    // retains its own anchor.
    LaunchedEffect(
        resolvedPageViewModel,
        accountId,
        requestedScale,
        selectedDay,
        selectedOwnerIds,
        scopeFingerprint,
        zone,
    ) {
        if (accountId.isNullOrBlank()) return@LaunchedEffect
        val current = resolvedPageViewModel.currentPageKey
        if (
            current == null ||
            current.accountId != accountId ||
            current.scopeFingerprint != scopeFingerprint ||
            current.normalizedOwnerIds != selectedOwnerIds ||
            current.zoneId != zone
        ) {
            resolvedPageViewModel.setContext(
                accountId = accountId,
                scopeFingerprint = scopeFingerprint,
                ownerIds = selectedOwnerIds,
                zoneId = zone,
                anchor = selectedDay,
                scale = requestedScale,
            )
            return@LaunchedEffect
        }

        if (current.scale != requestedScale) {
            resolvedPageViewModel.setScale(requestedScale)
            val retainedAnchor = resolvedPageViewModel.uiState.value.currentAnchor
            if (selectedDay != retainedAnchor) viewModel.setSelectedDay(retainedAnchor)
        } else {
            val desiredAnchor = normalizeTimelineAnchor(requestedScale, selectedDay)
            if (current.anchor != desiredAnchor) {
                resolvedPageViewModel.setAnchor(requestedScale, desiredAnchor)
            }
        }
    }

    TimelineScreenContent(
        viewModel = viewModel,
        overlay = overlay,
        pageViewModel = resolvedPageViewModel,
        pageState = pageState,
        selectedDay = selectedDay,
        requestedScale = requestedScale,
        zone = zone,
    )
}

@Composable
private fun resolveTimelinePageViewModel(
    explicit: TimelinePageViewModel?,
): TimelinePageViewModel? {
    if (explicit != null) return explicit
    // Existing JVM Compose tests intentionally host screens in a plain
    // ComponentActivity. They do not install Hilt, so resolving the optional
    // screen seam must not prevent those frame/FAB tests from rendering.
    return runCatching { hiltViewModel<TimelinePageViewModel>() }.getOrNull()
}

@Composable
private fun TimelineScreenContent(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel,
    pageViewModel: TimelinePageViewModel?,
    pageState: TimelinePageUiState,
    selectedDay: LocalDate,
    requestedScale: TimelineScale,
    zone: ZoneId,
) {
    val scale = if (pageViewModel == null) requestedScale else pageState.scale
    val today = remember { LocalDate.now() }
    val activePageKey = pageViewModel?.currentPageKey
    val baseKey = remember(pageViewModel, activePageKey, scale, selectedDay, zone, viewModel) {
        activePageKey?.takeIf { it.scale == scale }
            ?: TimelinePageKey(
                accountId = viewModel.timelineAccountId ?: "preview",
                scopeFingerprint = timelineScopeFingerprint(emptyList()),
                zoneId = zone,
                scale = scale,
                anchor = pageState.anchors[scale] ?: selectedDay,
            ).normalized()
    }

    val dayPagerState = rememberPagerState(
        initialPage = PAGER_CENTER,
        pageCount = { PAGER_TOTAL },
    )
    val weekPagerState = rememberPagerState(
        initialPage = PAGER_CENTER,
        pageCount = { PAGER_TOTAL },
    )
    val monthPagerState = rememberPagerState(
        initialPage = PAGER_CENTER,
        pageCount = { PAGER_TOTAL },
    )

    // A settled page becomes the new canonical anchor. Keep a generation token
    // for every scale/key context: a cancelled pager collector can still run
    // one callback when Compose switches scale, and that callback must not
    // overwrite the newly selected scale's anchor.
    val pagerForScale = when (scale) {
        TimelineScale.Day, TimelineScale.List -> dayPagerState
        TimelineScale.Week -> weekPagerState
        TimelineScale.Month -> monthPagerState
    }
    val generationCounter = remember { AtomicLong(0L) }
    val settleToken = remember(scale, baseKey) {
        TimelinePagerSettleToken(
            scale = scale,
            generation = generationCounter.incrementAndGet(),
            key = baseKey.normalized(),
        )
    }
    val currentSettleToken by rememberUpdatedState(settleToken)
    var lastRenderedScale by remember { androidx.compose.runtime.mutableStateOf(scale) }
    LaunchedEffect(scale, baseKey, pagerForScale) {
        // Each scale owns its pager position. Re-centering remains useful when
        // the current page anchor changes, but switching scale must preserve
        // that scale's retained pager state.
        val switchedScale = lastRenderedScale != scale
        lastRenderedScale = scale
        if (!switchedScale && pagerForScale.currentPage != PAGER_CENTER) {
            pagerForScale.scrollToPage(PAGER_CENTER)
        }
    }
    LaunchedEffect(settleToken, pagerForScale) {
        snapshotFlow { pagerForScale.currentPage to pagerForScale.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (page, isScrolling) ->
                if (isScrolling || page == PAGER_CENTER) return@collect
                if (!shouldApplyTimelinePagerSettle(settleToken, currentSettleToken)) {
                    return@collect
                }
                val offset = (page - PAGER_CENTER).toLong()
                val direction = if (offset > 0) {
                    TimelineRefreshDirection.Next
                } else {
                    TimelineRefreshDirection.Previous
                }
                val destination = shiftTimelineAnchor(baseKey, offset)
                pageViewModel?.onPageSettled(destination, direction)
                viewModel.setSelectedDay(destination)
            }
    }

    val onOpenDay = remember(viewModel, pageViewModel) {
        { day: LocalDate ->
            viewModel.setSelectedDay(day)
            viewModel.setScale(TimelineScale.Day)
            pageViewModel?.setScale(TimelineScale.Day)
            pageViewModel?.setAnchor(TimelineScale.Day, day)
            Unit
        }
    }
    val onEditEvent = remember(viewModel, overlay) {
        { item: CoreTimelineItem ->
            when (val target = calendarEventTarget(item)) {
                is CalendarEventTarget.RecurringTile -> {
                    viewModel.selectTile(target.tileId)
                    overlay.show(Overlay.TileEdit(tileId = target.tileId, sourceTileId = target.sourceTileId))
                }
                is CalendarEventTarget.Placement -> {
                    target.tileId?.let(viewModel::selectTile)
                    overlay.show(
                        Overlay.TileEdit(
                            tileId = target.tileId,
                            placementId = target.placementId,
                            sourceTileId = target.sourceTileId,
                        ),
                    )
                }
            }
        }
    }

    val dayZoom = pageState.zoomFor(TimelineScale.Day).takeIf { it > 0f } ?: INITIAL_ZOOM
    val weekZoom = pageState.zoomFor(TimelineScale.Week).takeIf { it > 0f } ?: INITIAL_ZOOM

    Box(modifier = Modifier.fillMaxSize()) {
        when (scale) {
            TimelineScale.Day -> TimelinePagePager(
                state = dayPagerState,
                baseKey = baseKey,
                pageState = pageState,
            ) { pageKey, snapshot ->
                val blocks = remember(snapshot.items, pageKey) {
                    toDayBlocks(snapshot.items, pageKey.anchor, pageKey.zoneId).toPersistentList()
                }
                DayView(
                    date = pageKey.anchor,
                    zoom = dayZoom,
                    blocks = blocks,
                    zone = pageKey.zoneId,
                    today = today,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(timelinePageTag(pageKey)),
                    onZoomChange = { zoom ->
                        if (pageViewModel != null) pageViewModel.setZoom(TimelineScale.Day, zoom)
                    },
                    onCreateAt = { hour, minute ->
                        val start = pageKey.anchor.atTime(hour, minute).atZone(pageKey.zoneId).toInstant()
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

            TimelineScale.Week -> TimelinePagePager(
                state = weekPagerState,
                baseKey = baseKey,
                pageState = pageState,
            ) { pageKey, snapshot ->
                WeekView(
                    items = snapshot.items,
                    weekStart = pageKey.anchor,
                    zone = pageKey.zoneId,
                    today = today,
                    onOpenDay = onOpenDay,
                    zoom = weekZoom,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(timelinePageTag(pageKey)),
                    onZoomChange = { zoom ->
                        if (pageViewModel != null) pageViewModel.setZoom(TimelineScale.Week, zoom)
                    },
                    onEditEvent = onEditEvent,
                )
            }

            TimelineScale.Month -> TimelinePagePager(
                state = monthPagerState,
                baseKey = baseKey,
                pageState = pageState,
            ) { pageKey, snapshot ->
                MonthView(
                    monthStart = pageKey.anchor,
                    selectedDate = selectedDay,
                    items = snapshot.items,
                    zone = pageKey.zoneId,
                    onOpenDay = onOpenDay,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(timelinePageTag(pageKey)),
                )
            }

            TimelineScale.List -> Unit
        }

        TastileFabMenu(
            mainIcon = Icons.Outlined.Add,
            mainLabel = stringResource(R.string.fab_create),
            expanded = false,
            onExpandedChange = {
                overlay.show(Overlay.QuickCreate)
            },
            items = listOf(
                FabMenuItem.Action(
                    icon = Icons.Outlined.Add,
                    label = "",
                    onClick = { overlay.show(Overlay.QuickCreate) },
                ),
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp)
                .testTag("quick-create-fab"),
        )
    }
}

/** Shared pager shell: every page receives exactly one immutable snapshot. */
@Composable
private fun TimelinePagePager(
    state: PagerState,
    baseKey: TimelinePageKey,
    pageState: TimelinePageUiState,
    pageContent: @Composable (TimelinePageKey, TimelinePageSnapshot) -> Unit,
) {
    HorizontalPager(
        state = state,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        // Pager saveable state keys must be Bundle-compatible on Android.
        // Keep the full TimelinePageKey for cache lookup, but expose its
        // canonical string identity to SaveableStateHolder.
        key = { page -> timelinePageTag(timelinePageKey(baseKey, page)) },
    ) { page ->
        val pageKey = timelinePageKey(baseKey, page)
        val snapshot = pageState.pages[pageKey]
            ?: TimelinePageSnapshot(
                key = pageKey,
                items = persistentListOf(),
            )
        pageContent(pageKey, snapshot)
    }
}

private fun timelinePageKey(baseKey: TimelinePageKey, page: Int): TimelinePageKey =
    baseKey.copy(anchor = shiftTimelineAnchor(baseKey, (page - PAGER_CENTER).toLong())).normalized()

private fun shiftTimelineAnchor(baseKey: TimelinePageKey, offset: Long): LocalDate = when (baseKey.scale) {
    TimelineScale.Day,
    TimelineScale.List,
    -> baseKey.anchor.plusDays(offset)

    TimelineScale.Week -> baseKey.anchor.plusWeeks(offset)
    TimelineScale.Month -> baseKey.anchor.plusMonths(offset)
}

private fun timelinePageTag(key: TimelinePageKey): String =
    "timeline-${key.scale.name.lowercase(Locale.ROOT)}-${key.normalizedAnchor}"

/** Identity captured by a pager settle collector. */
internal data class TimelinePagerSettleToken(
    val scale: TimelineScale,
    val generation: Long,
    val key: TimelinePageKey,
)

/** Rejects callbacks that belong to an old scale, generation, or page key. */
internal fun shouldApplyTimelinePagerSettle(
    candidate: TimelinePagerSettleToken,
    current: TimelinePagerSettleToken,
): Boolean = candidate.scale == current.scale &&
    candidate.generation == current.generation &&
    candidate.key.normalized() == current.key.normalized()

/**
 * Pure math helper. The same implementation lives as a private copy in
 * DayView.kt (used by the gesture handler); this public copy exists for
 * TimelineZoomMathTest, which calls it from the same package.
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
