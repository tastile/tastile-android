package app.tastile.android.ui.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

private data class DrawerRoute(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val drawerRoutes = listOf(
    DrawerRoute("timeline", "Timeline", Icons.Outlined.Schedule),
    DrawerRoute("execute", "Tasks", Icons.Outlined.Checklist),
    DrawerRoute("tiles", "Projects", Icons.Outlined.FolderOpen),
    DrawerRoute("integrations", "References", Icons.Outlined.Link),
    DrawerRoute("settings", "Preferences", Icons.Outlined.Settings),
)

/**
 * Material 3 modal navigation drawer that replaces the bottom-anchored
 * `SidePanelSheet` 2-page pager. Five primary-nav destinations; the active
 * route is highlighted via [NavigationDrawerItem]'s built-in selection
 * styling, and tapping any item navigates + closes the drawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidePanelDrawerContent(
    navController: NavHostController,
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    ModalDrawerSheet(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))
            drawerRoutes.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.testTag("side-panel-icon-${item.route}"),
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(),
                    modifier = Modifier.testTag("side-panel-row-${item.route}"),
                )
            }
        }
    }
}
