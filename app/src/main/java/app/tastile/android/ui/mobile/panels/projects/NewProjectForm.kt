package app.tastile.android.ui.mobile.panels.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.designsystem.AppSpacing

/**
 * Inline "+ New" create form — mirrors the `<form>` block in
 * `tastile-web/src/components/panels/ProjectsSidePanel.tsx`.
 *
 * Web parity on labels:
 *   - name placeholder = "Project name"
 *   - slug placeholder = "slug (optional)"
 *   - color picker = presets (we render 5 swatches: web default grey +
 *     blue / green / red / amber). The web picker is an HTML5
 *     `<input type="color">` which is freeform; on Android we mirror the
 *     affordance without depending on the platform ColorPicker module.
 *   - parent-project dropdown is omitted on Android (web's parent
 *     selector drives the same flat list ordering web renders, which
 *     we already produce via `orderWorkspaceTree`).
 */
@Composable
fun NewProjectForm(
    busy: Boolean,
    errorText: String?,
    onSubmit: (name: String, slug: String?, color: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#6b7280") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(80) },
            placeholder = { Text(stringResource(R.string.panels_projects_name_placeholder)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("project-create-name"),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            OutlinedTextField(
                value = slug,
                onValueChange = { v ->
                    slug = v.lowercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(40)
                },
                placeholder = { Text(stringResource(R.string.panels_projects_slug_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("project-create-slug"),
            )
            ColorPickerSwatch(
                value = color,
                onChange = { color = it },
                modifier = Modifier.testTag("project-create-color"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Button(
                onClick = { onSubmit(name, slug.ifBlank { null }, color) },
                enabled = !busy && name.isNotBlank(),
                modifier = Modifier.testTag("project-create-submit"),
            ) {
                Text(
                    if (busy) stringResource(R.string.panels_projects_creating)
                    else stringResource(R.string.panels_projects_create),
                )
            }
            TextButton(onClick = onCancel, enabled = !busy) {
                Text(stringResource(R.string.panels_projects_cancel))
            }
        }
        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ColorPickerSwatch(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = listOf("#6b7280", "#3b82f6", "#10b981", "#ef4444", "#f59e0b")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        presets.forEach { hex ->
            val isSelected = hex.equals(value, ignoreCase = true)
            val dot = parseHexColor(hex) ?: MaterialTheme.colorScheme.outline
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onChange(hex) }
                    .padding(2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 24.dp else 20.dp)
                        .clip(CircleShape)
                        .background(dot)
                        .padding(if (isSelected) 4.dp else 0.dp),
                )
            }
        }
    }
}
