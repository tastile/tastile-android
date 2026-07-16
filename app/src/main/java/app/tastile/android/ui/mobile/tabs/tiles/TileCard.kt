package app.tastile.android.ui.mobile.tabs.tiles

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
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.model.dueAtDate
import app.tastile.android.data.model.isRecurring
import app.tastile.android.data.model.projectLabel
import app.tastile.android.ui.designsystem.AppChevron
import app.tastile.android.ui.designsystem.AppListRow
import app.tastile.android.ui.designsystem.AppMetaText
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.dashboard.ListViewMode

/**
 * Three-card dispatch by [ListViewMode]. Each branch shares the same
 * `tile-card-${tile.id}-${mode}` testTag so a test can assert that the
 * right density renders without depending on the underlying
 * implementation (compact uses [AppListRow] directly; comfortable
 * builds a meta line; detailed adds duration/start/delete affordances).
 */
@Composable
fun CompactTileCard(
    tile: Tile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    val glyph = lifecycle.glyph()
    AppListRow(
        label = tile.title,
        meta = lifecycle.shortLabel(),
        leading = { Text(glyph, style = AppTheme.typography.bodyMedium) },
        trailing = { AppChevron() },
        onClick = onClick,
        modifier = modifier.testTag("tile-card-${tile.id}-compact"),
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
    AppListRow(
        label = tile.title,
        meta = meta.ifBlank { lifecycle.shortLabel() },
        leading = { Text(lifecycle.glyph(), style = AppTheme.typography.bodyMedium) },
        trailing = { AppChevron() },
        onClick = onClick,
        modifier = modifier.testTag("tile-card-${tile.id}-comfortable"),
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
    val colors = AppTheme.colors
    val meta = buildMeta(project = project, dueAt = dueAt, isRecurring = isRecurring)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tile-card-${tile.id}-detailed"),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Text(lifecycle.glyph(), style = AppTheme.typography.bodyMedium)
            Text(
                tile.title,
                style = AppTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Close, contentDescription = "Delete")
            }
        }
        if (meta.isNotBlank()) {
            AppMetaText(meta)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Text(
                text = "${stringResource(R.string.tiles_duration)}: ${tile.targetWorkMin ?: 0} min",
                style = AppTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            val start = tile.fixedStart ?: tile.activeStart
            if (!start.isNullOrBlank()) {
                Text(
                    text = "${stringResource(R.string.tiles_start_at)}: $start",
                    style = AppTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
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

private fun TileLifecycle.glyph(): String = when (this) {
    TileLifecycle.DONE -> "✓"
    TileLifecycle.STARTED -> "▶"
    TileLifecycle.READY -> "○"
    TileLifecycle.ARCHIVED -> "·"
}

private fun TileLifecycle.shortLabel(): String = name.lowercase()

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
