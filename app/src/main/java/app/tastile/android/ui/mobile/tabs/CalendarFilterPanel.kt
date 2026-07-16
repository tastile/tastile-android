package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.data.api.Workspace
import app.tastile.android.ui.mobile.designsystem.AppPickerButton
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppSecondaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
import app.tastile.android.ui.mobile.designsystem.SectionHeader
import app.tastile.android.ui.mobile.designsystem.StatChip
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
            SectionHeader(title = "Calendar", modifier = Modifier.weight(1f))
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
                horizontalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatChip(
                    label = "Projects",
                    value = "${selected.size}/${allIds.size}",
                    background = MaterialTheme.colorScheme.secondaryContainer,
                    foreground = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                AppSecondaryButton(
                    text = "Projects",
                    onClick = { onOwnerIdsChange(emptySet()) },
                    leadingIcon = Icons.Outlined.FilterList,
                )
            }
            SectionHeader(title = "Projects")
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
                    AppSecondaryButton(
                        text = entry.workspace.displayName,
                        onClick = {
                            onOwnerIdsChange(normalizeOwnerSelection(toggleProjectCascade(selected, entry.workspace.id, descendants), allIds))
                        },
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
                AppPrimaryButton(
                    text = "OK",
                    onClick = {
                        state.selectedDateMillis?.let { onSelectDate(localDateFromEpochMillis(it)) }
                        showDatePicker = false
                    },
                    leadingIcon = Icons.Outlined.Check,
                )
            },
            dismissButton = {
                AppTertiaryButton(text = "Cancel", onClick = { showDatePicker = false })
            },
        ) { DatePicker(state = state) }
    }
}

/** Empty selection means all projects to the v1 API, matching the Web URL convention. */
internal fun normalizeOwnerSelection(selected: Set<String>, allIds: Set<String>): Set<String> =
    if (selected == allIds) emptySet() else selected
