package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Palette
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: m3-component
import androidx.compose.material3.Text
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
import app.tastile.android.core.designsystem.theme.LocalTastileCardRoleTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.mobile.sheets.QuickCreateProject

/** Web-parity color swatches (Event set). */
private val WebColorSwatches: List<Color> = listOf(
    Color(0xFF3B82F6),
    Color(0xFF10B981),
    Color(0xFFA855F7),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF6B7280),
)

/**
 * Project picker + color swatch row — directly mirrors
 * `tastile-web/src/features/create-tile/ui/sections/ProjectColorRow.tsx`.
 */
@Composable
fun ProjectColorRow(
    projects: List<QuickCreateProject>,
    selectedProjectId: String?,
    selectedColor: Color,
    onProjectChange: (String?) -> Unit,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "project-color-row",
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    FormRow(
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            ProjectChip(
                projects = projects,
                selectedProjectId = selectedProjectId,
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                onProjectChange = {
                    dropdownExpanded = false
                    onProjectChange(it)
                },
                testTag = testTag,
            )
        },
        trailing = {
            ColorSwatchRow(
                selected = selectedColor,
                onColorChange = onColorChange,
                testTag = testTag,
            )
        },
    )
}

@Composable
private fun ProjectChip(
    projects: List<QuickCreateProject>,
    selectedProjectId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onProjectChange: (String?) -> Unit,
    testTag: String,
) {
    val selected = projects.firstOrNull { it.id == selectedProjectId }
    Surface(
        modifier = Modifier
            .clickable { onExpandedChange(!expanded) }
            .testTag("$testTag-picker"),
        shape = RoundedCornerShape(50),
        color = LocalTastileCardRoleTokens.current.actionable.container,
        border = BorderStroke(1.dp, LocalTastileCardRoleTokens.current.completed.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = selected?.displayName ?: stringResource(R.string.quickcreate_panel_meta_no_project),
                style = MaterialTheme.typography.labelLarge,
                color = LocalContentColor.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("$testTag-picker-label"),
            )
        }
    }
    if (expanded) {
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(stringResource(R.string.quickcreate_panel_meta_no_project)) },
                onClick = { onProjectChange(null) },
            )
            projects.forEach { project ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(project.displayName) },
                    onClick = { onProjectChange(project.id) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    selected: Color,
    onColorChange: (Color) -> Unit,
    testTag: String,
) {
    var showCustom by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WebColorSwatches.forEach { swatch ->
            val isSelected = swatch == selected
            val swatchTag = "$testTag-color-${swatch.toSwatchId()}"
            Surface(
                onClick = { onColorChange(swatch) },
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) LocalTastileCardRoleTokens.current.actionable.border else LocalTastileCardRoleTokens.current.completed.border,
                ),
                modifier = Modifier
                    .size(24.dp)
                    .testTag(swatchTag),
            ) {
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(swatch),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
        Surface(
            onClick = { showCustom = true },
            shape = CircleShape,
            color = LocalTastileCardRoleTokens.current.actionable.container,
            border = BorderStroke(1.dp, LocalTastileCardRoleTokens.current.completed.border),
            modifier = Modifier
                .size(24.dp)
                .testTag("$testTag-color-custom"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
    if (showCustom) {
        CustomColorDialog(
            initial = selected,
            onApply = { color ->
                onColorChange(color)
                showCustom = false
            },
            onDismiss = { showCustom = false },
        )
    }
}

@Composable
private fun CustomColorDialog(
    initial: Color,
    onApply: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var hex by remember { mutableStateOf(initial.toHexString()) }
    val parsed = runCatching {
        val cleaned = hex.removePrefix("#")
        when (cleaned.length) {
            6 -> Color((0xFF000000u).toInt() or cleaned.toLong(16).toInt())
            8 -> Color(cleaned.toLong(16))
            else -> initial
        }
    }.getOrNull() ?: initial
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quickcreate_color_custom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { input ->
                            hex = input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == '#' }
                        },
                        label = { Text(stringResource(R.string.quickcreate_color_custom_hex)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("project-color-custom-hex"),
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parsed),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WebColorSwatches.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .clickable { hex = swatch.toHexString() },
                        )
                    }
                }
            }
        },
        confirmButton = {
            NiaButton(
                onClick = { onApply(parsed) },
                text = { Text(stringResource(R.string.quickcreate_panel_meta_apply)) },
            )
        },
        dismissButton = {
            NiaTextButton(
                onClick = onDismiss,
                text = { Text(stringResource(R.string.quick_create_cancel)) },
            )
        },
    )
}


