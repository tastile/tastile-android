package app.tastile.android.ui.mobile.panels.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.shape.RoundedCornerShape
import app.tastile.android.R

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
            .clip(RoundedCornerShape(8.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
            )
            .padding(2.dp),
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
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                else Color.Transparent,
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

const val VIEW_RECURRING = "recurring"
const val VIEW_UPCOMING = "upcoming"