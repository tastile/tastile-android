package app.tastile.android.ui.mobile.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.tastile.android.R
import app.tastile.android.data.api.Workspace
import app.tastile.android.ui.designsystem.AppSpacing
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
    dashboardViewModel: DashboardViewModel? = null,
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by projectsViewModel.state.collectAsState()
    val creating by projectsViewModel.creating.collectAsState()
    val selectedOwnerId by projectsViewModel.selectedOwnerId.collectAsState()
    var deleteCandidate by remember { mutableStateOf<Workspace?>(null) }
    var editCandidate by remember { mutableStateOf<Workspace?>(null) }

    LaunchedEffect(selectedOwnerId) {
        dashboardViewModel?.setOwnerFilter(selectedOwnerId)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.panels_projects_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!creating) {
                AppTertiaryButton(
                    text = stringResource(R.string.panels_projects_new_button),
                    onClick = projectsViewModel::openCreateForm,
                    leadingIcon = Icons.Outlined.Check,
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
            state.loading -> Text(
                text = stringResource(R.string.panels_projects_loading_projects),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppSpacing.md),
            )
            state.error != null -> Text(
                text = state.error.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = AppSpacing.md),
            )
            else -> ProjectsList(
                workspaces = state.workspaces,
                selectedOwnerId = selectedOwnerId,
                onSelect = projectsViewModel::selectOwner,
                onEditRequest = { ws -> editCandidate = ws },
                onDeleteRequest = { ws -> deleteCandidate = ws },
                modifier = Modifier.padding(horizontal = AppSpacing.sm),
            )
        }
    }

    if (editCandidate != null) {
        val target = editCandidate!!
        AlertDialog(
            onDismissRequest = { editCandidate = null },
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
                    onCancel = { editCandidate = null },
                )
            },
            confirmButton = {},
        )
    }

    if (deleteCandidate != null) {
        val target = deleteCandidate!!
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
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
                AppPrimaryButton(
                    text = stringResource(R.string.panels_projects_delete),
                    onClick = {
                        projectsViewModel.deleteWorkspace(target.id)
                        deleteCandidate = null
                    },
                    leadingIcon = Icons.Outlined.Delete,
                )
            },
            dismissButton = {
                AppTertiaryButton(
                    text = stringResource(R.string.panels_projects_cancel),
                    onClick = { deleteCandidate = null },
                    leadingIcon = Icons.Outlined.Close,
                )
            },
        )
    }
}
