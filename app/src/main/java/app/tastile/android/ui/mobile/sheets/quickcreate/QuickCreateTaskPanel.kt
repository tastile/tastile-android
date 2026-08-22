package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Timer
// m2-allow: m3-component
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.ExposedDropdownMenuBox
// m2-allow: m3-component
import androidx.compose.material3.ExposedDropdownMenuDefaults
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenu
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: m3-component
import androidx.compose.material3.SegmentedButton
// m2-allow: m3-component
import androidx.compose.material3.SegmentedButtonDefaults
// m2-allow: m3-component
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
// m2-allow: m3-component
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreateProject
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateWhenMode

/**
 * Task workflow - directly mirrors
 * `tastile-web/src/features/create-tile/ui/QuickCreateTask.tsx`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCreateTaskPanel(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    isSubmitting: Boolean,
    submitError: String?,
    projects: List<QuickCreateProject>,
    knownTags: List<String>,
    batchScrollState: ScrollState? = null,
) {
    FormFieldColumn(
        modifier = Modifier
            .testTag("quick-create-task")
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        // Each child owns its own icon column reservation. Do not wrap in
        // FormFieldLayout { } — that would double-book the 20dp + 12dp
        // icon slot and push the title 32dp right of the close icon.
        QuickCreateHeader(
            title = draft.identity.title,
            onTitleChange = { store.updateIdentity(draft.identity.copy(title = it)) },
            modifier = Modifier.testTag("quick-create-task-header"),
        )
        WorkflowBatch(
            workflow = draft.workflow,
            onWorkflowChange = { kind -> store.setWorkflow(kind) },
            modifier = Modifier.testTag("quick-create-task-batch"),
            scrollState = batchScrollState,
        )
        DurationRow(
            currentMin = draft.time.durationMinMax.maxMs?.div(60_000L)?.toInt(),
            onChange = { minutes ->
                store.updateTime(
                    draft.time.copy(
                        durationMinMax = draft.time.durationMinMax.copy(
                            minMs = minutes * 60_000L,
                            maxMs = minutes * 60_000L,
                        )
                    )
                )
            },
        )
        SplitPolicyRow(
            current = draft.schedule.splitPolicyKind.toInt().let { if (it == 1) "split" else "single" },
            onChange = { kind ->
                val policyKind: Short = if (kind == "split") 1 else 0
                store.updateSchedule(draft.schedule.copy(splitPolicyKind = policyKind))
            },
        )
        FormFieldLayout {
            DateTimeRow(
                dateIso = draft.time.span.start,
                timeIso = draft.time.timeOfDayStart.takeIf { it.isNotBlank() },
                onDateChange = { newStart ->
                    val newEnd = if (draft.time.span.end.isBlank()) newStart else draft.time.span.end
                    store.updateTime(
                        draft.time.copy(
                            span = draft.time.span.copy(start = newStart, end = newEnd),
                            timeOfDayStart = draft.time.timeOfDayStart,
                            whenMode = QuickCreateWhenMode.Day,
                        )
                    )
                },
                onTimeChange = { newTime ->
                    store.updateTime(draft.time.copy(timeOfDayStart = newTime))
                },
                datePlaceholder = stringResource(R.string.quickcreate_task_due_date_placeholder),
                timePlaceholder = stringResource(R.string.quickcreate_task_due_time_label),
                dateTestTag = "task-due-date",
                timeTestTag = "task-due-time",
            )
        }
        DetailsAffordanceButton(
            label = stringResource(R.string.tile_edit_open_time),
            onOpen = { store.openSubpanel(QuickCreatePanel.Time) },
            modifier = Modifier.testTag("task-details-time"),
        )
        SubtasksSection(
            draft = draft,
            store = store,
            modifier = Modifier.testTag("task-subtasks"),
        )
        ProjectColorRow(
            projects = projects,
            selectedProjectId = draft.meta.ownerSubjectId,
            selectedColor = parseHexColor(draft.identity.visual.color),
            onProjectChange = { id -> store.updateMeta(draft.meta.copy(ownerSubjectId = id)) },
            onColorChange = { color ->
                store.updateIdentity(
                    draft.identity.copy(
                        visual = draft.identity.visual.copy(color = color.toHexString())
                    )
                )
            },
            modifier = Modifier.testTag("task-project-color"),
        )
        MemoSection(
            memo = draft.meta.memo,
            onMemoChange = { store.updateMeta(draft.meta.copy(memo = it)) },
        )
    }
}

private val DurationPresets: List<Int> = listOf(15, 30, 60, 90, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationRow(currentMin: Int?, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var isCustomMode by remember(currentMin) { mutableStateOf(currentMin != null && currentMin !in DurationPresets) }
    var customValue by remember(currentMin) { mutableStateOf((currentMin ?: 30).toString()) }
    val label = when {
        currentMin == null -> "—"
        isCustomMode -> stringResource(R.string.quickcreate_task_duration_minutes, customValue.toIntOrNull() ?: 0)
        else -> stringResource(R.string.quickcreate_task_duration_minutes, currentMin)
    }

    // FormRow provides its own icon column reservation; do NOT wrap in
    // FormFieldLayout { } (would double-book 20dp + 12dp).
    FormRow(
        icon = {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        trailing = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .width(120.dp)
                        .menuAnchor()
                        .testTag("task-duration-select"),
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DurationPresets.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.quickcreate_task_duration_minutes, preset))
                            },
                            onClick = {
                                expanded = false
                                isCustomMode = false
                                onChange(preset)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.quickcreate_task_duration_custom)) },
                        onClick = {
                            expanded = false
                            isCustomMode = true
                            onChange(customValue.toIntOrNull() ?: 30)
                        },
                    )
                }
            }
        },
    )
    if (isCustomMode) {
        FormFieldLayout {
            OutlinedTextField(
                value = customValue,
                onValueChange = {
                    customValue = it
                    val n = it.toIntOrNull()
                    if (n != null && n > 0) onChange(n)
                },
                label = { Text(stringResource(R.string.quickcreate_task_duration_custom_minutes)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("task-duration-manual"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitPolicyRow(current: String, onChange: (String) -> Unit) {
    // FormRow provides its own icon column reservation; do NOT wrap in
    // FormFieldLayout { } (would double-book 20dp + 12dp).
    FormRow(
        icon = {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = current == "single",
                    onClick = { onChange("single") },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("task-split-single"),
                ) {
                    Text(
                        text = stringResource(R.string.quick_create_break_keep_continuous),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SegmentedButton(
                    selected = current == "split",
                    onClick = { onChange("split") },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("task-split-allow"),
                ) {
                    Text(
                        text = stringResource(R.string.quick_create_break_allow_split),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
    )
}


