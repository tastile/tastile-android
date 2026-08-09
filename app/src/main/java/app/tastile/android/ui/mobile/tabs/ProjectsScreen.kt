package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.panels.ProjectsSectionContent
import app.tastile.android.ui.mobile.panels.ProjectsViewModel
import app.tastile.android.ui.mobile.components.AppEmptyState
import app.tastile.android.ui.mobile.tabs.tiles.glyphChar
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.core.designsystem.component.NiaListItemDefaults

private val PROJECTS_SPACING_SM = 8.dp

/**
 * Mobile Projects tab.
 *
 * Replaces the legacy drawer entry that mounted `TilesScreen` and labelled
 * itself "Projects". Composes two halves that mirror web's
 * `tastile-web/src/features/manage-projects/ui/ProjectsMain.tsx`:
 *
 *   1. `ProjectsSectionContent` — the project editor / tree (header +
 *      "+ New" inline form + "All Projects" + per-workspace rows with
 *      parent-before-child ordering). Long-press reveals edit / delete.
 *      Reuses the previously orphaned panel that lived behind the removed
 *      `SidePanelSheet`.
 *
 *   2. A filtered tile list scoped to the currently selected owner via
 *      `DashboardViewModel.tileFilter.ownerIds`. Selecting a project row
 *      calls `dashboardViewModel.setOwnerFilter(id)` (the wiring already
 *      present in `ProjectsSectionContent.kt:138-140`), so the tile list
 *      below this section reflects the same scope as web's `?owner=`.
 *
 * Tapping a tile selects it on the dashboard and opens the existing
 * `TileEditSheet` overlay (the same handler used by the Tiles and
 * Execute screens), so the edit panel contract is preserved.
 */
@Composable
fun ProjectsScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val selectedOwnerId by projectsViewModel.selectedOwnerId.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("projects-screen-body"),
        verticalArrangement = Arrangement.spacedBy(PROJECTS_SPACING_SM),
    ) {
        // Project editor: inline create form + workspace tree + edit/delete
        // dialogs. Selecting a row already routes through
        // `DashboardViewModel.setOwnerFilter` via the panel.
        ProjectsSectionContent(
            modifier = Modifier.fillMaxWidth(),
            dashboardViewModel = viewModel,
            projectsViewModel = projectsViewModel,
        )

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )

        FilteredTileListHeader(
            tilesCount = tiles.size,
            selectedOwnerId = selectedOwnerId,
        )

        if (tiles.isEmpty()) {
            AppEmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PROJECTS_SPACING_SM),
                icon = Icons.Outlined.Inbox,
                title = stringResource(R.string.empty_tiles_title),
                hint = stringResource(R.string.empty_tiles_hint),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                tiles.forEach { tile ->
                    ProjectTileRow(
                        tile = tile,
                        onClick = {
                            viewModel.selectTile(tile.id)
                            overlay.show(Overlay.TileEdit(tile.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilteredTileListHeader(
    tilesCount: Int,
    selectedOwnerId: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.panels_projects_tiles_section_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (selectedOwnerId == null) {
                stringResource(R.string.panels_projects_all_projects)
            } else {
                stringResource(R.string.panels_projects_filtered_tile_count, tilesCount)
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ProjectTileRow(
    tile: Tile,
    onClick: () -> Unit,
) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    NiaListItem(
        content = {
            Text(
                text = tile.title,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        leadingContent = {
            Text(lifecycle.glyphChar(), style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
            Text(
                text = "›",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        supportingContent = {
            Text(
                text = lifecycle.name.lowercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clickable(onClick = onClick)
            .testTag("projects-tile-row-${tile.id}"),
        colors = NiaListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
