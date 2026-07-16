package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton

/**
 * Confirmation dialog triggered by
 * `DashboardViewModel.requestDeleteTileId != null`. Mirrors the
 * `SettingsScreen.kt` AlertDialog pattern (Material3 direct import).
 * The title carries the human-readable [tileTitle] (when known) so the
 * destructive action is anchored to a concrete target; the body is the
 * localised "permanently deletes" copy required by the C4 plan.
 */
@Composable
fun DeleteTileDialog(
    tileTitle: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.dashboard_tiles_delete_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (!tileTitle.isNullOrBlank()) {
                    Text(
                        text = tileTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("tiles-delete-dialog-target"),
                    )
                }
            }
        },
        text = { Text(stringResource(R.string.dashboard_tiles_delete_dialog_body)) },
        confirmButton = {
            NiaButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("tiles-delete-dialog-confirm"),
                text = { Text(stringResource(R.string.dashboard_tiles_delete_dialog_confirm)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            )
        },
        dismissButton = {
            NiaTextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("tiles-delete-dialog-cancel"),
                text = { Text(stringResource(R.string.dashboard_tiles_delete_dialog_cancel)) },
                leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
            )
        },
        modifier = modifier.testTag("tiles-delete-dialog"),
    )
}
