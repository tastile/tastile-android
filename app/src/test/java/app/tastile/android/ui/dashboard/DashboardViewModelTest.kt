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
import app.tastile.android.ui.dashboard.ListGroupingMode
import androidx.lifecycle.viewModelScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        every { authRepository.authState } returns MutableStateFlow(TastileAuthState.Unauthenticated)
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

        coVerify(atLeast = 1) { tileRepository.requestPrompt("tile-1") }
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
    fun setSearchTerm_rebuildsTileFilter() = runTest {
        val viewModel = newViewModel()
        val seed = app.tastile.android.data.repository.TileFilter(limit = 50)
        viewModel.setTileFilter(seed)
        viewModel.setSearchTerm("alpha")
        assertEquals("alpha", viewModel.tileFilter.value.search)
        assertEquals(50, viewModel.tileFilter.value.limit)
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
        every { authRepository.authState } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.DARK
        every { userSettingsRepository.getLocale() } returns AppLocale.JA
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        coEvery { tileRepository.getTimeline(any(), any()) } returns emptyList()
        return Mocks(authRepository, profileRepository, tileRepository, userSettingsRepository, referenceOverlayStore)
    }
}
