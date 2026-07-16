package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: state-holder
import androidx.compose.material3.ListItemDefaults
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.core.designsystem.component.NiaOutlinedButton
import app.tastile.android.core.designsystem.component.NiaSegmentedButton
import app.tastile.android.core.designsystem.component.NiaSegmentedButtonDefaults
import app.tastile.android.core.designsystem.component.NiaSingleChoiceSegmentedButtonRow
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
    val draft by store.state.collectAsStateWithLifecycle()
    val active = draft.activePanel
    if (active != null && active != QuickCreatePanel.Base) {
        QuickCreateSubpanel(active, draft, store, store::backToBase, projects, knownTags)
    } else {
        QuickCreateBaseComposition(draft, store, isSubmitting, submitError, projects)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCreateBaseComposition(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    isSubmitting: Boolean,
    submitError: String?,
    projects: List<QuickCreateProject>,
) {
    val projectName = projects.firstOrNull { it.id == draft.meta.ownerSubjectId }?.displayName
        ?: draft.meta.ownerSubjectId
    Column(
        Modifier
            .testTag("quick-create-base")
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EditableTitleField(
            value = draft.identity.title,
            onValueChange = { newTitle ->
                store.updateIdentity(draft.identity.copy(title = newTitle))
            },
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().testTag("quick-create-organize-row"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (projectName != null) {
                AssistChip(
                    onClick = { store.openSubpanel(QuickCreatePanel.Meta) },
                    label = { Text(projectName) },
                )
            }
            draft.meta.tags.forEach { tag ->
                AssistChip(
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
            Modifier
                .fillMaxWidth()
                .testTag("quick-create-tile-kind")
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NiaSingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                QuickCreateTileKind.entries.forEachIndexed { index, kind ->
                    NiaSegmentedButton(
                        selected = draft.identity.kind == kind,
                        onClick = { store.updateIdentity(draft.identity.copy(kind = kind)) },
                        shape = NiaSegmentedButtonDefaults.itemShape(
                            index = index,
                            count = QuickCreateTileKind.entries.size,
                        ),
                        modifier = Modifier.testTag("quick-create-kind-${kind.name}"),
                        label = { Text(kind.name) },
                    )
                }
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
        NiaListItem(
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
        NiaListItem(
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
                NiaListItem(
                    headlineContent = { Text(task.content.title.ifBlank { "Untitled" }) },
                    leadingContent = { Icon(Icons.Outlined.CheckBox, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* tap to focus task */ },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                    NiaFilledTonalButton(
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
        NiaListItem(
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
        if (isSubmitting) {
            Text(
                text = "Creating…",
                modifier = Modifier.testTag("quick-create-submitting"),
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
    NiaListItem(
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

/**
 * Underline-style title input: tap to focus → primary-colored underline
 * appears; loses focus → fades to a faint on-surface divider. No enclosing
 * box (no rounded corners, no fill, no outline border), per the QuickCreate
 * redesign brief.
 */
@Composable
internal fun EditableTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = typography.titleLarge.copy(
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            cursorBrush = SolidColor(colors.primary),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = "Tile title",
                        style = typography.titleLarge,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    innerTextField()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-title"),
        )
        HorizontalDivider(
            thickness = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) colors.primary
            else colors.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
