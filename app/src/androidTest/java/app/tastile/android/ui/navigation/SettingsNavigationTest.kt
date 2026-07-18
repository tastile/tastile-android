package app.tastile.android.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
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
import app.tastile.android.ui.mobile.SidePanelDrawerContent
import app.tastile.android.ui.mobile.tabs.SettingsScreen
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Navigation smoke tests for the Settings screen and the Settings nav-drawer entry.
 *
 * These tests are expected to fail before the production UI changes land because
 * the required test tags (settings-app-bar) do not yet exist in SettingsScreen.
 */
class SettingsNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsScreen_displaysAppBarAndThemeAndBackButton() {
        val dashboardViewModel = stubDashboardViewModel()
        var onBackRan = false

        rule.setContent {
            SettingsScreen(
                viewModel = dashboardViewModel,
                onBack = { onBackRan = true },
            )
        }

        rule.onNodeWithTag("settings-app-bar", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Setting").assertIsDisplayed()
        rule.onNodeWithText("Theme").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
        rule.waitForIdle()

        check(onBackRan) { "Expected onBack to have run after clicking Back" }
    }

    @Test
    fun sidePanelDrawerContent_displaysSettingsRow() {
        rule.setContent {
            val navController = rememberNavController()
            val drawerState = rememberDrawerState(DrawerValue.Closed)

            SidePanelDrawerContent(
                navController = navController,
                drawerState = drawerState,
            )
        }

        rule.onNodeWithTag("side-panel-section-settings", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("side-panel-row-settings", useUnmergedTree = true).assertIsDisplayed()
    }

    /**
     * Build a relaxed-mock-backed [DashboardViewModel]. Mirrors the pattern
     * from [SidePanelSheetNavigationTest.stubDashboardViewModel].
     */
    private fun stubDashboardViewModel(): DashboardViewModel {
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
}
