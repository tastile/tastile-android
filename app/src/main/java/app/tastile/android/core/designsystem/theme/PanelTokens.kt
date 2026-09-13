package app.tastile.android.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tokens that pin every row in a QuickCreate base panel or routed subpanel
 * to a single x-coordinate. `LeadingColumnWidth` matches the M3 `ListItem`
 * `leadingContent` slot (16.dp start padding + 24.dp icon + 16.dp gap) and
 * is the single source of truth for row-class alignment in `quickcreate/`.
 *
 * TODO(designsystem): once Issue #100 introduces `TastileLayoutTokens`,
 * migrate these values into `TastileLayoutTokens` (gutter / row heights /
 * leading slot) and delete this object. The companion unit test
 * `PanelTokensTest` will move to `TastileLayoutTokensTest` at the same
 * time. Until then these values are referenced from
 * `ui/mobile/sheets/quickcreate/` and a removal would regress Phase 4.
 */
object PanelTokens {
    val LeadingColumnWidth: Dp = 56.dp
    val LeadingIconSize: Dp = 24.dp
    val LeadingColumnGap: Dp = 16.dp
}
