package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Inbox
// m2-allow: primitive
import androidx.compose.material3.CardDefaults
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.MobileSpacing
import app.tastile.android.core.designsystem.component.NiaCard
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.core.designsystem.component.NiaListItemDefaults
import app.tastile.android.core.designsystem.component.NiaOutlinedButton
import app.tastile.android.data.api.Workspace
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.components.AppEmptyState
import app.tastile.android.ui.mobile.panels.ProjectsSectionContent
import app.tastile.android.ui.mobile.panels.ProjectsViewModel
import app.tastile.android.ui.mobile.tabs.tiles.glyphChar

private val MobSpacingXs get() = MobileSpacing.xs
private val MobSpacingSm get() = MobileSpacing.sm
private val MobSpacingMd get() = MobileSpacing.md
private const val COLOR_DOT_SIZE_DP = 10

/**
 * Mobile Projects tab.
 *
 * Layout (top → bottom):
 *
 *   1. Summary row — total project count + tile count summary.
 *   2. Adaptive grid of project cards. Each card shows the workspace
 *      color chip, name, slug, tile count, last-activity timestamp,
 *      and a primary "Open" action that calls
 *      `ProjectsViewModel.selectOwner` so the tile list below scopes
 *      to that project (mirroring web's `?owner=` URL filter).
 *   3. The legacy project editor / tree from `ProjectsSectionContent`,
 *      retained for inline create / edit / delete — selecting a
 *      card above and selecting a row in the editor both drive the
 *      same `selectedOwnerId` flow.
 *   4. A filtered tile list scoped to `DashboardViewModel.tileFilter`,
 *      shown beneath a sticky-feeling section header.
 *
 * Tap targets are consistent (NiaCard / NiaListItem) and the spacing
 * comes from [MobileSpacing] tokens to match the rest of the mobile
 * UI. Empty state renders when both the project list and the tile
 * list are empty.
 */
@Composable
fun ProjectsScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val tileCountByOwner by viewModel.tileCountByOwnerId.collectAsStateWithLifecycle()
    val workspacesState by projectsViewModel.state.collectAsStateWithLifecycle()
    val selectedOwnerId by projectsViewModel.selectedOwnerId.collectAsStateWithLifecycle()

    val gridState = rememberLazyGridState()
    Box(modifier = Modifier.fillMaxSize().testTag("projects-screen-body")) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("projects-grid"),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(MobSpacingSm),
            verticalArrangement = Arrangement.spacedBy(MobSpacingXs),
            contentPadding = PaddingValues(
                horizontal = MobSpacingMd,
                vertical = MobSpacingSm,
            ),
        ) {
            item(
                key = "projects-summary",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "summary",
            ) {
                ProjectsSummary(
                    projectCount = workspacesState.workspaces.size,
                    tileCount = tiles.size,
                )
            }

            if (workspacesState.workspaces.isEmpty() && !workspacesState.loading) {
                item(
                    key = "projects-empty",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "empty",
                ) {
                    AppEmptyState(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.FolderOpen,
                        title = stringResource(R.string.empty_projects_title),
                        hint = stringResource(R.string.empty_projects_hint),
                    )
                }
            } else {
                gridItems(
                    items = workspacesState.workspaces,
                    key = { ws -> "projects-card-${ws.id}" },
                    contentType = { "project-card" },
                ) { workspace ->
                    ProjectCard(
                        workspace = workspace,
                        tileCount = tileCountByOwner[workspace.id] ?: 0,
                        selected = workspace.id == selectedOwnerId,
                        onOpen = { projectsViewModel.selectOwner(workspace.id) },
                    )
                }
            }

            item(
                key = "projects-editor",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "editor",
            ) {
                ProjectsSectionContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MobSpacingSm),
                    dashboardViewModel = viewModel,
                    projectsViewModel = projectsViewModel,
                )
            }

            item(
                key = "projects-tiles-divider",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "divider",
            ) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MobSpacingXs),
                )
            }

            item(
                key = "projects-tiles-header",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "tiles-header",
            ) {
                FilteredTileListHeader(
                    tilesCount = tiles.size,
                    selectedOwnerId = selectedOwnerId,
                )
            }

            if (tiles.isEmpty()) {
                item(
                    key = "projects-tiles-empty",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "tiles-empty",
                ) {
                    AppEmptyState(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.Inbox,
                        title = stringResource(R.string.empty_tiles_title),
                        hint = stringResource(R.string.empty_tiles_hint),
                    )
                }
            } else {
                gridItems(
                    items = tiles,
                    key = { tile -> "projects-tile-${tile.id}" },
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = { "project-tile-row" },
                ) { tile ->
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
private fun ProjectsSummary(
    projectCount: Int,
    tileCount: Int,
) {
    val projectsLabel = when (projectCount) {
        0 -> stringResource(R.string.projects_summary_count_zero)
        1 -> stringResource(R.string.projects_summary_count_one)
        else -> stringResource(R.string.projects_summary_count, projectCount)
    }
    val tilesLabel = when (tileCount) {
        0 -> stringResource(R.string.projects_tile_count_zero)
        1 -> stringResource(R.string.projects_tile_count_one)
        else -> stringResource(R.string.projects_tile_count, tileCount)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MobSpacingMd, vertical = MobSpacingSm),
        horizontalArrangement = Arrangement.spacedBy(MobSpacingMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = projectsLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = tilesLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProjectCard(
    workspace: Workspace,
    tileCount: Int,
    selected: Boolean,
    onOpen: () -> Unit,
) {
    val chipColor = parseHexColor(workspace.color) ?: MaterialTheme.colorScheme.outline
    val tileCountLabel = when (tileCount) {
        0 -> stringResource(R.string.projects_tile_count_zero)
        1 -> stringResource(R.string.projects_tile_count_one)
        else -> stringResource(R.string.projects_tile_count, tileCount)
    }
    val activityLabel = workspace.updatedAt?.let { updated ->
        stringResource(R.string.projects_last_activity_updated, updated.take(10))
    } ?: stringResource(R.string.projects_last_activity_never)
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    NiaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("projects-card-${workspace.id}"),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        onClick = onOpen,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MobSpacingMd),
            verticalArrangement = Arrangement.spacedBy(MobSpacingXs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MobSpacingXs),
            ) {
                Box(
                    modifier = Modifier
                        .size(COLOR_DOT_SIZE_DP.dp)
                        .background(chipColor, CircleShape),
                )
                Text(
                    text = workspace.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            workspace.slug?.takeIf { it.isNotBlank() }?.let { slug ->
                Text(
                    text = slug,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = tileCountLabel,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
            )
            Text(
                text = activityLabel,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            NiaOutlinedButton(
                onClick = onOpen,
                text = { Text(stringResource(R.string.projects_open_button)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MobSpacingXs)
                    .testTag("projects-card-open-${workspace.id}"),
            )
        }
    }
}

@Composable
private fun FilteredTileListHeader(
    tilesCount: Int,
    selectedOwnerId: String?,
) {
    val title = if (selectedOwnerId == null) {
        stringResource(R.string.panels_projects_all_projects)
    } else {
        stringResource(R.string.panels_projects_filtered_tile_count, tilesCount)
    }
    val hint = if (selectedOwnerId == null) {
        stringResource(R.string.projects_select_hint)
    } else {
        stringResource(R.string.projects_section_scoped)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MobSpacingMd, vertical = MobSpacingXs),
        verticalArrangement = Arrangement.spacedBy(MobSpacingXs),
    ) {
        Text(
            text = stringResource(R.string.panels_projects_tiles_section_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .padding(horizontal = MobSpacingXs)
            .clickable(onClick = onClick)
            .testTag("projects-tile-row-${tile.id}"),
        colors = NiaListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/**
 * Parses a 6-digit hex color (e.g. `#RRGGBB`) into an [Int]-backed
 * Compose [Color], returning null on any parse failure so the caller
 * can fall back to the design-system outline color.
 */
private fun parseHexColor(value: String?): Color? {
    val hex = value?.takeIf { it.isNotBlank() } ?: return null
    val cleaned = hex.removePrefix("#").trim()
    if (cleaned.length != 6 && cleaned.length != 8) return null
    return runCatching {
        val parsed = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(0xFF000000L or parsed)
        } else {
            Color(parsed)
        }
    }.getOrNull()
}
