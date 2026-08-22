package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.MobileSpacing
import app.tastile.android.data.model.Workspace
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.panels.ProjectsViewModel

private val MobSpacingXs get() = MobileSpacing.xs
private val MobSpacingMd get() = MobileSpacing.md
private const val COLOR_DOT_SIZE_DP = 10
private const val PERSONAL_KIND: Int = 0

/**
 * Mobile Projects tab — single list of workspaces with Personal
 * first (client-synthesised), then each WORKSPACE fetched via
 * `GET /v1/access/subjects?kind=1`. Tapping a row selects it as the
 * owner scope for the rest of the app; the dashboard tiles refresh
 * via `?owner_ids=<id>`.
 */
@Composable
fun ProjectsScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
) {
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val selectedOwnerId by projectsViewModel.selectedOwnerId.collectAsStateWithLifecycle()

    if (workspaces.isEmpty()) {
        EmptyProjectsMessage()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("projects-screen-body"),
        contentPadding = PaddingValues(vertical = MobSpacingMd),
        verticalArrangement = Arrangement.spacedBy(MobSpacingXs),
    ) {
        items(
            items = workspaces,
            key = { ws -> "projects-row-${ws.id}" },
            contentType = { "project-row" },
        ) { workspace ->
            ProjectListRow(
                workspace = workspace,
                selected = workspace.id == selectedOwnerId,
                onSelect = { projectsViewModel.selectOwner(workspace.id) },
            )
        }
    }
}

@Composable
private fun ProjectListRow(
    workspace: Workspace,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val rowBg = if (selected) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.Transparent
    }
    val titleText = if (workspace.kind == PERSONAL_KIND) {
        stringResource(R.string.projects_personal_title)
    } else {
        workspace.displayName.ifBlank { workspace.id.take(8) }
    }
    val subtitleText = when (workspace.kind) {
        PERSONAL_KIND -> stringResource(R.string.projects_personal_scope)
        else -> workspace.slug?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.projects_owner_prefix, workspace.id.take(8))
    }
    val dotColor = parseHexColor(workspace.color) ?: Color(0xFF6B7280)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onSelect)
            .padding(horizontal = MobSpacingMd, vertical = MobSpacingMd)
            .testTag("projects-row-${workspace.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MobSpacingMd),
    ) {
        Box(
            modifier = Modifier
                .size(COLOR_DOT_SIZE_DP.dp)
                .background(dotColor, CircleShape)
                .testTag("projects-row-dot-${workspace.id}"),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyProjectsMessage() {
    Box(
        modifier = Modifier.fillMaxSize().testTag("projects-screen-body"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_projects_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Parse a hex color string like `"#FF0000"` or `"aabbcc"`. Returns
 * null when the input is null, blank, or unparseable so callers can
 * fall back to a neutral chip color.
 */
private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val cleaned = hex.removePrefix("#").trim()
    if (cleaned.length != 6 && cleaned.length != 8) return null
    return runCatching {
        val parsed = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(0xFF000000L or parsed)
        } else {
            Color(parsed)
        }
    }.getOrNull()
}
