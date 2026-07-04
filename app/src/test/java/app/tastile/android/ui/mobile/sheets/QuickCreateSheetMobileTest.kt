package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Profile
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.GoogleCalendarIntegrationSettings
import app.tastile.android.data.repository.IntegrationRepository
import app.tastile.android.data.repository.IntegrationSettingsResponse
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.RuntimePathsResponse
import app.tastile.android.data.repository.SyncStatusResponse
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.data.repository.TileQuotaResponse
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.TilesResponse
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickCreateSheetMobileTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun newDashboardViewModel(): DashboardViewModel {
        val authRepo = mockk<AuthRepository>(relaxed = true)
        val profileRepo = mockk<ProfileRepository>(relaxed = true)
        val tileRepo = mockk<TileRepository>(relaxed = true)
        val userSettingsRepo = mockk<UserSettingsRepository>(relaxed = true)
        val integrationRepo = mockk<IntegrationRepository>(relaxed = true)
        every { userSettingsRepo.getLocale() } returns AppLocale.EN
        // Unauthenticated keeps refreshAll in the empty-state branch so it never
        // touches the mock repositories; refreshTimeline (fired by the
        // selectedDay/scale combine) still runs, so stub getTimeline explicitly.
        every { authRepo.authState } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        coEvery { tileRepo.getTimeline(any(), any()) } returns emptyList()
        coEvery { tileRepo.getTiles(any<String>()) } returns emptyList()
        coEvery { tileRepo.getTiles() } returns TilesResponse(emptyList())
        coEvery { profileRepo.getProfile(any()) } returns Profile(id = "user-1")
        coEvery { integrationRepo.getSettings() } returns IntegrationSettingsResponse(
            googleCalendar = GoogleCalendarIntegrationSettings()
        )
        every { integrationRepo.lastSuccessfulDaemonBaseUrl() } returns null
        coEvery { integrationRepo.getSyncStatus() } returns SyncStatusResponse()
        coEvery { integrationRepo.getRuntimePaths() } returns RuntimePathsResponse(
            profileName = "cloud",
            appDataDir = "",
            dbPath = "",
            sessionPath = "",
            daemonStartupLogPath = "",
            daemonExecutablePath = "",
        )
        coEvery { integrationRepo.getTileQuota() } returns TileQuotaResponse(
            plan = "free",
            tileCount = 0,
            maxTiles = 100,
            remainingTiles = 100,
            limitReached = false,
        )
        return DashboardViewModel(
            authRepository = authRepo,
            profileRepository = profileRepo,
            tileRepository = tileRepo,
            userSettingsRepository = userSettingsRepo,
            integrationRepository = integrationRepo,
        )
    }

    @Test
    fun `QuickCreateSheetMobile shows Quick Create title when overlay is QuickCreate`() {
        val overlay = OverlayViewModel()
        val vm = newDashboardViewModel()

        rule.setContent {
            QuickCreateSheetMobile(
                overlay = overlay,
                dashboardViewModel = vm,
            )
        }
        rule.waitForIdle()
        rule.onAllNodesWithText("Quick Create").assertCountEquals(0)

        rule.runOnUiThread {
            overlay.show(Overlay.QuickCreate)
        }
        rule.waitForIdle()
        // PanelSheet renders the title once and QuickCreateSheet itself renders
        // a second "Quick Create" header, so two nodes match — assert the
        // first is displayed rather than requiring an exact match.
        rule.onAllNodesWithText("Quick Create").onFirst().assertIsDisplayed()
    }

    @Test
    fun `QuickCreateSheetMobile does not show Quick Create when overlay is Hidden`() {
        val overlay = OverlayViewModel() // starts Hidden

        rule.setContent {
            QuickCreateSheetMobile(
                overlay = overlay,
                dashboardViewModel = newDashboardViewModel(),
            )
        }

        rule.onAllNodesWithText("Quick Create").assertCountEquals(0)
    }
}
