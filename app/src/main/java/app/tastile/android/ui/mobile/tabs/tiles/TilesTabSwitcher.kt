package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.designsystem.AppCorner
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.dashboard.TilesTab

/**
 * Three-button pill matching web's
 * `flex items-center gap-2 rounded-lg bg-surface-1 p-1` style. The
 * active tab takes the surface accent and the inactive tabs stay
 * transparent so the pill reads as a single segmented control.
 */
@Composable
fun TilesTabSwitcher(
    active: TilesTab,
    onSelect: (TilesTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .clip(AppCorner.mediumShape)
            .background(colors.surfaceVariant)
            .padding(AppTheme.spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
    ) {
        TilesTab.entries.forEach { tab ->
            val isActive = tab == active
            androidx.compose.material3.TextButton(
                onClick = { onSelect(tab) },
                modifier = Modifier
                    .clip(AppCorner.smallShape)
                    .background(if (isActive) colors.surface else colors.surfaceVariant)
                    .testTag("tiles-tab-${tab.name.lowercase()}"),
            ) {
                androidx.compose.material3.Text(
                    text = stringResource(
                        when (tab) {
                            TilesTab.LIST -> R.string.dashboard_tiles_tab_list
                            TilesTab.TIMELINE -> R.string.dashboard_tiles_tab_timeline
                            TilesTab.CHANGES -> R.string.dashboard_tiles_tab_changes
                        },
                    ),
                    color = if (isActive) colors.onSurface else colors.onSurfaceVariant,
                    style = AppTheme.typography.labelLarge,
                )
            }
        }
    }
}