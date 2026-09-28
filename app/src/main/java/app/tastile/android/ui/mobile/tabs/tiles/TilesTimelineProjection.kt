package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.timeline.TimelineProjectionRequest
import app.tastile.android.data.timeline.normalizeTimelineOwnerIds
import app.tastile.android.ui.dashboard.DashboardViewModel
import java.time.LocalDate
import java.time.ZoneId

/** Collects the local projection only while the Tiles panel is lifecycle-visible. */
@Composable
internal fun rememberTilesTimelineProjectionItems(
    dashboard: DashboardViewModel,
    projectionViewModel: TilesTimelineProjectionViewModel?,
): List<CoreTimelineItem> {
    val tileFilter by dashboard.tileFilter.collectAsStateWithLifecycle()
    val scale by dashboard.timelineScale.collectAsStateWithLifecycle()
    val selectedDay by dashboard.selectedDay.collectAsStateWithLifecycle()
    val customStart by dashboard.customStartIso.collectAsStateWithLifecycle()
    val customEnd by dashboard.customEndIso.collectAsStateWithLifecycle()
    val accountId = dashboard.timelineAccountId
    val ownerIds = normalizeTimelineOwnerIds(tileFilter.ownerIds)
    val request = remember(
        accountId,
        ownerIds,
        scale,
        selectedDay,
        customStart,
        customEnd,
    ) {
        accountId?.let {
            TimelineProjectionRequest(
                accountId = it,
                ownerIds = ownerIds,
                zoneId = ZoneId.systemDefault(),
                scale = scale,
                anchor = selectedDay,
                customStart = parseDate(customStart),
                customEnd = parseDate(customEnd),
            )
        }
    }

    LaunchedEffect(projectionViewModel, request) {
        projectionViewModel?.setRequest(request)
    }

    val state = projectionViewModel?.items?.collectAsStateWithLifecycle()
    return state?.value.orEmpty()
}

private fun parseDate(value: String?): LocalDate? =
    value?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
