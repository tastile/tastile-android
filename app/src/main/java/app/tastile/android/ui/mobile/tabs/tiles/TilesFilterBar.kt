package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenu
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.dashboard.ListGroupingMode
import app.tastile.android.ui.dashboard.ListViewMode
import app.tastile.android.ui.dashboard.TileGranularity
import app.tastile.android.ui.dashboard.TileRange

/**
 * Column of filter controls that drive `DashboardViewModel.setTileFilter`
 * via the per-knob setters. Mirrors the web `/dashboard/tiles` filter
 * stack (search + 3 dropdowns + 2 segmented rows) one-for-one so the
 * mobile screen exposes the same surface area.
 */
@Composable
fun TilesFilterBar(
    search: String,
    onSearchChange: (String) -> Unit,
    range: TileRange,
    onRangeChange: (TileRange) -> Unit,
    granularity: TileGranularity,
    onGranularityChange: (TileGranularity) -> Unit,
    limit: Int,
    onLimitChange: (Int) -> Unit,
    grouping: ListGroupingMode,
    onGroupingChange: (ListGroupingMode) -> Unit,
    viewMode: ListViewMode,
    onViewModeChange: (ListViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            placeholder = { Text(stringResource(R.string.dashboard_tiles_search_placeholder)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tiles-filter-search"),
        )
        RangeDropdown(
            current = range,
            onPick = onRangeChange,
        )
        GranularityDropdown(
            current = granularity,
            onPick = onGranularityChange,
        )
        LimitDropdown(
            current = limit,
            onPick = onLimitChange,
        )
        GroupingSegmented(
            current = grouping,
            onPick = onGroupingChange,
        )
        ViewModeSegmented(
            current = viewMode,
            onPick = onViewModeChange,
        )
    }
}

@Composable
private fun RangeDropdown(
    current: TileRange,
    onPick: (TileRange) -> Unit,
) {
    val label = stringResource(R.string.dashboard_tiles_filter_range_label)
    GenericDropdown(
        label = label,
        currentLabel = stringResource(rangeKey(current)),
        items = TileRange.entries.map { it to stringResource(rangeKey(it)) },
        onPick = { onPick(it as TileRange) },
        testTag = "tiles-filter-range",
    )
}

@Composable
private fun GranularityDropdown(
    current: TileGranularity,
    onPick: (TileGranularity) -> Unit,
) {
    val label = stringResource(R.string.dashboard_tiles_filter_granularity_label)
    GenericDropdown(
        label = label,
        currentLabel = stringResource(granularityKey(current)),
        items = TileGranularity.entries.map { it to stringResource(granularityKey(it)) },
        onPick = { onPick(it as TileGranularity) },
        testTag = "tiles-filter-granularity",
    )
}

@Composable
private fun LimitDropdown(
    current: Int,
    onPick: (Int) -> Unit,
) {
    val label = stringResource(R.string.dashboard_tiles_filter_limit_label)
    GenericDropdown(
        label = label,
        currentLabel = stringResource(limitKey(current)),
        items = limitOptions(),
        onPick = { onPick(it as Int) },
        testTag = "tiles-filter-limit",
    )
}

@Composable
private fun GroupingSegmented(
    current: ListGroupingMode,
    onPick: (ListGroupingMode) -> Unit,
) {
    val items = ListGroupingMode.entries.map { it to groupingLabel(it) }
    val currentLabel = groupingLabel(current)
    GenericSegmented(
        items = items,
        currentLabel = currentLabel,
        onPick = { onPick(it as ListGroupingMode) },
        testTag = "tiles-filter-grouping",
    )
}

@Composable
private fun ViewModeSegmented(
    current: ListViewMode,
    onPick: (ListViewMode) -> Unit,
) {
    val items = ListViewMode.entries.map { it to viewModeLabel(it) }
    val currentLabel = viewModeLabel(current)
    GenericSegmented(
        items = items,
        currentLabel = currentLabel,
        onPick = { onPick(it as ListViewMode) },
        testTag = "tiles-filter-view-mode",
    )
}

@Composable
private fun GenericDropdown(
    label: String,
    currentLabel: String,
    items: List<Pair<Any, String>>,
    onPick: (Any) -> Unit,
    testTag: String,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = true }
            .padding(vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(currentLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { (value, labelText) ->
                    DropdownMenuItem(
                        text = { Text(labelText) },
                        leadingIcon = { Icon(Icons.Outlined.FilterList, contentDescription = null) },
                        onClick = {
                        onPick(value)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
private fun GenericSegmented(
    items: List<Pair<Any, String>>,
    currentLabel: String,
    onPick: (Any) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEach { (value, labelText) ->
            val active = labelText == currentLabel
            Text(
                labelText,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        onPick(value)
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

private fun rangeKey(range: TileRange): Int = when (range) {
    TileRange.ALL -> R.string.dashboard_tiles_filter_range_all
    TileRange.TODAY -> R.string.dashboard_tiles_filter_range_today
    TileRange.RECENT -> R.string.dashboard_tiles_filter_range_recent
    TileRange.EXCLUDE_FUTURE -> R.string.dashboard_tiles_filter_range_exclude_future
}

private fun granularityKey(g: TileGranularity): Int = when (g) {
    TileGranularity.ALL -> R.string.dashboard_tiles_filter_granularity_all
    TileGranularity.NO_BREAKS -> R.string.dashboard_tiles_filter_granularity_no_breaks
    TileGranularity.MIN_5M -> R.string.dashboard_tiles_filter_granularity_min_5m
    TileGranularity.MIN_15M -> R.string.dashboard_tiles_filter_granularity_min_15m
    TileGranularity.MIN_30M -> R.string.dashboard_tiles_filter_granularity_min_30m
}

private fun limitKey(limit: Int): Int = when (limit) {
    20 -> R.string.dashboard_tiles_filter_limit_20
    50 -> R.string.dashboard_tiles_filter_limit_50
    100 -> R.string.dashboard_tiles_filter_limit_100
    500 -> R.string.dashboard_tiles_filter_limit_500
    else -> R.string.dashboard_tiles_filter_limit_unlimited
}

@Composable
private fun limitOptions(): List<Pair<Any, String>> = listOf(
    20 to stringResource(R.string.dashboard_tiles_filter_limit_20),
    50 to stringResource(R.string.dashboard_tiles_filter_limit_50),
    100 to stringResource(R.string.dashboard_tiles_filter_limit_100),
    500 to stringResource(R.string.dashboard_tiles_filter_limit_500),
    0 to stringResource(R.string.dashboard_tiles_filter_limit_unlimited),
)

private fun groupingLabel(g: ListGroupingMode): String = when (g) {
    ListGroupingMode.STATE -> "By State"
    ListGroupingMode.PROJECT -> "By Project"
    ListGroupingMode.TAG -> "By Tag"
}

private fun viewModeLabel(v: ListViewMode): String = when (v) {
    ListViewMode.COMPACT -> "Compact"
    ListViewMode.COMFORTABLE -> "Comfortable"
    ListViewMode.DETAILED -> "Detailed"
}
