package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.LocalTastileCardRoleTokens
import java.time.LocalDate

/**
 * Static Frame for the Week view (Phase v37 / Task 4).
 *
 * Owns the grid-lines Canvas of the Week body. The DOW header row
 * (`Mon..Sun` + day numbers) is owned by `WeekView`'s pinned `WeekHeaderRow`
 * so it stays above the scroll area — the Frame's only job here is to
 * render the 25 hour lines so the event chips in [WeekViewTile] have a
 * grid to align against.
 *
 * Holds ZERO references to `PlacedBlock` / `blocks` so Compose's stability
 * pass keeps its Canvas pinned when the timeline list changes. Event
 * chips and the NowIndicator live in [WeekViewTile].
 */
@Composable
fun WeekViewFrame(
    modifier: Modifier = Modifier,
    pxPerMin: Float,
) {
    val outlineColor = LocalTastileCardRoleTokens.current.completed.border

    Column(modifier = modifier.testTag("week-view-frame").fillMaxSize()) {
        // Hour-grid Canvas. Fills the rest of the Frame's height; the
        // WeekView host constrains the total height to
        // (pxPerMin × totalMinutes).dp so the 25 lines span the 24h
        // scrollable body. Modifier.fillMaxSize() makes the grid
        // cover the full 7 columns.
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(width = 0.5.dp, color = outlineColor)
                .testTag("week-view-frame-grid-lines"),
        ) {
            drawPoints(
                pointMode = PointMode.Lines,
                points = buildGridPoints(
                    size.width,
                    pxPerMin * density,
                    GridConstants.DAY_END_HOUR,
                ),
                color = outlineColor,
                strokeWidth = 1f,
            )
        }
    }
}
