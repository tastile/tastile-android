package app.tastile.android.ui.mobile.account

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: m3-component
import androidx.compose.material3.ListItem
// m2-allow: m3-component
import androidx.compose.material3.ListItemDefaults
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import app.tastile.android.core.designsystem.component.rememberNiaModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaLoadingWheel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.sheets.PanelSheet

/**
 * Profile / account panel — mirrors the `preferences.account.*` panel
 * from `tastile-web/src/app/dashboard/preferences/account/page.tsx`:
 *   1. Profile heading + guide.
 *   2. Account panel: refresh icon, email row, verification status, sub.
 *   3. Change email panel: new email + Send code + verify code.
 *   4. Login methods panel: Passkey / Email OTP re-login.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
    overlay: OverlayViewModel,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    if (current !is Overlay.AccountSettings) return
    val sheetState = rememberNiaModalBottomSheetState()

    PanelSheet(
        sheetState = sheetState,
        onDismiss = { overlay.dismiss() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.preferences_account_title),
                style = MaterialTheme.typography.titleLarge,
            )
            AccountSheetBody(viewModel = viewModel, overlay = overlay)
        }
    }
}

@Composable
internal fun AccountSheetBody(
    viewModel: AccountViewModel,
    overlay: OverlayViewModel,
) {
    val state by viewModel.profile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileHeading()
        AccountPanel(state = state, onRefresh = viewModel::loadProfile)
        ChangeEmailPanel(
            state = state,
            onPendingEmailChange = viewModel::updatePendingEmail,
            onSendCode = viewModel::sendEmailCode,
            onCodeChange = viewModel::updateVerificationCode,
            onVerify = viewModel::verifyEmailCode,
        )
        LoginMethodsPanel()
    }
}

@Composable
private fun ProfileHeading() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.preferences_account_profile_heading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.preferences_account_profile_guide),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountPanel(
    state: AccountViewModel.ProfileState,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SectionHeader(
                title = stringResource(R.string.preferences_account_account_heading),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.preferences_account_refresh),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.loading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("account-sheet-loading"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NiaLoadingWheel(
                    contentDesc = stringResource(R.string.preferences_account_loading),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.preferences_account_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }
        val profile = state.profile
        EmailRow(email = profile?.email.orEmpty())
        EmailVerificationRow(verified = profile?.emailVerified == true)
        AccountIdRow(
            sub = profile?.sub ?: profile?.username.orEmpty(),
            fallback = profile?.username.orEmpty(),
        )
    }
}

@Composable
private fun EmailRow(email: String) {
    AccountListItem(
        headline = stringResource(R.string.preferences_account_email),
        supporting = email.ifBlank { "-" },
        leading = Icons.Outlined.Mail,
        onClick = null,
    )
}

@Composable
private fun EmailVerificationRow(verified: Boolean) {
    AccountListItem(
        headline = stringResource(R.string.preferences_account_email_verified),
        supporting = stringResource(
            if (verified) R.string.preferences_account_verified
            else R.string.preferences_account_unverified,
        ),
        leading = Icons.Outlined.Verified,
        onClick = null,
    )
}

@Composable
private fun AccountIdRow(sub: String, fallback: String) {
    val display = sub.takeIf { it.isNotBlank() } ?: fallback.takeIf { it.isNotBlank() } ?: "-"
    AccountListItem(
        headline = stringResource(R.string.preferences_account_account_id),
        supporting = display,
        leading = Icons.Outlined.AccountCircle,
        onClick = null,
    )
}

@Composable
private fun ChangeEmailPanel(
    state: AccountViewModel.ProfileState,
    onPendingEmailChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionHeader(title = stringResource(R.string.preferences_account_change_email_heading))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.pendingEmail,
                onValueChange = onPendingEmailChange,
                label = { Text(stringResource(R.string.preferences_account_new_email)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            NiaButton(
                onClick = onSendCode,
                enabled = !state.submitting && state.pendingEmail.isNotBlank(),
                text = { Text(stringResource(R.string.preferences_account_send_code)) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.verificationCode,
                onValueChange = onCodeChange,
                label = { Text(stringResource(R.string.preferences_account_code)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            NiaButton(
                onClick = onVerify,
                enabled = !state.submitting && state.verificationCode.isNotBlank(),
                text = { Text(stringResource(R.string.preferences_account_verify_code)) },
            )
        }
    }
}

@Composable
private fun LoginMethodsPanel() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionHeader(title = stringResource(R.string.preferences_account_login_methods))
        AccountListItem(
            headline = stringResource(R.string.preferences_account_passkey),
            supporting = null,
            leading = Icons.Outlined.Shield,
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://app.tastile.app/auth/cognito/login?next=/dashboard/account".toUri(),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
        )
        AccountListItem(
            headline = stringResource(R.string.preferences_account_email_otp_relogin),
            supporting = null,
            leading = Icons.Outlined.Mail,
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://app.tastile.app/auth/email".toUri(),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    ListItem(
        content = {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier,
    )
}

@Composable
private fun AccountListItem(
    headline: String,
    supporting: String?,
    leading: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)?,
) {
    val baseModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ListItem(
        content = { Text(headline, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = supporting?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = { Icon(leading, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = baseModifier,
    )
}