package app.tastile.android.ui.mobile.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
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
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
    submissionViewModel: QuickCreateSubmissionViewModel = hiltViewModel(),
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val projectsState by projectsViewModel.state.collectAsStateWithLifecycle()
    val tiles by dashboardViewModel.tiles.collectAsStateWithLifecycle()
    val submission by submissionViewModel.state.collectAsStateWithLifecycle()
    val knownTags = tiles.flatMap { it.labels }
        .filter { it.isNotBlank() && !it.startsWith("project:") }
        .map(String::trim)
        .distinct()
        .sortedBy { it.lowercase() }

    if (current is Overlay.QuickCreate || current is Overlay.QuickCreateAt) {
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
                submissionViewModel.consumeCreatedTile()
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
                onSubmit = submissionViewModel::submit,
                isSubmitting = submission.isSubmitting,
                submitError = submission.error,
                projects = projectsState.workspaces.map { QuickCreateProject(it.id, it.displayName) },
                knownTags = knownTags,
            )
        }
    }
}
