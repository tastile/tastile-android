package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AccountMenuSheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `AccountMenuSheet renders email and menu rows when opened`() {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.email } returns MutableStateFlow("op@example.com")
        every { vm.profile } returns MutableStateFlow(null)
        val overlay = OverlayViewModel()

        rule.setContent {
            AccountMenuSheet(viewModel = vm, overlay = overlay)
        }
        rule.runOnUiThread {
            overlay.show(Overlay.AccountMenu)
        }
        rule.waitForIdle()

        rule.onNodeWithText("op@example.com").assertIsDisplayed()
        rule.onNodeWithText("Account").assertIsDisplayed()
        rule.onNodeWithText("Subscription").assertIsDisplayed()
        rule.onNodeWithText("Memo").assertIsDisplayed()
        rule.onNodeWithText("Prompt history").assertIsDisplayed()
        rule.onNodeWithText("Billing").assertIsDisplayed()
        rule.onNodeWithText("Sign out").assertIsDisplayed()
    }

    @Test
    fun `AccountMenuSheet shows Signed in fallback when email is blank`() {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.email } returns MutableStateFlow("")
        every { vm.profile } returns MutableStateFlow(null)
        val overlay = OverlayViewModel()

        rule.setContent {
            AccountMenuSheet(viewModel = vm, overlay = overlay)
        }
        rule.runOnUiThread {
            overlay.show(Overlay.AccountMenu)
        }
        rule.waitForIdle()

        rule.onNodeWithText("Signed in").assertIsDisplayed()
    }

    @Test
    fun `AccountMenuSheet does not render content when overlay is Hidden`() {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.email } returns MutableStateFlow("op@example.com")
        every { vm.profile } returns MutableStateFlow(null)
        val overlay = OverlayViewModel() // starts Hidden

        rule.setContent {
            AccountMenuSheet(viewModel = vm, overlay = overlay)
        }

        rule.onNodeWithText("op@example.com").assertDoesNotExist()
        rule.onNodeWithText("Account").assertDoesNotExist()
    }
}