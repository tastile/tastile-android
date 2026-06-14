package app.tastile.android.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
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
import app.tastile.android.ui.dashboard.CalendarViewMode
import app.tastile.android.ui.dashboard.TimelineScreen
import app.tastile.android.ui.dashboard.TilesDashboardScreen
import app.tastile.android.ui.dashboard.isStarted
import app.tastile.android.ui.designsystem.AppAvatar
import app.tastile.android.ui.designsystem.AppBodyText
import app.tastile.android.ui.designsystem.AppIconButton
import app.tastile.android.ui.designsystem.AppSectionTitle
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.login.LoginScreen
import app.tastile.android.ui.login.LoginViewModel
import app.tastile.android.data.repository.TastileAuthState
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object CalendarDay : Screen("calendar/day", "Day", Icons.Default.Today)
    data object CalendarWeek : Screen("calendar/week", "Week", Icons.Default.Today)
    data object CalendarMonth : Screen("calendar/month", "Month", Icons.Default.Today)
    data object Now : Screen("flow/now", "Now", Icons.Default.Home)
    data object Next : Screen("flow/next", "Next", Icons.AutoMirrored.Filled.List)
    data object Review : Screen("flow/review", "Review", Icons.Default.Today)
    data object Tiles : Screen("manage/tiles", "Tiles", Icons.AutoMirrored.Filled.List)
    data object Integrations : Screen("manage/integrations", "Integrations", Icons.Default.Build)
    data object Settings : Screen("manage/settings", "Settings", Icons.Default.Settings)
    data object Account : Screen("manage/account", "Account", Icons.Default.Settings)
}

data class SidePanelSection(
    val id: String,
    val title: String,
    val items: List<Screen>
)

val appNavigationStartRoute: String = Screen.CalendarDay.route

fun sidePanelSections(): List<SidePanelSection> = listOf(
    SidePanelSection(
        id = "display",
        title = "Display",
        items = listOf(Screen.CalendarDay, Screen.CalendarWeek, Screen.CalendarMonth)
    ),
    SidePanelSection(
        id = "flow",
        title = "Flow",
        items = listOf(Screen.Now, Screen.Next, Screen.Review)
    ),
    SidePanelSection(
        id = "management",
        title = "Management",
        items = listOf(Screen.Tiles, Screen.Integrations, Screen.Settings, Screen.Account)
    )
)

private val screenByRoute = sidePanelSections()
    .flatMap { it.items }
    .associateBy { it.route }

@Composable
fun TastileNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val authState by loginViewModel.authState.collectAsStateWithLifecycle()
    val isAuthenticated = authState is TastileAuthState.Authenticated

    if (!isAuthenticated) {
        LoginScreen(onLoginSuccess = {})
    } else {
        LaunchedEffect(authState) {
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
    val avatarUrl by dashboardViewModel.avatarUrl.collectAsStateWithLifecycle()
    val tiles by dashboardViewModel.tiles.collectAsStateWithLifecycle()
    val activeTile = tiles.firstOrNull { it.isStarted() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val panelSections = sidePanelSections()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    panelSections.forEach { section ->
                        AppSectionTitle(
                            text = section.title,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        section.items.forEach { screen ->
                            val selected = currentRoute == screen.route
                            val bg by animateColorAsState(
                                targetValue = if (selected) AppTheme.colors.secondaryContainer else AppTheme.colors.surface,
                                animationSpec = tween(180),
                                label = "drawer_item_bg"
                            )
                            val fg by animateColorAsState(
                                targetValue = if (selected) AppTheme.colors.onSecondaryContainer else AppTheme.colors.onSurface,
                                animationSpec = tween(180),
                                label = "drawer_item_fg"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .background(bg)
                                    .clickable {
                                        navController.navigate(screen.route) {
                                            launchSingleTop = true
                                        }
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(screen.icon, contentDescription = screen.title, tint = fg)
                                AppBodyText(screen.title, color = fg)
                            }
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
                        AppIconButton(icon = Icons.Default.Menu, contentDescription = "Open menu", onClick = { scope.launch { drawerState.open() } })
                        Column {
                            AppBodyText(screenByRoute[currentRoute]?.title ?: "Tastile")
                            AppSectionTitle(activeTile?.title ?: "Idle")
                        }
                    }
                    val fallbackText = profile?.displayName?.firstOrNull()?.toString() ?: email.firstOrNull()?.toString() ?: "U"
                    Box(
                        modifier = Modifier
                            .clickable { navController.navigate(Screen.Account.route) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppAvatar(
                            imageUrl = avatarUrl,
                            fallbackText = fallbackText
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = appNavigationStartRoute,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    composable(Screen.CalendarDay.route) {
                        TimelineScreen(viewModel = dashboardViewModel, mode = CalendarViewMode.DAY)
                    }
                    composable(Screen.CalendarWeek.route) {
                        TimelineScreen(viewModel = dashboardViewModel, mode = CalendarViewMode.WEEK)
                    }
                    composable(Screen.CalendarMonth.route) {
                        TimelineScreen(viewModel = dashboardViewModel, mode = CalendarViewMode.MONTH)
                    }
                    composable(Screen.Now.route) { ExecuteDashboardScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Next.route) { TilesDashboardScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Review.route) { AccountDashboardScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Tiles.route) { TilesDashboardScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Integrations.route) { IntegrationsDashboardScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Settings.route) { SettingsDashboardScreen(viewModel = dashboardViewModel) }
                    composable(Screen.Account.route) { AccountDashboardScreen(viewModel = dashboardViewModel) }
                }
            }
        }
    }
}

