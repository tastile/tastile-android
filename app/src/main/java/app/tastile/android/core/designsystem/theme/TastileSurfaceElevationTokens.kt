package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Per-level elevation slot. Bundles Material 3 `shadowElevation` (drop shadow)
 * with `tonalElevation` (surface tint applied to the surface color) so a
 * single token describes how a level "lifts" off the background.
 */
@Immutable
data class TastileElevationLevel(
    val shadowElevation: Dp,
    val tonalElevation: Dp,
)

/**
 * Elevation slots.
 *
 * The legacy `card / sheet / overlay` triple (3 ad-hoc `shadowElevation`
 * values used across `ui/dashboard/`) is preserved for backwards
 * compatibility. Phase 1 (Issue #100, release:0-6-0) appends the
 * `level0..level3` ladder that frame primitives (Issue #101) read for
 * `Surface` / `Card` / `ModalBottomSheet` / `Drawer` to enforce a single
 * elevation vocabulary.
 */
@Immutable
data class TastileSurfaceElevationTokens(
    val card: Dp,
    val sheet: Dp,
    val overlay: Dp,
    val level0: TastileElevationLevel = TastileElevationLevel(0.dp, 0.dp),
    val level1: TastileElevationLevel = TastileElevationLevel(1.dp, 1.dp),
    val level2: TastileElevationLevel = TastileElevationLevel(3.dp, 3.dp),
    val level3: TastileElevationLevel = TastileElevationLevel(6.dp, 6.dp),
) {
    companion object {
        val Default = TastileSurfaceElevationTokens(
            card = 1.dp,
            sheet = 3.dp,
            overlay = 6.dp,
            level0 = TastileElevationLevel(0.dp, 0.dp),
            level1 = TastileElevationLevel(1.dp, 1.dp),
            level2 = TastileElevationLevel(3.dp, 3.dp),
            level3 = TastileElevationLevel(6.dp, 6.dp),
        )
    }
}
