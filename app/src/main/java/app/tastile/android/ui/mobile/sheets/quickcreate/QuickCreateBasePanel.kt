package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.material3.FilterChip
// m2-allow: m3-component
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.core.designsystem.component.NiaLoadingWheel
import app.tastile.android.core.designsystem.theme.PanelTokens
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreatePlanRole
import app.tastile.android.ui.mobile.sheets.QuickCreateProject
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import java.time.OffsetDateTime

/** Mobile Quick Create content. Set [keepBaseVisible] when the outer sheet hosts subpanels. */
@Composable
fun QuickCreatePanelContent(
    store: QuickCreateStateStore,
    onClose: () -> Unit,
    projects: List<QuickCreateProject> = emptyList(),
    knownTags: List<String> = emptyList(),
    isSubmitting: Boolean = false,
    submitError: String? = null,
    keepBaseVisible: Boolean = false,
) {
    val draft by store.state.collectAsStateWithLifecycle()
    val active = draft.activePanel
    if (!keepBaseVisible && active != null && active != QuickCreatePanel.Base) {
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
            .fillMaxHeight()
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PanelTokens.LeadingColumnWidth, end = 16.dp)
                .testTag("quick-create-organize-row"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (projectName != null) {
                FilterChip(
                    selected = false,
                    onClick = { store.openSubpanel(QuickCreatePanel.Meta) },
                    label = { Text(projectName) },
                )
            }
            draft.meta.tags.forEach { tag ->
                FilterChip(
                    selected = false,
                    onClick = { store.openSubpanel(QuickCreatePanel.Meta) },
                    label = { Text("#$tag") },
                )
            }
            FilterChip(
                selected = false,
                onClick = { store.openSubpanel(QuickCreatePanel.Meta) },
                label = { Text("Organize") },
                leadingIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                modifier = Modifier.testTag("quick-create-organize"),
            )
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
        HorizontalDivider()
        NiaListItem(
            content = { Text("Completion logic") },
            supportingContent = { Text(conditionSummary(draft.plan.completion.root.kind)) },
            leadingContent = { Icon(Icons.Outlined.Checklist, contentDescription = null) },
            trailingContent = { Icon(Icons.Outlined.Add, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { store.openSubpanel(QuickCreatePanel.Completion) }
                .testTag("quick-create-condition-card"),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        NiaListItem(
            content = { Text("Completion requires") },
            supportingContent = { Text("${draft.plan.completion.tasks.size} item(s)") },
            leadingContent = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
            trailingContent = { Icon(Icons.Outlined.Add, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { store.openSubpanel(QuickCreatePanel.Completion) }
                .testTag("quick-create-tasks-header"),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        draft.plan.completion.tasks.forEachIndexed { index, task ->
            NiaListItem(
                content = { Text(task.content.title.ifBlank { "Untitled" }) },
                leadingContent = { Icon(Icons.Outlined.CheckBox, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* tap to focus task */ }
                    .testTag("quick-create-task-row-$index"),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
        HorizontalDivider()
        val isLabel = draft.plan.role == QuickCreatePlanRole.Label
        NiaListItem(
            content = { Text("Label") },
            supportingContent = {
                Text(if (isLabel) "Markers only" else "Executable when scheduled")
            },
            leadingContent = { Icon(Icons.Outlined.Flag, contentDescription = null) },
            trailingContent = {
                // m2-allow: m3-component
                Switch(
                    checked = isLabel,
                    onCheckedChange = { on ->
                        store.updateRole(if (on) QuickCreatePlanRole.Label else QuickCreatePlanRole.Executable)
                    },
                    modifier = Modifier.testTag("quick-create-label-toggle"),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-behavior-card"),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        NiaListItem(
            content = { Text("References") },
            leadingContent = { Icon(Icons.Outlined.Link, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { store.openSubpanel(QuickCreatePanel.References) }
                .testTag("quick-create-references-link"),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        val submissionValidation = quickCreateSubmissionValidation(draft)
        if (!submissionValidation.isValid) {
            Text(
                submissionValidation.message ?: "Fix required fields",
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("quick-create-validation-error"),
            )
        }
        submitError?.let {
            Text(
                it,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("quick-create-submit-error"),
            )
        }
        if (isSubmitting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("quick-create-submitting"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NiaLoadingWheel(
                    contentDesc = "Creating",
                    wheelSize = 20.dp,
                )
                Text(text = "Creating…")
            }
        }
    }
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
        content = { Text(label) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(leadingIcon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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

private fun conditionSummary(kind: Int): String = when (kind) {
    0 -> "ALL"
    1 -> "ANY"
    2 -> "NOT"
    else -> "ALL"
}

private fun String.parseOffsetDateTimeOrNull(): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(this) }.getOrNull()

/**
 * Underline-style title input: tap to focus → primary-colored underline
 * appears; loses focus → fades to a faint on-surface divider. No enclosing
 * box (no rounded corners, no fill, no outline border), per the QuickCreate
 * redesign brief.
 *
 * Text is left-aligned (matches every other row in the panel), and the field
 * requests focus as soon as it enters composition — the keyboard opens with
 * the sheet so users can start typing the tile title immediately.
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
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    LaunchedEffect(value) {
        if (value.isEmpty()) focusRequester.requestFocus()
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PanelTokens.LeadingColumnWidth, end = 16.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = typography.titleLarge.copy(
                color = colors.onSurface,
                textAlign = TextAlign.Start,
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
                        textAlign = TextAlign.Start,
                    )
                } else {
                    innerTextField()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
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
