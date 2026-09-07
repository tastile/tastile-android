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

/**
 * Card role tokens consumed by project rows, list rows, and detail headers.
 *
 * Phase 1 (Issue #100, release:0-6-0) appends two role entries:
 *  - `cardAccent`  – accent-tinted card surface; consumers in frame primitives
 *    (Issue #101) read `LocalTintTheme` to override `iconTint` and adjust the
 *    border to match. The default binds to `primaryContainer` / `primary`.
 *  - `cardNeutral` – lower-emphasis neutral card; bound to
 *    `surfaceContainer` / `outlineVariant`.
 *
 * New fields are appended with sensible defaults so existing named and
 * positional constructors continue to compile.
 */
@Immutable
data class TastileCardRoleTokens(
    val neutral: TastileCardRoleColors,
    val actionable: TastileCardRoleColors,
    val completed: TastileCardRoleColors,
    val cardAccent: TastileCardRoleColors,
    val cardNeutral: TastileCardRoleColors,
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
            cardAccent = TastileCardRoleColors(
                container = scheme.primaryContainer,
                border = scheme.primary,
            ),
            cardNeutral = TastileCardRoleColors(
                container = scheme.surfaceContainer,
                border = scheme.outlineVariant,
            ),
        )
    }
}
