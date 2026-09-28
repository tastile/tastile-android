package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Warning
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.CircularProgressIndicator
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: m3-component
import androidx.compose.material3.HorizontalDivider
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
import app.tastile.android.core.designsystem.component.rememberNiaModalBottomSheetState
import app.tastile.android.core.designsystem.theme.LocalTastileCardRoleTokens
import app.tastile.android.core.designsystem.theme.LocalTastileStatusTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecutionControlState
import app.tastile.android.ui.dashboard.TileUpdateField
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.sheets.quickcreate.DetailsAffordanceButton
import app.tastile.android.ui.mobile.sheets.quickcreate.FormRow
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateHeader
import app.tastile.android.ui.mobile.sheets.quickcreate.UnderlineTextArea
import app.tastile.android.ui.mobile.sheets.quickcreate.UnderlineTextField
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
@OptIn(ExperimentalMaterial3Api::class)
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
        // Source reads/writes address the canonical source id. Timeline
        // occurrences carry a placement tile id that 404s against
        // GET /v1/source-tiles/{id} (A05); prefer the timeline's
        // source_tile_id whenever the entry point supplied one.
        // For placements without a source tile (legacy / v0 era), leave
        // detailId null so the sheet renders the cached Tile without
        // making a doomed server call.
        val detailId = tileEdit.sourceTileId ?: tileId
        // Trigger the v1 source-tile detail fetch whenever the sheet opens for
        // a new tile id. The repository's read path is suspended + fault-tolerant
        // (returns null on auth/network/server errors), so the UI only ever
        // renders a placeholder or the real payload — never a hard error.
        if (detailId != null) {
            LaunchedEffect(detailId) {
                viewModel.loadTileDetail(detailId)
            }
        }
        // The QuickCreateStateStore is keyed by the (tileId, placementId) pair
        // so the same tile reopens with the same draft, but a different tile
        // gets a clean store seeded from the freshly loaded detail.
        val store = remember(tileId, tileEdit.placementId) {
            QuickCreateStateStore()
        }
        // Local-first seed (no network): the timeline/tiles rows already carry
        // the title, so the editor shows it instantly instead of waiting for
        // the source-tile detail round-trip. Runs once per tile while the
        // draft is still pristine; detail hydration later overwrites only
        // fields the user has not touched.
        LaunchedEffect(tile?.id) {
            val t = tile
            val cur = store.state.value
            if (t != null && cur.editingTileId == null && cur.identity.title.isBlank() && t.title.isNotBlank()) {
                store.updateIdentity(cur.identity.copy(title = t.title))
            }
        }
        LaunchedEffect(detail, detailId, tileEdit.placementId) {
            val currentDetail = detail
            val resolvedId = detailId
            if (currentDetail != null && resolvedId != null) {
                val existing = store.state.value
                if (existing.editingTileId != resolvedId) {
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
                        tileId = resolvedId,
                        placementId = tileEdit.placementId,
                        detail = currentDetail,
                        workflow = initialWorkflow,
                    )
                    // Local-first editing: the form is fully interactive before
                    // the detail round-trip returns, so a first hydration must
                    // not clobber fields the user already touched. Blank title /
                    // description and store-default color / icon mean untouched.
                    if (existing.editingTileId == null) {
                        val hydrated = store.state.value.identity
                        var restored = hydrated
                        if (existing.identity.title.isNotBlank()) restored = restored.copy(title = existing.identity.title)
                        val preDescription = existing.identity.description
                        if (!preDescription.isNullOrBlank()) restored = restored.copy(description = preDescription)
                        val preColor = existing.identity.visual.color
                        if (!preColor.equals("#3b82f6", ignoreCase = true)) {
                            restored = restored.copy(visual = restored.visual.copy(color = preColor))
                        }
                        val preIcon = existing.identity.visual.icon
                        if (preIcon != "check-circle") {
                            restored = restored.copy(visual = restored.visual.copy(icon = preIcon))
                        }
                        if (restored != hydrated) store.updateIdentity(restored)
                    }
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
                    // A05: the edit form (identity + schedule + actions) is taller
                    // than the sheet viewport, so the content must scroll —
                    // otherwise the Actions row (Start/Complete/Delete) below
                    // the fold is unreachable by touch.
                    // Create-panel parity: no outer horizontal padding. Every
                    // row owns its 16dp gutter (FormRow / padded loose rows),
                    // so the icon track lines up with the header × centerline.
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val headerTitle = tile?.title
                    ?: detail?.source?.title
                    ?: if (detailLoading) "Loading tile…" else "Tile"
                // Create-panel parity: cancel × on the left, save pill on the
                // right. This header save is the only save entry point.
                // Gutters mirror QuickCreateHandleRow: × at start=4dp puts its
                // 48dp centerline on the body icon-track centerline (28dp),
                // save pill at end=16dp.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            viewModel.clearSelectedTile()
                            overlay.dismiss()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tile-edit-cancel"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.quick_create_close),
                            tint = LocalContentColor.current,
                        )
                    }
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tile-edit-header-title"),
                    )
                    NiaButton(
                        onClick = { confirmSave = true },
                        enabled = draft.identity.title.isNotBlank(),
                        modifier = Modifier.testTag("tile-edit-save-details"),
                        text = { Text(stringResource(R.string.tile_edit_save_details)) },
                    )
                }
                // Workflow batch — mirrors the peer workflow structure for
                // consistent authors. Pre-selected via hydrateForEdit.
                WorkflowBatch(
                    workflow = draft.workflow,
                    onWorkflowChange = { kind -> store.setWorkflow(kind) },
                    modifier = Modifier.testTag("tile-edit-workflow-batch"),
                )
                // Status rows use the same FormRow track as the form body so
                // the whole sheet sits on one icon column.
                FormRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    content = {
                        Text(
                            text = tile?.lifecycle ?: "—",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current,
                        )
                    },
                )
                error?.let { message ->
                    FormRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = LocalTastileStatusTokens.current.archived.icon,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        content = {
                            Text(message, color = LocalTastileStatusTokens.current.archived.icon)
                        },
                    )
                }
                if (detailLoading && detail == null) {
                    FormRow(
                        icon = {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("tile-edit-detail-loading"),
                            )
                        },
                        content = { },
                    )
                }
                if (detail != null && !detailLoading && error == null) {
                    // Only ask the server to retry when we actually had a
                    // source id to load. Legacy / v0 placements have no
                    // detail to fetch (A05).
                    if (detailId != null) {
                        FormRow(
                            modifier = Modifier
                                .clickable { viewModel.loadTileDetail(detailId) }
                                .testTag("tile-edit-retry"),
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = LocalContentColor.current,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            content = {
                                Text(
                                    text = stringResource(R.string.tile_edit_retry_loading),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = LocalContentColor.current,
                                )
                            },
                        )
                    }
                }
                // Create-panel form system, driven by the edit draft store.
                // Title renders instantly from local rows (seeded from the
                // tiles list, no network); description / color / icon /
                // schedule fill in from the source-tile detail when it
                // arrives. Those detail-held fields exist only in the detail
                // read — the list payloads do not carry them.
                if (tile != null) {
                    QuickCreateHeader(
                        title = draft.identity.title,
                        onTitleChange = { store.updateIdentity(draft.identity.copy(title = it)) },
                        modifier = Modifier.testTag("tile-edit-header"),
                        titleTestTag = "tile-edit-title-input",
                        placeholder = stringResource(R.string.tile_edit_title_label),
                    )
                    FormRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = LocalContentColor.current,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        content = {
                            UnderlineTextArea(
                                value = draft.identity.description.orEmpty(),
                                onValueChange = {
                                    store.updateIdentity(
                                        draft.identity.copy(description = it.takeIf { d -> d.isNotBlank() })
                                    )
                                },
                                placeholder = stringResource(R.string.tile_edit_description_hint),
                                testTag = "tile-edit-description-input",
                            )
                        },
                    )
                    FormRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null,
                                tint = LocalContentColor.current,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        content = {
                            EditColorDots(
                                selectedHex = draft.identity.visual.color,
                                onSelect = { hex ->
                                    store.updateIdentity(
                                        draft.identity.copy(visual = draft.identity.visual.copy(color = hex))
                                    )
                                },
                            )
                        },
                    )
                    FormRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEmotions,
                                contentDescription = null,
                                tint = LocalContentColor.current,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        content = {
                            UnderlineTextField(
                                value = draft.identity.visual.icon,
                                onValueChange = {
                                    store.updateIdentity(
                                        draft.identity.copy(visual = draft.identity.visual.copy(icon = it))
                                    )
                                },
                                placeholder = stringResource(R.string.tile_edit_icon_label),
                                testTag = "tile-edit-icon-input",
                            )
                        },
                    )
                    DetailsAffordanceButton(
                        label = stringResource(R.string.tile_edit_open_time),
                        onOpen = { store.openSubpanel(QuickCreatePanel.Time) },
                        modifier = Modifier.testTag("tile-edit-open-time"),
                        testTag = "tile-edit-open-time",
                    )
                    DetailsAffordanceButton(
                        label = stringResource(R.string.tile_edit_open_schedule),
                        onOpen = { store.openSubpanel(QuickCreatePanel.Schedule) },
                        modifier = Modifier.testTag("tile-edit-open-schedule"),
                        testTag = "tile-edit-open-schedule",
                    )
                    if (tileEdit.placementId != null) {
                        Text(
                            text = stringResource(R.string.tile_occurrence_label, tileEdit.placementId),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current,
                            modifier = Modifier.padding(horizontal = 16.dp),
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
                }
                tile?.let { selected ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        stringResource(R.string.tile_edit_actions_header),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    val lifecycle = TileLifecycle.fromString(selected.lifecycle)
                    if (lifecycle == TileLifecycle.READY) {
                        EditActionRow(
                            icon = Icons.Outlined.PlayArrow,
                            label = stringResource(R.string.tile_edit_start),
                            testTag = "tile-edit-start",
                            onClick = {
                                // A05: timeline occurrences already carry the placement id,
                                // so start the execution directly. Source-emitted tiles never
                                // satisfy the tile-start plan bootstrap; legacy tiles without
                                // a placement keep the tile-start route.
                                val placementId = tileEdit.placementId
                                if (placementId != null) viewModel.startPlacementExecution(placementId, selected.id)
                                else viewModel.startTile(selected.id)
                            },
                        )
                        EditActionRow(
                            icon = Icons.Outlined.Schedule,
                            label = stringResource(R.string.tile_edit_defer),
                            testTag = "tile-edit-defer",
                            onClick = { viewModel.setDeferTileCandidate(selected.id) },
                        )
                        EditActionRow(
                            icon = Icons.Outlined.Notifications,
                            label = stringResource(R.string.tile_edit_request_prompt),
                            testTag = "tile-edit-prompt",
                            onClick = { viewModel.setPromptTileCandidate(selected.id) },
                        )
                    }
                    if (lifecycle == TileLifecycle.STARTED) {
                        EditActionRow(
                            icon = Icons.Outlined.TaskAlt,
                            label = stringResource(R.string.tile_edit_complete),
                            testTag = "tile-edit-complete",
                            onClick = { viewModel.completeTile(selected.id) },
                        )
                        when (executionStates[selected.id]) {
                            ExecutionControlState.Active -> EditActionRow(
                                icon = Icons.Outlined.Pause,
                                label = stringResource(R.string.tile_edit_pause),
                                testTag = "tile-edit-pause",
                                enabled = selected.id !in executionControlsInFlight,
                                onClick = { viewModel.pauseTile(selected.id) },
                            )
                            ExecutionControlState.Paused -> EditActionRow(
                                icon = Icons.Outlined.PlayArrow,
                                label = stringResource(R.string.tile_edit_resume),
                                testTag = "tile-edit-resume",
                                enabled = selected.id !in executionControlsInFlight,
                                onClick = { viewModel.resumeTile(selected.id) },
                            )
                            null -> EditActionRow(
                                icon = Icons.Outlined.PlayArrow,
                                label = stringResource(R.string.tile_edit_start_execution),
                                testTag = "tile-edit-start-execution",
                                enabled = selected.id !in executionControlsInFlight,
                                onClick = { confirmExecutionAction = true },
                            )
                        }
                        if (executionStates[selected.id] != null) {
                            EditActionRow(
                                icon = Icons.Outlined.CheckCircle,
                                label = stringResource(R.string.tile_edit_finish_execution),
                                testTag = "tile-edit-finish-execution",
                                enabled = selected.id !in executionControlsInFlight,
                                onClick = { confirmExecutionAction = false },
                            )
                        }
                    }
                    EditActionRow(
                        icon = Icons.Outlined.DeleteOutline,
                        label = if (tileEdit.placementId != null) "Delete occurrence" else "Delete",
                        testTag = "tile-edit-delete-or-occurrence",
                        onClick = {
                            val placementId = tileEdit.placementId
                            if (placementId != null) viewModel.setClosePlacementCandidate(placementId)
                            else viewModel.setDeleteTileCandidate(selected.id)
                        },
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
 * Single action row in the create-panel form system: leading icon track,
 * body-large label, whole row tappable. Used for the edit-only lifecycle
 * actions (Start / Defer / Complete / …) so they sit on the same icon
 * column as the form rows above.
 */
@Composable
private fun EditActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FormRow(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = if (enabled) 1f else 0.38f),
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = LocalContentColor.current.copy(alpha = if (enabled) 1f else 0.38f),
            )
        },
    )
}

/**
 * Color dot row mirroring the create panel's swatch set
 * (`ProjectColorRow` web-parity Event colors). Dots instead of chips so the
 * edit form reads as the same component family as QuickCreate.
 */
private val EditColorSwatches: List<String> = listOf(
    "#3B82F6",
    "#10B981",
    "#A855F7",
    "#F59E0B",
    "#EF4444",
    "#6B7280",
)

@Composable
private fun EditColorDots(
    selectedHex: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditColorSwatches.forEach { hex ->
            val isSelected = hex.equals(selectedHex, ignoreCase = true)
            Surface(
                onClick = { onSelect(hex) },
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) LocalTastileCardRoleTokens.current.actionable.border
                    else LocalTastileCardRoleTokens.current.completed.border,
                ),
                modifier = Modifier
                    .size(24.dp)
                    .testTag("tile-edit-color-$hex"),
            ) {
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex))),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
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
    // Detail-held fields (description / color / icon / span) exist only in the
    // source-tile read, which may still be in flight when the user saves from
    // the instant local title row. Sending store defaults for them would
    // clobber server values, so they go out only after detail hydration
    // (editingTileId is set exclusively by hydrateForEdit).
    if (store.state.value.editingTileId == null) return
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
