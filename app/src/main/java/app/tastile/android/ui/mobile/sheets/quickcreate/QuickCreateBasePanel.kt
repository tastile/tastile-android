package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateTileKind
import app.tastile.android.ui.mobile.sheets.QuickCreatePlan
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskContent
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskDefinition
import java.util.UUID
import java.time.OffsetDateTime

private val panelOrder = listOf(
    QuickCreatePanel.Base,
    QuickCreatePanel.Intent,
    QuickCreatePanel.Time,
    QuickCreatePanel.Duration,
    QuickCreatePanel.Recurring,
    QuickCreatePanel.References,
    QuickCreatePanel.Completion,
    QuickCreatePanel.Meta,
    QuickCreatePanel.Behavior,
)

@Composable
fun QuickCreatePanelContent(store: QuickCreateStateStore, onClose: () -> Unit) {
    val draft by store.state.collectAsState()
    var baseDetailsOpen by remember { mutableStateOf(false) }
    val active = draft.activePanel

    when {
        baseDetailsOpen -> QuickCreateBaseDetails(
            draft = draft,
            onUpdate = store::updateIdentity,
            onUpdatePlan = store::updatePlan,
            onBack = { baseDetailsOpen = false },
        )
        active != null && active != QuickCreatePanel.Base -> QuickCreateSubpanel(
            panel = active,
            draft = draft,
            store = store,
            onBack = store::backToBase,
        )
        else -> QuickCreatePanelList(
            draft = draft,
            onPanelClick = { panel ->
                if (panel == QuickCreatePanel.Base) baseDetailsOpen = true else store.openSubpanel(panel)
            },
            onClose = onClose,
        )
    }
}

@Composable
private fun QuickCreatePanelList(
    draft: QuickCreateDraftState,
    onPanelClick: (QuickCreatePanel) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.testTag("quick-create-panel-list").verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Create tile")
        panelOrder.forEachIndexed { index, panel ->
            val summary = when (panel) {
                QuickCreatePanel.Base -> draft.identity.title.ifBlank { "Tile identity" }
                QuickCreatePanel.Intent -> draft.plan.intent.name
                QuickCreatePanel.Time -> draft.time.span.start.ifBlank { "No time span" }
                QuickCreatePanel.Duration -> durationSummary(draft)
                QuickCreatePanel.Recurring -> if (draft.identity.kind == QuickCreateTileKind.Recurring) draft.recurring.repeatMode.name else "Available for recurring tiles"
                QuickCreatePanel.References -> "${draft.plan.references.size} references"
                QuickCreatePanel.Completion -> "${draft.plan.completion.tasks.size} task(s)"
                QuickCreatePanel.Meta -> "${draft.meta.tags.size} tag(s)"
                QuickCreatePanel.Behavior -> draft.plan.role.name
            }
            Row(
                modifier = Modifier.fillMaxWidth().testTag("quick-create-row-$index").clickable { onPanelClick(panel) }.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(panel.name)
                Text(summary)
            }
            HorizontalDivider()
        }
        if (!quickCreateValidation(draft).isValid) Text("Fix required fields", Modifier.testTag("quick-create-validation-error"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onClose) { Text("Cancel") }
            Button(onClick = { }, enabled = quickCreateValidation(draft).isValid) { Text("Create") }
        }
    }
}

@Composable
private fun QuickCreateBaseDetails(
    draft: QuickCreateDraftState,
    onUpdate: (app.tastile.android.ui.mobile.sheets.QuickCreateIdentity) -> Unit,
    onUpdatePlan: (QuickCreatePlan) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.testTag("quick-create-subpanel-Base").verticalScroll(rememberScrollState())) {
        BackHeader(onBack)
        OutlinedTextField(draft.identity.title, { onUpdate(draft.identity.copy(title = it)) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-title"))
        OutlinedTextField(draft.identity.description.orEmpty(), { onUpdate(draft.identity.copy(description = it.ifBlank { null })) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(QuickCreateTileKind.Placement, QuickCreateTileKind.Recurring).forEach { kind ->
                TextButton(onClick = { onUpdate(draft.identity.copy(kind = kind)) }, modifier = Modifier.testTag("quick-create-kind-${kind.name}")) { Text(kind.name) }
            }
        }
        OutlinedTextField(draft.identity.visual.color, { onUpdate(draft.identity.copy(visual = draft.identity.visual.copy(color = it))) }, label = { Text("Color") })
        OutlinedTextField(draft.identity.visual.icon, { onUpdate(draft.identity.copy(visual = draft.identity.visual.copy(icon = it))) }, label = { Text("Icon") })
        OutlinedTextField(draft.identity.externalId.orEmpty(), { onUpdate(draft.identity.copy(externalId = it.ifBlank { null })) }, label = { Text("External ID") })
        Text("Tasks")
        draft.plan.completion.tasks.forEachIndexed { index, task ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(task.content.title.ifBlank { "Untitled" })
                TextButton(onClick = { if (index > 0) onUpdatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks.swap(index, index - 1)))) }) { Text("Up") }
                TextButton(onClick = { if (index < draft.plan.completion.tasks.lastIndex) onUpdatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks.swap(index, index + 1)))) }) { Text("Down") }
                TextButton(onClick = { onUpdatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks.filterIndexed { item, _ -> item != index }))) }) { Text("Remove") }
            }
        }
        TextButton(onClick = { onUpdatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks + QuickCreateTaskDefinition(UUID.randomUUID().toString(), QuickCreateTaskContent(""))))) }) { Text("Add task") }
    }
}

private fun durationSummary(draft: QuickCreateDraftState): String =
    "${draft.time.durationMinMax.minMs ?: "-"} – ${draft.time.durationMinMax.maxMs ?: "-"} ms"

data class QuickCreateValidation(val isValid: Boolean)

fun quickCreateValidation(draft: QuickCreateDraftState): QuickCreateValidation {
    val duration = draft.time.durationMinMax
    val start = draft.time.span.start.parseOffsetDateTimeOrNull()
    val end = draft.time.span.end.parseOffsetDateTimeOrNull()
    val validSpan = when {
        draft.time.span.start.isBlank() && draft.time.span.end.isBlank() -> true
        draft.time.span.start.isBlank() -> end != null
        draft.time.span.end.isBlank() -> start != null
        start == null || end == null -> false
        else -> end.isAfter(start)
    }
    val validDuration = duration.minMs == null || duration.maxMs == null || duration.minMs <= duration.maxMs
    return QuickCreateValidation(draft.identity.title.isNotBlank() && validSpan && validDuration)
}

@Composable
internal fun BackHeader(onBack: () -> Unit) { TextButton(onClick = onBack) { Text("Back") } }

private fun String.parseOffsetDateTimeOrNull(): OffsetDateTime? = runCatching { OffsetDateTime.parse(this) }.getOrNull()
private fun <T> List<T>.swap(first: Int, second: Int): List<T> = toMutableList().also { list -> val item = list[first]; list[first] = list[second]; list[second] = item }
