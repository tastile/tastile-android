package app.tastile.android.ui.mobile.sheets

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.dueAtDate
import app.tastile.android.data.model.isRecurring
import app.tastile.android.data.model.projectLabel
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.designsystem.AppCorner
import app.tastile.android.ui.designsystem.AppListRow
import app.tastile.android.ui.designsystem.AppSpacing
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.SidePanelSection
import app.tastile.android.ui.mobile.designsystem.MobileTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Section-aware right-pane content for [SidePanelSheet]. The page title lives
 * in the outer [PanelSheet] header (it switches with the pager page); this
 * composable renders only the section body. Back navigation is via swipe on
 * the pager, not a back button.
 */
@Composable
internal fun SectionPanelContent(
    section: SidePanelSection,
    dashboardViewModel: DashboardViewModel,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (section) {
            SidePanelSection.Calendar -> TimelineSectionContent(dashboardViewModel)
            SidePanelSection.Schedule -> ScheduleSectionContent(dashboardViewModel)
            SidePanelSection.Projects -> ProjectsSectionContent(dashboardViewModel)
            SidePanelSection.References -> ReferencesSectionContent()
            SidePanelSection.Preferences -> PreferencesSectionContent(dashboardViewModel)
        }
    }
}

// ─────────────────────────────────────────────
// Timeline: mini month calendar + scale + project checks
// ─────────────────────────────────────────────
@Composable
private fun TimelineSectionContent(viewModel: DashboardViewModel) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val scale by viewModel.scale.collectAsStateWithLifecycle()
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        MiniMonthCalendar(
            selected = selectedDay,
            onSelect = { viewModel.setSelectedDay(it) },
        )
        ScalePicker(
            current = scale,
            onSelect = { viewModel.setScale(it) },
        )
        ProjectsCheckboxSection(tiles = tiles)
    }
}

@Composable
private fun MiniMonthCalendar(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    var month by remember(selected) { mutableStateOf(YearMonth.from(selected)) }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .size(AppTheme.component.iconButton)
                    .clip(AppCorner.smallShape)
                    .clickable { month = month.minusMonths(1) }
                    .padding(AppSpacing.xs),
            )
            Text(
                text = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.year,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .size(AppTheme.component.iconButton)
                    .clip(AppCorner.smallShape)
                    .clickable { month = month.plusMonths(1) }
                    .padding(AppSpacing.xs),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    text = d,
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = AppSpacing.xs),
                )
            }
        }

        val firstDay = month.atDay(1)
        val leading = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val daysInMonth = month.lengthOfMonth()
        val cells: List<LocalDate?> = buildList {
            repeat(leading) { add(null) }
            for (day in 1..daysInMonth) add(month.atDay(day))
            while (size % 7 != 0) add(null)
        }

        val sel = selected
        val today = LocalDate.now()
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val isSel = date != null && date == sel
                    val isToday = date != null && date == today
                    val cellAlpha = when {
                        isSel -> MobileTokens.SurfaceAlpha.strongSelected
                        isToday -> MobileTokens.SurfaceAlpha.subtle
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(AppTheme.component.calendarCellSize)
                            .padding(AppSpacing.xxs)
                            .clip(AppCorner.mediumShape)
                            .background(AppTheme.colors.surfaceVariant.copy(alpha = cellAlpha))
                            .clickable(enabled = date != null) { date?.let(onSelect) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = AppTheme.typography.bodySmall,
                                fontWeight = if (isToday || isSel) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (date.month != month.month)
                                    AppTheme.colors.onSurface.copy(alpha = 0.34f)
                                else AppTheme.colors.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScalePicker(current: TimelineScale, onSelect: (TimelineScale) -> Unit) {
    val items = listOf(TimelineScale.Day, TimelineScale.Week, TimelineScale.Month)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorner.mediumShape)
            .background(
                AppTheme.colors.surfaceVariant.copy(alpha = MobileTokens.SurfaceAlpha.selected),
            )
            .padding(AppSpacing.xxs),
    ) {
        items.forEach { item ->
            val selected = item == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(AppCorner.smallShape)
                    .background(
                        if (selected) AppTheme.colors.surfaceVariant.copy(
                            alpha = MobileTokens.SurfaceAlpha.strongSelected,
                        ) else Color.Transparent,
                    )
                    .clickable { onSelect(item) }
                    .padding(vertical = AppSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.name,
                    style = AppTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Schedule, References, Preferences (real data)
// ─────────────────────────────────────────────
@Composable
private fun ScheduleSectionContent(viewModel: DashboardViewModel) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val recurring = tiles.filter { it.isRecurring() }
    val now = LocalDate.now()
    val upcoming = tiles.filter { tile ->
        tile.dueAtDate()
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { it in now..now.plusDays(7) } ?: false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Text(
            "Recurring",
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        if (recurring.isEmpty()) {
            Text("No recurring tiles", style = AppTheme.typography.bodySmall)
        } else {
            recurring.take(10).forEach { tile ->
                AppListRow(
                    label = tile.title,
                    leading = { Text("↻", style = AppTheme.typography.bodyMedium) },
                    onClick = { viewModel.selectTile(tile.id) },
                    description = "Recurring: ${tile.title}",
                )
            }
        }
        Box(Modifier.size(AppSpacing.md))
        Text(
            "Upcoming (7 days)",
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        if (upcoming.isEmpty()) {
            Text("No upcoming deadlines", style = AppTheme.typography.bodySmall)
        } else {
            upcoming.take(10).forEach { tile ->
                AppListRow(
                    label = tile.title,
                    onClick = { viewModel.selectTile(tile.id) },
                    description = "Upcoming: ${tile.title}",
                )
            }
        }
    }
}

@Composable
private fun ReferencesSectionContent() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        ReferenceLink("Help", "https://tastile.app/help", context)
        ReferenceLink("Changelog", "https://tastile.app/changelog", context)
        ReferenceLink("GitHub", "https://github.com/rebuildup/tastile", context)
        ReferenceLink("Send feedback", "https://github.com/rebuildup/tastile/issues", context)
    }
}

@Composable
private fun ReferenceLink(label: String, url: String, context: Context) {
    AppListRow(
        label = label,
        onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
        description = label,
    )
}

@Composable
private fun PreferencesSectionContent(viewModel: DashboardViewModel) {
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val lock by viewModel.securityLockEnabled.collectAsStateWithLifecycle()
    val timeout by viewModel.securityLockTimeoutMinutes.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        PreferenceSummaryRow(label = "Locale", value = locale.toString())
        PreferenceSummaryRow(label = "Theme", value = theme.toString())
        PreferenceSummaryRow(label = "Lock", value = if (lock) "On (${timeout}m)" else "Off")
        Text(
            "Open Settings tab to change.",
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreferenceSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = AppTheme.typography.bodyMedium)
        Text(
            value,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────
// Projects checkbox section (used inside Timeline pane + standalone)
// ─────────────────────────────────────────────
@Composable
internal fun ProjectsCheckboxSection(
    tiles: List<app.tastile.android.data.model.Tile>,
) {
    val projects = remember(tiles) { extractProjects(tiles) }
    val checked = remember { mutableStateOf(setOf<String>()) }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(
            text = "Projects",
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        if (projects.isEmpty()) {
            Text(
                text = "No projects yet",
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AppSpacing.xs),
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

@Composable
private fun ProjectsSectionContent(viewModel: DashboardViewModel) {
    // C5: full CRUD port — see ProjectsSectionContent.kt in ui/mobile/panels/.
    app.tastile.android.ui.mobile.panels.ProjectsSectionContent()
}

private fun extractProjects(tiles: List<app.tastile.android.data.model.Tile>): List<String> =
    tiles.asSequence()
        .mapNotNull { it.projectLabel() }
        .distinct()
        .sorted()
        .toList()