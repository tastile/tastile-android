package app.tastile.android.ui.dashboard

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.IntegrationRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.UserSettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelSelectedTileTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(): DashboardViewModel {
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val tileRepository = mockk<TileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val integrationRepository = mockk<IntegrationRepository>(relaxed = true)
        every { authRepository.currentSession } returns null
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.DARK
        every { userSettingsRepository.getLocale() } returns AppLocale.JA
        coEvery { tileRepository.getTiles("user-1") } returns emptyList()
        coEvery { tileRepository.getTimeline() } returns emptyList()
        coEvery { integrationRepository.getSettings() } returns mockk(relaxed = true)
        return DashboardViewModel(
            authRepository,
            profileRepository,
            tileRepository,
            userSettingsRepository,
            integrationRepository
        )
    }

    private fun tile(id: String, title: String) = Tile(
        id = id,
        title = title,
        lifecycle = TileLifecycle.READY.value,
    )

    @Test
    fun `selectedTile is null before selectTile is called`() = runTest {
        val vm = newViewModel()
        assertNull(vm.selectedTile.first())
    }

    @Test
    fun `selectTile finds the tile and clearSelectedTile resets`() = runTest {
        val vm = newViewModel()
        vm.replaceTilesForTest(listOf(tile("a", "Alpha"), tile("b", "Bravo")))
        vm.selectTile("b")
        assertEquals("Bravo", vm.selectedTile.first()?.title)

        vm.clearSelectedTile()
        assertNull(vm.selectedTile.first())
    }
}