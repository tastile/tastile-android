package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecutionControlState
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppSecondaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
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
    val executionControlsInFlight by viewModel.executionControlInFlightTileIds.collectAsStateWithLifecycle()

    if (current is Overlay.TileEdit) {
        var editedTitle by remember(tile?.id) { mutableStateOf(tile?.title.orEmpty()) }
        var confirmSave by remember(tile?.id) { mutableStateOf(false) }
        var confirmExecutionAction by remember(tile?.id) { mutableStateOf<Boolean?>(null) }
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
                        text = stringResource(R.string.tile_occurrence_label, placementId),
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
                    AppPrimaryButton(
                        text = "Save changes",
                        onClick = { confirmSave = true },
                        enabled = editedTitle.isNotBlank(),
                        modifier = Modifier.testTag("tile-edit-save-details"),
                    )
                    if (lifecycle == TileLifecycle.READY) {
                        AppSecondaryButton(
                            text = "Start",
                            onClick = { viewModel.startTile(selected.id) },
                        )
                        AppSecondaryButton(
                            text = "Defer",
                            onClick = { viewModel.setDeferTileCandidate(selected.id) },
                        )
                        AppSecondaryButton(
                            text = "Request prompt",
                            onClick = { viewModel.setPromptTileCandidate(selected.id) },
                        )
                    }
                    if (lifecycle == TileLifecycle.STARTED) {
                        AppSecondaryButton(
                            text = "Complete",
                            onClick = { viewModel.completeTile(selected.id) },
                        )
                        when (executionStates[selected.id]) {
                            ExecutionControlState.Active -> AppSecondaryButton(
                                text = "Pause",
                                onClick = { viewModel.pauseTile(selected.id) },
                                enabled = selected.id !in executionControlsInFlight,
                            )
                            ExecutionControlState.Paused -> AppSecondaryButton(
                                text = "Resume",
                                onClick = { viewModel.resumeTile(selected.id) },
                                enabled = selected.id !in executionControlsInFlight,
                            )
                            null -> AppSecondaryButton(
                                text = "Start execution",
                                onClick = { confirmExecutionAction = true },
                                enabled = selected.id !in executionControlsInFlight,
                            )
                        }
                        if (executionStates[selected.id] != null) {
                            AppSecondaryButton(
                                text = "Finish execution",
                                onClick = { confirmExecutionAction = false },
                                enabled = selected.id !in executionControlsInFlight,
                            )
                        }
                    }
                    AppSecondaryButton(
                        text = if ((current as Overlay.TileEdit).placementId != null) "Delete occurrence" else "Delete",
                        onClick = {
                            val placementId = (current as Overlay.TileEdit).placementId
                            if (placementId != null) viewModel.setClosePlacementCandidate(placementId)
                            else viewModel.setDeleteTileCandidate(selected.id)
                        },
                    )
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
            AlertDialog(
                onDismissRequest = { viewModel.setClosePlacementCandidate(null) },
                title = { Text("Delete occurrence?") },
                text = { Text("Only this calendar occurrence will be removed.") },
                confirmButton = {
                    AppPrimaryButton(
                        text = "Delete",
                        onClick = viewModel::confirmClosePlacement,
                    )
                },
                dismissButton = {
                    AppTertiaryButton(
                        text = "Cancel",
                        onClick = { viewModel.setClosePlacementCandidate(null) },
                    )
                },
            )
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
        confirmExecutionAction?.takeIf { selected != null }?.let { start ->
            AlertDialog(
                onDismissRequest = { confirmExecutionAction = null },
                title = { Text(if (start) "Start execution?" else "Finish execution?") },
                text = { Text(if (start) "Start work on this occurrence." else "Finish this execution without completing the tile.") },
                confirmButton = {
                    AppPrimaryButton(
                        text = if (start) "Start" else "Finish",
                        onClick = {
                            if (start) viewModel.startExecution(selected!!.id) else viewModel.finishExecution(selected!!.id)
                            confirmExecutionAction = null
                        },
                    )
                },
                dismissButton = {
                    AppTertiaryButton(
                        text = "Cancel",
                        onClick = { confirmExecutionAction = null },
                    )
                },
            )
        }
        if (confirmSave && selected != null) {
            AlertDialog(
                onDismissRequest = { confirmSave = false },
                title = { Text("Save tile changes?") },
                text = { Text("Update ${selected.title} to $editedTitle.") },
                confirmButton = {
                    AppPrimaryButton(
                        text = "Save",
                        onClick = {
                            viewModel.updateTileTitle(selected.id, editedTitle)
                            confirmSave = false
                        },
                    )
                },
                dismissButton = {
                    AppTertiaryButton(
                        text = "Cancel",
                        onClick = { confirmSave = false },
                    )
                },
            )
        }
    }
}
