package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
            }
        }
    }
}