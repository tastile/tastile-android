package app.tastile.android.ui.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private object BrandTokens {
    val Primary = Color(0xFF27272A)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFE4E4E7)
    val OnPrimaryContainer = Color(0xFF18181B)
    val Secondary = Color(0xFF52525B)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE4E4E7)
    val OnSecondaryContainer = Color(0xFF18181B)
    val Tertiary = Color(0xFF0F766E)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFCCFBE1)
    val OnTertiaryContainer = Color(0xFF064E3B)
    val Error = Color(0xFFB91C1C)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFEE2E2)
    val OnErrorContainer = Color(0xFF7F1D1D)
    val Background = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF18181B)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF18181B)
    val SurfaceVariant = Color(0xFFF4F4F5)
    val OnSurfaceVariant = Color(0xFF52525B)
    val Outline = Color(0xFFA1A1AA)
    val OutlineVariant = Color(0xFFE4E4E7)
    val Scrim = Color(0xFF000000)
    val InverseSurface = Color(0xFF18181B)
    val InverseOnSurface = Color(0xFFF4F4F5)
    val InversePrimary = Color(0xFFE4E4E7)

    val PrimaryDark = Color(0xFFE4E4E7)
    val OnPrimaryDark = Color(0xFF18181B)
    val PrimaryContainerDark = Color(0xFF27272A)
    val OnPrimaryContainerDark = Color(0xFFF4F4F5)
    val SecondaryDark = Color(0xFFA1A1AA)
    val OnSecondaryDark = Color(0xFF18181B)
    val SecondaryContainerDark = Color(0xFF3F3F46)
    val OnSecondaryContainerDark = Color(0xFFE4E4E7)
    val TertiaryDark = Color(0xFF6EE7B7)
    val OnTertiaryDark = Color(0xFF064E3B)
    val TertiaryContainerDark = Color(0xFF065F46)
    val OnTertiaryContainerDark = Color(0xFFCCFBE1)
    val ErrorDark = Color(0xFFFCA5A5)
    val OnErrorDark = Color(0xFF7F1D1D)
    val ErrorContainerDark = Color(0xFF7F1D1D)
    val OnErrorContainerDark = Color(0xFFFEE2E2)
    val BackgroundDark = Color(0xFF09090B)
    val OnBackgroundDark = Color(0xFFE4E4E7)
    val SurfaceDark = Color(0xFF18181B)
    val OnSurfaceDark = Color(0xFFE4E4E7)
    val SurfaceVariantDark = Color(0xFF27272A)
    val OnSurfaceVariantDark = Color(0xFFA1A1AA)
    val OutlineDark = Color(0xFF52525B)
    val OutlineVariantDark = Color(0xFF27272A)
    val ScrimDark = Color(0xFF000000)
    val InverseSurfaceDark = Color(0xFFE4E4E7)
    val InverseOnSurfaceDark = Color(0xFF18181B)
    val InversePrimaryDark = Color(0xFF71717A)
}

fun BrandColors.Companion.light(): ColorScheme = lightColorScheme(
    primary = BrandTokens.Primary, onPrimary = BrandTokens.OnPrimary,
    primaryContainer = BrandTokens.PrimaryContainer, onPrimaryContainer = BrandTokens.OnPrimaryContainer,
    secondary = BrandTokens.Secondary, onSecondary = BrandTokens.OnSecondary,
    secondaryContainer = BrandTokens.SecondaryContainer, onSecondaryContainer = BrandTokens.OnSecondaryContainer,
    tertiary = BrandTokens.Tertiary, onTertiary = BrandTokens.OnTertiary,
    tertiaryContainer = BrandTokens.TertiaryContainer, onTertiaryContainer = BrandTokens.OnTertiaryContainer,
    error = BrandTokens.Error, onError = BrandTokens.OnError,
    errorContainer = BrandTokens.ErrorContainer, onErrorContainer = BrandTokens.OnErrorContainer,
    background = BrandTokens.Background, onBackground = BrandTokens.OnBackground,
    surface = BrandTokens.Surface, onSurface = BrandTokens.OnSurface,
    surfaceVariant = BrandTokens.SurfaceVariant, onSurfaceVariant = BrandTokens.OnSurfaceVariant,
    outline = BrandTokens.Outline, outlineVariant = BrandTokens.OutlineVariant,
    scrim = BrandTokens.Scrim, inverseSurface = BrandTokens.InverseSurface,
    inverseOnSurface = BrandTokens.InverseOnSurface, inversePrimary = BrandTokens.InversePrimary,
)

fun BrandColors.Companion.dark(): ColorScheme = darkColorScheme(
    primary = BrandTokens.PrimaryDark, onPrimary = BrandTokens.OnPrimaryDark,
    primaryContainer = BrandTokens.PrimaryContainerDark, onPrimaryContainer = BrandTokens.OnPrimaryContainerDark,
    secondary = BrandTokens.SecondaryDark, onSecondary = BrandTokens.OnSecondaryDark,
    secondaryContainer = BrandTokens.SecondaryContainerDark, onSecondaryContainer = BrandTokens.OnSecondaryContainerDark,
    tertiary = BrandTokens.TertiaryDark, onTertiary = BrandTokens.OnTertiaryDark,
    tertiaryContainer = BrandTokens.TertiaryContainerDark, onTertiaryContainer = BrandTokens.OnTertiaryContainerDark,
    error = BrandTokens.ErrorDark, onError = BrandTokens.OnErrorDark,
    errorContainer = BrandTokens.ErrorContainerDark, onErrorContainer = BrandTokens.OnErrorContainerDark,
    background = BrandTokens.BackgroundDark, onBackground = BrandTokens.OnBackgroundDark,
    surface = BrandTokens.SurfaceDark, onSurface = BrandTokens.OnSurfaceDark,
    surfaceVariant = BrandTokens.SurfaceVariantDark, onSurfaceVariant = BrandTokens.OnSurfaceVariantDark,
    outline = BrandTokens.OutlineDark, outlineVariant = BrandTokens.OutlineVariantDark,
    scrim = BrandTokens.ScrimDark, inverseSurface = BrandTokens.InverseSurfaceDark,
    inverseOnSurface = BrandTokens.InverseOnSurfaceDark, inversePrimary = BrandTokens.InversePrimaryDark,
)

/** Internal so Theme.kt can branch on brand vs gray vs dynamic. */
class BrandColors internal constructor() {
    companion object
}
