package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Integration
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.designsystem.AppLoading
import app.tastile.android.ui.designsystem.AppTheme

@Composable
fun IntegrationsScreen(viewModel: DashboardViewModel) {
    val integrations by viewModel.integrations.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    if (loading && integrations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppLoading()
        }
        return
    }

    val (connected, available) = integrations.partition { it.connected }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        connected.forEach { IntegrationRow(integration = it, glyph = "●") }
        available.forEach { IntegrationRow(integration = it, glyph = "○") }
    }
}

@Composable
private fun IntegrationRow(integration: Integration, glyph: String) {
    val action = if (integration.connected) "⚙" else "+"
    val status = if (integration.connected) "connected" else "available"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.xs)
            .semantics { contentDescription = "${integration.name}: $status" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$glyph ${integration.name}", style = MaterialTheme.typography.bodyMedium)
        Text(action, style = MaterialTheme.typography.bodyMedium)
    }
}