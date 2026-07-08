package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.data.model.Tile
import app.tastile.android.ui.designsystem.AppOutlinedPanel
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.dashboard.ListViewMode

/**
 * One labelled section header + the first [limit] tiles of the section.
 * The header row is tappable and bumps the section's visible cap via
 * [onToggleExpanded], mirroring web's `nextTileSectionLimit`
 * 8 → 16 → 32 → 60 → reset pattern (see
 * `tastile-web/src/app/dashboard/tiles/page.tsx`). When more tiles are
 * hidden than the cap allows, the header shows the web-style
 * `dashboard_tiles_omitted_more` `"+%1$d more ▼"` affordance.
 */
@Composable
fun TilesSectionColumn(
    groupId: String,
    label: String,
    tiles: List<Tile>,
    viewMode: ListViewMode,
    limit: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onTileClick: (Tile) -> Unit,
    onTileDelete: (Tile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val totalMinutes = tiles.sumOf { (it.targetWorkMin ?: 0).toLong() }
    val effectiveLimit = limit.coerceAtMost(tiles.size)
    val visible = if (expanded) tiles else tiles.take(effectiveLimit)
    val omitted = tiles.size - visible.size
    val atMax = effectiveLimit >= tiles.size
    AppOutlinedPanel(
        modifier = modifier.testTag("tiles-section-$groupId"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .testTag("tiles-section-header-tap-$groupId")
                .padding(vertical = AppTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = AppTheme.typography.titleMedium, color = colors.onSurface)
                    Text(
                        "$totalMinutes min · ${tiles.size}",
                        style = AppTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (omitted > 0) {
                    Text(
                        text = stringResource(R.string.dashboard_tiles_omitted_more, omitted),
                        style = AppTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.testTag("tiles-section-expand-$groupId"),
                    )
                } else if (atMax) {
                    Text(
                        text = "▴",
                        style = AppTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.testTag("tiles-section-collapse-$groupId"),
                    )
                }
            }
        }
        if (visible.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                visible.forEach { tile ->
                    TileCard(
                        tile = tile,
                        mode = viewMode,
                        onClick = { onTileClick(tile) },
                        onDelete = { onTileDelete(tile) },
                    )
                }
            }
        }
    }
}
