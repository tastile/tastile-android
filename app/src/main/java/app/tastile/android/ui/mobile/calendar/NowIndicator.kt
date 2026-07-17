package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Vertical "now" indicator rendered on top of the day grid.
 *
 * Positions a red dot + horizontal line at the minute corresponding to
 * [nowProvider] relative to the visible day range, scaled by [pxPerMin]
 * (dp/min). The Instant from [nowProvider] is converted to minutes-of-day
 * via [zone] so the indicator lines up with the user's wall clock, not UTC.
 * Renders nothing when [nowProvider] returns null.
 */
@Composable
fun NowIndicator(
    nowProvider: () -> java.time.Instant?,
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    pxPerMin: Float,
    dayRangeStartHour: Int,
    dayRangeEndHour: Int,
    modifier: Modifier = Modifier,
) {
    val now = nowProvider() ?: return
    val localTime = now.atZone(zone).toLocalTime()
    val minutesOfDay = localTime.hour * 60 + localTime.minute
    val pxPerMinEff = pxPerMin.coerceAtLeast(0.0001f)
    val nowY = ((minutesOfDay - dayRangeStartHour * 60) * pxPerMinEff).dp
    Box(modifier) {
        Box(
            modifier = Modifier
                .offset(y = nowY - 5.dp)
                .size(10.dp)
                .background(Color.Red, CircleShape)
                .testTag("now-indicator-dot"),
        )
        Box(
            modifier = Modifier
                .offset(y = nowY - 1.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.Red)
                .testTag("now-indicator-line"),
        )
    }
}
