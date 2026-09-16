package app.tastile.android.data.timeline

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.timeline.local.TimelineCacheDao
import app.tastile.android.data.timeline.local.TimelineCoverageEntity
import app.tastile.android.data.timeline.local.TimelineDayMembershipEntity
import app.tastile.android.data.timeline.local.TimelineItemEntity
import app.tastile.android.data.tile.TileRepository
import app.tastile.android.data.tile.TimelineFetchResult
import app.tastile.android.ui.dashboard.TimelineScale
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineSyncRepositoryTest {
    private val zone = ZoneId.of("UTC")
    private val firstDay = LocalDate.of(2026, 9, 16)

    @Test
    fun refresh_writesItemsMembershipAndCoverageInOneDaoCall() = runTest {
        val dao = RecordingTimelineCacheDao()
        val tileRepository = mockk<TileRepository>()
        val item = item(
            id = "overnight",
            startAt = "2026-09-16T23:30:00Z",
            endAt = "2026-09-17T01:30:00Z",
        )
        coEvery { tileRepository.getTimelineCanonical(any(), any(), any()) } returns
            TimelineFetchResult.Success(listOf(item))
        val repository = DefaultTimelineSyncRepository(tileRepository, dao)

        val result = repository.refresh(request(scale = TimelineScale.Week))

        assertEquals(TimelineRefreshStatus.Refreshed, result.status)
        assertEquals(1, dao.replaceCalls.size)
        assertEquals(listOf("overnight"), dao.replaceCalls.single().items.map { it.itemId })
        assertEquals(
            setOf("2026-09-16", "2026-09-17"),
            dao.replaceCalls.single().memberships.map { it.localDate }.toSet(),
        )
        assertEquals(
            request(scale = TimelineScale.Week).key.visibleDates.map(LocalDate::toString),
            dao.replaceCalls.single().coverage.map { it.localDate },
        )
        assertEquals(1, dao.replaceCalls.single().coverage.first().refreshGeneration)
        assertTrue(result.ranges.single().start.isBefore(result.ranges.single().end))
    }

    @Test
    fun emptySuccess_marksDaysAvailableAndRemovesOldMembership() = runTest {
        val dao = RecordingTimelineCacheDao(
            coverage = listOf(coverage(firstDay)),
            memberships = listOf(membership(firstDay, "old")),
            items = listOf(itemEntity("old")),
        )
        val tileRepository = mockk<TileRepository>()
        coEvery { tileRepository.getTimelineCanonical(any(), any(), any()) } returns
            TimelineFetchResult.Success(emptyList())
        val repository = DefaultTimelineSyncRepository(tileRepository, dao)

        val result = repository.refresh(request())

        assertEquals(TimelineRefreshStatus.Refreshed, result.status)
        assertEquals(1, dao.replaceCalls.size)
        assertTrue(dao.replaceCalls.single().items.isEmpty())
        assertTrue(dao.replaceCalls.single().memberships.isEmpty())
        assertEquals(null, dao.replaceCalls.single().coverage.single().lastFailureKind)
        // The recording seam applies the same replacement semantics as the
        // Room transaction; importantly, the repository itself made no write
        // before the successful empty response.
    }

    @Test
    fun networkFailure_preservesExistingRowsAndRecordsFailure() = runTest {
        val existingItem = itemEntity("existing")
        val existingMembership = membership(firstDay, existingItem.itemId)
        val existingCoverage = coverage(firstDay)
        val dao = RecordingTimelineCacheDao(
            coverage = listOf(existingCoverage),
            memberships = listOf(existingMembership),
            items = listOf(existingItem),
        )
        val tileRepository = mockk<TileRepository>()
        coEvery { tileRepository.getTimelineCanonical(any(), any(), any()) } returns
            TimelineFetchResult.Failure(IllegalStateException("network down"))
        val repository = DefaultTimelineSyncRepository(tileRepository, dao)

        val result = repository.refresh(request())

        assertEquals(TimelineRefreshStatus.Failed, result.status)
        assertTrue(dao.replaceCalls.isEmpty())
        assertEquals(listOf(existingItem), dao.items)
        assertEquals(listOf(existingMembership), dao.memberships)
        assertEquals(1, dao.failureCalls.size)
        assertEquals("network", dao.failureCalls.single().single().lastFailureKind)
        assertEquals(existingCoverage.fetchedAtEpochMs, dao.coverageFlow.value.single().fetchedAtEpochMs)
    }

    @Test
    fun overlappingRequests_areCoalesced() = runTest {
        val dao = RecordingTimelineCacheDao()
        val tileRepository = mockk<TileRepository>()
        val fetchStarted = CompletableDeferred<Unit>()
        val releaseFetch = CompletableDeferred<Unit>()
        val fetchCount = AtomicInteger()
        coEvery { tileRepository.getTimelineCanonical(any(), any(), any()) } coAnswers {
            fetchCount.incrementAndGet()
            fetchStarted.complete(Unit)
            releaseFetch.await()
            TimelineFetchResult.Success(emptyList())
        }
        val repository = DefaultTimelineSyncRepository(tileRepository, dao)

        // The second request is a narrower Day page wholly covered by the
        // first in-flight Week request; both must share one fetch/transaction.
        val first = async { repository.refresh(request(scale = TimelineScale.Week)) }
        fetchStarted.await()
        val second = async { repository.refresh(request()) }
        releaseFetch.complete(Unit)

        assertEquals(TimelineRefreshStatus.Refreshed, first.await().status)
        assertEquals(TimelineRefreshStatus.Refreshed, second.await().status)
        assertEquals(1, fetchCount.get())
        assertEquals(1, dao.replaceCalls.size)
    }

    @Test
    fun olderGeneration_cannotOverwriteNewerCoverage() = runTest {
        val dao = RecordingTimelineCacheDao()
        val tileRepository = mockk<TileRepository>()
        val firstFetchStarted = CompletableDeferred<Unit>()
        val releaseFirstFetch = CompletableDeferred<Unit>()
        coEvery { tileRepository.getTimelineCanonical(any(), any(), any()) } coAnswers {
            if (firstFetchStarted.complete(Unit)) {
                releaseFirstFetch.await()
                TimelineFetchResult.Success(
                    listOf(item("old-result", "2026-09-16T09:00:00Z", "2026-09-16T10:00:00Z")),
                )
            } else {
                TimelineFetchResult.Success(
                    listOf(item("new-result", "2026-09-16T11:00:00Z", "2026-09-16T12:00:00Z")),
                )
            }
        }
        val repository = DefaultTimelineSyncRepository(tileRepository, dao)

        val older = async { repository.refresh(request(generation = 1L)) }
        firstFetchStarted.await()
        val newer = async { repository.refresh(request(generation = 2L)) }
        releaseFirstFetch.complete(Unit)
        val newerResult = newer.await()
        val olderResult = older.await()

        assertEquals(TimelineRefreshStatus.Refreshed, newerResult.status)
        assertEquals(TimelineRefreshStatus.Superseded, olderResult.status)
        assertEquals(2L, newerResult.generation)
        assertEquals(1L, olderResult.generation)
        // Both callers share one canonical response; the newer generation is
        // still retained as the winning write metadata.
        assertEquals(listOf("old-result"), dao.items.map { it.itemId })
        assertEquals(2L, dao.coverageFlow.value.single().refreshGeneration)
    }

    @Test
    fun overnightItem_isAssignedToEveryOverlappingLocalDate() = runTest {
        val dao = RecordingTimelineCacheDao()
        val tileRepository = mockk<TileRepository>()
        coEvery { tileRepository.getTimelineCanonical(any(), any(), any()) } returns
            TimelineFetchResult.Success(
                listOf(
                    item(
                        id = "cross-midnight",
                        startAt = "2026-09-16T23:30:00Z",
                        endAt = "2026-09-17T01:30:00Z",
                    ),
                ),
            )
        val repository = DefaultTimelineSyncRepository(tileRepository, dao)
        val weekRequest = request(scale = TimelineScale.Week)

        repository.refresh(weekRequest)

        val matchingDays = dao.replaceCalls.single().memberships
            .filter { it.itemId == "cross-midnight" }
            .map { it.localDate }
            .toSet()
        assertEquals(setOf("2026-09-16", "2026-09-17"), matchingDays)
    }

    @Test
    fun partiallyOverlappingRanges_shareBoundaryDateFetch() = runTest {
        val dao = RecordingTimelineCacheDao()
        val tileRepository = mockk<TileRepository>()
        val firstFetchStarted = CompletableDeferred<Unit>()
        val releaseFirstFetch = CompletableDeferred<Unit>()
        val calls = mutableListOf<Pair<Instant, Instant>>()
        coEvery { tileRepository.getTimelineCanonical(any(), any(), any()) } coAnswers {
            calls += firstArg<Instant>() to secondArg<Instant>()
            if (calls.size == 1) {
                firstFetchStarted.complete(Unit)
                releaseFirstFetch.await()
            }
            TimelineFetchResult.Success(emptyList())
        }
        val repository = DefaultTimelineSyncRepository(tileRepository, dao)
        val firstDates = (0..6).map { firstDay.plusDays(it.toLong()) }
        val secondDates = (6..12).map { firstDay.plusDays(it.toLong()) }

        val first = async {
            repository.refresh(request(generation = 1L, localDates = firstDates))
        }
        firstFetchStarted.await()
        val second = async {
            repository.refresh(request(generation = 2L, localDates = secondDates))
        }
        // Let the second refresh register its shared boundary flight before
        // releasing the first response.
        kotlinx.coroutines.yield()
        releaseFirstFetch.complete(Unit)

        assertEquals(TimelineRefreshStatus.Refreshed, first.await().status)
        assertEquals(TimelineRefreshStatus.Refreshed, second.await().status)
        assertEquals(2, calls.size)
        assertEquals(Instant.parse("2026-09-16T00:00:00Z"), calls[0].first)
        assertEquals(Instant.parse("2026-09-23T00:00:00Z"), calls[1].first)
        assertEquals(
            2L,
            dao.coverageFlow.value.first { it.localDate == "2026-09-22" }.refreshGeneration,
        )
    }

    private fun request(
        scale: TimelineScale = TimelineScale.Day,
        generation: Long? = null,
        localDates: List<LocalDate>? = null,
    ): TimelineRefreshRequest = TimelineRefreshRequest(
        key = TimelinePageKey(
            accountId = "account-a",
            scopeKey = "scope-a",
            zoneId = zone,
            scale = scale,
            anchor = firstDay,
        ),
        ownerIds = listOf("owner-a"),
        now = Instant.parse("2026-09-16T12:00:00Z"),
        staleAfter = java.time.Duration.ofHours(1),
        generation = generation,
        localDates = localDates,
    )

    private fun item(
        id: String,
        startAt: String = "2026-09-16T09:00:00Z",
        endAt: String? = "2026-09-16T10:00:00Z",
    ): CoreTimelineItem = CoreTimelineItem(
        id = id,
        tileId = "tile-$id",
        sourceKind = 1,
        title = "Item $id",
        type = "work",
        status = "scheduled",
        startAt = startAt,
        endAt = endAt,
        sourceTileId = "source-$id",
    )

    private fun itemEntity(id: String): TimelineItemEntity = TimelineItemEntity(
        accountId = "account-a",
        scopeKey = "scope-a",
        itemId = id,
        startEpochMs = 1_000L,
        endEpochMs = 2_000L,
        payloadJson = "{}",
        contentHash = id,
    )

    private fun membership(
        date: LocalDate,
        itemId: String,
    ): TimelineDayMembershipEntity = TimelineDayMembershipEntity(
        accountId = "account-a",
        scopeKey = "scope-a",
        zoneId = zone.id,
        localDate = date.toString(),
        itemId = itemId,
    )

    private fun coverage(
        date: LocalDate,
        generation: Long = 1L,
        failure: String? = null,
    ): TimelineCoverageEntity = TimelineCoverageEntity(
        accountId = "account-a",
        scopeKey = "scope-a",
        zoneId = zone.id,
        localDate = date.toString(),
        fetchedAtEpochMs = 1_000L,
        contractVersion = 1,
        lastFailureKind = failure,
        lastAccessedAtEpochMs = 1_000L,
        refreshGeneration = generation,
    )

    private class RecordingTimelineCacheDao(
        coverage: List<TimelineCoverageEntity> = emptyList(),
        items: List<TimelineItemEntity> = emptyList(),
        memberships: List<TimelineDayMembershipEntity> = emptyList(),
    ) : TimelineCacheDao() {
        val coverageFlow = MutableStateFlow(coverage)
        var items: List<TimelineItemEntity> = items
        var memberships: List<TimelineDayMembershipEntity> = memberships
        val replaceCalls = mutableListOf<ReplaceCall>()
        val failureCalls = mutableListOf<List<TimelineCoverageEntity>>()

        override fun observeRange(
            accountId: String,
            scopeKey: String,
            zoneId: String,
            localDates: List<String>,
            rangeStartEpochMs: Long,
            rangeEndEpochMs: Long,
        ): Flow<List<TimelineItemEntity>> = flowOf(emptyList())

        override fun observeRange(
            accountId: String,
            scopeKey: String,
            rangeStartEpochMs: Long,
            rangeEndEpochMs: Long,
        ): Flow<List<TimelineItemEntity>> = flowOf(emptyList())

        override fun observeCoverageForDates(
            accountId: String,
            scopeKey: String,
            zoneId: String,
            localDates: List<String>,
        ): Flow<List<TimelineCoverageEntity>> = coverageFlow.map { rows ->
            rows.filter {
                it.accountId == accountId &&
                    it.scopeKey == scopeKey &&
                    it.zoneId == zoneId &&
                    it.localDate in localDates
            }
        }

        override suspend fun upsertItems(items: List<TimelineItemEntity>) {
            this@RecordingTimelineCacheDao.items = this@RecordingTimelineCacheDao.items
                .filterNot { old -> items.any { it.itemId == old.itemId } } + items
        }

        override suspend fun upsertMemberships(memberships: List<TimelineDayMembershipEntity>) {
            this@RecordingTimelineCacheDao.memberships = this@RecordingTimelineCacheDao.memberships
                .filterNot { old -> memberships.any { it == old } } + memberships
        }

        override suspend fun upsertCoverage(coverage: List<TimelineCoverageEntity>) {
            coverageFlow.value = coverageFlow.value
                .filterNot { old -> coverage.any { it.accountId == old.accountId && it.scopeKey == old.scopeKey && it.zoneId == old.zoneId && it.localDate == old.localDate } } + coverage
        }

        override suspend fun deleteMembershipsForDates(
            accountId: String,
            scopeKey: String,
            zoneId: String,
            localDates: List<String>,
        ) {
            this@RecordingTimelineCacheDao.memberships = this@RecordingTimelineCacheDao.memberships
                .filterNot {
                    it.accountId == accountId && it.scopeKey == scopeKey && it.zoneId == zoneId && it.localDate in localDates
                }
        }

        override suspend fun deleteItemsForAccount(accountId: String) = Unit

        override suspend fun deleteMembershipsForAccount(accountId: String) = Unit

        override suspend fun deleteCoverageForAccount(accountId: String) = Unit

        override suspend fun replaceDays(
            items: List<TimelineItemEntity>,
            memberships: List<TimelineDayMembershipEntity>,
            coverage: List<TimelineCoverageEntity>,
        ) {
            val currentGeneration = coverageFlow.value
                .filter { existing -> coverage.any { it.localDate == existing.localDate } }
                .maxOfOrNull(TimelineCoverageEntity::refreshGeneration)
            if (currentGeneration != null && coverage.any { it.refreshGeneration < currentGeneration }) {
                return
            }
            replaceCalls += ReplaceCall(items, memberships, coverage)
            this.items = items
            this.memberships = memberships
            coverageFlow.value = coverage
        }

        override suspend fun recordFailures(failures: List<TimelineCoverageEntity>) {
            failureCalls += failures
            val failureByKey = failures.associateBy { it.localDate }
            coverageFlow.value = coverageFlow.value.map { existing ->
                failureByKey[existing.localDate]?.let { failure ->
                    existing.copy(
                        lastFailureKind = failure.lastFailureKind,
                        refreshGeneration = failure.refreshGeneration,
                    )
                } ?: existing
            }
        }

        data class ReplaceCall(
            val items: List<TimelineItemEntity>,
            val memberships: List<TimelineDayMembershipEntity>,
            val coverage: List<TimelineCoverageEntity>,
        )
    }
}
