package app.tastile.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.tastile.android.data.repository.ThemeMode
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val M3LightColorScheme = lightColorScheme()
private val M3DarkColorScheme = darkColorScheme()

private val PlainShapes = Shapes(
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp)
)

@Composable
fun TastileTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = if (themeMode == ThemeMode.LIGHT) M3LightColorScheme else M3DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = themeMode == ThemeMode.LIGHT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = PlainShapes,
        typography = Typography,
        content = content
    )
}
