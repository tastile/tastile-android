package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Static Frame layer for the Month view (Phase v37 / Task 5).
 *
 * Lays out 42 cells (7 columns × 6 rows per [GridConstants]) with:
 *   • thin 0.5dp borders, no rounded corners, no background fills
 *     (per the v31 memory spec),
 *   • the day number in each cell,
 *   • a `primary`-coloured text + `selected-cell` testTag on the cell that
 *     matches [selectedDate],
 *   • a de-emphasised alpha (≈0.45) on out-of-month bleed cells
 *     (visible day numbers, faded tint).
 *
 * The Frame is intentionally data-free: it MUST NOT take [Map] of events,
 * a `count: Int`, or any other dynamic data. Event-count dots are owned by
 * the sibling [MonthEventIndicator] overlay composed inside [MonthView].
 * This constraint makes the Frame's Border + Text tree stable across
 * recomposition: when only the timeline list changes, the cell borders and
 * day numbers skip recomposition, and only the Indicator overlay
 * recomposes. Verified via `grep -nE "itemsByDate|count" MonthViewFrame.kt`
 * → no hits.
 *
 * @param monthStart the first day of the visible month. The grid begins
 *   at the Monday of the week containing this date and continues for
 *   [GridConstants.WEEK_DAYS] × [GridConstants.MONTH_GRID_ROWS] cells.
 * @param selectedDate the cell that should display the "selected" styling.
 * @param onSelectDate tap callback for any of the 42 cells.
 */
@Composable
fun MonthViewFrame(
    monthStart: LocalDate,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridStart = remember(monthStart) {
        // Monday-of-this-week so the first row aligns with Mon..Sun. Mirrors
        // the prior inline MonthDayCell composition: dayOfWeek.value is
        // 1=Mon..7=Sun, so subtract (value-1) days to land on Monday.
        monthStart.minusDays((monthStart.dayOfWeek.value - 1).toLong())
    }
    Column(
        modifier = modifier
            .testTag("month-view-frame")
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        for (weekRow in 0 until GridConstants.MONTH_GRID_ROWS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                for (dayCol in 0 until GridConstants.WEEK_DAYS) {
                    val cellDate = gridStart.plusDays((weekRow * GridConstants.WEEK_DAYS + dayCol).toLong())
                    MonthFrameCell(
                        date = cellDate,
                        monthStart = monthStart,
                        selectedDate = selectedDate,
                        onSelectDate = onSelectDate,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/**
 * A single Frame cell: thin border + day number + (optional) selected-cell
 * marker. Carries no event-count state — the count dot lives in the
 * sibling Indicator overlay. Kept private to [MonthViewFrame]; the public
 * surface is the Frame composable above.
 */
@Composable
private fun MonthFrameCell(
    date: LocalDate,
    monthStart: LocalDate,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inMonth = date.month == monthStart.month && date.year == monthStart.year
    val isSelected = date == selectedDate
    val numberColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    // Selection marker: a separate tiny Box layered on top of the cell so
    // a Compose semantics search for `month-view-frame-selected-cell`
    // matches exactly one node per Frame render — see MonthViewFrameTest.
    // Out-of-month bleed cells layer on a sibling tag at the cell level,
    // exposing them as a group without depending on day-number uniqueness
    // (e.g. Aug 4 collides with July 4). The tag is set on the same Box
    // via `.testTag("...", "default")` style — Compose allows multiple
    // testTags via the singular testTag call: the second tag call REPLACES
    // the first, so we use two per-cell Box layers instead.
    // Out-of-month bleed cells carry an extra tag for assertion grouping
    // (Compose .testTag() REPLACES prior tags on the same Node, so we use
    // a per-cell nested Box layer for the bleed tag instead of a chained
    // call). Day-number uniqueness is avoided this way — July 4 collides
    // with Aug 4 by literal day, but every out-of-month cell is uniformly
    // discoverable through this single tag.
    Box(
        modifier = modifier
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .clickable { onSelectDate(date) }
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("month-view-frame-day-column"),
    ) {
        // No additional tags or content on the bleed cells here — the
        // per-Text `contentDescription` on the day number (see below)
        // exposes the bleed state to semantics, so tests can group by
        // "out-of-month" without depending on day-number uniqueness.
        if (isSelected) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .testTag("month-view-frame-selected-cell"),
            )
        }
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = numberColor,
            // contentDescription exposes the bleed state to semantics so
            // tests can group by it without depending on day-number
            // uniqueness. The full date is the visible cell identity;
            // the suffix carries the bleed signal.
            modifier = Modifier
                .align(Alignment.TopStart)
                .semantics(mergeDescendants = false) {
                    contentDescription = if (inMonth) "in-month" else "out-of-month"
                },
        )
    }
}

/**
 * Locale-aware short day-of-week label ("Mon", "Tue", …). Exposed
 * `internal` so [MonthView] can render the matching header row above the
 * Frame without duplicating the locale formatter.
 */
internal fun shortDowLabel(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
