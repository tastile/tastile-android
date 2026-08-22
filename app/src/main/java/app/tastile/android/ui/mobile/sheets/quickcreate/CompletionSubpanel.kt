/*
 * CompletionSubpanel.kt
 *
 * Authoring UI for the tile's completion tree:
 *   * root [QuickCreateConditionNode] logic (all / any / not / term)
 *   * term-type chips and per-kind term field editors
 *     (calendar, moment, relation, task, gap, requirement, fact/metric/feedback,
 *      life)
 *   * time-requirement list with minute steppers
 *
 * Rows use the 24dp icon column + 16dp gap reservation provided
 * structurally by `FormFieldLayout`, or bypass it entirely via
 * `ScrollableChipRow` for chrome-less chip batches. The 16dp outer
 * horizontal padding is applied by `FormFieldRow` / `ScrollableChipRow`.
 *
 * Pure JSON mutation helpers live at the bottom of the file; condition
 * (de)serialization is delegated to the larger submission encoder.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistAddCheck
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
// m2-allow: m3-component
import androidx.compose.material3.AssistChip
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.mobile.components.picker.ReferenceOption
import app.tastile.android.ui.mobile.components.picker.TimePickerSheet
import app.tastile.android.ui.mobile.sheets.QuickCreateConditionNode
import app.tastile.android.ui.mobile.sheets.QuickCreateDateRange
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskDefinition
import app.tastile.android.ui.mobile.sheets.QuickCreateTimeRequirement
import app.tastile.android.ui.mobile.sheets.QuickCreateWindowRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
internal fun CompletionPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_logic_header))
    val logicKinds = listOf(
        0 to stringResource(R.string.quickcreate_completion_logic_all),
        1 to stringResource(R.string.quickcreate_completion_logic_any),
        2 to stringResource(R.string.quickcreate_completion_logic_not),
    )
    val logicIcon: (Int) -> androidx.compose.ui.graphics.vector.ImageVector = { kind -> when (kind) {
        0 -> Icons.AutoMirrored.Outlined.PlaylistAddCheck
        1 -> Icons.AutoMirrored.Outlined.PlaylistAdd
        else -> Icons.Outlined.Block
    } }
    ScrollableChipRow {
        logicKinds.forEach { (kind, label) ->
            FilterChip(
                selected = draft.plan.completion.root.kind == kind,
                onClick = {
                    store.updatePlan(
                        draft.plan.copy(
                            completion = draft.plan.completion.copy(
                                root = draft.plan.completion.root.copy(kind = kind, term = null),
                            ),
                        ),
                    )
                },
                label = { Text(label) },
                leadingIcon = { Icon(logicIcon(kind), contentDescription = null) },
                modifier = Modifier.testTag("quick-create-completion-logic-${label.lowercase()}"),
            )
        }
    }
    ConditionControls(
        draft.plan.completion.root,
        onChange = { root -> store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = root))) },
        allowTermKind = false,
        tasks = draft.plan.completion.tasks,
        requirements = draft.plan.completion.timeRequirements,
        references = referenceOptionsFor(draft.plan.references),
    )
    val completionAddTerms = listOf(
        Triple("task", stringResource(R.string.quickcreate_panel_completion_term_meta), Icons.Outlined.Task),
        Triple("relation", stringResource(R.string.quickcreate_panel_completion_term_relation), Icons.Outlined.Link),
        Triple("metric", stringResource(R.string.quickcreate_panel_completion_term_metric), Icons.Outlined.BarChart),
    )
    ScrollableChipRow {
        completionAddTerms.forEach { (kind, label, icon) ->
            AssistChip(
                onClick = { addCompletionTerm(draft, store, kind) },
                label = { Text(label) },
                leadingIcon = { Icon(icon, contentDescription = null) },
                modifier = Modifier.testTag("quick-create-completion-add-$kind"),
            )
        }
    }
    FormFieldLayout {
        NiaFilledTonalButton(
            onClick = {
                store.updatePlan(
                    draft.plan.copy(
                        completion = draft.plan.completion.copy(
                            timeRequirements = draft.plan.completion.timeRequirements + webTimeRequirement(
                                draft.time.durationMinMax.minMs,
                            ),
                        ),
                    ),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-completion-add-time"),
            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.quickcreate_panel_add_time_requirement)) },
        )
    }
    draft.plan.completion.timeRequirements.forEachIndexed { index, requirement ->
        val required = requirement.required.jsonObjectOrEmpty()
        val minimumMinutes = required.long("minMs")?.div(60_000L)?.toString().orEmpty()
        FormFieldLayout(icon = Icons.Outlined.Timer) {
            LocalNumberField(
                value = minimumMinutes,
                onValueChange = { input: String ->
                    val minutes = input.toLongOrNull()
                    val nextRequired = when {
                        input.isBlank() -> required.with("minMs", null)
                        minutes == null -> required
                        else -> required.with("minMs", minutes.coerceAtLeast(5L) * 60_000L)
                    }
                    updateTimeRequirement(draft, store, index, requirement.copy(required = nextRequired))
                },
                label = stringResource(R.string.quickcreate_panel_field_minutes),
                suffix = stringResource(R.string.quickcreate_panel_field_minutes_suffix),
                min = 5,
                step = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("time-requirement-$index-required-minutes"),
            )
        }
        FormFieldLayout {
            NiaFilledTonalButton(
                onClick = {
                    store.updatePlan(
                        draft.plan.copy(
                            completion = draft.plan.completion.copy(
                                timeRequirements = draft.plan.completion.timeRequirements.filterIndexed { item, _ -> item != index },
                            ),
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("time-requirement-$index-remove"),
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                text = { Text(stringResource(R.string.quickcreate_panel_remove_time_requirement)) },
            )
        }
    }
    FormFieldLayout {
        NiaTextButton(
            onClick = {
                store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = QuickCreateConditionNode(0), timeRequirements = emptyList(), tasks = emptyList())))
            },
            modifier = Modifier
                .testTag("quick-create-completion-clear"),
            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
            text = { Text(stringResource(R.string.quickcreate_panel_clear_completion)) },
        )
    }
}

internal fun addCompletionTerm(draft: QuickCreateDraftState, store: QuickCreateStateStore, kind: String) {
    val term = if (kind == "task" && draft.plan.completion.tasks.isNotEmpty()) {
        termValue("task", mapOf("taskId" to JsonPrimitive(draft.plan.completion.tasks.first().id), "state" to JsonPrimitive(2)))
    } else defaultTermValue(kind)
    store.appendCompletionTerm(term)
}

@Composable internal fun ConditionControls(
    node: QuickCreateConditionNode,
    onChange: (QuickCreateConditionNode) -> Unit,
    path: String = "root",
    allowTermKind: Boolean = true,
    tasks: List<QuickCreateTaskDefinition> = emptyList(),
    requirements: List<QuickCreateTimeRequirement> = emptyList(),
    references: List<ReferenceOption> = emptyList(),
) {
    val logicKinds = listOf(
        0 to stringResource(R.string.quickcreate_completion_logic_all),
        1 to stringResource(R.string.quickcreate_completion_logic_any),
        2 to stringResource(R.string.quickcreate_completion_logic_not),
        3 to "TERM",
    ).filter { allowTermKind || it.first != 3 }
    val logicIcon: (Int) -> androidx.compose.ui.graphics.vector.ImageVector = { kind -> when (kind) {
        0 -> Icons.AutoMirrored.Outlined.PlaylistAddCheck
        1 -> Icons.AutoMirrored.Outlined.PlaylistAdd
        2 -> Icons.Outlined.Block
        else -> Icons.Outlined.TextFields
    } }
    ScrollableChipRow {
        logicKinds.forEach { (kind, label) ->
            FilterChip(
                selected = node.kind == kind,
                onClick = { onChange(node.copy(kind = kind, children = if (kind == 3) emptyList() else node.children, term = if (kind == 3) defaultTermValue("calendar") else null)) },
                label = { Text(label) },
                leadingIcon = { Icon(logicIcon(kind), contentDescription = null) },
                modifier = Modifier.testTag("condition-$path-logic-${label.lowercase()}"),
            )
        }
    }
    if (node.kind == 3) {
        val termTypes = listOf(
            "calendar" to stringResource(R.string.quickcreate_panel_term_calendar),
            "moment" to stringResource(R.string.quickcreate_panel_term_moment),
            "relation" to stringResource(R.string.quickcreate_panel_term_relation),
            "gap" to stringResource(R.string.quickcreate_panel_term_gap),
            "requirement" to stringResource(R.string.quickcreate_panel_term_requirement),
            "task" to stringResource(R.string.quickcreate_panel_term_task),
            "fact" to stringResource(R.string.quickcreate_panel_term_fact),
            "metric" to stringResource(R.string.quickcreate_panel_term_metric),
            "life" to stringResource(R.string.quickcreate_panel_term_life),
        )
        val termIcon: (String) -> androidx.compose.ui.graphics.vector.ImageVector = { type -> when (type) {
            "calendar" -> Icons.Outlined.CalendarMonth
            "moment" -> Icons.Outlined.Schedule
            "relation" -> Icons.Outlined.Link
            "gap" -> Icons.Outlined.HorizontalRule
            "requirement" -> Icons.Outlined.Check
            "task" -> Icons.Outlined.Task
            "fact" -> Icons.Filled.Lightbulb
            "metric" -> Icons.Outlined.BarChart
            "life" -> Icons.Outlined.Favorite
            else -> Icons.Outlined.TextFields
        } }
        ScrollableChipRow {
            termTypes.forEach { (type, label) ->
                FilterChip(
                    selected = node.term?.jsonObjectOrEmpty()?.string("kind") == type,
                    onClick = { onChange(node.copy(term = defaultTermValue(type))) },
                    label = { Text(label) },
                    leadingIcon = { Icon(termIcon(type), contentDescription = null) },
                    modifier = Modifier.testTag("condition-$path-term-$type"),
                )
            }
        }
        val term = node.term?.jsonObjectOrEmpty() ?: JsonObject(emptyMap())
        when (term.string("kind")) {
            "calendar" -> CalendarTermFields(term, path) { value -> onChange(node.copy(term = value)) }
            "moment" -> MomentTermFields(term, path, references) { value -> onChange(node.copy(term = value)) }
            "relation" -> RelationTermFields(term, references) { value -> onChange(node.copy(term = value)) }
            "task" -> TaskTermFields(term, tasks) { value -> onChange(node.copy(term = value)) }
            "gap" -> GapTermFields(path)
            "requirement" -> RequirementTermFields(term, path, requirements) { value -> onChange(node.copy(term = value)) }
            "fact" -> ScalarTermFields(term, path, "fact", "factId") { value -> onChange(node.copy(term = value)) }
            "metric" -> ScalarTermFields(term, path, "metric", "metricId") { value -> onChange(node.copy(term = value)) }
            "feedback" -> ScalarTermFields(term, path, "feedback", "feedbackTxnId") { value -> onChange(node.copy(term = value)) }
            "life" -> LifeTermFields(term, path) { value -> onChange(node.copy(term = value)) }
        }
    }
    else {
        node.children.forEachIndexed { index, child ->
            ConditionControls(
                child,
                { updated -> onChange(node.copy(children = node.children.replace(index, updated))) },
                "$path-$index",
                allowTermKind = true,
                tasks = tasks,
                requirements = requirements,
                references = references,
            )
            FormFieldLayout {
                NiaFilledTonalButton(
                    onClick = { onChange(node.copy(children = node.children.filterIndexed { item, _ -> item != index })) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("condition-$path-child-$index-remove"),
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    text = { Text(stringResource(R.string.quickcreate_panel_remove)) },
                )
            }
        }
        FormFieldLayout {
            NiaFilledTonalButton(
                onClick = { onChange(node.copy(children = node.children + QuickCreateConditionNode(3, term = defaultTermValue("calendar")))) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("condition-$path-add-child"),
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.quickcreate_panel_add_condition)) },
            )
        }
    }
}

@Composable internal fun CalendarTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    FormFieldLayout(icon = Icons.Outlined.CalendarViewWeek) {
        LocalNumberField(
            value = value.string("weekdayMask", "0"),
            onValueChange = { input -> onChange(term.withValue("weekdayMask", input.toIntOrNull() ?: 0)) },
            label = stringResource(R.string.quickcreate_panel_field_weekday_mask),
            suffix = stringResource(R.string.quickcreate_panel_field_weekday_mask_suffix),
            min = 0,
            max = 127,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("condition-$path-calendar-weekday-mask"),
        )
    }
    FormFieldLayout(icon = Icons.Outlined.Timeline) {
        LocalNumberField(
            value = value.string("offsetMin", "0"),
            onValueChange = { input -> onChange(term.withValue("offsetMin", input.toIntOrNull() ?: 0)) },
            label = stringResource(R.string.quickcreate_panel_field_offset_minutes),
            suffix = stringResource(R.string.quickcreate_panel_field_offset_minutes_suffix),
            min = -720,
            max = 720,
            step = 15,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("condition-$path-calendar-offset"),
        )
    }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var showCalendarStart by remember { mutableStateOf(false) }
    var showCalendarEnd by remember { mutableStateOf(false) }
    FormFieldLayout(icon = Icons.Outlined.Schedule) {
        LocalPickerField(
            label = stringResource(R.string.quickcreate_panel_field_time_start),
            value = value.string("timeStart").ifBlank { "—" },
            onClick = { showCalendarStart = true },
            modifier = Modifier.fillMaxWidth().testTag("condition-calendar-start"),
        )
    }
    if (showCalendarStart) {
        TimePickerSheet(
            initial = runCatching { LocalTime.parse(value.string("timeStart"), timeFmt) }
                .getOrElse { LocalTime.of(9, 0) },
            onConfirm = { time ->
                onChange(term.withValue("timeStart", time.format(timeFmt)))
                showCalendarStart = false
            },
            onDismiss = { showCalendarStart = false },
            titleRes = R.string.picker_time_start,
        )
    }
    FormFieldLayout(icon = Icons.Outlined.Schedule) {
        LocalPickerField(
            label = stringResource(R.string.quickcreate_panel_field_time_end),
            value = value.string("timeEnd").ifBlank { "—" },
            onClick = { showCalendarEnd = true },
            modifier = Modifier.fillMaxWidth().testTag("condition-calendar-end"),
        )
    }
    if (showCalendarEnd) {
        TimePickerSheet(
            initial = runCatching { LocalTime.parse(value.string("timeEnd"), timeFmt) }
                .getOrElse { LocalTime.of(18, 0) },
            onConfirm = { time ->
                onChange(term.withValue("timeEnd", time.format(timeFmt)))
                showCalendarEnd = false
            },
            onDismiss = { showCalendarEnd = false },
            titleRes = R.string.picker_time_end,
        )
    }
}

@Composable internal fun MomentTermFields(
    term: JsonObject,
    path: String,
    references: List<ReferenceOption>,
    onChange: (JsonObject) -> Unit,
) {
    val value = term.valueObject()
    FormFieldLayout(icon = Icons.Outlined.Key) {
        LocalOptionPickerField(
            label = stringResource(R.string.quickcreate_panel_field_moment_id),
            value = value.string("referenceId"),
            options = references.map { it.id to it.label },
            onSelect = { id -> onChange(term.withValue("referenceId", id)) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "condition-moment-reference",
        )
    }
    FormFieldLayout(icon = Icons.Outlined.Timer) {
        LocalNumberField(
            value = value.string("offsetMs", "0"),
            onValueChange = { input -> onChange(term.withValue("offsetMs", input.toLongOrNull() ?: 0L)) },
            label = stringResource(R.string.quickcreate_panel_field_offset_ms),
            suffix = stringResource(R.string.quickcreate_panel_field_offset_ms_suffix),
            step = 1000,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("condition-moment-offset"),
        )
    }
}

@Composable internal fun RelationTermFields(
    term: JsonObject,
    references: List<ReferenceOption>,
    onChange: (JsonObject) -> Unit,
) {
    val value = term.valueObject()
    FormFieldLayout(icon = Icons.Outlined.Key) {
        LocalOptionPickerField(
            label = stringResource(R.string.quickcreate_panel_field_reference_id),
            value = value.string("referenceId"),
            options = references.map { it.id to it.label },
            onSelect = { id -> onChange(term.withValue("referenceId", id)) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "condition-relation-reference",
        )
    }
    val relations = listOf(0, 1, 2, 3, 4)
    ScrollableChipRow {
        relations.forEach { relation ->
            FilterChip(
                selected = value.string("relation", "0").toIntOrNull() == relation,
                onClick = { onChange(term.withValue("relation", relation)) },
                label = {
                    val labelRes = when (relation) {
                        0 -> R.string.quickcreate_panel_relation_touch
                        1 -> R.string.quickcreate_panel_relation_inside
                        2 -> R.string.quickcreate_panel_relation_overlap
                        3 -> R.string.quickcreate_panel_relation_before
                        else -> R.string.quickcreate_panel_relation_after
                    }
                    Text(stringResource(labelRes))
                },
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                modifier = Modifier.testTag("condition-relation-kind-$relation"),
            )
        }
    }
    val windowKinds = listOf(0, 1, 2, 3)
    ScrollableChipRow {
        windowKinds.forEach { kind ->
            FilterChip(
                selected = value.string("windowKind", "0").toIntOrNull() == kind,
                onClick = { onChange(term.withValue("windowKind", kind)) },
                label = {
                    val labelRes = when (kind) {
                        0 -> R.string.quickcreate_panel_window_kind_calendar
                        1 -> R.string.quickcreate_panel_window_kind_label_span
                        2 -> R.string.quickcreate_panel_window_kind_parent_span
                        else -> R.string.quickcreate_panel_window_kind_gap
                    }
                    Text(stringResource(labelRes))
                },
                leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                modifier = Modifier.testTag("condition-relation-window-$kind"),
            )
        }
    }
}

@Composable internal fun TaskTermFields(
    term: JsonObject,
    tasks: List<QuickCreateTaskDefinition>,
    onChange: (JsonObject) -> Unit,
) {
    val value = term.valueObject()
    FormFieldLayout(icon = Icons.Outlined.Task) {
        LocalOptionPickerField(
            label = stringResource(R.string.quickcreate_panel_field_task_id),
            value = value.string("taskId"),
            options = tasks.map { task ->
                task.id to task.content.title.ifBlank { stringResource(R.string.quickcreate_subtasks_untitled) }
            },
            onSelect = { id -> onChange(term.withValue("taskId", id)) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "condition-task-id",
        )
    }
    FormFieldLayout(icon = Icons.Outlined.Checklist) {
        LocalNumberField(
            value = value.string("state", "0"),
            onValueChange = { input -> onChange(term.withValue("state", input.toIntOrNull() ?: 0)) },
            label = stringResource(R.string.quickcreate_panel_field_state_label),
            suffix = stringResource(R.string.quickcreate_panel_field_state_suffix),
            min = 0,
            max = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("condition-task-state"),
        )
    }
}

@Composable internal fun GapTermFields(path: String) {
    FormFieldLayout(icon = Icons.Outlined.HorizontalRule) {
        Text(
            text = stringResource(R.string.quickcreate_panel_gap_placeholder),
            modifier = Modifier
                .testTag("condition-$path-gap-placeholder"),
        )
    }
}

@Composable internal fun RequirementTermFields(
    term: JsonObject,
    path: String,
    requirements: List<QuickCreateTimeRequirement>,
    onChange: (JsonObject) -> Unit,
) {
    val value = term.valueObject()
    FormFieldLayout(icon = Icons.Outlined.Key) {
        LocalOptionPickerField(
            label = stringResource(R.string.quickcreate_panel_field_requirement_id),
            value = value.string("requirementId"),
            options = requirements.mapIndexed { index, requirement ->
                requirement.id to stringResource(R.string.quick_create_rule_number, index + 1)
            },
            onSelect = { id -> onChange(term.withValue("requirementId", id)) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "condition-$path-requirement-id",
        )
    }
    FormFieldLayout(icon = Icons.Outlined.Checklist) {
        LocalNumberField(
            value = value.string("state", "0"),
            onValueChange = { input -> onChange(term.withValue("state", input.toIntOrNull() ?: 0)) },
            label = stringResource(R.string.quickcreate_panel_field_state_label),
            suffix = stringResource(R.string.quickcreate_panel_field_state_suffix),
            min = 0,
            max = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("condition-$path-requirement-state"),
        )
    }
}

@Composable internal fun ScalarTermFields(
    term: JsonObject,
    path: String,
    kind: String,
    idKey: String,
    onChange: (JsonObject) -> Unit,
) {
    val value = term.valueObject()
    FormFieldLayout(icon = Icons.Outlined.Key) {
        OutlinedTextField(
            value = value.string(idKey),
            onValueChange = { input -> onChange(term.withValue(idKey, input)) },
            label = { Text(stringResource(R.string.quickcreate_panel_field_id_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("condition-$path-$kind-id"),
        )
    }
    FormFieldLayout(icon = Icons.Outlined.Tune) {
        LocalNumberField(
            value = value.string("op", "0"),
            onValueChange = { input -> onChange(term.withValue("op", input.toIntOrNull() ?: 0)) },
            label = stringResource(R.string.quickcreate_panel_field_op_label),
            min = 0,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("condition-$path-$kind-op"),
        )
    }
    FormFieldLayout(icon = Icons.Outlined.TextFields) {
        OutlinedTextField(
            value = value.string("value"),
            onValueChange = { input -> onChange(term.withValue("value", scalarValue(input))) },
            label = { Text(stringResource(R.string.quickcreate_panel_field_value_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("condition-$path-$kind-value"),
        )
    }
}

@Composable internal fun LifeTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    FormFieldLayout(icon = Icons.Outlined.Favorite) {
        OutlinedTextField(
            value = value.string("target"),
            onValueChange = { input -> onChange(term.withValue("target", input)) },
            label = { Text(stringResource(R.string.quickcreate_panel_field_target_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("condition-$path-life-target"),
        )
    }
    FormFieldLayout(icon = Icons.Outlined.Checklist) {
        LocalNumberField(
            value = value.string("state", "0"),
            onValueChange = { input -> onChange(term.withValue("state", input.toIntOrNull() ?: 0)) },
            label = stringResource(R.string.quickcreate_panel_field_state_label),
            suffix = stringResource(R.string.quickcreate_panel_field_state_suffix),
            min = 0,
            max = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("condition-$path-life-state"),
        )
    }
}

internal fun defaultTermValue(kind: String): JsonObject = when (kind) {
    "calendar" -> termValue(kind, mapOf("weekdayMask" to JsonPrimitive(0), "timeStart" to JsonNull, "timeEnd" to JsonNull, "holidayKind" to JsonPrimitive(2), "dateRange" to JsonNull, "offsetMin" to JsonPrimitive(0)))
    "moment" -> termValue(kind, mapOf("referenceId" to JsonNull, "point" to JsonNull, "offsetMs" to JsonPrimitive(0)))
    "relation" -> termValue(kind, mapOf("referenceId" to JsonPrimitive(""), "relation" to JsonPrimitive(0), "windowKind" to JsonPrimitive(0)))
    "gap" -> termValue(kind, mapOf(
        "scope" to JsonPrimitive(0),
        "leftAnchor" to JsonObject(mapOf("referenceId" to JsonNull, "point" to JsonNull)),
        "rightAnchor" to JsonObject(mapOf("referenceId" to JsonNull, "point" to JsonNull)),
        "size" to JsonObject(mapOf("minMs" to JsonNull, "maxMs" to JsonNull)),
    ))
    "requirement" -> termValue(kind, mapOf("requirementId" to JsonPrimitive(""), "state" to JsonPrimitive(0)))
    "fact" -> termValue(kind, mapOf("factId" to JsonPrimitive(""), "op" to JsonPrimitive(0), "value" to JsonNull))
    "metric" -> termValue(kind, mapOf("metricId" to JsonPrimitive(""), "op" to JsonPrimitive(0), "value" to JsonNull))
    "feedback" -> termValue(kind, mapOf("feedbackTxnId" to JsonPrimitive(""), "op" to JsonPrimitive(0), "value" to JsonNull))
    "life" -> termValue(kind, mapOf("target" to JsonPrimitive(""), "state" to JsonPrimitive(0)))
    "task" -> termValue(kind, mapOf("taskId" to JsonPrimitive(""), "state" to JsonPrimitive(0)))
    else -> defaultTermValue("calendar")
}

internal fun termValue(kind: String, value: Map<String, JsonElement>) = JsonObject(mapOf("kind" to JsonPrimitive(kind), "value" to JsonObject(value)))
internal fun JsonObject.valueObject(): JsonObject = this["value"] as? JsonObject ?: JsonObject(emptyMap())
internal fun JsonObject.withValue(key: String, value: Any?): JsonObject = with("value", valueObject().with(key, value))
internal fun scalarValue(input: String): Any? = when {
    input.isBlank() -> null
    input.toLongOrNull() != null -> input.toLong()
    input.toDoubleOrNull()?.isFinite() == true -> input.toDouble()
    else -> input
}

internal fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull

internal fun JsonObject.with(key: String, value: Any?): JsonObject = JsonObject(toMutableMap().also { map -> map[key] = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is Int -> JsonPrimitive(value)
    is Long -> JsonPrimitive(value)
    is Double -> JsonPrimitive(value)
    else -> JsonPrimitive(value.toString())
} })

// ── Reserved helpers (defined here so the original monolithic file remains
// fully representable; the submission encoder consumes these).

internal fun updateTask(draft: QuickCreateDraftState, store: QuickCreateStateStore, index: Int, task: QuickCreateTaskDefinition) {
    store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks.replace(index, task))))
}

internal fun conditionToJson(condition: QuickCreateConditionNode): JsonElement = buildJsonObject {
    put("kind", JsonPrimitive(condition.kind))
    put("children", buildJsonArray { condition.children.forEach { add(conditionToJson(it)) } })
    put("term", condition.term ?: JsonNull)
}

internal fun conditionFromJson(value: JsonElement): QuickCreateConditionNode? = runCatching {
    val objectValue = value.jsonObject
    QuickCreateConditionNode(
        kind = objectValue.getValue("kind").jsonPrimitive.content.toInt(),
        children = objectValue["children"]?.jsonArray?.mapNotNull(::conditionFromJson) ?: emptyList(),
        term = objectValue["term"].takeUnless { it == null || it is JsonNull },
    )
}.getOrNull()

internal fun windowRulesToJson(rules: List<QuickCreateWindowRule>): JsonElement = buildJsonArray {
    rules.forEach { rule -> add(buildJsonObject {
        put("id", JsonPrimitive(rule.id)); put("weekdayMask", rule.weekdayMask?.let(::JsonPrimitive) ?: JsonNull)
        put("timeStart", rule.timeStart?.let(::JsonPrimitive) ?: JsonNull); put("timeEnd", rule.timeEnd?.let(::JsonPrimitive) ?: JsonNull)
        put("holidayKind", rule.holidayKind?.let(::JsonPrimitive) ?: JsonNull)
        put("dateStart", rule.dateRange?.startDate?.let(::JsonPrimitive) ?: JsonNull); put("dateEnd", rule.dateRange?.endDate?.let(::JsonPrimitive) ?: JsonNull)
        put("when", rule.`when`?.let(::conditionToJson) ?: JsonNull)
    }) }
}

internal fun windowRulesFromJson(value: JsonElement): List<QuickCreateWindowRule>? = runCatching {
    value.jsonArray.map { element ->
        val item = element.jsonObject
        fun string(name: String) = item[name]?.jsonPrimitive?.content?.takeUnless { it == "null" }
        QuickCreateWindowRule(string("id") ?: UUID.randomUUID().toString(), string("weekdayMask")?.toIntOrNull(), string("timeStart"), string("timeEnd"), string("holidayKind")?.toIntOrNull(), string("dateStart")?.let { QuickCreateDateRange(it, string("dateEnd").orEmpty()) }, item["when"]?.let(::conditionFromJson))
    }
}.getOrNull()

@Composable
private fun JsonEditor(label: String, value: JsonElement, tag: String? = null, onValidValue: (JsonElement) -> Unit) {
    val encoded = Json.encodeToString(JsonElement.serializer(), value)
    var text by remember(encoded) { mutableStateOf(encoded) }
    FormFieldLayout(icon = Icons.Outlined.TextFields) {
        OutlinedTextField(
            value = text,
            onValueChange = { candidate ->
                text = candidate
                runCatching { Json.parseToJsonElement(candidate) }.getOrNull()?.let(onValidValue)
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().then(if (tag == null) Modifier else Modifier.testTag(tag)),
        )
    }
}
