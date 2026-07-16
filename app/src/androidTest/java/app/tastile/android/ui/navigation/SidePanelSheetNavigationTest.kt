package app.tastile.android.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import app.tastile.android.ui.mobile.sheets.SidePanelSheet
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * R17 (audit 2026-07-16): instrumented navigation smoke test for the side
 * panel that drives route changes in [app.tastile.android.ui.mobile.MobileScaffold].
 *
 * Mounts [SidePanelSheet] in a real [ComponentActivity] on the device, opens
 * the panel via [OverlayViewModel.show], taps a tab row, and asserts that the
 * `onNavigate` callback receives the expected destination route. This catches
 * regressions where the side panel stops forwarding taps to the nav controller
 * (e.g. a contentDescription flip, a route rename, or a closure of the panel
 * before the click is dispatched).
 */
class SidePanelSheetNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tappingTasksTab_invokesOnNavigate_withExecuteRoute() {
        val overlay = OverlayViewModel()
        val dashboardViewModel = stubDashboardViewModel()
        var capturedRoute: String? = null

        rule.setContent {
            SidePanelSheet(
                overlay = overlay,
                dashboardViewModel = dashboardViewModel,
                onNavigate = { route -> capturedRoute = route },
            )
        }

        rule.runOnUiThread { overlay.show(Overlay.SidePanel(SidePanelSection.Schedule)) }
        rule.waitForIdle()

        rule.onNodeWithText("Tasks").assertIsDisplayed().performClick()
        rule.waitForIdle()

        check(capturedRoute == "execute") {
            "Expected onNavigate(\"execute\") but got onNavigate(\"$capturedRoute\")"
        }
    }

    /**
     * Build a relaxed-mock-backed [DashboardViewModel]. The side panel only
     * reads default-theming state synchronously; the relaxed stubs satisfy
     * the constructor and the [MutableStateFlow] backing fields return safe
     * defaults without pulling the real network / datastore.
     */
    private fun stubDashboardViewModel(): DashboardViewModel {
        val authRepository = mockk<AuthRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val tileRepository = mockk<TileRepository>(relaxed = true)
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        val referenceOverlayStore = mockk<ReferenceOverlayStore>(relaxed = true)
        every { authRepository.currentSession } returns null
        every { authRepository.getAuthStateStream } returns MutableStateFlow(TastileAuthState.Unauthenticated)
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
}
