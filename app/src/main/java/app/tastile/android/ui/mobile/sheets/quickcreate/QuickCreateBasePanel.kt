package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreatePlan
import app.tastile.android.ui.mobile.sheets.QuickCreateProject
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskContent
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskDefinition
import app.tastile.android.ui.mobile.sheets.QuickCreateTileKind
import java.time.OffsetDateTime
import java.util.UUID

/** Mobile rendering of the same base composition as Web, with detail panels routed as sheets. */
@Composable
fun QuickCreatePanelContent(
    store: QuickCreateStateStore,
    onClose: () -> Unit,
    projects: List<QuickCreateProject> = emptyList(),
    knownTags: List<String> = emptyList(),
) {
    val draft by store.state.collectAsState()
    val active = draft.activePanel
    if (active != null && active != QuickCreatePanel.Base) {
        QuickCreateSubpanel(active, draft, store, store::backToBase, projects, knownTags)
    } else {
        QuickCreateBaseComposition(draft, store, onClose, projects)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCreateBaseComposition(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    onClose: () -> Unit,
    projects: List<QuickCreateProject>,
) {
    val projectName = projects.firstOrNull { it.id == draft.meta.ownerSubjectId }?.displayName
        ?: draft.meta.ownerSubjectId
    Column(
        Modifier.testTag("quick-create-base").verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Create tile")
        OutlinedTextField(
            draft.identity.title,
            { store.updateIdentity(draft.identity.copy(title = it)) },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth().testTag("quick-create-title"),
        )
        FlowRow(Modifier.testTag("quick-create-organize-row"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (projectName != null) FilterChip(true, { store.openSubpanel(QuickCreatePanel.Meta) }, { Text(projectName) })
            draft.meta.tags.forEach { tag -> FilterChip(true, { store.openSubpanel(QuickCreatePanel.Meta) }, { Text("#$tag") }) }
            TextButton({ store.openSubpanel(QuickCreatePanel.Meta) }, Modifier.testTag("quick-create-organize")) { Text("Organize") }
        }
        Row(Modifier.testTag("quick-create-tile-kind"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickCreateTileKind.entries.forEach { kind ->
                FilterChip(draft.identity.kind == kind, { store.updateIdentity(draft.identity.copy(kind = kind)) }, { Text(kind.name) }, Modifier.testTag("quick-create-kind-${kind.name}"))
            }
        }
        HorizontalDivider()
        EssentialRow("Time", timeSummary(draft), "quick-create-essential-time") { store.openSubpanel(QuickCreatePanel.Time) }
        EssentialRow("Duration", durationSummary(draft), "quick-create-essential-duration") { store.openSubpanel(QuickCreatePanel.Duration) }
        EssentialRow("Repeat", repeatSummary(draft), "quick-create-essential-repeat") { store.openSubpanel(QuickCreatePanel.Recurring) }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().clickable { store.openSubpanel(QuickCreatePanel.Completion) }.testTag("quick-create-tasks-header"), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Completion requires")
            Text("${draft.plan.completion.tasks.size} item(s)")
        }
        draft.plan.completion.tasks.forEachIndexed { index, task ->
            Row(Modifier.fillMaxWidth().testTag("quick-create-task-row-$index"), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(task.content.title.ifBlank { "Untitled" })
                TextButton({ store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks.filterIndexed { item, _ -> item != index }))) }) { Text("Remove") }
            }
        }
        TextButton(
            { store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks + QuickCreateTaskDefinition(UUID.randomUUID().toString(), QuickCreateTaskContent(""))))) },
            Modifier.testTag("quick-create-add-task"),
        ) { Text("Add task") }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().clickable { store.openSubpanel(QuickCreatePanel.Behavior) }.testTag("quick-create-behavior-card"), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Behavior"); Text(if (draft.plan.role.name == "Label") "Label" else "Executable") }
            Text("Edit")
        }
        TextButton({ store.openSubpanel(QuickCreatePanel.References) }, Modifier.testTag("quick-create-references-link")) { Text("References") }
        if (!quickCreateValidation(draft).isValid) Text("Fix required fields", Modifier.testTag("quick-create-validation-error"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onClose) { Text("Cancel") }
            Button(onClick = { }, enabled = quickCreateValidation(draft).isValid) { Text("Create") }
        }
    }
}

@Composable
internal fun BackHeader(onBack: () -> Unit) { TextButton(onClick = onBack) { Text("Back") } }

@Composable private fun EssentialRow(label: String, summary: String, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(summary) }
}

private fun timeSummary(draft: QuickCreateDraftState): String = when {
    draft.time.whenMode.name == "None" -> "Not set"
    draft.time.whenMode.name == "Reference" -> "Reference range"
    draft.time.span.start.isNotBlank() -> draft.time.span.start
    else -> "Not set"
}
private fun durationSummary(draft: QuickCreateDraftState): String = draft.time.durationMinMax.minMs?.div(60_000)?.let { "$it min" } ?: "Not set"
private fun repeatSummary(draft: QuickCreateDraftState): String = if (draft.recurring.repeatMode.name == "Once") "Not set" else draft.recurring.repeatMode.name

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
    return QuickCreateValidation(draft.identity.title.isNotBlank() && validSpan && (duration.minMs == null || duration.maxMs == null || duration.minMs <= duration.maxMs))
}
private fun String.parseOffsetDateTimeOrNull(): OffsetDateTime? = runCatching { OffsetDateTime.parse(this) }.getOrNull()
