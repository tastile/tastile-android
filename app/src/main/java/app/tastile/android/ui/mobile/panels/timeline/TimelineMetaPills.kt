package app.tastile.android.ui.mobile.panels.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.designsystem.AppSpacing
import app.tastile.android.ui.designsystem.AppTheme

/**
 * Meta-pills row shown above the timeline block list. Mirrors web's
 * `panels.timeline.meta.{blocks,work,breaks}` cluster in
 * `tastile-web/src/app/dashboard/timeline/page.tsx`.
 *
 * Compose:
 *   "N blocks · ${Nm} work · ${N} breaks"
 *
 * The three counts come from [TimelineMetaPillsModel] (derived in
 * `DashboardViewModel.timelineMetaPills`).
 */
data class TimelineMetaPillsModel(
    val blockCount: Int,
    val totalWorkMin: Long,
    val totalBreakMin: Long,
)

@Composable
internal fun TimelineMetaPills(
    model: TimelineMetaPillsModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${model.blockCount} " + stringResource(R.string.panels_timeline_meta_blocks),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Text(
            text = "${model.totalWorkMin}m " + stringResource(R.string.panels_timeline_meta_work),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
        Text(
            text = "${model.totalBreakMin} " + stringResource(R.string.panels_timeline_meta_breaks),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}
