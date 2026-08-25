package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Color slot for a single lifecycle status. Phase 1 fills slots with
 * Material 3 placeholders that match the current look. Phase 2 will swap
 * these for brand-palette entries without touching call sites.
 */
@Immutable
data class TastileStatusColors(
    val container: Color,
    val onContainer: Color,
    val icon: Color,
)

/**
 * Status tokens keyed by [app.tastile.android.data.model.TileLifecycle].
 * Defaults read from `MaterialTheme.colorScheme` so today's visuals are
 * preserved.
 */
@Immutable
data class TastileStatusTokens(
    val ready: TastileStatusColors,
    val started: TastileStatusColors,
    val done: TastileStatusColors,
    val archived: TastileStatusColors,
) {
    companion object {
        fun default(
            scheme: androidx.compose.material3.ColorScheme,
        ): TastileStatusTokens = TastileStatusTokens(
            ready = TastileStatusColors(
                container = scheme.surfaceVariant,
                onContainer = scheme.onSurfaceVariant,
                icon = scheme.primary,
            ),
            started = TastileStatusColors(
                container = scheme.tertiaryContainer,
                onContainer = scheme.onTertiaryContainer,
                icon = scheme.tertiary,
            ),
            done = TastileStatusColors(
                container = scheme.secondaryContainer,
                onContainer = scheme.onSecondaryContainer,
                icon = scheme.secondary,
            ),
            archived = TastileStatusColors(
                container = scheme.surfaceVariant,
                onContainer = scheme.onSurfaceVariant,
                icon = scheme.outline,
            ),
        )
    }
}