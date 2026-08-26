package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.PrimaryScrollableTabRow
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: m3-component
import androidx.compose.material3.Tab
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.MobileSpacing
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaLoadingWheel
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.core.designsystem.theme.LocalTastileCardRoleTokens
import app.tastile.android.core.designsystem.theme.LocalTastileShapeTokens
import app.tastile.android.core.designsystem.theme.LocalTastileStatusTokens
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.model.projectLabels
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecutionControlState
import app.tastile.android.ui.dashboard.ProjectSection
import app.tastile.android.ui.dashboard.SortOrder
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
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
    // ---------------------------------------------------------------
    // Tasks screen geometry — calibrated to Google Calendar Tasks.
    // (Reference render: ~899×2048, 1dp ≈ 2.25px.)
    //
    // Geometry rules:
    //   * Section is a wrap-content surface (no fixed height). The
    //     Section CONTAINER is not the touch target — each row and the
    //     header are independent touch targets.
    //   * Section outer margin = 8dp.
    //   * Section internal padding = 24dp at start/end.
    //   * Header padding top/bottom = 16dp (≈ half a text line on each
    //     side, so the title sits "between two half-lines" of whitespace).
    //   * Row padding top/bottom = 12dp on each side (≈ half a text
    //     line). Title sits centred vertically inside its row.
    //   * No fixed row height. No nested scroll. No divider between rows.
    //   * Title text is `bodyLarge` (16sp Regular), not `titleMedium`
    //     SemiBold. The SemiBold weight over-emphasised the section.
    // ---------------------------------------------------------------

    /** Outer margin keeps the section surface inside the screen edge. */
    val sectionOuterPadding: Dp = 8.dp

    /** Section internal padding — title text starts 24dp from section left. */
    val sectionStartPadding: Dp = 24.dp
    val sectionEndPadding: Dp = 24.dp

    /** Distance between two sections in the LazyColumn. */
    val sectionGap: Dp = 8.dp

    /** Distance between a row's leading slot and its content column. */
    val leadingContentGap: Dp = 16.dp

    /** Header padding — small, fixed. heightIn min caps the height
     *  so the IconButton touch targets still fit comfortably. */
    val headerTopPadding: Dp = 4.dp
    val headerBottomPadding: Dp = 4.dp
    val headerMinHeight: Dp = 48.dp

    /** Row padding — half a text line on each side of the title. */
    val rowTopPadding: Dp = 12.dp
    val rowBottomPadding: Dp = 12.dp

    /** Vertical breathing room between rows. */
    val rowGap: Dp = 4.dp

    /** Section bottom whitespace after the last row. */
    val sectionBottomPadding: Dp = 8.dp

    /** Vertical space between title and an optional metadata line. */
    val titleMetadataGap: Dp = 2.dp

    /** Vertical space between (title+metadata) and an optional related-mail chip. */
    val metadataRelatedGap: Dp = 8.dp

    /** Size of the leading radio/checkbox slot. */
    val leadingSlotSize: Dp = 20.dp

    /** Size of the optional trailing star touch target. The visual
     *  star glyph is 24dp; the touch target is 40dp (M3 minimum). */
    val trailingTouchTarget: Dp = 40.dp

    /** Legacy aliases kept for the rest of the file so we don't have to
     *  refactor every call site in one pass. */
    val pageMargin: Dp = sectionOuterPadding
    val columnInset: Dp = sectionOuterPadding
    val gutter: Dp = leadingContentGap
    val bucketLeadingInset: Dp = sectionStartPadding
    val sectionBarVerticalPad: Dp = 0.dp
    val rowHeight: Dp = 0.dp
    val rowMinHeight: Dp = 0.dp
    val rowLeadingWidth: Dp = leadingSlotSize
    val headerVerticalPad: Dp = 0.dp
    val headerHeight: Dp = headerMinHeight
    val headerGap: Dp = 0.dp
    val listBottom: Dp = 32.dp
    val tabHorizontalPadding: Dp = 16.dp
    val tileTrailingPad: Dp = 0.dp
}

@Composable
/**
 * Task title style. M3 list item uses `bodyLarge` for its primary text.
 * We deliberately do NOT use `titleMedium` (which is SemiBold by default)
 * — the SemiBold weight made the active section feel heavier than the
 * items below it, breaking the visual rhythm.
 */
private fun TasksTitleStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge

/**
 * Task metadata (project · scheduled time) style. Smaller and lighter
 * weight than the title so it reads as a secondary line.
 */
@Composable
private fun TasksMetaStyle(): TextStyle =
    MaterialTheme.typography.bodySmall.copy(
        color = androidx.compose.ui.graphics.Color.Unspecified,
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
            verticalArrangement = Arrangement.spacedBy(TasksGrid.sectionGap),
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
                    color = LocalTastileCardRoleTokens.current.completed.border,
                )
            }

            error?.let { message ->
                item(key = "tasks-error", contentType = "error") {
                    Text(
                        text = message,
                        color = LocalTastileStatusTokens.current.archived.icon,
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
                        color = LocalTastileCardRoleTokens.current.actionable.border,
                        modifier = Modifier
                            .padding(horizontal = TasksGrid.columnInset,
                                vertical = TasksGrid.gutter),
                    )
                }
            }

            // ---------------- Main task section ----------------
            item(
                key = "tasks-main-section-${visibleSection.id}",
                contentType = "main-section",
            ) {
                if (sectionExpanded && displayedTiles.isEmpty()) {
                    SectionSurface(testTag = "tasks-main-section-${visibleSection.id}") {
                        Column {
                            SectionHeader(
                                title = visibleSection.label,
                                contentDescription = visibleSection.label,
                                onClick = { sectionExpanded = !sectionExpanded },
                                sectionId = visibleSection.id,
                                actions = {
                                    HeaderActionIcon(
                                        onClick = { sortMenuOpen = true },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Outlined.SwapVert,
                                                contentDescription = stringResource(
                                                    R.string.tasks_sort_button,
                                                ),
                                            )
                                        },
                                        testTag = "tasks-sort-button",
                                    )
                                    HeaderActionIcon(
                                        onClick = { sectionExpanded = !sectionExpanded },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                                contentDescription = null,
                                            )
                                        },
                                        testTag = "tasks-main-section-collapse",
                                    )
                                },
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tasks-empty")
                                    .padding(vertical = MobSpacingLg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.tasks_all_done),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            SectionBottomPadding()
                        }
                    }
                } else if (sectionExpanded) {
                    TaskListSection(
                        sectionId = visibleSection.id,
                        title = visibleSection.label,
                        tiles = displayedTiles,
                        tileExecutionStates = executionStates,
                        tileExecutionControlsInFlight = executionControlsInFlight,
                        onSortClick = { sortMenuOpen = true },
                        onToggle = { sectionExpanded = !sectionExpanded },
                        onTileTap = { id ->
                            viewModel.selectTile(id)
                            overlay.show(Overlay.TileEdit(id))
                        },
                        onTileStart = viewModel::startTile,
                        onTileComplete = viewModel::completeTile,
                        onTileStartExecution = { id -> executionActionCandidate = Pair(id, true) },
                        onTileFinishExecution = { id -> executionActionCandidate = Pair(id, false) },
                        onTilePause = viewModel::pauseTile,
                        onTileResume = viewModel::resumeTile,
                        onTileDelete = viewModel::setDeleteTileCandidate,
                        onTileDefer = viewModel::setDeferTileCandidate,
                        onTilePrompt = viewModel::setPromptTileCandidate,
                    )
                } else {
                    SectionSurface(testTag = "tasks-main-section-${visibleSection.id}") {
                        Column {
                            SectionHeader(
                                title = visibleSection.label,
                                contentDescription = visibleSection.label,
                                onClick = { sectionExpanded = !sectionExpanded },
                                sectionId = visibleSection.id,
                                actions = {
                                    HeaderActionIcon(
                                        onClick = { sortMenuOpen = true },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Outlined.SwapVert,
                                                contentDescription = stringResource(
                                                    R.string.tasks_sort_button,
                                                ),
                                            )
                                        },
                                        testTag = "tasks-sort-button",
                                    )
                                    HeaderActionIcon(
                                        onClick = { sectionExpanded = !sectionExpanded },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                                contentDescription = null,
                                            )
                                        },
                                        testTag = "tasks-main-section-expand",
                                    )
                                },
                            )
                            SectionBottomPadding()
                        }
                    }
                }
            }

            // ---------------- Completed section ----------------
            item(
                key = "tasks-completed-section",
                contentType = "completed-section",
            ) {
                CompletedSection(
                    completedTiles = completedTiles,
                    expanded = doneExpanded,
                    onToggle = { doneExpanded = !doneExpanded },
                    onTileTap = { id ->
                        viewModel.selectTile(id)
                        overlay.show(Overlay.TileEdit(id))
                    },
                    onTileStart = viewModel::startTile,
                    onTileComplete = viewModel::completeTile,
                    tileExecutionStates = executionStates,
                    tileExecutionControlsInFlight = executionControlsInFlight,
                    onTileStartExecution = { id -> executionActionCandidate = Pair(id, true) },
                    onTileFinishExecution = { id -> executionActionCandidate = Pair(id, false) },
                    onTilePause = viewModel::pauseTile,
                    onTileResume = viewModel::resumeTile,
                    onTileDelete = viewModel::setDeleteTileCandidate,
                    onTileDefer = viewModel::setDeferTileCandidate,
                    onTilePrompt = viewModel::setPromptTileCandidate,
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

// ============================================================================
// Tasks geometry primitives
// ============================================================================

/**
 * Section surface — fillMaxWidth with 8dp outer margin on each side, no
 * nested scroll. The Column inside grows naturally with its children.
 */
@Composable
private fun SectionSurface(
    testTag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerColor = LocalTastileCardRoleTokens.current.actionable.container
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TasksGrid.sectionOuterPadding)
            .testTag(testTag),
        color = containerColor,
        contentColor = LocalContentColor.current,
        // M3 ListItemContainer uses 12dp corner radius. 16dp made the
        // section look like a floating card; 12dp reads as a list
        // container.
        shape = RoundedCornerShape(LocalTastileShapeTokens.current.m),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/**
 * Section header used by both the main task section and the completed
 * section. Geometry is identical regardless of which section hosts it —
 * the title placement, top/bottom padding, and minimum height are all
 * managed here. The optional [actions] slot lives to the right, packed
 * tightly together (NO SpaceBetween — actions are anchored at the end).
 */
@Composable
private fun SectionHeader(
    title: String,
    contentDescription: String?,
    onClick: (() -> Unit)?,
    sectionId: String? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = TasksGrid.headerMinHeight)
        .let {
            if (onClick != null) it.clickable(onClick = onClick) else it
        }
        .let {
            if (sectionId != null) it.testTag("tasks-section-bar") else it
        }
        .padding(
            start = TasksGrid.sectionStartPadding,
            end = TasksGrid.sectionEndPadding,
            top = TasksGrid.headerTopPadding,
            bottom = TasksGrid.headerBottomPadding,
        )
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = TasksTitleStyle(),
            modifier = Modifier
                .weight(1f)
                .let { if (contentDescription != null) it.semantics { this.contentDescription = contentDescription } else it }
                .let {
                    if (sectionId != null) it.testTag("tasks-bucket-label-$sectionId")
                    else it.testTag("tasks-section-title")
                },
        )
        if (actions != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
    }
}

/**
 * Header action button. 48dp rounded IconButton (touch target) used for
 * sort, more, and expand/collapse. Adjacent HeaderActionIcons sit
 * shoulder-to-shoulder; the parent Row handles end alignment.
 */
@Composable
private fun HeaderActionIcon(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    testTag: String,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag(testTag),
    ) {
        icon()
    }
}

/**
 * Bottom breathing room inside a section after the last row.
 */
@Composable
private fun SectionBottomPadding() {
    Spacer(modifier = Modifier.height(TasksGrid.sectionBottomPadding))
}

// ============================================================================
// Main task section
// ============================================================================

/**
 * Main task section. Header has two actions (sort + collapse). Each row
 * has a leading checkbox slot, a content column (title + optional
 * metadata + optional related-mail chip), and an optional trailing
 * star. Rows are content-driven; no fixed height.
 */
@Composable
private fun TaskListSection(
    sectionId: String,
    title: String,
    tiles: List<Tile>,
    tileExecutionStates: Map<String, ExecutionControlState>,
    tileExecutionControlsInFlight: Set<String>,
    onSortClick: () -> Unit,
    onToggle: () -> Unit,
    onTileTap: (String) -> Unit,
    onTileStart: (String) -> Unit,
    onTileComplete: (String) -> Unit,
    onTileStartExecution: (String) -> Unit,
    onTileFinishExecution: (String) -> Unit,
    onTilePause: (String) -> Unit,
    onTileResume: (String) -> Unit,
    onTileDelete: (String) -> Unit,
    onTileDefer: (String) -> Unit,
    onTilePrompt: (String) -> Unit,
) {
    SectionSurface(testTag = "tasks-main-section-${sectionId}") {
        Column {
            SectionHeader(
                title = title,
                contentDescription = title,
                onClick = onToggle,
                sectionId = sectionId,
                actions = {
                    HeaderActionIcon(
                        onClick = onSortClick,
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.SwapVert,
                                contentDescription = stringResource(R.string.tasks_sort_button),
                            )
                        },
                        testTag = "tasks-sort-button",
                    )
                    HeaderActionIcon(
                        onClick = onToggle,
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                contentDescription = null,
                            )
                        },
                        testTag = "tasks-main-section-collapse",
                    )
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(TasksGrid.rowGap)) {
                tiles.forEach { tile ->
                    TaskRow(
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
            SectionBottomPadding()
        }
    }
}

// ============================================================================
// Completed section
// ============================================================================

/**
 * Completed section. Header has only one action (expand/collapse). No
 * sort, no overflow menu. Each row has a leading checkmark slot, a
 * content column (title + optional metadata + optional related-mail),
 * and NO trailing slot — Content claims the full width.
 */
@Composable
private fun CompletedSection(
    completedTiles: List<Tile>,
    expanded: Boolean,
    onToggle: () -> Unit,
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
    SectionSurface(testTag = "tasks-done-card") {
        Column {
            val headerTitle = stringResource(R.string.tasks_done_header, completedTiles.size)
            SectionHeader(
                title = headerTitle,
                contentDescription = headerTitle,
                onClick = onToggle,
                actions = {
                    HeaderActionIcon(
                        onClick = onToggle,
                        icon = {
                            Icon(
                                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp
                                else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                            )
                        },
                        testTag = "tasks-completed-section-toggle",
                    )
                },
            )
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(TasksGrid.rowGap)) {
                    completedTiles.forEach { tile ->
                        CompletedTaskRow(
                            tile = tile,
                            onTap = { onTileTap(tile.id) },
                        )
                    }
                }
                SectionBottomPadding()
            } else {
                SectionBottomPadding()
            }
        }
    }
}

// ============================================================================
// TaskRow — 3-slot structure (leading | content | optional trailing)
// ============================================================================

/**
 * TaskRow. 3-slot layout:
 *   leading  : checkbox slot (top-aligned to title line 1)
 *   content  : title + optional metadata + optional related-mail chip
 *   trailing : optional star (Main section only). Compressed entirely
 *              when null — content claims the full width.
 *
 * Row height is content-driven. No fillMaxHeight, no fixed height.
 * leading and trailing align to the title's first line via a small
 * optical offset to compensate for the title's top text padding.
 */
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
        TileLifecycle.ARCHIVED -> stringResource(R.string.tasks_status_archived)
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

    val projectName = tile.projectLabels().firstOrNull()
    val metadata: String? = when {
        projectName != null && formattedScheduled != null ->
            "$projectName  ·  $formattedScheduled"
        projectName != null -> projectName
        formattedScheduled != null -> formattedScheduled
        else -> null
    }

    // Trailing slot: Main task shows a star icon (matches Google Tasks —
    // active tasks have a star on the right, not a 3-dot menu). The
    // trailing IconButton is wrapped in a fixed-size 40dp Box so the
    // trailing area only takes its measured width and does NOT stretch
    // the Row height. The icon is then anchored to the top of the box
    // so it lines up with the title's first line.
    val trailingSlot: (@Composable () -> Unit)? = {
        Box(
            modifier = Modifier.size(TasksGrid.trailingTouchTarget),
            contentAlignment = Alignment.TopCenter,
        ) {
            IconButton(
                onClick = { },
                modifier = Modifier
                    .size(TasksGrid.trailingTouchTarget)
                    .testTag("tasks-row-star-${tile.id}"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.StarOutline,
                    contentDescription = stringResource(R.string.tasks_more_actions),
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .semantics { contentDescription = rowDescription }
            .padding(
                start = TasksGrid.sectionStartPadding,
                end = TasksGrid.sectionEndPadding,
                top = TasksGrid.rowTopPadding,
                bottom = TasksGrid.rowBottomPadding,
            )
            .testTag("execute-tile-${tile.id}"),
        verticalAlignment = Alignment.Top,
    ) {
        // Leading slot — top-aligned to title line 1. Box is 20dp so
        // the row never stretches to match the trailing 40dp touch
        // target.
        Box(
            modifier = Modifier
                .size(TasksGrid.leadingSlotSize)
                .testTag("tasks-row-leading-${tile.id}"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
            )
        }
        Spacer(modifier = Modifier.width(TasksGrid.leadingContentGap))

        // Content column — title, optional metadata, optional related mail.
        // This is the only column that grows. The trailing slot, if
        // present, lives outside this column and only takes its own
        // measured width.
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = tile.title,
                style = TasksTitleStyle(),
                modifier = Modifier
                    .testTag("tasks-row-title-${tile.id}")
                    .fillMaxWidth(),
            )
            if (metadata != null) {
                Spacer(modifier = Modifier.height(TasksGrid.titleMetadataGap))
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasks-row-meta-${tile.id}"),
                )
            }
            when (executionState) {
                ExecutionControlState.Active -> {
                    if (metadata == null) {
                        Spacer(modifier = Modifier.height(TasksGrid.titleMetadataGap))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NiaTextButton(
                            onClick = onPause,
                            enabled = !executionControlInFlight,
                            text = { Text(stringResource(R.string.tasks_active_hero_pause)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                            },
                            modifier = Modifier
                                .testTag("execute-pause-${tile.id}"),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        NiaTextButton(
                            onClick = { onComplete() },
                            enabled = !executionControlInFlight,
                            text = { Text(stringResource(R.string.tasks_active_hero_complete)) },
                            modifier = Modifier
                                .testTag("tasks-row-complete-${tile.id}"),
                        )
                    }
                }
                ExecutionControlState.Paused -> {
                    if (metadata == null) {
                        Spacer(modifier = Modifier.height(TasksGrid.titleMetadataGap))
                    }
                    NiaTextButton(
                        onClick = onResume,
                        enabled = !executionControlInFlight,
                        text = { Text(stringResource(R.string.tasks_active_hero_resume)) },
                        modifier = Modifier.testTag("execute-resume-${tile.id}"),
                    )
                }
                else -> {}
            }
        }

        // Trailing slot — only present when caller supplies it.
        // When null, the content column claims the full width.
        if (trailingSlot != null) {
            Spacer(modifier = Modifier.width(TasksGrid.leadingContentGap))
            trailingSlot()
        }
    }
}

// ============================================================================
// CompletedTaskRow — leading + content. No trailing slot.
// ============================================================================

/**
 * Completed task row. Same 3-slot structure as TaskRow but with no
 * trailing slot — the content column claims the full width. The
 * leading slot shows a check glyph instead of a circle.
 */
@Composable
private fun CompletedTaskRow(
    tile: Tile,
    onTap: () -> Unit,
) {
    val rawScheduled = tile.projectedNextStartAt ?: tile.releaseAt ?: tile.fixedStart
    val formattedScheduled = formatScheduledLabel(
        iso = rawScheduled,
        tomorrowLabel = stringResource(R.string.tasks_schedule_tomorrow),
    )
    val projectName = tile.projectLabels().firstOrNull()
    val metadata: String? = when {
        projectName != null && formattedScheduled != null ->
            "$projectName  ·  $formattedScheduled"
        projectName != null -> projectName
        formattedScheduled != null -> formattedScheduled
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(
                start = TasksGrid.sectionStartPadding,
                end = TasksGrid.sectionEndPadding,
                top = TasksGrid.rowTopPadding,
                bottom = TasksGrid.rowBottomPadding,
            )
            .testTag("tasks-done-row-${tile.id}"),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(TasksGrid.leadingSlotSize)
                .testTag("tasks-completed-row-leading-${tile.id}"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
            )
        }
        Spacer(modifier = Modifier.width(TasksGrid.leadingContentGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tile.title,
                style = TasksTitleStyle(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tasks-completed-row-title-${tile.id}"),
            )
            if (metadata != null) {
                Spacer(modifier = Modifier.height(TasksGrid.titleMetadataGap))
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasks-completed-row-meta-${tile.id}"),
                )
            }
        }
    }
}

// ============================================================================
// Project tabs row — unchanged from the previous version.
// ============================================================================

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
        containerColor = LocalTastileCardRoleTokens.current.neutral.container,
        contentColor = LocalContentColor.current,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tasks-scope-tabs-row"),
    ) {
        Tab(
            selected = selectedId == "starred",
            onClick = { onSelect("starred") },
            selectedContentColor = LocalTastileCardRoleTokens.current.actionable.border,
            unselectedContentColor = LocalContentColor.current,
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
        sections.forEach { section ->
            Tab(
                selected = section.id == selectedId,
                onClick = { onSelect(section.id) },
                selectedContentColor = LocalTastileCardRoleTokens.current.actionable.border,
                unselectedContentColor = LocalContentColor.current,
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
            selectedContentColor = LocalTastileCardRoleTokens.current.actionable.border,
            unselectedContentColor = LocalContentColor.current,
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

// ============================================================================
// Helpers
// ============================================================================

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
