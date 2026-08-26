/*
 * IdentitySubpanel.kt
 *
 * Authoring UI for [QuickCreateTileKind] identity metadata: description,
 * color swatch and visual icon. Sibling to the [TaskSubpanel] / [EventSubpanel]
 * source-tile envelopes.
 *
 * Rows use the 24dp icon column + 16dp gap reservation provided
 * structurally by `FormFieldLayout`, or bypass it entirely via
 * `ScrollableChipRow` for chrome-less chip batches. The 16dp outer
 * horizontal padding is applied by `FormFieldRow` / `ScrollableChipRow`.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Star
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore

@Composable
internal fun IdentityPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val identity = draft.identity
    LocalSectionHeader(title = stringResource(R.string.quick_create_identity_title))
    FormFieldLayout(icon = Icons.Outlined.Description) {
        UnderlineTextArea(
            value = identity.description.orEmpty(),
            onValueChange = { store.updateIdentity(identity.copy(description = it.ifBlank { null })) },
            placeholder = stringResource(R.string.quick_create_description),
            minLines = 2,
            maxLines = 6,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-description"),
        )
    }
    LocalSectionHeader(title = stringResource(R.string.quick_create_color))
    val colors = listOf("#3b82f6", "#8b5cf6", "#ec4899", "#ef4444", "#f59e0b", "#10b981", "#06b6d4", "#6b7280")
    ScrollableChipRow(spacing = 8.dp) {
        colors.forEach { color ->
            SwatchChip(
                color = Color(color.toColorInt()),
                selected = identity.visual.color.equals(color, ignoreCase = true),
                onClick = { store.updateIdentity(identity.copy(visual = identity.visual.copy(color = color))) },
                modifier = Modifier.testTag("quick-create-color-${color.removePrefix("#")}"),
            )
        }
    }
    LocalSectionHeader(title = stringResource(R.string.quick_create_icon))
    val icons = listOf("check-circle", "calendar", "clock", "repeat", "flag", "star")
    val iconVector: (String) -> androidx.compose.ui.graphics.vector.ImageVector = { icon -> when (icon) {
        "check-circle" -> Icons.Outlined.CheckCircle
        "calendar" -> Icons.Outlined.CalendarMonth
        "clock" -> Icons.Outlined.AccessTime
        "repeat" -> Icons.Outlined.Repeat
        "flag" -> Icons.Outlined.Flag
        else -> Icons.Outlined.Star
    } }
    ScrollableChipRow(spacing = 8.dp) {
        icons.forEach { icon ->
            IconChip(
                icon = iconVector(icon),
                selected = identity.visual.icon == icon,
                onClick = { store.updateIdentity(identity.copy(visual = identity.visual.copy(icon = icon))) },
                modifier = Modifier.testTag("quick-create-icon-$icon"),
            )
        }
    }
    var showIconPicker by remember { mutableStateOf(false) }
    FormFieldLayout(icon = Icons.Outlined.Edit) {
        LocalPickerField(
            label = stringResource(R.string.quick_create_icon_custom),
            value = identity.visual.icon,
            onClick = { showIconPicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-icon-custom"),
        )
    }
    if (showIconPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showIconPicker = false },
            title = { Text(stringResource(R.string.quickcreate_icon_picker_title)) },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    icons.forEach { icon ->
                        IconChip(
                            icon = iconVector(icon),
                            selected = identity.visual.icon == icon,
                            onClick = {
                                store.updateIdentity(identity.copy(visual = identity.visual.copy(icon = icon)))
                                showIconPicker = false
                            },
                            modifier = Modifier.testTag("quick-create-icon-picker-$icon"),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                NiaTextButton(
                    onClick = { showIconPicker = false },
                    leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                    text = { Text(stringResource(R.string.quick_create_cancel)) },
                )
            },
        )
    }
}
