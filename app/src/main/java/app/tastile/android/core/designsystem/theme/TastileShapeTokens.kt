package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Rounded corner radius vocabulary. Bound to Material 3's small/medium/large/extraLarge
 * shape slots (xs -> extraSmall, s -> small, m -> medium, large -> large, xl -> extraLarge)
 * and to the M3 Expressive slots (largeIncreased, extraLargeIncreased, extraExtraLarge).
 */
@Immutable
data class TastileShapeTokens(
    val xs: Dp,
    val s: Dp,
    val m: Dp,
    val large: Dp,
    val xl: Dp,
    val largeIncreased: Dp = 20.dp,
    val extraLargeIncreased: Dp = 32.dp,
    val extraExtraLarge: Dp = 48.dp,
) {
    companion object {
        val Default = TastileShapeTokens(
            xs = 4.dp,
            s = 8.dp,
            m = 16.dp,
            large = 20.dp,
            xl = 28.dp,
            largeIncreased = 20.dp,
            extraLargeIncreased = 32.dp,
            extraExtraLarge = 48.dp,
        )
    }
}
