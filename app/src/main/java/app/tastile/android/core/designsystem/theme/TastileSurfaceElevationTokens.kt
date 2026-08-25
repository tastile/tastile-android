package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation slots. Phase 1 mirrors current ad-hoc values used across
 * `ui/dashboard/` so visuals are preserved; Phase 2 may recalibrate.
 */
@Immutable
data class TastileSurfaceElevationTokens(
    val card: Dp,
    val sheet: Dp,
    val overlay: Dp,
) {
    companion object {
        val Default = TastileSurfaceElevationTokens(
            card = 1.dp,
            sheet = 3.dp,
            overlay = 6.dp,
        )
    }
}
