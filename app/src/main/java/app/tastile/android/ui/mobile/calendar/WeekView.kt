package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import app.tastile.android.core.CoreTimelineItem
import java.time.LocalDate
import java.time.ZoneId

/**
 * Top-level Week view (Phase v37 / Task 4).
 *
 * Owns the single shared vertical scroll state and routes data to:
 *   - [WeekHeaderRow] (pinned DOW header, overlaid on the body with a
 *     translucent gradient so the scrolled grid is visible through it,
 *     mirroring the Day top bar)
 *   - [WeekViewFrame] (static 7-column grid lines, scrolls with body)
 *   - [WeekViewTile] (per-day event chips + today's NowIndicator,
 *     overlaid on the Frame inside one weighted Box)
 *
 * Week uses the same anchored two-finger zoom behavior as Day while the
 * gutter, grid, and event tiles remain in one shared scroll space.
 *
 * Layout shape:
 *   BoxWithConstraints {
 *     Column(pointerInput, verticalScroll) {   // body: extends behind top bar
 *       Row(graphicsLayer) {
 *         TimeGutter
 *         Box {
 *           WeekViewFrame (grid lines)
 *           WeekViewTile    (event chips)
 *         }
 *       }
 *     }
 *     Row(align=TopStart, padding top for top bar) // pinned header only
 *       | Spacer over TimeGutter (transparent)
 *       | WeekHeaderRow (7 day columns)
 *   }
 */
@Composable
fun WeekView(
    items: List<CoreTimelineItem>,
    weekStart: LocalDate,
    zone: ZoneId,
    today: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onEditEvent: (CoreTimelineItem) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
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
    val blocksByDay = remember(items, weekStart, zone) {
        (0 until GridConstants.WEEK_DAYS).associate { offset ->
            val day = weekStart.plusDays(offset.toLong())
            day to toDayBlocks(items, day, zone)
        }
    }
    val totalMinutes = GridConstants.DAY_END_HOUR * 60 + GridConstants.SCROLL_BUFFER_MIN * 2
    val background = MaterialTheme.colorScheme.background

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current.density
        val viewportPx = maxHeight.value * density
        val effectiveZoom = pinchZoom ?: zoom
        val pxPerMin = computeWeekPxPerMin(zoom = effectiveZoom, totalMinutes = totalMinutes)
        val totalHeight: Dp = (pxPerMin * totalMinutes).dp
        // Body: gutter + Frame grid + Tile columns share one scroll.
        // It starts at the screen top so the top-bar gradient reveals the
        // timeline instead of a reserved background band.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .testTag("week-view-body")
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
                                    val newZoom = (initialZoom * currentDistance / initialDistance)
                                        .coerceIn(GridConstants.ZOOM_MIN, GridConstants.ZOOM_MAX)
                                    val targetScroll = anchoredWeekZoomScrollTarget(
                                        currentScrollPx = initialScroll,
                                        anchorYpx = initialCentroidY,
                                        oldPxPerMin = computeWeekPxPerMin(initialZoom, totalMinutes) * density,
                                        newPxPerMin = computeWeekPxPerMin(newZoom, totalMinutes) * density,
                                        totalMinutes = totalMinutes,
                                        viewportPx = viewportPx,
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
                WeekTimeGutter(
                    endHour = GridConstants.DAY_END_HOUR,
                    pxPerHour = (pxPerMin * 60).dp,
                    totalHeight = totalHeight,
                    modifier = Modifier.width(GridConstants.TIME_GUTTER_WIDTH),
                )
                // Frame + Tile overlay inside one weighted Box so the Tile gets
                // nonzero width (single-scroll multi-layer pattern, identical
                // to DayView's Scaffold body Box).
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(totalHeight),
                ) {
                    // Frame: 7-column hour grid (sibling of Tile in the Box)
                    WeekViewFrame(
                        modifier = Modifier.fillMaxSize(),
                        pxPerMin = pxPerMin,
                    )
                    // Tile: 7 columns of event chips + today's NowIndicator.
                    // Sibling of Frame so the chips translate with the Frame's
                    // grid under the shared scroll.
                    WeekViewTile(
                        weekStart = weekStart,
                        blocksByDay = blocksByDay,
                        pxPerMin = pxPerMin,
                        zone = zone,
                        today = today,
                        scrollState = scrollState,
                        onEditEvent = onEditEvent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        // Translucent WeekHeaderRow overlay — sibling of the body inside the
        // root BoxWithConstraints, aligned below the top bar. The gradient (0.65 → 0.35 → 0.0) is applied to the
            // 7-column area only, NOT the gutter, so the left time labels
            // stay visible right under the title bar — mirroring Day, where
            // the gutter is never masked.
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(top = topBarTotalHeight())
                    .height(GridConstants.WEEK_HEADER_HEIGHT),
            ) {
                // Transparent gutter column — no gradient here so the time
                // gutter labels (00, 01, 02, ...) show through cleanly, the
                // same way the Day gutter is never masked.
                Spacer(
                    modifier = Modifier
                        .width(GridConstants.TIME_GUTTER_WIDTH)
                        .height(GridConstants.WEEK_HEADER_HEIGHT),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to background.copy(alpha = 0.65f),
                                0.50f to background.copy(alpha = 0.35f),
                                1f to Color.Transparent,
                            ),
                        ),
                ) {
                    // 7 header text composables — slotted here to keep them
                    // visually aligned with the body columns. The
                    // `WeekViewFrame` composable separately owns the
                    // `week-view-frame` testTag for unit tests; the visible
                    // headers here are equivalent Composables.
                    WeekHeaderRow(
                        weekStart = weekStart,
                        today = today,
                        onOpenDay = onOpenDay,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
    }
}

/**
 * The DOW header row (7 day-of-week Text columns). Mirrors the
 * private header `Row` that lived in the inline `WeekView` from
 * TimelineScreen.kt — same shape, just lifted to a reusable function
 * so both the production view and the test render the same widget.
 */
@Composable
private fun WeekHeaderRow(
    weekStart: LocalDate,
    today: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        for (offset in 0 until GridConstants.WEEK_DAYS) {
            val day = weekStart.plusDays(offset.toLong())
            val isToday = day == today
            val label = remember(day) {
                day.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.getDefault()))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onOpenDay(day) }
                    .padding(vertical = 4.dp)
                    .testTag("week-view-pin-header-day-column"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isToday) FontWeight.Bold
                    else FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun anchoredWeekZoomScrollTarget(
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
 * Compute the week body's `pxPerMin` (dp/min) from the caller's zoom
 * value. The pinch gesture clamps zoom to [GridConstants.ZOOM_MIN] and
 * [GridConstants.ZOOM_MAX] before this helper lays out the shared body.
 */
internal fun computeWeekPxPerMin(zoom: Float, totalMinutes: Int): Float {
    val base = 1f
    return (base * zoom.coerceIn(GridConstants.ZOOM_MIN, GridConstants.ZOOM_MAX))
        .coerceAtLeast(0.1f)
}

/**
 * Shared time-gutter Composable for the Week body. Mirrors the prior
 * private `WeekTimeGutter` from TimelineScreen.kt so the labels stay
 * pixel-aligned with the grid lines drawn by [WeekViewFrame]. Kept
 * `internal` and colocated inside WeekView.kt so the v37 split has
 * exactly 3 new production files (per the design's file tree).
 */
@Composable
internal fun WeekTimeGutter(
    endHour: Int,
    pxPerHour: Dp,
    totalHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val padRight = 6.dp
    val measurements = rememberGutterMeasurements(textMeasurer, labelStyle, 0, endHour)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
    ) {
        val pxPerHourPx = pxPerHour.toPx()
        val padRightPx = padRight.toPx()
        for ((h, m) in measurements.withIndex()) {
            val yLine = h * pxPerHourPx
            // Clamp the top so the first label ("00") sits flush at the
            // canvas top instead of being half-clipped above it. For h>=1
            // yLine is well below the clamp so the label stays centered on
            // its grid line.
            val yTop = (yLine - m.size.height / 2f).coerceAtLeast(0f)
            drawText(
                textMeasurer = textMeasurer,
                text = "%02d".format(h),
                topLeft = Offset(
                    x = size.width - m.size.width - padRightPx,
                    y = yTop,
                ),
                style = labelStyle,
            )
        }
    }
}
