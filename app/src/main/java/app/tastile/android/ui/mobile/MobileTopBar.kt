package app.tastile.android.ui.mobile

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenu
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: primitive
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
import coil.compose.AsyncImage

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
    Box {
        CompactPickerButton(
            label = scale.name,
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = "Scale: ${scale.name}" },
        )
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

/**
 * Compact pill-shaped picker button — wraps [Surface] with a pill shape and
 * outline border so the top-bar dropdown trigger matches the Material 3
 * surface interaction behaviour (built-in ripple via [Surface]).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CompactPickerButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
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
            .heightIn(min = 48.dp)
            .semantics { role = Role.Button },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(id = descriptionRes),
            modifier = Modifier.size(24.dp),
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
            .heightIn(min = 48.dp)
            .clearAndSetSemantics {
                contentDescription = descriptionString
                role = Role.Button
            },
    ) {
        AvatarCircle(
            imageUrl = avatarUrl,
            fallbackText = avatarFallback,
        )
    }
}

/**
 * Circular avatar used by the top-bar avatar action. Renders a 40dp circular
 * surface; if [imageUrl] is non-blank it loads with [AsyncImage], otherwise it
 * shows the first letter of [fallbackText] on the primary-container surface.
 */
@Composable
private fun AvatarCircle(
    imageUrl: String?,
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    val size = 40.dp
    val hasImage = !imageUrl.isNullOrBlank()
    if (hasImage) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Avatar",
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fallbackText.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}