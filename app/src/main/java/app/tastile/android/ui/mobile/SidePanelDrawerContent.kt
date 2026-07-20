package app.tastile.android.ui.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
// m2-allow: state-holder
import androidx.compose.material3.DrawerState
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.ModalDrawerSheet
// m2-allow: m3-component
import androidx.compose.material3.NavigationDrawerItem
// m2-allow: state-holder
import androidx.compose.material3.NavigationDrawerItemDefaults
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.tastile.android.R
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
)

/**
 * Material 3 modal navigation drawer that replaces the bottom-anchored
 * `SidePanelSheet` 2-page pager. A brand header (icon + app title) sits above
 * the four primary-nav destinations; the dedicated Settings row sits below a
 * thin divider. The active route is highlighted via [NavigationDrawerItem]'s
 * built-in selection styling, and tapping any item navigates + closes the
 * drawer.
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
    val settingLabel = stringResource(R.string.nav_setting)

    ModalDrawerSheet(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            BrandHeader()
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
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            NavigationDrawerItem(
                label = { Text(settingLabel) },
                selected = currentRoute == "settings",
                onClick = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
                    scope.launch { drawerState.close() }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        modifier = Modifier.testTag("side-panel-icon-settings"),
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(),
                modifier = Modifier.testTag("side-panel-row-settings"),
            )
        }
    }
}

@Composable
private fun BrandHeader() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val markRes = if (isDark) R.drawable.ic_tastile_icon_dark else R.drawable.ic_tastile_icon
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(id = markRes),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .testTag("side-panel-brand-mark"),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("side-panel-brand-title"),
        )
    }
}
