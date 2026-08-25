package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens consumed by dashboard and (later) by other screens.
 * Phase 1 adds the keys; existing literal values continue to live where
 * Phase 2 has not migrated them.
 */
@Immutable
data class TastileSpacingTokens(
    val xs: Dp,
    val s: Dp,
    val m: Dp,
    val l: Dp,
    val xl: Dp,
) {
    companion object {
        val Default = TastileSpacingTokens(
            xs = 4.dp,
            s = 8.dp,
            m = 12.dp,
            l = 16.dp,
            xl = 24.dp,
        )
    }
}
