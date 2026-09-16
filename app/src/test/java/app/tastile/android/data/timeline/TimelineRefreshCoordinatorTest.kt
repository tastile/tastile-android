package app.tastile.android.data.timeline

import app.tastile.android.data.timeline.local.TimelineCacheDao
import app.tastile.android.data.timeline.local.TimelineCoverageEntity
import app.tastile.android.data.timeline.local.TimelineDayMembershipEntity
import app.tastile.android.data.timeline.local.TimelineItemEntity
import app.tastile.android.ui.dashboard.TimelineScale
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineRefreshCoordinatorTest {
    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-09-16T12:00:00Z")
    private val currentDate = LocalDate.of(2026, 9, 16)

    @Test
    fun coordinator_prioritizesCurrentMissingThenSwipeDirectionThenOppositeThenStale() = runTest {
        val current = key(currentDate)
        val previous = key(currentDate.minusDays(1))
        val next = key(currentDate.plusDays(1))
        val dao = CoordinatorDao(
            coverage = listOf(
                coverage(previous, fetchedAt = now.minusSeconds(60), failure = "network"),
                coverage(next, fetchedAt = now.minusSeconds(60), failure = "network"),
            ),
        )
        val sync = RecordingSyncRepository()
        val coordinator = coordinator(sync, dao, StandardTestDispatcher(testScheduler))

        coordinator.requestRefresh(
            keys = listOf(current, previous, next),
            direction = TimelineRefreshDirection.Next,
        )
        advanceUntilIdle()

        assertEquals(
            listOf(current, next, previous),
            sync.requests.map { it.key },
        )
    }

    @Test
    fun coordinator_doesNotRefreshFreshCoverage() = runTest {
        val current = key(currentDate)
        val dao = CoordinatorDao(
            coverage = listOf(
                coverage(current, fetchedAt = now.minusSeconds(60), failure = null),
            ),
        )
        val sync = RecordingSyncRepository()
        val coordinator = coordinator(sync, dao, StandardTestDispatcher(testScheduler))

        coordinator.requestRefresh(listOf(current), TimelineRefreshDirection.None)
        advanceUntilIdle()

        assertTrue(sync.requests.isEmpty())
    }

    @Test
    fun coordinator_refreshesStaleCoverageWithoutBlockingSnapshots() = runTest {
        val current = key(currentDate)
        val dao = CoordinatorDao(
            coverage = listOf(
                coverage(current, fetchedAt = now.minusSeconds(60), failure = "network"),
            ),
        )
        val sync = RecordingSyncRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        val coordinator = TimelineRefreshCoordinator(
            syncRepository = sync,
            dao = dao,
            applicationScope = scope,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        coordinator.requestRefresh(listOf(current), TimelineRefreshDirection.None)
        // requestRefresh is an intent: the caller does not await the network
        // work. It is queued on the injected application scope instead.
        assertTrue(sync.requests.isEmpty())
        advanceUntilIdle()

        assertEquals(listOf(current), sync.requests.map { it.key })
    }

    private fun coordinator(
        sync: RecordingSyncRepository,
        dao: CoordinatorDao,
        dispatcher: CoroutineDispatcher,
    ): TimelineRefreshCoordinator = TimelineRefreshCoordinator(
        syncRepository = sync,
        dao = dao,
        applicationScope = CoroutineScope(dispatcher),
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    private fun key(anchor: LocalDate): TimelinePageKey = TimelinePageKey(
        accountId = "account-a",
        scopeKey = "scope-a",
        zoneId = zone,
        scale = TimelineScale.Day,
        anchor = anchor,
    )

    private fun coverage(
        key: TimelinePageKey,
        fetchedAt: Instant,
        failure: String?,
    ): TimelineCoverageEntity = TimelineCoverageEntity(
        accountId = key.accountId,
        scopeKey = key.scopeKey,
        zoneId = key.zoneId.id,
        localDate = key.anchor.toString(),
        fetchedAtEpochMs = fetchedAt.toEpochMilli(),
        contractVersion = 1,
        lastFailureKind = failure,
        lastAccessedAtEpochMs = fetchedAt.toEpochMilli(),
        refreshGeneration = 1,
    )

    private class RecordingSyncRepository : TimelineSyncRepository {
        val requests = CopyOnWriteArrayList<TimelineRefreshRequest>()

        override suspend fun refresh(request: TimelineRefreshRequest): TimelineRefreshResult {
            requests += request
            return TimelineRefreshResult(
                status = TimelineRefreshStatus.Refreshed,
                refreshedDates = request.key.visibleDates,
            )
        }
    }

    private class CoordinatorDao(
        coverage: List<TimelineCoverageEntity>,
    ) : TimelineCacheDao() {
        private val coverageFlow = MutableStateFlow(coverage)

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
    }
}
