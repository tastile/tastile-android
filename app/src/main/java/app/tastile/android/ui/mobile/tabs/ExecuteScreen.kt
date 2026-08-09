package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.MobileSpacing
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaCard
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.core.designsystem.component.NiaLoadingWheel
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.model.projectLabels
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecutionControlState
import app.tastile.android.ui.dashboard.TaskBucket
import app.tastile.android.ui.dashboard.TaskBucketGroup
import app.tastile.android.ui.dashboard.isStarted
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.components.AppEmptyState
import app.tastile.android.ui.mobile.tabs.tiles.DeleteTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.DeferTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.PromptRequestDialog

private val MobSpacingXs get() = MobileSpacing.xs
private val MobSpacingSm get() = MobileSpacing.sm
private val MobSpacingMd get() = MobileSpacing.md
private val MobSpacingLg get() = MobileSpacing.lg

/**
 * Mobile Tasks tab.
 *
 * Mirrors `tastile-web/src/app/dashboard/tasks/tasks-page-client.tsx`'s
 * time-bucketed grouping:
 *
 *   1. Active tile hero (the single STARTED tile, if any) at the top.
 *   2. A scrollable list grouped by [TaskBucket] in this order:
 *      Today → This Week → Later → No Date.
 *      Each section shows a header with the bucket label and a count,
 *      followed by per-tile rows that mirror the existing row layout
 *      (tile glyph + title + status chip + project/date label +
 *      trailing more-action menu).
 *   3. Empty state when there is nothing to show.
 *
 * The list scrolls because each tile row is rendered by a `LazyColumn`,
 * not a non-scrollable `Column.forEach` (the bug this screen used to
 * ship with — see `docs/ux-investigation-tasks-projects.md` Section 1).
 */
@Composable
fun ExecuteScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val buckets by viewModel.tasksByBucket.collectAsStateWithLifecycle()
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
    val totalShowable = buckets.sumOf { it.tiles.size }
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().testTag("tasks-screen-body")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MobSpacingMd)
                .testTag("tasks-list"),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(MobSpacingXs),
            contentPadding = PaddingValues(top = MobSpacingSm, bottom = MobSpacingLg),
        ) {
            error?.let { message ->
                item(key = "tasks-error", contentType = "error") {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = MobSpacingXs),
                    )
                }
            }
            actionMessage?.let { message ->
                item(key = "tasks-action-message", contentType = "action-message") {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = MobSpacingXs),
                    )
                }
            }
            active?.let { activeTile ->
                item(
                    key = "tasks-active-hero-${activeTile.id}",
                    contentType = "active-hero",
                ) {
                    ActiveTileHero(
                        tile = activeTile,
                        executionState = executionStates[activeTile.id],
                        executionControlInFlight = activeTile.id in executionControlsInFlight,
                        onStartExecution = { executionActionCandidate = activeTile.id to true },
                        onFinishExecution = { executionActionCandidate = activeTile.id to false },
                        viewModel = viewModel,
                    )
                }
            }

            if (totalShowable == 0 && active == null) {
                item(key = "tasks-empty", contentType = "empty") {
                    AppEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MobSpacingLg),
                        icon = Icons.Outlined.EventBusy,
                        title = stringResource(R.string.tasks_empty_title),
                        hint = stringResource(R.string.tasks_empty_hint),
                        actionLabel = stringResource(R.string.quick_create_title),
                        onAction = { overlay.show(Overlay.QuickCreate) },
                    )
                }
            } else {
                buckets.forEach { group ->
                    item(
                        key = "tasks-bucket-header-${group.bucket.groupId}",
                        contentType = "bucket-header",
                    ) {
                        BucketHeader(group = group)
                    }
                    items(
                        items = group.tiles,
                        key = { tile -> "tasks-row-${group.bucket.groupId}-${tile.id}" },
                        contentType = { "task-row" },
                    ) { tile ->
                        TaskRow(
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
            title = {
                Text(
                    if (start) {
                        stringResource(R.string.tasks_start_execution_title)
                    } else {
                        stringResource(R.string.tasks_finish_execution_title)
                    },
                )
            },
            text = {
                Text(
                    if (start) {
                        stringResource(R.string.tasks_start_execution_body)
                    } else {
                        stringResource(R.string.tasks_finish_execution_body)
                    },
                )
            },
            confirmButton = {
                NiaButton(
                    onClick = {
                        if (start) viewModel.startExecution(tileId) else viewModel.finishExecution(tileId)
                        executionActionCandidate = null
                    },
                    text = {
                        Text(
                            if (start) {
                                stringResource(R.string.tasks_start_button)
                            } else {
                                stringResource(R.string.tasks_finish_button)
                            },
                        )
                    },
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
                    text = { Text(stringResource(R.string.common_cancel)) },
                    leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                )
            },
        )
    }
}

@Composable
private fun BucketHeader(group: TaskBucketGroup) {
    val label = stringResource(bucketTitleRes(group.bucket))
    val icon: ImageVector = when (group.bucket) {
        TaskBucket.TODAY -> Icons.Outlined.Today
        TaskBucket.THIS_WEEK -> Icons.Outlined.AccessTime
        TaskBucket.LATER -> Icons.Outlined.AccessTime
        TaskBucket.NO_DATE -> Icons.Outlined.EventBusy
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MobSpacingSm, bottom = MobSpacingXs)
            .testTag("tasks-bucket-header-${group.bucket.groupId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MobSpacingXs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = group.tiles.size.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun bucketTitleRes(bucket: TaskBucket): Int = when (bucket) {
    TaskBucket.TODAY -> R.string.tasks_bucket_today
    TaskBucket.THIS_WEEK -> R.string.tasks_bucket_this_week
    TaskBucket.LATER -> R.string.tasks_bucket_later
    TaskBucket.NO_DATE -> R.string.tasks_bucket_no_date
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
                Button(onClick = { viewModel.completeTile(tile.id) }) {
                    Text(stringResource(R.string.tasks_active_hero_complete))
                }
                when (executionState) {
                    ExecutionControlState.Active -> OutlinedButton(
                        onClick = { viewModel.pauseTile(tile.id) },
                        enabled = !executionControlInFlight,
                        modifier = Modifier.testTag("execute-pause-${tile.id}"),
                    ) { Text(stringResource(R.string.tasks_active_hero_pause)) }
                    ExecutionControlState.Paused -> OutlinedButton(
                        onClick = { viewModel.resumeTile(tile.id) },
                        enabled = !executionControlInFlight,
                        modifier = Modifier.testTag("execute-resume-${tile.id}"),
                    ) { Text(stringResource(R.string.tasks_active_hero_resume)) }
                    null -> OutlinedButton(
                        onClick = onStartExecution,
                        enabled = !executionControlInFlight,
                    ) { Text(stringResource(R.string.tasks_active_hero_start_execution)) }
                }
                if (executionState != null) {
                    OutlinedButton(
                        onClick = onFinishExecution,
                        enabled = !executionControlInFlight,
                    ) { Text(stringResource(R.string.tasks_active_hero_finish_execution)) }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
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
    val statusLabel = when (lifecycle) {
        TileLifecycle.READY -> stringResource(R.string.tasks_status_ready)
        TileLifecycle.STARTED -> stringResource(R.string.tasks_status_started)
        TileLifecycle.DONE -> stringResource(R.string.tasks_status_completed)
        TileLifecycle.ARCHIVED -> lifecycle.name
    }
    val scheduled = tile.projectedNextStartAt
        ?: tile.releaseAt
        ?: tile.fixedStart
    val rowDescription = stringResource(
        R.string.tasks_time_range_at,
        statusLabel,
        scheduled.orEmpty(),
    )
    var menuOpen by remember { mutableStateOf(false) }

    NiaListItem(
        content = { Text(tile.title) },
        leadingContent = {
            Text(glyph, style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            Column {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val projectName = tile.projectLabels().firstOrNull()
                when {
                    projectName != null -> Text(
                        text = projectName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    scheduled != null -> Text(
                        text = scheduled,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.tasks_more_actions),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (lifecycle == TileLifecycle.READY) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.tasks_action_start)) },
                            leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                            onClick = { menuOpen = false; onStart() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.tasks_action_defer)) },
                            onClick = { menuOpen = false; onDefer() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.tasks_action_request_prompt)) },
                            onClick = { menuOpen = false; onPrompt() },
                        )
                    }
                    if (lifecycle == TileLifecycle.STARTED) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.tasks_action_complete)) },
                            onClick = { menuOpen = false; onComplete() },
                        )
                        when (executionState) {
                            ExecutionControlState.Active -> DropdownMenuItem(
                                text = { Text(stringResource(R.string.tasks_action_pause)) },
                                leadingIcon = { Icon(Icons.Outlined.Pause, contentDescription = null) },
                                onClick = { menuOpen = false; onPause() },
                                enabled = !executionControlInFlight,
                            )
                            ExecutionControlState.Paused -> DropdownMenuItem(
                                text = { Text(stringResource(R.string.tasks_action_resume)) },
                                leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                                onClick = { menuOpen = false; onResume() },
                                enabled = !executionControlInFlight,
                            )
                            null -> DropdownMenuItem(
                                text = { Text(stringResource(R.string.tasks_active_hero_start_execution)) },
                                onClick = { menuOpen = false; onStartExecution() },
                                enabled = !executionControlInFlight,
                            )
                        }
                        if (executionState != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.tasks_active_hero_finish_execution)) },
                                onClick = { menuOpen = false; onFinishExecution() },
                                enabled = !executionControlInFlight,
                            )
                        }
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tasks_action_delete)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        },
        modifier = Modifier
            .testTag("execute-tile-${tile.id}")
            .clickable(onClick = onTap)
            .semantics { contentDescription = rowDescription },
    )
}
