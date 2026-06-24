package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var query by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { overlay.dismiss() },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Task 17 wires this to actual command execution.
                                overlay.dismiss()
                            }
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}
