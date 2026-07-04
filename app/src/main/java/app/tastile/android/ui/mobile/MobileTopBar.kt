package app.tastile.android.ui.mobile

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.designsystem.AppAvatar
import app.tastile.android.ui.mobile.designsystem.MobileTokens

@Composable
fun MobileTopBar(
    title: String,
    scale: TimelineScale,
    onScaleChange: (TimelineScale) -> Unit,
    onMenu: () -> Unit,
    onNotifications: () -> Unit,
    onAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    avatarFallback: String = "U",
    showScale: Boolean = true,
) {
    val background = MaterialTheme.colorScheme.background
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Smooth linear fade: the notch band is ~80% opaque so the menu
            // / title / icons remain readable, and the bar eases to fully
            // transparent at its bottom edge. No kink at a hold-point so the
            // gradient reads as one continuous surface, not two stacked bands.
            .background(
                Brush.verticalGradient(
                    0f to background.copy(alpha = 1.0f),
                    0.50f to background.copy(alpha = 0.70f),
                    1f to Color.Transparent,
                ),
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarAction(
            icon = Icons.Outlined.Menu,
            descriptionRes = R.string.mobile_top_menu,
            onClick = onMenu,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp),
        )
        Box(modifier = Modifier.weight(1f))
        if (showScale) {
            ScaleDropdown(scale = scale, onScaleChange = onScaleChange)
            Spacer(Modifier.width(4.dp))
        }
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
private fun ScaleDropdown(
    scale: TimelineScale,
    onScaleChange: (TimelineScale) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val pillShape = RoundedCornerShape(50)
    Box {
        Row(
            modifier = Modifier
                .clip(pillShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, pillShape)
                .clickable(onClick = { expanded = true })
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .semantics { contentDescription = "Scale: ${scale.name}" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = scale.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TimelineScale.entries.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = entry.name,
                            fontWeight = if (entry == scale) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onScaleChange(entry)
                        expanded = false
                    },
                )
            }
        }
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