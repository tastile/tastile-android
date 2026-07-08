package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.designsystem.AppComponentSize
import app.tastile.android.ui.designsystem.AppListRow
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
            AccountMenuRow("Account", Icons.Outlined.AccountCircle) { overlay.dismiss() }
            AccountMenuRow("Subscription", Icons.Outlined.BarChart) {
                overlay.show(Overlay.SidePanel(SidePanelSection.Preferences))
            }
            AccountMenuRow("Memo", Icons.Outlined.Description) {
                overlay.show(Overlay.SidePanel(SidePanelSection.Schedule))
            }
            AccountMenuRow("Prompt history", Icons.Outlined.History) {
                overlay.show(Overlay.SidePanel(SidePanelSection.References))
            }
            AccountMenuRow("Billing", Icons.Outlined.CreditCard) {
                overlay.show(Overlay.SidePanel(SidePanelSection.Preferences))
            }
            Spacer(modifier = Modifier.weight(1f))
            AccountMenuRow("Sign out", Icons.AutoMirrored.Outlined.Logout) { overlay.dismiss() }
        }
    }
}

@Composable
private fun AccountMenuRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    AppListRow(
        label = label,
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AppComponentSize.listRowGlyphSize),
            )
        },
        role = Role.Button,
        onClick = onClick,
    )
}