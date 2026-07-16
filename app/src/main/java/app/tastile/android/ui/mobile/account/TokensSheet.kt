package app.tastile.android.ui.mobile.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.repository.AccountTokenView
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.designsystem.AppEmptyState
import app.tastile.android.ui.mobile.designsystem.AppListItem
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
import app.tastile.android.ui.mobile.designsystem.SectionHeader
import app.tastile.android.ui.mobile.sheets.PanelSheet

/**
 * API tokens panel — mirrors `AccessTokenSection` from
 * `tastile-web/src/components/account/AccessTokenSection.tsx`:
 *   - heading + description
 *   - inline created-token disclosure (with Copy button)
 *   - form: token name + Issue
 *   - list of issued tokens (revoke / copy-prefix)
 *
 * Rename (PATCH) is intentionally dropped on mobile because the v1 daemon
 * route is not yet exposed; the C7 plan covers list + create + revoke.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokensSheet(
    overlay: OverlayViewModel,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    if (current !is Overlay.Tokens) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    PanelSheet(
        title = stringResource(R.string.account_tokens_heading),
        sheetState = sheetState,
        onDismiss = { overlay.dismiss() },
    ) {
        TokensBody(viewModel = viewModel)
    }
}

@Composable
private fun TokensBody(viewModel: AccountViewModel) {
    val state by viewModel.tokens.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MobileSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.sm),
    ) {
        Text(
            text = stringResource(R.string.account_tokens_description),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )

        state.error?.let { msg ->
            Text(
                text = msg,
                style = AppTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        state.created?.let { created ->
            CreatedTokenDisclosure(
                displayName = created.displayName,
                secret = created.secret,
                onCopy = { /* clipboard handled inside; stays visible until dismiss */ },
                onDismiss = viewModel::dismissCreatedToken,
            )
        }

        CreateTokenForm(
            name = name,
            onNameChange = { name = it },
            submitting = state.submitting,
            onSubmit = {
                viewModel.createToken(name)
                name = ""
            },
        )

        SectionHeader(title = stringResource(R.string.account_tokens_issued_heading))

        when {
            state.loading -> Text(
                text = stringResource(R.string.preferences_account_loading),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
            state.tokens.isEmpty() -> AppEmptyState(
                icon = Icons.Outlined.Key,
                title = stringResource(R.string.account_tokens_empty),
                hint = "",
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs)) {
                state.tokens.forEach { token ->
                    TokenRow(
                        token = token,
                        submitting = state.submitting,
                        onRevoke = { viewModel.revokeToken(token.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateTokenForm(
    name: String,
    onNameChange: (String) -> Unit,
    submitting: Boolean,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MobileSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.account_tokens_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        AppPrimaryButton(
            text = stringResource(R.string.account_tokens_issue),
            onClick = onSubmit,
            enabled = !submitting && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CreatedTokenDisclosure(
    displayName: String,
    secret: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MobileSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.xxs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.account_tokens_new_token_heading),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
            )
            Row {
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(secret))
                        onCopy()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.account_tokens_copy),
                        tint = AppTheme.colors.primary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        // TODO i18n: dismiss disclosure a11y label
                        contentDescription = "Dismiss",
                        tint = AppTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = displayName.ifBlank { secret.take(8) },
            style = AppTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = AppTheme.colors.onSurface,
        )
        Text(
            text = secret,
            style = AppTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun TokenRow(
    token: AccountTokenView,
    submitting: Boolean,
    onRevoke: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppListItem(
            headline = token.displayName.ifBlank { stringResource(R.string.account_tokens_issued_heading) },
            supporting = buildString {
                append(token.tokenPrefix)
                if (token.tokenPrefix.isNotBlank()) append("...")
                if (token.isRevoked) {
                    append(" · ")
                    append(stringResource(R.string.account_tokens_revoked))
                }
            },
            leading = Icons.Outlined.Key,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRevoke, enabled = !token.isRevoked && !submitting) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.account_tokens_revoke),
                tint = if (token.isRevoked) AppTheme.colors.onSurfaceVariant
                else MaterialTheme.colorScheme.error,
            )
        }
    }
}