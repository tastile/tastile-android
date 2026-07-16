package app.tastile.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
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

private val M3GrayColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF71717A),
    onPrimary = Color(0xFFFAFAFA),
    primaryContainer = Color(0xFFE4E4E7),
    onPrimaryContainer = Color(0xFF18181B),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF18181B),
    surface = Color(0xFFF4F4F5),
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFE4E4E7),
    onSurfaceVariant = Color(0xFF52525B),
    outline = Color(0xFFD4D4D8),
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
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> M3LightColorScheme
        ThemeMode.GRAY -> M3GrayColorScheme
        ThemeMode.DARK, ThemeMode.SYSTEM, ThemeMode.BRAND -> M3DarkColorScheme
    }
    val isLightAppearance = themeMode != ThemeMode.DARK
    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightAppearance
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLightAppearance
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = TastileShapes,
        typography = TastileTypography,
        content = content
    )
}
