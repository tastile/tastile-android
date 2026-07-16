package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Timeline
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
// m2-allow: m3-component
import androidx.compose.material3.SegmentedButton
// m2-allow: m3-component
import androidx.compose.material3.SegmentedButtonDefaults
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.dashboard.TilesTab

/**
 * Three-button segmented control matching the web tiles tab switcher.
 */
@Composable
fun TilesTabSwitcher(
    active: TilesTab,
    onSelect: (TilesTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        TilesTab.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = tab == active,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = TilesTab.entries.size),
                modifier = Modifier.testTag("tiles-tab-${tab.name.lowercase()}"),
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            TilesTab.LIST -> Icons.Outlined.FormatListBulleted
                            TilesTab.TIMELINE -> Icons.Outlined.Timeline
                            TilesTab.CHANGES -> Icons.Outlined.ChangeHistory
                        },
                        contentDescription = null,
                    )
                },
            ) {
                Text(
                    text = stringResource(
                        when (tab) {
                            TilesTab.LIST -> R.string.dashboard_tiles_tab_list
                            TilesTab.TIMELINE -> R.string.dashboard_tiles_tab_timeline
                            TilesTab.CHANGES -> R.string.dashboard_tiles_tab_changes
                        },
                    ),
                )
            }
        }
    }
}
