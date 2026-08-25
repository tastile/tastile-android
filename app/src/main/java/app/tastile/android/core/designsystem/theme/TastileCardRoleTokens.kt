package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Card role tokens. Phase 1 resolves slots from `MaterialTheme.colorScheme`;
 * Phase 2 injects brand-palette entries through `TastileTheme`.
 */
@Immutable
data class TastileCardRoleColors(
    val container: Color,
    val border: Color,
)

@Immutable
data class TastileCardRoleTokens(
    val neutral: TastileCardRoleColors,
    val actionable: TastileCardRoleColors,
    val completed: TastileCardRoleColors,
) {
    companion object {
        fun default(
            scheme: androidx.compose.material3.ColorScheme,
        ): TastileCardRoleTokens = TastileCardRoleTokens(
            neutral = TastileCardRoleColors(
                container = scheme.surface,
                border = scheme.outlineVariant,
            ),
            actionable = TastileCardRoleColors(
                container = scheme.surfaceContainerLow,
                border = scheme.primary,
            ),
            completed = TastileCardRoleColors(
                container = scheme.surfaceContainerLowest,
                border = scheme.outline,
            ),
        )
    }
}
