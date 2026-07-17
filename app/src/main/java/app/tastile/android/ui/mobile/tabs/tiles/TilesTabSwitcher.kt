package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Timeline
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaSegmentedButton
import app.tastile.android.core.designsystem.component.NiaSingleChoiceSegmentedButtonRow
import app.tastile.android.core.designsystem.component.NiaSegmentedButtonDefaults
import app.tastile.android.ui.dashboard.TilesTab

/**
 * Three-button segmented control matching the web tiles tab switcher.
 *
 * Each segment carries its own `tiles-tab-${name.lowercase()}` test tag in a
 * non-merged semantics block; this keeps `onNodeWithTag` matches consistent
 * across segments and prevents the selectable-group parent from absorbing
 * per-segment tags.
 */
@Composable
fun TilesTabSwitcher(
    active: TilesTab,
    onSelect: (TilesTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NiaSingleChoiceSegmentedButtonRow(modifier = modifier) {
        TilesTab.entries.forEachIndexed { index, tab ->
            NiaSegmentedButton(
                selected = tab == active,
                onClick = { onSelect(tab) },
                shape = NiaSegmentedButtonDefaults.itemShape(index = index, count = TilesTab.entries.size),
                modifier = Modifier.testTag("tiles-tab-${tab.name.lowercase()}"),
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            TilesTab.LIST -> Icons.AutoMirrored.Outlined.FormatListBulleted
                            TilesTab.TIMELINE -> Icons.Outlined.Timeline
                            TilesTab.CHANGES -> Icons.Outlined.ChangeHistory
                        },
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(
                            when (tab) {
                                TilesTab.LIST -> R.string.dashboard_tiles_tab_list
                                TilesTab.TIMELINE -> R.string.dashboard_tiles_tab_timeline
                                TilesTab.CHANGES -> R.string.dashboard_tiles_tab_changes
                            },
                        ),
                    )
                },
            )
        }
    }
}
