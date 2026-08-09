package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.ui.dashboard.DashboardViewModel
import io.mockk.every
import io.mockk.mockk
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
        every { vm.themeMode } returns MutableStateFlow(ThemeMode.DARK)
        every { vm.securityLockEnabled } returns MutableStateFlow(false)
        every { vm.securityLockTimeoutMinutes } returns MutableStateFlow(15)
        return vm
    }

    @Test
    fun `renders all 4 settings rows with icon and label`() {
        rule.setContent {
            SettingsScreen(viewModel = stubVm(), onBack = {})
        }

        rule.onAllNodesWithText("Theme", substring = true).onFirst().performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Language", substring = true).onFirst().performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Security", substring = true).onFirst().performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Notifications", substring = true).onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `locale value reflects current AppLocale`() {
        rule.setContent {
            SettingsScreen(viewModel = stubVm(locale = AppLocale.JA), onBack = {})
        }
        // The dropdown TextField mirrors `localeLabel(current)` — for JA the
        // canonical name is "日本語" (locale-invariant across all values-*/
        // folders). Scroll first because the dropdown sits below the
        // settings list and may render off-screen on small devices.
        rule.onAllNodesWithText("日本語", substring = true).onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `row tap opens locale picker dialog`() {
        rule.setContent {
            SettingsScreen(viewModel = stubVm(), onBack = {})
        }
        // The picker is an ExposedDropdownMenuBox. Click the TextField anchor
        // (tagged "language-dropdown-trigger"); clicks on a Text child of the
        // OutlinedTextField don't reliably bubble up to the menuAnchor's
        // clickable region in Compose UI tests.
        rule.onNodeWithTag("language-dropdown-trigger").performScrollTo().performClick()

        rule.onAllNodesWithText("日本語", substring = true).onFirst().assertIsDisplayed()
    }
}