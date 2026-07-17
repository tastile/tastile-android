package app.tastile.android.ui.mobile.sheets

// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.core.designsystem.component.NiaSideSheet
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.panels.ProjectsViewModel
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreatePanelContent
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSubmissionViewModel
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSubpanel
import app.tastile.android.ui.mobile.sheets.quickcreate.quickCreateSubmissionValidation

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
        val baseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val subpanelSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
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
        val draft by quickCreateStore.state.collectAsStateWithLifecycle()
        val validation = quickCreateSubmissionValidation(draft)
        val canSubmit = validation.isValid && !submission.isSubmitting
        val projects = projectsState.workspaces.map { QuickCreateProject(it.id, it.displayName) }

        // M3 Phase 2: dispatch on (activePanel, screenWidthDp).
        // - Base panel → full PanelSheet (current behaviour, hosts submit CTA).
        // - Subpanel on narrow phones (<600.dp) → full PanelSheet so the M3
        //   drag handle / scrim stay consistent with the base flow.
        // - Subpanel on wide screens (≥600.dp, tablets / foldables) → NiaSideSheet
        //   (widthIn(max = 480.dp), side-anchored) so the parent context stays
        //   visible behind the panel.
        val active = draft.activePanel
        val isSubpanel = active != null && active != QuickCreatePanel.Base
        val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600
        when {
            isSubpanel && isWideScreen -> {
                NiaSideSheet(
                    sheetState = subpanelSheetState,
                    onDismissRequest = { quickCreateStore.backToBase() },
                ) {
                    QuickCreateSubpanel(
                        panel = active,
                        draft = draft,
                        store = quickCreateStore,
                        onBack = quickCreateStore::backToBase,
                        projects = projects,
                        knownTags = knownTags,
                    )
                }
            }
            isSubpanel -> {
                PanelSheet(
                    sheetState = subpanelSheetState,
                    onDismiss = { quickCreateStore.backToBase() },
                ) {
                    QuickCreateSubpanel(
                        panel = active,
                        draft = draft,
                        store = quickCreateStore,
                        onBack = quickCreateStore::backToBase,
                        projects = projects,
                        knownTags = knownTags,
                    )
                }
            }
            else -> {
                PanelSheet(
                    sheetState = baseSheetState,
                    onDismiss = { overlay.dismiss() },
                    onSubmit = { resolvedSubmissionViewModel.submit(draft) },
                    submitEnabled = canSubmit,
                    submitLabel = "Create",
                    submitTestTag = "quick-create-submit",
                ) {
                    QuickCreatePanelContent(
                        store = quickCreateStore,
                        onClose = { overlay.dismiss() },
                        onSubmit = { resolvedSubmissionViewModel.submit(draft) },
                        isSubmitting = submission.isSubmitting,
                        submitError = submission.error,
                        projects = projects,
                        knownTags = knownTags,
                    )
                }
            }
        }
    }
}