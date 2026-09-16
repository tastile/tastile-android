package app.tastile.android.data.timeline.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

@Dao
abstract class TimelineCacheDao {

    /**
     * Observes items represented by at least one of [localDates]. A placement
     * can have membership rows for more than one day, so DISTINCT is required
     * to keep an overnight item from appearing more than once.
     */
    fun observeRange(
        accountId: String,
        scopeKey: String,
        zoneId: String,
        localDates: List<String>,
    ): Flow<List<TimelineItemEntity>> {
        if (localDates.isEmpty()) return flowOf(emptyList())
        return observeRange(
            accountId = accountId,
            scopeKey = scopeKey,
            zoneId = zoneId,
            localDates = localDates,
            rangeStartEpochMs = Long.MIN_VALUE,
            rangeEndEpochMs = Long.MAX_VALUE,
        )
    }

    /**
     * Observes items overlapping the requested UTC range and represented by a
     * membership in one of the requested local dates.
     */
    @Query(
        """
        SELECT DISTINCT i.*
        FROM timeline_items AS i
        INNER JOIN timeline_day_memberships AS m
            ON m.accountId = i.accountId
            AND m.scopeKey = i.scopeKey
            AND m.itemId = i.itemId
        WHERE i.accountId = :accountId
          AND i.scopeKey = :scopeKey
          AND m.zoneId = :zoneId
          AND m.localDate IN (:localDates)
          AND i.startEpochMs < :rangeEndEpochMs
          AND (i.endEpochMs IS NULL OR i.endEpochMs > :rangeStartEpochMs)
        ORDER BY i.startEpochMs ASC, i.itemId ASC
        """
    )
    abstract fun observeRange(
        accountId: String,
        scopeKey: String,
        zoneId: String,
        localDates: List<String>,
        rangeStartEpochMs: Long,
        rangeEndEpochMs: Long,
    ): Flow<List<TimelineItemEntity>>

    /**
     * Observes all cached items in a UTC range when a caller already has a
     * range rather than local-date membership keys.
     */
    @Query(
        """
        SELECT DISTINCT i.*
        FROM timeline_items AS i
        WHERE i.accountId = :accountId
          AND i.scopeKey = :scopeKey
          AND i.startEpochMs < :rangeEndEpochMs
          AND (i.endEpochMs IS NULL OR i.endEpochMs > :rangeStartEpochMs)
        ORDER BY i.startEpochMs ASC, i.itemId ASC
        """
    )
    abstract fun observeRange(
        accountId: String,
        scopeKey: String,
        rangeStartEpochMs: Long,
        rangeEndEpochMs: Long,
    ): Flow<List<TimelineItemEntity>>

    fun observeCoverage(
        accountId: String,
        scopeKey: String,
        zoneId: String,
        localDates: List<String>,
    ): Flow<List<TimelineCoverageEntity>> {
        if (localDates.isEmpty()) return flowOf(emptyList())
        return observeCoverageForDates(accountId, scopeKey, zoneId, localDates)
    }

    @Query(
        """
        SELECT *
        FROM timeline_coverage
        WHERE accountId = :accountId
          AND scopeKey = :scopeKey
          AND zoneId = :zoneId
          AND localDate IN (:localDates)
        ORDER BY localDate ASC
        """
    )
    protected abstract fun observeCoverageForDates(
        accountId: String,
        scopeKey: String,
        zoneId: String,
        localDates: List<String>,
    ): Flow<List<TimelineCoverageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertItems(items: List<TimelineItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertMemberships(memberships: List<TimelineDayMembershipEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertCoverage(coverage: List<TimelineCoverageEntity>)

    @Query(
        """
        DELETE FROM timeline_day_memberships
        WHERE accountId = :accountId
          AND scopeKey = :scopeKey
          AND zoneId = :zoneId
          AND localDate IN (:localDates)
        """
    )
    protected abstract suspend fun deleteMembershipsForDates(
        accountId: String,
        scopeKey: String,
        zoneId: String,
        localDates: List<String>,
    )

    /**
     * Replaces memberships and coverage for exactly the days returned by a
     * successful API response. Empty item lists still upsert coverage, which
     * distinguishes an available empty day from a day never fetched.
     */
    @Transaction
    open suspend fun replaceDays(
        items: List<TimelineItemEntity>,
        memberships: List<TimelineDayMembershipEntity>,
        coverage: List<TimelineCoverageEntity>,
    ) {
        if (coverage.isEmpty()) return

        // A late response must not replace a newer response for the same
        // account/scope/zone/date. Filter the whole write set before touching
        // any row so stale items and memberships cannot leak through a fresh
        // coverage row.
        val currentCoverage = loadCoverage(coverage)
        val acceptedCoverage = coverage.filter { candidate ->
            val current = currentCoverage[candidate.coverageKey()]
            current == null || candidate.refreshGeneration >= current.refreshGeneration
        }
        if (acceptedCoverage.isEmpty()) return

        val acceptedKeys = acceptedCoverage.mapTo(mutableSetOf()) { it.coverageKey() }
        val acceptedMemberships = memberships.filter { it.membershipKey() in acceptedKeys }
        val acceptedItemIds = acceptedMemberships
            .mapTo(mutableSetOf()) { Triple(it.accountId, it.scopeKey, it.itemId) }
        val acceptedItems = items.filter {
            Triple(it.accountId, it.scopeKey, it.itemId) in acceptedItemIds
        }

        if (acceptedItems.isNotEmpty()) upsertItems(acceptedItems)

        coverage
            .filter { it.coverageKey() in acceptedKeys }
            .map { Triple(it.accountId, it.scopeKey, it.zoneId) }
            .distinct()
            .forEach { (accountId, scopeKey, zoneId) ->
                val localDates = acceptedCoverage
                    .asSequence()
                    .filter {
                        it.accountId == accountId &&
                            it.scopeKey == scopeKey &&
                            it.zoneId == zoneId
                    }
                    .map { it.localDate }
                    .distinct()
                    .toList()
                if (localDates.isNotEmpty()) {
                    deleteMembershipsForDates(accountId, scopeKey, zoneId, localDates)
                }
            }

        if (acceptedMemberships.isNotEmpty()) upsertMemberships(acceptedMemberships)
        if (acceptedCoverage.isNotEmpty()) upsertCoverage(acceptedCoverage)
    }

    /**
     * Records a failed refresh without replacing or deleting cached timeline
     * items and memberships. Existing coverage retains its successful fetch
     * timestamp and contract version; only failure metadata and the refresh
     * generation are advanced. Missing coverage rows are inserted so a
     * failed first fetch is observable as metadata rather than an empty
     * successful day.
     */
    @Transaction
    open suspend fun recordFailures(failures: List<TimelineCoverageEntity>) {
        if (failures.isEmpty()) return

        val currentCoverage = loadCoverage(failures)
        val updates = failures.mapNotNull { failure ->
            val current = currentCoverage[failure.coverageKey()]
            when {
                current == null -> failure
                failure.refreshGeneration >= current.refreshGeneration -> current.copy(
                    lastFailureKind = failure.lastFailureKind,
                    refreshGeneration = failure.refreshGeneration,
                )
                else -> null
            }
        }
        if (updates.isNotEmpty()) upsertCoverage(updates)
    }

    @Query("DELETE FROM timeline_items WHERE accountId = :accountId")
    protected abstract suspend fun deleteItemsForAccount(accountId: String)

    @Query("DELETE FROM timeline_day_memberships WHERE accountId = :accountId")
    protected abstract suspend fun deleteMembershipsForAccount(accountId: String)

    @Query("DELETE FROM timeline_coverage WHERE accountId = :accountId")
    protected abstract suspend fun deleteCoverageForAccount(accountId: String)

    @Transaction
    open suspend fun purgeAccount(accountId: String) {
        deleteMembershipsForAccount(accountId)
        deleteCoverageForAccount(accountId)
        deleteItemsForAccount(accountId)
    }

    private suspend fun loadCoverage(
        rows: List<TimelineCoverageEntity>,
    ): Map<CoverageKey, TimelineCoverageEntity> = rows
        .groupBy { Triple(it.accountId, it.scopeKey, it.zoneId) }
        .values
        .flatMap { group ->
            val first = group.first()
            val dates = group.map(TimelineCoverageEntity::localDate).distinct()
            observeCoverageForDates(
                accountId = first.accountId,
                scopeKey = first.scopeKey,
                zoneId = first.zoneId,
                localDates = dates,
            ).first()
        }
        .associateBy { it.coverageKey() }

    private fun TimelineCoverageEntity.coverageKey(): CoverageKey = CoverageKey(
        accountId = accountId,
        scopeKey = scopeKey,
        zoneId = zoneId,
        localDate = localDate,
    )

    private fun TimelineDayMembershipEntity.membershipKey(): CoverageKey = CoverageKey(
        accountId = accountId,
        scopeKey = scopeKey,
        zoneId = zoneId,
        localDate = localDate,
    )

    private data class CoverageKey(
        val accountId: String,
        val scopeKey: String,
        val zoneId: String,
        val localDate: String,
    )
}
