package app.tastile.android.ui.mobile.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object MobileTokens {
    val topBarHeight = 56.dp
    val bottomBarHeight = 64.dp

    val sheetCornerRadius = 12.dp
    const val sheetScrimAlpha = 0.45f
    const val sheetMaxHeightFraction = 0.92f

    val iconHitTarget = 48.dp
    val iconVisualSize = 24.dp

    object Status {
        val ready = Color(0xFFC08A2B)
        val started = Color(0xFF0D8A72)
        val done = Color(0xFF6E6E6E)
        val interruption = Color(0xFFC34141)
        val primary = Color(0xFF5E6AD2)
    }
}
