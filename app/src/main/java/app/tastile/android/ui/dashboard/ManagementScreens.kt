package app.tastile.android.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.core.designsystem.component.NiaOutlinedButton
import app.tastile.android.core.designsystem.component.NiaOutlinedCard
import app.tastile.android.data.model.Plan

@Composable
fun AccountDashboardScreen(viewModel: DashboardViewModel) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val statsDiagnostics by viewModel.statsDiagnostics.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val activeTab = remember { mutableStateOf("profile") }
    val total = tiles.size
    val completed = tiles.count { it.isDone() }
    val started = tiles.count { it.isStarted() }
    val ready = total - completed - started
    val completionRate = if (total == 0) 0 else (completed * 100) / total

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.dashboard_account_heading), style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                "profile" to stringResource(R.string.dashboard_account_tab_profile),
                "subscription" to stringResource(R.string.dashboard_account_tab_subscription),
                "statistics" to stringResource(R.string.dashboard_account_tab_statistics),
                "usage" to stringResource(R.string.dashboard_account_tab_usage)
            ).forEach { (key, label) ->
                if (activeTab.value == key) {
                    NiaFilledTonalButton(
                        text = { Text(label) },
                        onClick = { activeTab.value = key },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    NiaOutlinedButton(
                        text = { Text(label) },
                        onClick = { activeTab.value = key },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        when (activeTab.value) {
            "profile" -> NiaOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dashboard_account_profile_info))
                    Text(profile?.displayName ?: stringResource(R.string.dashboard_account_no_display_name))
                    Text(email)
                    NiaButton(
                        text = { Text(stringResource(R.string.dashboard_account_open_web)) },
                        onClick = { uriHandler.openUri("https://app.tastile.app/dashboard/account") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    NiaOutlinedButton(
                        text = { Text(stringResource(R.string.dashboard_account_sign_out)) },
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            "subscription" -> NiaOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dashboard_account_subscription_label))
                    Text(stringResource(R.string.dashboard_account_current_plan_label) + ": ${profile?.plan ?: Plan.FREE.value}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NiaButton(text = { Text(stringResource(R.string.dashboard_account_upgrade_pro)) }, onClick = { uriHandler.openUri("https://tastile.app/api/stripe/checkout") })
                        NiaOutlinedButton(text = { Text(stringResource(R.string.dashboard_account_manage_billing)) }, onClick = { uriHandler.openUri("https://tastile.app/api/stripe/portal") })
                    }
                }
            }

            "statistics" -> NiaOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.dashboard_account_tile_statistics))
                    Text(stringResource(R.string.dashboard_account_total) + ": $total")
                    Text(stringResource(R.string.dashboard_account_completed) + ": $completed")
                    Text(stringResource(R.string.dashboard_account_in_progress) + ": $started")
                    Text(stringResource(R.string.dashboard_account_ready) + ": $ready")
                    Text(stringResource(R.string.dashboard_account_completion_rate) + ": $completionRate%")
                    Text(stringResource(R.string.dashboard_account_diagnostics) + ": $statsDiagnostics")
                }
            }

            "usage" -> NiaOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dashboard_account_usage_dashboard))
                    Text(stringResource(R.string.dashboard_account_usage_coming_soon))
                    Text("• " + stringResource(R.string.dashboard_account_usage_tiles_over_time))
                    Text("• " + stringResource(R.string.dashboard_account_usage_completion_rate))
                    Text("• " + stringResource(R.string.dashboard_account_usage_focus_time))
                    Text("• " + stringResource(R.string.dashboard_account_usage_activity_heatmap))
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
