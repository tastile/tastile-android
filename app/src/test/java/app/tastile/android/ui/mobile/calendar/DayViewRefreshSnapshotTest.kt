package app.tastile.android.ui.mobile.calendar

import androidx.lifecycle.viewModelScope
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.ReferenceOverlayStore
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.TilesResponse
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.ui.dashboard.DashboardViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Pure-JVM regression test for the v37 `refreshTimeline()` keep-prior-snapshot
 * invariant (design §2). Sibling of [DashboardViewModelTest] /
 * [DashboardViewModelSelectedTileTest] — uses the same mockk +
 * UnconfinedTestDispatcher construction pattern, no Robolectric.
 *
 * Seeds the timeline via [DashboardViewModel.replaceTimelineForTest],
 * mocks [TileRepository.getTimeline] to throw, then triggers a refresh
 * via the same path the UI uses ([DashboardViewModel.setOwnerFilter])
 * and asserts:
 *   1. The pre-call list survives (old behavior, no flicker).
 *   2. `error` StateFlow captures the thrown message.
 *   3. `isLoadingTimeline` returns to `false` (the `finally` branch runs).
 *
 * The test does NOT touch the broader dashboard render path; only the
 * calendar timeline refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DayViewRefreshSnapshotTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val viewModels = mutableListOf<DashboardViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
        Dispatchers.resetMain()
    }

    private fun newViewModel(
        authRepository: AuthRepository,
        profileRepository: ProfileRepository,
        tileRepository: TileRepository,
        userSettingsRepository: UserSettingsRepository,
        referenceOverlayStore: ReferenceOverlayStore,
    ): DashboardViewModel {
        every { authRepository.currentSession } returns null
        every { authRepository.authState } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.DARK
        every { userSettingsRepository.getLocale() } returns AppLocale.JA
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        return DashboardViewModel(
            authRepository,
            profileRepository,
            tileRepository,
            userSettingsRepository,
            referenceOverlayStore,
        ).also { viewModels.add(it) }
    }

    private fun mockRepo(): TileRepository = mockk<TileRepository>(relaxed = true).also {
        coEvery { it.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        coEvery { it.getTimeline(any(), any(), any()) } returns emptyList()
    }

    private fun item(id: String, startMinuteOfDay: Int): CoreTimelineItem = CoreTimelineItem(
        id = id,
        tileId = null,
        title = "Item $id",
        type = "work",
        status = "scheduled",
        startAt = Instant.parse("2026-07-17T00:00:00Z").toString(),
        endAt = Instant.parse("2026-07-17T00:00:00Z").toString(),
    )

    @Test
    fun timeline_preservesPriorValue_onFetchError() = runTest {
        val initial = listOf(item("a", 540), item("b", 600), item("c", 660))

        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val referenceOverlayStore = mockk<ReferenceOverlayStore>(relaxed = true)
        every { authRepository.currentSession } returns null
        every { authRepository.authState } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.DARK
        every { userSettingsRepository.getLocale() } returns AppLocale.JA

        val tileRepository = mockk<TileRepository>(relaxed = true)
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        // getTimeline throws — this is the failure path we want to cover.
        coEvery { tileRepository.getTimeline(any(), any(), any()) } throws
            RuntimeException("net down")

        val viewModel = newViewModel(
            authRepository,
            profileRepository,
            tileRepository,
            userSettingsRepository,
            referenceOverlayStore,
        )
        viewModel.replaceTimelineForTest(initial)

        val before = viewModel.timeline.value
        assertEquals(initial, before)

        // Trigger the refresh path that the UI uses when the workspace
        // filter changes (the most common entry-point per design §2).
        viewModel.setOwnerFilter("project-x")
        advanceUntilIdle()

        // Snapshot must be preserved — `_timeline.value` was only ever
        // assigned in the `try` block, and the throwing `getTimeline`
        // bypasses that assignment.
        assertEquals(initial, viewModel.timeline.value)
        // The exception message must be surfaced for UX.
        assertEquals("net down", viewModel.error.value)
        // `isLoadingTimeline` must return to false (the `finally` branch).
        assertTrue(viewModel.isLoadingTimeline.value == false)
    }

    @Test
    fun timeline_overwritesPriorValue_onSuccessfulRefresh() = runTest {
        val initial = listOf(item("a", 540))
        val refreshed = listOf(item("d", 720), item("e", 780))

        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val referenceOverlayStore = mockk<ReferenceOverlayStore>(relaxed = true)
        every { authRepository.currentSession } returns null
        every { authRepository.authState } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.DARK
        every { userSettingsRepository.getLocale() } returns AppLocale.JA

        val tileRepository = mockk<TileRepository>(relaxed = true)
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        // Build a tiny state machine that returns `initial` once then
        // `refreshed` for every subsequent call. The 1-arg + 3-arg
        // overloads are both common in this codebase; match both.
        var calls = 0
        coEvery { tileRepository.getTimeline(any(), any()) } coAnswers {
            calls++
            if (calls <= 1) emptyList() else refreshed
        }
        coEvery { tileRepository.getTimeline(any(), any(), any()) } coAnswers {
            calls++
            if (calls <= 1) emptyList() else refreshed
        }

        val viewModel = newViewModel(
            authRepository,
            profileRepository,
            tileRepository,
            userSettingsRepository,
            referenceOverlayStore,
        )
        viewModel.replaceTimelineForTest(initial)

        // First owner-filter change kicks the refresh; the second one
        // observes the freshly fetched `refreshed` list.
        viewModel.setOwnerFilter("project-x")
        advanceUntilIdle()
        viewModel.setOwnerFilter("project-y")
        advanceUntilIdle()

        assertEquals(refreshed, viewModel.timeline.value)
        assertEquals(null, viewModel.error.value)
    }
}
