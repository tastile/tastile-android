package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Rounded corner radius vocabulary. Bound to Material 3's small/medium/large/extraLarge
 * shape slots (xs -> extraSmall, s -> small, m -> medium, large -> large, xl -> extraLarge).
 */
@Immutable
data class TastileShapeTokens(
    val xs: Dp,
    val s: Dp,
    val m: Dp,
    val large: Dp,
    val xl: Dp,
) {
    companion object {
        val Default = TastileShapeTokens(
            xs = 4.dp,
            s = 8.dp,
            m = 16.dp,
            large = 20.dp,
            xl = 28.dp,
        )
    }
}
