package app.tastile.android.ui.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

private fun grayColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = Color(0xFFA1A1AA),
        onPrimary = Color(0xFF18181B),
        primaryContainer = Color(0xFF52525B),
        onPrimaryContainer = Color(0xFFE4E4E7),
        secondary = Color(0xFF71717A),
        onSecondary = Color(0xFFE4E4E7),
        tertiary = Color(0xFF71717A),
        onTertiary = Color(0xFFE4E4E7),
        error = Color(0xFFC34141),
        onError = Color(0xFFFFFFFF),
        background = Color(0xFF18181B),
        onBackground = Color(0xFFE4E4E7),
        surface = Color(0xFF18181B),
        onSurface = Color(0xFFE4E4E7),
        surfaceVariant = Color(0xFF27272A),
        onSurfaceVariant = Color(0xFFA1A1AA),
        outline = Color(0xFF3F3F46),
    )
} else {
    lightColorScheme(
        primary = Color(0xFF71717A),
        onPrimary = Color(0xFFFAFAFA),
        primaryContainer = Color(0xFFE4E4E7),
        onPrimaryContainer = Color(0xFF18181B),
        secondary = Color(0xFF52525B),
        onSecondary = Color(0xFFFAFAFA),
        tertiary = Color(0xFF52525B),
        onTertiary = Color(0xFFFAFAFA),
        error = Color(0xFFC34141),
        onError = Color(0xFFFFFFFF),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF18181B),
        surface = Color(0xFFF4F4F5),
        onSurface = Color(0xFF18181B),
        surfaceVariant = Color(0xFFE4E4E7),
        onSurfaceVariant = Color(0xFF52525B),
        outline = Color(0xFFD4D4D8),
    )
}

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
        else                        -> if (dark) BrandColors.dark() else BrandColors.light()
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