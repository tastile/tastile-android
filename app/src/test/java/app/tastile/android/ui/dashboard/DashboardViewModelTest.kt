package app.tastile.android.ui.dashboard

import app.tastile.android.data.model.Profile
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.IntegrationRepository
import app.tastile.android.data.repository.RecoveryResetResponse
import app.tastile.android.data.repository.RuntimePathsResponse
import app.tastile.android.data.repository.SyncStatusResponse
import app.tastile.android.data.repository.TileQuotaResponse
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun handleCardAction_routesToRequestPromptCommand() {
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
        coEvery { tileRepository.requestPrompt(any()) } returns true
        coEvery { integrationRepository.getSettings() } returns mockk(relaxed = true)

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, integrationRepository)

        viewModel.handleCardAction(CardAction.TriggerPrompt("tile-1"))

        coVerify(atLeast = 1) { tileRepository.requestPrompt("tile-1") }
    }

    @Test
    fun handleCardAction_routesToBreakExtendAndDeferCommands() {
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
        coEvery { tileRepository.startBreak(any(), any()) } returns Unit
        coEvery { tileRepository.extendTile(any()) } returns Unit
        coEvery { tileRepository.deferTile(any(), any(), any()) } returns Unit
        coEvery { integrationRepository.getSettings() } returns mockk(relaxed = true)

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, integrationRepository)

        viewModel.handleCardAction(CardAction.StartBreak("tile-1"))
        viewModel.handleCardAction(CardAction.ExtendTile("tile-1", minutes = 10))
        viewModel.handleCardAction(CardAction.DeferTile("tile-1"))

        coVerify(atLeast = 1) { tileRepository.startBreak(5, null) }
        coVerify(atLeast = 1) { tileRepository.extendTile(10) }
        coVerify(atLeast = 1) { tileRepository.deferTile("tile-1", null, null) }
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

    @Test
    fun refreshAll_exposesDiagnosticsForStatsValidation() {
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
        coEvery { integrationRepository.getSettings() } returns mockk(relaxed = true)
        every { tileRepository.latestReadDiagnostics() } returns "source=core revision=12 snapshot_tiles=5"
        every { integrationRepository.lastSuccessfulDaemonBaseUrl() } returns "http://10.0.2.2:3140"

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, integrationRepository)
        viewModel.refreshAll()

        val diagnostics = viewModel.statsDiagnostics.value
        assertTrue(diagnostics.contains("source=core"))
        assertTrue(diagnostics.contains("10.0.2.2:3140"))
    }

    @Test
    fun refreshAll_setsDiagnosticsWhenUnauthenticated() {
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val tileRepository = mockk<TileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val integrationRepository = mockk<IntegrationRepository>(relaxed = true)
        val sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated(isSignOut = false))
        every { authRepository.currentSession } returns null
        every { authRepository.sessionStatus } returns sessionStatus as StateFlow<SessionStatus>
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.DARK
        every { userSettingsRepository.getLocale() } returns AppLocale.JA

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, integrationRepository)
        viewModel.refreshAll()

        assertEquals("source=none reason=unauthenticated", viewModel.statsDiagnostics.value)
    }

    @Test
    fun daemonMaintenanceActions_delegateToIntegrationRepository() {
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
        coEvery { integrationRepository.getSettings() } returns mockk(relaxed = true)
        coEvery { integrationRepository.triggerTick() } returns Unit
        coEvery { integrationRepository.resetLocalSyncData() } returns RecoveryResetResponse(ok = true, message = "ok", applied = 1)
        coEvery { integrationRepository.redownloadRemoteSyncData() } returns RecoveryResetResponse(ok = true, message = "ok", applied = 2)

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, integrationRepository)
        viewModel.triggerDaemonTick()
        viewModel.resetLocalSyncData()
        viewModel.redownloadRemoteSyncData()

        coVerify(atLeast = 1) { integrationRepository.triggerTick() }
        coVerify(atLeast = 1) { integrationRepository.resetLocalSyncData() }
        coVerify(atLeast = 1) { integrationRepository.redownloadRemoteSyncData() }
    }

    @Test
    fun refreshDaemonStatus_buildsReadableSummary() {
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
        coEvery { integrationRepository.getSettings() } returns mockk(relaxed = true)
        coEvery { integrationRepository.getSyncStatus() } returns SyncStatusResponse(
            active = true,
            running = true,
            lastSuccessAt = "2026-04-06T12:00:00Z",
            lastError = null
        )
        coEvery { integrationRepository.getRuntimePaths() } returns RuntimePathsResponse(
            profileName = "default",
            appDataDir = "C:\\data",
            dbPath = "C:\\data\\db.sqlite",
            sessionPath = "C:\\data\\session.json",
            daemonStartupLogPath = "C:\\data\\daemon.log",
            daemonExecutablePath = "C:\\bin\\daemon.exe"
        )
        coEvery { integrationRepository.getTileQuota() } returns TileQuotaResponse(
            plan = "pro",
            tileCount = 10,
            maxTiles = 500,
            remainingTiles = 490,
            limitReached = false,
            source = "server"
        )

        val viewModel = DashboardViewModel(authRepository, profileRepository, tileRepository, userSettingsRepository, integrationRepository)
        viewModel.refreshDaemonStatus()

        assertTrue(viewModel.daemonStatusSummary.value.contains("sync=running"))
        assertTrue(viewModel.daemonStatusSummary.value.contains("quota=10/500"))
        assertTrue(viewModel.daemonStatusSummary.value.contains("profile=default"))
    }
}

