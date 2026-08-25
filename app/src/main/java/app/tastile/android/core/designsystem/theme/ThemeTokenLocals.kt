package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocals for the design-system token families. Resolved once
 * inside [TastileTheme]; consumers read via
 * `LocalTastileStatusTokens.current` etc.
 */
val LocalTastileStatusTokens = staticCompositionLocalOf<TastileStatusTokens> {
    error("TastileStatusTokens not provided. Wrap content in TastileTheme { ... }.")
}

val LocalTastileCardRoleTokens = staticCompositionLocalOf<TastileCardRoleTokens> {
    error("TastileCardRoleTokens not provided. Wrap content in TastileTheme { ... }.")
}

val LocalTastileSurfaceElevationTokens = staticCompositionLocalOf<TastileSurfaceElevationTokens> {
    error("TastileSurfaceElevationTokens not provided. Wrap content in TastileTheme { ... }.")
}

val LocalTastileSpacingTokens = staticCompositionLocalOf<TastileSpacingTokens> {
    error("TastileSpacingTokens not provided. Wrap content in TastileTheme { ... }.")
}