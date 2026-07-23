package app.tastile.android.ui.mobile.panels.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: state-holder
import androidx.compose.material3.ListItemDefaults
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.data.api.Workspace
import app.tastile.android.ui.mobile.components.AppEmptyState

/**
 * Renders the "All Projects" + per-workspace rows in a parent-before-child
 * presentation order.
 *
 * Web parity: equivalent to the body of `ProjectsTree` in
 * `tastile-web/src/components/panels/ProjectsSidePanel.tsx`.
 */
@Composable
fun ProjectsList(
    workspaces: List<Workspace>,
    selectedOwnerId: String?,
    onSelect: (String?) -> Unit,
    onEditRequest: (Workspace) -> Unit,
    onDeleteRequest: (Workspace) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        NiaListItem(
            content = {
                Text(
                    stringResource(R.string.panels_projects_all_projects),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null) },
            modifier = Modifier.clickable { onSelect(null) },
            colors = ListItemDefaults.colors(
                containerColor = if (selectedOwnerId == null) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ),
        )
        if (workspaces.isEmpty()) {
            AppEmptyState(
                icon = Icons.Outlined.FolderOff,
                title = stringResource(R.string.empty_projects_title),
                hint = stringResource(R.string.empty_projects_hint),
            )
            return@Column
        }
        val ordered = remember(workspaces) { orderWorkspaceTree(workspaces) }
        ordered.forEach { entry ->
            ProjectRow(
                workspace = entry.workspace,
                depth = entry.depth,
                selected = selectedOwnerId == entry.workspace.id,
                onClick = { onSelect(entry.workspace.id) },
                onEdit = { onEditRequest(entry.workspace) },
                onDelete = { onDeleteRequest(entry.workspace) },
            )
        }
    }
}

internal data class WorkspaceTreeEntry(val workspace: Workspace, val depth: Int)

internal fun orderWorkspaceTree(workspaces: List<Workspace>): List<WorkspaceTreeEntry> {
    val byParent = mutableMapOf<String?, MutableList<Workspace>>()
    val ids = workspaces.map { it.id }.toSet()
    workspaces.forEach { w ->
        val parent = w.parentSubjectId?.takeIf { ids.contains(it) }
        byParent.getOrPut(parent) { mutableListOf() }.add(w)
    }
    byParent.values.forEach { list -> list.sortBy { it.displayName.lowercase() } }
    val result = mutableListOf<WorkspaceTreeEntry>()
    fun visit(parent: String?, depth: Int) {
        byParent[parent].orEmpty().forEach { ws ->
            result += WorkspaceTreeEntry(ws, depth)
            visit(ws.id, depth + 1)
        }
    }
    visit(null, 0)
    return result
}