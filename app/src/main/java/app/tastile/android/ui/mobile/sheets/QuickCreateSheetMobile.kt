package app.tastile.android.ui.mobile.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.mobile.panels.ProjectsViewModel
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreatePanelContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCreateSheetMobile(
    overlay: OverlayViewModel,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val projectsState by projectsViewModel.state.collectAsStateWithLifecycle()
    val tiles by dashboardViewModel.tiles.collectAsStateWithLifecycle()
    val knownTags = tiles.flatMap { it.labels }
        .filter { it.isNotBlank() && !it.startsWith("project:") }
        .map(String::trim)
        .distinct()
        .sortedBy { it.lowercase() }

    if (current is Overlay.QuickCreate) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val quickCreateStore = remember { QuickCreateStateStore() }
        PanelSheet(
            title = "Quick Create",
            sheetState = sheetState,
            onDismiss = { overlay.dismiss() },
        ) {
            QuickCreatePanelContent(
                store = quickCreateStore,
                onClose = { overlay.dismiss() },
                projects = projectsState.workspaces.map { QuickCreateProject(it.id, it.displayName) },
                knownTags = knownTags,
            )
        }
    }
}
