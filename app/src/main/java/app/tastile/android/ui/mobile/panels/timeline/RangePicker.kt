package app.tastile.android.ui.mobile.panels.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.dashboard.TimelineSubScale

/**
 * 4-tab pill scale selector (Day / Week / Month / Custom) that mirrors
 * web's `<RowSegmented size="tiny">` in
 * `tastile-web/src/app/dashboard/timeline/page.tsx`.
 *
 * When [current] is [TimelineSubScale.CUSTOM] the caller is expected
 * to also surface two date pickers (see `TimelineSectionContent`).
 */
@Composable
internal fun RangePicker(
    current: TimelineSubScale,
    onSelect: (TimelineSubScale) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        TimelineSubScale.DAY to R.string.panels_calendar_day,
        TimelineSubScale.WEEK to R.string.panels_calendar_week,
        TimelineSubScale.MONTH to R.string.panels_calendar_month,
        TimelineSubScale.CUSTOM to R.string.panels_calendar_custom,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
            )
            .padding(2.dp),
    ) {
        items.forEach { (item, labelRes) ->
            val selected = item == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        else Color.Transparent,
                    )
                    .clickable { onSelect(item) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}