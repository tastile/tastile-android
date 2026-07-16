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
import androidx.compose.ui.res.stringResource
import app.tastile.android.data.api.Workspace
import app.tastile.android.R
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
import app.tastile.android.ui.mobile.designsystem.MobileSpacing

private val SLUG_REGEX = Regex("[a-z0-9-]+")

/**
 * Inline "+ New" create form — mirrors the `<form>` block in
 * `tastile-web/src/components/panels/ProjectsSidePanel.tsx`.
 *
 * Web parity on labels:
 *   - name placeholder = "Project name"
 *   - slug placeholder = "slug (optional)"
 * The color and parent controls intentionally accept the same value space as
 * web: any CSS-style color string and any existing workspace (or top level).
 */
@Composable
fun NewProjectForm(
    busy: Boolean,
    errorText: String?,
    workspaces: List<Workspace>,
    onSubmit: (name: String, slug: String?, color: String?, parentId: String?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#6b7280") }
    var parentId by remember { mutableStateOf<String?>(null) }
    var parentMenuOpen by remember { mutableStateOf(false) }
    val ordered = remember(workspaces) { orderWorkspaceTree(workspaces) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MobileSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(80) },
            placeholder = { Text(stringResource(R.string.panels_projects_name_placeholder)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("project-create-name"),
        )
        OutlinedTextField(
            value = slug,
            onValueChange = { v ->
                val lower = v.lowercase()
                slug = lower.filter { c -> SLUG_REGEX.matches(c.toString()) }.take(40)
            },
            placeholder = { Text(stringResource(R.string.panels_projects_slug_placeholder)) },
            supportingText = { Text(stringResource(R.string.panels_projects_slug_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("project-create-slug"),
        )
        OutlinedTextField(
            value = color,
            onValueChange = { color = it },
            label = { Text("Color") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("project-create-color"),
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { parentMenuOpen = true },
                modifier = Modifier.testTag("project-create-parent"),
            ) { Text("Parent project: " + (workspaces.firstOrNull { it.id == parentId }?.displayName ?: "Top level")) }
            DropdownMenu(expanded = parentMenuOpen, onDismissRequest = { parentMenuOpen = false }) {
                DropdownMenuItem(text = { Text("Top level") }, onClick = { parentId = null; parentMenuOpen = false })
                ordered.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text("  ".repeat(entry.depth) + entry.workspace.displayName) },
                        onClick = { parentId = entry.workspace.id; parentMenuOpen = false },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
        ) {
            AppPrimaryButton(
                text = if (busy) stringResource(R.string.panels_projects_creating)
                else stringResource(R.string.panels_projects_create),
                onClick = { onSubmit(name, slug.ifBlank { null }, color.ifBlank { null }, parentId) },
                enabled = !busy && name.isNotBlank(),
                modifier = Modifier.testTag("project-create-submit"),
            )
            AppTertiaryButton(
                text = stringResource(R.string.panels_projects_cancel),
                onClick = onCancel,
                enabled = !busy,
            )
        }
        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}