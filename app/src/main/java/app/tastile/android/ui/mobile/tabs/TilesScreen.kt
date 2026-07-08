package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.model.Tile
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TilesTab
import app.tastile.android.ui.designsystem.AppEmptyState
import app.tastile.android.ui.designsystem.AppPageWithOverlay
import app.tastile.android.ui.designsystem.AppScreenTitle
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.tabs.tiles.DeleteTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.TilesChangesBody
import app.tastile.android.ui.mobile.tabs.tiles.TilesFilterBar
import app.tastile.android.ui.mobile.tabs.tiles.TilesSectionColumn
import app.tastile.android.ui.mobile.tabs.tiles.TilesTabSwitcher
import app.tastile.android.ui.mobile.tabs.tiles.TilesTimelineBody

/**
 * Mobile Tiles tab. Mirrors web's `/dashboard/tiles` composition:
 *   1. Title row + tab pill (LIST / TIMELINE / CHANGES)
 *   2. Tab-specific body:
 *      - LIST: filter bar + grouped sections
 *      - TIMELINE: full timeline body (scale + rows)
 *      - CHANGES: full changes body (cap 120 rows)
 *   3. `+ New` FAB pointing at the QuickCreate overlay
 *   4. Delete dialog mounted at the bottom
 */
@Composable
fun TilesScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
) {
    val activeTab by viewModel.activeTilesTab.collectAsStateWithLifecycle()
    val deleteCandidate by viewModel.requestDeleteTileId.collectAsStateWithLifecycle()
    val grouped by viewModel.groupedTiles.collectAsStateWithLifecycle()
    val expanded by viewModel.expandedSections.collectAsStateWithLifecycle()
    val sectionLimits by viewModel.sectionLimits.collectAsStateWithLifecycle()
    val listViewMode by viewModel.listViewMode.collectAsStateWithLifecycle()
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val locale by viewModel.locale.collectAsStateWithLifecycle()

    AppPageWithOverlay(
        overlay = {
            ExtendedFloatingActionButton(
                onClick = { overlay.show(Overlay.QuickCreate) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppTheme.spacing.md)
                    .testTag("tiles-fab-new"),
                text = { Text("New") },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            )
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tiles-header-row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            AppScreenTitle(
                text = stringResource(R.string.dashboard_tiles_title),
                modifier = Modifier.weight(1f),
            )
            TilesTabSwitcher(
                active = activeTab,
                onSelect = { viewModel.setActiveTilesTab(it) },
            )
        }

        when (activeTab) {
            TilesTab.LIST -> ListBody(
                vm = viewModel,
                grouped = grouped,
                expanded = expanded,
                sectionLimits = sectionLimits,
                viewMode = listViewMode,
                tilesCount = tiles.size,
                onTileClick = { tile ->
                    viewModel.selectTile(tile.id)
                    overlay.show(Overlay.TileEdit(tile.id))
                },
                onTileDelete = { tile ->
                    viewModel.setDeleteTileCandidate(tile.id)
                },
                onSectionBump = { section ->
                    viewModel.bumpSectionLimit(section.groupId, section.tiles.size)
                    viewModel.toggleSectionExpanded(section.groupId)
                },
            )
            TilesTab.TIMELINE -> TilesTimelineBody(vm = viewModel, locale = locale)
            TilesTab.CHANGES -> TilesChangesBody(vm = viewModel, locale = locale)
        }
    }

    if (deleteCandidate != null) {
        val title = tiles.firstOrNull { it.id == deleteCandidate }?.title
        DeleteTileDialog(
            tileTitle = title,
            onConfirm = { viewModel.confirmDeleteTile() },
            onCancel = { viewModel.setDeleteTileCandidate(null) },
        )
    }
}

@Composable
private fun ListBody(
    vm: DashboardViewModel,
    grouped: List<app.tastile.android.ui.dashboard.TileSection>,
    expanded: Set<String>,
    sectionLimits: Map<String, Int>,
    viewMode: app.tastile.android.ui.dashboard.ListViewMode,
    tilesCount: Int,
    onTileClick: (Tile) -> Unit,
    onTileDelete: (Tile) -> Unit,
    onSectionBump: (app.tastile.android.ui.dashboard.TileSection) -> Unit,
) {
    val search by vm.searchTerm.collectAsStateWithLifecycle()
    val range by vm.filterRange.collectAsStateWithLifecycle()
    val granularity by vm.filterGranularity.collectAsStateWithLifecycle()
    val limit by vm.filterLimit.collectAsStateWithLifecycle()
    val grouping by vm.listGroupingMode.collectAsStateWithLifecycle()
    val listView by vm.listViewMode.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tiles-list-body"),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Text(
            text = "Open: $tilesCount · Estimated: ${tilesCount * 30}m · Sections: ${grouped.size}",
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        TilesFilterBar(
            search = search,
            onSearchChange = { vm.setSearchTerm(it) },
            range = range,
            onRangeChange = { vm.setFilterRange(it) },
            granularity = granularity,
            onGranularityChange = { vm.setFilterGranularity(it) },
            limit = limit,
            onLimitChange = { vm.setFilterLimit(it) },
            grouping = grouping,
            onGroupingChange = { vm.setListGroupingMode(it) },
            viewMode = listView,
            onViewModeChange = { vm.setListViewMode(it) },
        )
        if (grouped.isEmpty()) {
            AppEmptyState(message = stringResource(R.string.dashboard_tiles_empty_main))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                grouped.forEach { section ->
                    TilesSectionColumn(
                        groupId = section.groupId,
                        label = section.labelKey,
                        tiles = section.tiles,
                        viewMode = viewMode,
                        limit = sectionLimits[section.groupId] ?: INITIAL_SECTION_LIMIT,
                        expanded = expanded.contains(section.groupId),
                        onToggleExpanded = { onSectionBump(section) },
                        onTileClick = onTileClick,
                        onTileDelete = onTileDelete,
                    )
                }
            }
        }
    }
}

private const val INITIAL_SECTION_LIMIT = 8