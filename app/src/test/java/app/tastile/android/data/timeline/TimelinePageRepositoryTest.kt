package app.tastile.android.data.timeline

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.timeline.local.TimelineCacheDao
import app.tastile.android.data.timeline.local.TimelineCacheMapper
import app.tastile.android.data.timeline.local.TimelineCoverageEntity
import app.tastile.android.data.timeline.local.TimelineDayMembershipEntity
import app.tastile.android.data.timeline.local.TimelineItemEntity
import app.tastile.android.ui.dashboard.TimelineScale
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelinePageRepositoryTest {
    private val zone = ZoneId.of("America/New_York")
    private val day = LocalDate.of(2026, 9, 16)

    @Test
    fun observePage_readsOnlyLocalDaoAndNeverCallsNetwork() = runTest {
        val dao = FakeTimelineCacheDao()
        val repository = DefaultTimelinePageRepository(dao)
        val key = key(TimelineScale.Day, day)

        val snapshot = firstSnapshot(repository, dao, key, emptyList(), emptyList())

        assertTrue(snapshot.items.isEmpty())
        assertEquals(1, dao.itemObserveCount.get())
        assertEquals(1, dao.coverageObserveCount.get())
    }

    @Test
    fun unchangedRows_reuseSnapshotInstance() = runTest {
        val dao = FakeTimelineCacheDao()
        val repository = DefaultTimelinePageRepository(dao)
        val key = key(TimelineScale.Day, day)
        val entity = TimelineCacheMapper.toEntity(
            "account-a",
            "scope-a",
            item(id = "item-a", title = "Keep"),
        )
        val first = firstSnapshot(repository, dao, key, listOf(entity), listOf(coverage(day)))
        val second = replayedSnapshot(repository, key)

        assertSame(first, second)
    }

    @Test
    fun changedTitle_emitsNewSnapshotEvenWhenIdsMatch() = runTest {
        val dao = FakeTimelineCacheDao()
        val repository = DefaultTimelinePageRepository(dao)
        val key = key(TimelineScale.Day, day)
        val original = TimelineCacheMapper.toEntity(
            "account-a",
            "scope-a",
            item(id = "item-a", title = "Before"),
        )
        val changed = TimelineCacheMapper.toEntity(
            "account-a",
            "scope-a",
            item(id = "item-a", title = "After"),
        )
        val first = firstSnapshot(repository, dao, key, listOf(original), listOf(coverage(day)))
        val secondResult = async(start = CoroutineStart.UNDISPATCHED) {
            repository.observePage(key).first()
        }
        dao.itemFlow.emit(listOf(changed))
        val second = secondResult.await()

        assertEquals("Before", first.items.single().title)
        assertEquals("After", second.items.single().title)
        assertNotSame(first, second)
    }

    @Test
    fun neverFetchedAndFetchedEmptyRemainDistinct() = runTest {
        val dao = FakeTimelineCacheDao()
        val repository = DefaultTimelinePageRepository(dao)
        val key = key(TimelineScale.Day, day)
        val first = firstSnapshot(repository, dao, key, emptyList(), emptyList())
        val secondResult = async(start = CoroutineStart.UNDISPATCHED) {
            repository.observePage(key).first()
        }
        dao.coverageFlow.emit(listOf(coverage(day)))
        val second = secondResult.await()

        assertEquals(TimelineCoverageState.NeverFetched, first.coverage[day])
        assertEquals(TimelineCoverageState.Available, second.coverage[day])
        assertNotSame(first, second)
    }

    @Test
    fun dayWeekMonthComposeTheSameMembershipRows() = runTest {
        val overnightItem = item(
            id = "item-overnight",
            title = "Overnight",
            startAt = "2026-09-16T23:30:00Z",
            endAt = "2026-09-17T01:30:00Z",
        )
        val outsideItem = item(id = "item-outside", title = "Outside")
        val adjacentItem = item(id = "item-adjacent", title = "Adjacent")
        val entity = TimelineCacheMapper.toEntity("account-a", "scope-a", overnightItem)
        val outsideEntity = TimelineCacheMapper.toEntity("account-a", "scope-a", outsideItem)
        val adjacentEntity = TimelineCacheMapper.toEntity("account-a", "scope-a", adjacentItem)
        val foreignEntity = TimelineCacheMapper.toEntity("account-b", "scope-a", item("item-foreign", "Foreign"))
        val memberships = listOf(
            membership("item-overnight", LocalDate.of(2026, 9, 16)),
            membership("item-overnight", LocalDate.of(2026, 9, 17)),
            membership("item-outside", LocalDate.of(2026, 10, 20)),
            membership("item-adjacent", LocalDate.of(2026, 8, 31)),
            membership("item-foreign", LocalDate.of(2026, 9, 16), accountId = "account-b"),
            // These rows must not make the requested page include a foreign
            // scope, account, or timezone membership.
            membership("item-overnight", LocalDate.of(2026, 9, 16), scopeKey = "scope-other"),
            membership("item-overnight", LocalDate.of(2026, 9, 16), zoneId = "UTC"),
        )
        val resultByScale = TimelineScale.entries
            .filter { it != TimelineScale.List }
            .associateWith { scale ->
                val dao = FakeTimelineCacheDao()
                val repository = DefaultTimelinePageRepository(dao)
                val key = key(scale, day)
                firstSnapshot(
                    repository = repository,
                    dao = dao,
                    key = key,
                    items = listOf(entity, outsideEntity, adjacentEntity, foreignEntity),
                    coverage = key.visibleDates.map(::coverage),
                    memberships = memberships,
                )
            }

        assertEquals(
            listOf("item-overnight"),
            resultByScale[TimelineScale.Day]?.items?.map { it.id }?.sorted(),
        )
        assertEquals(
            listOf("item-overnight"),
            resultByScale[TimelineScale.Week]?.items?.map { it.id }?.sorted(),
        )
        assertEquals(
            listOf("item-adjacent", "item-overnight"),
            resultByScale[TimelineScale.Month]?.items?.map { it.id }?.sorted(),
        )
    }

    @Test
    fun purgeAccount_delegatesToDaoAndDropsCachedSnapshot() = runTest {
        val dao = FakeTimelineCacheDao()
        val repository = DefaultTimelinePageRepository(dao)
        val key = key(TimelineScale.Day, day)
        val entity = TimelineCacheMapper.toEntity(
            "account-a",
            "scope-a",
            item(id = "item-a", title = "Cached"),
        )

        val first = firstSnapshot(repository, dao, key, listOf(entity), listOf(coverage(day)))
        repository.purgeAccount("account-a")
        dao.itemFlow.emit(emptyList())
        dao.coverageFlow.emit(emptyList())
        val afterPurge = repository.observePage(key).first()

        assertEquals(listOf("account-a"), dao.purgedAccounts)
        assertTrue(afterPurge.items.isEmpty())
        assertNotSame(first, afterPurge)
    }

    private suspend fun firstSnapshot(
        repository: TimelinePageRepository,
        dao: FakeTimelineCacheDao,
        key: TimelinePageKey,
        items: List<TimelineItemEntity>,
        coverage: List<TimelineCoverageEntity>,
        memberships: List<TimelineDayMembershipEntity> = items.flatMap { item ->
            key.visibleDates.map { date -> membership(item.itemId, date) }
        },
    ): TimelinePageSnapshot = coroutineScope {
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            repository.observePage(key).first()
        }
        dao.itemFlow.emit(items)
        dao.membershipFlow.emit(memberships)
        dao.coverageFlow.emit(coverage)
        result.await()
    }

    private suspend fun replayedSnapshot(
        repository: TimelinePageRepository,
        key: TimelinePageKey,
    ): TimelinePageSnapshot = coroutineScope {
        async(start = CoroutineStart.UNDISPATCHED) {
            repository.observePage(key).first()
        }.await()
    }

    private fun key(scale: TimelineScale, anchor: LocalDate): TimelinePageKey = TimelinePageKey(
        accountId = "account-a",
        scopeKey = "scope-a",
        zoneId = zone,
        scale = scale,
        anchor = anchor,
    )

    private fun item(
        id: String,
        title: String,
        startAt: String = "2026-09-16T09:00:00Z",
        endAt: String? = "2026-09-16T10:00:00Z",
    ): CoreTimelineItem = CoreTimelineItem(
        id = id,
        tileId = "tile-$id",
        sourceKind = 4,
        title = title,
        type = "placement",
        status = "open",
        startAt = startAt,
        endAt = endAt,
        sourceTileId = "source-$id",
    )

    private fun coverage(localDate: LocalDate): TimelineCoverageEntity = TimelineCoverageEntity(
        accountId = "account-a",
        scopeKey = "scope-a",
        zoneId = zone.id,
        localDate = localDate.toString(),
        fetchedAtEpochMs = 1_000L,
        contractVersion = 1,
        lastFailureKind = null,
        lastAccessedAtEpochMs = 1_000L,
        refreshGeneration = 1L,
    )

    private fun membership(
        itemId: String,
        localDate: LocalDate,
        accountId: String = "account-a",
        scopeKey: String = "scope-a",
        zoneId: String = zone.id,
    ): TimelineDayMembershipEntity = TimelineDayMembershipEntity(
        accountId = accountId,
        scopeKey = scopeKey,
        zoneId = zoneId,
        localDate = localDate.toString(),
        itemId = itemId,
    )

    private class FakeTimelineCacheDao : TimelineCacheDao() {
        val itemFlow = MutableSharedFlow<List<TimelineItemEntity>>(replay = 1)
        val membershipFlow = MutableSharedFlow<List<TimelineDayMembershipEntity>>(replay = 1)
        val coverageFlow = MutableSharedFlow<List<TimelineCoverageEntity>>(replay = 1)
        val itemObserveCount = AtomicInteger(0)
        val coverageObserveCount = AtomicInteger(0)
        val purgedAccounts = mutableListOf<String>()

        override fun observeRange(
            accountId: String,
            scopeKey: String,
            zoneId: String,
            localDates: List<String>,
            rangeStartEpochMs: Long,
            rangeEndEpochMs: Long,
        ): Flow<List<TimelineItemEntity>> {
            itemObserveCount.incrementAndGet()
            return combine(itemFlow, membershipFlow) { items, memberships ->
                val requestedDates = localDates.toSet()
                val memberItemIds = memberships.asSequence()
                    .filter { membership ->
                        membership.accountId == accountId &&
                            membership.scopeKey == scopeKey &&
                            membership.zoneId == zoneId &&
                            membership.localDate in requestedDates
                    }
                    .map { it.itemId }
                    .toSet()
                items.filter { item ->
                    item.accountId == accountId &&
                        item.scopeKey == scopeKey &&
                        item.itemId in memberItemIds &&
                        item.startEpochMs < rangeEndEpochMs &&
                        (item.endEpochMs == null || item.endEpochMs > rangeStartEpochMs)
                }
            }
        }

        override fun observeRange(
            accountId: String,
            scopeKey: String,
            rangeStartEpochMs: Long,
            rangeEndEpochMs: Long,
        ): Flow<List<TimelineItemEntity>> = itemFlow.map { items ->
            items.filter { item ->
                item.accountId == accountId &&
                    item.scopeKey == scopeKey &&
                    item.startEpochMs < rangeEndEpochMs &&
                    (item.endEpochMs == null || item.endEpochMs > rangeStartEpochMs)
            }
        }

        override fun observeCoverageForDates(
            accountId: String,
            scopeKey: String,
            zoneId: String,
            localDates: List<String>,
        ): Flow<List<TimelineCoverageEntity>> {
            coverageObserveCount.incrementAndGet()
            return coverageFlow.map { coverage ->
                coverage.filter { entity ->
                    entity.accountId == accountId &&
                        entity.scopeKey == scopeKey &&
                        entity.zoneId == zoneId &&
                        entity.localDate in localDates
                }
            }
        }

        override suspend fun upsertItems(items: List<TimelineItemEntity>) = Unit

        override suspend fun upsertMemberships(memberships: List<TimelineDayMembershipEntity>) = Unit

        override suspend fun upsertCoverage(coverage: List<TimelineCoverageEntity>) = Unit

        override suspend fun deleteMembershipsForDates(
            accountId: String,
            scopeKey: String,
            zoneId: String,
            localDates: List<String>,
        ) = Unit

        override suspend fun deleteItemsForAccount(accountId: String) = Unit

        override suspend fun deleteMembershipsForAccount(accountId: String) = Unit

        override suspend fun deleteCoverageForAccount(accountId: String) = Unit

        override suspend fun purgeAccount(accountId: String) {
            purgedAccounts += accountId
        }
    }
}
