package app.tastile.android.ui.timeline

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.timeline.TimelinePageKey
import app.tastile.android.data.timeline.TimelinePageRepository
import app.tastile.android.data.timeline.TimelinePageSnapshot
import app.tastile.android.data.timeline.TimelineRefreshDirection
import app.tastile.android.ui.dashboard.TimelineScale
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimelinePageViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeTimelinePageRepository()
    private val refreshes = RecordingRefreshRequester()
    private val currentKey = TimelinePageKey(
        accountId = "account-a",
        scopeKey = "scope-a",
        zoneId = ZoneId.of("America/New_York"),
        scale = TimelineScale.Day,
        anchor = LocalDate.of(2026, 9, 16),
    ).normalized()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun visiblePages_observePreviousCurrentAndNextKeys() = runTest {
        val viewModel = viewModel()

        assertEquals(
            setOf(
                currentKey.copy(anchor = LocalDate.of(2026, 9, 15)),
                currentKey,
                currentKey.copy(anchor = LocalDate.of(2026, 9, 17)),
            ),
            viewModel.uiState.value.pages.keys,
        )
        assertEquals(3, repository.observedKeys.size)
    }

    @Test
    fun swipeDirection_requestsDirectionalPrefetch() = runTest {
        val viewModel = viewModel()
        refreshes.clear()

        viewModel.onSwipe(TimelineRefreshDirection.Next)

        assertEquals(TimelineRefreshDirection.Next, refreshes.lastDirection)
        assertEquals(
            setOf(
                currentKey.copy(anchor = LocalDate.of(2026, 9, 15)),
                currentKey,
                currentKey.copy(anchor = LocalDate.of(2026, 9, 17)),
            ),
            refreshes.lastKeys.toSet(),
        )
    }

    @Test
    fun switchingScale_preservesIndependentAnchorAndZoom() = runTest {
        val viewModel = viewModel()
        val day = LocalDate.of(2026, 9, 16)
        val week = LocalDate.of(2026, 9, 14)

        viewModel.setAnchor(day)
        viewModel.setZoom(2.25f)
        viewModel.setScale(TimelineScale.Week)
        viewModel.setAnchor(week)
        viewModel.setZoom(3.5f)

        viewModel.setScale(TimelineScale.Day)

        assertEquals(day, viewModel.uiState.value.anchors[TimelineScale.Day])
        assertEquals(2.25f, viewModel.uiState.value.zoomFor(TimelineScale.Day))
        assertEquals(week, viewModel.uiState.value.anchors[TimelineScale.Week])
        assertEquals(3.5f, viewModel.uiState.value.zoomFor(TimelineScale.Week))
    }

    @Test
    fun unrelatedPageEmission_doesNotReplaceCurrentSnapshot() = runTest {
        val viewModel = viewModel()
        val currentSnapshot = snapshot(currentKey, "current")
        repository.emit(currentSnapshot)
        val stateSnapshot = viewModel.uiState.value.pages.getValue(currentKey)

        repository.emit(snapshot(currentKey.copy(anchor = LocalDate.of(2026, 9, 18)), "unrelated"))

        assertSame(stateSnapshot, viewModel.uiState.value.pages.getValue(currentKey))
        assertEquals("current", viewModel.uiState.value.pages.getValue(currentKey)?.items?.single()?.title)
    }

    private fun viewModel(): TimelinePageViewModel = TimelinePageViewModel(
        pageRepository = repository,
        refreshRequester = refreshes,
        initialKey = currentKey,
    )

    private fun snapshot(key: TimelinePageKey, title: String): TimelinePageSnapshot =
        TimelinePageSnapshot(
            key = key,
            items = kotlinx.collections.immutable.persistentListOf(
                CoreTimelineItem(
                    id = "item-$title",
                    title = title,
                    type = "placement",
                    status = "open",
                    startAt = "2026-09-16T09:00:00Z",
                    endAt = "2026-09-16T10:00:00Z",
                ),
            ),
        )

    private class RecordingRefreshRequester : TimelinePageRefreshRequester {
        var lastDirection: TimelineRefreshDirection? = null
            private set
        var lastKeys: List<TimelinePageKey> = emptyList()
            private set

        override fun requestRefresh(
            keys: Collection<TimelinePageKey>,
            direction: TimelineRefreshDirection,
        ) {
            lastKeys = keys.toList()
            lastDirection = direction
        }

        fun clear() {
            lastKeys = emptyList()
            lastDirection = null
        }
    }

    private class FakeTimelinePageRepository : TimelinePageRepository {
        private val emissions = MutableSharedFlow<TimelinePageSnapshot>(extraBufferCapacity = 16)
        val observedKeys = mutableListOf<TimelinePageKey>()

        override fun observePage(key: TimelinePageKey): Flow<TimelinePageSnapshot> {
            val normalizedKey = key.normalized()
            observedKeys += normalizedKey
            return flow {
                emit(TimelinePageSnapshot(key = normalizedKey))
                emitAll(emissions)
            }
        }

        override suspend fun purgeAccount(accountId: String) = Unit

        fun emit(snapshot: TimelinePageSnapshot) {
            emissions.tryEmit(snapshot)
        }
    }
}
