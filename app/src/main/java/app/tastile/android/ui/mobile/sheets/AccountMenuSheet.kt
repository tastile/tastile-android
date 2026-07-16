package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.WorkspacePremium
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.designsystem.AppListItem
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton

/**
 * Account dropdown mirroring the web account menu (`src/app/app/account-menu.tsx`).
 *
 * Composition (4 rows + email header):
 *  - Header   : `email.ifBlank { "Signed in" }`
 *  - Profile  → `Overlay.AccountSettings` (C7 sheet)
 *  - Subscription → `Overlay.Subscription` (C7 sheet)
 *  - Access Tokens → `Overlay.Tokens` (C7 sheet)
 *  - Sign out → `viewModel.signOut()` (gated by a confirm dialog)
 *
 * Memo / Prompt history / Billing are intentionally dropped — the web account
 * menu does not expose them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountMenuSheet(
    viewModel: DashboardViewModel = hiltViewModel(),
    overlay: OverlayViewModel,
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    var showSignOutConfirm by remember { mutableStateOf(false) }

    if (current is Overlay.AccountMenu) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        PanelSheet(
            title = email.ifBlank { stringResource(R.string.shell_account_signed_in) },
            sheetState = sheetState,
            onDismiss = { overlay.dismiss() },
        ) {
            AccountMenuRow(
                label = stringResource(R.string.nav_account_profile),
                icon = Icons.Outlined.AccountCircle,
                testTag = "account_menu_profile",
                onClick = {
                    overlay.dismiss()
                    overlay.show(Overlay.AccountSettings)
                },
            )
            AccountMenuRow(
                label = stringResource(R.string.nav_account_subscription),
                icon = Icons.Outlined.WorkspacePremium,
                testTag = "account_menu_subscription",
                onClick = {
                    overlay.dismiss()
                    overlay.show(Overlay.Subscription)
                },
            )
            AccountMenuRow(
                label = stringResource(R.string.nav_account_tokens),
                icon = Icons.Outlined.Key,
                testTag = "account_menu_tokens",
                onClick = {
                    overlay.dismiss()
                    overlay.show(Overlay.Tokens)
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            AccountMenuRow(
                label = stringResource(R.string.nav_account_sign_out),
                icon = Icons.AutoMirrored.Outlined.Logout,
                testTag = "account_menu_sign_out",
                onClick = { showSignOutConfirm = true },
            )
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(stringResource(R.string.nav_account_sign_out)) },
            text = { Text(stringResource(R.string.shell_account_sign_out_confirm)) },
            confirmButton = {
                AppPrimaryButton(
                    text = stringResource(R.string.nav_account_sign_out),
                    onClick = {
                        showSignOutConfirm = false
                        overlay.dismiss()
                        viewModel.signOut()
                    },
                )
            },
            dismissButton = {
                AppTertiaryButton(
                    text = stringResource(R.string.dashboard_tiles_delete_dialog_cancel),
                    onClick = { showSignOutConfirm = false },
                )
            },
        )
    }
}

@Composable
private fun AccountMenuRow(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
) {
    AppListItem(
        headline = label,
        leading = icon,
        trailing = Icons.Outlined.ChevronRight,
        onClick = onClick,
        modifier = Modifier.testTag(testTag),
    )
}
