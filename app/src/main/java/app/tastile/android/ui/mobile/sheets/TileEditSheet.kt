package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecutionControlState
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.mobile.tabs.tiles.DeleteTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.DeferTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.PromptRequestDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileEditSheet(
    overlay: OverlayViewModel,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val tile by viewModel.selectedTile.collectAsStateWithLifecycle()
    val deleteCandidate by viewModel.requestDeleteTileId.collectAsStateWithLifecycle()
    val closePlacementCandidate by viewModel.requestClosePlacementId.collectAsStateWithLifecycle()
    val deferCandidate by viewModel.requestDeferTileId.collectAsStateWithLifecycle()
    val promptCandidate by viewModel.requestPromptTileId.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val executionStates by viewModel.executionControlStates.collectAsStateWithLifecycle()

    if (current is Overlay.TileEdit) {
        var editedTitle by remember(tile?.id) { mutableStateOf(tile?.title.orEmpty()) }
        var confirmSave by remember(tile?.id) { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        PanelSheet(
            title = tile?.title ?: "Tile",
            sheetState = sheetState,
            onDismiss = {
                viewModel.clearSelectedTile()
                overlay.dismiss()
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = tile?.lifecycle ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                (current as Overlay.TileEdit).placementId?.let { placementId ->
                    Text(
                        text = "Occurrence: $placementId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                tile?.let { selected ->
                    val lifecycle = TileLifecycle.fromString(selected.lifecycle)
                    Text("Edit details", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    TextButton(
                        onClick = { confirmSave = true },
                        enabled = editedTitle.isNotBlank(),
                        modifier = Modifier.testTag("tile-edit-save-details"),
                    ) { Text("Save changes") }
                    if (lifecycle == TileLifecycle.READY) {
                        TextButton(onClick = { viewModel.startTile(selected.id) }) { Text("Start") }
                        TextButton(onClick = { viewModel.setDeferTileCandidate(selected.id) }) { Text("Defer") }
                        TextButton(onClick = { viewModel.setPromptTileCandidate(selected.id) }) { Text("Request prompt") }
                    }
                    if (lifecycle == TileLifecycle.STARTED) {
                        TextButton(onClick = { viewModel.completeTile(selected.id) }) { Text("Complete") }
                        when (executionStates[selected.id]) {
                            ExecutionControlState.Active -> TextButton(onClick = { viewModel.pauseTile(selected.id) }) { Text("Pause") }
                            ExecutionControlState.Paused -> TextButton(onClick = { viewModel.resumeTile(selected.id) }) { Text("Resume") }
                            null -> Unit
                        }
                    }
                    TextButton(onClick = {
                        val placementId = (current as Overlay.TileEdit).placementId
                        if (placementId != null) viewModel.setClosePlacementCandidate(placementId)
                        else viewModel.setDeleteTileCandidate(selected.id)
                    }) { Text(if ((current as Overlay.TileEdit).placementId != null) "Delete occurrence" else "Delete") }
                }
            }
        }
        val selected = tile
        deleteCandidate?.takeIf { it == selected?.id }?.let {
            DeleteTileDialog(
                tileTitle = selected?.title,
                onConfirm = viewModel::confirmDeleteTile,
                onCancel = { viewModel.setDeleteTileCandidate(null) },
            )
        }
        closePlacementCandidate?.takeIf { (current as Overlay.TileEdit).placementId == it }?.let {
            AlertDialog(onDismissRequest = { viewModel.setClosePlacementCandidate(null) }, title = { Text("Delete occurrence?") }, text = { Text("Only this calendar occurrence will be removed.") }, confirmButton = { TextButton(onClick = viewModel::confirmClosePlacement) { Text("Delete") } }, dismissButton = { TextButton(onClick = { viewModel.setClosePlacementCandidate(null) }) { Text("Cancel") } })
        }
        deferCandidate?.takeIf { it == selected?.id }?.let {
            DeferTileDialog(
                tileTitle = selected?.title,
                onConfirm = viewModel::confirmDeferTile,
                onCancel = { viewModel.setDeferTileCandidate(null) },
            )
        }
        promptCandidate?.takeIf { it == selected?.id }?.let {
            PromptRequestDialog(
                tileTitle = selected?.title,
                onConfirm = viewModel::confirmPromptTile,
                onCancel = { viewModel.setPromptTileCandidate(null) },
            )
        }
        if (confirmSave && selected != null) {
            AlertDialog(
                onDismissRequest = { confirmSave = false },
                title = { Text("Save tile changes?") },
                text = { Text("Update ${selected.title} to $editedTitle.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateTileTitle(selected.id, editedTitle)
                        confirmSave = false
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { confirmSave = false }) { Text("Cancel") } },
            )
        }
    }
}
