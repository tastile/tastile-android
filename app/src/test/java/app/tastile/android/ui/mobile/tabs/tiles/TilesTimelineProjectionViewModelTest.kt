package app.tastile.android.ui.mobile.tabs.tiles

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.timeline.TimelinePageKey
import app.tastile.android.data.timeline.TimelinePageRepository
import app.tastile.android.data.timeline.TimelinePageSnapshot
import app.tastile.android.data.timeline.TimelineProjectionRequest
import app.tastile.android.ui.dashboard.TimelineSubScale
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TilesTimelineProjectionViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = RecordingPageRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun requestUsesNormalizedOwnerScopeForLocalPageReads() = runTest {
        val viewModel = TilesTimelineProjectionViewModel(repository)
        val request = TimelineProjectionRequest(
            accountId = "account-a",
            ownerIds = listOf(" owner-b ", "owner-a", "owner-b"),
            zoneId = ZoneId.of("America/New_York"),
            scale = TimelineSubScale.DAY,
            anchor = LocalDate.of(2026, 9, 16),
        )
        val collector = backgroundScope.launch {
            viewModel.items.collect { }
        }

        viewModel.setRequest(request)
        testScheduler.advanceUntilIdle()

        val key = repository.observedKeys.single()
        assertEquals(listOf("owner-a", "owner-b"), key.ownerIds)
        assertEquals(request.normalized().scopeFingerprint, key.scopeFingerprint)
        assertEquals("account-a", key.accountId)
        collector.cancel()
    }

    @Test
    fun requestDoesNotObservePagesUntilPanelCollectsItems() = runTest {
        val viewModel = TilesTimelineProjectionViewModel(repository)

        viewModel.setRequest(
            TimelineProjectionRequest(
                accountId = "account-a",
                ownerIds = emptyList(),
                zoneId = ZoneId.of("America/New_York"),
                scale = TimelineSubScale.DAY,
                anchor = LocalDate.of(2026, 9, 16),
            ),
        )
        testScheduler.advanceUntilIdle()

        assertTrue(repository.observedKeys.isEmpty())
    }

    @Test
    fun monthRequestReadsOnePageSnapshotAndEmitsItsContent() = runTest {
        val item = CoreTimelineItem(
            id = "month-item",
            title = "Month-local",
            type = "placement",
            status = "open",
            startAt = "2026-09-16T09:00:00Z",
            endAt = "2026-09-16T10:00:00Z",
        )
        repository.snapshotItems = listOf(item)
        val viewModel = TilesTimelineProjectionViewModel(repository)
        val values = mutableListOf<List<CoreTimelineItem>>()
        val collector = backgroundScope.launch {
            viewModel.items.collect { values += it }
        }

        viewModel.setRequest(
            TimelineProjectionRequest(
                accountId = "account-a",
                ownerIds = emptyList(),
                zoneId = ZoneId.of("America/New_York"),
                scale = TimelineSubScale.MONTH,
                anchor = LocalDate.of(2026, 9, 16),
            ),
        )
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("month-item"), values.last().map { it.id })
        assertEquals(1, repository.observedKeys.size)
        assertTrue(repository.observedKeys.single().visibleDates.size == 42)
        collector.cancel()
    }

    private class RecordingPageRepository : TimelinePageRepository {
        val observedKeys = mutableListOf<TimelinePageKey>()
        var snapshotItems: List<CoreTimelineItem> = emptyList()

        override fun observePage(key: TimelinePageKey): Flow<TimelinePageSnapshot> {
            observedKeys += key.normalized()
            return flowOf(
                TimelinePageSnapshot(
                    key = key.normalized(),
                    items = snapshotItems.toPersistentList(),
                ),
            )
        }

        override suspend fun purgeAccount(accountId: String) = Unit
    }
}
