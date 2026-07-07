package app.tastile.android.ui.mobile.sheets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.mobile.SidePanelSection
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
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { month = month.minusMonths(1) }
                    .padding(4.dp),
            )
            Text(
                text = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.year,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { month = month.plusMonths(1) }
                    .padding(4.dp),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    date == sel -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    date == today -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                                    else -> Color.Transparent
                                },
                            )
                            .clickable(enabled = date != null) { date?.let(onSelect) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            val isToday = date == today
                            val isSel = date == sel
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isToday || isSel) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (date.month != month.month)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)
                                else MaterialTheme.colorScheme.onSurface,
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
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f))
            .padding(2.dp),
    ) {
        items.forEach { item ->
            val selected = item == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else Color.Transparent)
                    .clickable { onSelect(item) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
    val recurring = tiles.filter { tile ->
        tile.annotationConditions?.containsKey("recurrence") == true ||
        tile.temporalConditions?.containsKey("recurrence") == true
    }
    val now = LocalDate.now()
    val upcoming = tiles.filter { tile ->
        val dueStr = tile.temporalConditions?.get("due_at")?.toString()?.trim('"')
            ?: tile.annotationConditions?.get("due_at")?.toString()?.trim('"')
        dueStr?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { it in now..now.plusDays(7) } ?: false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Recurring",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (recurring.isEmpty()) {
            Text("No recurring tiles", style = MaterialTheme.typography.bodySmall)
        } else {
            recurring.take(10).forEach { tile ->
                Text(
                    "↻ ${tile.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectTile(tile.id) }
                        .padding(vertical = 4.dp)
                        .semantics { contentDescription = "Recurring: ${tile.title}" },
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            "Upcoming (7 days)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (upcoming.isEmpty()) {
            Text("No upcoming deadlines", style = MaterialTheme.typography.bodySmall)
        } else {
            upcoming.take(10).forEach { tile ->
                Text(
                    tile.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectTile(tile.id) }
                        .padding(vertical = 4.dp)
                        .semantics { contentDescription = "Upcoming: ${tile.title}" },
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ReferenceLink("Help", "https://tastile.app/help", context)
        ReferenceLink("Changelog", "https://tastile.app/changelog", context)
        ReferenceLink("GitHub", "https://github.com/rebuildup/tastile", context)
        ReferenceLink("Send feedback", "https://github.com/rebuildup/tastile/issues", context)
    }
}

@Composable
private fun ReferenceLink(label: String, url: String, context: Context) {
    Text(
        "$label ›",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            .padding(vertical = 8.dp)
            .semantics { contentDescription = label },
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PreferenceSummaryRow(label = "Locale", value = locale.toString())
        PreferenceSummaryRow(label = "Theme", value = theme.toString())
        PreferenceSummaryRow(label = "Lock", value = if (lock) "On (${timeout}m)" else "Off")
        Text(
            "Open Settings tab to change.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreferenceSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Projects",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (projects.isEmpty()) {
            Text(
                text = "No projects yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            return@Column
        }
        projects.forEach { name ->
            val isChecked = name in checked.value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        checked.value = if (isChecked) checked.value - name else checked.value + name
                    }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ProjectsSectionContent(viewModel: DashboardViewModel) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        ProjectsCheckboxSection(tiles = tiles)
    }
}

private fun extractProjects(tiles: List<app.tastile.android.data.model.Tile>): List<String> {
    return tiles
        .asSequence()
        .mapNotNull { tile ->
            val labels = tile.annotationConditions?.get("labels")?.toString().orEmpty()
            Regex("\"project:([^\"]+)\"").find(labels)?.groupValues?.getOrNull(1)
        }
        .distinct()
        .sorted()
        .toList()
}
