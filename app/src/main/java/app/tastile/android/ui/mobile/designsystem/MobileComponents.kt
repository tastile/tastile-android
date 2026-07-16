package app.tastile.android.ui.mobile.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
// m2-allow: m3-component
import androidx.compose.material3.AssistChip
// m2-allow: m3-component
import androidx.compose.material3.AssistChipDefaults
// m2-allow: m3-component
import androidx.compose.material3.Button
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.FilledTonalButton
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.ListItem
// m2-allow: m3-component
import androidx.compose.material3.ListItemDefaults
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedButton
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
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
    leadingIcon: ImageVector? = null,
) = Button(onClick = onClick, modifier = modifier, enabled = enabled) {
    if (leadingIcon != null) {
        Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(MobileSpacing.xs))
    }
    Text(text)
}

/** Secondary CTA. Important secondary actions. */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) = FilledTonalButton(onClick = onClick, modifier = modifier, enabled = enabled) {
    if (leadingIcon != null) {
        Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(MobileSpacing.xs))
    }
    Text(text)
}

/** Tertiary CTA. Cancellation, alternate actions. */
@Composable
fun AppTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) = OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
    if (leadingIcon != null) {
        Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(MobileSpacing.xs))
    }
    Text(text)
}

/** Quaternary CTA. Dismiss / "learn more" only. */
@Composable
fun AppDismissButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) = TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
    if (leadingIcon != null) {
        Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(MobileSpacing.xs))
    }
    Text(text)
}

/**
 * Button-shaped input. Replaces OutlinedTextField for constrained values
 * (time / date / reference). Click opens a picker sheet via [onClick].
 */
@Composable
fun AppPickerButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = MobileSpacing.lg, vertical = MobileSpacing.md),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Box(Modifier.size(MobileSpacing.sm))
                }
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
    }
}

/** Compact pill-shaped picker button for the top bar. Carries built-in ripple. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerButtonCompact(
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
            modifier = Modifier.padding(horizontal = MobileSpacing.md, vertical = MobileSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/** Section header with divider underline. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = MobileSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(modifier = Modifier.padding(top = MobileSpacing.xs))
    }
}

/** Coloured stat chip — replaces wall-of-text summary strings. */
@Composable
fun StatChip(
    label: String,
    value: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = {},
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = foreground)
                Text(" · ", color = foreground)
                Text(value, style = MaterialTheme.typography.labelLarge, color = foreground)
            }
        },
        colors = AssistChipDefaults.assistChipColors(containerColor = background, labelColor = foreground),
        modifier = modifier,
    )
}

/** Empty state — icon + title + hint. */
@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(MobileSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Box(Modifier.size(MobileSpacing.md))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Box(Modifier.size(MobileSpacing.xs))
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Vertical single-select list. Each option is a tappable [AppListItem]; the
 * selected row is highlighted and shows a trailing check. Unlike a full-width
 * [SingleChoiceSegmentedButtonRow] crammed with many long-labelled items, this
 * never overflows the screen width — it grows downward instead.
 */
@Composable
fun <T> AppSelectList(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    leading: (T) -> ImageVector? = { null },
    testTag: (T) -> String? = { null },
) {
    Column(modifier.fillMaxWidth()) {
        options.forEach { option ->
            val isSelected = option == selected
            val rowModifier = testTag(option)?.let { Modifier.testTag(it) } ?: Modifier
            AppListItem(
                headline = label(option),
                leading = leading(option),
                trailing = if (isSelected) Icons.Outlined.Check else null,
                selected = isSelected,
                onClick = { onSelect(option) },
                modifier = rowModifier,
            )
        }
    }
}

/**
 * Compact weekday multi-select: seven single-character toggle chips sharing the
 * row width equally. Single characters never overflow, so this stays legible on
 * a phone where a word-labelled 7-item segmented row would collapse.
 */
@Composable
fun AppWeekdayPicker(
    selectedMask: Int,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: (Int) -> String? = { null },
) {
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
    ) {
        labels.forEachIndexed { bit, char ->
            val isSelected = (selectedMask shr bit) and 1 == 1
            val chipModifier = Modifier.weight(1f).then(testTag(bit)?.let { Modifier.testTag(it) } ?: Modifier)
            FilterChip(
                selected = isSelected,
                enabled = enabled,
                onClick = { onToggle(bit) },
                label = { Text(char, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                modifier = chipModifier,
            )
        }
    }
}

/**
 * Numeric text field: number keyboard, single line, optional trailing unit.
 * Replaces the bare [OutlinedTextField] used for minutes / offset / mask / state
 * inputs (which defaulted to a text keyboard) and folds the old dangling unit
 * labels (e.g. a separate `Text("minutes")`) into the [suffix].
 */
@Composable
fun AppNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        suffix = suffix?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
    )
}
