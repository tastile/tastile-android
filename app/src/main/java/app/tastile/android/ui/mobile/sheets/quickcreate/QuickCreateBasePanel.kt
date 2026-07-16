package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaOutlinedButton
import app.tastile.android.core.designsystem.component.NiaTextButton
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
        Column {
            Text(
                text = "Create tile",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            HorizontalDivider()
        }
        OutlinedTextField(
            draft.identity.title,
            { store.updateIdentity(draft.identity.copy(title = it)) },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth().testTag("quick-create-title"),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().testTag("quick-create-organize-row"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (projectName != null) {
                FilterChip(
                    selected = true,
                    onClick = { store.openSubpanel(QuickCreatePanel.Meta) },
                    label = { Text(projectName) },
                )
            }
            draft.meta.tags.forEach { tag ->
                FilterChip(
                    selected = true,
                    onClick = { store.openSubpanel(QuickCreatePanel.Meta) },
                    label = { Text("#$tag") },
                )
            }
            NiaOutlinedButton(
                onClick = { store.openSubpanel(QuickCreatePanel.Meta) },
                modifier = Modifier.testTag("quick-create-organize"),
                text = { Text("Organize") },
                leadingIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
            )
        }
        Column(
            Modifier.fillMaxWidth().testTag("quick-create-tile-kind"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            QuickCreateTileKind.entries.forEach { kind ->
                ListItem(
                    headlineContent = { Text(kind.name) },
                    trailingContent = if (draft.identity.kind == kind) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { store.updateIdentity(draft.identity.copy(kind = kind)) }
                        .testTag("quick-create-kind-${kind.name}"),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        }
        HorizontalDivider()
        EssentialRow(
            label = "Time",
            summary = timeSummary(draft),
            tag = "quick-create-essential-time",
            leadingIcon = Icons.Outlined.Schedule,
            onClick = { store.openSubpanel(QuickCreatePanel.Time) },
        )
        EssentialRow(
            label = "Duration",
            summary = durationSummary(draft),
            tag = "quick-create-essential-duration",
            leadingIcon = Icons.Outlined.Timer,
            onClick = { store.openSubpanel(QuickCreatePanel.Duration) },
        )
        EssentialRow(
            label = "Repeat",
            summary = repeatSummary(draft),
            tag = "quick-create-essential-repeat",
            leadingIcon = Icons.Outlined.Repeat,
            onClick = { store.openSubpanel(QuickCreatePanel.Recurring) },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Completion logic") },
            supportingContent = { Text(conditionSummary(draft.plan.completion.root.kind)) },
            leadingContent = { Icon(Icons.Outlined.Checklist, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { store.openSubpanel(QuickCreatePanel.Completion) }
                .testTag("quick-create-condition-card"),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
        NiaOutlinedButton(
            onClick = { store.openSubpanel(QuickCreatePanel.Intent) },
            modifier = Modifier.testTag("quick-create-condition-add"),
            text = { Text("Add condition or group") },
            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
        )
        ListItem(
            headlineContent = { Text("Completion requires") },
            supportingContent = { Text("${draft.plan.completion.tasks.size} item(s)") },
            leadingContent = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { store.openSubpanel(QuickCreatePanel.Completion) }
                .testTag("quick-create-tasks-header"),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
        draft.plan.completion.tasks.forEachIndexed { index, task ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag("quick-create-task-row-$index"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ListItem(
                    headlineContent = { Text(task.content.title.ifBlank { "Untitled" }) },
                    leadingContent = { Icon(Icons.Outlined.CheckBox, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* tap to focus task */ },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NiaOutlinedButton(
                        onClick = {
                            if (index > 0) {
                                store.updatePlan(
                                    draft.plan.copy(
                                        completion = draft.plan.completion.copy(
                                            tasks = draft.plan.completion.tasks.swap(index, index - 1),
                                        ),
                                    ),
                                )
                            }
                        },
                        enabled = index > 0,
                        modifier = Modifier.testTag("quick-create-task-move-up-$index"),
                        text = { Text("Move up") },
                    )
                    NiaOutlinedButton(
                        onClick = {
                            if (index < draft.plan.completion.tasks.lastIndex) {
                                store.updatePlan(
                                    draft.plan.copy(
                                        completion = draft.plan.completion.copy(
                                            tasks = draft.plan.completion.tasks.swap(index, index + 1),
                                        ),
                                    ),
                                )
                            }
                        },
                        enabled = index < draft.plan.completion.tasks.lastIndex,
                        modifier = Modifier.testTag("quick-create-task-move-down-$index"),
                        text = { Text("Move down") },
                    )
                    FilledTonalButton(
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
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Text("Remove")
                    }
                }
            }
        }
        NiaOutlinedButton(
            onClick = {
                store.updatePlan(
                    draft.plan.copy(
                        completion = draft.plan.completion.copy(
                            tasks = draft.plan.completion.tasks +
                                QuickCreateTaskDefinition(
                                    UUID.randomUUID().toString(),
                                    QuickCreateTaskContent(""),
                                ),
                        ),
                    ),
                )
            },
            modifier = Modifier.testTag("quick-create-add-task"),
            text = { Text("Add task") },
            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Behavior") },
            supportingContent = {
                Text(if (draft.plan.role.name == "Label") "Label" else "Executable")
            },
            leadingContent = { Icon(Icons.Outlined.Tune, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { store.openSubpanel(QuickCreatePanel.Meta) }
                .testTag("quick-create-behavior-card"),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
        NiaOutlinedButton(
            onClick = { store.openSubpanel(QuickCreatePanel.References) },
            modifier = Modifier.testTag("quick-create-references-link"),
            text = { Text("References") },
            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
        )
        val submissionValidation = quickCreateSubmissionValidation(draft)
        if (!submissionValidation.isValid) {
            Text(
                submissionValidation.message ?: "Fix required fields",
                Modifier.testTag("quick-create-validation-error"),
            )
        }
        submitError?.let { Text(it, Modifier.testTag("quick-create-submit-error")) }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NiaTextButton(
                onClick = onClose,
                enabled = !isSubmitting,
                text = { Text("Cancel") },
                leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
            )
            NiaButton(
                onClick = { onSubmit(draft) },
                enabled = submissionValidation.isValid && !isSubmitting,
                modifier = Modifier.testTag("quick-create-submit"),
                text = { Text(if (isSubmitting) "Creating…" else "Create") },
                leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
            )
        }
    }
}

@Composable
internal fun BackHeader(onBack: () -> Unit) {
    NiaTextButton(
        onClick = onBack,
        text = { Text("Back") },
        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) },
    )
}

@Composable
private fun EssentialRow(
    label: String,
    summary: String,
    tag: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(leadingIcon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

private fun timeSummary(draft: QuickCreateDraftState): String = when {
    draft.time.whenMode.name == "None" -> "Not set"
    draft.time.whenMode.name == "Reference" -> "Reference range"
    draft.time.span.start.isNotBlank() -> draft.time.span.start
    else -> "Not set"
}

private fun durationSummary(draft: QuickCreateDraftState): String =
    draft.time.durationMinMax.minMs?.div(60_000)?.let { "$it min" } ?: "Not set"

private fun repeatSummary(draft: QuickCreateDraftState): String =
    if (draft.recurring.repeatMode.name == "Once") "Not set" else draft.recurring.repeatMode.name

private fun conditionSummary(kind: Int): String = when (kind) {
    0 -> "ALL"
    1 -> "ANY"
    2 -> "NOT"
    else -> "ALL"
}

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
    return QuickCreateValidation(
        draft.identity.title.isNotBlank() &&
            validSpan &&
            (duration.minMs == null || duration.maxMs == null || duration.minMs <= duration.maxMs),
    )
}

private fun String.parseOffsetDateTimeOrNull(): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(this) }.getOrNull()