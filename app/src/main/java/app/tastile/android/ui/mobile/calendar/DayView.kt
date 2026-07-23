package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import java.time.Instant
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tastile.android.core.CoreTimelineItem
import java.time.LocalDate
import java.time.ZoneId

/**
 * Internal model used by the Day-view Frame + Tile layout. Lifted here from
 * the former `private data class PlacedBlock` in TimelineScreen.kt so the
 * sibling Week view in that file can keep building it via [toDayBlocks].
 *
 * Lane geometry is pre-computed by [assignLanes] so the layout code does not
 * have to repeat the overlap-detection loop inline.
 */
data class PlacedBlock(
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

/**
 * Top-level Day view wrapper (Phase v37 / Task 3).
 *
 * Owns:
 *   • the pinch-zoom + vertical-scroll gesture state (translated out of
 *     the old private `DayGrid` to keep the framework-agnostic Frame/Tile
 *     composables reusable),
 *   • the single scrollable Column whose children are the gutter, the
 *     [DayViewFrame], and the [DayViewTile],
 *   • the pxPerMin math (24h-fit-on-screen floor + zoom-driven scale).
 *
 * DayViewFrame and DayViewTile are siblings inside the content Box and
 * render into the same vertical-translation space, so the Frame's grid
 * lines and the Tile's event blocks stay locked as the user scrolls.
 * Because the Frame holds no reference to `blocks`, Compose's stability
 * pass keeps its Canvas pinned when only the timeline list changes —
 * the canonical v37 recomposition benefit.
 */
@Composable
fun DayView(
    date: LocalDate,
    zoom: Float,
    blocks: List<PlacedBlock>,
    zone: ZoneId,
    today: LocalDate,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onCreateAt: (hour: Int, minute: Int) -> Unit,
    onEditEvent: (CoreTimelineItem) -> Unit,
    onZoomChange: (Float) -> Unit = {},
) {
    DayViewScaffold(
        blocks = blocks,
        day = date,
        today = today,
        zoom = zoom,
        zone = zone,
        scrollState = scrollState,
        onCreateAt = onCreateAt,
        onEditEvent = onEditEvent,
        onZoomChange = onZoomChange,
        modifier = modifier,
    )
}

/**
 * Concrete Day-view scaffold. Pinch + scroll live here so the Frame and
 * Tile are pure renderers of static / dynamic content respectively.
 */
@Composable
private fun DayViewScaffold(
    blocks: List<PlacedBlock>,
    day: LocalDate,
    today: LocalDate,
    zoom: Float,
    zone: ZoneId,
    scrollState: ScrollState,
    onCreateAt: (hour: Int, minute: Int) -> Unit,
    onEditEvent: (CoreTimelineItem) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier,
) {
    // Single per-minute now-instant source hoisted to the scaffold so the
    // tile's `LaunchedEffect` is keyed on `day` only — a stale lambda
    // default can no longer churn the coroutine on every recompose.
    // `nowInstant` is `null` whenever the visible day is not today; the
    // tile renders nothing in that case.
    val isToday = day == today
    var nowInstant by remember { mutableStateOf<Instant?>(if (isToday) Instant.now() else null) }
    LaunchedEffect(day) {
        if (!isToday) return@LaunchedEffect
        while (true) {
            delay(60_000L)
            nowInstant = Instant.now()
        }
    }
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

    // Day view always spans the full 24 hours so that (a) the user can
    // scroll through the entire day, (b) the min zoom
    // (pxPerMin = availableHeight / 1440) shows the whole day on one
    // screen without overflowing.
    val startHour = 0
    val endHour = 24
    // 15 min of empty space above and below the labeled 0–24 range so the
    // day has a small scrollable buffer at the min zoom floor.
    val totalMinutes = 24 * 60 + GridConstants.SCROLL_BUFFER_MIN * 2

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current.density
        val availableHeightPx: Float = maxHeight.value
        val pxPerMinBase: Float = availableHeightPx / totalMinutes
        val minPxPerMin: Float = pxPerMinBase

        val effectiveZoom = pinchZoom ?: zoom
        // Delegate the actual formula to the top-level pure helper so
        // [PxPerMinTest] can exercise it without spinning up Compose.
        val pxPerMin: Float = computePxPerMin(
            availableHeightPx = availableHeightPx,
            totalMinutes = totalMinutes,
            zoom = effectiveZoom,
        )
        val totalHeight: Dp = (pxPerMin * totalMinutes).dp
        val pxPerHour: Dp = (pxPerMin * 60).dp

        // Gesture math (scrollState.value, pointer y) is in pixels; the
        // gesture handler converts using `density`. The Frame's DrawScope
        // also multiplies by density so the two paths agree on raw pixels.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
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
                                    val newZoom = (initialZoom * rawFactor)
                                        .coerceIn(GridConstants.ZOOM_MIN, GridConstants.ZOOM_MAX)
                                    val targetScroll = anchoredZoomScrollTarget(
                                        currentScrollPx = initialScroll,
                                        anchorYpx = initialCentroidY,
                                        oldPxPerMin = (pxPerMinBase * initialZoom)
                                            .coerceAtLeast(minPxPerMin) * density,
                                        newPxPerMin = (pxPerMinBase * newZoom)
                                            .coerceAtLeast(minPxPerMin) * density,
                                        totalMinutes = totalMinutes,
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
                }
                .verticalScroll(scrollState),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
                    .graphicsLayer { translationY = pinchTranslationY },
            ) {
                // Time gutter (left of the grid). Static — labels do not
                // depend on the timeline list.
                Column(
                    modifier = Modifier
                        .width(GridConstants.TIME_GUTTER_WIDTH)
                        .height(totalHeight),
                ) {
                    DayGutter(startHour, endHour, pxPerHour, totalHeight)
                }

                // Content area: Frame and Tile are siblings inside one Box
                // and overlay each other so their vertical translation
                // stays identical (one scroll source → no drift).
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(totalHeight),
                ) {
                    // Frame — Canvas + tap. Lays out via the modifier chain
                    // so the Canvas fillMaxSize() inside it is exactly the
                    // same area as the Tile's fillMaxSize() below.
                    DayViewFrame(
                        pxPerMin = pxPerMin,
                        onCreateAt = onCreateAt,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Tile — blocks + NowIndicator overlay. Lane width
                    // is driven by fillMaxWidth() inside DayViewTile so
                    // the chips line up over the grid lines Frame drew.
                    DayViewTile(
                        blocks = blocks,
                        date = day,
                        pxPerMin = pxPerMin,
                        zone = zone,
                        onEditEvent = onEditEvent,
                        today = today,
                        nowInstant = nowInstant,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("day-view-tile-wrapper"),
                    )
                }
            }
        }
    }
}

/**
 * Static time-gutter Composable. Extracted from the prior `TimeGutterContent`
 * so the gutter is colocated with the rest of the Frame and so future
 * setters (e.g. localized hour formats, NTP-time vertical guide) have one
 * obvious home.
 */
@Composable
private fun DayGutter(startHour: Int, endHour: Int, pxPerHour: Dp, totalHeight: Dp) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val padRight = 6.dp
    val measurements = rememberGutterMeasurements(textMeasurer, labelStyle, startHour, endHour)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight),
    ) {
        // Use pxPerHour.toPx() so the label y positions are pixel-aligned
        // with the grid-line y positions in DayViewFrame. drawText centers
        // glyphs at the given y via size.height / 2 in the offset.
        val pxPerHourPx = pxPerHour.toPx()
        val padRightPx = padRight.toPx()
        for ((h, m) in measurements.withIndex()) {
            val yLine = (h - startHour) * pxPerHourPx
            drawText(
                textMeasurer = textMeasurer,
                text = "%02d".format(startHour + h),
                topLeft = Offset(
                    x = size.width - m.size.width - padRightPx,
                    y = yLine - m.size.height / 2f,
                ),
                style = labelStyle,
            )
        }
    }
}

/**
 * Compute the new scroll position that keeps the centroid of a pinch zoom
 * anchored on the same minute-of-day. Mirrors the helper previously named
 * `anchoredZoomScrollTarget` inside TimelineScreen.kt; the function is
 * structurally identical and lifts verbatim — only the file location moves.
 */
private fun anchoredZoomScrollTarget(
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

/**
 * Pure-function formulation of the day-view `pxPerMin` math. Lifted out of
 * the Compose `[DayViewScaffold]` body so [PxPerMinTest] can lock the
 * invariants without spinning up a Compose runtime:
 *
 *  - At `zoom = ZOOM_DEFAULT` and a 24h-fit-available viewport the value
 *    equals `availableHeightPx / totalMinutes` (the v34 baseline that
 *    "the whole day fits on one screen at min zoom").
 *  - The result is `coerceAtLeast(minPxPerMin)` so zooming below the
 *    baseline never makes the day overflow the viewport.
 *  - Zoom is clamped to `[ZOOM_MIN, ZOOM_MAX]` so an out-of-range gesture
 *    cannot invalidate the day-range `0..24` invariant.
 *
 * The day range is forced via the composable site (`startHour = 0`,
 * `endHour = 24`) rather than via this function — `totalMinutes = 1440 +
 * 2 * SCROLL_BUFFER_MIN` keeps the 15-min buffer above and below the
 * labeled range (matches the existing v34 behavior).
 *
 * `density` is intentionally omitted: the helper operates in raw px so
 * the composable caller can multiply by `LocalDensity.current.density`
 * once for the DrawScope and once for the gesture path, matching the
 * existing pixel-alignment logic.
 */
internal fun computePxPerMin(
    availableHeightPx: Float,
    totalMinutes: Int,
    zoom: Float,
): Float {
    val zoomClamped = zoom.coerceIn(GridConstants.ZOOM_MIN, GridConstants.ZOOM_MAX)
    val pxPerMinBase = if (totalMinutes <= 0) 0f else availableHeightPx / totalMinutes
    return (pxPerMinBase * zoomClamped).coerceAtLeast(pxPerMinBase)
}

/**
 * Map raw [CoreTimelineItem]s onto placed day-blocks. Stays `internal` so
 * the Week view in `TimelineScreen.kt` can keep calling it.
 */
internal fun toDayBlocks(
    items: List<CoreTimelineItem>,
    day: LocalDate,
    zone: ZoneId,
): List<PlacedBlock> {
    val filtered = items.mapNotNull { item ->
        val s = parseInstantOrNull(item.startAt) ?: return@mapNotNull null
        val e = parseInstantOrNull(item.endAt) ?: s
        val sLocal = s.atZone(zone)
        val eLocal = e.atZone(zone)
        if (sLocal.toLocalDate() != day && eLocal.toLocalDate() != day) {
            return@mapNotNull null
        }
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

/**
 * Greedy overlap-aware lane assignment. O(N²) on `lanes` enumeration; kept
 * here to preserve the prior behavior (laneIndex / laneCount pre-computed
 * so the chip layout is a pure function of the Block, no per-render
 * overlap work).
 */
internal fun assignLanes(blocks: List<PlacedBlock>): List<PlacedBlock> {
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

