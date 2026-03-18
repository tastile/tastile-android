package app.tastile.android.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tastile.android.ui.dashboard.AccountDashboardScreen
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecuteDashboardScreen
import app.tastile.android.ui.dashboard.IntegrationsDashboardScreen
import app.tastile.android.ui.dashboard.QuickCreateSheet
import app.tastile.android.ui.dashboard.SettingsDashboardScreen
import app.tastile.android.ui.dashboard.TilesDashboardScreen
import app.tastile.android.ui.dashboard.isStarted
import app.tastile.android.ui.login.LoginScreen
import app.tastile.android.ui.login.LoginViewModel
import io.github.jan.supabase.auth.status.SessionStatus

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val tabItem: Boolean = true
) {
    data object Execute : Screen("dashboard/execute", "Execute", Icons.Default.Home)
    data object Tiles : Screen("dashboard/tiles", "Tiles", Icons.Default.List)
    data object New : Screen("dashboard/new", "New", Icons.Default.Add)
    data object Integrations : Screen("dashboard/integrations", "Integrations", Icons.Default.Build)
    data object Settings : Screen("dashboard/settings", "Settings", Icons.Default.Settings)
    data object Account : Screen("dashboard/account", "Account", Icons.Default.Settings, tabItem = false)
}

private val bottomNavItems = listOf(
    Screen.Execute,
    Screen.Tiles,
    Screen.New,
    Screen.Integrations,
    Screen.Settings
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
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    val profile by dashboardViewModel.profile.collectAsStateWithLifecycle()
    val email by dashboardViewModel.email.collectAsStateWithLifecycle()
    val tiles by dashboardViewModel.tiles.collectAsStateWithLifecycle()
    val activeTile = tiles.firstOrNull { it.isStarted() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tastile", fontWeight = FontWeight.SemiBold)
                        Text(
                            activeTile?.title ?: "待機中",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val displayText = profile?.displayName?.firstOrNull()?.uppercase() ?: email.firstOrNull()?.uppercase() ?: "U"
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                            .clickable { navController.navigate(Screen.Account.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(displayText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    if (screen == Screen.New) {
                        NavigationBarItem(
                            selected = false,
                            onClick = { showCreateSheet = true },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
                        )
                    } else {
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Execute.route,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                composable(Screen.Execute.route) { ExecuteDashboardScreen(viewModel = dashboardViewModel) }
                composable(Screen.Tiles.route) { TilesDashboardScreen(viewModel = dashboardViewModel) }
                composable(Screen.Integrations.route) { IntegrationsDashboardScreen() }
                composable(Screen.Settings.route) { SettingsDashboardScreen(viewModel = dashboardViewModel) }
                composable(Screen.Account.route) { AccountDashboardScreen(viewModel = dashboardViewModel) }
            }
        }
    }

    if (showCreateSheet) {
        ModalBottomSheet(onDismissRequest = { showCreateSheet = false }) {
            QuickCreateSheet(
                viewModel = dashboardViewModel,
                onClose = { showCreateSheet = false }
            )
        }
    }
}
