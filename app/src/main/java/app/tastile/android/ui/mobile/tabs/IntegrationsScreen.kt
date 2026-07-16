package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.component.NiaOutlinedCard
import app.tastile.android.ui.dashboard.DashboardViewModel

/**
 * Integrations tab — v1 static notice.
 *
 * Mirrors `tastile-web/src/app/dashboard/integrations/page.tsx`. The legacy
 * Google Calendar sync surface lives outside v1 scope and there is no
 * daemon-backed loader that could populate an integration list, so the
 * tab renders an explicit notice instead of a row of cards.
 */
@Composable
fun IntegrationsScreen(viewModel: DashboardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Integrations",
            style = MaterialTheme.typography.titleLarge,
        )
        HorizontalDivider()
        Text(
            text = "Manage Google Calendar connections and sync.",
            style = MaterialTheme.typography.bodyMedium,
        )

        NiaOutlinedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Google Calendar",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "This integration is outside the current Tastile v1 scope.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Until v2, do not expect the dashboard to load or sync any integration state.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
