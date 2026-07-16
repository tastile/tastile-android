package app.tastile.android.ui.mobile.panels.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.projectLabel
import app.tastile.android.ui.designsystem.AppCorner
import app.tastile.android.ui.designsystem.AppSpacing
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.designsystem.AppEmptyState
import app.tastile.android.ui.mobile.designsystem.SectionHeader

/**
 * "Projects" filter section reused by both [TimelineSectionContent]
 * (C10) and [ScheduleSectionContent] (C11). Mirrors the web
 * `ProjectsCheckboxSection` from the right-pane panels
 * (`tastile-web/src/components/panels/ScheduleSidePanel.tsx`).
 *
 * Each workspace label extracted from [Tile.projectLabel] gets a
 * checkbox; the checked set lives in this composable's local
 * `remember` state (mobile analog of the web's local component
 * state — filtering downstream is intentionally not wired here).
 */
@Composable
fun ProjectsCheckboxSection(tiles: List<Tile>) {
    val projects = remember(tiles) { extractProjects(tiles) }
    val checked = remember { mutableStateOf(setOf<String>()) }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        SectionHeader(title = stringResource(R.string.panels_schedule_projects))
        if (projects.isEmpty()) {
            AppEmptyState(
                icon = Icons.Outlined.FolderOff,
                title = stringResource(R.string.empty_projects_title),
                hint = stringResource(R.string.empty_projects_hint),
            )
            return@Column
        }
        projects.forEach { name ->
            val isChecked = name in checked.value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppCorner.mediumShape)
                    .clickable {
                        checked.value = if (isChecked) checked.value - name else checked.value + name
                    }
                    .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs + AppSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { newVal ->
                        checked.value = if (newVal) checked.value + name else checked.value - name
                    },
                )
                Text(
                    text = name,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurface,
                )
            }
        }
    }
}

private fun extractProjects(tiles: List<Tile>): List<String> =
    tiles.asSequence()
        .mapNotNull { it.projectLabel() }
        .distinct()
        .sorted()
        .toList()