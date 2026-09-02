package app.tastile.android.core.designsystem.component

import androidx.compose.ui.graphics.vector.ImageVector

data class ButtonGroupItem(
    val icon: ImageVector? = null,
    val label: String,
    val enabled: Boolean = true,
)
