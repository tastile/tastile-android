package app.tastile.android.ui.mobile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.WorkspacePremium
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.dashboard.DashboardViewModel

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
 *
 * Anchored to the top-bar avatar trigger (managed by `MobileTopBar`); the
 * parent owns the `expanded` state and passes it plus `onDismiss` so the menu
 * participates in standard M3 outside-click dismissal.
 */
@Composable
fun AccountDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    overlay: OverlayViewModel = hiltViewModel(),
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    var showSignOutConfirm by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        Text(
            text = email.ifBlank { stringResource(R.string.shell_account_signed_in) },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics(mergeDescendants = false) { },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.nav_account_profile)) },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    Icons.Outlined.AccountCircle,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismiss()
                overlay.show(Overlay.AccountSettings)
            },
            modifier = Modifier.testTag("account_menu_profile"),
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.nav_account_subscription)) },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismiss()
                overlay.show(Overlay.Subscription)
            },
            modifier = Modifier.testTag("account_menu_subscription"),
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.nav_account_tokens)) },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Key,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismiss()
                overlay.show(Overlay.Tokens)
            },
            modifier = Modifier.testTag("account_menu_tokens"),
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.nav_account_sign_out)) },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                )
            },
            onClick = { showSignOutConfirm = true },
            modifier = Modifier.testTag("account_menu_sign_out"),
        )
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
                        onDismiss()
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
