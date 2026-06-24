package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.IntegrationRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TileEditSheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun newDashboardViewModel(): DashboardViewModel {
        val authRepo = mockk<AuthRepository>(relaxed = true)
        val profileRepo = mockk<ProfileRepository>(relaxed = true)
        val tileRepo = mockk<TileRepository>(relaxed = true)
        val userSettingsRepo = mockk<UserSettingsRepository>(relaxed = true)
        val integrationRepo = mockk<IntegrationRepository>(relaxed = true)
        every { userSettingsRepo.getLocale() } returns AppLocale.EN
        return DashboardViewModel(
            authRepository = authRepo,
            profileRepository = profileRepo,
            tileRepository = tileRepo,
            userSettingsRepository = userSettingsRepo,
            integrationRepository = integrationRepo,
        )
    }

    @Test
    fun `TileEditSheet renders tile title for the requested id`() {
        val overlay = OverlayViewModel()
        val vm = newDashboardViewModel()
        val tile = Tile(id = "abc", title = "Write spec", lifecycle = "Ready")
        vm.replaceTilesForTest(listOf(tile))
        vm.selectTile("abc")

        rule.setContent {
            TileEditSheet(overlay = overlay, viewModel = vm)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Write spec").assertDoesNotExist()

        rule.runOnUiThread {
            overlay.show(Overlay.TileEdit(tileId = "abc"))
        }
        rule.waitForIdle()
        rule.onNodeWithText("Write spec").assertIsDisplayed()
    }

    @Test
    fun `TileEditSheet does not render content when overlay is Hidden`() {
        val overlay = OverlayViewModel() // starts Hidden
        val vm = newDashboardViewModel()
        val tile = Tile(id = "abc", title = "Write spec", lifecycle = "Ready")
        vm.replaceTilesForTest(listOf(tile))
        vm.selectTile("abc")

        rule.setContent {
            TileEditSheet(overlay = overlay, viewModel = vm)
        }

        rule.onNodeWithText("Write spec").assertDoesNotExist()
    }
}