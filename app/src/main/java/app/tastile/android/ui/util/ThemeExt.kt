package app.tastile.android.ui.util

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.tastile.android.data.repository.ThemeMode

/**
 * Maps [ThemeMode] to a boolean flag suitable for `NiaTheme(darkTheme = …)`.
 * SYSTEM follows the platform; LIGHT/DARK are explicit overrides.
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
 * Sets the appropriate light/dark appearance for the status + navigation bars.
 *
 * On API 35+ the system enforces edge-to-edge; setting bar background
 * colors is no longer honored (the deprecated `Window.statusBarColor` /
 * `Window.navigationBarColor` setters are silently ignored at runtime).
 * The insets controller is the only supported knob for the light/dark
 * icon contrast, which is what callers actually need.
 */
@Composable
fun SystemBarEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
