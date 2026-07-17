package app.tastile.android.ui.mobile.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.data.model.Tile
import app.tastile.android.ui.mobile.panels.references.ReferencesLabelList
import app.tastile.android.ui.dashboard.DashboardViewModel

/**
 * Section pane content for `SidePanelSection.References`.
 *
 * Mirrors `tastile-web/src/components/panels/ReferencesSidePanel.tsx`:
 *   1. Header label (`panels_references_title`).
 *   2. One `Switch` row per unique label from `groupTilesByLabel(tiles)`
 *      — additive, no other controls.
 *   3. Empty state when no tiles carry any label.
 *
 * Previously this section rendered 4 hardcoded URL rows (`Help`,
 * `Changelog`, `GitHub`, `Send feedback`). The web reference surface
 * is label-overlay toggles instead, so the mobile UI follows suit.
 * The hardcoded links were dropped on purpose — there is no
 * `panels_references_*` web string for them.
 */
@Composable
fun ReferencesSectionContent(
    modifier: Modifier = Modifier,
    dashboardViewModel: DashboardViewModel,
) {
    val tiles by dashboardViewModel.tiles.collectAsStateWithLifecycle()
    val enabled by dashboardViewModel.referenceOverlayEnabled.collectAsStateWithLifecycle()
    val labels = remember(tiles) { groupTilesByLabel(tiles).keys.toList() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.panels_references_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        ReferencesLabelList(
            labels = labels,
            enabled = enabled,
            onToggle = dashboardViewModel::toggleReference,
        )
    }
}

private fun groupTilesByLabel(tiles: List<Tile>): Map<String, List<Tile>> =
    tiles.flatMap { tile -> tile.labels.map { it to tile } }
        .groupBy({ it.first }, { it.second })