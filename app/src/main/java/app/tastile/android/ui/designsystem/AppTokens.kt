package app.tastile.android.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Spacing scale on a **4dp baseline grid**. Every layout value in the app must
 * resolve to one of these tokens — ad-hoc `N.dp` literals are not allowed
 * outside this file. The scale is intentionally short and step-doubling so
 * the visual rhythm stays coherent:
 *
 * ```
 *  xxs (2)   hairline nudges, border offsets
 *  xs  (4)   icon→text gap inside a single row
 *  sm  (8)   list inter-item, tight inner component padding
 *  md  (12)  component internal horizontal padding (buttons, chips)
 *  lg  (16)  page horizontal padding, default section inner padding
 *  xl  (24)  between distinct sections, page top/bottom
 *  xxl (32)  between major content blocks (hero → first list)
 * ```
 */
object AppSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/**
 * Canonical corner-radius scale for every rounded surface in the app:
 *  - [small]  cards, list rows, picker segments
 *  - [medium] chips, drop-zones
 *  - [large]  bottom sheets, panels, dialogs
 *  - [pill]   capsules (segmented selectors, top-bar dropdowns)
 *
 * Use these instead of inline `RoundedCornerShape(N.dp)` so radii stay coherent
 * across screens.
 */
object AppCorner {
    val small = 6.dp
    val medium = 8.dp
    val large = 12.dp
    val pill = 999.dp

    val smallShape = RoundedCornerShape(small)
    val mediumShape = RoundedCornerShape(medium)
    val largeShape = RoundedCornerShape(large)
    val pillShape = RoundedCornerShape(pill)
}

/** Back-compat alias — every pre-existing `AppShape.chip` / `AppShape.panel`
 *  call site still resolves and now shares the unified scale. */
object AppShape {
    val panel = AppCorner.smallShape
    val chip = AppCorner.mediumShape
}

/**
 * Component sizes, all on the 4dp baseline.
 *
 *  - [buttonMinHeight]     48dp  (M3 recommended tap target)
 *  - [iconButton]          40dp  (square icon button)
 *  - [avatar]              40dp  (matches iconButton)
 *  - [listRowMinHeight]    56dp  (slightly taller than M3 default — thumb-friendly)
 *  - [listRowGlyphSize]    20dp  (small status glyph / leading icon)
 *  - [listRowIconSize]     24dp  (full Material icon)
 *  - [listRowIndent]       48dp  (= pageHorizontal 16 + listRowIconSize 24 + sm gap 8 —
 *                                sub-row indent that lands directly under the label column
 *                                of an AppListRow when its parent is `AppPageColumn` /
 *                                `PanelSheet`)
 *  - [fabSafeBottom]       80dp  (scroll padding so list content doesn't slip under FAB)
 */
object AppComponentSize {
    val buttonMinHeight = 48.dp
    val iconButton = 40.dp
    val avatar = 40.dp

    val listRowMinHeight = 56.dp
    val listRowGlyphSize = 20.dp
    val listRowIconSize = 24.dp
    val listRowIndent = 48.dp
    val calendarCellSize = 40.dp
    val fabSafeBottom = 80.dp
}

/**
 * Page-level (outer) padding composition. Use [ScreenPadding] from
 * `AppPageColumn` / `AppPageWithOverlay` — never inline `padding(...)` at the
 * tab-root level. Two rhythms:
 *
 *  - [horizontal] / [top] / [bottom]  — page-edge breathing (16dp on all sides)
 *  - [interItem]                      — gap between sibling rows in the same section (8dp)
 *  - [interSection]                   — gap that marks a distinct section boundary
 *                                       (24dp — applied by `AppSectionHeader`)
 */
object ScreenPadding {
    val horizontal = AppSpacing.lg
    val top = AppSpacing.lg
    val bottom = AppSpacing.lg
    val interItem = AppSpacing.sm
    val interSection = AppSpacing.xl
}