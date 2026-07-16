package app.tastile.android.ui.mobile.account

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.designsystem.AppListItem
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
import app.tastile.android.ui.mobile.designsystem.SectionHeader
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    PanelSheet(
        title = stringResource(R.string.preferences_account_title),
        sheetState = sheetState,
        onDismiss = { overlay.dismiss() },
    ) {
        AccountSheetBody(viewModel = viewModel, overlay = overlay)
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
            .padding(bottom = MobileSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.sm),
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
    Column(verticalArrangement = Arrangement.spacedBy(MobileSpacing.xxs)) {
        Text(
            text = stringResource(R.string.preferences_account_profile_heading),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onSurface,
        )
        Text(
            text = stringResource(R.string.preferences_account_profile_guide),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
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
            .padding(vertical = MobileSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
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
                    tint = AppTheme.colors.onSurfaceVariant,
                )
            }
        }
        if (state.loading) {
            Text(
                text = stringResource(R.string.preferences_account_loading),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
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
    AppListItem(
        headline = stringResource(R.string.preferences_account_email),
        supporting = email.ifBlank { "-" },
        leading = Icons.Outlined.Mail,
    )
}

@Composable
private fun EmailVerificationRow(verified: Boolean) {
    AppListItem(
        headline = stringResource(R.string.preferences_account_email_verified),
        supporting = stringResource(
            if (verified) R.string.preferences_account_verified
            else R.string.preferences_account_unverified,
        ),
        leading = Icons.Outlined.Verified,
    )
}

@Composable
private fun AccountIdRow(sub: String, fallback: String) {
    val display = sub.takeIf { it.isNotBlank() } ?: fallback.takeIf { it.isNotBlank() } ?: "-"
    AppListItem(
        headline = stringResource(R.string.preferences_account_account_id),
        supporting = display,
        leading = Icons.Outlined.AccountCircle,
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
            .padding(vertical = MobileSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
    ) {
        SectionHeader(title = stringResource(R.string.preferences_account_change_email_heading))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.pendingEmail,
                onValueChange = onPendingEmailChange,
                label = { Text(stringResource(R.string.preferences_account_new_email)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            AppPrimaryButton(
                text = stringResource(R.string.preferences_account_send_code),
                onClick = onSendCode,
                enabled = !state.submitting && state.pendingEmail.isNotBlank(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
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
            AppPrimaryButton(
                text = stringResource(R.string.preferences_account_verify_code),
                onClick = onVerify,
                enabled = !state.submitting && state.verificationCode.isNotBlank(),
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
            .padding(vertical = MobileSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
    ) {
        SectionHeader(title = stringResource(R.string.preferences_account_login_methods))
        AppListItem(
            headline = stringResource(R.string.preferences_account_passkey),
            leading = Icons.Outlined.Shield,
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://app.tastile.app/auth/cognito/login?next=/dashboard/account".toUri(),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
        )
        AppListItem(
            headline = stringResource(R.string.preferences_account_email_otp_relogin),
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
