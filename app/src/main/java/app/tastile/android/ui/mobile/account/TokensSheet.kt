package app.tastile.android.ui.mobile.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.data.repository.AccountTokenView
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
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
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.account_tokens_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.error?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.tokens.isEmpty() -> TokensEmptyState()
            else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
private fun SectionHeader(title: String) {
    ListItem(
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
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
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.account_tokens_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        NiaButton(
            onClick = onSubmit,
            enabled = !submitting && name.isNotBlank(),
            text = { Text(stringResource(R.string.account_tokens_issue)) },
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
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.account_tokens_new_token_heading),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
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
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        // TODO i18n: dismiss disclosure a11y label
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = displayName.ifBlank { secret.take(8) },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = secret,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TokensEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Key,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Box(Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.account_tokens_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(Modifier.size(4.dp))
        Text(
            text = "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        ListItem(
            headlineContent = {
                Text(
                    token.displayName.ifBlank { stringResource(R.string.account_tokens_issued_heading) },
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            supportingContent = {
                Text(
                    buildString {
                        append(token.tokenPrefix)
                        if (token.tokenPrefix.isNotBlank()) append("...")
                        if (token.isRevoked) {
                            append(" · ")
                            append(stringResource(R.string.account_tokens_revoked))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = { Icon(Icons.Outlined.Key, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = false) {},
        )
        IconButton(onClick = onRevoke, enabled = !token.isRevoked && !submitting) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.account_tokens_revoke),
                tint = if (token.isRevoked) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error,
            )
        }
    }
}