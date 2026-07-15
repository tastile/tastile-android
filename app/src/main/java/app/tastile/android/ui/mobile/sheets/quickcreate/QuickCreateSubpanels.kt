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
    OutlinedTextField(draft.time.span.start, { store.updateTime(draft.time.copy(span = draft.time.span.copy(start = it))) }, label = { Text("Start") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(draft.time.span.end, { store.updateTime(draft.time.copy(span = draft.time.span.copy(end = it))) }, label = { Text("End") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(draft.time.timeOfDayStart, { store.updateTime(draft.time.copy(timeOfDayStart = it)) }, label = { Text("Time of day start") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(draft.time.timeOfDayEnd, { store.updateTime(draft.time.copy(timeOfDayEnd = it)) }, label = { Text("Time of day end") }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun DurationPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val duration = draft.time.durationMinMax
    OutlinedTextField(duration.minMs?.toString().orEmpty(), { value -> store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(value.toLongOrNull(), duration.maxMs))) }, label = { Text("Minimum duration (ms)") })
    OutlinedTextField(duration.maxMs?.toString().orEmpty(), { value -> store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(duration.minMs, value.toLongOrNull()))) }, label = { Text("Maximum duration (ms)") })
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
    }
}

@Composable
private fun ReferencesPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    Text("${draft.plan.references.size} reference(s) attached")
    Text("References are retained in the v1 draft and submitted by the command workflow.")
}

@Composable
private fun CompletionPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    OutlinedTextField(draft.plan.completion.tasks.firstOrNull()?.content?.title.orEmpty(), { value ->
        val tasks = draft.plan.completion.tasks
        if (tasks.isNotEmpty()) store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = tasks.toMutableList().also { it[0] = it[0].copy(content = it[0].content.copy(title = value)) })))
    }, label = { Text("Completion task") })
    Text("Condition tree and time requirements are preserved as v1 structures.")
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
