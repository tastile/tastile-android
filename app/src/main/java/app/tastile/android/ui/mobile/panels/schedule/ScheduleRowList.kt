package app.tastile.android.ui.mobile.panels.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tastile.android.data.model.Tile
import app.tastile.android.ui.designsystem.AppListRow
import app.tastile.android.ui.designsystem.AppSpacing
import app.tastile.android.ui.designsystem.AppTheme

/**
 * Filtered list of schedule rows for the active view. Mirrors the web
 * `ScheduleSidePanel`'s `filteredTileList` rendering
 * (`tastile-web/src/components/panels/ScheduleSidePanel.tsx`).
 *
 * @param tiles tiles already filtered to either recurring or
 * upcoming (the filter logic lives in [ScheduleSectionContent] so it
 * can read `LocalDate.now()` once per recomposition).
 * @param recurring whether the active view is "recurring" — toggles
 * the leading glyph (↻) and the description prefix.
 * @param onSelect invoked when a row is tapped.
 */
@Composable
fun ScheduleRowList(
    tiles: List<Tile>,
    recurring: Boolean,
    onSelect: (String) -> Unit,
) {
    if (tiles.isEmpty()) {
        Text(
            text = "No tiles",
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(vertical = AppSpacing.xs),
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
    ) {
        items(tiles.take(VISIBLE_LIMIT), key = { it.id }) { tile ->
            ScheduleRow(
                tile = tile,
                recurring = recurring,
                onClick = { onSelect(tile.id) },
            )
        }
    }
}

@Composable
private fun ScheduleRow(
    tile: Tile,
    recurring: Boolean,
    onClick: () -> Unit,
) {
    if (recurring) {
        AppListRow(
            label = tile.title,
            leading = {
                Text(
                    text = "↻",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            },
            onClick = onClick,
            description = tile.title,
        )
    } else {
        AppListRow(
            label = tile.title,
            onClick = onClick,
            description = tile.title,
        )
    }
}

private const val VISIBLE_LIMIT = 10
