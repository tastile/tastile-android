package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateDurationRange
import app.tastile.android.ui.mobile.sheets.QuickCreateIntent
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreatePlanRole
import app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateTileKind
import app.tastile.android.ui.mobile.sheets.QuickCreatePlanReference
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskContent
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskDefinition
import app.tastile.android.ui.mobile.sheets.QuickCreateTimeRequirement
import app.tastile.android.ui.mobile.sheets.QuickCreateTimeOfDayMode
import app.tastile.android.ui.mobile.sheets.QuickCreateWhenMode
import app.tastile.android.ui.mobile.sheets.QuickCreateWindow
import app.tastile.android.ui.mobile.sheets.QuickCreateFrameGenerator
import app.tastile.android.ui.mobile.sheets.QuickCreateFrameRule
import app.tastile.android.ui.mobile.sheets.QuickCreateRecurringRule
import app.tastile.android.ui.mobile.sheets.QuickCreateConditionNode
import app.tastile.android.ui.mobile.sheets.QuickCreateWindowRule
import app.tastile.android.ui.mobile.sheets.QuickCreateDateRange
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.LocalDate
import java.util.UUID

@Composable
internal fun QuickCreateSubpanel(
    panel: QuickCreatePanel,
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    onBack: () -> Unit,
) {
    Column(
        Modifier.testTag("quick-create-subpanel-${panel.name}").verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
    ) {
        BackHeader(onBack)
        Text(panel.name)
        when (panel) {
            QuickCreatePanel.Intent -> IntentPanel(draft, store)
            QuickCreatePanel.Time -> TimePanel(draft, store)
            QuickCreatePanel.Duration -> DurationPanel(draft, store)
            QuickCreatePanel.Recurring -> RecurringPanel(draft, store)
            QuickCreatePanel.References -> ReferencesPanel(draft, store)
            QuickCreatePanel.Completion -> CompletionPanel(draft, store)
            QuickCreatePanel.Meta -> MetaPanel(draft, store)
            QuickCreatePanel.Behavior -> BehaviorPanel(draft, store)
            QuickCreatePanel.Base -> Unit
        }
    }
}

@Composable
private fun IntentPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    Text("How should this tile be planned?")
    Row { QuickCreateIntent.entries.forEach { value -> TextButton(onClick = { store.updatePlan(draft.plan.copy(intent = value)) }) { Text(value.name) } } }
}

@Composable
private fun TimePanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    fun setWhen(mode: QuickCreateWhenMode) {
        val time = draft.time
        store.updateTime(when (mode) {
            QuickCreateWhenMode.None -> time.copy(whenMode = mode, span = app.tastile.android.ui.mobile.sheets.QuickCreateSpan(), timeOfDayMode = QuickCreateTimeOfDayMode.Unspecified, timeOfDayStart = "", timeOfDayEnd = "")
            QuickCreateWhenMode.Day -> time.copy(whenMode = mode, span = time.span.copy(end = ""))
            QuickCreateWhenMode.Range -> time.copy(whenMode = mode)
            QuickCreateWhenMode.Reference -> time.copy(whenMode = mode, span = app.tastile.android.ui.mobile.sheets.QuickCreateSpan())
        })
    }
    TextButton(onClick = { setWhen(QuickCreateWhenMode.None) }, modifier = Modifier.fillMaxWidth().testTag("quick-create-when-none")) { Text("No date or time") }
    Text("When")
    Row { listOf(QuickCreateWhenMode.Day, QuickCreateWhenMode.Range, QuickCreateWhenMode.Reference).forEach { mode -> TextButton(onClick = { setWhen(mode) }, modifier = Modifier.testTag("quick-create-when-${mode.name.lowercase()}")) { Text(mode.name) } } }
    if (draft.time.whenMode == QuickCreateWhenMode.Day || draft.time.whenMode == QuickCreateWhenMode.Range) Column(Modifier.testTag("quick-create-calendar")) {
        OutlinedTextField(draft.time.span.start, { value -> store.updateTime(draft.time.copy(span = draft.time.span.copy(start = value))) }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-start"))
        if (draft.time.whenMode == QuickCreateWhenMode.Range) OutlinedTextField(draft.time.span.end, { value -> store.updateTime(draft.time.copy(span = draft.time.span.copy(end = value))) }, label = { Text("End date") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-end"))
    }
    if (draft.time.whenMode == QuickCreateWhenMode.Reference) Column(Modifier.testTag("quick-create-reference-catalog")) {
        Text("Reference range")
        OutlinedTextField(draft.time.referenceId.orEmpty(), { value -> store.updateTime(draft.time.copy(referenceId = value.ifBlank { null })) }, label = { Text("Reference ID") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-reference-id"))
        OutlinedTextField(draft.time.referenceLabel, { value -> store.updateTime(draft.time.copy(referenceLabel = value)) }, label = { Text("Reference label") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-reference-label"))
    }
    if (draft.time.whenMode != QuickCreateWhenMode.None) {
        Text("Time of day")
        Row { QuickCreateTimeOfDayMode.entries.forEach { mode -> TextButton(onClick = {
            store.updateTime(if (mode == QuickCreateTimeOfDayMode.Range) draft.time.copy(timeOfDayMode = mode, timeOfDayStart = draft.time.timeOfDayStart.ifBlank { "09:00" }, timeOfDayEnd = draft.time.timeOfDayEnd.ifBlank { "18:00" }) else draft.time.copy(timeOfDayMode = mode, timeOfDayStart = "", timeOfDayEnd = ""))
        }, modifier = Modifier.testTag("quick-create-time-of-day-${mode.name.lowercase()}")) { Text(mode.name) } } }
        if (draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.Range) {
            OutlinedTextField(draft.time.timeOfDayStart, { value -> store.updateTime(draft.time.copy(timeOfDayStart = value)) }, label = { Text("Start time") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-time-of-day-start"))
            OutlinedTextField(draft.time.timeOfDayEnd, { value -> store.updateTime(draft.time.copy(timeOfDayEnd = value)) }, label = { Text("End time") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-time-of-day-end"))
            Row { listOf("morning" to ("06:00" to "10:00"), "midday" to ("09:00" to "18:00"), "night" to ("18:00" to "24:00")).forEach { (label, range) -> TextButton(onClick = { store.updateTime(draft.time.copy(timeOfDayMode = QuickCreateTimeOfDayMode.Range, timeOfDayStart = range.first, timeOfDayEnd = range.second)) }, modifier = Modifier.testTag("quick-create-time-quick-$label")) { Text(label) } } }
        }
    }
    TextButton(onClick = { store.updateWindows(draft.windows + QuickCreateWindow(UUID.randomUUID().toString(), "self", 0, app.tastile.android.ui.mobile.sheets.QuickCreateSpan())) }, modifier = Modifier.testTag("quick-create-add-window")) { Text("Add window") }
    draft.windows.forEachIndexed { index, window ->
        Text("Window ${index + 1}")
        Row { listOf(0, 1, 2, 3).forEach { kind -> TextButton(onClick = { store.updateWindows(draft.windows.replace(index, window.copy(kind = kind))) }, modifier = Modifier.testTag("quick-create-window-$index-kind-$kind")) { Text("Kind $kind") } } }
        OutlinedTextField(window.bounds.start, { value ->
            store.updateWindows(draft.windows.replace(index, window.copy(bounds = window.bounds.copy(start = value))))
        }, label = { Text("Window start") }, modifier = Modifier.testTag("quick-create-window-$index-start"))
        OutlinedTextField(window.bounds.end, { value ->
            store.updateWindows(draft.windows.replace(index, window.copy(bounds = window.bounds.copy(end = value))))
        }, label = { Text("Window end") }, modifier = Modifier.testTag("quick-create-window-$index-end"))
        if (window.kind in 1..3) OutlinedTextField(window.referenceId.orEmpty(), { value -> store.updateWindows(draft.windows.replace(index, window.copy(referenceId = value.ifBlank { null }))) }, label = { Text("Window reference") }, modifier = Modifier.testTag("quick-create-window-$index-reference"))
        TextButton(onClick = { store.updateWindows(draft.windows.filterIndexed { item, _ -> item != index }) }) { Text("Remove window") }
    }
}

@Composable
private fun DurationPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val duration = draft.time.durationMinMax
    TextButton(onClick = { store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange())) }, modifier = Modifier.fillMaxWidth().testTag("quick-create-duration-none")) { Text("No duration") }
    OutlinedTextField(
        value = duration.minMs?.div(60_000L)?.toString() ?: "90",
        onValueChange = { value -> value.toLongOrNull()?.let { minutes ->
            val milliseconds = minutes.coerceIn(10L, 720L) * 60_000L
            store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(milliseconds, milliseconds)))
        } },
        label = { Text("Duration (minutes)") },
        modifier = Modifier.fillMaxWidth().testTag("quick-create-duration-minutes"),
    )
    Text("Use for completion", modifier = Modifier.testTag("quick-create-duration-completion-link"))
}

@Composable
private fun RecurringPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    Column(Modifier.testTag("quick-create-recurring-controls")) {
        Row { QuickCreateRepeatMode.entries.forEach { value -> TextButton(onClick = {
            store.updateRecurring(draft.recurring.copy(repeatMode = value))
            if (value != QuickCreateRepeatMode.Once) store.updateIdentity(draft.identity.copy(kind = QuickCreateTileKind.Recurring))
        }, modifier = Modifier.testTag("quick-create-repeat-${value.name.lowercase()}")) { Text(value.name) } } }
        Text(if (draft.recurring.repeatMode == QuickCreateRepeatMode.Weekly) "Weekdays" else "Weekdays (weekly only)")
        Row { (0..6).forEach { bit -> TextButton(onClick = { store.updateRecurring(draft.recurring.copy(weekdayMask = draft.recurring.weekdayMask xor (1 shl bit))) }, enabled = draft.recurring.repeatMode == QuickCreateRepeatMode.Weekly, modifier = Modifier.testTag("quick-create-weekday-$bit")) { Text(bit.toString()) } } }
        TextButton(onClick = { store.updateRecurring(draft.recurring.copy(endDate = if (draft.recurring.endDate.isBlank()) LocalDate.now().toString() else "")) }, modifier = Modifier.testTag("quick-create-recurring-end-switch")) { Text("End date") }
        if (draft.recurring.endDate.isNotBlank()) OutlinedTextField(draft.recurring.endDate, { value -> store.updateRecurring(draft.recurring.copy(endDate = value)) }, label = { Text("End date") }, modifier = Modifier.testTag("quick-create-recurring-end-date"))
    }
}

@Composable
private fun ReferencesPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    TextButton(onClick = { store.updatePlan(draft.plan.copy(references = draft.plan.references + QuickCreatePlanReference(UUID.randomUUID().toString(), JsonNull, JsonNull))) }, modifier = Modifier.testTag("quick-create-add-reference")) { Text("Add reference") }
    draft.plan.references.forEachIndexed { index, reference ->
        val target = reference.target.jsonObjectOrEmpty()
        val pick = reference.pick.jsonObjectOrEmpty()
        OutlinedTextField(target.string("referenceId"), { value -> updateReference(draft, store, index, reference.copy(target = target.with("referenceId", value.ifBlank { null }))) }, label = { Text("Target") }, modifier = Modifier.testTag("quick-create-reference-id-$index"))
        Text("Relation")
        Row { listOf(4, 3, 1, 2, 0).forEach { relation -> TextButton(onClick = { updateReference(draft, store, index, reference.copy(pick = pick.with("kind", relation))) }) { Text(relation.toString()) } } }
        OutlinedTextField(pick.string("momentId", "10"), { value -> value.toIntOrNull()?.coerceIn(5, 120)?.let { minutes -> updateReference(draft, store, index, reference.copy(pick = pick.with("momentId", minutes.toString()))) } }, label = { Text("Interval (minutes)") })
        TextButton(onClick = { store.updatePlan(draft.plan.copy(references = draft.plan.references.filterIndexed { item, _ -> item != index })) }) { Text("Remove reference") }
    }
}

@Composable
private fun CompletionPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    Text("Logic")
    Row { listOf(0 to "ALL", 1 to "ANY", 2 to "NOT").forEach { (kind, label) -> TextButton(onClick = { store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = draft.plan.completion.root.copy(kind = kind, term = null)))) }) { Text(label) } } }
    ConditionControls(draft.plan.completion.root, onChange = { root -> store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = root))) })
    TextButton(
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
        modifier = Modifier.testTag("quick-create-add-time-requirement"),
    ) { Text("Add time requirement") }
    draft.plan.completion.timeRequirements.forEachIndexed { index, requirement ->
        val required = requirement.required.jsonObjectOrEmpty()
        val minimumMinutes = required.long("minMs")?.div(60_000L)?.toString().orEmpty()
        OutlinedTextField(
            value = minimumMinutes,
            onValueChange = { input ->
                val minutes = input.toLongOrNull()
                val nextRequired = when {
                    input.isBlank() -> required.with("minMs", null)
                    minutes == null -> required
                    else -> required.with("minMs", minutes.coerceAtLeast(5L) * 60_000L)
                }
                updateTimeRequirement(draft, store, index, requirement.copy(required = nextRequired))
            },
            label = { Text("Minutes") },
            modifier = Modifier.testTag("time-requirement-$index-required-minutes"),
        )
        Text("minutes")
        TextButton(
            onClick = {
                store.updatePlan(
                    draft.plan.copy(
                        completion = draft.plan.completion.copy(
                            timeRequirements = draft.plan.completion.timeRequirements.filterIndexed { item, _ -> item != index },
                        ),
                    ),
                )
            },
            modifier = Modifier.testTag("time-requirement-$index-remove"),
        ) { Text("Remove time requirement") }
    }
}

private fun webTimeRequirement(durationMinimumMs: Long?): QuickCreateTimeRequirement = QuickCreateTimeRequirement(
    id = UUID.randomUUID().toString(),
    observation = JsonObject(mapOf("scope" to JsonPrimitive(0))),
    required = JsonObject(mapOf("minMs" to JsonPrimitive(durationMinimumMs ?: 60 * 60_000L))),
)

private fun updateTimeRequirement(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    index: Int,
    requirement: QuickCreateTimeRequirement,
) {
    store.updatePlan(
        draft.plan.copy(
            completion = draft.plan.completion.copy(
                timeRequirements = draft.plan.completion.timeRequirements.replace(index, requirement),
            ),
        ),
    )
}

@Composable private fun ConditionControls(node: QuickCreateConditionNode, onChange: (QuickCreateConditionNode) -> Unit, path: String = "root") {
    Row { listOf(0 to "ALL", 1 to "ANY", 2 to "NOT", 3 to "TERM").forEach { (kind, label) -> TextButton(onClick = { onChange(node.copy(kind = kind, children = if (kind == 3) emptyList() else node.children, term = if (kind == 3) defaultTermValue("calendar") else null)) }) { Text(label) } } }
    if (node.kind == 3) {
        Row { listOf("calendar", "moment", "relation", "gap", "requirement", "task", "fact", "metric", "life").forEach { type -> TextButton(onClick = { onChange(node.copy(term = defaultTermValue(type))) }, modifier = Modifier.testTag("condition-$path-term-$type")) { Text(type) } } }
        val term = node.term?.jsonObjectOrEmpty() ?: JsonObject(emptyMap())
        when (term.string("kind")) {
            "calendar" -> CalendarTermFields(term, path) { value -> onChange(node.copy(term = value)) }
            "moment" -> MomentTermFields(term, path) { value -> onChange(node.copy(term = value)) }
            "relation" -> RelationTermFields(term) { value -> onChange(node.copy(term = value)) }
            "task" -> TaskTermFields(term) { value -> onChange(node.copy(term = value)) }
            "gap" -> GapTermFields(path)
            "requirement" -> RequirementTermFields(term, path) { value -> onChange(node.copy(term = value)) }
            "fact" -> ScalarTermFields(term, path, "fact", "factId") { value -> onChange(node.copy(term = value)) }
            "metric" -> ScalarTermFields(term, path, "metric", "metricId") { value -> onChange(node.copy(term = value)) }
            "life" -> LifeTermFields(term, path) { value -> onChange(node.copy(term = value)) }
        }
    }
    else { node.children.forEachIndexed { index, child -> ConditionControls(child, { updated -> onChange(node.copy(children = node.children.replace(index, updated))) }, "$path-$index"); TextButton(onClick = { onChange(node.copy(children = node.children.filterIndexed { item, _ -> item != index })) }) { Text("Remove") } }; TextButton(onClick = { onChange(node.copy(children = node.children + QuickCreateConditionNode(3, term = defaultTermValue("calendar")))) }) { Text("Add condition") } }
}

@Composable private fun CalendarTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("weekdayMask", "0"), { input -> onChange(term.withValue("weekdayMask", input.toIntOrNull() ?: 0)) }, label = { Text("Weekday mask") }, modifier = Modifier.testTag("condition-$path-calendar-weekday-mask"))
    OutlinedTextField(value.string("offsetMin", "0"), { input -> onChange(term.withValue("offsetMin", input.toIntOrNull() ?: 0)) }, label = { Text("Offset minutes") }, modifier = Modifier.testTag("condition-$path-calendar-offset"))
    OutlinedTextField(value.string("timeStart"), { input -> onChange(term.withValue("timeStart", input.ifBlank { null })) }, label = { Text("Time start") }, modifier = Modifier.testTag("condition-calendar-start"))
    OutlinedTextField(value.string("timeEnd"), { input -> onChange(term.withValue("timeEnd", input.ifBlank { null })) }, label = { Text("Time end") }, modifier = Modifier.testTag("condition-calendar-end"))
}

@Composable private fun MomentTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("referenceId"), { input -> onChange(term.withValue("referenceId", input.ifBlank { null })) }, label = { Text("Reference ID") }, modifier = Modifier.testTag("condition-moment-reference"))
    OutlinedTextField(value.string("offsetMs", "0"), { input -> onChange(term.withValue("offsetMs", input.toLongOrNull() ?: 0L)) }, label = { Text("Offset ms") }, modifier = Modifier.testTag("condition-moment-offset"))
}

@Composable private fun RelationTermFields(term: JsonObject, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("referenceId"), { input -> onChange(term.withValue("referenceId", input)) }, label = { Text("Reference ID") }, modifier = Modifier.testTag("condition-relation-reference"))
    OutlinedTextField(value.string("relation", "0"), { input -> onChange(term.withValue("relation", input.toIntOrNull() ?: 0)) }, label = { Text("Relation") }, modifier = Modifier.testTag("condition-relation-kind"))
    OutlinedTextField(value.string("windowKind", "0"), { input -> onChange(term.withValue("windowKind", input.toIntOrNull() ?: 0)) }, label = { Text("Window kind") }, modifier = Modifier.testTag("condition-relation-window"))
}

@Composable private fun TaskTermFields(term: JsonObject, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("taskId"), { input -> onChange(term.withValue("taskId", input)) }, label = { Text("Task ID") }, modifier = Modifier.testTag("condition-task-id"))
    OutlinedTextField(value.string("state", "0"), { input -> onChange(term.withValue("state", input.toIntOrNull() ?: 0)) }, label = { Text("State") }, modifier = Modifier.testTag("condition-task-state"))
}

@Composable private fun GapTermFields(path: String) {
    // Current Web TermFields intentionally exposes Gap as an informational placeholder only.
    Text("Gap configuration is preserved when supplied by the API.", modifier = Modifier.testTag("condition-$path-gap-placeholder"))
}

@Composable private fun RequirementTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("requirementId"), { input ->
        onChange(term.withValue("requirementId", input))
    }, label = { Text("Requirement ID") }, modifier = Modifier.testTag("condition-$path-requirement-id"))
    OutlinedTextField(value.string("state", "0"), { input ->
        onChange(term.withValue("state", input.toIntOrNull() ?: 0))
    }, label = { Text("State") }, modifier = Modifier.testTag("condition-$path-requirement-state"))
}

@Composable private fun ScalarTermFields(
    term: JsonObject,
    path: String,
    kind: String,
    idKey: String,
    onChange: (JsonObject) -> Unit,
) {
    val value = term.valueObject()
    OutlinedTextField(value.string(idKey), { input ->
        onChange(term.withValue(idKey, input))
    }, label = { Text("ID") }, modifier = Modifier.testTag("condition-$path-$kind-id"))
    OutlinedTextField(value.string("op", "0"), { input ->
        onChange(term.withValue("op", input.toIntOrNull() ?: 0))
    }, label = { Text("Operator") }, modifier = Modifier.testTag("condition-$path-$kind-op"))
    OutlinedTextField(value.string("value"), { input ->
        onChange(term.withValue("value", scalarValue(input)))
    }, label = { Text("Value") }, modifier = Modifier.testTag("condition-$path-$kind-value"))
}

@Composable private fun LifeTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("target"), { input ->
        onChange(term.withValue("target", input))
    }, label = { Text("Target") }, modifier = Modifier.testTag("condition-$path-life-target"))
    OutlinedTextField(value.string("state", "0"), { input ->
        onChange(term.withValue("state", input.toIntOrNull() ?: 0))
    }, label = { Text("State") }, modifier = Modifier.testTag("condition-$path-life-state"))
}

private fun defaultTermValue(kind: String): JsonObject = when (kind) {
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
    "life" -> termValue(kind, mapOf("target" to JsonPrimitive(""), "state" to JsonPrimitive(0)))
    "task" -> termValue(kind, mapOf("taskId" to JsonPrimitive(""), "state" to JsonPrimitive(0)))
    else -> defaultTermValue("calendar")
}

private fun termValue(kind: String, value: Map<String, JsonElement>) = JsonObject(mapOf("kind" to JsonPrimitive(kind), "value" to JsonObject(value)))
private fun JsonObject.valueObject(): JsonObject = this["value"] as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.withValue(key: String, value: Any?): JsonObject = with("value", valueObject().with(key, value))
private fun scalarValue(input: String): Any? = when {
    input.isBlank() -> null
    input.toLongOrNull() != null -> input.toLong()
    input.toDoubleOrNull()?.isFinite() == true -> input.toDouble()
    else -> input
}

@Composable
private fun MetaPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    OutlinedTextField(draft.meta.ownerSubjectId.orEmpty(), { value -> store.updateMeta(draft.meta.copy(ownerSubjectId = value.ifBlank { null })) }, label = { Text("Owner subject ID") })
    OutlinedTextField(draft.meta.tags.joinToString(", "), { value -> store.updateMeta(draft.meta.copy(tags = value.split(',').map(String::trim).filter(String::isNotBlank))) }, label = { Text("Tags") })
    OutlinedTextField(draft.meta.memo, { value -> store.updateMeta(draft.meta.copy(memo = value)) }, label = { Text("Memo") })
}

@Composable
private fun BehaviorPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    Row { QuickCreatePlanRole.entries.forEach { value -> TextButton(onClick = { store.updateBehavior(value) }) { Text(value.name) } } }
}

@Composable
private fun JsonEditor(label: String, value: JsonElement, tag: String? = null, onValidValue: (JsonElement) -> Unit) {
    val encoded = Json.encodeToString(JsonElement.serializer(), value)
    var text by remember(encoded) { mutableStateOf(encoded) }
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

private fun <T> List<T>.replace(index: Int, value: T): List<T> = toMutableList().also { it[index] = value }

private fun JsonElement.jsonObjectOrEmpty(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.string(key: String, fallback: String = ""): String = this[key]?.jsonPrimitive?.content?.takeUnless { it == "null" } ?: fallback
private fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
private fun JsonObject.with(key: String, value: Any?): JsonObject = JsonObject(toMutableMap().also { map -> map[key] = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is Int -> JsonPrimitive(value)
    is Long -> JsonPrimitive(value)
    is Double -> JsonPrimitive(value)
    else -> JsonPrimitive(value.toString())
} })
private fun updateReference(draft: QuickCreateDraftState, store: QuickCreateStateStore, index: Int, reference: QuickCreatePlanReference) = store.updatePlan(draft.plan.copy(references = draft.plan.references.replace(index, reference)))

private fun updateTask(draft: QuickCreateDraftState, store: QuickCreateStateStore, index: Int, task: QuickCreateTaskDefinition) {
    store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks.replace(index, task))))
}

private fun conditionToJson(condition: QuickCreateConditionNode): JsonElement = buildJsonObject {
    put("kind", JsonPrimitive(condition.kind))
    put("children", buildJsonArray { condition.children.forEach { add(conditionToJson(it)) } })
    put("term", condition.term ?: JsonNull)
}

private fun conditionFromJson(value: JsonElement): QuickCreateConditionNode? = runCatching {
    val objectValue = value.jsonObject
    QuickCreateConditionNode(
        kind = objectValue.getValue("kind").jsonPrimitive.content.toInt(),
        children = objectValue["children"]?.jsonArray?.mapNotNull(::conditionFromJson) ?: emptyList(),
        term = objectValue["term"].takeUnless { it == null || it is JsonNull },
    )
}.getOrNull()

private fun windowRulesToJson(rules: List<QuickCreateWindowRule>): JsonElement = buildJsonArray {
    rules.forEach { rule -> add(buildJsonObject {
        put("id", JsonPrimitive(rule.id)); put("weekdayMask", rule.weekdayMask?.let(::JsonPrimitive) ?: JsonNull)
        put("timeStart", rule.timeStart?.let(::JsonPrimitive) ?: JsonNull); put("timeEnd", rule.timeEnd?.let(::JsonPrimitive) ?: JsonNull)
        put("holidayKind", rule.holidayKind?.let(::JsonPrimitive) ?: JsonNull)
        put("dateStart", rule.dateRange?.startDate?.let(::JsonPrimitive) ?: JsonNull); put("dateEnd", rule.dateRange?.endDate?.let(::JsonPrimitive) ?: JsonNull)
        put("when", rule.`when`?.let(::conditionToJson) ?: JsonNull)
    }) }
}

private fun windowRulesFromJson(value: JsonElement): List<QuickCreateWindowRule>? = runCatching {
    value.jsonArray.map { element ->
        val item = element.jsonObject
        fun string(name: String) = item[name]?.jsonPrimitive?.content?.takeUnless { it == "null" }
        QuickCreateWindowRule(string("id") ?: UUID.randomUUID().toString(), string("weekdayMask")?.toIntOrNull(), string("timeStart"), string("timeEnd"), string("holidayKind")?.toIntOrNull(), string("dateStart")?.let { QuickCreateDateRange(it, string("dateEnd").orEmpty()) }, item["when"]?.let(::conditionFromJson))
    }
}.getOrNull()
