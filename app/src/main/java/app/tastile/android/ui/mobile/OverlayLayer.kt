package app.tastile.android.ui.mobile

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import app.tastile.android.notifications.NotificationRepository
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.sheets.AccountMenuSheet
import app.tastile.android.ui.mobile.sheets.NotificationsSheet
import app.tastile.android.ui.mobile.sheets.QuickCreateSheetMobile
import app.tastile.android.ui.mobile.sheets.SearchOverlaySheet
import app.tastile.android.ui.mobile.sheets.SidePanelSheet
import app.tastile.android.ui.mobile.sheets.TileEditSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Mount point for all mobile overlay sheets. The composable itself owns no
 * state — each sheet subscribes to [OverlayViewModel.current] and renders
 * itself only when its [Overlay] variant is active.
 */
@Composable
fun OverlayLayer(
    overlay: OverlayViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    notificationsViewModel: NotificationsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    QuickCreateSheetMobile(overlay = overlay, dashboardViewModel = dashboardViewModel)
    TileEditSheet(overlay = overlay, viewModel = dashboardViewModel)
    SearchOverlaySheet(overlay = overlay)
    NotificationsSheet(overlay = overlay, repository = notificationsViewModel.repository)
    AccountMenuSheet(viewModel = dashboardViewModel, overlay = overlay)
    SidePanelSheet(overlay = overlay, onNavigate = onNavigate)
}

/**
 * Thin Hilt-aware wrapper that exposes [NotificationRepository] to the
 * composable scope. We need a [ViewModel] here because `hiltViewModel<>()`
 * only resolves ViewModel-typed bindings; an `@EntryPoint` interface would
 * require an Activity/Fragment scope, which a composable does not have.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    val repository: NotificationRepository,
) : ViewModel()
