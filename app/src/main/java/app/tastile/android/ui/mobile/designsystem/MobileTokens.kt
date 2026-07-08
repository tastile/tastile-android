package app.tastile.android.ui.mobile.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.designsystem.AppCorner

/**
 * Mobile-only design tokens. Anything shared with the desktop / tablet UI
 * lives in `app.tastile.android.ui.designsystem` instead.
 */
object MobileTokens {
    val topBarHeight = 56.dp
    val bottomBarHeight = 64.dp

    val sheetCornerRadius = AppCorner.large
    const val sheetMaxHeightFraction = 0.92f

    val iconHitTarget = 48.dp
    val iconVisualSize = 24.dp

    /**
     * Surface tint alphas. Every colored surface in the app picks one from
     * this table (or `Color.Transparent`) instead of an ad-hoc alpha — the
     * pre-unification values were 0.10 / 0.18 / 0.20 / 0.35 / 0.45 / 0.55 /
     * 0.70 picked by eye.
     */
    object SurfaceAlpha {
        const val subtle = 0.10f
        const val selected = 0.20f
        const val pressed = 0.14f
        const val container = 0.30f
        const val strongSelected = 0.55f
        const val scrim = 0.45f
        const val barFillMax = 1.0f
        const val barFillMid = 0.70f
    }

    /**
     * Tile-lifecycle + integration-state accents. Centralising means a
     * status pill and a status dot never disagree.
     */
    object Status {
        val ready = Color(0xFFC08A2B)
        val started = Color(0xFF0D8A72)
        val done = Color(0xFF6E6E6E)
        val interruption = Color(0xFFC34141)
        val primary = Color(0xFF5E6AD2)
    }
}
