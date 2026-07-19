package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.util.formatIsoDateTime
import app.tastile.android.ui.dashboard.DashboardViewModel

/**
 * Recent-changes sub-tab body. Capped at [MAX_VISIBLE_CHANGES] rows
 * (matching web's constant). Each row shows the tile title, the event
 * type, and a locale-aware timestamp.
 */
private const val MAX_VISIBLE_CHANGES = 120

@Composable
fun TilesChangesBody(
    vm: DashboardViewModel,
    locale: AppLocale,
    modifier: Modifier = Modifier,
) {
    val timeline by vm.timeline.collectAsStateWithLifecycle()
    val visible = remember(timeline) { timeline.take(MAX_VISIBLE_CHANGES) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.dashboard_tiles_changes_section_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (visible.isEmpty()) {
            Text(
                text = stringResource(R.string.dashboard_tiles_changes_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                visible.forEach { item ->
                    ChangeRow(item = item, locale = locale)
                }
            }
        }
    }
}

@Composable
private fun ChangeRow(item: CoreTimelineItem, locale: AppLocale) {
    val ended = item.type.endsWith("_ended") || item.status == "done" || item.status == "completed"
    val dotColor = if (ended) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tile-change-${item.id}-${item.type}")
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = item.type,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatIsoDateTime(item.startAt, locale),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
