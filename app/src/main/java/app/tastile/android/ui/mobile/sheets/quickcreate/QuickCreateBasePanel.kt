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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.mobile.designsystem.AppDismissButton
import app.tastile.android.ui.mobile.designsystem.AppListItem
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppSecondaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
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
    onSubmit: (QuickCreateDraftState) -> Unit = {},
    isSubmitting: Boolean = false,
    submitError: String? = null,
) {
    val draft by store.state.collectAsState()
    val active = draft.activePanel
    if (active != null && active != QuickCreatePanel.Base) {
        QuickCreateSubpanel(active, draft, store, store::backToBase, projects, knownTags)
    } else {
        QuickCreateBaseComposition(draft, store, onClose, onSubmit, isSubmitting, submitError, projects)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCreateBaseComposition(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    onClose: () -> Unit,
    onSubmit: (QuickCreateDraftState) -> Unit,
    isSubmitting: Boolean,
    submitError: String?,
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
            AppTertiaryButton(
                text = "Organize",
                onClick = { store.openSubpanel(QuickCreatePanel.Meta) },
                modifier = Modifier.testTag("quick-create-organize"),
                leadingIcon = Icons.Outlined.Tune,
            )
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
        AppListItem(
            headline = "Completion logic",
            supporting = conditionSummary(draft.plan.completion.root.kind),
            leading = Icons.Outlined.Checklist,
            trailing = Icons.Outlined.ChevronRight,
            onClick = { store.openSubpanel(QuickCreatePanel.Completion) },
            modifier = Modifier.testTag("quick-create-condition-card"),
        )
        AppTertiaryButton(
            text = "Add condition or group",
            onClick = { store.openSubpanel(QuickCreatePanel.Intent) },
            modifier = Modifier.testTag("quick-create-condition-add"),
            leadingIcon = Icons.Outlined.Add,
        )
        AppListItem(
            headline = "Completion requires",
            supporting = "${draft.plan.completion.tasks.size} item(s)",
            leading = Icons.Outlined.PlayArrow,
            trailing = Icons.Outlined.ChevronRight,
            onClick = { store.openSubpanel(QuickCreatePanel.Completion) },
            modifier = Modifier.testTag("quick-create-tasks-header"),
        )
        draft.plan.completion.tasks.forEachIndexed { index, task ->
            Row(Modifier.fillMaxWidth().testTag("quick-create-task-row-$index"), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AppListItem(
                    headline = task.content.title.ifBlank { "Untitled" },
                    leading = Icons.Outlined.CheckBox,
                    modifier = Modifier.weight(1f),
                    onClick = { /* tap to focus task */ },
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppTertiaryButton(
                        text = "Move up",
                        onClick = {
                            if (index > 0) store.updatePlan(
                                draft.plan.copy(
                                    completion = draft.plan.completion.copy(
                                        tasks = draft.plan.completion.tasks.swap(index, index - 1),
                                    ),
                                ),
                            )
                        },
                        enabled = index > 0,
                        modifier = Modifier.testTag("quick-create-task-move-up-$index"),
                    )
                    AppTertiaryButton(
                        text = "Move down",
                        onClick = {
                            if (index < draft.plan.completion.tasks.lastIndex) store.updatePlan(
                                draft.plan.copy(
                                    completion = draft.plan.completion.copy(
                                        tasks = draft.plan.completion.tasks.swap(index, index + 1),
                                    ),
                                ),
                            )
                        },
                        enabled = index < draft.plan.completion.tasks.lastIndex,
                        modifier = Modifier.testTag("quick-create-task-move-down-$index"),
                    )
                }
                AppSecondaryButton(
                    text = "Remove",
                    onClick = {
                        store.updatePlan(
                            draft.plan.copy(
                                completion = draft.plan.completion.copy(
                                    tasks = draft.plan.completion.tasks.filterIndexed { item, _ -> item != index },
                                ),
                            ),
                        )
                    },
                    modifier = Modifier.testTag("quick-create-task-remove-$index"),
                    leadingIcon = Icons.Outlined.Delete,
                )
            }
        }
        AppTertiaryButton(
            text = "Add task",
            onClick = {
                store.updatePlan(
                    draft.plan.copy(
                        completion = draft.plan.completion.copy(
                            tasks = draft.plan.completion.tasks +
                                QuickCreateTaskDefinition(UUID.randomUUID().toString(), QuickCreateTaskContent("")),
                        ),
                    ),
                )
            },
            modifier = Modifier.testTag("quick-create-add-task"),
            leadingIcon = Icons.Outlined.Add,
        )
        HorizontalDivider()
        AppListItem(
            headline = "Behavior",
            supporting = if (draft.plan.role.name == "Label") "Label" else "Executable",
            leading = Icons.Outlined.Tune,
            trailing = Icons.Outlined.ChevronRight,
            onClick = { store.openSubpanel(QuickCreatePanel.Meta) },
            modifier = Modifier.testTag("quick-create-behavior-card"),
        )
        AppTertiaryButton(
            text = "References",
            onClick = { store.openSubpanel(QuickCreatePanel.References) },
            modifier = Modifier.testTag("quick-create-references-link"),
            leadingIcon = Icons.Outlined.Link,
        )
        val submissionValidation = quickCreateSubmissionValidation(draft)
        if (!submissionValidation.isValid) Text(submissionValidation.message ?: "Fix required fields", Modifier.testTag("quick-create-validation-error"))
        submitError?.let { Text(it, Modifier.testTag("quick-create-submit-error")) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTertiaryButton(
                text = "Cancel",
                onClick = onClose,
                enabled = !isSubmitting,
                leadingIcon = Icons.Outlined.Close,
            )
            AppPrimaryButton(
                text = if (isSubmitting) "Creating…" else "Create",
                onClick = { onSubmit(draft) },
                enabled = submissionValidation.isValid && !isSubmitting,
                modifier = Modifier.testTag("quick-create-submit"),
                leadingIcon = Icons.Outlined.Check,
            )
        }
    }
}

@Composable
internal fun BackHeader(onBack: () -> Unit) {
    AppDismissButton(
        text = "Back",
        onClick = onBack,
        leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
    )
}

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
private fun conditionSummary(kind: Int): String = when (kind) { 0 -> "ALL"; 1 -> "ANY"; 2 -> "NOT"; else -> "ALL" }
private fun <T> List<T>.swap(first: Int, second: Int): List<T> = toMutableList().also { items ->
    val item = items[first]
    items[first] = items[second]
    items[second] = item
}

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
