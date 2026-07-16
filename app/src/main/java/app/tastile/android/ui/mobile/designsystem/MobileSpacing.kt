package app.tastile.android.ui.mobile.designsystem

import androidx.compose.ui.unit.dp

/**
 * Canonical spacing scale for the mobile UI. The 4/8/12/16/24 grid
 * consolidates the pre-existing ad-hoc `.dp` literals into one vocabulary
 * so padding / Spacer / Arrangement stay consistent across screens.
 *
 * Aliases the nested `MobileTokens.Spacing` object so call sites can
 * `import app.tastile.android.ui.mobile.designsystem.MobileSpacing`
 * without reaching through the parent object.
 */
object MobileSpacing {
    val xxs = MobileTokens.Spacing.xxs
    val xs = MobileTokens.Spacing.xs
    val sm = MobileTokens.Spacing.sm
    val md = MobileTokens.Spacing.md
    val lg = MobileTokens.Spacing.lg
    val xl = MobileTokens.Spacing.xl
    val xxl = MobileTokens.Spacing.xxl
}
