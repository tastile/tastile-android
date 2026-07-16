package app.tastile.android.ui.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

object AppTheme {
    val spacing = AppSpacing

    val shape = AppShape

    val component = AppComponentSize

    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    val colors: ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme
}
