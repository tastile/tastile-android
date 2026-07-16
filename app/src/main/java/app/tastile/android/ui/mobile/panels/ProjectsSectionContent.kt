package app.tastile.android.ui.mobile.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaLoadingWheel
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.mobile.panels.projects.NewProjectForm
import app.tastile.android.ui.mobile.panels.projects.ProjectsList
import app.tastile.android.ui.mobile.panels.projects.ProjectEditForm
import app.tastile.android.ui.dashboard.DashboardViewModel

/**
 * Section pane content for `SidePanelSection.Projects`.
 *
 * Composition (binding, mirrors `tastile-web/src/components/panels/
 * ProjectsSidePanel.tsx`):
 *   1. Header row — projects label + "+ New" button.
 *   2. Inline new-project form (when open).
 *   3. List of workspaces (sorted parent-before-child) + "All Projects" row.
 *
 * Mobile variation: the web parent-select dropdown is omitted (we
 * present the same flat ordered list without a nested control) and
 * the long-press × reveal replaces `group-hover:visible`. Total visible
 * control count per state:
 *   - idle:    2 buttons (label, "+ New") + N rows
 *   - creating: 6 (label, "New", 2 inputs, 1 swatch row, submit, cancel)
 *              + N list rows
 *   - loading: 1 ("Loading projects…")
 *   - error:   2 (label, "+ New") + error row + 0 list rows
 */
@Composable
fun ProjectsSectionContent(
    modifier: Modifier = Modifier,
    dashboardViewModel: DashboardViewModel,
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by projectsViewModel.state.collectAsStateWithLifecycle()
    val creating by projectsViewModel.creating.collectAsStateWithLifecycle()
    val selectedOwnerId by projectsViewModel.selectedOwnerId.collectAsStateWithLifecycle()
    val deleteCandidate by projectsViewModel.deleteCandidate.collectAsStateWithLifecycle()
    val editCandidate by projectsViewModel.editCandidate.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.panels_projects_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!creating) {
                NiaTextButton(
                    text = { Text(stringResource(R.string.panels_projects_new_button)) },
                    onClick = projectsViewModel::openCreateForm,
                    leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
                )
            }
        }

        if (creating) {
            NewProjectForm(
                busy = state.createBusy,
                errorText = state.createError,
                workspaces = state.workspaces,
                onSubmit = { name, slug, color, parentId ->
                    projectsViewModel.create(name, slug, color ?: "#6b7280", parentId)
                },
                onCancel = projectsViewModel::closeCreateForm,
            )
        }

        when {
            state.loading -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("projects-section-loading"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NiaLoadingWheel(
                    contentDesc = stringResource(R.string.panels_projects_loading_projects),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.panels_projects_loading_projects),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.error != null -> Text(
                text = state.error.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            else -> ProjectsList(
                workspaces = state.workspaces,
                selectedOwnerId = selectedOwnerId,
                onSelect = { id ->
                    projectsViewModel.selectOwner(id)
                    dashboardViewModel.setOwnerFilter(id)
                },
                onEditRequest = projectsViewModel::requestEdit,
                onDeleteRequest = projectsViewModel::requestDelete,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }

    if (editCandidate != null) {
        val target = editCandidate!!
        AlertDialog(
            onDismissRequest = projectsViewModel::cancelEdit,
            title = { Text("Edit project") },
            text = {
                ProjectEditForm(
                    workspace = target,
                    workspaces = state.workspaces,
                    busy = state.updateBusy,
                    errorText = state.updateError,
                    onSave = { name, slug, color, parent ->
                        projectsViewModel.update(target.id, name, slug, color, parent)
                    },
                    onCancel = projectsViewModel::cancelEdit,
                )
            },
            confirmButton = {},
        )
    }

    if (deleteCandidate != null) {
        val target = deleteCandidate!!
        AlertDialog(
            onDismissRequest = projectsViewModel::cancelDelete,
            title = {
                Text(
                    stringResource(
                        R.string.panels_projects_delete_confirm_title,
                        target.displayName,
                    ),
                )
            },
            text = {
                Text(stringResource(R.string.panels_projects_delete_confirm_body))
            },
            confirmButton = {
                NiaButton(
                    text = { Text(stringResource(R.string.panels_projects_delete)) },
                    onClick = {
                        projectsViewModel.deleteWorkspace(target.id)
                    },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                )
            },
            dismissButton = {
                NiaTextButton(
                    text = { Text(stringResource(R.string.panels_projects_cancel)) },
                    onClick = projectsViewModel::cancelDelete,
                    leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                )
            },
        )
    }
}