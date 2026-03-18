package app.tastile.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.tastile.android.data.repository.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = TastileColors.LightPrimary,
    onPrimary = TastileColors.LightOnPrimary,
    background = TastileColors.LightBackground,
    surface = TastileColors.LightSurface2,
    surfaceContainer = TastileColors.LightSurface1,
    surfaceContainerLow = TastileColors.LightSurface0,
    surfaceContainerHigh = TastileColors.LightSurface2,
    onSurface = TastileColors.LightForeground,
    onSurfaceVariant = TastileColors.LightForegroundMuted,
    outline = TastileColors.LightOutline
)

private val GrayColorScheme = darkColorScheme(
    primary = TastileColors.GrayPrimary,
    onPrimary = TastileColors.GrayOnPrimary,
    background = TastileColors.GrayBackground,
    surface = TastileColors.GraySurface2,
    surfaceContainer = TastileColors.GraySurface1,
    surfaceContainerLow = TastileColors.GraySurface0,
    surfaceContainerHigh = TastileColors.GraySurface2,
    onSurface = TastileColors.GrayForeground,
    onSurfaceVariant = TastileColors.GrayForegroundMuted,
    outline = TastileColors.GrayOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = TastileColors.DarkPrimary,
    onPrimary = TastileColors.DarkOnPrimary,
    background = TastileColors.DarkBackground,
    surface = TastileColors.DarkSurface2,
    surfaceContainer = TastileColors.DarkSurface1,
    surfaceContainerLow = TastileColors.DarkSurface0,
    surfaceContainerHigh = TastileColors.DarkSurface2,
    onSurface = TastileColors.DarkForeground,
    onSurfaceVariant = TastileColors.DarkForegroundMuted,
    outline = TastileColors.DarkOutline
)

@Composable
fun TastileTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.GRAY -> GrayColorScheme
        ThemeMode.DARK -> DarkColorScheme
    }
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
        typography = Typography,
        content = content
    )
}
