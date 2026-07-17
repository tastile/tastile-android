package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Per-cell event-count indicator for the Month view (Phase v37 / Task 5).
 *
 * Contract (see [MonthEventIndicatorTest]):
 *   * `count == 0`     → renders nothing (no Box, no semantics node).
 *   * `count == 1`     → a 6dp dot.
 *   * `count in 2..3`  → a pill with the numeric label ("2" / "3").
 *   * `count > 3`      → an overflow pill with text "+N".
 *
 * The component is intentionally data-free from the Frame's perspective:
 * [MonthView] resolves `itemsByDate[date]` and calls this with a single
 * `count: Int`. When the timeline list changes, only cells whose count
 * actually changed recompose; cells with the same count skip the branch
 * entirely (Compose stability pass).
 *
 * No `itemsByDate`, `Map`, or `List` parameters — see
 * `grep -nE "itemsByDate|Map<" MonthEventIndicator.kt`.
 *
 * Marked `internal` to keep the public calendar surface focused on the
 * Frame + wrapper. [MonthView] calls it directly per cell.
 */
@Composable
internal fun MonthEventIndicator(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Box(modifier = modifier.testTag("month-event-indicator")) {
        when {
            count == 1 -> MonthDot()
            count in 2..3 -> MonthPill(text = count.toString())
            else -> MonthPill(text = "+$count")
        }
    }
}

/**
 * 6dp filled dot. Color follows the Month theme's primary so the dot
 * reads as a single event on the same accent as the selected-cell border.
 */
@Composable
private fun MonthDot() {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}

/**
 * Pill variant for `2..3` exact counts and `>3` overflow. Rounded 4dp
 * corners to read as a pill, with the count text inside. The text aligns
 * inside the pill via [Box] + [Alignment.Center] so single- and double-digit
 * overflows render consistently.
 */
@Composable
private fun MonthPill(text: String) {
    val pillColor = MaterialTheme.colorScheme.primary
    val onPillColor = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(pillColor)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = onPillColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                // Pin the color so a Compose theme override does not
                // collide with the pill background.
                color = onPillColor,
            ),
        )
    }
}
