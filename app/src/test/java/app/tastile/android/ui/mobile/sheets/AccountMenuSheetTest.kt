package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.R
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

    private fun openMenu(overlay: OverlayViewModel) {
        rule.runOnUiThread {
            overlay.show(Overlay.AccountMenu)
        }
        rule.waitForIdle()
    }

    @Test
    fun `AccountMenuSheet renders email header and 4 menu rows when opened`() {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.email } returns MutableStateFlow("op@example.com")
        val overlay = OverlayViewModel()

        rule.setContent {
            AccountMenuSheet(viewModel = vm, overlay = overlay)
        }
        openMenu(overlay)

        rule.onNodeWithText("op@example.com").assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.nav_account_profile)).assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.nav_account_subscription)).assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.nav_account_tokens)).assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.nav_account_sign_out)).assertIsDisplayed()
    }

    @Test
    fun `AccountMenuSheet shows Signed in fallback when email is blank`() {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.email } returns MutableStateFlow("")
        val overlay = OverlayViewModel()

        rule.setContent {
            AccountMenuSheet(viewModel = vm, overlay = overlay)
        }
        openMenu(overlay)

        rule.onNodeWithText(rule.activity.getString(R.string.shell_account_signed_in)).assertIsDisplayed()
    }

    @Test
    fun `AccountMenuSheet does not render content when overlay is Hidden`() {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.email } returns MutableStateFlow("op@example.com")
        val overlay = OverlayViewModel() // starts Hidden

        rule.setContent {
            AccountMenuSheet(viewModel = vm, overlay = overlay)
        }

        rule.onNodeWithText("op@example.com").assertDoesNotExist()
        rule.onNodeWithText(rule.activity.getString(R.string.nav_account_profile)).assertDoesNotExist()
    }

    @Test
    fun `AccountMenuSheet does not invoke signOut on initial Sign out tap (confirm dialog gates it)`() {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.email } returns MutableStateFlow("op@example.com")
        val overlay = OverlayViewModel()

        rule.setContent {
            AccountMenuSheet(viewModel = vm, overlay = overlay)
        }
        openMenu(overlay)
        // Give ModalBottomSheet time to animate in; without this the touch
        // injection can land on a transitioning layout node and be swallowed.
        rule.waitForIdle()

        rule.onNodeWithTag("account_menu_sign_out").performClick()
        rule.waitForIdle()

        // Confirm dialog gating: signOut NOT yet invoked from the first tap.
        verify(exactly = 0) { vm.signOut() }
    }

    @Test
    fun `AccountMenuSheet invokes viewModel signOut after confirm`() {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.email } returns MutableStateFlow("op@example.com")
        val overlay = OverlayViewModel()

        rule.setContent {
            AccountMenuSheet(viewModel = vm, overlay = overlay)
        }
        openMenu(overlay)
        rule.waitForIdle()

        // The "Sign out" row's clickable region is tagged on the row itself
        // (mirrors QuickCreateBasePanel's working pattern); clicking the Text
        // node does not always dispatch through Compose's gesture handling
        // inside ModalBottomSheet under Robolectric, so dispatch through the
        // testTag instead.
        rule.onNodeWithTag("account_menu_sign_out").performClick()
        rule.waitForIdle()
        // The AlertDialog renders the confirm button in its own DialogWindow
        // which `createAndroidComposeRule`'s main tree does not see. The
        // dialog confirm Button has testTag `account_menu_sign_out_confirm`
        // and reuses the "Sign out" label; we click it via that tag after
        // allowing a few frames for the dialog to settle.
        for (attempt in 1..5) {
            val found = runCatching {
                rule.onNodeWithTag("account_menu_sign_out_confirm").assertExists()
                true
            }.getOrDefault(false)
            if (found) {
                rule.onNodeWithTag("account_menu_sign_out_confirm").performClick()
                rule.waitForIdle()
                break
            }
            rule.waitForIdle()
            Thread.sleep(60L * attempt)
        }

        verify(exactly = 1) { vm.signOut() }
    }
}
