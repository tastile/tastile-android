package app.tastile.android.ui.dashboard

import app.tastile.android.data.model.Profile
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.IntegrationRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.UserSettingsRepository
import io.github.jan.supabase.auth.user.UserSession
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import io.github.jan.supabase.auth.status.SessionStatus
import io.mockk.coEvery
import io.mockk.coVerify
import app.tastile.android.core.CoreTimelineItem

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun handleCardAction_routesToContinueCoreCommand() {
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val tileRepository = mockk<TileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val integrationRepository = mockk<IntegrationRepository>(relaxed = true)
        val sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated(isSignOut = false))
        val session = mockk<UserSession>(relaxed = true)
        val user = mockk<io.github.jan.supabase.auth.user.UserInfo>(relaxed = true)
        every { user.id } returns "user-1"
        every { user.email } returns "u@test.dev"
        every { session.user } returns user
        every { authRepository.currentSession } returns session
        every { authRepository.sessionStatus } returns sessionStatus as StateFlow<SessionStatus>
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.DARK
        every { userSettingsRepository.getLocale() } returns AppLocale.JA
        coEvery { tileRepository.getTiles("user-1") } returns emptyList()
        coEvery { tileRepository.getTimeline() } returns emptyList()
        coEvery { profileRepository.getProfile("user-1") } returns Profile(id = "user-1")
        coEvery { tileRepository.continueTile(any()) } returns Unit
        coEvery { integrationRepository.getSettings() } returns mockk(relaxed = true)

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, integrationRepository)

        viewModel.handleCardAction(CardAction.TriggerPrompt("tile-1"))

        coVerify(atLeast = 1) { tileRepository.continueTile("tile-1") }
    }

    @Test
    fun rescheduleTimelineItem_routesToCoreRescheduleCommand() {
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val tileRepository = mockk<TileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val integrationRepository = mockk<IntegrationRepository>(relaxed = true)
        val sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated(isSignOut = false))
        val session = mockk<UserSession>(relaxed = true)
        val user = mockk<io.github.jan.supabase.auth.user.UserInfo>(relaxed = true)
        every { user.id } returns "user-1"
        every { user.email } returns "u@test.dev"
        every { session.user } returns user
        every { authRepository.currentSession } returns session
        every { authRepository.sessionStatus } returns sessionStatus as StateFlow<SessionStatus>
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.DARK
        every { userSettingsRepository.getLocale() } returns AppLocale.JA
        coEvery { tileRepository.getTiles("user-1") } returns emptyList()
        coEvery { tileRepository.getTimeline() } returns emptyList()
        coEvery { profileRepository.getProfile("user-1") } returns Profile(id = "user-1")
        coEvery { tileRepository.rescheduleTile(any(), any(), any()) } returns Unit
        coEvery { integrationRepository.getSettings() } returns mockk(relaxed = true)

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, integrationRepository)
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
}

