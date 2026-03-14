package app.tastile.android.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.ui.account.AccountScreen
import app.tastile.android.ui.billing.BillingScreen
import app.tastile.android.ui.login.LoginScreen
import app.tastile.android.ui.login.LoginViewModel
import app.tastile.android.ui.memo.MemoScreen
import app.tastile.android.ui.now.NowScreen
import app.tastile.android.ui.prompt.PromptScreen
import io.github.jan.supabase.auth.status.SessionStatus

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Now : Screen("now", "Now", Icons.Default.Home)
    data object Prompt : Screen("prompt", "Prompt", Icons.Default.Notifications)
    data object Memo : Screen("memo", "Memo", Icons.Default.Create)
    data object Account : Screen("account", "Account", Icons.Default.AccountCircle)
}

val bottomNavItems = listOf(
    Screen.Now,
    Screen.Prompt,
    Screen.Memo,
    Screen.Account
)

@Composable
fun TastileNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val sessionStatus by loginViewModel.sessionStatus.collectAsStateWithLifecycle()
    val isAuthenticated = sessionStatus is SessionStatus.Authenticated

    if (!isAuthenticated) {
        // Show login screen when not authenticated
        LoginScreen(
            onLoginSuccess = {
                // Navigation will automatically update when session changes
            }
        )
    } else {
        // Main app with bottom navigation
        MainAppScaffold(navController = navController, modifier = modifier)
    }
}

@Composable
fun MainAppScaffold(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected destination
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Now.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Now.route) {
                NowScreen()
            }
            composable(Screen.Prompt.route) {
                PromptScreen()
            }
            composable(Screen.Memo.route) {
                MemoScreen()
            }
            composable(Screen.Account.route) {
                AccountScreen(
                    onNavigateToBilling = {
                        navController.navigate("billing")
                    },
                    onSignOut = {
                        // Navigation will automatically update when session changes
                    }
                )
            }
            composable("billing") {
                BillingScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
