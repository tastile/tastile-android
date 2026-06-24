package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun stubVm(locale: AppLocale = AppLocale.EN): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.locale } returns MutableStateFlow(locale)
        every { vm.loading } returns MutableStateFlow(false)
        return vm
    }

    private fun stubOverlay(): OverlayViewModel = mockk<OverlayViewModel>(relaxed = true)

    @Test
    fun `renders all 5 settings rows with icon and label`() {
        rule.setContent {
            SettingsScreen(viewModel = stubVm(), overlay = stubOverlay())
        }

        rule.onAllNodesWithText("Locale", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Theme", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Notifications", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Privacy", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("About", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `locale value reflects current AppLocale`() {
        rule.setContent {
            SettingsScreen(viewModel = stubVm(locale = AppLocale.JA), overlay = stubOverlay())
        }
        rule.onAllNodesWithText("ja", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `row tap opens preferences side panel overlay`() {
        val overlay = stubOverlay()
        rule.setContent {
            SettingsScreen(viewModel = stubVm(), overlay = overlay)
        }
        rule.onAllNodesWithText("Locale", substring = true).onFirst().performClick()

        verify { overlay.show(Overlay.SidePanel(SidePanelSection.Preferences)) }
    }
}