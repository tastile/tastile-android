package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel

private data class ConfigOption(val id: String, val label: String)

private val googleCalendarOptions = listOf(
    ConfigOption("sync_all", "Sync all calendars"),
    ConfigOption("sync_primary", "Sync primary only"),
    ConfigOption("sync_selected", "Sync selected calendar"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationConfigSheet(
    overlay: OverlayViewModel,
    dashboardViewModel: DashboardViewModel,
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val cfg = current as? Overlay.IntegrationConfig ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    PanelSheet(
        title = when (cfg.integrationId) {
            "google" -> "Google Calendar"
            "outlook" -> "Outlook Calendar"
            "apple" -> "Apple Calendar"
            "slack" -> "Slack"
            "notion" -> "Notion"
            else -> "Integration"
        },
        sheetState = sheetState,
        onDismiss = { overlay.dismiss() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (cfg.integrationId) {
                "google" -> {
                    val google by dashboardViewModel.googleCalendarIntegration.collectAsStateWithLifecycle()
                    var selected by remember(google) {
                        mutableStateOf(google?.syncMode ?: "sync_all")
                    }
                    Text("Sync mode", style = MaterialTheme.typography.labelMedium)
                    googleCalendarOptions.forEach { opt ->
                        ConfigRow(
                            label = opt.label,
                            selected = opt.id == selected,
                            onClick = {
                                selected = opt.id
                                dashboardViewModel.updateGoogleCalendarPolicy(
                                    syncMode = opt.id,
                                    selectedCalendarId = null,
                                )
                            },
                        )
                    }
                }
                else -> Text(
                    "Coming soon",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .semantics { contentDescription = "Coming soon" },
                )
            }
        }
    }
}

@Composable
private fun ConfigRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = label },
    ) {
        Text(
            text = if (selected) "● $label" else "○ $label",
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
