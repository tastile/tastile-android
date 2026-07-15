package app.tastile.android.ui.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

/**
 * Light "Gray" palette (Tailwind's `gray-*` / `zinc-*` family) used when
 * [app.tastile.android.data.repository.ThemeMode.GRAY] is selected.
 * Surface is intentionally near-white (#F4F4F5) — web uses the same
 * token so the android shell matches app.tastile.app.
 */
object GrayColors {
    val surface = Color(0xFFF4F4F5)
    val onSurface = Color(0xFF18181B)
    val background = Color(0xFFFAFAFA)
    val onBackground = Color(0xFF18181B)
    val surfaceVariant = Color(0xFFE4E4E7)
    val onSurfaceVariant = Color(0xFF52525B)
    val outline = Color(0xFFD4D4D8)
    val primary = Color(0xFF71717A)
    val onPrimary = Color(0xFFFAFAFA)
    val primaryContainer = Color(0xFFE4E4E7)
    val onPrimaryContainer = Color(0xFF18181B)
}
