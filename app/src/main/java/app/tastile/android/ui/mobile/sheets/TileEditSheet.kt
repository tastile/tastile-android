package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.CircularProgressIndicator
// m2-allow: m3-component
import androidx.compose.material3.HorizontalDivider
import app.tastile.android.core.designsystem.component.rememberNiaModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaOutlinedButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.data.api.SourceTileDetailRead
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecutionControlState
import app.tastile.android.ui.dashboard.TileUpdateField
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.sheets.WorkflowKind
import app.tastile.android.ui.mobile.sheets.quickcreate.WorkflowBatch
import app.tastile.android.ui.mobile.tabs.tiles.DeleteTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.DeferTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.PromptRequestDialog

/**
 * Edit panel for an existing tile. Mirrors the web `QuickTileEditPanel`
 * (`tastile-web/src/features/create-tile/ui/QuickCreate.tsx` in edit mode)
 * by reusing the same draft store + subpanels for the editable Identity /
 * Time / Schedule fields and adding a lifecycle-conditional action bar
 * (Start / Defer / Complete / Pause / Resume / Delete) on top.
 *
 * Data flow when the user changes a field:
 *   1. UI mutates the [QuickCreateStateStore] bound to the visible draft.
 *   2. The user taps "Save changes" — the confirm dialog calls
 *      [pushEdit] which routes per-field updates through
 *      [DashboardViewModel.updateTileField] (v1
 *      `POST /v1/tiles/{id}/update`) and, when a placement id is present,
 *      the time span through [DashboardViewModel.reschedulePlacement]
 *      (v1 `POST /v1/placements/{id}/changes`).
 *   3. After each update succeeds, the VM refreshes the selected-tile
 *      detail so the next composition shows the updated state.
 *
 * The existing dialogs (DeleteTileDialog, DeferTileDialog, PromptRequestDialog,
 * close-placement dialog, execution action confirm, save confirm) are
 * preserved verbatim from the prior implementation; they remain the
 * affordance for the destructive / irreversible actions that the new
 * editable fields cannot substitute for.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TileEditSheet(
    overlay: OverlayViewModel,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val tile by viewModel.selectedTile.collectAsStateWithLifecycle()
    val detail by viewModel.selectedTileDetail.collectAsStateWithLifecycle()
    val detailLoading by viewModel.selectedTileDetailLoading.collectAsStateWithLifecycle()
    val deleteCandidate by viewModel.requestDeleteTileId.collectAsStateWithLifecycle()
    val closePlacementCandidate by viewModel.requestClosePlacementId.collectAsStateWithLifecycle()
    val deferCandidate by viewModel.requestDeferTileId.collectAsStateWithLifecycle()
    val promptCandidate by viewModel.requestPromptTileId.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val executionStates by viewModel.executionControlStates.collectAsStateWithLifecycle()
    val executionControlsInFlight by viewModel.executionControlInFlightTileIds.collectAsStateWithLifecycle()

    if (current is Overlay.TileEdit) {
        val tileEdit = current as Overlay.TileEdit
        val tileId = tileEdit.tileId
        // Trigger the v1 source-tile detail fetch whenever the sheet opens for
        // a new tile id. The repository's read path is suspended + fault-tolerant
        // (returns null on auth/network/server errors), so the UI only ever
        // renders a placeholder or the real payload — never a hard error.
        LaunchedEffect(tileId) {
            tileId?.let(viewModel::loadTileDetail)
        }
        // The QuickCreateStateStore is keyed by the (tileId, placementId) pair
        // so the same tile reopens with the same draft, but a different tile
        // gets a clean store seeded from the freshly loaded detail.
        val store = remember(tileId, tileEdit.placementId) {
            QuickCreateStateStore()
        }
        LaunchedEffect(detail, tileId, tileEdit.placementId) {
            val currentDetail = detail
            if (currentDetail != null && tileId != null) {
                val existing = store.state.value
                if (existing.editingTileId != tileId) {
                    // Heuristic: recurring tiles set `schedule.generation.kind = 1`
                    // (Recurring) on the v1 wire; placement / event / task tiles
                    // leave it at 0 (OneTime) or 2 (DemandDriven). Pre-select the
                    // matching workflow so the user sees the correct base form
                    // when they reopen the edit sheet; the chip below lets them
                    // switch peer workflows without losing their draft.
                    val initialWorkflow = if (currentDetail.source.schedule.generation.kind == 1.toShort()) {
                        WorkflowKind.Recurring
                    } else {
                        WorkflowKind.Event
                    }
                    store.hydrateForEdit(
                        tileId = tileId,
                        placementId = tileEdit.placementId,
                        detail = currentDetail,
                        workflow = initialWorkflow,
                    )
                }
            }
        }
        val draft by store.state.collectAsStateWithLifecycle()
        var confirmSave by remember(tileId) { mutableStateOf(false) }
        var confirmExecutionAction by remember(tileId) { mutableStateOf<Boolean?>(null) }
        val sheetState = rememberNiaModalBottomSheetState()
        PanelSheet(
            sheetState = sheetState,
            onDismiss = {
                viewModel.clearSelectedTile()
                overlay.dismiss()
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val headerTitle = tile?.title
                    ?: detail?.source?.title
                    ?: if (detailLoading) "Loading tile…" else "Tile"
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("tile-edit-header-title"),
                )
                // Workflow batch — mirrors the peer workflow structure for
                // consistent authors. Pre-selected via hydrateForEdit.
                WorkflowBatch(
                    workflow = draft.workflow,
                    onWorkflowChange = { kind -> store.setWorkflow(kind) },
                    modifier = Modifier.testTag("tile-edit-workflow-batch"),
                )
                Text(
                    text = tile?.lifecycle ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                if (detailLoading && detail == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("tile-edit-detail-loading"),
                    )
                }
                if (detail == null && !detailLoading && error == null) {
                    NiaTextButton(
                        onClick = { tileId?.let(viewModel::loadTileDetail) },
                        text = { Text(stringResource(R.string.tile_edit_retry_loading)) },
                    )
                }
                if (detail != null && tileId != null) {
                    EditableIdentityBlock(
                        detail = detail!!,
                        onTitleChange = { newTitle ->
                            store.updateIdentity(draft.identity.copy(title = newTitle))
                        },
                        onDescriptionChange = { newDescription ->
                            store.updateIdentity(draft.identity.copy(description = newDescription))
                        },
                        onColorChange = { newColor ->
                            store.updateIdentity(
                                draft.identity.copy(visual = draft.identity.visual.copy(color = newColor))
                            )
                        },
                        onIconChange = { newIcon ->
                            store.updateIdentity(
                                draft.identity.copy(visual = draft.identity.visual.copy(icon = newIcon))
                            )
                        },
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NiaOutlinedButton(
                            onClick = { store.openSubpanel(QuickCreatePanel.Time) },
                            text = { Text(stringResource(R.string.tile_edit_open_time)) },
                            modifier = Modifier.testTag("tile-edit-open-time"),
                        )
                        NiaOutlinedButton(
                            onClick = { store.openSubpanel(QuickCreatePanel.Schedule) },
                            text = { Text(stringResource(R.string.tile_edit_open_schedule)) },
                            modifier = Modifier.testTag("tile-edit-open-schedule"),
                        )
                    }
                    if (tileEdit.placementId != null) {
                        Text(
                            text = stringResource(R.string.tile_occurrence_label, tileEdit.placementId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val active = draft.activePanel
                    if (active != null && active != QuickCreatePanel.Base) {
                        NiaTextButton(
                            onClick = { store.backToBase() },
                            text = { Text(stringResource(R.string.tile_edit_back)) },
                            modifier = Modifier.testTag("tile-edit-back"),
                        )
                    }
                    NiaButton(
                        onClick = { confirmSave = true },
                        enabled = draft.identity.title.isNotBlank(),
                        modifier = Modifier.testTag("tile-edit-save-details"),
                        text = { Text(stringResource(R.string.tile_edit_save_details)) },
                    )
                }
                tile?.let { selected ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.tile_edit_actions_header), style = MaterialTheme.typography.titleSmall)
                    val lifecycle = TileLifecycle.fromString(selected.lifecycle)
                    if (lifecycle == TileLifecycle.READY) {
                        NiaOutlinedButton(
                            onClick = { viewModel.startTile(selected.id) },
                            text = { Text(stringResource(R.string.tile_edit_start)) },
                            modifier = Modifier.testTag("tile-edit-start"),
                        )
                        NiaOutlinedButton(
                            onClick = { viewModel.setDeferTileCandidate(selected.id) },
                            text = { Text(stringResource(R.string.tile_edit_defer)) },
                            modifier = Modifier.testTag("tile-edit-defer"),
                        )
                        NiaOutlinedButton(
                            onClick = { viewModel.setPromptTileCandidate(selected.id) },
                            text = { Text(stringResource(R.string.tile_edit_request_prompt)) },
                            modifier = Modifier.testTag("tile-edit-prompt"),
                        )
                    }
                    if (lifecycle == TileLifecycle.STARTED) {
                        NiaOutlinedButton(
                            onClick = { viewModel.completeTile(selected.id) },
                            text = { Text(stringResource(R.string.tile_edit_complete)) },
                            modifier = Modifier.testTag("tile-edit-complete"),
                        )
                        when (executionStates[selected.id]) {
                            ExecutionControlState.Active -> NiaOutlinedButton(
                                onClick = { viewModel.pauseTile(selected.id) },
                                enabled = selected.id !in executionControlsInFlight,
                                text = { Text(stringResource(R.string.tile_edit_pause)) },
                            )
                            ExecutionControlState.Paused -> NiaOutlinedButton(
                                onClick = { viewModel.resumeTile(selected.id) },
                                enabled = selected.id !in executionControlsInFlight,
                                text = { Text(stringResource(R.string.tile_edit_resume)) },
                            )
                            null -> NiaOutlinedButton(
                                onClick = { confirmExecutionAction = true },
                                enabled = selected.id !in executionControlsInFlight,
                                text = { Text(stringResource(R.string.tile_edit_start_execution)) },
                            )
                        }
                        if (executionStates[selected.id] != null) {
                            NiaOutlinedButton(
                                onClick = { confirmExecutionAction = false },
                                enabled = selected.id !in executionControlsInFlight,
                                text = { Text(stringResource(R.string.tile_edit_finish_execution)) },
                            )
                        }
                    }
                    NiaOutlinedButton(
                        onClick = {
                            val placementId = tileEdit.placementId
                            if (placementId != null) viewModel.setClosePlacementCandidate(placementId)
                            else viewModel.setDeleteTileCandidate(selected.id)
                        },
                        text = {
                            Text(
                                if (tileEdit.placementId != null) "Delete occurrence" else "Delete",
                            )
                        },
                        modifier = Modifier.testTag("tile-edit-delete-or-occurrence"),
                    )
                }
            }
        }
        // Subpanel sheet — when the user opens Time / Schedule we stack a
        // second M3 sheet on top of the base. The subpanel sheet is
        // rendered from the same `store` the base panel reads, so changes
        // to time/span are visible in the base panel as soon as the
        // subpanel is dismissed.
        val active = draft.activePanel
        if (active != null && active != QuickCreatePanel.Base) {
            val subpanelSheetState = rememberNiaModalBottomSheetState(skipPartiallyExpanded = true)
            PanelSheet(
                sheetState = subpanelSheetState,
                onDismiss = { store.backToBase() },
            ) {
                app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSubpanel(
                    panel = active,
                    draft = draft,
                    store = store,
                    onBack = store::backToBase,
                    projects = emptyList(),
                    knownTags = emptyList(),
                )
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
        closePlacementCandidate?.takeIf { tileEdit.placementId == it }?.let {
            AlertDialog(
                onDismissRequest = { viewModel.setClosePlacementCandidate(null) },
                title = { Text(stringResource(R.string.tile_edit_delete_occurrence_title)) },
                text = { Text(stringResource(R.string.tile_edit_delete_occurrence_body)) },
                confirmButton = {
                    NiaButton(
                        onClick = viewModel::confirmClosePlacement,
                        text = { Text(stringResource(R.string.tile_edit_delete)) },
                    )
                },
                dismissButton = {
                    NiaTextButton(
                        onClick = { viewModel.setClosePlacementCandidate(null) },
                        text = { Text(stringResource(R.string.dialog_cancel)) },
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
                title = {
                    Text(
                        if (start) stringResource(R.string.tile_edit_start_execution_title)
                        else stringResource(R.string.tile_edit_finish_execution_title)
                    )
                },
                text = {
                    Text(
                        if (start) stringResource(R.string.tile_edit_start_execution_body)
                        else stringResource(R.string.tile_edit_finish_execution_body)
                    )
                },
                confirmButton = {
                    NiaButton(
                        onClick = {
                            if (start) viewModel.startExecution(selected!!.id) else viewModel.finishExecution(selected!!.id)
                            confirmExecutionAction = null
                        },
                        text = {
                            Text(
                                if (start) stringResource(R.string.tasks_start_button)
                                else stringResource(R.string.tile_edit_finish_button)
                            )
                        },
                    )
                },
                dismissButton = {
                    NiaTextButton(
                        onClick = { confirmExecutionAction = null },
                        text = { Text(stringResource(R.string.dialog_cancel)) },
                    )
                },
            )
        }
        if (confirmSave && selected != null && detail != null) {
            val newTitle = draft.identity.title.trim()
            AlertDialog(
                onDismissRequest = { confirmSave = false },
                title = { Text(stringResource(R.string.tile_edit_save_changes_title)) },
                text = { Text(stringResource(R.string.tile_edit_save_changes_body, selected.title, newTitle)) },
                confirmButton = {
                    NiaButton(
                        onClick = {
                            pushEdit(
                                viewModel = viewModel,
                                store = store,
                                tileId = selected.id,
                                placementId = tileEdit.placementId,
                            )
                            confirmSave = false
                        },
                        text = { Text(stringResource(R.string.tile_edit_save)) },
                    )
                },
                dismissButton = {
                    NiaTextButton(
                        onClick = { confirmSave = false },
                        text = { Text(stringResource(R.string.dialog_cancel)) },
                    )
                },
            )
        }
    }
}

/**
 * Editable identity block rendered at the top of the tile-edit sheet.
 *
 * Binds the v1 source-tile identity fields (title / description / color /
 * icon) to the local store so the user can edit them in place. Each field
 * is wired to a callback that mutates the store, so the same `Save changes`
 * flow that already exists can dispatch the v1 `update-tile` command.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditableIdentityBlock(
    detail: SourceTileDetailRead,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String?) -> Unit,
    onColorChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
) {
    val source = detail.source
    var title by remember(source.sourceTileId) { mutableStateOf(source.title) }
    var description by remember(source.sourceTileId) { mutableStateOf(source.description.orEmpty()) }
    var color by remember(source.sourceTileId) { mutableStateOf(source.color ?: "#3b82f6") }
    var icon by remember(source.sourceTileId) { mutableStateOf(source.icon ?: "check-circle") }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                onTitleChange(it)
            },
            label = { Text(stringResource(R.string.tile_edit_title_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tile-edit-title-input"),
            singleLine = true,
        )
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                onDescriptionChange(it.takeIf { d -> d.isNotBlank() })
            },
            label = { Text(stringResource(R.string.tile_edit_description_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tile-edit-description-input"),
            supportingText = { Text(stringResource(R.string.tile_edit_description_hint)) },
        )
        Text(stringResource(R.string.tile_edit_color_label), style = MaterialTheme.typography.labelLarge)
        val colorPalette = listOf(
            "#3b82f6" to stringResource(R.string.tile_edit_color_blue),
            "#22c55e" to stringResource(R.string.tile_edit_color_green),
            "#a855f7" to stringResource(R.string.tile_edit_color_purple),
            "#f97316" to stringResource(R.string.tile_edit_color_orange),
            "#ec4899" to stringResource(R.string.tile_edit_color_pink),
            "#06b6d4" to stringResource(R.string.tile_edit_color_cyan),
            "#eab308" to stringResource(R.string.tile_edit_color_yellow),
            "#ef4444" to stringResource(R.string.tile_edit_color_red),
            "#14b8a6" to stringResource(R.string.tile_edit_color_teal),
            "#6b7280" to stringResource(R.string.tile_edit_color_gray),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            colorPalette.forEach { (hex, label) ->
                FilterChip(
                    selected = color == hex,
                    onClick = {
                        color = hex
                        onColorChange(hex)
                    },
                    label = { Text(label) },
                    modifier = Modifier.testTag("tile-edit-color-$hex"),
                )
            }
        }
        OutlinedTextField(
            value = icon,
            onValueChange = {
                icon = it
                onIconChange(it)
            },
            label = { Text(stringResource(R.string.tile_edit_icon_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tile-edit-icon-input"),
            supportingText = { Text(stringResource(R.string.tile_edit_icon_hint)) },
            singleLine = true,
        )
    }
}

/**
 * Phase A of the edit save. Pushes each edited identity field through
 * [DashboardViewModel.updateTileField] (which calls the v1
 * `POST /v1/tiles/{id}/update` command) and, when a placement id is
 * present, the time span through [DashboardViewModel.reschedulePlacement]
 * (which calls the v1 `POST /v1/placements/{id}/changes` command).
 *
 * The split into per-field updates mirrors tastile-web's
 * `submitUpdateTile`; the v1 dispatcher treats absent fields as
 * "leave unchanged" so we only put the keys that actually changed.
 */
private fun pushEdit(
    viewModel: DashboardViewModel,
    store: QuickCreateStateStore,
    tileId: String,
    placementId: String?,
) {
    val draft = store.state.value
    val identity = draft.identity
    viewModel.updateTileField(tileId, TileUpdateField.TITLE, identity.title.trim())
    viewModel.updateTileField(tileId, TileUpdateField.DESCRIPTION, identity.description)
    viewModel.updateTileField(
        tileId = tileId,
        field = TileUpdateField.COLOR,
        value = identity.visual.color,
    )
    viewModel.updateTileField(
        tileId = tileId,
        field = TileUpdateField.ICON,
        value = identity.visual.icon,
    )
    if (placementId != null) {
        val start = draft.time.span.start.takeIf { it.isNotBlank() } ?: return
        val end = draft.time.span.end.takeIf { it.isNotBlank() } ?: return
        viewModel.reschedulePlacement(placementId, start, end)
    }
}
