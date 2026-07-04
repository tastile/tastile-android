package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountMenuSheet(
    viewModel: DashboardViewModel = hiltViewModel(),
    overlay: OverlayViewModel,
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()

    if (current is Overlay.AccountMenu) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        PanelSheet(
            title = email.ifBlank { "Signed in" },
            sheetState = sheetState,
            onDismiss = { overlay.dismiss() },
        ) {
            PanelRow(
                label = "Account",
                icon = Icons.Outlined.AccountCircle,
                selected = false,
                role = Role.Button,
                onClick = { overlay.dismiss() },
            )
            PanelRow(
                label = "Subscription",
                icon = Icons.Outlined.BarChart,
                selected = false,
                role = Role.Button,
                onClick = { overlay.show(Overlay.SidePanel(SidePanelSection.Preferences)) },
            )
            PanelRow(
                label = "Memo",
                icon = Icons.Outlined.Description,
                selected = false,
                role = Role.Button,
                onClick = { overlay.show(Overlay.SidePanel(SidePanelSection.Schedule)) },
            )
            PanelRow(
                label = "Prompt history",
                icon = Icons.Outlined.History,
                selected = false,
                role = Role.Button,
                onClick = { overlay.show(Overlay.SidePanel(SidePanelSection.References)) },
            )
            PanelRow(
                label = "Billing",
                icon = Icons.Outlined.CreditCard,
                selected = false,
                role = Role.Button,
                onClick = { overlay.show(Overlay.SidePanel(SidePanelSection.Preferences)) },
            )
            Spacer(modifier = Modifier.weight(1f))
            PanelRow(
                label = "Sign out",
                icon = Icons.AutoMirrored.Outlined.Logout,
                selected = false,
                role = Role.Button,
                onClick = { overlay.dismiss() },
            )
        }
    }
}
