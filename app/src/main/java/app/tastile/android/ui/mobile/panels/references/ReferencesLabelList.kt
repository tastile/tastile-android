package app.tastile.android.ui.mobile.panels.references

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.Switch
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R

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
            modifier = modifier.padding(horizontal = 12.dp),
        )
        return
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEach { label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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