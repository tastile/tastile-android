package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
// m2-allow: m3-api
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: m3-component
import androidx.compose.material3.Card
// m2-allow: m3-component
import androidx.compose.material3.CardDefaults
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenu
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
// m2-allow: m3-component
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedButton
// m2-allow: m3-component
import androidx.compose.material3.PrimaryScrollableTabRow
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: m3-component
import androidx.compose.material3.Tab
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SwapVert
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.MobileSpacing
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.core.designsystem.component.NiaLoadingWheel
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.model.projectLabels
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecutionControlState
import app.tastile.android.ui.dashboard.FixedTasksScope
import app.tastile.android.ui.dashboard.ProjectSection
import app.tastile.android.ui.dashboard.SortOrder
import app.tastile.android.ui.dashboard.isStarted
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.components.AppEmptyState
import app.tastile.android.ui.mobile.tabs.tiles.DeleteTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.DeferTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.PromptRequestDialog
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private val MobSpacingXs get() = MobileSpacing.xs
private val MobSpacingSm get() = MobileSpacing.sm
private val MobSpacingLg get() = MobileSpacing.lg

private object TasksGrid {
    val gutter: Dp = MobSpacingXs
    // v9.4 — Google Tasks screen gutter measured at x=22 (≈ 8dp) for
    // task cards. v9.2-9.3 used 8dp which left buckets/tasks flush with
    // the tab row, creating the "cramped" feel the user called out.
    // Match the accordion's left edge to its header by giving the body
    // the same leading 22dp inset.
    val columnInset: Dp = MobSpacingLg + MobSpacingXs * 2  // 20dp
    val rowGap: Dp = MobSpacingSm
    val rowLeadingWidth: Dp = MobSpacingLg * 2
    val listBottom: Dp = MobSpacingLg * 2
    val tabHorizontalPadding: Dp = MobSpacingLg
    val rowMinHeight: Dp = 40.dp
    // Bucket label sits ~30dp from the screen edge (Google "マイタスク" x=88
    // ≈ 33dp). Sized to align with the new 22dp body inset plus an extra
    // 8dp so the label reads as a deliberate anchor, not a margin error.
    val bucketLeadingInset: Dp = MobSpacingLg + MobSpacingXs * 2  // 20dp
    val sectionBarVerticalPad: Dp = MobSpacingXs
    val tileTrailingPad: Dp = MobSpacingLg + MobSpacingXs  // 18dp
}

@Composable
private fun completedTextStyle(): TextStyle =
    MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecuteScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val completedTiles by viewModel.completedTiles.collectAsStateWithLifecycle()
    val sections by viewModel.projectSections.collectAsStateWithLifecycle()
    val visibleSection by viewModel.visibleSection.collectAsStateWithLifecycle()
    val selectedSectionId by viewModel.selectedSectionId.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val deleteCandidate by viewModel.requestDeleteTileId.collectAsStateWithLifecycle()
    val deferCandidate by viewModel.requestDeferTileId.collectAsStateWithLifecycle()
    val promptCandidate by viewModel.requestPromptTileId.collectAsStateWithLifecycle()
    val actionMessage by viewModel.lastActionMessage.collectAsStateWithLifecycle()
    val executionStates by viewModel.executionControlStates.collectAsStateWithLifecycle()
    val executionControlsInFlight by viewModel.executionControlInFlightTileIds.collectAsStateWithLifecycle()
    var executionActionCandidate by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var doneExpanded by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(true) }

    if (loading && tiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            NiaLoadingWheel(contentDesc = stringResource(R.string.common_loading))
        }
        return
    }

    val listState = rememberLazyListState()
    val displayedTiles = visibleSection.tiles

    Box(modifier = Modifier.fillMaxSize().testTag("tasks-screen-body")) {
        DropdownMenu(
            expanded = sortMenuOpen,
            onDismissRequest = { sortMenuOpen = false },
            modifier = Modifier.testTag("tasks-sort-menu"),
        ) {
            SortOrder.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        sortMenuOpen = false
                        viewModel.setSortOrder(option)
                    },
                    modifier = Modifier.testTag("tasks-sort-option-${option.id}"),
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tasks-list"),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(TasksGrid.rowGap),
            contentPadding = PaddingValues(top = 0.dp, bottom = TasksGrid.listBottom),
        ) {
            item(key = "tasks-scope-tabs", contentType = "scope-tabs") {
                ProjectTabsRow(
                    sections = sections,
                    selectedId = selectedSectionId,
                    onSelect = viewModel::setSelectedSection,
                    onNewListClick = { overlay.show(Overlay.QuickCreate) },
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }

            error?.let { message ->
                item(key = "tasks-error", contentType = "error") {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(horizontal = TasksGrid.columnInset,
                                vertical = TasksGrid.gutter),
                    )
                }
            }
            actionMessage?.let { message ->
                item(key = "tasks-action-message", contentType = "action-message") {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = TasksGrid.columnInset,
                                vertical = TasksGrid.gutter),
                    )
                }
            }

            item(
                key = "tasks-accordion-group-${visibleSection.id}",
                contentType = "accordion-group",
            ) {
                AccordionGroup(
                    label = visibleSection.label,
                    sectionId = visibleSection.id,
                    expanded = sectionExpanded,
                    onToggle = { sectionExpanded = !sectionExpanded },
                    onSortClick = { sortMenuOpen = true },
                    tiles = displayedTiles,
                    isEmptyFallback = if (!sectionExpanded) null else if (displayedTiles.isEmpty()) {
                        @Composable {
                            AppEmptyState(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tasks-empty")
                                    .padding(vertical = MobSpacingLg),
                                icon = Icons.Outlined.EventBusy,
                                title = stringResource(R.string.tasks_empty_title),
                                hint = stringResource(R.string.tasks_empty_hint),
                                actionLabel = stringResource(R.string.quick_create_title),
                                onAction = { overlay.show(Overlay.QuickCreate) },
                            )
                        }
                    } else null,
                    onTileTap = { id: String ->
                        viewModel.selectTile(id)
                        overlay.show(Overlay.TileEdit(id))
                    },
                    onTileStart = viewModel::startTile,
                    onTileComplete = viewModel::completeTile,
                    tileExecutionStates = executionStates,
                    tileExecutionControlsInFlight = executionControlsInFlight,
                    onTileStartExecution = { id: String -> executionActionCandidate = Pair(id, true) },
                    onTileFinishExecution = { id: String -> executionActionCandidate = Pair(id, false) },
                    onTilePause = viewModel::pauseTile,
                    onTileResume = viewModel::resumeTile,
                    onTileDelete = viewModel::setDeleteTileCandidate,
                    onTileDefer = viewModel::setDeferTileCandidate,
                    onTilePrompt = viewModel::setPromptTileCandidate,
                )
            }

            item(key = "tasks-done", contentType = "done-card") {
                DoneCard(
                    count = completedTiles.size,
                    expanded = doneExpanded,
                    onToggle = { doneExpanded = !doneExpanded },
                )
            }
            if (doneExpanded) {
                items(
                    items = completedTiles,
                    key = { tile -> "tasks-done-row-${tile.id}" },
                    contentType = { "task-row" },
                ) { tile ->
                    CompletedTileRow(
                        tile = tile,
                        onTap = {
                            viewModel.selectTile(tile.id)
                            overlay.show(Overlay.TileEdit(tile.id))
                        },
                        onStart = { viewModel.startTile(tile.id) },
                        onComplete = { viewModel.completeTile(tile.id) },
                        executionState = executionStates[tile.id],
                        executionControlInFlight = tile.id in executionControlsInFlight,
                        onStartExecution = { executionActionCandidate = Pair(tile.id, true) },
                        onFinishExecution = { executionActionCandidate = Pair(tile.id, false) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectTabsRow(
    sections: List<ProjectSection>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onNewListClick: () -> Unit,
) {
    if (sections.isEmpty() && selectedId != "starred") return
    val selectedTabIndex = sections.indexOfFirst { it.id == selectedId }
        .takeIf { it >= 0 } ?: 0
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = 0.dp,
        minTabWidth = 0.dp,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tasks-scope-tabs-row"),
    ) {
        Tab(
            selected = selectedId == "starred",
            onClick = { onSelect("starred") },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("tasks-scope-tab-starred"),
        ) {
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = TasksGrid.tabHorizontalPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "★",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        sections.filter { it.id != FixedTasksScope.STARRED.id }.forEach { section ->
            Tab(
                selected = section.id == selectedId,
                onClick = { onSelect(section.id) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("tasks-scope-tab-${section.id}"),
            ) {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = TasksGrid.tabHorizontalPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Tab(
            selected = false,
            onClick = onNewListClick,
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("tasks-scope-tab-new-list"),
        ) {
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = TasksGrid.tabHorizontalPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.tasks_tab_new_list),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun AccordionGroup(
    label: String,
    sectionId: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSortClick: () -> Unit,
    tiles: List<Tile>,
    isEmptyFallback: (@Composable () -> Unit)?,
    onTileTap: (String) -> Unit,
    onTileStart: (String) -> Unit,
    onTileComplete: (String) -> Unit,
    tileExecutionStates: Map<String, ExecutionControlState>,
    tileExecutionControlsInFlight: Set<String>,
    onTileStartExecution: (String) -> Unit,
    onTileFinishExecution: (String) -> Unit,
    onTilePause: (String) -> Unit,
    onTileResume: (String) -> Unit,
    onTileDelete: (String) -> Unit,
    onTileDefer: (String) -> Unit,
    onTilePrompt: (String) -> Unit,
) {
    val containerColor = if (expanded) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TasksGrid.columnInset)
            .testTag("tasks-accordion-${sectionId}"),
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(start = TasksGrid.bucketLeadingInset,
                        end = TasksGrid.columnInset,
                        top = TasksGrid.sectionBarVerticalPad,
                        bottom = TasksGrid.sectionBarVerticalPad)
                    .testTag("tasks-section-bar"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tasks-bucket-label-${sectionId}"),
                )
                Box(
                    modifier = Modifier
                        .clickable(onClick = onSortClick)
                        .padding(4.dp)
                        .testTag("tasks-sort-button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapVert,
                        contentDescription = stringResource(R.string.tasks_sort_button),
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp
                    else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
            if (expanded) {
                if (tiles.isEmpty() && isEmptyFallback != null) {
                    isEmptyFallback()
                } else {
                    tiles.forEach { tile ->
                        TileRow(
                            tile = tile,
                            onTap = { onTileTap(tile.id) },
                            onStart = { onTileStart(tile.id) },
                            onComplete = { onTileComplete(tile.id) },
                            executionState = tileExecutionStates[tile.id],
                            executionControlInFlight = tile.id in tileExecutionControlsInFlight,
                            onStartExecution = { onTileStartExecution(tile.id) },
                            onFinishExecution = { onTileFinishExecution(tile.id) },
                            onPause = { onTilePause(tile.id) },
                            onResume = { onTileResume(tile.id) },
                            onDelete = { onTileDelete(tile.id) },
                            onDefer = { onTileDefer(tile.id) },
                            onPrompt = { onTilePrompt(tile.id) },
                        )
                    }
                }
            }
        }
    }
}

private fun formatScheduledLabel(
    iso: String?,
    tomorrowLabel: String,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val here = Instant.parse(iso).toLocalDateTime(zone)
        val nowDate: LocalDate = Clock.System.now().toLocalDateTime(zone).date
        val tomorrowDate = nowDate.plus(1, DateTimeUnit.DAY)
        val timeText = "%02d:%02d".format(here.hour, here.minute)
        when (here.date) {
            nowDate -> timeText
            tomorrowDate -> tomorrowLabel
            else -> timeText
        }
    } catch (_: Throwable) {
        null
    }
}

@Composable
private fun TileRow(
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
    val rawScheduled = tile.projectedNextStartAt ?: tile.releaseAt ?: tile.fixedStart
    val formattedScheduled = formatScheduledLabel(
        iso = rawScheduled,
        tomorrowLabel = stringResource(R.string.tasks_schedule_tomorrow),
    )
    val scheduledForDescription = formattedScheduled ?: rawScheduled
    val rowDescription = stringResource(
        R.string.tasks_time_range_at,
        statusLabel,
        scheduledForDescription.orEmpty(),
    )
    var menuOpen by remember { mutableStateOf(false) }

    val projectName = tile.projectLabels().firstOrNull()
    val subLine: String? = when {
        projectName != null && formattedScheduled != null ->
            "$projectName  ·  $formattedScheduled"
        projectName != null -> projectName
        formattedScheduled != null -> formattedScheduled
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .semantics { contentDescription = rowDescription }
            .padding(horizontal = TasksGrid.columnInset)
            .testTag("execute-tile-${tile.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TasksGrid.rowMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(TasksGrid.rowLeadingWidth)
                    .height(TasksGrid.rowMinHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = glyph,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = tile.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TasksGrid.gutter),
            )
            Box(modifier = Modifier.padding(end = TasksGrid.tileTrailingPad)) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.tasks_more_actions),
                    )
                }
            }
        }
        if (subLine != null) {
            Spacer(modifier = Modifier.height(TasksGrid.gutter / 2))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = TasksGrid.rowLeadingWidth + TasksGrid.gutter),
            ) {
                Text(
                    text = subLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (executionState) {
            ExecutionControlState.Active -> {
                TextButton(
                    onClick = onPause,
                    enabled = !executionControlInFlight,
                    contentPadding = PaddingValues(
                        start = TasksGrid.rowLeadingWidth + TasksGrid.gutter,
                        end = 0.dp, top = 0.dp, bottom = 0.dp,
                    ),
                    modifier = Modifier.testTag("execute-pause-${tile.id}"),
                ) { Text(stringResource(R.string.tasks_active_hero_pause)) }
                TextButton(
                    onClick = { onComplete() },
                    enabled = !executionControlInFlight,
                    contentPadding = PaddingValues(
                        start = 0.dp,
                        end = 0.dp, top = 0.dp, bottom = 0.dp,
                    ),
                    modifier = Modifier.testTag("execute-complete-${tile.id}"),
                ) { Text(stringResource(R.string.tasks_active_hero_complete)) }
            }
            ExecutionControlState.Paused -> TextButton(
                onClick = onResume,
                enabled = !executionControlInFlight,
                contentPadding = PaddingValues(
                    start = TasksGrid.rowLeadingWidth + TasksGrid.gutter,
                    end = 0.dp, top = 0.dp, bottom = 0.dp,
                ),
                modifier = Modifier.testTag("execute-resume-${tile.id}"),
            ) { Text(stringResource(R.string.tasks_active_hero_resume)) }
            else -> {}
        }
    }
}

@Composable
private fun CompletedTileRow(
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TasksGrid.columnInset)
            .clickable(onClick = onTap)
            .testTag("tasks-done-row-${tile.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TasksGrid.columnInset,
                    vertical = TasksGrid.gutter),
        ) {
            Text(
                text = tile.title,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun DoneCard(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TasksGrid.columnInset)
            .testTag("tasks-done-card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TasksGrid.columnInset,
                    vertical = TasksGrid.rowGap * 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tasks_done_header, count),
                style = completedTextStyle(),
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp
                else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
            )
        }
    }
}
