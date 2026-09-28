package app.tastile.android.data.timeline

import app.tastile.android.data.timeline.local.TimelineCacheDao
import app.tastile.android.data.timeline.local.TimelineCoverageEntity
import app.tastile.android.ui.dashboard.TimelineScale
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class TimelineRefreshDirection {
    Previous,
    Next,
    None,
}

/** Application-owned scope keeps refresh jobs independent from Compose state. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TimelineApplicationScope

/**
 * Queues coverage-aware refresh intents in priority order. The caller only
 * supplies visible page keys; all coverage and network work stays outside UI
 * state and is performed by [TimelineSyncRepository].
 */
@Singleton
class TimelineRefreshCoordinator @Inject constructor(
    private val syncRepository: TimelineSyncRepository,
    private val dao: TimelineCacheDao,
    @TimelineApplicationScope private val applicationScope: CoroutineScope,
    private val clock: Clock,
) {
    /** Convenience constructor for JVM callers that do not use Hilt. */
    constructor(
        syncRepository: TimelineSyncRepository,
        dao: TimelineCacheDao,
        applicationScope: CoroutineScope,
    ) : this(syncRepository, dao, applicationScope, Clock.systemUTC())

    /**
     * Enqueues refreshes and returns immediately. Keys should be ordered with
     * the current page first, followed by its adjacent pages; the coordinator
     * still derives adjacent roles from normalized anchors for deterministic
     * priority.
     */
    fun requestRefresh(
        keys: Collection<TimelinePageKey>,
        direction: TimelineRefreshDirection = TimelineRefreshDirection.None,
    ) {
        val normalizedKeys = keys
            .map(TimelinePageKey::normalized)
            .distinct()
        if (normalizedKeys.isEmpty()) return

        applicationScope.launch {
            val now = clock.instant()
            val decisions = normalizedKeys.mapIndexed { index, key ->
                val coverage = dao.observeCoverage(
                    accountId = key.accountId,
                    scopeKey = key.normalizedScopeFingerprint,
                    zoneId = key.zoneId.id,
                    localDates = key.visibleDates.map(LocalDate::toString),
                ).first()
                RefreshDecision(
                    key = key,
                    index = index,
                    refreshKind = refreshKind(coverage, key, now),
                    role = roleOf(key, normalizedKeys.first(), direction),
                )
            }

            decisions
                .filter { it.refreshKind != RefreshKind.Fresh }
                .sortedWith(compareBy<RefreshDecision>({ it.priority }, { it.index }))
                .forEach { decision ->
                    syncRepository.refresh(
                        TimelineRefreshRequest(
                            key = decision.key,
                            ownerIds = decision.key.normalizedOwnerIds,
                            now = now,
                        ),
                    )
                }
        }
    }

    private fun refreshKind(
        coverage: List<TimelineCoverageEntity>,
        key: TimelinePageKey,
        now: java.time.Instant,
    ): RefreshKind {
        val byDate = coverage.associateBy { it.localDate }
        val staleCutoff = now.toEpochMilli() - TimelineRefreshRequest.DEFAULT_TIMELINE_STALE_AFTER.toMillis()
        val dates = key.visibleDates.map { date ->
            val row = byDate[date.toString()]
            when {
                row == null -> RefreshKind.Missing
                row.lastFailureKind != null || row.fetchedAtEpochMs < staleCutoff -> RefreshKind.Stale
                else -> RefreshKind.Fresh
            }
        }
        return when {
            dates.any { it == RefreshKind.Missing } -> RefreshKind.Missing
            dates.any { it == RefreshKind.Stale } -> RefreshKind.Stale
            else -> RefreshKind.Fresh
        }
    }

    private fun roleOf(
        key: TimelinePageKey,
        current: TimelinePageKey,
        direction: TimelineRefreshDirection,
    ): PageRole {
        if (key == current) return PageRole.Current
        val previous = shiftAnchor(current, -1)
        val next = shiftAnchor(current, 1)
        return when {
            direction == TimelineRefreshDirection.Next && key == next -> PageRole.DirectionalAdjacent
            direction == TimelineRefreshDirection.Previous && key == previous -> PageRole.DirectionalAdjacent
            direction == TimelineRefreshDirection.Next && key == previous -> PageRole.OppositeAdjacent
            direction == TimelineRefreshDirection.Previous && key == next -> PageRole.OppositeAdjacent
            key == previous || key == next -> PageRole.Adjacent
            else -> PageRole.Other
        }
    }

    private fun shiftAnchor(key: TimelinePageKey, amount: Long): TimelinePageKey {
        val anchor = when (key.scale) {
            TimelineScale.Day,
            TimelineScale.List,
            -> key.anchor.plusDays(amount)

            TimelineScale.Week -> key.anchor.plusWeeks(amount)
            TimelineScale.Month -> key.anchor.plusMonths(amount)
        }
        return key.copy(anchor = anchor).normalized()
    }

    private enum class PageRole(val priority: Int) {
        Current(0),
        DirectionalAdjacent(1),
        Adjacent(2),
        OppositeAdjacent(3),
        Other(4),
    }

    private enum class RefreshKind {
        Missing,
        Stale,
        Fresh,
    }

    private data class RefreshDecision(
        val key: TimelinePageKey,
        val index: Int,
        val refreshKind: RefreshKind,
        val role: PageRole,
    ) {
        val priority: Int
            get() = (if (refreshKind == RefreshKind.Missing) 0 else 10) + role.priority
    }
}
