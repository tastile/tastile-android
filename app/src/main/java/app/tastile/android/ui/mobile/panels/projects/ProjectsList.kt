package app.tastile.android.ui.mobile.panels.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tastile.android.data.api.Workspace
import app.tastile.android.ui.designsystem.AppCorner
import app.tastile.android.ui.designsystem.AppSpacing
import app.tastile.android.R

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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        AllProjectsRow(
            selected = selectedOwnerId == null,
            onClick = { onSelect(null) },
        )
        if (workspaces.isEmpty()) return@Column
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

@Composable
private fun AllProjectsRow(selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorner.mediumShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline),
        )
        Box(Modifier.size(AppSpacing.sm))
        Text(
            text = stringResource(R.string.panels_projects_all_projects),
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
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
