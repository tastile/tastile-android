package app.tastile.android.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.mobile.designsystem.MobileTokens
import coil.compose.AsyncImage

// ─────────────────────────────────────────────────────────────────────────────
// Typography
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = AppTheme.typography.titleLarge, modifier = modifier)
}

@Composable
fun AppSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.onSurfaceVariant,
) {
    Text(
        text = text,
        style = AppTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun AppBodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.onSurface,
) {
    Text(text = text, style = AppTheme.typography.bodyMedium, color = color, modifier = modifier)
}

@Composable
fun AppMetaText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTheme.typography.bodySmall,
        color = AppTheme.colors.onSurfaceVariant,
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header — used inside a scroll container to label a group of rows.
// Centralises the inter-section break + labelSmall + onSurfaceVariant triple
// so every screen reads each group as a distinct block.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppSectionHeader(text: String, modifier: Modifier = Modifier) {
    AppSectionTitle(
        text = text,
        modifier = modifier.padding(top = ScreenPadding.interSection),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Page scaffolding — the common scrollable Column every tab sits inside.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a [Column] pre-configured with the page-level padding, vertical
 * arrangement, and (optional) [rememberScrollState]. Use this as the root of
 * every tab so the canvas framing (margins + inter-item spacing) is identical
 * across screens.
 */
@Composable
fun AppPageColumn(
    scrollable: Boolean = true,
    bottomInset: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .let { if (scrollable) it.verticalScroll(scroll) else it }
            .padding(
                start = ScreenPadding.horizontal,
                end = ScreenPadding.horizontal,
                top = ScreenPadding.top,
                bottom = if (bottomInset) AppTheme.spacing.md else 0.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(ScreenPadding.interItem),
        content = content,
    )
}

/**
 * Root for tabs that need a FAB / sticky-bottom widget anchored over the
 * scroll area. Renders an `AppPageColumn` and exposes the [BoxScope] so the
 * caller can `align(Alignment.BottomEnd)` the FAB.
 */
@Composable
fun AppPageWithOverlay(
    scrollable: Boolean = true,
    bottomInset: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AppPageColumn(scrollable = scrollable, bottomInset = bottomInset, content = content)
        overlay()
    }
}

/** Page-level loading state (centered spinner, full-bleed). */
@Composable
fun AppCenteredLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AppLoading()
    }
}

/** Empty-state container — center-aligned message + optional action. */
@Composable
fun AppEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            message,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            AppTonalButton(text = actionLabel, onClick = onAction)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Buttons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AppTheme.component.buttonMinHeight),
        shape = AppCorner.mediumShape,
    ) {
        Text(text)
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AppTheme.component.buttonMinHeight),
        shape = AppCorner.mediumShape,
    ) {
        Text(text)
    }
}

@Composable
fun AppTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AppTheme.component.buttonMinHeight),
        shape = AppCorner.mediumShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colors.primary,
        ),
    ) {
        Text(text)
    }
}

@Composable
fun AppDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AppTheme.component.buttonMinHeight),
        shape = AppCorner.mediumShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colors.error,
        ),
    ) {
        Text(text)
    }
}

@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.size(AppTheme.component.iconButton)) {
        Icon(icon, contentDescription = contentDescription)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panels / surfaces
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppOutlinedPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .border(1.dp, AppTheme.colors.outlineVariant, AppCorner.smallShape)
            .clip(AppCorner.smallShape)
            .background(AppTheme.colors.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            content = { content() },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// List row — the canonical clickable row used by every tab.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Single tappable list row. Designed as an **inset card**: no internal
 * horizontal padding — the parent (`AppPageColumn`, `PanelSheet`) supplies
 * the page-edge margin (16dp). The row itself reserves vertical padding for
 * content breathing and the standard minimum tap height. Background and
 * rounded corners are visible all around (Material 3 list-item style).
 *
 * Pass any leading widget (icon, glyph, dot) and any trailing widget
 * (chevron, meta label, button).
 */
@Composable
fun AppListRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    role: Role = Role.Button,
    description: String? = null,
) {
    val colors = AppTheme.colors
    val containerAlpha =
        if (selected) MobileTokens.SurfaceAlpha.strongSelected
        else MobileTokens.SurfaceAlpha.subtle
    val contentAlpha = if (selected) colors.onSurface else colors.onSurfaceVariant
    val resolvedDescription: String =
        description ?: if (meta.isNullOrBlank()) label else "$label: $meta"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppCorner.smallShape)
            .background(colors.surface.copy(alpha = containerAlpha))
            .clickable(role = role, onClick = onClick)
            .heightIn(min = AppComponentSize.listRowMinHeight)
            .padding(vertical = AppTheme.spacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = resolvedDescription
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        if (leading != null) leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = AppTheme.typography.bodyMedium,
                color = if (selected) colors.onSurface else contentAlpha,
            )
            if (!meta.isNullOrBlank()) {
                Text(
                    text = meta,
                    style = AppTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/** Standard chevron used as a trailing slot hint for rows that drill in. */
@Composable
fun AppChevron(modifier: Modifier = Modifier) {
    Text(
        text = "›",
        style = AppTheme.typography.bodyMedium,
        color = AppTheme.colors.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Round status dot used by integrations (●/○). */
@Composable
fun AppStatusDot(connected: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = if (connected) "●" else "○",
        style = AppTheme.typography.bodyMedium,
        color = if (connected) MobileTokens.Status.started else AppTheme.colors.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Same dot as [AppStatusDot] but uses the *primary* status color when
 *  connected (used for the in-row leading glyph). */
@Composable
fun AppPrimaryDot(modifier: Modifier = Modifier) {
    Text(
        text = "●",
        style = AppTheme.typography.bodyMedium,
        color = MobileTokens.Status.primary,
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Misc
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppInlineError(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = AppTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier,
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
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        content = content,
    )
}

@Composable
fun AppAvatar(
    imageUrl: String?,
    fallbackText: String,
    modifier: Modifier = Modifier,
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
                .border(1.dp, AppTheme.colors.outlineVariant, shape),
        )
    } else {
        Box(
            modifier = modifier
                .size(avatarSize)
                .clip(shape)
                .background(AppTheme.colors.primaryContainer)
                .border(1.dp, AppTheme.colors.outlineVariant, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fallbackText.take(1).uppercase(),
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.onPrimaryContainer,
            )
        }
    }
}
