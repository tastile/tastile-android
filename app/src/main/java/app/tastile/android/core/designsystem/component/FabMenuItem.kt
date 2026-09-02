package app.tastile.android.core.designsystem.component

import androidx.compose.ui.graphics.vector.ImageVector

sealed class FabMenuItem {
    abstract val icon: ImageVector
    abstract val label: String

    data class Action(
        override val icon: ImageVector,
        override val label: String,
        val onClick: () -> Unit,
    ) : FabMenuItem()
}
