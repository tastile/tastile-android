package app.tastile.android.ui.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import app.tastile.android.data.repository.ThemeMode

val LocalAppTouchTarget = staticCompositionLocalOf<Dp> { 48.dp }

/** Pass concrete corner tokens here; the wrapper layer reads it in Phase M2. */
val LocalAppCornerRadius = staticCompositionLocalOf<CornerTokens> { DefaultCornerTokens }

interface CornerTokens { val small: Dp; val medium: Dp; val large: Dp }
object DefaultCornerTokens : CornerTokens {
    override val small  = 4.dp
    override val medium = 8.dp
    override val large  = 16.dp
}

// gray = "always M3 baseline, never dynamic" — uses Compose Material 3 defaults.
// The gray-mode label is for users who want to disable dynamic color explicitly;
// the palette itself is the M3 stock light/dark, not a custom zinc scheme.
private fun grayColorScheme(dark: Boolean) = if (dark) darkColorScheme() else lightColorScheme()

@Composable
fun TastileTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK  -> true
        else            -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    val cs = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        themeMode == ThemeMode.GRAY -> grayColorScheme(dark)
        else                        -> if (dark) darkColorScheme() else lightColorScheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = cs.background.toArgb()
            window.navigationBarColor = cs.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = cs,
        typography  = AppTypography,
        shapes      = AppShapes,
    ) {
        CompositionLocalProvider(
            LocalAppTouchTarget  provides 48.dp,
            LocalAppCornerRadius provides DefaultCornerTokens,
        ) { content() }
    }
}