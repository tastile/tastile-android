package app.tastile.android.ui.mobile.tabs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.tastile.android.ui.dashboard.DashboardViewModel

@Composable fun ExecuteScreen(viewModel: DashboardViewModel) { Text("Execute") }
@Composable fun TilesScreen(viewModel: DashboardViewModel) { Text("Tiles") }
@Composable fun IntegrationsScreen(viewModel: DashboardViewModel) { Text("Integrations") }
@Composable fun SettingsScreen(viewModel: DashboardViewModel) { Text("Settings") }