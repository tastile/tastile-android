package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection

@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel,
    overlay: OverlayViewModel = hiltViewModel(),
) {
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
    ) {
        SettingsRow(icon = "🌐", label = "Locale", value = localeLabel(locale), overlay = overlay)
        SettingsRow(icon = "🎨", label = "Theme", value = "gray", overlay = overlay)
        SettingsRow(icon = "🔔", label = "Notifications", value = "›", overlay = overlay)
        SettingsRow(icon = "🔒", label = "Privacy", value = "›", overlay = overlay)
        SettingsRow(icon = "ℹ", label = "About", value = "›", overlay = overlay)
    }
}

private fun localeLabel(l: AppLocale): String = when (l) {
    AppLocale.JA -> "ja"
    AppLocale.EN -> "en"
}

@Composable
private fun SettingsRow(
    icon: String,
    label: String,
    value: String,
    overlay: OverlayViewModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                overlay.show(Overlay.SidePanel(SidePanelSection.Preferences))
            }
            .padding(vertical = AppTheme.spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$icon $label", style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}