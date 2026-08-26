package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Tune
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
// m2-allow: primitive
import androidx.compose.material3.Text
import app.tastile.android.core.designsystem.theme.LocalTastileStatusTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreateProject
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.WorkflowKind

/**
 * Root dispatcher for the QuickCreate authors. Evaluates the [WorkflowKind]
 * in the draft and routes to the appropriate peer workflow panel.
 *
 * The workflow batch scroll state is hoisted here (not inside each
 * panel) so switching workflow chips keeps the batch row's horizontal
 * offset instead of resetting it — the panels swap in and out of
 * composition, but this dispatcher stays mounted for the whole session,
 * and a `remember` at this level survives those swaps.
 */
@Composable
fun QuickCreatePanelContent(
    store: QuickCreateStateStore,
    onClose: () -> Unit,
    projects: List<QuickCreateProject> = emptyList(),
    knownTags: List<String> = emptyList(),
    isSubmitting: Boolean = false,
    batchScrollState: ScrollState? = null,
) {
    val draft = store.draft
    val submitError = store.submitError
    val sharedBatchScroll = batchScrollState ?: remember { ScrollState(0) }

    when (draft.workflow) {
        WorkflowKind.Event -> QuickCreateEventPanel(draft, store, isSubmitting, submitError, projects, knownTags, sharedBatchScroll)
        WorkflowKind.Task -> QuickCreateTaskPanel(draft, store, isSubmitting, submitError, projects, knownTags, sharedBatchScroll)
        WorkflowKind.Recurring -> QuickCreateRecurringPanel(draft, store, isSubmitting, submitError, projects, knownTags, sharedBatchScroll)
        WorkflowKind.Detailed -> QuickCreateDetailedComposition(draft, store, isSubmitting, submitError, projects, sharedBatchScroll)
    }
}

/**
 * Detailed workflow — mirrors the web's peer workflow structure while
 * exposing all sub-panel affordances.
 *
 * Each row is a tappable navigation affordance that opens the
 * corresponding sub-panel; the leading icon column stays reserved via
 * [FormRow] so the body's icon track lines up with the rest of the panel.
 * Matches `tastile-web/src/features/create-tile/ui/QuickCreate.tsx` (Event /
 * Duration / Repeat / Source rules / Relations / Flows / Placement rules /
 * Completion / Project+Color / Memo).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCreateDetailedComposition(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    isSubmitting: Boolean,
    submitError: String?,
    projects: List<QuickCreateProject>,
    batchScrollState: ScrollState? = null,
) {
    FormFieldColumn(
        modifier = Modifier
            .testTag("quick-create-detailed")
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        QuickCreateHeader(
            title = draft.identity.title,
            onTitleChange = { store.updateIdentity(draft.identity.copy(title = it)) },
            modifier = Modifier.testTag("quick-create-detailed-header"),
        )

        WorkflowBatch(
            workflow = draft.workflow,
            onWorkflowChange = { kind -> store.setWorkflow(kind) },
            modifier = Modifier.testTag("quick-create-detailed-batch"),
            scrollState = batchScrollState,
        )

        DetailedRow(
            icon = Icons.Outlined.CalendarMonth,
            label = stringResource(R.string.tile_edit_open_time),
            summary = detailedTimeSummary(draft),
            onClick = { store.openSubpanel(QuickCreatePanel.Time) },
            testTag = "detailed-open-time",
        )
        DetailedRow(
            icon = Icons.Outlined.HourglassEmpty,
            label = stringResource(R.string.tile_edit_open_duration),
            summary = detailedDurationSummary(draft),
            onClick = { store.openSubpanel(QuickCreatePanel.Duration) },
            testTag = "detailed-open-duration",
        )
        DetailedRow(
            icon = Icons.Outlined.Repeat,
            label = stringResource(R.string.tile_edit_open_schedule),
            summary = detailedRepeatSummary(draft),
            onClick = { store.openSubpanel(QuickCreatePanel.Schedule) },
            testTag = "detailed-open-schedule",
        )
        DetailedRow(
            icon = Icons.Outlined.Tune,
            label = stringResource(R.string.quickcreate_detailed_subpanel_source_rules),
            summary = detailedPrioritySummary(draft),
            onClick = { store.openSubpanel(QuickCreatePanel.Meta) },
            testTag = "detailed-open-source-rules",
        )
        DetailedRow(
            icon = Icons.Outlined.Link,
            label = stringResource(R.string.quickcreate_detailed_subpanel_relations),
            summary = null,
            onClick = { store.openSubpanel(QuickCreatePanel.References) },
            testTag = "detailed-open-relations",
        )
        DetailedRow(
            icon = Icons.Outlined.Layers,
            label = stringResource(R.string.quickcreate_detailed_subpanel_flows),
            summary = null,
            onClick = { store.openSubpanel(QuickCreatePanel.PlacementRules) },
            testTag = "detailed-open-flows",
        )
        DetailedRow(
            icon = Icons.Outlined.Tune,
            label = stringResource(R.string.quickcreate_detailed_subpanel_placement_rules),
            summary = null,
            onClick = { store.openSubpanel(QuickCreatePanel.PlacementRules) },
            testTag = "detailed-open-placement-rules",
        )
        DetailedRow(
            icon = Icons.Outlined.Checklist,
            label = stringResource(R.string.quickcreate_section_completion_logic),
            summary = null,
            onClick = { store.openSubpanel(QuickCreatePanel.Completion) },
            testTag = "detailed-open-completion",
        )

        ProjectColorRow(
            projects = projects,
            selectedProjectId = draft.meta.ownerSubjectId,
            selectedColor = parseHexColor(draft.identity.visual.color),
            onProjectChange = { id -> store.updateMeta(draft.meta.copy(ownerSubjectId = id)) },
            onColorChange = { color ->
                store.updateIdentity(
                    draft.identity.copy(
                        visual = draft.identity.visual.copy(color = color.toHexString())
                    )
                )
            },
            modifier = Modifier.testTag("detailed-project-color"),
        )

        MemoSection(
            memo = draft.meta.memo,
            onMemoChange = { store.updateMeta(draft.meta.copy(memo = it)) },
        )

        submitError?.let {
            Text(
                text = it,
                color = LocalTastileStatusTokens.current.archived.icon,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun DetailedRow(
    icon: ImageVector,
    label: String,
    summary: String?,
    onClick: () -> Unit,
    testTag: String,
) {
    FormRow(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = LocalContentColor.current,
            )
        },
    )
}

private fun detailedTimeSummary(draft: QuickCreateDraftState): String? {
    val start = draft.time.span.start
    val end = draft.time.span.end
    return when {
        start.isBlank() && end.isBlank() -> null
        else -> {
            val s = start.take(10)
            val e = end.take(10)
            if (s == e) s else "$s → $e"
        }
    }
}

private fun detailedDurationSummary(draft: QuickCreateDraftState): String? {
    val minMs = draft.time.durationMinMax.minMs
    val maxMs = draft.time.durationMinMax.maxMs
    if (minMs == null && maxMs == null) return null
    val min = ((minMs ?: maxMs!!) / 60_000L).toInt()
    return if (minMs == null || maxMs == null || minMs == maxMs) {
        "${min}m"
    } else {
        "${min}m – ${(maxMs / 60_000L).toInt()}m"
    }
}

private fun detailedRepeatSummary(draft: QuickCreateDraftState): String? {
    val mode = draft.recurring.repeatMode
    return when (mode.name) {
        "Once" -> null
        "Daily" -> "Daily"
        "Weekly" -> "Weekly"
        "Interval" -> "Every ${draft.recurring.intervalValue}d"
        "Condition" -> "Conditional"
        else -> null
    }
}

@Composable
private fun detailedPrioritySummary(draft: QuickCreateDraftState): String {
    val priority = draft.schedule.priority
    val splitSuffix = if (draft.schedule.splitPolicyKind.toInt() == 1) " · split" else ""
    return stringResource(R.string.quickcreate_detailed_priority_value, priority) + splitSuffix
}
