package app.tastile.android.data.timeline.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
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
        if (items.isNotEmpty()) upsertItems(items)

        coverage
            .map { Triple(it.accountId, it.scopeKey, it.zoneId) }
            .distinct()
            .forEach { (accountId, scopeKey, zoneId) ->
                val localDates = coverage
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

        if (memberships.isNotEmpty()) upsertMemberships(memberships)
        if (coverage.isNotEmpty()) upsertCoverage(coverage)
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
}
