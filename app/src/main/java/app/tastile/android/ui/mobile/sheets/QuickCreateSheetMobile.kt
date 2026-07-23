package app.tastile.android.ui.mobile.sheets

// m2-allow: experimental-annotation
// m2-allow: m3-component
import androidx.compose.foundation.layout.Box
// m2-allow: m3-component
import androidx.compose.foundation.layout.fillMaxWidth
// m2-allow: m3-component
import androidx.compose.foundation.layout.padding
// m2-allow: m3-component
import androidx.compose.foundation.layout.width
// m2-allow: m3-component
import androidx.compose.material.icons.Icons
// m2-allow: m3-component
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
// m2-allow: m3-component
import androidx.compose.material.icons.outlined.Close
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.BottomSheetDefaults
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: m3-component
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.foundation.layout.PaddingValues
// m2-allow: m3-component
import androidx.compose.foundation.shape.RoundedCornerShape
// m2-allow: m3-component
import androidx.compose.material3.Button
// m2-allow: m3-component
import androidx.compose.material3.ButtonDefaults
// m2-allow: m3-component
import androidx.compose.material3.ModalBottomSheet
// m2-allow: m3-component
import androidx.compose.material3.Text
// m2-allow: state-holder
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.panels.ProjectsViewModel
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreatePanelContent
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSubmissionViewModel
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSubpanel
import app.tastile.android.ui.mobile.sheets.quickcreate.quickCreateSubmissionValidation
import app.tastile.android.core.designsystem.theme.PanelTokens

/**
 * Two-sheet QuickCreate flow.
 *
 * The base panel sits in its own [ModalBottomSheet]; when the user drills into
 * Time / Duration / Repeat / Meta / etc. a second [ModalBottomSheet] is
 * stacked on top of it. The subpanel's swipe-to-dismiss calls
 * [QuickCreateStateStore.backToBase] so only the subpanel tears down — the
 * base sheet (and its Create button) stays underneath.
 *
 * The base sheet is kept mounted across subpanel open/close (passed
 * [keepBaseVisible] = true) so the only thing that re-mounts on subpanel
 * navigation is the subpanel sheet itself.
 */
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
        val knownTags = remember(tiles) {
            tiles.flatMap { it.labels }
                .filter { it.isNotBlank() && !it.startsWith("project:") }
                .map(String::trim)
                .distinct()
                .sortedBy { it.lowercase() }
        }
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
        val projects = projectsState.workspaces.map { QuickCreateProject(it.id, it.displayName) }

        val active = draft.activePanel
        val isSubpanel = active != null && active != QuickCreatePanel.Base

        // Base sheet — always rendered while the overlay is QuickCreate.
        val baseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val baseValidation = quickCreateSubmissionValidation(draft)
        ModalBottomSheet(
            onDismissRequest = { overlay.dismiss() },
            sheetState = baseSheetState,
            dragHandle = {
                QuickCreateHandleRow(
                    leading = {
                        IconButton(
                            onClick = { overlay.dismiss() },
                            modifier = Modifier.testTag("quick-create-close"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    trailing = {
                        Button(
                            onClick = { resolvedSubmissionViewModel.submit(draft) },
                            enabled = baseValidation.isValid && !submission.isSubmitting,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.testTag("quick-create-handle-submit"),
                        ) {
                            Text("Create")
                        }
                    },
                )
            },
        ) {
            QuickCreatePanelContent(
                store = quickCreateStore,
                onClose = { overlay.dismiss() },
                isSubmitting = submission.isSubmitting,
                submitError = submission.error,
                projects = projects,
                knownTags = knownTags,
                keepBaseVisible = true,
            )
        }

        // Subpanel sheet — stacked on top of the base; its dismiss only
        // pops the subpanel via backToBase, leaving the base sheet open.
        if (isSubpanel) {
            val subpanelSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { quickCreateStore.backToBase() },
                sheetState = subpanelSheetState,
                dragHandle = {
                    QuickCreateHandleRow(
                        leading = {
                            IconButton(
                                onClick = { quickCreateStore.backToBase() },
                                modifier = Modifier.testTag("quick-create-back"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                },
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
    }
}

/**
 * Drag-handle row shared by the base sheet and the subpanel sheet.
 *
 * Uses a `Box` layout (not `Row`) so the drag pill is *always* screen-centered
 * regardless of the leading / trailing slot widths:
 *  - [leading] (X close / back): absolutely positioned to the leading edge,
 *    with [PanelTokens.LeadingColumnWidth] of breathing room so the icon
 *    lines up with the leading icon column of the rows underneath.
 *  - center: standard M3 drag pill, screen-centered.
 *  - [trailing] (Create): absolutely positioned to the trailing edge with
 *    16.dp of right padding so the button's right edge lines up with the
 *    content rows below.
 *
 * The whole row remains a drag target because [ModalBottomSheet.dragHandle]
 * is non-null — the [IconButton] / [Button] touch targets sit inside the
 * draggable area but claim taps via the standard clickable gesture detector.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCreateHandleRow(
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(PanelTokens.LeadingColumnWidth),
                contentAlignment = Alignment.Center,
            ) { leading() }
        }
        BottomSheetDefaults.DragHandle(
            modifier = Modifier.align(Alignment.Center),
        )
        if (trailing != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
            ) { trailing() }
        }
    }
}