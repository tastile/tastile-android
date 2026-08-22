package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable, scrollable chip row for QuickCreate panels.
 *
 * Contract:
 *  - **Full sheet width viewport**: the scrollable row spans the whole
 *    sheet width (no `weight` / fixed-width side gutters), so the scroll
 *    viewport runs from the sheet's left edge to its right edge.
 *  - **Leading 56dp reservation is part of the scroll content**: a
 *    leading `Spacer` (16dp body padding + 24dp icon column + 16dp gap)
 *    sits *inside* `horizontalScroll`. At scroll offset 0 the first
 *    chip's left edge lands exactly on the body's content start,
 *    matching `FormRow` / `FormFieldRow`. Because the reservation scrolls
 *    with the chips (like the web's `pl-8` inside `overflow-x-auto`),
 *    scrolling right moves the whole strip smoothly: the first chip
 *    slides past 56dp and off the sheet's left edge instead of being
 *    sliced mid-chip against a fixed gutter.
 *  - **Composable row of chips**: each chip is whatever the caller
 *    provides; this composable owns only the layout, gutter, and scroll
 *    state. Use this for workflow pickers, day-of-week selectors, or
 *    any other horizontally-scrolling chip group.
 *  - **Starts at scroll 0 on every open**: the scroll offset is held in
 *    plain [remember], NOT `rememberSaveable`, so a stale offset from a
 *    previous session/layout cannot restore mid-scrolled. The row always
 *    opens showing the first chip flush against the body's content
 *    start. (The offset still survives recomposition / chip selection
 *    within the session.)
 *  - **Preserves scroll position** across recompositions and
 *    selections via [remember] so tapping a chip does not reset the
 *    user's horizontal position.
 *
 * This is the panel-wide replacement for the inline `WorkflowBatch`
 * implementation that used `FormFieldLayout { ... }` and ran into the
 * 24+16=40dp icon column dead zone on the left.
 *
 * @param chips The composable that emits the chips. Usually a Row
 *   containing several chip composables. The row's intrinsic width
 *   is what determines how far the user can scroll.
 * @param spacing Horizontal gap between chips (defaults to 8dp,
 *   matching the original `WorkflowBatch` chip spacing).
 * @param scrollState The scroll state. Defaults to a fresh one that
 *   starts at 0 and is not saveable (see contract above). Pass a state
 *   hoisted above the panel composition (e.g. the one owned by
 *   [QuickCreatePanelContent]) to preserve the offset when the host
 *   panel swaps in and out of composition — e.g. when the user taps a
 *   different workflow chip.
 * @param modifier Modifier applied to the outer row.
 */
@Composable
fun ScrollableChipRow(
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp = 8.dp,
    scrollState: ScrollState = remember { ScrollState(0) },
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    chips: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight),
        verticalAlignment = verticalAlignment,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = verticalAlignment,
        ) {
            // Leading content reservation INSIDE the scroll so it travels
            // with the chips: at offset 0 the first chip starts at
            // 16 + 24 + 16 = 56dp (the body's content start), and when
            // the user scrolls right the whole strip — reservation and
            // chips — moves together, letting the first chip slide off
            // the sheet's left edge instead of clipping mid-chip.
            // `Arrangement.spacedBy(spacing)` already inserts one `spacing`
            // gap after the leading spacer, so the spacer itself is
            // LeadingContentOffset - spacing to land the first chip on 56dp.
            Spacer(modifier = Modifier.width(LeadingContentOffset - spacing))
            chips()
            // Trailing 16dp gutter so the last chip can scroll flush
            // against the right sheet edge instead of clipping mid-pixel.
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

/**
 * Content offset for chrome-less rows: 16dp outer padding + 24dp icon
 * column + 16dp icon/content gap. Must stay in sync with `FormRow` /
 * `FormFieldRow` so chip rows start where icon-bearing rows' content
 * begins.
 */
private val LeadingContentOffset: androidx.compose.ui.unit.Dp = 56.dp
