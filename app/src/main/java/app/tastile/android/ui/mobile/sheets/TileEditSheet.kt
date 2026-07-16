package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.data.model.TileLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileEditSheet(
    overlay: OverlayViewModel,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val tile by viewModel.selectedTile.collectAsStateWithLifecycle()

    if (current is Overlay.TileEdit) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        PanelSheet(
            title = tile?.title ?: "Tile",
            sheetState = sheetState,
            onDismiss = {
                viewModel.clearSelectedTile()
                overlay.dismiss()
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = tile?.lifecycle ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                (current as Overlay.TileEdit).placementId?.let { placementId ->
                    Text(
                        text = "Occurrence: $placementId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                tile?.let { selected ->
                    val lifecycle = TileLifecycle.fromString(selected.lifecycle)
                    if (lifecycle == TileLifecycle.READY) {
                        TextButton(onClick = { viewModel.startTile(selected.id) }) { Text("Start") }
                        TextButton(onClick = { viewModel.setDeferTileCandidate(selected.id) }) { Text("Defer") }
                        TextButton(onClick = { viewModel.setPromptTileCandidate(selected.id) }) { Text("Request prompt") }
                    }
                    if (lifecycle == TileLifecycle.STARTED) {
                        TextButton(onClick = { viewModel.completeTile(selected.id) }) { Text("Complete") }
                        TextButton(onClick = { viewModel.pauseTile(selected.id) }) { Text("Pause") }
                        TextButton(onClick = { viewModel.resumeTile(selected.id) }) { Text("Resume") }
                    }
                    TextButton(onClick = { viewModel.setDeleteTileCandidate(selected.id) }) { Text("Delete") }
                    TextButton(onClick = {
                        // Keep one canonical editing surface: Quick Create owns all
                        // field panels and is the only route used for editing entry.
                        overlay.show(Overlay.QuickCreate)
                    }) { Text("Edit details") }
                }
            }
        }
    }
}
