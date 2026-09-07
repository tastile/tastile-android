package app.tastile.android.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout tokens. These are static values that don't depend on the active
 * `MaterialTheme.colorScheme` or any other `CompositionLocal`, so they
 * are exposed as a `data object` (no CompositionLocal plumbing required).
 *
 * Read by frame primitives (Issue #101) for screen-level geometry:
 *  - `screenGutter` / `screenGutterCompact` – horizontal padding for
 *    screen content (regular and compact widths).
 *  - `listRowMinHeight`                     – minimum tap target for
 *    list rows (matches M3 `ListItem` defaults).
 *  - `stickyHeaderOffset`                   – top inset for sticky
 *    headers inside a `LazyColumn` / `LazyList`.
 *  - `fabBottomInset`                       – bottom padding so a FAB
 *    clears the bottom bar.
 *  - `bottomBarHeight`                      – default bottom navigation
 *    bar height.
 */
data object TastileLayoutTokens {
    val screenGutter: Dp = 16.dp
    val screenGutterCompact: Dp = 12.dp
    val listRowMinHeight: Dp = 56.dp
    val stickyHeaderOffset: Dp = 64.dp
    val fabBottomInset: Dp = 16.dp
    val bottomBarHeight: Dp = 80.dp
}
