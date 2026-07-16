package app.tastile.android.ui.mobile.panels.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.ui.graphics.vector.ImageVector
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenu
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedButton
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.data.api.Workspace

/** Web ProjectsMain edit fields, including the Android-visible parent link. */
@Composable
fun ProjectEditForm(
    workspace: Workspace,
    workspaces: List<Workspace>,
    busy: Boolean,
    errorText: String?,
    onSave: (String, String?, String?, String?) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(workspace.id) { mutableStateOf(workspace.displayName) }
    var slug by remember(workspace.id) { mutableStateOf(workspace.slug.orEmpty()) }
    var color by remember(workspace.id) { mutableStateOf(workspace.color.orEmpty()) }
    var parentId by remember(workspace.id) { mutableStateOf(workspace.parentSubjectId) }
    var menuOpen by remember(workspace.id) { mutableStateOf(false) }
    val blockedIds = remember(workspace.id, workspaces) { descendantIds(workspace.id, workspaces) + workspace.id }
    val candidates = remember(workspace.id, workspaces) {
        orderWorkspaceTree(workspaces.filterNot { it.id in blockedIds })
    }
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        OutlinedTextField(name, { name = it.take(80) }, label = { Text("Name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("project-edit-name"))
        OutlinedTextField(slug, { slug = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(40) }, label = { Text("Slug") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("project-edit-slug"))
        OutlinedTextField(color, { color = it }, label = { Text("Color") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("project-edit-color"))
        Box(modifier = Modifier.fillMaxWidth()) {
            AppPickerButton(
                label = "Parent",
                value = workspaces.firstOrNull { it.id == parentId }?.displayName ?: "Top level",
                onClick = { menuOpen = true },
                leadingIcon = Icons.Outlined.AccountTree,
                modifier = Modifier.testTag("project-edit-parent"),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Top level") },
                    leadingIcon = { Icon(Icons.Outlined.AccountTree, contentDescription = null) },
                    onClick = { parentId = null; menuOpen = false },
                )
                candidates.forEach { entry -> DropdownMenuItem(
                    text = { Text("  ".repeat(entry.depth) + entry.workspace.displayName) },
                    leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                    onClick = { parentId = entry.workspace.id; menuOpen = false },
                ) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NiaButton(
                text = { Text(if (busy) "Saving…" else "Save") },
                onClick = { onSave(name, slug.ifBlank { null }, color.ifBlank { null }, parentId) },
                enabled = !busy && name.isNotBlank(),
                leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
                modifier = Modifier.testTag("project-edit-save"),
            )
            NiaTextButton(
                text = { Text("Cancel") },
                onClick = onCancel,
                enabled = !busy,
            )
        }
        if (!errorText.isNullOrBlank()) Text(errorText, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun AppPickerButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Box(Modifier.size(8.dp))
                }
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
    }
}

internal fun descendantIds(rootId: String, workspaces: List<Workspace>): Set<String> {
    val byParent = workspaces.groupBy { it.parentSubjectId }
    val result = mutableSetOf<String>()
    fun visit(id: String) { byParent[id].orEmpty().forEach { if (result.add(it.id)) visit(it.id) } }
    visit(rootId)
    return result
}