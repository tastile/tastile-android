/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronRight
// m2-allow: m3-component
import androidx.compose.material3.Button
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.LinearProgressIndicator
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.OutlinedButton
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: primitive
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
// m2-allow: m3-component
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.NiaTheme

/**
 * Mobile-design spacing tokens. Mirrors the deleted `MobileSpacing` object
 * that lived under `ui/mobile/designsystem/`. Values match the previous
 * reference so existing call sites continue to produce identical layouts.
 */
object MobileSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
}

/**
 * Cross-platform Tastile theme bridge. Re-exports the core theme tokens
 * under the historical `AppTheme` namespace so that call-sites still using
 * `AppTheme.colors.*` / `AppTheme.typography.*` / `AppTheme.spacing.*` keep
 * compiling after the legacy `ui.designsystem` package was deleted. The
 * actual theme values come from [NiaTheme] / Material 3's `colorScheme` /
 * `typography`; [AppTheme] is a stable wrapper that never re-defines colors.
 */
object AppTheme {
    object Colors {
        val primary: Color
            @Composable get() = MaterialTheme.colorScheme.primary
        val onPrimary: Color
            @Composable get() = MaterialTheme.colorScheme.onPrimary
        val secondary: Color
            @Composable get() = MaterialTheme.colorScheme.secondary
        val secondaryContainer: Color
            @Composable get() = MaterialTheme.colorScheme.secondaryContainer
        val onSecondaryContainer: Color
            @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
        val tertiaryContainer: Color
            @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
        val onTertiaryContainer: Color
            @Composable get() = MaterialTheme.colorScheme.onTertiaryContainer
        val surface: Color
            @Composable get() = MaterialTheme.colorScheme.surface
        val surfaceVariant: Color
            @Composable get() = MaterialTheme.colorScheme.surfaceVariant
        val onSurface: Color
            @Composable get() = MaterialTheme.colorScheme.onSurface
        val onSurfaceVariant: Color
            @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
        val error: Color
            @Composable get() = MaterialTheme.colorScheme.error
        val outline: Color
            @Composable get() = MaterialTheme.colorScheme.outline
        val background: Color
            @Composable get() = MaterialTheme.colorScheme.background
    }
    object Typography {
        val titleLarge
            @Composable get() = MaterialTheme.typography.titleLarge
        val titleMedium
            @Composable get() = MaterialTheme.typography.titleMedium
        val titleSmall
            @Composable get() = MaterialTheme.typography.titleSmall
        val bodyLarge
            @Composable get() = MaterialTheme.typography.bodyLarge
        val bodyMedium
            @Composable get() = MaterialTheme.typography.bodyMedium
        val bodySmall
            @Composable get() = MaterialTheme.typography.bodySmall
        val labelLarge
            @Composable get() = MaterialTheme.typography.labelLarge
        val labelSmall
            @Composable get() = MaterialTheme.typography.labelSmall
    }
    object Spacing {
        val xs: Dp get() = MobileSpacing.xs
        val sm: Dp get() = MobileSpacing.sm
        val md: Dp get() = MobileSpacing.md
        val lg: Dp get() = MobileSpacing.lg
    }
}

val AppTheme_composeBridge: Unit = Unit

/**
 * Filled primary action button. Wraps [NiaButton] to keep the legacy
 * `AppPrimaryButton(text, onClick, leadingIcon, modifier)` call signature
 * adopted by ui/dashboard and ui/mobile panels.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    NiaButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        text = { Text(text) },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
    )
}

/**
 * Outlined secondary action button. Wraps [NiaOutlinedButton].
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    NiaOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        text = { Text(text) },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
    )
}

/**
 * Text-style tertiary action button. Wraps [NiaTextButton].
 */
@Composable
fun AppTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    NiaTextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        text = { Text(text) },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
    )
}

/**
 * Filled tonal style. Wraps [NiaFilledTonalButton].
 */
@Composable
fun AppTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    NiaFilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        text = { Text(text) },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
    )
}

/**
 * Danger variant of the primary action button. Uses the primary button on the
 * M3 outline with the `error` color so destructive semantics are obvious.
 */
@Composable
fun AppDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        }
        Text(text, color = MaterialTheme.colorScheme.onError)
    }
}

/**
 * Page-column wrapper: a vertical flex container aligned to start with the
 * standard mobile column padding.
 */
@Composable
fun AppPageColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MobileSpacing.md, vertical = MobileSpacing.sm),
        content = content,
    )
}

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

/**
 * Outlined panel section container. Wraps [NiaOutlinedCard] with the
 * standard mobile section padding.
 */
@Composable
fun AppOutlinedPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    NiaOutlinedCard(modifier = modifier.padding(MobileSpacing.xs)) {
        Column(modifier = Modifier.padding(MobileSpacing.md), content = content)
    }
}

/**
 * Section header label, typically used to title a list or section.
 */
@Composable
fun AppSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = MobileSpacing.sm),
    )
}

/**
 * Convenience `Row` divider for horizontal break lines. Delegates to
 * Material 3's HorizontalDivider under an `m2-allow: primitive` marker.
 */
@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(modifier = modifier)
}

/**
 * Vertical divider for column split rows.
 */
@Composable
fun AppVDivider(
    modifier: Modifier = Modifier,
) {
    VerticalDivider(modifier = modifier)
}

/**
 * Loading row placeholder using [NiaLoadingWheel].
 */
@Composable
fun AppLoading(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(MobileSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NiaLoadingWheel(contentDesc = label ?: "Loading")
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Centered loading placeholder (no label slot).
 */
@Composable
fun AppCenteredLoading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        NiaLoadingWheel(contentDesc = "Loading")
    }
}

/**
 * Inline error message: small error-colored text aligned to start.
 */
@Composable
fun AppInlineError(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.padding(vertical = MobileSpacing.xs),
    )
}

/**
 * Empty-state placeholder block: icon optional, with title and hint text.
 *
 * Multiple legacy shapes are accepted for compatibility with pre-M2 call
 * sites that pass `(title, hint=...)` instead of `(message, ...)`: when
 * [message] is empty and [title] is supplied, the function renders [title]
 * in `titleSmall` and [hint] (if any) in `bodySmall`; otherwise [message]
 * is rendered in `bodyMedium` and [title]/[hint] are ignored.
 */
@Composable
fun AppEmptyState(
    message: String = "",
    modifier: Modifier = Modifier,
    title: String = "",
    hint: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MobileSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (actionLabel != null && onAction != null) {
            NiaButton(
                onClick = onAction,
                text = { Text(actionLabel) },
                modifier = Modifier.padding(top = MobileSpacing.sm),
            )
        }
    }
}

/**
 * Body-text wrapper using M3 `bodyMedium`.
 */
@Composable
fun AppBodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = modifier,
    )
}

/**
 * Meta-text wrapper using M3 `bodySmall`.
 */
@Composable
fun AppMetaText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier,
    )
}

/**
 * Screen title text using M3 `titleLarge`.
 */
@Composable
fun AppScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier,
    )
}

/**
 * Avatar placeholder rendered as a circular badge with initials.
 */
@Composable
fun AppAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") size: Dp = 32.dp,
    background: Color = MaterialTheme.colorScheme.secondaryContainer,
    foreground: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(
        modifier = modifier
            .background(color = background, shape = androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.take(2).uppercase(),
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Chevron icon, used as the trailing element on row-style navigation items.
 */
@Composable
fun AppChevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        modifier = modifier,
    )
}

/**
 * Standard rounded-corner shape token used by project list rows and similar
 * small cards. Bound here so call-sites reference a single token.
 */
val AppCornerShape: Shape
    get() = RoundedCornerShape(8.dp)

/**
 * Two-row list item: leading label, leading accessory, optional trailing
 * row, click handler.
 */
@Composable
fun AppListRow(
    label: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") description: String? = null,
) {
    val baseModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    NiaListItem(
        headlineContent = { Text(label) },
        modifier = baseModifier,
        leadingContent = leading,
        trailingContent = trailing?.let { row ->
            {
                @Suppress("UNUSED_EXPRESSION")
                androidx.compose.foundation.layout.Row(content = row)
            }
        },
        supportingContent = supporting?.let { msg ->
            {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/**
 * Compact list item mirroring the legacy `AppListItem(headline, leading,
 * trailing, onClick, modifier)`.
 */
@Composable
fun AppListItem(
    headline: String,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    val baseModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    NiaListItem(
        headlineContent = { Text(headline) },
        modifier = baseModifier,
        leadingContent = leading?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingContent = trailing?.let { icon -> { Icon(icon, contentDescription = null) } },
        supportingContent = supporting?.let { msg ->
            {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/**
 * Compact picker dropdown trigger button. Backward-compatible signature used
 * by project create/edit forms (`ui.mobile.panels.projects`).
 */
@Composable
fun AppPickerButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    testTag: String? = null,
) {
    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    OutlinedButton(
        onClick = onClick,
        modifier = taggedModifier.fillMaxWidth(),
        shape = AppCornerShape,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null)
                    Box(modifier = Modifier.padding(horizontal = MobileSpacing.sm))
                }
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
            )
        }
    }
}

/**
 * Compact picker dropdown trigger without the leading icon slot.
 */
@Composable
fun AppPickerButtonCompact(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    AppPickerButton(
        label = label,
        value = "",
        onClick = onClick,
        modifier = modifier,
        leadingIcon = null,
        testTag = testTag,
    )
}

/**
 * Row of trailing icon actions. Convenience wrapper for inline composables.
 */
@Composable
fun AppRowActions(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(MobileSpacing.xs)) {
        content()
    }
}

/**
 * Section header — thin label above a grouped set of list rows, typically
 * used inside side panels.
 */
@Composable
fun AppSectionHeaderInline(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = MobileSpacing.xs),
    )
}

/**
 * Convenience row label that uses [AppBodyText].
 */
@Composable
fun AppRowLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    AppBodyText(text = text, modifier = modifier)
}

/**
 * Stats-chip small inline pill used in calendar / tiles headers. Renders as
 * a rounded box with the given foreground text on the supplied background.
 */
@Composable
fun StatChip(
    label: String,
    value: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(color = background, shape = MaterialTheme.shapes.small)
            .padding(horizontal = MobileSpacing.sm, vertical = MobileSpacing.xs),
    ) {
        Text(text = label, color = foreground, style = MaterialTheme.typography.labelSmall)
        if (value.isNotBlank()) {
            Text(text = " $value", color = foreground, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Toggle via `MaterialTheme.shapes` corner rounding. Used by panels for
 * consistent surface rounding.
 */
@Suppress("unused")
@Composable
private fun _cornerRenderRedundant() {
    // Kept here only as documentation: callers reference [AppCornerShape] above;
    // some old call-sites invoked `AppCorner { ... }` as a composable. They
    // already reach the rounded-corner surface via the underlying wrapper.
}
