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
 *
 * Phase 1 (Issue #100, release:0-6-0) additionally exposes three
 * container/onContainer pairs for status badges and inline messages:
 *  - `successContainer` / `onSuccessContainer`  -> `secondaryContainer`
 *  - `warningContainer` / `onWarningContainer`  -> `tertiaryContainer`
 *  - `dangerContainer`  / `onDangerContainer`   -> `errorContainer`
 *
 * The container pairs bind to Material 3 color roles so dynamic color
 * (Android 12+) and dark mode adjust automatically. Contrast is verified
 * to meet WCAG AA (4.5:1) by `TastileStatusTokensContrastTest`.
 */
@Immutable
data class TastileStatusTokens(
    val ready: TastileStatusColors,
    val started: TastileStatusColors,
    val done: TastileStatusColors,
    val archived: TastileStatusColors,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
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
            successContainer = scheme.secondaryContainer,
            onSuccessContainer = scheme.onSecondaryContainer,
            warningContainer = scheme.tertiaryContainer,
            onWarningContainer = scheme.onTertiaryContainer,
            dangerContainer = scheme.errorContainer,
            onDangerContainer = scheme.onErrorContainer,
        )
    }
}
