package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Tune
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection

private data class TabSpec(
    val section: SidePanelSection,
    val label: String,
    val icon: ImageVector,
    val route: String,
)

private val tabs = listOf(
    TabSpec(SidePanelSection.Calendar, "Timeline", Icons.Outlined.CalendarMonth, "timeline"),
    TabSpec(SidePanelSection.Schedule, "Tasks", Icons.Outlined.AccountCircle, "execute"),
    TabSpec(SidePanelSection.Projects, "Projects", Icons.Outlined.Folder, "tiles"),
    TabSpec(SidePanelSection.References, "References", Icons.Outlined.Bookmark, "integrations"),
    TabSpec(SidePanelSection.Preferences, "Preferences", Icons.Outlined.Tune, "settings"),
)

/**
 * Bottom-anchored navigation drawer with a 2-page HorizontalPager. Page 0
 * lists the section tabs; tapping any tab switches the main canvas
 * immediately (no intermediate commit step) and dismisses the sheet. Page 1
 * shows the section's content preview (mini calendar, recurring tasks,
 * reference links, etc.) and is reachable by swiping right — swipe left to
 * come back to the tab list. The header title switches with the active
 * page; pager dots below the body indicate the two pages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidePanelSheet(
    overlay: OverlayViewModel,
    dashboardViewModel: DashboardViewModel,
    onNavigate: (String) -> Unit,
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val section = (current as? Overlay.SidePanel)?.section ?: return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialIndex = tabs.indexOfFirst { it.section == section }.coerceAtLeast(0)
    var selectedIndex by remember(section) { mutableStateOf(initialIndex) }
    val pagerState = rememberPagerState(initialPage = 0) { 2 }

    val titleText = if (pagerState.currentPage == 0) {
        "Navigation"
    } else {
        tabs[selectedIndex].label
    }

    PanelSheet(
        title = titleText,
        sheetState = sheetState,
        onDismiss = { overlay.dismiss() },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = true,
            ) { page ->
                when (page) {
                    0 -> TabsPage(
                        selectedIndex = selectedIndex,
                        onSelect = { index ->
                            selectedIndex = index
                            onNavigate(tabs[index].route)
                            overlay.dismiss()
                        },
                    )
                    else -> SectionPage(
                        spec = tabs[selectedIndex],
                        dashboardViewModel = dashboardViewModel,
                    )
                }
            }
            PagerDots(
                currentPage = pagerState.currentPage,
                pageCount = 2,
            )
        }
    }
}

@Composable
private fun TabsPage(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEachIndexed { index, spec ->
            SidePanelTabRow(
                label = spec.label,
                leading = {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                selected = selectedIndex == index,
                role = Role.Tab,
                onClick = { onSelect(index) },
            )
        }
        Spacer(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun SectionPage(
    spec: TabSpec,
    dashboardViewModel: DashboardViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SectionPanelContent(
            section = spec.section,
            dashboardViewModel = dashboardViewModel,
        )
        Spacer(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PagerDots(
    currentPage: Int,
    pageCount: Int,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            val isCurrent = i == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isCurrent) 12.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) colors.onSurface
                        else colors.onSurfaceVariant.copy(alpha = 0.30f),
                    ),
            )
        }
    }
}

/**
 * Tastile single-tap navigation row used in the side-panel tab list. Inset
 * card with 6dp rounded corners and a subtle surface tint that intensifies
 * when the row is selected.
 */
@Composable
private fun SidePanelTabRow(
    label: String,
    leading: @Composable () -> Unit,
    selected: Boolean,
    role: Role,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (selected) colors.surfaceVariant.copy(alpha = 0.55f) else colors.surface
    val contentColor = if (selected) colors.onSurface else colors.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(role = role, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                this.role = role
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leading()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
    }
}