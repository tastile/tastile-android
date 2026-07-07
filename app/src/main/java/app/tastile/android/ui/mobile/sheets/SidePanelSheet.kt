package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
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
 * Bottom-anchored navigation drawer. Tapping a tab immediately navigates
 * the main canvas to that route and dismisses the sheet — no intermediate
 * preview / commit step.
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
    val selectedIndex = tabs.indexOfFirst { it.section == section }.coerceAtLeast(0)

    PanelSheet(
        title = "Navigation",
        sheetState = sheetState,
        onDismiss = { overlay.dismiss() },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TabsList(
                selectedIndex = selectedIndex,
                onSelect = { spec ->
                    onNavigate(spec.route)
                    overlay.dismiss()
                },
            )
            Spacer(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TabsList(
    selectedIndex: Int,
    onSelect: (TabSpec) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        tabs.forEachIndexed { index, spec ->
            PanelRow(
                label = spec.label,
                icon = spec.icon,
                selected = selectedIndex == index,
                role = Role.Tab,
                onClick = { onSelect(spec) },
            )
        }
    }
}