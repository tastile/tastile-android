package app.tastile.android.ui.mobile

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.mobile.designsystem.MobileTokens

@Composable
fun MobileBottomBar(
    currentRoute: String,
    onSelect: (route: String) -> Unit,
    onQuickCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        BottomSlot(
            icon = Icons.Outlined.FlashOn,
            description = "Execute",
            selected = currentRoute == "execute",
            onClick = { onSelect("execute") },
        )
        BottomSlot(
            icon = Icons.Outlined.Tune,
            description = "Tiles",
            selected = currentRoute == "tiles",
            onClick = { onSelect("tiles") },
        )
        BottomActionSlot(
            icon = Icons.Outlined.Add,
            description = "Quick create",
            onClick = onQuickCreate,
        )
        BottomSlot(
            icon = Icons.Outlined.Extension,
            description = "Integrations",
            selected = currentRoute == "integrations",
            onClick = { onSelect("integrations") },
        )
        BottomSlot(
            icon = Icons.Outlined.Settings,
            description = "Settings",
            selected = currentRoute == "settings",
            onClick = { onSelect("settings") },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomSlot(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = description.toDescriptionRes()),
                modifier = Modifier.size(MobileTokens.iconVisualSize),
            )
        },
        modifier = Modifier.semantics { role = Role.Button },
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomActionSlot(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = false,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = description.toDescriptionRes()),
                modifier = Modifier.size(MobileTokens.iconVisualSize),
            )
        },
        modifier = Modifier.semantics { role = Role.Button },
    )
}

private fun String.toDescriptionRes(): Int = when (this) {
    "Execute" -> app.tastile.android.R.string.mobile_bottom_execute
    "Tiles" -> app.tastile.android.R.string.mobile_bottom_tiles
    "Quick create" -> app.tastile.android.R.string.mobile_bottom_quick_create
    "Integrations" -> app.tastile.android.R.string.mobile_bottom_integrations
    "Settings" -> app.tastile.android.R.string.mobile_bottom_settings
    else -> app.tastile.android.R.string.mobile_bottom_settings
}