package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.ListItem
// m2-allow: m3-component
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel

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
            text = {
                Text(
                    stringResource(R.string.shell_account_sign_out_confirm),
                    modifier = Modifier.semantics(mergeDescendants = false) { },
                )
            },
            confirmButton = {
                NiaButton(
                    onClick = {
                        showSignOutConfirm = false
                        overlay.dismiss()
                        viewModel.signOut()
                    },
                    modifier = Modifier
                        .semantics(mergeDescendants = false) { }
                        .testTag("account_menu_sign_out_confirm"),
                    text = { Text(stringResource(R.string.nav_account_sign_out)) },
                )
            },
            dismissButton = {
                NiaTextButton(
                    onClick = { showSignOutConfirm = false },
                    modifier = Modifier
                        .semantics(mergeDescendants = false) { }
                        .testTag("account_menu_sign_out_cancel"),
                    text = { Text(stringResource(R.string.dashboard_tiles_delete_dialog_cancel)) },
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
    // Apply clickable + testTag directly to the ListItem (mirrors the working
    // pattern in `QuickCreateBasePanel.kt`'s essential card). Wrapping in an
    // outer Box breaks ModalBottomSheet click dispatch under Robolectric.
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}