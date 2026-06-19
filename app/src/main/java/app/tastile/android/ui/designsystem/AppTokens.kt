package app.tastile.android.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AppSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

object AppShape {
    val panelRadius = 6.dp
    val chipRadius = 8.dp
    val panel = RoundedCornerShape(panelRadius)
    val chip = RoundedCornerShape(chipRadius)
}

object AppComponentSize {
    val buttonMinHeight = 48.dp
    val iconButton = 40.dp
    val avatar = 40.dp
}
