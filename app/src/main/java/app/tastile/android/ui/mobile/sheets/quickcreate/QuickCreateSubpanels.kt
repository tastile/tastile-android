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
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    OutlinedTextField(draft.time.span.start, { store.updateTime(draft.time.copy(span = draft.time.span.copy(start = it))) }, label = { Text("Start") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-start"))
    OutlinedTextField(draft.time.span.end, { store.updateTime(draft.time.copy(span = draft.time.span.copy(end = it))) }, label = { Text("End") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-end"))
    Text("When")
    Row { app.tastile.android.ui.mobile.sheets.QuickCreateWhenMode.entries.forEach { mode -> TextButton(onClick = { store.updateTime(draft.time.copy(whenMode = mode)) }) { Text(mode.name) } } }
    Text("Time of day")
    Row { app.tastile.android.ui.mobile.sheets.QuickCreateTimeOfDayMode.entries.forEach { mode -> TextButton(onClick = { store.updateTime(draft.time.copy(timeOfDayMode = mode)) }) { Text(mode.name) } } }
    OutlinedTextField(draft.time.timeOfDayStart, { store.updateTime(draft.time.copy(timeOfDayStart = it)) }, label = { Text("Time of day start") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(draft.time.timeOfDayEnd, { store.updateTime(draft.time.copy(timeOfDayEnd = it)) }, label = { Text("Time of day end") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(draft.time.referenceId.orEmpty(), { store.updateTime(draft.time.copy(referenceId = it.ifBlank { null })) }, label = { Text("Reference ID") })
    OutlinedTextField(draft.time.referenceLabel, { store.updateTime(draft.time.copy(referenceLabel = it)) }, label = { Text("Reference label") })
    TextButton(onClick = { store.updateWindows(draft.windows + QuickCreateWindow(UUID.randomUUID().toString(), "self", 0, app.tastile.android.ui.mobile.sheets.QuickCreateSpan())) }, modifier = Modifier.testTag("quick-create-add-window")) { Text("Add window") }
    draft.windows.forEachIndexed { index, window ->
        OutlinedTextField(window.id, { value -> store.updateWindows(draft.windows.replace(index, window.copy(id = value))) }, label = { Text("Window ID") }, modifier = Modifier.testTag("quick-create-window-id-$index"))
        OutlinedTextField(window.owner, { value -> store.updateWindows(draft.windows.replace(index, window.copy(owner = value))) }, label = { Text("Owner") })
        OutlinedTextField(window.kind.toString(), { value -> store.updateWindows(draft.windows.replace(index, window.copy(kind = value.toIntOrNull() ?: 0))) }, label = { Text("Kind") })
        OutlinedTextField(window.bounds.start, { value ->
            store.updateWindows(draft.windows.replace(index, window.copy(bounds = window.bounds.copy(start = value))))
        }, label = { Text("Window start") })
        OutlinedTextField(window.bounds.end, { value ->
            store.updateWindows(draft.windows.replace(index, window.copy(bounds = window.bounds.copy(end = value))))
        }, label = { Text("Window end") })
        OutlinedTextField(window.referenceId.orEmpty(), { value -> store.updateWindows(draft.windows.replace(index, window.copy(referenceId = value.ifBlank { null }))) }, label = { Text("Window reference") })
        TextButton(onClick = { store.updateWindows(draft.windows.filterIndexed { item, _ -> item != index }) }) { Text("Remove window") }
    }
}

@Composable
private fun DurationPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val duration = draft.time.durationMinMax
    OutlinedTextField(duration.minMs?.toString().orEmpty(), { value -> store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(value.toLongOrNull(), duration.maxMs))) }, label = { Text("Minimum duration (ms)") }, modifier = Modifier.testTag("quick-create-duration-min"))
    OutlinedTextField(duration.maxMs?.toString().orEmpty(), { value -> store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(duration.minMs, value.toLongOrNull()))) }, label = { Text("Maximum duration (ms)") }, modifier = Modifier.testTag("quick-create-duration-max"))
}

@Composable
private fun RecurringPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    if (draft.identity.kind != QuickCreateTileKind.Recurring) {
        Text("Choose Recurring in Base to configure repetition.")
        return
    }
    Column(Modifier.testTag("quick-create-recurring-controls")) {
        Row { QuickCreateRepeatMode.entries.forEach { value -> TextButton(onClick = { store.updateRecurring(draft.recurring.copy(repeatMode = value)) }) { Text(value.name) } } }
        OutlinedTextField(draft.recurring.weekdayMask.toString(), { value -> store.updateRecurring(draft.recurring.copy(weekdayMask = value.toIntOrNull() ?: 0)) }, label = { Text("Weekday mask") })
        OutlinedTextField(draft.recurring.endDate, { value -> store.updateRecurring(draft.recurring.copy(endDate = value)) }, label = { Text("End date") })
        OutlinedTextField(draft.recurring.life.active.startDate, { value -> store.updateRecurring(draft.recurring.copy(life = draft.recurring.life.copy(active = draft.recurring.life.active.copy(startDate = value)))) }, label = { Text("Active from") })
        OutlinedTextField(draft.recurring.life.active.endDate, { value -> store.updateRecurring(draft.recurring.copy(life = draft.recurring.life.copy(active = draft.recurring.life.active.copy(endDate = value)))) }, label = { Text("Active until") })
        if (draft.recurring.frameRules.isNotEmpty() || draft.recurring.rules.isNotEmpty()) Text("Advanced recurrence details are preserved.")
    }
}

@Composable
private fun ReferencesPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    TextButton(onClick = { store.updatePlan(draft.plan.copy(references = draft.plan.references + QuickCreatePlanReference(UUID.randomUUID().toString(), JsonNull, JsonNull))) }, modifier = Modifier.testTag("quick-create-add-reference")) { Text("Add reference") }
    draft.plan.references.forEachIndexed { index, reference ->
        OutlinedTextField(reference.id, { value -> store.updatePlan(draft.plan.copy(references = draft.plan.references.replace(index, reference.copy(id = value)))) }, label = { Text("Reference ID") }, modifier = Modifier.testTag("quick-create-reference-id-$index"))
        JsonEditor("Reference target", reference.target) { value -> store.updatePlan(draft.plan.copy(references = draft.plan.references.replace(index, reference.copy(target = value)))) }
        JsonEditor("Reference pick", reference.pick) { value -> store.updatePlan(draft.plan.copy(references = draft.plan.references.replace(index, reference.copy(pick = value)))) }
        TextButton(onClick = { store.updatePlan(draft.plan.copy(references = draft.plan.references.filterIndexed { item, _ -> item != index })) }) { Text("Remove reference") }
    }
}

@Composable
private fun CompletionPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    JsonEditor("Completion root condition", conditionToJson(draft.plan.completion.root)) { value ->
        conditionFromJson(value)?.let { root -> store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = root))) }
    }
    TextButton(onClick = { store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(timeRequirements = draft.plan.completion.timeRequirements + QuickCreateTimeRequirement(UUID.randomUUID().toString(), JsonNull, JsonNull)))) }) { Text("Add time requirement") }
    draft.plan.completion.timeRequirements.forEachIndexed { index, requirement ->
        OutlinedTextField(requirement.id, { value ->
            store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(timeRequirements = draft.plan.completion.timeRequirements.replace(index, requirement.copy(id = value)))))
        }, label = { Text("Requirement ID") })
        JsonEditor("Observation", requirement.observation) { value -> store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(timeRequirements = draft.plan.completion.timeRequirements.replace(index, requirement.copy(observation = value)))) ) }
        JsonEditor("Required", requirement.required) { value -> store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(timeRequirements = draft.plan.completion.timeRequirements.replace(index, requirement.copy(required = value)))) ) }
        JsonEditor("Preferred", requirement.preferred ?: JsonNull) { value -> store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(timeRequirements = draft.plan.completion.timeRequirements.replace(index, requirement.copy(preferred = value)))) ) }
        TextButton(onClick = { store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(timeRequirements = draft.plan.completion.timeRequirements.filterIndexed { item, _ -> item != index }))) }) { Text("Remove time requirement") }
    }
    TextButton(onClick = {
        store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks + QuickCreateTaskDefinition(UUID.randomUUID().toString(), QuickCreateTaskContent("")))))
    }) { Text("Add completion task") }
    draft.plan.completion.tasks.forEachIndexed { index, task ->
        OutlinedTextField(task.id, { value -> updateTask(draft, store, index, task.copy(id = value)) }, label = { Text("Task ID") })
        OutlinedTextField(task.content.title, { value -> updateTask(draft, store, index, task.copy(content = task.content.copy(title = value))) }, label = { Text("Task title") })
        OutlinedTextField(task.content.note.orEmpty(), { value -> updateTask(draft, store, index, task.copy(content = task.content.copy(note = value.ifBlank { null }))) }, label = { Text("Task note") })
        JsonEditor("Show", task.show ?: JsonNull, "quick-create-task-show-$index") { value -> updateTask(draft, store, index, task.copy(show = value)) }
        JsonEditor("Complete condition", conditionToJson(task.complete), "quick-create-task-complete-$index") { value -> conditionFromJson(value)?.let { complete -> updateTask(draft, store, index, task.copy(complete = complete)) } }
        JsonEditor("Order", task.order, "quick-create-task-order-$index") { value -> (value as? kotlinx.serialization.json.JsonArray)?.let { order -> updateTask(draft, store, index, task.copy(order = order)) } }
        TextButton(onClick = { store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks.filterIndexed { item, _ -> item != index }))) }) { Text("Remove completion task") }
    }
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
