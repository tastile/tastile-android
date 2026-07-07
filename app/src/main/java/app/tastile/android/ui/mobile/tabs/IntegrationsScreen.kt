package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Integration
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.designsystem.AppLoading
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel

private data class IntegrationStub(val id: String, val name: String)

private val availableStubs = listOf(
    IntegrationStub("outlook", "Outlook Calendar"),
    IntegrationStub("apple", "Apple Calendar"),
    IntegrationStub("slack", "Slack"),
    IntegrationStub("notion", "Notion"),
)

@Composable
fun IntegrationsScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
) {
    val integrations by viewModel.integrations.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    if (loading && integrations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppLoading()
        }
        return
    }

    val connected = integrations.filter { it.connected }
    val scrollState = rememberScrollState()
    var disconnectCandidate by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        SectionLabel("Connected")
        if (connected.isEmpty()) {
            Text(
                "No integrations connected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AppTheme.spacing.xs),
            )
        }
        connected.forEach { integration ->
            ConnectedRow(
                integration = integration,
                onSync = { viewModel.syncGoogleCalendarNow() },
                onDisconnect = { disconnectCandidate = integration.id },
                onTap = { overlay.show(Overlay.IntegrationConfig(integration.id)) },
            )
        }

        SectionLabel("Available")
        availableStubs.forEach { stub ->
            AvailableRow(
                name = stub.name,
                onTap = { overlay.show(Overlay.IntegrationConfig(stub.id)) },
            )
        }
    }

    disconnectCandidate?.let { id ->
        AlertDialog(
            onDismissRequest = { disconnectCandidate = null },
            title = { Text("Disconnect Google Calendar?") },
            text = { Text("Existing synced events stay; new events won't sync.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.disconnectGoogleCalendar()
                    disconnectCandidate = null
                }) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { disconnectCandidate = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = AppTheme.spacing.sm),
    )
}

@Composable
private fun ConnectedRow(
    integration: Integration,
    onSync: () -> Unit,
    onDisconnect: () -> Unit,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onTap)
            .padding(vertical = AppTheme.spacing.xs)
            .semantics(mergeDescendants = true) { contentDescription = "${integration.name}: connected" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Text("●", style = MaterialTheme.typography.bodyMedium)
        Column(modifier = Modifier.weight(1f)) {
            Text(integration.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Connected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onSync) { Text("Sync now") }
        TextButton(onClick = onDisconnect) { Text("Disconnect") }
    }
}

@Composable
private fun AvailableRow(
    name: String,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onTap)
            .padding(vertical = AppTheme.spacing.xs)
            .semantics(mergeDescendants = true) { contentDescription = "$name: available" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Text("○", style = MaterialTheme.typography.bodyMedium)
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "›",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
