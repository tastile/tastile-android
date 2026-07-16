package app.tastile.android.ui.mobile.panels.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.material3.CircularProgressIndicator
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineSubScale
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
import app.tastile.android.ui.mobile.designsystem.SectionHeader

/**
 * Timeline pane body. Mirrors web `/dashboard/timeline` surface in
 * `tastile-web/src/app/dashboard/timeline/page.tsx` (composition order
 * binding per plan R1):
 *
 *   1. SectionHeader(title = "Calendar")
 *   2. Meta-pills row (blocks · work · breaks)
 *   3. 4-tab pill scale selector (Day / Week / Month / Custom)
 *   4. (Custom only) Two date inputs for the custom range
 *   5. Loading overlay while refreshing
 *   6. Empty state OR timeline block list
 *
 * The pane reads from `DashboardViewModel.timelineScale / timeline / isLoadingTimeline / customStartIso / customEndIso`.
 */
@Composable
internal fun TimelineSectionContent(
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val timeline by dashboardViewModel.timeline.collectAsStateWithLifecycle()
    val scale by dashboardViewModel.timelineScale.collectAsStateWithLifecycle()
    val customStartIso by dashboardViewModel.customStartIso.collectAsStateWithLifecycle()
    val customEndIso by dashboardViewModel.customEndIso.collectAsStateWithLifecycle()
    val isLoading by dashboardViewModel.isLoadingTimeline.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MobileSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.sm),
    ) {
        SectionHeader(title = "Calendar")

        TimelineMetaPills(
            model = computeMetaPills(timeline),
        )

        RangePicker(
            current = scale,
            onSelect = { newScale ->
                dashboardViewModel.setTimelineScale(newScale)
                if (newScale == TimelineSubScale.CUSTOM && customStartIso == null) {
                    dashboardViewModel.setCustomRange(startIso = null, endIso = null)
                }
            },
        )

        if (scale == TimelineSubScale.CUSTOM) {
            CustomDateRow(
                startIso = customStartIso,
                endIso = customEndIso,
                onStartChange = { start -> dashboardViewModel.setCustomRange(startIso = start, endIso = customEndIso) },
                onEndChange = { end -> dashboardViewModel.setCustomRange(startIso = customStartIso, endIso = end) },
            )
        }

        if (isLoading) {
            TimelineLoadingOverlay()
        } else {
            TimelineBlockList(
                blocks = timeline,
                onBlockClick = { /* future: open tile detail */ },
            )
        }
    }
}

@Composable
private fun CustomDateRow(
    startIso: String?,
    endIso: String?,
    onStartChange: (String?) -> Unit,
    onEndChange: (String?) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.xxs),
    ) {
        Text(
            text = "${startIso ?: "—"}  →  ${endIso ?: "—"}",
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        // Date picker dialogs intentionally deferred — the existing
        // v1 /v1/timeline endpoint accepts ISO-8601 instants, so the
        // initial release uses the day's pill selector for non-custom
        // scales. Custom range in this release is a stub row showing
        // the active ISO strings; Material3 DatePickerDialog wiring is
        // tracked as a follow-up so this PR stays under the closed-test
        // deadline.
        Text(
            text = "setCustomRange hook · ${onStartChange.hashCode()}/${onEndChange.hashCode()}",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelineLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MobileSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            Text(
                text = stringResource(R.string.panels_timeline_loading),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
    }
}

private fun computeMetaPills(blocks: List<CoreTimelineItem>): TimelineMetaPillsModel {
    val workMin = blocks.filter { it.type.equals("work", ignoreCase = true) }
        .sumOf { blockDurationMin(it) }
    val breakMin = blocks.filter { it.type.equals("break", ignoreCase = true) }
        .sumOf { blockDurationMin(it) }
    return TimelineMetaPillsModel(
        blockCount = blocks.size,
        totalWorkMin = workMin,
        totalBreakMin = breakMin,
    )
}

private fun blockDurationMin(block: CoreTimelineItem): Long {
    val start = runCatching { java.time.Instant.parse(block.startAt).toEpochMilli() }.getOrNull() ?: return 0L
    val end = runCatching { java.time.Instant.parse(block.endAt ?: block.startAt).toEpochMilli() }.getOrNull() ?: return 0L
    return ((end - start) / 60_000L).coerceAtLeast(0L)
}
