package app.tastile.android.ui.mobile.panels.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: state-holder
import androidx.compose.material3.ListItemDefaults
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.data.model.Tile

/**
 * Filtered list of schedule rows for the active view. Mirrors the web
 * `ScheduleSidePanel`'s `filteredTileList` rendering
 * (`tastile-web/src/components/panels/ScheduleSidePanel.tsx`).
 *
 * @param tiles tiles already filtered to either recurring or
 * upcoming (the filter logic lives in [ScheduleSectionContent] so it
 * can read `LocalDate.now()` once per recomposition).
 * @param recurring whether the active view is "recurring" — toggles
 * the leading icon and the description prefix.
 * @param onSelect invoked when a row is tapped.
 */
@Composable
fun ScheduleRowList(
    tiles: List<Tile>,
    recurring: Boolean,
    onSelect: (String) -> Unit,
) {
    if (tiles.isEmpty()) {
        AppEmptyState(
            icon = Icons.Outlined.EventBusy,
            title = stringResource(R.string.empty_schedule_title),
            hint = stringResource(R.string.empty_schedule_hint),
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
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
    NiaListItem(
        headlineContent = { Text(tile.title, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            Icon(
                imageVector = if (recurring) Icons.Outlined.Replay else Icons.Outlined.Schedule,
                contentDescription = null,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun AppEmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private const val VISIBLE_LIMIT = 10