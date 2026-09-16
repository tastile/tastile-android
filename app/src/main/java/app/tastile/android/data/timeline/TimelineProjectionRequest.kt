package app.tastile.android.data.timeline

import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.dashboard.TimelineSubScale
import java.time.LocalDate
import java.time.ZoneId

/**
 * Local-only projection context used by compact Tiles-tab timeline panels.
 *
 * The request is deliberately expressed as page keys. The projection does
 * not own a transport path; it composes the same Room-backed page snapshots
 * used by the calendar route. [ownerIds] therefore participates in the page
 * key fingerprint and cannot accidentally read another workspace's rows.
 */
data class TimelineProjectionRequest(
    val accountId: String,
    val ownerIds: List<String>,
    val zoneId: ZoneId,
    val scale: TimelineSubScale,
    val anchor: LocalDate,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
) {
    val normalizedOwnerIds: List<String>
        get() = normalizeTimelineOwnerIds(ownerIds)

    val scopeFingerprint: String
        get() = timelineScopeFingerprint(normalizedOwnerIds)

    /** Local membership dates covered by this projection, in render order. */
    val localDates: List<LocalDate>
        get() {
            val pageScale = scale.toPageScaleOrNull()
            if (pageScale != null) {
                return timelinePageDates(pageScale, anchor)
            }
            val start = customStart ?: return emptyList()
            val end = customEnd ?: return emptyList()
            if (end.isBefore(start)) return emptyList()
            return generateSequence(start) { date ->
                date.takeIf { it.isBefore(end) }?.plusDays(1)
            }.toList()
        }

    fun normalized(): TimelineProjectionRequest {
        val pageScale = scale.toPageScaleOrNull()
        val normalizedAnchor = pageScale?.let { normalizeTimelineAnchor(it, anchor) } ?: anchor
        val normalizedStart = customStart
        val normalizedEnd = customEnd
        return if (
            ownerIds == normalizedOwnerIds &&
                anchor == normalizedAnchor
        ) {
            this
        } else {
            copy(
                ownerIds = normalizedOwnerIds,
                anchor = normalizedAnchor,
                customStart = normalizedStart,
                customEnd = normalizedEnd,
            )
        }
    }

    /** Returns the local cache pages needed for this compact projection. */
    fun pageKeys(): List<TimelinePageKey> {
        val request = normalized()
        val pageScale = request.scale.toPageScaleOrNull()
        if (pageScale == null) {
            return request.localDates
                .map { date -> request.pageKey(TimelineScale.Day, date) }
        }
        return listOf(request.pageKey(pageScale, request.anchor))
    }

    private fun pageKey(scale: TimelineScale, anchor: LocalDate): TimelinePageKey =
        TimelinePageKey.forScope(
            accountId = accountId,
            ownerIds = normalizedOwnerIds,
            zoneId = zoneId,
            scale = scale,
            anchor = anchor,
        )

    private fun TimelineSubScale.toPageScaleOrNull(): TimelineScale? = when (this) {
        TimelineSubScale.DAY -> TimelineScale.Day
        TimelineSubScale.WEEK -> TimelineScale.Week
        TimelineSubScale.MONTH -> TimelineScale.Month
        TimelineSubScale.CUSTOM -> null
    }
}
