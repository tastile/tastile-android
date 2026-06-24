package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
        ModalBottomSheet(
            onDismissRequest = { overlay.dismiss() },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = email.ifBlank { "Signed in" },
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalDivider()
                AccountMenuRow(label = "Account", onClick = { overlay.dismiss() })
                AccountMenuRow(
                    label = "Subscription",
                    onClick = { overlay.show(Overlay.SidePanel(SidePanelSection.Preferences)) },
                )
                AccountMenuRow(
                    label = "Memo",
                    onClick = { overlay.show(Overlay.SidePanel(SidePanelSection.Schedule)) },
                )
                AccountMenuRow(
                    label = "Prompt history",
                    onClick = { overlay.show(Overlay.SidePanel(SidePanelSection.References)) },
                )
                AccountMenuRow(
                    label = "Billing",
                    onClick = { overlay.show(Overlay.SidePanel(SidePanelSection.Preferences)) },
                )
                HorizontalDivider()
                AccountMenuRow(label = "Sign out", onClick = { overlay.dismiss() })
            }
        }
    }
}

@Composable
private fun AccountMenuRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                onClickLabel = "Open $label",
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) { },
        style = MaterialTheme.typography.bodyMedium,
    )
}