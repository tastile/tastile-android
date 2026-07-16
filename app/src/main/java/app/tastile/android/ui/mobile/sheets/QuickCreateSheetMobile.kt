package app.tastile.android.ui.mobile.sheets

// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.mobile.panels.ProjectsViewModel
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreatePanelContent
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSubmissionViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCreateSheetMobile(
    overlay: OverlayViewModel,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    projectsViewModel: ProjectsViewModel? = null,
    submissionViewModel: QuickCreateSubmissionViewModel? = null,
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val tiles by dashboardViewModel.tiles.collectAsStateWithLifecycle()

    if (current is Overlay.QuickCreate || current is Overlay.QuickCreateAt) {
        val resolvedProjectsViewModel = projectsViewModel ?: hiltViewModel()
        val resolvedSubmissionViewModel = submissionViewModel ?: hiltViewModel()
        val projectsState by resolvedProjectsViewModel.state.collectAsStateWithLifecycle()
        val submission by resolvedSubmissionViewModel.state.collectAsStateWithLifecycle()
        val knownTags = tiles.flatMap { it.labels }
            .filter { it.isNotBlank() && !it.startsWith("project:") }
            .map(String::trim)
            .distinct()
            .sortedBy { it.lowercase() }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val initialDraft = (current as? Overlay.QuickCreateAt)?.let { slot ->
            QuickCreateDraftState(
                time = QuickCreateTime(
                    span = QuickCreateSpan(slot.startIso, slot.endIso),
                    whenMode = QuickCreateWhenMode.Range,
                    timeOfDayMode = QuickCreateTimeOfDayMode.Range,
                ),
            )
        } ?: QuickCreateDraftState()
        val quickCreateStore = remember(current) { QuickCreateStateStore(initialDraft) }
        LaunchedEffect(submission.createdTileId) {
            if (submission.createdTileId != null) {
                quickCreateStore.reset()
                dashboardViewModel.refreshAll()
                resolvedSubmissionViewModel.consumeCreatedTile()
                overlay.dismiss()
            }
        }
        PanelSheet(
            title = "Quick Create",
            sheetState = sheetState,
            onDismiss = { overlay.dismiss() },
        ) {
            QuickCreatePanelContent(
                store = quickCreateStore,
                onClose = { overlay.dismiss() },
                onSubmit = resolvedSubmissionViewModel::submit,
                isSubmitting = submission.isSubmitting,
                submitError = submission.error,
                projects = projectsState.workspaces.map { QuickCreateProject(it.id, it.displayName) },
                knownTags = knownTags,
            )
        }
    }
}
