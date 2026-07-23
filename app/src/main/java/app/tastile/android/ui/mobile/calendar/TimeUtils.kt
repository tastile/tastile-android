package app.tastile.android.ui.mobile.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import java.time.Instant

internal fun parseInstantOrNull(value: String?): Instant? {
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

/**
 * Measure each hour label once for a [style]; re-use the resulting
 * [TextLayoutResult] list across frames instead of re-measuring every
 * redraw. The list identity is stable so Compose's recompose tracker
 * does not flag `drawText` calls as new work.
 */
@Composable
internal fun rememberGutterMeasurements(
    measurer: TextMeasurer,
    style: TextStyle,
    startHour: Int,
    endHour: Int,
): List<TextLayoutResult> {
    val labels = remember(startHour, endHour) {
        (startHour..endHour).map { "%02d".format(it) }
    }
    return remember(measurer, style, labels) {
        labels.map { measurer.measure(it, style) }
    }
}
