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
import androidx.compose.ui.unit.dp

private val M3LightColorScheme = lightColorScheme(
    primary = TastileColors.BrandAccent,
    onPrimary = TastileColors.BrandAccentOn,
    primaryContainer = TastileColors.BrandTintLight,
    onPrimaryContainer = TastileColors.GrayBackground,
    background = TastileColors.LightBackground,
    onBackground = TastileColors.LightForeground,
    surface = TastileColors.LightSurface0,
    onSurface = TastileColors.LightForeground,
    surfaceVariant = TastileColors.LightSurface1,
    onSurfaceVariant = TastileColors.LightForegroundMuted,
    outline = TastileColors.LightOutline,
)

private val M3DarkColorScheme = darkColorScheme(
    primary = TastileColors.BrandAccent,
    onPrimary = TastileColors.BrandAccentOn,
    primaryContainer = TastileColors.BrandTintDark,
    onPrimaryContainer = TastileColors.GrayForeground,
    background = TastileColors.GrayBackground,
    onBackground = TastileColors.GrayForeground,
    surface = TastileColors.GraySurface2,
    onSurface = TastileColors.GrayForeground,
    surfaceVariant = TastileColors.GraySurface1,
    onSurfaceVariant = TastileColors.GrayForegroundMuted,
    outline = TastileColors.GrayOutline,
)

private val TastileShapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
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
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = themeMode == ThemeMode.LIGHT
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = themeMode == ThemeMode.LIGHT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = TastileShapes,
        typography = TastileTypography,
        content = content
    )
}
