package app.tastile.android.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun AppScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = AppTheme.typography.titleLarge, modifier = modifier)
}

@Composable
fun AppSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.onSurfaceVariant
) {
    Text(
        text = text,
        style = AppTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

@Composable
fun AppBodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.onSurface
) {
    Text(text = text, style = AppTheme.typography.bodyMedium, color = color, modifier = modifier)
}

@Composable
fun AppMetaText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTheme.typography.bodySmall,
        color = AppTheme.colors.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AppTheme.component.buttonMinHeight),
        shape = AppTheme.shape.chip
    ) {
        Text(text)
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AppTheme.component.buttonMinHeight),
        shape = AppTheme.shape.chip
    ) {
        Text(text)
    }
}

@Composable
fun AppTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AppTheme.component.buttonMinHeight),
        shape = AppTheme.shape.chip,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colors.primary
        )
    ) {
        Text(text)
    }
}

@Composable
fun AppDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AppTheme.component.buttonMinHeight),
        shape = AppTheme.shape.chip,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colors.error
        )
    ) {
        Text(text)
    }
}

@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(AppTheme.component.iconButton)) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
fun AppOutlinedPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .border(1.dp, AppTheme.colors.outlineVariant, AppTheme.shape.panel)
            .clip(AppTheme.shape.panel)
            .background(AppTheme.colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            content = { content() }
        )
    }
}

@Composable
fun AppInlineError(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = AppTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
    )
}

@Composable
fun AppLoading(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier)
}

@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier)
}

@Composable
fun AppRowActions(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun AppAvatar(
    imageUrl: String?,
    fallbackText: String,
    modifier: Modifier = Modifier
) {
    val avatarSize = AppTheme.component.avatar
    val shape = CircleShape
    val hasImage = !imageUrl.isNullOrBlank()
    if (hasImage) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Avatar",
            modifier = modifier
                .size(avatarSize)
                .clip(shape)
                .border(1.dp, AppTheme.colors.outlineVariant, shape)
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .size(avatarSize)
                .clip(shape)
                .background(AppTheme.colors.primaryContainer)
                .border(1.dp, AppTheme.colors.outlineVariant, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText.take(1).uppercase(),
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.onPrimaryContainer
            )
        }
    }
}
