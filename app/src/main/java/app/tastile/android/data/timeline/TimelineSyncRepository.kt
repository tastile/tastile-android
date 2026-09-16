package app.tastile.android.data.timeline

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Intent for one finite local timeline page refresh. */
data class TimelineRefreshRequest(
    val key: TimelinePageKey,
    val ownerIds: List<String> = emptyList(),
    val now: Instant = Instant.now(),
    val staleAfter: Duration = DEFAULT_TIMELINE_STALE_AFTER,
    /** Optional generation used by callers that already own a request order. */
    val generation: Long? = null,
    /** Optional subset of [key]'s dates used by range-level prefetch callers. */
    val localDates: List<LocalDate>? = null,
) {
    init {
        require(staleAfter >= Duration.ZERO) { "staleAfter must not be negative" }
    }

    internal val normalizedKey: TimelinePageKey
        get() = key.normalized()

    /** Owner scope in the canonical order used by API/cache coalescing. */
    val normalizedOwnerIds: List<String>
        get() = normalizeTimelineOwnerIds(ownerIds.ifEmpty { key.ownerIds })

    internal val requestedDates: List<LocalDate>
        get() = (localDates ?: normalizedKey.visibleDates.toList()).distinct().sorted()

    internal companion object {
        val DEFAULT_TIMELINE_STALE_AFTER: Duration = Duration.ofMinutes(15)
        const val CACHE_CONTRACT_VERSION: Int = TIMELINE_SCOPE_API_CONTRACT_VERSION
    }
}

/** Outcome of a background cache refresh. */
enum class TimelineRefreshStatus {
    Refreshed,
    Skipped,
    Failed,
    Superseded,
}

/** A finite UTC range passed to the canonical timeline read. */
data class TimelineUtcRange(
    val start: Instant,
    val end: Instant,
    val localDates: List<LocalDate>,
)

data class TimelineRefreshResult(
    val status: TimelineRefreshStatus,
    val refreshedDates: List<LocalDate> = emptyList(),
    val failedDates: List<LocalDate> = emptyList(),
    val ranges: List<TimelineUtcRange> = emptyList(),
    val generation: Long? = null,
    val failureMessage: String? = null,
)

/** Background producer for the local timeline read model. */
interface TimelineSyncRepository {
    suspend fun refresh(request: TimelineRefreshRequest): TimelineRefreshResult
}
