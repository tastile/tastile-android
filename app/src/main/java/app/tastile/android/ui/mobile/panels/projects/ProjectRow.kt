package app.tastile.android.ui.mobile.panels.projects

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.data.api.Workspace
import app.tastile.android.ui.mobile.designsystem.AppListItem
import app.tastile.android.ui.mobile.designsystem.MobileSpacing

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
    val startIndent = MobileSpacing.sm + MobileSpacing.sm * depth
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { revealDelete = true },
            )
            .testTag("project-select-${workspace.id}"),
    ) {
        AppListItem(
            headline = workspace.displayName,
            leading = Icons.Outlined.Folder,
            selected = selected,
            modifier = Modifier.fillMaxWidth(),
        )
        if (revealDelete) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = MobileSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(MobileSpacing.xxs),
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp).testTag("project-edit-${workspace.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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