package app.tastile.android.ui.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.login.LoginScreen
import app.tastile.android.ui.login.LoginViewModel

@Composable
fun MobileNavGraph(
    loginViewModel: LoginViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
) {
    val authState by loginViewModel.authState.collectAsStateWithLifecycle()
    val isAuthenticated = authState is TastileAuthState.Authenticated

    if (!isAuthenticated) {
        // Auth gate is state-driven: the `authState` flip on success re-renders this branch,
        // so the callback is intentionally a no-op.
        LoginScreen(onLoginSuccess = {})
    } else {
        LaunchedEffect(authState) {
            dashboardViewModel.refreshAll()
        }
        MobileScaffold(dashboardViewModel = dashboardViewModel)
    }
}