package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens consumed by dashboard and (later) by other screens.
 *
 * Phase 1 (Issue #100, release:0-6-0) adds the semantic aliases
 * `xxs / xxl / gutter / gutterCompact` to the existing numeric scale so
 * downstream code can name layout intent ("gutter", "compact gutter")
 * rather than encoding intent via a numeric literal (`l`, `m`). New fields
 * are appended with defaults; existing positional constructors and named
 * callers continue to compile unchanged.
 */
@Immutable
data class TastileSpacingTokens(
    val xs: Dp,
    val s: Dp,
    val m: Dp,
    val l: Dp,
    val xl: Dp,
    val xxs: Dp = 4.dp,
    val xxl: Dp = 32.dp,
    val gutter: Dp = 16.dp,
    val gutterCompact: Dp = 12.dp,
) {
    companion object {
        val Default = TastileSpacingTokens(
            xs = 4.dp,
            s = 8.dp,
            m = 12.dp,
            l = 16.dp,
            xl = 24.dp,
            xxs = 4.dp,
            xxl = 32.dp,
            gutter = 16.dp,
            gutterCompact = 12.dp,
        )
    }
}
