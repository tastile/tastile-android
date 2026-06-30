package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
 * Bottom-anchored navigation drawer. The panel is intentionally content-less
 * below the tab list — Timeline, Tasks, and Projects all live on the main
 * screen, so the panel's job is to switch to a different destination, not to
 * re-render the same data in miniature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidePanelSheet(
    overlay: OverlayViewModel,
    onNavigate: (String) -> Unit,
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val section = (current as? Overlay.SidePanel)?.section ?: return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIndex by remember(section) {
        mutableStateOf(tabs.indexOfFirst { it.section == section }.coerceAtLeast(0))
    }

    ModalBottomSheet(
        onDismissRequest = { overlay.dismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Sections",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            )
            tabs.forEachIndexed { index, spec ->
                SidePanelTab(
                    label = spec.label,
                    icon = spec.icon,
                    selected = selectedIndex == index,
                    onClick = {
                        selectedIndex = index
                        onNavigate(spec.route)
                        overlay.dismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun SidePanelTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.surfaceVariant
             else MaterialTheme.colorScheme.background
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.background,
                ),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.height(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
