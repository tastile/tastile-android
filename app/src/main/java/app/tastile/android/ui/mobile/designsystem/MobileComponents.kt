package app.tastile.android.ui.mobile.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * M3 ListItem wrapped for tappable rows. Replaces ad-hoc `Row.clickable{}`
 * patterns so every interactive row carries M3's built-in ripple, min-height,
 * and shape.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    ListItem(
        headlineContent = { Text(headline, style = MaterialTheme.typography.bodyLarge) },
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        supportingContent = supporting?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = leading?.let { { Icon(it, contentDescription = null) } },
        trailingContent = trailing?.let { { Icon(it, contentDescription = null) } },
        colors = if (selected) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else ListItemDefaults.colors(),
        tonalElevation = if (onClick != null) 1.dp else 0.dp,
    )
}

/** Primary CTA. Use at most once per surface. */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = Button(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }

/** Secondary CTA. Important secondary actions. */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = FilledTonalButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }

/** Tertiary CTA. Cancellation, alternate actions. */
@Composable
fun AppTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }

/** Quaternary CTA. Dismiss / "learn more" only. */
@Composable
fun AppDismissButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = TextButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }
