package app.tastile.android.ui.util

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.tastile.android.data.repository.ThemeMode

/**
 * Maps [ThemeMode] to a boolean flag suitable for `NiaTheme(darkTheme = …)`.
 * SYSTEM follows the platform; LIGHT/DARK are explicit; GRAY follows the platform
 * but will be paired with `disableDynamicTheming = true` at the call site.
 */
@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    else -> isSystemInDarkTheme()
}

/** Returns true when the platform supports Material You dynamic color (Android 12+). */
fun supportsDynamicColor(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Tints the status bar + navigation bar to [color] and sets the appropriate
 * light/dark appearance flag. Replaces the inline SideEffect that the old
 * `TastileTheme` used to do.
 */
@Composable
fun SystemBarEffect(color: Color, darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = color.toArgb()
            window.navigationBarColor = color.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
