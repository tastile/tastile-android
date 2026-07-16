package app.tastile.android.ui.mobile.panels.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.ui.designsystem.AppCorner
import app.tastile.android.ui.designsystem.AppListRow
import app.tastile.android.ui.designsystem.AppSpacing
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.designsystem.AppEmptyState
import app.tastile.android.ui.mobile.designsystem.MobileTokens
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip

/**
 * Renders the timeline block list using the v1 wire payload
 * (`CoreTimelineItem`). Mirrors web's
 * `<TimelineBlockList>` card list in `tastile-web/src/app/dashboard/timeline/page.tsx`.
 *
 * Mobile variation: per plan R3 the cards render as `AppListRow` items
 * in a `Column` (no card boundary, no elevation).
 */
@Composable
internal fun TimelineBlockList(
    blocks: List<CoreTimelineItem>,
    onBlockClick: (CoreTimelineItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (blocks.isEmpty()) {
        AppEmptyState(
            icon = Icons.Outlined.CalendarMonth,
            title = stringResource(R.string.panels_timeline_empty),
            hint = "",
            modifier = modifier,
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
    ) {
        blocks.forEach { block ->
            TimelineBlockRow(block = block, onClick = { onBlockClick(block) })
        }
    }
}

@Composable
private fun TimelineBlockRow(
    block: CoreTimelineItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorner.smallShape)
            .background(AppTheme.colors.surfaceVariant.copy(alpha = MobileTokens.SurfaceAlpha.subtle))
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppListRow(
            label = block.title,
            description = "${block.type} · ${block.status} · ${block.startAt}",
            onClick = onClick,
            modifier = Modifier.padding(horizontal = AppSpacing.xxs),
        )
    }
}
