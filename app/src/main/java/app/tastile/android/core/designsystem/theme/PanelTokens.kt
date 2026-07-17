package app.tastile.android.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tokens that pin every row in a QuickCreate base panel or routed subpanel
 * to a single x-coordinate. `LeadingColumnWidth` matches the M3 `ListItem`
 * `leadingContent` slot (16.dp start padding + 24.dp icon + 16.dp gap) and
 * is the single source of truth for row-class alignment in `quickcreate/`.
 */
object PanelTokens {
    val LeadingColumnWidth: Dp = 56.dp
    val LeadingIconSize: Dp = 24.dp
    val LeadingColumnGap: Dp = 16.dp
}
