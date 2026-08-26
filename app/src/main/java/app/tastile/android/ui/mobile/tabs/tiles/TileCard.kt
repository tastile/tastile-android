package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: m3-component
import androidx.compose.material3.ListItem
// m2-allow: m3-component
import androidx.compose.material3.ListItemDefaults
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.model.dueAtDate
import app.tastile.android.data.model.isRecurring
import app.tastile.android.data.model.projectLabel
import app.tastile.android.core.designsystem.theme.LocalTastileCardRoleTokens
import app.tastile.android.ui.dashboard.ListViewMode

/**
 * Three-card dispatch by [ListViewMode]. Each branch shares the same
 * `tile-card-${tile.id}-${mode}` testTag so a test can assert that the
 * right density renders without depending on the underlying
 * implementation (compact uses a tappable [ListItem] directly; comfortable
 * builds a meta line; detailed adds duration/start/delete affordances).
 */
@Composable
fun CompactTileCard(
    tile: Tile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    val glyph = lifecycle.glyphChar()
    ListItem(
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(glyph, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                Text(
                    text = "›",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current,
                )
            }
        },
        supportingContent = {
            Text(
                text = lifecycle.shortLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current,
            )
        },
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("tile-card-${tile.id}-compact"),
        colors = ListItemDefaults.colors(containerColor = LocalTastileCardRoleTokens.current.neutral.container),
    )
}

@Composable
fun ComfortableTileCard(
    tile: Tile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    val project = tile.projectLabel()
    val dueAt = tile.dueAtDate()
    val isRecurring = tile.isRecurring()
    val meta = buildMeta(project = project, dueAt = dueAt, isRecurring = isRecurring)
    ListItem(
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(lifecycle.glyphChar(), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                Text(
                    text = "›",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current,
                )
            }
        },
        supportingContent = {
            Text(
                text = meta.ifBlank { lifecycle.shortLabel() },
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current,
            )
        },
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("tile-card-${tile.id}-comfortable"),
        colors = ListItemDefaults.colors(containerColor = LocalTastileCardRoleTokens.current.neutral.container),
    )
}

@Composable
fun DetailedTileCard(
    tile: Tile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    val project = tile.projectLabel()
    val dueAt = tile.dueAtDate()
    val isRecurring = tile.isRecurring()
    val meta = buildMeta(project = project, dueAt = dueAt, isRecurring = isRecurring)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tile-card-${tile.id}-detailed"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(lifecycle.glyphChar(), style = MaterialTheme.typography.bodyMedium)
            Text(
                tile.title,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.tile_edit_delete_cd))
            }
        }
        if (meta.isNotBlank()) {
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.tiles_duration) + ": " + stringResource(R.string.tile_card_meta_minutes, tile.targetWorkMin ?: 0),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current,
            )
            val start = tile.fixedStart ?: tile.activeStart
            if (!start.isNullOrBlank()) {
                Text(
                    text = "${stringResource(R.string.tiles_start_at)}: $start",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current,
                )
            }
        }
    }
}

@Composable
fun TileCard(
    tile: Tile,
    mode: ListViewMode,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (mode) {
        ListViewMode.COMPACT -> CompactTileCard(tile = tile, onClick = onClick, modifier = modifier)
        ListViewMode.COMFORTABLE -> ComfortableTileCard(tile = tile, onClick = onClick, modifier = modifier)
        ListViewMode.DETAILED -> DetailedTileCard(tile = tile, onClick = onClick, onDelete = onDelete, modifier = modifier)
    }
}

/**
 * Public lifecycle glyph. Used by the `TileCard` density dispatch in this
 * file and reused by the standalone project-scoped tile rows in
 * `ui/mobile/tabs/ProjectsScreen.kt:ProjectTileRow` so the same character
 * shape appears in every tile row across the app.
 */
fun TileLifecycle.glyphChar(): String = when (this) {
    TileLifecycle.DONE -> "✓"
    TileLifecycle.STARTED -> "▶"
    TileLifecycle.READY -> "○"
    TileLifecycle.ARCHIVED -> "·"
}

@Composable
private fun TileLifecycle.shortLabel(): String = when (this) {
    TileLifecycle.READY -> stringResource(R.string.tasks_status_ready)
    TileLifecycle.STARTED -> stringResource(R.string.tasks_status_started)
    TileLifecycle.DONE -> stringResource(R.string.tasks_status_completed)
    TileLifecycle.ARCHIVED -> stringResource(R.string.tasks_status_archived)
}

private fun buildMeta(project: String?, dueAt: String?, isRecurring: Boolean): String = buildString {
    if (!project.isNullOrBlank()) append(project)
    if (!dueAt.isNullOrBlank()) {
        if (isNotEmpty()) append(" · ")
        append(dueAt)
    }
    if (isRecurring) {
        if (isNotEmpty()) append(" · ")
        append("↻")
    }
}
