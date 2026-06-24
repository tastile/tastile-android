package app.tastile.android.ui.mobile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import app.tastile.android.R
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
            descriptionRes = R.string.mobile_bottom_execute,
            selected = currentRoute == "execute",
            onClick = { onSelect("execute") },
        )
        BottomSlot(
            icon = Icons.Outlined.Tune,
            descriptionRes = R.string.mobile_bottom_tiles,
            selected = currentRoute == "tiles",
            onClick = { onSelect("tiles") },
        )
        BottomActionSlot(
            icon = Icons.Outlined.Add,
            descriptionRes = R.string.mobile_bottom_quick_create,
            onClick = onQuickCreate,
        )
        BottomSlot(
            icon = Icons.Outlined.Extension,
            descriptionRes = R.string.mobile_bottom_integrations,
            selected = currentRoute == "integrations",
            onClick = { onSelect("integrations") },
        )
        BottomSlot(
            icon = Icons.Outlined.Settings,
            descriptionRes = R.string.mobile_bottom_settings,
            selected = currentRoute == "settings",
            onClick = { onSelect("settings") },
        )
    }
}

@Composable
private fun RowScope.BottomSlot(
    icon: ImageVector,
    @StringRes descriptionRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = descriptionRes),
                modifier = Modifier.size(MobileTokens.iconVisualSize),
            )
        },
        modifier = Modifier
            .semantics { role = Role.Button }
            .heightIn(min = MobileTokens.iconHitTarget),
    )
}

@Composable
private fun RowScope.BottomActionSlot(
    icon: ImageVector,
    @StringRes descriptionRes: Int,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = false,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = descriptionRes),
                modifier = Modifier.size(MobileTokens.iconVisualSize),
            )
        },
        modifier = Modifier
            .semantics { role = Role.Button }
            .heightIn(min = MobileTokens.iconHitTarget),
    )
}
