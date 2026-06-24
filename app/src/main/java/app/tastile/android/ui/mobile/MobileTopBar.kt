package app.tastile.android.ui.mobile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.designsystem.AppAvatar
import app.tastile.android.ui.mobile.designsystem.MobileTokens

@Composable
fun MobileTopBar(
    title: String,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    avatarFallback: String = "U",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarAction(
            icon = Icons.Outlined.Menu,
            descriptionRes = R.string.mobile_top_menu,
            onClick = onMenu,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
        Box(modifier = Modifier.weight(1f))
        TopBarAction(
            icon = Icons.Outlined.Search,
            descriptionRes = R.string.mobile_top_search,
            onClick = onSearch,
        )
        TopBarAction(
            icon = Icons.Outlined.NotificationsNone,
            descriptionRes = R.string.mobile_top_notifications,
            onClick = onNotifications,
        )
        TopBarAvatarAction(
            descriptionRes = R.string.mobile_top_avatar,
            onClick = onAvatar,
            avatarUrl = avatarUrl,
            avatarFallback = avatarFallback,
        )
    }
}

@Composable
private fun TopBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    @StringRes descriptionRes: Int,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = MobileTokens.iconHitTarget)
            .semantics { role = Role.Button },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(id = descriptionRes),
            modifier = Modifier.size(MobileTokens.iconVisualSize),
        )
    }
}

@Composable
private fun TopBarAvatarAction(
    @StringRes descriptionRes: Int,
    onClick: () -> Unit,
    avatarUrl: String?,
    avatarFallback: String,
) {
    val descriptionString = stringResource(id = descriptionRes)
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = MobileTokens.iconHitTarget)
            .clearAndSetSemantics {
                contentDescription = descriptionString
                role = Role.Button
            },
    ) {
        AppAvatar(imageUrl = avatarUrl, fallbackText = avatarFallback)
    }
}
