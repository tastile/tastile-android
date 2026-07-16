package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.isStarted
import app.tastile.android.ui.designsystem.AppCenteredLoading
import app.tastile.android.ui.designsystem.AppEmptyState
import app.tastile.android.ui.designsystem.AppListRow
import app.tastile.android.ui.designsystem.AppOutlinedPanel
import app.tastile.android.ui.designsystem.AppPageColumn
import app.tastile.android.ui.designsystem.AppSectionHeader
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.tabs.tiles.DeleteTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.DeferTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.PromptRequestDialog

@Composable
fun ExecuteScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val deleteCandidate by viewModel.requestDeleteTileId.collectAsStateWithLifecycle()
    val deferCandidate by viewModel.requestDeferTileId.collectAsStateWithLifecycle()
    val promptCandidate by viewModel.requestPromptTileId.collectAsStateWithLifecycle()
    val actionMessage by viewModel.lastActionMessage.collectAsStateWithLifecycle()

    if (loading && tiles.isEmpty()) {
        AppCenteredLoading()
        return
    }

    val active = tiles.firstOrNull { it.isStarted() }
    val others = active?.let { tiles.filterNot { tile -> tile.id == it.id } } ?: tiles
    val showable = others.filter { tile ->
        TileLifecycle.fromString(tile.lifecycle) != TileLifecycle.DONE
    }

    AppPageColumn {
        error?.let { message ->
            Text(text = message, color = AppTheme.colors.error)
        }
        actionMessage?.let { message -> Text(text = message, color = AppTheme.colors.primary) }
        active?.let { ActiveTileHero(tile = it, viewModel = viewModel) }

        AppSectionHeader(text = if (showable.isEmpty()) "Nothing to do — create a tile" else "Today and ready")

        if (showable.isEmpty()) {
            AppEmptyState(
                message = "No tiles for today.",
                actionLabel = "Create",
                onAction = { overlay.show(Overlay.QuickCreate) },
            )
        } else {
            showable.forEach { tile ->
                TileActionRow(
                    tile = tile,
                    onTap = {
                        viewModel.selectTile(tile.id)
                        overlay.show(Overlay.TileEdit(tile.id))
                    },
                    onStart = { viewModel.startTile(tile.id) },
                    onComplete = { viewModel.completeTile(tile.id) },
                    onPause = { viewModel.pauseTile(tile.id) },
                    onDelete = { viewModel.setDeleteTileCandidate(tile.id) },
                    onDefer = { viewModel.setDeferTileCandidate(tile.id) },
                    onPrompt = { viewModel.setPromptTileCandidate(tile.id) },
                )
            }
        }
    }

    deleteCandidate?.let { id ->
        DeleteTileDialog(
            tileTitle = tiles.firstOrNull { it.id == id }?.title,
            onConfirm = viewModel::confirmDeleteTile,
            onCancel = { viewModel.setDeleteTileCandidate(null) },
        )
    }
    deferCandidate?.let { id -> DeferTileDialog(tiles.firstOrNull { it.id == id }?.title, viewModel::confirmDeferTile) { viewModel.setDeferTileCandidate(null) } }
    promptCandidate?.let { id -> PromptRequestDialog(tiles.firstOrNull { it.id == id }?.title, viewModel::confirmPromptTile) { viewModel.setPromptTileCandidate(null) } }
}

@Composable
private fun ActiveTileHero(tile: Tile, viewModel: DashboardViewModel) {
    AppOutlinedPanel {
        Text(
            text = "▶ ${tile.title}",
            style = AppTheme.typography.titleMedium,
        )
        tile.nextAction?.takeIf { it.isNotBlank() }?.let { next ->
            Text(
                text = "Next: $next",
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            Button(onClick = { viewModel.completeTile(tile.id) }) { Text("Complete") }
            OutlinedButton(onClick = { viewModel.pauseTile(tile.id) }) { Text("Pause") }
        }
    }
}

@Composable
private fun TileActionRow(
    tile: Tile,
    onTap: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    onDefer: () -> Unit,
    onPrompt: () -> Unit,
) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    val glyph = when (lifecycle) {
        TileLifecycle.DONE -> "✓"
        TileLifecycle.STARTED -> "▶"
        TileLifecycle.READY -> "○"
        TileLifecycle.ARCHIVED -> "·"
    }
    var menuOpen by remember { mutableStateOf(false) }

    AppListRow(
        label = tile.title,
        leading = { Text(glyph, style = AppTheme.typography.bodyMedium) },
        trailing = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More actions")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (lifecycle == TileLifecycle.READY) {
                        DropdownMenuItem(
                            text = { Text("Start") },
                            leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                            onClick = { menuOpen = false; onStart() },
                        )
                        DropdownMenuItem(text = { Text("Defer") }, onClick = { menuOpen = false; onDefer() })
                        DropdownMenuItem(text = { Text("Request prompt") }, onClick = { menuOpen = false; onPrompt() })
                    }
                    if (lifecycle == TileLifecycle.STARTED) {
                        DropdownMenuItem(
                            text = { Text("Complete") },
                            onClick = { menuOpen = false; onComplete() },
                        )
                        DropdownMenuItem(
                            text = { Text("Pause") },
                            leadingIcon = { Icon(Icons.Outlined.Pause, contentDescription = null) },
                            onClick = { menuOpen = false; onPause() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        },
        onClick = onTap,
        description = "${lifecycle.name}: ${tile.title}",
    )
}
