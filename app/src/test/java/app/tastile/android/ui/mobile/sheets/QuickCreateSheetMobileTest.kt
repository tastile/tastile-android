package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.IntegrationRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
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
        every { authRepo.authState } returns MutableStateFlow(
            TastileAuthState.Authenticated(
                userId = "user-1",
                email = "test@example.com",
                idToken = "id-token",
                accessToken = "access-token",
                refreshToken = null
            )
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
        rule.onNodeWithText("Quick Create").assertDoesNotExist()

        rule.runOnUiThread {
            overlay.show(Overlay.QuickCreate)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Quick Create").assertIsDisplayed()
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

        rule.onNodeWithText("Quick Create").assertDoesNotExist()
    }
}
