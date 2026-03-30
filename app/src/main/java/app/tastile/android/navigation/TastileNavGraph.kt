package app.tastile.android.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tastile.android.ui.dashboard.AccountDashboardScreen
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecuteDashboardScreen
import app.tastile.android.ui.dashboard.IntegrationsDashboardScreen
import app.tastile.android.ui.dashboard.SettingsDashboardScreen
import app.tastile.android.ui.dashboard.TimelineScreen
import app.tastile.android.ui.dashboard.TilesDashboardScreen
import app.tastile.android.ui.dashboard.isStarted
import app.tastile.android.ui.login.LoginScreen
import app.tastile.android.ui.login.LoginViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Execute : Screen("dashboard/execute", "Execute", Icons.Default.Home)
    data object Tiles : Screen("dashboard/tiles", "Tiles", Icons.Default.List)
    data object Timeline : Screen("dashboard/timeline", "Timeline", Icons.Default.Today)
    data object Integrations : Screen("dashboard/integrations", "Integrations", Icons.Default.Build)
    data object Settings : Screen("dashboard/settings", "Settings", Icons.Default.Settings)
    data object Account : Screen("dashboard/account", "Account", Icons.Default.Settings)
}

private val drawerItems = listOf(
    Screen.Execute,
    Screen.Tiles,
    Screen.Timeline,
    Screen.Integrations,
    Screen.Settings,
    Screen.Account
)

@Composable
fun TastileNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val sessionStatus by loginViewModel.sessionStatus.collectAsStateWithLifecycle()
    val isAuthenticated = sessionStatus is SessionStatus.Authenticated

    if (!isAuthenticated) {
        LoginScreen(onLoginSuccess = {})
    } else {
        LaunchedEffect(sessionStatus) {
            dashboardViewModel.refreshAll()
        }
        MainAppScaffold(
            navController = navController,
            dashboardViewModel = dashboardViewModel,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val profile by dashboardViewModel.profile.collectAsStateWithLifecycle()
    val email by dashboardViewModel.email.collectAsStateWithLifecycle()
    val tiles by dashboardViewModel.tiles.collectAsStateWithLifecycle()
    val activeTile = tiles.firstOrNull { it.isStarted() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    drawerItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(screen.route) {
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(screen.icon, contentDescription = screen.title)
                            Text(
                                screen.title,
                                style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                        Column {
                            Text("Tastile")
                            Text(activeTile?.title ?: "Idle", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    val displayText = profile?.displayName?.firstOrNull()?.uppercase() ?: email.firstOrNull()?.uppercase() ?: "U"
                    Text(
                        text = displayText,
                        modifier = Modifier
                            .clickable { navController.navigate(Screen.Account.route) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Timeline.route,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    composable(Screen.Execute.route) { ExecuteDashboardScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Tiles.route) { TilesDashboardScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Timeline.route) { TimelineScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Integrations.route) { IntegrationsDashboardScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Settings.route) { SettingsDashboardScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Account.route) { AccountDashboardScreen(viewModel = dashboardViewModel) }
                }
            }
        }
    }
}

