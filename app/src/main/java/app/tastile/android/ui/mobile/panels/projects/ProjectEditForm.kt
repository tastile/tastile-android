package app.tastile.android.ui.mobile.panels.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.tastile.android.data.api.Workspace
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
import app.tastile.android.ui.mobile.designsystem.MobileSpacing

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
    Column(modifier = Modifier.fillMaxWidth().padding(MobileSpacing.sm)) {
        OutlinedTextField(name, { name = it.take(80) }, label = { Text("Name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("project-edit-name"))
        OutlinedTextField(slug, { slug = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(40) }, label = { Text("Slug") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("project-edit-slug"))
        OutlinedTextField(color, { color = it }, label = { Text("Color") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("project-edit-color"))
        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("project-edit-parent")) {
                Text("Parent project: " + (workspaces.firstOrNull { it.id == parentId }?.displayName ?: "Top level"))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Top level") }, onClick = { parentId = null; menuOpen = false })
                candidates.forEach { entry -> DropdownMenuItem(
                    text = { Text("  ".repeat(entry.depth) + entry.workspace.displayName) },
                    onClick = { parentId = entry.workspace.id; menuOpen = false },
                ) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
        ) {
            AppPrimaryButton(
                text = if (busy) "Saving…" else "Save",
                onClick = { onSave(name, slug.ifBlank { null }, color.ifBlank { null }, parentId) },
                enabled = !busy && name.isNotBlank(),
                modifier = Modifier.testTag("project-edit-save"),
            )
            AppTertiaryButton(
                text = "Cancel",
                onClick = onCancel,
                enabled = !busy,
            )
        }
        if (!errorText.isNullOrBlank()) Text(errorText, color = MaterialTheme.colorScheme.error)
    }
}

internal fun descendantIds(rootId: String, workspaces: List<Workspace>): Set<String> {
    val byParent = workspaces.groupBy { it.parentSubjectId }
    val result = mutableSetOf<String>()
    fun visit(id: String) { byParent[id].orEmpty().forEach { if (result.add(it.id)) visit(it.id) } }
    visit(rootId)
    return result
}