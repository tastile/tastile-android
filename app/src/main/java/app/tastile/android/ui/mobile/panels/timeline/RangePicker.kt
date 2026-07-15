package app.tastile.android.ui.mobile.panels.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.tastile.android.R
import app.tastile.android.ui.dashboard.TimelineSubScale
import app.tastile.android.ui.designsystem.AppCorner
import app.tastile.android.ui.designsystem.AppSpacing
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.designsystem.MobileTokens

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
            .clip(AppCorner.mediumShape)
            .background(
                AppTheme.colors.surfaceVariant.copy(alpha = MobileTokens.SurfaceAlpha.selected),
            )
            .padding(AppSpacing.xxs),
    ) {
        items.forEach { (item, labelRes) ->
            val selected = item == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(AppCorner.smallShape)
                    .background(
                        if (selected) AppTheme.colors.surfaceVariant.copy(
                            alpha = MobileTokens.SurfaceAlpha.strongSelected,
                        ) else Color.Transparent,
                    )
                    .clickable { onSelect(item) }
                    .padding(vertical = AppSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}
