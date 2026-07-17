package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag

/**
 * Static Frame half of the Day view (Phase v37 / Task 3.5).
 *
 * Renders the **non-timeline** layer of the Day grid: the canvas of 25
 * horizontal hour grid lines and the tap-to-create gesture that converts a
 * tap-y in pixels to a (hour, 15-minute) bucket and emits via [onCreateAt].
 *
 * Deliberately holds **no reference** to `blocks` or to time-keeping state
 * (no `date`, no `zone`, no `scrollState`) — when the upstream timeline list
 * changes, Compose's stability pass keeps the Frame's Canvas pinned (no
 * recomposition, no re-draw), so the only dynamic layer affected is the
 * sibling [DayViewTile].
 *
 * Sizing is dictated by the modifier passed in by [DayView]; this composable
 * assumes it fills the content column to the right of the time gutter.
 */
@Composable
fun DayViewFrame(
    pxPerMin: Float,
    onCreateAt: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outlineColor: Color = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current.density

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(pxPerMin) {
                detectTapGestures { tap ->
                    val minute = ((tap.y / density) / pxPerMin).toInt().coerceIn(0, 1439)
                    onCreateAt(minute / 60, (minute % 60 / 15) * 15)
                }
            },
    ) {
        // Hour-grid Canvas. pxPerMin is in dp/min; DrawScope uses raw
        // pixels, so multiply by density. This matches the gutter's
        // drawText y-positions in DayView, so every hour line lands on
        // the same y as the "08" / "12" / "16" label rendered to its left.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("day-view-frame-grid-lines"),
        ) {
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
    }
}
