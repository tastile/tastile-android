package app.tastile.android.ui.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.tabs.ExecuteScreen
import app.tastile.android.ui.mobile.tabs.IntegrationsScreen
import app.tastile.android.ui.mobile.tabs.SettingsScreen
import app.tastile.android.ui.mobile.tabs.TilesScreen

private const val START = "execute"

@Composable
fun MobileScaffold(
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    overlayViewModel: OverlayViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: START
    val title = when (currentRoute) {
        "tiles" -> "Tiles"
        "integrations" -> "Integrations"
        "settings" -> "Settings"
        else -> "Execute"
    }
    val email by dashboardViewModel.email.collectAsStateWithLifecycle()
    val avatarUrl by dashboardViewModel.avatarUrl.collectAsStateWithLifecycle()
    val profile by dashboardViewModel.profile.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            MobileTopBar(
                title = title,
                onMenu = { overlayViewModel.show(Overlay.SidePanel(SidePanelSection.Calendar)) },
                onSearch = { overlayViewModel.show(Overlay.Search) },
                onNotifications = { overlayViewModel.show(Overlay.Notifications) },
                onAvatar = { overlayViewModel.show(Overlay.AccountMenu) },
                avatarUrl = avatarUrl,
                avatarFallback = profile?.displayName?.firstOrNull()?.toString()
                    ?: email.firstOrNull()?.toString()
                    ?: "U",
            )
        },
        bottomBar = {
            MobileBottomBar(
                currentRoute = currentRoute,
                onSelect = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                },
                onQuickCreate = { overlayViewModel.show(Overlay.QuickCreate) },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = START,
            ) {
                composable("execute") { ExecuteScreen(viewModel = dashboardViewModel) }
                composable("tiles") { TilesScreen(viewModel = dashboardViewModel) }
                composable("integrations") { IntegrationsScreen(viewModel = dashboardViewModel) }
                composable("settings") { SettingsScreen(viewModel = dashboardViewModel) }
            }
            OverlayLayer(overlayViewModel)
        }
    }

    // Task 22 will hook BackHandler here.
}