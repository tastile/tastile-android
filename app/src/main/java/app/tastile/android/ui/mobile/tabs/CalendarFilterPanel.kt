package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
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
import app.tastile.android.data.api.Workspace
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
            Text("Calendar", modifier = Modifier.weight(1f))
            TextButton(onClick = { showDatePicker = true }, modifier = Modifier.testTag("calendar-mini-date")) {
                Text(selectedDayLabel)
            }
        }
        if (workspaces.isNotEmpty()) {
            Text("Projects ${selected.size}/${allIds.size}")
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
                    TextButton(
                        onClick = { onOwnerIdsChange(normalizeOwnerSelection(toggleProjectCascade(selected, entry.workspace.id, descendants), allIds)) },
                    ) { Text(entry.workspace.displayName) }
                }
            }
        }
    }
    if (showDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onSelectDate(localDateFromEpochMillis(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

/** Empty selection means all projects to the v1 API, matching the Web URL convention. */
internal fun normalizeOwnerSelection(selected: Set<String>, allIds: Set<String>): Set<String> =
    if (selected == allIds) emptySet() else selected
