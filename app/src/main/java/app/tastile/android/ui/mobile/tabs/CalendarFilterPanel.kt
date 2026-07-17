package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
// m2-allow: m3-component
import androidx.compose.material3.Checkbox
// m2-allow: m3-component
import androidx.compose.material3.DatePicker
// m2-allow: m3-component
import androidx.compose.material3.DatePickerDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.TriStateCheckbox
// m2-allow: m3-component
import androidx.compose.material3.rememberDatePickerState
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
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.data.api.Workspace
import app.tastile.android.ui.mobile.components.AppPickerButton
import app.tastile.android.ui.mobile.components.AppSectionHeader
import app.tastile.android.ui.mobile.panels.projects.orderWorkspaceTree
import app.tastile.android.ui.util.localDateFromEpochMillis

/** Compact mobile counterpart of Web's CalendarSidePanel mini calendar and project tree. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarFilterPanel(
    selectedDayLabel: String,
    workspaces: List<Workspace>,
    ownerIds: List<String>,
    onSelectDate: (String) -> Unit,
    onOwnerIdsChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val allIds = remember(workspaces) { workspaces.mapTo(linkedSetOf()) { it.id } }
    val selected = remember(ownerIds, allIds) {
        if (ownerIds.isEmpty()) allIds else ownerIds.filterTo(linkedSetOf()) { it in allIds }
    }
    val descendants = remember(workspaces) { projectDescendantMap(workspaces) }
    Column(
        modifier = modifier.testTag("calendar-filters").padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppSectionHeader(title = "Calendar", modifier = Modifier.weight(1f))
            AppPickerButton(
                label = "Date", // TODO i18n
                value = selectedDayLabel.ifBlank { "—" },
                onClick = { showDatePicker = true },
                leadingIcon = Icons.Outlined.CalendarMonth,
                modifier = Modifier.testTag("calendar-mini-date"),
            )
        }
        if (workspaces.isNotEmpty()) {
            Row(
                modifier = Modifier.clickable { onOwnerIdsChange(emptySet()) },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatChip(
                    label = "Projects",
                    value = "${selected.size}/${allIds.size}",
                    background = MaterialTheme.colorScheme.secondaryContainer,
                    foreground = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                NiaFilledTonalButton(
                    onClick = { onOwnerIdsChange(emptySet()) },
                    leadingIcon = { Icon(Icons.Outlined.FilterList, contentDescription = null) },
                    text = { Text("Projects") },
                )
            }
            AppSectionHeader(title = "Projects")
            orderWorkspaceTree(workspaces).forEach { entry ->
                val state = projectCheckState(entry.workspace.id, selected, descendants)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = (entry.depth * 18).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.indeterminate) {
                        TriStateCheckbox(
                            state = androidx.compose.ui.state.ToggleableState.Indeterminate,
                            onClick = {
                                onOwnerIdsChange(normalizeOwnerSelection(toggleProjectCascade(selected, entry.workspace.id, descendants), allIds))
                            },
                            modifier = Modifier.testTag("calendar-project-${entry.workspace.id}"),
                        )
                    } else {
                        Checkbox(
                            checked = state.checked,
                            onCheckedChange = {
                                onOwnerIdsChange(normalizeOwnerSelection(toggleProjectCascade(selected, entry.workspace.id, descendants), allIds))
                            },
                            modifier = Modifier.testTag("calendar-project-${entry.workspace.id}"),
                        )
                    }
                    NiaFilledTonalButton(
                        onClick = {
                            onOwnerIdsChange(normalizeOwnerSelection(toggleProjectCascade(selected, entry.workspace.id, descendants), allIds))
                        },
                        text = { Text(entry.workspace.displayName) },
                    )
                }
            }
        }
    }
    if (showDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                NiaButton(
                    onClick = {
                        state.selectedDateMillis?.let { onSelectDate(localDateFromEpochMillis(it)) }
                        showDatePicker = false
                    },
                    leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
                    text = { Text("OK") },
                )
            },
            dismissButton = {
                NiaTextButton(
                    onClick = { showDatePicker = false },
                    text = { Text("Cancel") },
                )
            },
        ) { DatePicker(state = state) }
    }
}

/** Empty selection means all projects to the v1 API, matching the Web URL convention. */
internal fun normalizeOwnerSelection(selected: Set<String>, allIds: Set<String>): Set<String> =
    if (selected == allIds) emptySet() else selected

@Composable
private fun StatChip(
    label: String,
    value: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .background(background, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, color = foreground, style = MaterialTheme.typography.labelSmall)
    }
}
