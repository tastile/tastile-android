package app.tastile.android.ui.mobile.panels.references

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.designsystem.AppSpacing

/**
 * Renders the per-label overlay rows for the mobile References side panel.
 *
 * One [Switch] row per unique label, additive (`Switch` mirrors the web
 * `<Toggleable>` which mutates the `enabled: string[]` set symmetrically).
 * The label list is passed in directly so the parent can compute it once
 * via `groupTilesByLabel(viewModel.tiles)` and avoid recomputing the
 * grouping inside each row.
 */
@Composable
fun ReferencesLabelList(
    labels: List<String>,
    enabled: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) {
        Text(
            text = stringResource(R.string.panels_references_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(horizontal = AppSpacing.md),
        )
        return
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        labels.forEach { label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = label in enabled,
                    onCheckedChange = { onToggle(label) },
                )
            }
        }
    }
}