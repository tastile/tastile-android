package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SidePanelSheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Build a real [DashboardViewModel] backed by relaxed mocks for its
     * five concrete repositories. The Compose tree under test never
     * invokes any behaviour beyond reading defaults; the relaxed stubs
     * satisfy the constructor's `userSettingsRepository.getThemeMode()`,
     * `authRepository.authState.collect { … }`, and friends without
     * pulling the real network / datastore.
     */
    private fun newDashboardViewModel(): DashboardViewModel {
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val tileRepository = mockk<TileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val referenceOverlayStore = mockk<ReferenceOverlayStore>(relaxed = true)
        every { authRepository.currentSession } returns null
        every { authRepository.authState } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        every { userSettingsRepository.getThemeMode() } returns ThemeMode.SYSTEM
        every { userSettingsRepository.getLocale() } returns AppLocale.EN
        every { userSettingsRepository.getSecurityLockEnabled() } returns false
        every { userSettingsRepository.getSecurityLockTimeoutMinutes() } returns 5
        coEvery { tileRepository.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        coEvery { tileRepository.getTimeline(any(), any()) } returns emptyList()
        coEvery { profileRepository.getProfile(any()) } returns null
        return DashboardViewModel(
            authRepository,
            profileRepository,
            tileRepository,
            userSettingsRepository,
            referenceOverlayStore,
        )
    }

    @Test
    fun `SidePanelSheet renders section navigation for Calendar section`() {
        val overlay = OverlayViewModel()
        val dashboardViewModel = newDashboardViewModel()
        rule.setContent {
            SidePanelSheet(
                overlay = overlay,
                dashboardViewModel = dashboardViewModel,
                onNavigate = {},
            )
        }
        rule.runOnUiThread { overlay.show(Overlay.SidePanel(SidePanelSection.Calendar)) }
        rule.waitForIdle()

        rule.onNodeWithText("Navigation").assertIsDisplayed()
        rule.onAllNodesWithText("Timeline", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Tasks", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Projects", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `SidePanelSheet does not render content when overlay is Hidden`() {
        val overlay = OverlayViewModel() // starts Hidden
        val dashboardViewModel = newDashboardViewModel()
        rule.setContent {
            SidePanelSheet(
                overlay = overlay,
                dashboardViewModel = dashboardViewModel,
                onNavigate = {},
            )
        }

        rule.onNodeWithText("Navigation").assertDoesNotExist()
        rule.onNodeWithText("Timeline").assertDoesNotExist()
    }
}