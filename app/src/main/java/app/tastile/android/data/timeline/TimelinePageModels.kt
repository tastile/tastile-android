package app.tastile.android.data.timeline

import androidx.compose.runtime.Immutable
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.ui.dashboard.TimelineScale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Optional flags that change the v1 timeline projection. These flags are
 * part of the cache scope even when the current Android read path uses the
 * v1 defaults (all false).
 */
data class TimelineIncludeFlags(
    val labels: Boolean = false,
    val closed: Boolean = false,
    val blocked: Boolean = false,
    val nested: Boolean = false,
)

/**
 * Canonicalizes the owner scope before it is used in a page key or request.
 * Owner ids are UUIDs at the v1 boundary, but retaining non-blank values here
 * keeps this helper usable by deterministic unit tests and future subjects.
 */
fun normalizeTimelineOwnerIds(ownerIds: Iterable<String>): List<String> = ownerIds
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinct()
    .sorted()

/**
 * Builds the stable scope fingerprint used by the timeline cache and API
 * request context. The descriptor is intentionally explicit and versioned so
 * a future contract change cannot reuse rows written by this implementation.
 */
fun timelineScopeFingerprint(
    ownerIds: Iterable<String>,
    include: TimelineIncludeFlags = TimelineIncludeFlags(),
    apiContractVersion: Int = TIMELINE_SCOPE_API_CONTRACT_VERSION,
): String {
    val owners = normalizeTimelineOwnerIds(ownerIds)
    return buildString {
        append("v").append(apiContractVersion)
        append("|owners=").append(owners.joinToString(","))
        append("|include=")
            .append("labels:").append(include.labels)
            .append(",closed:").append(include.closed)
            .append(",blocked:").append(include.blocked)
            .append(",nested:").append(include.nested)
    }
}

const val TIMELINE_SCOPE_API_CONTRACT_VERSION = 1

/**
 * Identifies one locally cached timeline page.
 *
 * The supplied [anchor] is accepted from navigation code as-is, while
 * [normalizedAnchor] and [normalized] provide the canonical page identity.
 * Repository reads always use [normalized], so a non-canonical navigation
 * anchor cannot create a second cache entry for the same page.
 */
data class TimelinePageKey(
    val accountId: String,
    val scopeFingerprint: String,
    val zoneId: ZoneId,
    val scale: TimelineScale,
    val anchor: LocalDate,
    /** Normalized owner scope used to build refresh requests. */
    val ownerIds: List<String> = emptyList(),
) {
    /**
     * Compatibility alias for Room adapters that still call their persisted
     * column `scopeKey`. The value is always the canonical fingerprint.
     */
    @Deprecated("Use scopeFingerprint; Room column naming is retained for migration compatibility")
    val scopeKey: String
        get() = scopeFingerprint

    /** Owner ids in the exact order used by API and cache request identity. */
    val normalizedOwnerIds: List<String>
        get() = normalizeTimelineOwnerIds(ownerIds)

    /** Whitespace-normalized fingerprint used as the persisted cache value. */
    val normalizedScopeFingerprint: String
        get() = scopeFingerprint.trim()

    /** The stable anchor used by this scale's page. */
    val normalizedAnchor: LocalDate
        get() = normalizeTimelineAnchor(scale, anchor)

    /** Every local date rendered by this page, in render order. */
    val visibleDates: ImmutableList<LocalDate>
        get() = timelinePageDates(scale, normalizedAnchor)

    /** Alias useful to callers that refer to page membership as dates. */
    val dates: ImmutableList<LocalDate>
        get() = visibleDates

    /** Returns this key with its anchor canonicalized for [scale]. */
    fun normalized(): TimelinePageKey {
        val normalizedOwners = normalizedOwnerIds
        return if (
            anchor == normalizedAnchor &&
            ownerIds == normalizedOwners &&
            scopeFingerprint == normalizedScopeFingerprint
        ) {
            this
        } else {
            copy(
                scopeFingerprint = normalizedScopeFingerprint,
                anchor = normalizedAnchor,
                ownerIds = normalizedOwners,
            )
        }
    }

    companion object {
        /** Creates a key and derives its fingerprint from the selected scope. */
        fun forScope(
            accountId: String,
            ownerIds: Iterable<String>,
            zoneId: ZoneId,
            scale: TimelineScale,
            anchor: LocalDate,
            include: TimelineIncludeFlags = TimelineIncludeFlags(),
        ): TimelinePageKey {
            val normalizedOwners = normalizeTimelineOwnerIds(ownerIds)
            return TimelinePageKey(
                accountId = accountId,
                scopeFingerprint = timelineScopeFingerprint(normalizedOwners, include),
                zoneId = zoneId,
                scale = scale,
                anchor = anchor,
                ownerIds = normalizedOwners,
            )
        }
    }

    /** Legacy constructor retained for callers that have a persisted key. */
    @Deprecated("Use scopeFingerprint or TimelinePageKey.forScope")
    constructor(
        accountId: String,
        scopeKey: String,
        zoneId: ZoneId,
        scale: TimelineScale,
        anchor: LocalDate,
    ) : this(
        accountId = accountId,
        scopeFingerprint = scopeKey,
        zoneId = zoneId,
        scale = scale,
        anchor = anchor,
    )
}

/**
 * Coverage state for one local calendar date. Absence of a Room coverage row
 * is represented explicitly as [NeverFetched], rather than as an empty item
 * list.
 */
enum class TimelineCoverageState {
    NeverFetched,
    Available,
    Stale,
}

/** Immutable, page-scoped read state consumed by the timeline UI. */
@Immutable
data class TimelinePageSnapshot(
    val key: TimelinePageKey,
    val items: ImmutableList<CoreTimelineItem> = persistentListOf(),
    val coverage: ImmutableMap<LocalDate, TimelineCoverageState> = persistentMapOf(),
    val lastUpdatedAt: Instant? = null,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
)

/** Canonical anchor for a timeline page. */
fun normalizeTimelineAnchor(scale: TimelineScale, anchor: LocalDate): LocalDate = when (scale) {
    TimelineScale.Day,
    TimelineScale.List,
    -> anchor

    TimelineScale.Week -> anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    TimelineScale.Month -> anchor.withDayOfMonth(1)
}

/**
 * Returns the exact Monday-first dates rendered by the current timeline
 * calendar surfaces. MonthViewFrame renders a six-week grid, including cells
 * from adjacent months, so its page contains 42 dates.
 */
fun timelinePageDates(scale: TimelineScale, anchor: LocalDate): ImmutableList<LocalDate> {
    val pageAnchor = normalizeTimelineAnchor(scale, anchor)
    val pageStart = when (scale) {
        TimelineScale.Month -> pageAnchor.minusDays((pageAnchor.dayOfWeek.value - 1).toLong())
        else -> pageAnchor
    }
    val dayCount = when (scale) {
        TimelineScale.Day,
        TimelineScale.List,
        -> 1

        TimelineScale.Week -> 7
        TimelineScale.Month -> MONTH_GRID_DAY_COUNT
    }

    return (0 until dayCount)
        .asSequence()
        .map { pageStart.plusDays(it.toLong()) }
        .toPersistentList()
}

/** Convenience form for callers that already hold a page key. */
fun TimelinePageKey.pageDates(): ImmutableList<LocalDate> = visibleDates

private const val MONTH_GRID_DAY_COUNT = 42
