package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Single import surface for every token family consumed by screens and
 * frame primitives. Phase 1 (Issue #100, release:0-6-0) introduces this
 * object so future lint rules (Issue #102, `HardcodedColorRule`,
 * `NoRawDpInUiRule`) can whitelist a single entry point rather than
 * scanning every `Local*Tokens.current` call site.
 *
 * Reads from each `LocalTastile*Tokens.current` and the static
 * `TastileLayoutTokens` data object. The `layout` accessor is plain
 * because `TastileLayoutTokens` does not depend on a `CompositionLocal`.
 */
object TastileTokenMap {

    /** Lifecycle (ready/started/done/archived) + container status colors. */
    val status: TastileStatusTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTastileStatusTokens.current

    /** Card role tokens (neutral, actionable, completed, cardAccent, cardNeutral). */
    val cardRole: TastileCardRoleTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTastileCardRoleTokens.current

    /** Surface elevation slots (card/sheet/overlay + level0..level3 ladder). */
    val surfaceElevation: TastileSurfaceElevationTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTastileSurfaceElevationTokens.current

    /** Numeric spacing scale + semantic aliases (gutter, gutterCompact). */
    val spacing: TastileSpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTastileSpacingTokens.current

    /** Corner radius scale + semantic shapes (bubbleLarge, chipPill, drawerWide). */
    val shape: TastileShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTastileShapeTokens.current

    /** Static layout tokens (gutter, row height, FAB inset, bottom bar). */
    val layout: TastileLayoutTokens
        get() = TastileLayoutTokens
}
