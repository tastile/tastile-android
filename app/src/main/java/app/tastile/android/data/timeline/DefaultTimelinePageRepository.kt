package app.tastile.android.data.timeline

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.timeline.local.TimelineCacheDao
import app.tastile.android.data.timeline.local.TimelineCacheMapper
import app.tastile.android.data.timeline.local.TimelineCoverageEntity
import app.tastile.android.data.timeline.local.TimelineItemEntity
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import java.time.LocalDate
import java.util.LinkedHashMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Composes page snapshots entirely from the Room timeline read model.
 *
 * This class intentionally has no transport dependency. Synchronization code
 * writes the cache, and Room invalidation is the only way a page observation
 * changes.
 */
@Singleton
class DefaultTimelinePageRepository @Inject constructor(
    private val dao: TimelineCacheDao,
) : TimelinePageRepository {
    private val snapshotCache = object : LinkedHashMap<TimelinePageKey, TimelinePageSnapshot>(
        SNAPSHOT_CACHE_SIZE,
        CACHE_LOAD_FACTOR,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<TimelinePageKey, TimelinePageSnapshot>,
        ): Boolean = size > SNAPSHOT_CACHE_SIZE
    }
    private val snapshotCacheLock = Any()

    override fun observePage(key: TimelinePageKey): Flow<TimelinePageSnapshot> {
        val normalizedKey = key.normalized()
        val localDates = normalizedKey.visibleDates
        val localDateStrings = localDates.map(LocalDate::toString)

        return combine(
            dao.observeRange(
                accountId = normalizedKey.accountId,
                scopeKey = normalizedKey.normalizedScopeFingerprint,
                zoneId = normalizedKey.zoneId.id,
                localDates = localDateStrings,
            ),
            dao.observeCoverage(
                accountId = normalizedKey.accountId,
                scopeKey = normalizedKey.normalizedScopeFingerprint,
                zoneId = normalizedKey.zoneId.id,
                localDates = localDateStrings,
            ),
        ) { itemEntities, coverageEntities ->
            composeSnapshot(normalizedKey, localDates, itemEntities, coverageEntities)
        }
            .map(::internSnapshot)
            .distinctUntilChanged()
    }

    override fun observeProjection(request: TimelineProjectionRequest): Flow<List<CoreTimelineItem>> {
        val normalizedRequest = request.normalized()
        val localDates = normalizedRequest.localDates
        if (localDates.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())

        val rangeStart = localDates.first()
            .atStartOfDay(normalizedRequest.zoneId)
            .toInstant()
            .toEpochMilli()
        val rangeEnd = localDates.last()
            .plusDays(1)
            .atStartOfDay(normalizedRequest.zoneId)
            .toInstant()
            .toEpochMilli()

        // This is intentionally one membership-aware Room observation for
        // the entire request. In particular, CUSTOM must not create one Flow
        // per day or multiply collectors with the selected range length.
        return dao.observeRange(
            accountId = normalizedRequest.accountId,
            scopeKey = normalizedRequest.scopeFingerprint,
            zoneId = normalizedRequest.zoneId.id,
            localDates = localDates.map(LocalDate::toString),
            rangeStartEpochMs = rangeStart,
            rangeEndEpochMs = rangeEnd,
        ).map { entities ->
            entities
                .mapNotNull { entity -> runCatching { TimelineCacheMapper.fromEntity(entity) }.getOrNull() }
                .distinctBy { it.id }
        }.distinctUntilChanged()
    }

    override suspend fun purgeAccount(accountId: String) {
        dao.purgeAccount(accountId)
        synchronized(snapshotCacheLock) {
            snapshotCache.keys.removeAll { it.accountId == accountId }
        }
    }

    private fun composeSnapshot(
        key: TimelinePageKey,
        localDates: List<LocalDate>,
        itemEntities: List<TimelineItemEntity>,
        coverageEntities: List<TimelineCoverageEntity>,
    ): TimelinePageSnapshot {
        val items = itemEntities
            .asSequence()
            .filter { entity ->
                entity.accountId == key.accountId && entity.scopeKey == key.normalizedScopeFingerprint
            }
            .distinctBy { it.itemId }
            .mapNotNull { entity ->
                runCatching { TimelineCacheMapper.fromEntity(entity) }.getOrNull()
            }
            .toPersistentList()

        val pageCoverageEntities = coverageEntities.filter { entity ->
            entity.accountId == key.accountId &&
                entity.scopeKey == key.normalizedScopeFingerprint &&
                entity.zoneId == key.zoneId.id &&
                entity.localDate in localDates.map(LocalDate::toString)
        }
        val coverageByDate = pageCoverageEntities.asSequence()
            .mapNotNull { entity ->
                runCatching { LocalDate.parse(entity.localDate) }
                    .getOrNull()
                    ?.let { date -> date to entity }
            }
            .toMap()
        val coverage = localDates.associateWith { date ->
            coverageByDate[date]?.let(::coverageState)
                ?: TimelineCoverageState.NeverFetched
        }.toPersistentMap()
        val lastUpdatedAt = pageCoverageEntities
            .maxOfOrNull(TimelineCoverageEntity::fetchedAtEpochMs)
            ?.let(Instant::ofEpochMilli)

        return TimelinePageSnapshot(
            key = key,
            items = items,
            coverage = coverage,
            lastUpdatedAt = lastUpdatedAt,
            isRefreshing = false,
            isOffline = false,
        )
    }

    private fun coverageState(entity: TimelineCoverageEntity): TimelineCoverageState =
        if (entity.lastFailureKind == null) {
            TimelineCoverageState.Available
        } else {
            TimelineCoverageState.Stale
        }

    private fun internSnapshot(snapshot: TimelinePageSnapshot): TimelinePageSnapshot =
        synchronized(snapshotCacheLock) {
            snapshotCache[snapshot.key]?.takeIf { it == snapshot } ?: snapshot.also {
                snapshotCache[snapshot.key] = it
            }
        }

    private companion object {
        const val SNAPSHOT_CACHE_SIZE = 32
        const val CACHE_LOAD_FACTOR = 0.75f
    }
}
