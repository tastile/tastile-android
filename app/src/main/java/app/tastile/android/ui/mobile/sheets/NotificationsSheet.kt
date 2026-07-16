package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.notifications.NotificationRepository
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.designsystem.AppEmptyState
import app.tastile.android.ui.mobile.designsystem.AppListItem
import app.tastile.android.ui.mobile.designsystem.MobileSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    overlay: OverlayViewModel,
    repository: NotificationRepository,
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val items by repository.pending.collectAsStateWithLifecycle()

    if (current is Overlay.Notifications) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        PanelSheet(
            title = stringResource(R.string.mobile_top_notifications),
            sheetState = sheetState,
            onDismiss = { overlay.dismiss() },
        ) {
            if (items.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(R.string.empty_tiles_title),
                    hint = stringResource(R.string.empty_tiles_hint),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MobileSpacing.xxs),
                ) {
                    items.forEach {
                        AppListItem(
                            headline = it.label,
                            leading = Icons.Outlined.Notifications,
                        )
                    }
                }
            }
        }
    }
}