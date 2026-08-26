package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
import app.tastile.android.core.designsystem.theme.LocalTastileCardRoleTokens
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.mobile.sheets.WORKFLOW_CONFIG
import app.tastile.android.ui.mobile.sheets.WORKFLOW_ORDER
import app.tastile.android.ui.mobile.sheets.WorkflowKind

/**
 * Workflow picker chip row.
 *
 * A thin wrapper around [ScrollableChipRow] that emits one chip per
 * [WorkflowKind] in [WORKFLOW_ORDER]. Chips keep their natural sizing
 * (12dp horizontal padding, 18dp icon, 8dp gap); the row scrolls
 * horizontally when the four chips exceed the available width, and the
 * 56dp leading reservation in [ScrollableChipRow] keeps the first
 * chip's left edge aligned with the body's content start.
 *
 * The host panel can hand in a [scrollState] that lives *above* the
 * per-panel composition (see `QuickCreatePanelContent`). Sharing one
 * scroll state across the Event / Task / Recurring / Detailed panels
 * means tapping a workflow chip does not reset the batch's horizontal
 * position — the row keeps its offset instead of jumping back to the
 * left edge.
 */
@Composable
fun WorkflowBatch(
    workflow: WorkflowKind,
    onWorkflowChange: (WorkflowKind) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null,
) {
    ScrollableChipRow(
        modifier = modifier.testTag("workflow-batch"),
        scrollState = scrollState ?: remember { ScrollState(0) },
    ) {
        WORKFLOW_ORDER.forEach { kind ->
            WorkflowBatchChip(
                kind = kind,
                selected = kind == workflow,
                onClick = { onWorkflowChange(kind) },
            )
        }
    }
}

@Composable
private fun WorkflowBatchChip(
    kind: WorkflowKind,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val config = WORKFLOW_CONFIG[kind] ?: return
    val label = stringResource(config.labelResId)
    val containerColor = if (selected) LocalTastileCardRoleTokens.current.actionable.container else LocalTastileCardRoleTokens.current.neutral.container
    val contentColor = if (selected) LocalContentColor.current else LocalContentColor.current
    val border = if (selected) null else BorderStroke(1.dp, LocalTastileCardRoleTokens.current.completed.border)

    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("workflow-batch-chip-$kind"),
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
        border = border,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = iconFor(kind),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

fun iconFor(workflow: WorkflowKind): ImageVector = when (workflow) {
    WorkflowKind.Event -> Icons.Outlined.CalendarMonth
    WorkflowKind.Task -> Icons.Outlined.CheckCircle
    WorkflowKind.Recurring -> Icons.Outlined.Repeat
    WorkflowKind.Detailed -> Icons.Outlined.Layers
}
