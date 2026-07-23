package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: m3-component
import androidx.compose.material3.Button
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenu
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedButton
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.AppTheme
import app.tastile.android.core.designsystem.component.MobileSpacing
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaCard
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.core.designsystem.component.NiaLoadingWheel
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecutionControlState
import app.tastile.android.ui.dashboard.isStarted
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.tabs.tiles.DeleteTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.DeferTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.PromptRequestDialog

private val MobSpacingXs get() = MobileSpacing.xs
private val MobSpacingSm get() = MobileSpacing.sm

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
    val executionStates by viewModel.executionControlStates.collectAsStateWithLifecycle()
    val executionControlsInFlight by viewModel.executionControlInFlightTileIds.collectAsStateWithLifecycle()
    var executionActionCandidate by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    if (loading && tiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            NiaLoadingWheel(contentDesc = "Loading")
        }
        return
    }

    val active = tiles.firstOrNull { it.isStarted() }
    val others = active?.let { tiles.filterNot { tile -> tile.id == it.id } } ?: tiles
    val showable = others.filter { tile ->
        TileLifecycle.fromString(tile.lifecycle) != TileLifecycle.DONE
    }

    Column(modifier = Modifier.fillMaxWidth().padding(MobSpacingSm)) {
        error?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }
        actionMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.primary)
        }
        active?.let {
            ActiveTileHero(
                tile = it,
                executionState = executionStates[it.id],
                executionControlInFlight = it.id in executionControlsInFlight,
                onStartExecution = { executionActionCandidate = it.id to true },
                onFinishExecution = { executionActionCandidate = it.id to false },
                viewModel = viewModel,
            )
        }

        Text(
            text = if (showable.isEmpty()) "Nothing to do — create a tile" else "Today and ready",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = MobSpacingSm),
        )

        if (showable.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(MobSpacingSm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No tiles for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NiaButton(
                    onClick = { overlay.show(Overlay.QuickCreate) },
                    text = { Text("Create") },
                    modifier = Modifier.padding(top = MobSpacingSm),
                )
            }
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
                    executionState = executionStates[tile.id],
                    executionControlInFlight = tile.id in executionControlsInFlight,
                    onStartExecution = { executionActionCandidate = tile.id to true },
                    onFinishExecution = { executionActionCandidate = tile.id to false },
                    onPause = { viewModel.pauseTile(tile.id) },
                    onResume = { viewModel.resumeTile(tile.id) },
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
    deferCandidate?.let { id ->
        DeferTileDialog(
            tileTitle = tiles.firstOrNull { it.id == id }?.title,
            onConfirm = viewModel::confirmDeferTile,
            onCancel = { viewModel.setDeferTileCandidate(null) },
        )
    }
    promptCandidate?.let { id ->
        PromptRequestDialog(
            tileTitle = tiles.firstOrNull { it.id == id }?.title,
            onConfirm = viewModel::confirmPromptTile,
            onCancel = { viewModel.setPromptTileCandidate(null) },
        )
    }
    executionActionCandidate?.let { (tileId, start) ->
        AlertDialog(
            onDismissRequest = { executionActionCandidate = null },
            title = { Text(if (start) "Start execution?" else "Finish execution?") },
            text = {
                Text(
                    if (start) "Start work on this existing occurrence."
                    else "Finish this execution without completing the tile.",
                )
            },
            confirmButton = {
                NiaButton(
                    onClick = {
                        if (start) viewModel.startExecution(tileId) else viewModel.finishExecution(tileId)
                        executionActionCandidate = null
                    },
                    text = { Text(if (start) "Start" else "Finish") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (start) Icons.Outlined.PlayArrow else Icons.Outlined.Flag,
                            contentDescription = null,
                        )
                    },
                )
            },
            dismissButton = {
                NiaTextButton(
                    onClick = { executionActionCandidate = null },
                    text = { Text("Cancel") },
                    leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                )
            },
        )
    }
}

@Composable
private fun ActiveTileHero(
    tile: Tile,
    executionState: ExecutionControlState?,
    executionControlInFlight: Boolean,
    onStartExecution: () -> Unit,
    onFinishExecution: () -> Unit,
    viewModel: DashboardViewModel,
) {
    NiaCard(modifier = Modifier.padding(vertical = MobSpacingSm)) {
        Column(modifier = Modifier.padding(MobSpacingSm)) {
            Text(
                text = "▶ ${tile.title}",
                style = MaterialTheme.typography.titleMedium,
            )
            tile.nextAction?.takeIf { it.isNotBlank() }?.let { next ->
                NiaListItem(
                    content = {
                        Text(stringResource(R.string.execute_next_label, next))
                    },
                    leadingContent = {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.padding(top = MobSpacingXs),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MobSpacingXs)) {
                Button(onClick = { viewModel.completeTile(tile.id) }) { Text("Complete") }
                when (executionState) {
                    ExecutionControlState.Active -> OutlinedButton(
                        onClick = { viewModel.pauseTile(tile.id) },
                        enabled = !executionControlInFlight,
                        modifier = Modifier.testTag("execute-pause-${tile.id}"),
                    ) { Text("Pause") }
                    ExecutionControlState.Paused -> OutlinedButton(
                        onClick = { viewModel.resumeTile(tile.id) },
                        enabled = !executionControlInFlight,
                        modifier = Modifier.testTag("execute-resume-${tile.id}"),
                    ) { Text("Resume") }
                    null -> OutlinedButton(
                        onClick = onStartExecution,
                        enabled = !executionControlInFlight,
                    ) { Text("Start execution") }
                }
                if (executionState != null) {
                    OutlinedButton(
                        onClick = onFinishExecution,
                        enabled = !executionControlInFlight,
                    ) { Text("Finish execution") }
                }
            }
        }
    }
}

@Composable
private fun TileActionRow(
    tile: Tile,
    onTap: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    executionState: ExecutionControlState?,
    executionControlInFlight: Boolean,
    onStartExecution: () -> Unit,
    onFinishExecution: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
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

    NiaListItem(
        content = { Text(tile.title) },
        leadingContent = {
            Text(glyph, style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
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
                        DropdownMenuItem(
                            text = { Text("Request prompt") },
                            onClick = { menuOpen = false; onPrompt() },
                        )
                    }
                    if (lifecycle == TileLifecycle.STARTED) {
                        DropdownMenuItem(
                            text = { Text("Complete") },
                            onClick = { menuOpen = false; onComplete() },
                        )
                        when (executionState) {
                            ExecutionControlState.Active -> DropdownMenuItem(
                                text = { Text("Pause") },
                                leadingIcon = { Icon(Icons.Outlined.Pause, contentDescription = null) },
                                onClick = { menuOpen = false; onPause() },
                                enabled = !executionControlInFlight,
                            )
                            ExecutionControlState.Paused -> DropdownMenuItem(
                                text = { Text("Resume") },
                                leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                                onClick = { menuOpen = false; onResume() },
                                enabled = !executionControlInFlight,
                            )
                            null -> DropdownMenuItem(
                                text = { Text("Start execution") },
                                onClick = { menuOpen = false; onStartExecution() },
                                enabled = !executionControlInFlight,
                            )
                        }
                        if (executionState != null) {
                            DropdownMenuItem(
                                text = { Text("Finish execution") },
                                onClick = { menuOpen = false; onFinishExecution() },
                                enabled = !executionControlInFlight,
                            )
                        }
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        },
        modifier = Modifier
            .testTag("execute-tile-${tile.id}")
            .clickable(onClick = onTap)
            .semantics { contentDescription = "${lifecycle.name}: ${tile.title}" },
    )
}
