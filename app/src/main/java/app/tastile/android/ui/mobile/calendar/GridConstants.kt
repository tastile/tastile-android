package app.tastile.android.ui.mobile.calendar

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object GridConstants {
    val TIME_GUTTER_WIDTH: Dp = 48.dp
    val MIN_EVENT_HEIGHT_DP: Dp = 22.dp
    val WEEK_HEADER_HEIGHT: Dp = 52.dp
    const val DAY_START_HOUR: Int = 0
    const val DAY_END_HOUR: Int = 24
    const val ZOOM_MIN: Float = 1f
    const val ZOOM_MAX: Float = 6f
    const val ZOOM_DEFAULT: Float = 1f
    const val SCROLL_BUFFER_MIN: Int = 15
    const val MONTH_GRID_ROWS: Int = 6
    const val WEEK_DAYS: Int = 7
}