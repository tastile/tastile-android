package app.tastile.android.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.mobile.tabs.ExecuteScreen
import app.tastile.android.ui.mobile.tabs.IntegrationsScreen
import app.tastile.android.ui.mobile.tabs.SettingsScreen
import app.tastile.android.ui.mobile.tabs.TilesScreen
import app.tastile.android.ui.mobile.tabs.TimelineScreen

private const val START = "timeline"

@Composable
fun MobileScaffold(
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    overlayViewModel: OverlayViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: START
    val selectedDay by dashboardViewModel.selectedDay.collectAsStateWithLifecycle()
    val email by dashboardViewModel.email.collectAsStateWithLifecycle()
    val avatarUrl by dashboardViewModel.avatarUrl.collectAsStateWithLifecycle()
    val profile by dashboardViewModel.profile.collectAsStateWithLifecycle()
    val scale by dashboardViewModel.scale.collectAsStateWithLifecycle()

    // Range-aware header titles: Day=single date, Week=Mon–Sun short range, Month=long month name.
    val weekStart = remember(selectedDay) {
        selectedDay.minusDays((selectedDay.dayOfWeek.value - 1).toLong())
    }
    val weekEnd = remember(weekStart) { weekStart.plusDays(6) }
    val monthStart = remember(selectedDay) { selectedDay.withDayOfMonth(1) }
    val dayFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("M月d日 (EEE)", java.util.Locale.getDefault())
    }
    val weekShortFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("M/d", java.util.Locale.getDefault())
    }
    val monthFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.getDefault())
    }
    val title = when (currentRoute) {
        "execute" -> "Execute"
        "tiles" -> "Tiles"
        "integrations" -> "Integrations"
        "settings" -> "Settings"
        else -> when (scale) {
            TimelineScale.Day -> selectedDay.format(dayFormatter)
            TimelineScale.Week -> "${weekStart.format(weekShortFormatter)} – ${weekEnd.format(weekShortFormatter)}"
            TimelineScale.Month -> monthStart.format(monthFormatter)
        }
    }

    Scaffold(
        topBar = {
            MobileTopBar(
                title = title,
                scale = scale,
                onScaleChange = { dashboardViewModel.setScale(it) },
                onMenu = { overlayViewModel.show(Overlay.SidePanel(SidePanelSection.Calendar)) },
                onNotifications = { overlayViewModel.show(Overlay.Notifications) },
                onAvatar = { overlayViewModel.show(Overlay.AccountMenu) },
                avatarUrl = avatarUrl,
                avatarFallback = profile?.displayName?.firstOrNull()?.toString()
                    ?: email.firstOrNull()?.toString()
                    ?: "U",
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = START,
            ) {
                composable("timeline") { TimelineScreen(viewModel = dashboardViewModel) }
                composable("execute") { ExecuteScreen(viewModel = dashboardViewModel) }
                composable("tiles") { TilesScreen(viewModel = dashboardViewModel) }
                composable("integrations") { IntegrationsScreen(viewModel = dashboardViewModel) }
                composable("settings") { SettingsScreen(viewModel = dashboardViewModel) }
            }
            OverlayLayer(
                overlay = overlayViewModel,
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
            )

            // Task 21: prioritize overlay dismissal over nav pop on system back press.
            val overlayCurrent by overlayViewModel.current.collectAsStateWithLifecycle()
            BackHandler(enabled = overlayCurrent !is Overlay.Hidden) {
                dashboardViewModel.clearSelectedTile()
                overlayViewModel.dismiss()
            }
        }
    }
}