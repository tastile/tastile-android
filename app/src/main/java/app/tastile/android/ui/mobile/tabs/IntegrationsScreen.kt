package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.designsystem.AppBodyText
import app.tastile.android.ui.designsystem.AppOutlinedPanel
import app.tastile.android.ui.designsystem.AppScreenTitle
import app.tastile.android.ui.designsystem.AppTheme

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
            .padding(AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppScreenTitle("Integrations")
        AppBodyText("Manage Google Calendar connections and sync.")

        AppOutlinedPanel {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppBodyText("Google Calendar")
                AppBodyText("This integration is outside the current Tastile v1 scope.")
                AppBodyText(
                    "Until v2, do not expect the dashboard to load or sync any integration state."
                )
            }
        }
    }
}
