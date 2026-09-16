package app.tastile.android.ui.timeline

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.timeline.TimelinePageKey
import app.tastile.android.data.timeline.TimelinePageRepository
import app.tastile.android.data.timeline.TimelinePageSnapshot
import app.tastile.android.data.timeline.TimelineRefreshDirection
import app.tastile.android.ui.dashboard.TimelineScale
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
        val expected = listOf(
            currentKey.copy(anchor = LocalDate.of(2026, 9, 15)),
            currentKey,
            currentKey.copy(anchor = LocalDate.of(2026, 9, 17)),
        )

        assertEquals(expected, viewModel.visiblePageKeys)
        assertEquals(3, repository.observedKeys.size)
        assertEquals(expected, repository.observedKeys)
        expected.forEach { key ->
            assertTrue(viewModel.uiState.value.pages.containsKey(key))
        }
    }

    @Test
    fun swipeDirection_requestsDirectionalPrefetch() = runTest {
        val viewModel = viewModel()
        refreshes.clear()
        val expected = listOf(
            currentKey,
            currentKey.copy(anchor = LocalDate.of(2026, 9, 15)),
            currentKey.copy(anchor = LocalDate.of(2026, 9, 17)),
        )

        viewModel.onSwipe(TimelineRefreshDirection.Next)

        assertEquals(TimelineRefreshDirection.Next, refreshes.lastDirection)
        assertEquals(expected, refreshes.lastKeys)
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

    @Test
    fun cancelledOldFlowAfterAnchorAndScaleChange_doesNotUpdateState() = runTest {
        val viewModel = viewModel()
        val oldDayObservation = repository.observationFor(currentKey)

        val movedDay = LocalDate.of(2026, 9, 20)
        viewModel.setAnchor(movedDay)
        val movedDayKey = currentKey.copy(anchor = movedDay)
        oldDayObservation.emit(snapshot(currentKey, "stale-anchor"))

        assertEquals(movedDayKey, viewModel.currentPageKey)
        assertEquals(3, viewModel.uiState.value.pages.size)
        assertFalse(viewModel.uiState.value.pages.values.any { page ->
            page.items.any { item -> item.title == "stale-anchor" }
        })

        viewModel.setScale(TimelineScale.Week)
        val oldWeekKey = viewModel.currentPageKey
            ?: error("Week page key must be configured")
        val oldWeekObservation = repository.observationFor(oldWeekKey)
        val movedWeek = LocalDate.of(2026, 9, 28)
        viewModel.setAnchor(movedWeek)
        val movedWeekKey = oldWeekKey.copy(anchor = movedWeek).normalized()
        oldWeekObservation.emit(snapshot(oldWeekKey, "stale-scale"))

        assertEquals(movedWeekKey, viewModel.currentPageKey)
        assertEquals(3, viewModel.uiState.value.pages.size)
        assertFalse(viewModel.uiState.value.pages.values.any { page ->
            page.items.any { item -> item.title == "stale-scale" }
        })
    }

    @Test
    fun changingAnchorAndScale_cancelsPreviousPageCollectors() = runTest {
        val viewModel = viewModel()
        val initialObservations = repository.observationsSnapshot()
        assertEquals(3, initialObservations.size)

        viewModel.setAnchor(LocalDate.of(2026, 9, 20))
        awaitCancellation(initialObservations)

        val anchorObservations = repository.observationsSnapshot().drop(initialObservations.size)
        assertEquals(3, anchorObservations.size)

        viewModel.setScale(TimelineScale.Week)
        awaitCancellation(anchorObservations)
    }

    @Test
    fun nonCooperativeStaleUpstream_doesNotMutateStateAfterAnchorChange() = runTest {
        repository.nonCooperativeUpstream = true
        val viewModel = viewModel()
        val oldObservations = repository.observationsSnapshot()
        assertEquals(3, oldObservations.size)
        awaitStaleReady(oldObservations)
        repository.nonCooperativeUpstream = false

        viewModel.setAnchor(LocalDate.of(2026, 9, 20))
        val expectedState = viewModel.uiState.value

        oldObservations.forEach { observation ->
            observation.emitStale(snapshot(observation.key, "stale-upstream"))
        }
        awaitStaleDelivery(oldObservations)

        assertSame(expectedState, viewModel.uiState.value)
        assertEquals(LocalDate.of(2026, 9, 20), viewModel.currentPageKey?.anchor)
    }

    private fun viewModel(): TimelinePageViewModel = TimelinePageViewModel(
        pageRepository = repository,
        refreshRequester = refreshes,
        initialKey = currentKey,
    )

    private suspend fun awaitCancellation(
        observations: List<FakeTimelinePageRepository.Observation>,
    ) {
        withTimeout(1_000) {
            observations.forEach { observation -> observation.cancelled.await() }
        }
        assertTrue(observations.all { observation -> observation.cancelled.isCompleted })
    }

    private suspend fun awaitStaleReady(
        observations: List<FakeTimelinePageRepository.Observation>,
    ) {
        withTimeout(1_000) {
            observations.forEach { observation -> observation.staleReady.await() }
        }
    }

    private suspend fun awaitStaleDelivery(
        observations: List<FakeTimelinePageRepository.Observation>,
    ) {
        withTimeout(1_000) {
            observations.forEach { observation -> observation.staleDelivered.await() }
        }
    }

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
        private val observations = mutableListOf<Observation>()
        var nonCooperativeUpstream = false

        override fun observePage(key: TimelinePageKey): Flow<TimelinePageSnapshot> {
            val normalizedKey = key.normalized()
            val observation = Observation(normalizedKey)
            val useNonCooperativeUpstream = nonCooperativeUpstream
            observedKeys += normalizedKey
            observations += observation
            return if (useNonCooperativeUpstream) {
                observation.nonCooperativeFlow()
            } else {
                flow {
                    emit(TimelinePageSnapshot(key = normalizedKey))
                    try {
                        emitAll(kotlinx.coroutines.flow.merge(emissions, observation.emissions))
                    } finally {
                        observation.cancelled.complete(Unit)
                    }
                }
            }
        }

        override suspend fun purgeAccount(accountId: String) = Unit

        fun emit(snapshot: TimelinePageSnapshot) {
            emissions.tryEmit(snapshot)
        }

        fun observationFor(key: TimelinePageKey): Observation =
            observations.first { observation -> observation.key == key.normalized() }

        fun observationsSnapshot(): List<Observation> = observations.toList()

        class Observation(val key: TimelinePageKey) {
            val emissions = MutableSharedFlow<TimelinePageSnapshot>(extraBufferCapacity = 2)
            val cancelled = CompletableDeferred<Unit>()
            val staleReady = CompletableDeferred<Unit>()
            val staleDelivered = CompletableDeferred<Unit>()
            private var staleContinuation: Continuation<TimelinePageSnapshot>? = null

            fun emit(snapshot: TimelinePageSnapshot) {
                emissions.tryEmit(snapshot)
            }

            fun nonCooperativeFlow(): Flow<TimelinePageSnapshot> = object : Flow<TimelinePageSnapshot> {
                override suspend fun collect(collector: FlowCollector<TimelinePageSnapshot>) {
                    collector.emit(TimelinePageSnapshot(key = key))
                    collector.emit(awaitStale())
                    staleDelivered.complete(Unit)
                }
            }

            suspend fun awaitStale(): TimelinePageSnapshot =
                suspendCoroutine { continuation ->
                    staleContinuation = continuation
                    staleReady.complete(Unit)
                }

            fun emitStale(snapshot: TimelinePageSnapshot) {
                val continuation = checkNotNull(staleContinuation) {
                    "Non-cooperative upstream is not awaiting stale emission"
                }
                staleContinuation = null
                continuation.resume(snapshot)
            }
        }
    }
}
