package app.tastile.android.ui.dashboard

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.model.Profile
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.ReferenceOverlayStore
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.TilesResponse
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.data.command.ExecutionStateLookup
import app.tastile.android.ui.dashboard.ListGroupingMode
import androidx.lifecycle.viewModelScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val viewModels = mutableListOf<DashboardViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
        Dispatchers.resetMain()
    }

    private fun newViewModel(): DashboardViewModel {
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val tileRepository = mockk<TileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val referenceOverlayStore = mockk<ReferenceOverlayStore>(relaxed = true)
        every { authRepository.currentSession } returns null
        every { authRepository.getAuthStateStream } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.DARK
        every { userSettingsRepository.getLocale() } returns AppLocale.JA
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        coEvery { tileRepository.getTimeline(any(), any()) } returns emptyList()
        coEvery { profileRepository.getProfile("user-1") } returns Profile(id = "user-1")
        return DashboardViewModel(
            authRepository,
            profileRepository,
            tileRepository,
            userSettingsRepository,
            referenceOverlayStore,
        ).also { viewModels.add(it) }
    }

    @Test
    fun handleCardAction_routesToRequestPromptCommand() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()
        coEvery { profileRepository.getProfile("user-1") } returns Profile(id = "user-1")
        coEvery { tileRepository.requestPrompt(any()) } returns true

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)

        viewModel.handleCardAction(CardAction.TriggerPrompt("tile-1"))

        assertEquals("tile-1", viewModel.requestPromptTileId.value)
        coVerify(exactly = 0) { tileRepository.requestPrompt(any()) }
    }

    @Test
    fun rescheduleTimelineItem_routesToCoreRescheduleCommand() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()
        coEvery { profileRepository.getProfile("user-1") } returns Profile(id = "user-1")
        coEvery { tileRepository.rescheduleTile(any(), any(), any()) } returns Unit

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)
        val item = CoreTimelineItem(
            id = "tl-1",
            tileId = "tile-1",
            title = "Focus",
            type = "work",
            status = "scheduled",
            startAt = "2026-03-29T10:00:00Z",
            endAt = "2026-03-29T10:30:00Z"
        )

        viewModel.rescheduleTimelineItem(item, minuteOffset = 37, zoomScale = 1.6f)

        coVerify(atLeast = 1) { tileRepository.rescheduleTile(eq("tile-1"), any(), any()) }
    }

    @Test
    fun refreshAll_setsDiagnosticsWhenUnauthenticated() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)
        viewModel.refreshAll()

        assertEquals("source=none reason=unauthenticated", viewModel.statsDiagnostics.value)
    }

    @Test
    fun groupedTiles_forStateMode_partitionsByLifecycle() = runTest {
        val viewModel = newViewModel()
        val ready = Tile(id = "r1", title = "R", lifecycle = "Ready")
        val started = Tile(id = "s1", title = "S", lifecycle = "Started")
        val done = Tile(id = "d1", title = "D", lifecycle = "Done")
        viewModel.replaceTilesForTest(listOf(ready, started, done))

        val sections = viewModel.groupedTiles.first { it.size >= 3 }
        val labels = sections.map { it.labelKey }
        assertEquals(setOf("ready", "started", "done"), labels.toSet())
        assertEquals(1, sections.first { it.labelKey == "ready" }.tiles.size)
        assertEquals(1, sections.first { it.labelKey == "started" }.tiles.size)
        assertEquals(1, sections.first { it.labelKey == "done" }.tiles.size)
    }

    @Test
    fun groupedTiles_forProjectMode_usesProjectLabel() = runTest {
        val viewModel = newViewModel()
        viewModel.setListGroupingMode(ListGroupingMode.PROJECT)
        val t1 = Tile(id = "t1", title = "A", lifecycle = "Ready", labels = listOf("project:alpha"))
        val t2 = Tile(id = "t2", title = "B", lifecycle = "Ready", labels = listOf("project:beta"))
        val t3 = Tile(id = "t3", title = "C", lifecycle = "Ready", labels = emptyList())
        viewModel.replaceTilesForTest(listOf(t1, t2, t3))

        val sections = viewModel.groupedTiles.first { it.size >= 3 }
        val projectLabels = sections.map { it.labelKey }.toSet()
        assertTrue("sections must include the two projects", projectLabels.contains("alpha"))
        assertTrue("sections must include the unassigned bucket", projectLabels.contains("unassigned"))
    }

    @Test
    fun groupedTiles_forTagMode_splitsByNonProjectLabels() = runTest {
        val viewModel = newViewModel()
        viewModel.setListGroupingMode(ListGroupingMode.TAG)
        val t1 = Tile(id = "t1", title = "A", lifecycle = "Ready", labels = listOf("project:x", "urgent"))
        val t2 = Tile(id = "t2", title = "B", lifecycle = "Ready", labels = listOf("urgent"))
        val t3 = Tile(id = "t3", title = "C", lifecycle = "Ready", labels = emptyList())
        viewModel.replaceTilesForTest(listOf(t1, t2, t3))

        val sections = viewModel.groupedTiles.first { it.size >= 2 }
        val tagLabels = sections.map { it.labelKey }.toSet()
        assertTrue(tagLabels.contains("urgent"))
        assertTrue(tagLabels.contains("untagged"))
    }

    @Test
    fun toggleSectionExpanded_flipsMembershipInSet() = runTest {
        val viewModel = newViewModel()
        assertFalse(viewModel.expandedSections.value.contains("alpha"))
        viewModel.toggleSectionExpanded("alpha")
        assertTrue(viewModel.expandedSections.value.contains("alpha"))
        viewModel.toggleSectionExpanded("alpha")
        assertFalse(viewModel.expandedSections.value.contains("alpha"))
    }

    @Test
    fun confirmDeleteTile_clearsCandidateAndCallsRepository() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()
        coEvery { tileRepository.deleteTile(any()) } returns Unit
        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)

        viewModel.setDeleteTileCandidate("kill-me")
        assertEquals("kill-me", viewModel.requestDeleteTileId.value)
        viewModel.confirmDeleteTile()

        coVerify(atLeast = 1) { tileRepository.deleteTile("kill-me") }
        assertEquals(null, viewModel.requestDeleteTileId.value)
    }

    @Test
    fun startTile_reloadsTheStartedTileAndExposesPauseControl() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()
        var response = TilesResponse(
            listOf(Tile(id = "tile-1", title = "Focus", lifecycle = "Ready")),
            null,
            null,
        )
        coEvery { tileRepository.getTiles(any()) } answers { response }
        coEvery { tileRepository.startTile("tile-1") } answers {
            response = TilesResponse(
                listOf(Tile(id = "tile-1", title = "Focus", lifecycle = "Started")),
                null,
                null,
            )
            Tile(id = "tile-1", title = "Focus", lifecycle = "Started")
        }
        coEvery { tileRepository.executionStateLookupForTile("tile-1") } returns ExecutionStateLookup.Found(0)

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)

        viewModel.startTile("tile-1")

        assertEquals("Started", viewModel.tiles.value.single().lifecycle)
        assertEquals(ExecutionControlState.Active, viewModel.executionControlStates.value["tile-1"])
        coVerify(exactly = 1) { tileRepository.startTile("tile-1") }
        coVerify(atLeast = 1) { tileRepository.getTiles(any()) }
    }

    @Test
    fun authoritativeExecutionRefresh_clearsUnvalidatedPausedState() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()
        val authState = MutableStateFlow<TastileAuthState>(TastileAuthState.Authenticated("user-1", "user@example.test", "id-token", "access-token", null))
        every { authRepository.getAuthStateStream } returns authState
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(
            listOf(Tile(id = "tile-1", title = "Focus", lifecycle = "Started")),
            null,
            null,
        )
        coEvery { tileRepository.executionStateLookupForTile("tile-1") } returns ExecutionStateLookup.NoActiveExecution

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)
        viewModel.replaceExecutionControlStatesForTest(mapOf("tile-1" to ExecutionControlState.Paused))

        viewModel.setTileFilter(app.tastile.android.data.repository.TileFilter(limit = 21))

        assertEquals(null, viewModel.executionControlStates.value["tile-1"])
    }

    @Test
    fun pauseThenResume_transitionsControlStateForTheSameExecution() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()
        coEvery { tileRepository.pauseTile("tile-1") } returns Unit
        coEvery { tileRepository.continueTile("tile-1") } returns Unit
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(
            listOf(Tile(id = "tile-1", title = "Focus", lifecycle = "Started")),
            null,
            null,
        )
        coEvery { tileRepository.executionStateLookupForTile("tile-1") } returnsMany listOf(
            ExecutionStateLookup.Found(1),
            ExecutionStateLookup.Found(0),
        )
        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)
        viewModel.replaceExecutionControlStatesForTest(mapOf("tile-1" to ExecutionControlState.Active))

        viewModel.pauseTile("tile-1")
        coVerify(exactly = 1) { tileRepository.pauseTile("tile-1") }
        assertEquals(ExecutionControlState.Paused, viewModel.executionControlStates.value["tile-1"])

        viewModel.resumeTile("tile-1")
        coVerify(exactly = 1) { tileRepository.continueTile("tile-1") }
        assertEquals(ExecutionControlState.Active, viewModel.executionControlStates.value["tile-1"])
    }

    @Test
    fun pausedGrace_expiresAfterFiveSecondsWithoutAnotherRefresh() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()
        coEvery { tileRepository.pauseTile("tile-1") } returns Unit
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(
            listOf(Tile(id = "tile-1", title = "Focus", lifecycle = "Started")), null, null,
        )
        coEvery { tileRepository.executionStateLookupForTile("tile-1") } returns ExecutionStateLookup.NoActiveExecution
        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)
        viewModel.replaceExecutionControlStatesForTest(mapOf("tile-1" to ExecutionControlState.Active))

        viewModel.pauseTile("tile-1")
        assertEquals(ExecutionControlState.Paused, viewModel.executionControlStates.value["tile-1"])

        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(ExecutionControlState.Paused, viewModel.executionControlStates.value["tile-1"])
        advanceTimeBy(1)
        runCurrent()

        assertEquals(null, viewModel.executionControlStates.value["tile-1"])
        verify(exactly = 1) { tileRepository.clearExecutionCacheForTile("tile-1") }
    }

    @Test
    fun invalidExecutionLookup_neverKeepsResumeDuringPauseGrace() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()
        coEvery { tileRepository.pauseTile("tile-1") } returns Unit
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(
            listOf(Tile(id = "tile-1", title = "Focus", lifecycle = "Started")), null, null,
        )
        coEvery { tileRepository.executionStateLookupForTile("tile-1") } returns ExecutionStateLookup.InvalidExecution
        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)
        viewModel.replaceExecutionControlStatesForTest(mapOf("tile-1" to ExecutionControlState.Active))

        viewModel.pauseTile("tile-1")

        assertEquals(null, viewModel.executionControlStates.value["tile-1"])
    }

    @Test
    fun resumeFailure_clearsStaleResumeControl() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()
        coEvery { tileRepository.continueTile("tile-1") } throws IllegalStateException("execution is terminal")
        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)
        viewModel.replaceExecutionControlStatesForTest(mapOf("tile-1" to ExecutionControlState.Paused))

        viewModel.resumeTile("tile-1")

        assertEquals(null, viewModel.executionControlStates.value["tile-1"])
        assertTrue(viewModel.executionControlInFlightTileIds.value.isEmpty())
    }

    @Test
    fun pauseTile_ignoresRepeatedCallWhilePauseIsInFlight() = runTest {
        val (authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore) = mocks()
        val pauseGate = CompletableDeferred<Unit>()
        coEvery { tileRepository.pauseTile("tile-1") } coAnswers { pauseGate.await() }
        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
        viewModels.add(viewModel)

        viewModel.pauseTile("tile-1")
        viewModel.pauseTile("tile-1")

        assertTrue("tile-1" in viewModel.executionControlInFlightTileIds.value)
        coVerify(exactly = 1) { tileRepository.pauseTile("tile-1") }
        pauseGate.complete(Unit)
        runCurrent()
        assertTrue(viewModel.executionControlInFlightTileIds.value.isEmpty())
    }

    @Test
    fun setSearchTerm_rebuildsTileFilter() = runTest {
        val viewModel = newViewModel()
        val seed = app.tastile.android.data.repository.TileFilter(limit = 50)
        viewModel.setTileFilter(seed)
        viewModel.setSearchTerm("alpha")
        assertEquals("alpha", viewModel.tileFilter.value.search)
        assertEquals(50, viewModel.tileFilter.value.limit)
    }

    @Test
    fun setOwnerFilter_updatesTileFilterAndClearsBackToAllProjects() = runTest {
        val viewModel = newViewModel()

        viewModel.setOwnerFilter("project-1")
        assertEquals(listOf("project-1"), viewModel.tileFilter.value.ownerIds)

        viewModel.setOwnerFilter(null)
        assertTrue(viewModel.tileFilter.value.ownerIds.isEmpty())
    }

    @Test
    fun bumpSectionLimit_doubles8_16_32_60_thenResets() = runTest {
        val viewModel = newViewModel()
        val total = 200
        viewModel.bumpSectionLimit("sec", total)
        assertEquals(16, viewModel.sectionLimits.value["sec"])
        viewModel.bumpSectionLimit("sec", total)
        assertEquals(32, viewModel.sectionLimits.value["sec"])
        viewModel.bumpSectionLimit("sec", total)
        assertEquals(60, viewModel.sectionLimits.value["sec"])
        viewModel.bumpSectionLimit("sec", total)
        assertEquals(8, viewModel.sectionLimits.value["sec"])
    }

    private data class Mocks(
        val authRepository: AuthRepository,
        val profileRepository: ProfileRepository,
        val tileRepository: TileRepository,
        val userSettingsRepository: UserSettingsRepository,
        val referenceOverlayStore: ReferenceOverlayStore,
    )

    private fun mocks(): Mocks {
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val tileRepository = mockk<TileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val referenceOverlayStore = mockk<ReferenceOverlayStore>(relaxed = true)
        every { authRepository.currentSession } returns null
        every { authRepository.getAuthStateStream } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.DARK
        every { userSettingsRepository.getLocale() } returns AppLocale.JA
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        coEvery { tileRepository.getTimeline(any(), any()) } returns emptyList()
        return Mocks(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
    }
}
