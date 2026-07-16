package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton

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
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
                Text(
                    text = stringResource(R.string.dashboard_tiles_delete_dialog_title),
                    style = AppTheme.typography.headlineSmall,
                )
                if (!tileTitle.isNullOrBlank()) {
                    Text(
                        text = tileTitle,
                        style = AppTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("tiles-delete-dialog-target"),
                    )
                }
            }
        },
        text = { Text(stringResource(R.string.dashboard_tiles_delete_dialog_body)) },
        confirmButton = {
            AppPrimaryButton(
                text = stringResource(R.string.dashboard_tiles_delete_dialog_confirm),
                onClick = onConfirm,
                leadingIcon = Icons.Outlined.Delete,
                modifier = Modifier.testTag("tiles-delete-dialog-confirm"),
            )
        },
        dismissButton = {
            AppTertiaryButton(
                text = stringResource(R.string.dashboard_tiles_delete_dialog_cancel),
                onClick = onCancel,
                leadingIcon = Icons.Outlined.Close,
                modifier = Modifier.testTag("tiles-delete-dialog-cancel"),
            )
        },
        modifier = modifier.testTag("tiles-delete-dialog"),
    )
}
