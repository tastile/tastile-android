package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import app.tastile.android.core.designsystem.component.rememberNiaModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.mobile.EndpointsCatalog
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOverlaySheet(overlay: OverlayViewModel) {
    val current by overlay.current.collectAsStateWithLifecycle()

    if (current is Overlay.Search) {
        val sheetState = rememberNiaModalBottomSheetState()
        var query by remember { mutableStateOf("") }

        PanelSheet(
            sheetState = sheetState,
            onDismiss = { overlay.dismiss() },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                )
                val matches = EndpointsCatalog.entries.filter {
                    query.isBlank() ||
                        it.label.contains(query, ignoreCase = true) ||
                        it.operationId.contains(query, ignoreCase = true)
                }
                matches.take(8).forEach { entry ->
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                overlay.dismiss()
                            }
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}
