package app.tastile.android.ui.mobile.panels.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.tastile.android.R
import app.tastile.android.ui.designsystem.AppCorner
import app.tastile.android.ui.designsystem.AppSpacing
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.designsystem.MobileTokens

/**
 * Two-button "segmented control" that swaps the Schedule right-pane
 * between Recurring Tiles and Upcoming Deadlines views. Web parity for
 * the RowSegmented in `tastile-web/src/components/panels/
 * ScheduleSidePanel.tsx`.
 *
 * @param view active view key: `"recurring"` or `"upcoming"`.
 * @param onViewChange invoked when the user picks the other button.
 */
@Composable
fun ScheduleViewToggle(
    view: String,
    onViewChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorner.mediumShape)
            .background(
                AppTheme.colors.surfaceVariant.copy(alpha = MobileTokens.SurfaceAlpha.selected),
            )
            .padding(AppSpacing.xxs),
    ) {
        ScheduleSegmentButton(
            label = stringResource(R.string.panels_schedule_recurring),
            selected = view == VIEW_RECURRING,
            modifier = Modifier.weight(1f),
            onClick = { onViewChange(VIEW_RECURRING) },
        )
        ScheduleSegmentButton(
            label = stringResource(R.string.panels_schedule_upcoming),
            selected = view == VIEW_UPCOMING,
            modifier = Modifier.weight(1f),
            onClick = { onViewChange(VIEW_UPCOMING) },
        )
    }
}

@Composable
private fun ScheduleSegmentButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(AppCorner.smallShape)
            .background(
                if (selected) AppTheme.colors.surfaceVariant.copy(
                    alpha = MobileTokens.SurfaceAlpha.strongSelected,
                ) else Color.Transparent,
            )
            .clickable { onClick() }
            .padding(vertical = AppSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant,
        )
    }
}

const val VIEW_RECURRING = "recurring"
const val VIEW_UPCOMING = "upcoming"
