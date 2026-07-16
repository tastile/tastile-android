package app.tastile.android.ui.mobile.panels.projects

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tastile.android.data.api.Workspace
import app.tastile.android.ui.designsystem.AppCorner
import app.tastile.android.ui.designsystem.AppSpacing

/**
 * Single workspace row with a long-press-revealed delete × button.
 *
 * Web parity: `<button data-testid="project-select-{id}">` (select) +
 * `<button data-testid="project-delete-{id}">` (delete, `group-hover:visible`
 * on web, `long-press` on Android).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectRow(
    workspace: Workspace,
    depth: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var revealDelete by remember(workspace.id) { mutableStateOf(false) }
    val bg = if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorner.mediumShape)
            .background(bg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { revealDelete = true },
            )
            .padding(start = (AppSpacing.sm + AppSpacing.sm * depth), end = AppSpacing.xs, top = AppSpacing.sm, bottom = AppSpacing.sm)
            .testTag("project-select-${workspace.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dotColor = parseHexColor(workspace.color) ?: MaterialTheme.colorScheme.outline
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Box(Modifier.size(AppSpacing.sm))
        Text(
            text = workspace.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (revealDelete) {
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp).testTag("project-edit-${workspace.id}")) {
                Text("Edit", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("project-delete-${workspace.id}"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val s = hex.removePrefix("#")
    return runCatching {
        val v = s.toLong(16)
        when (s.length) {
            6 -> Color(0xFF000000 or v)
            8 -> Color(v)
            else -> null
        }
    }.getOrNull()
}
