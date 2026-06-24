package app.tastile.android.ui.mobile

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Verifies OverlayViewModel state transitions used by the production BackHandler
// wiring in MobileScaffold. The production BackHandler itself is not exercised
// here: Robolectric cannot deliver a real hardware back press to a
// ComponentActivity-backed composition, so wiring is covered by code review
// and manual testing.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class OverlayViewModelDismissTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `OverlayViewModel dismiss transitions from QuickCreate to Hidden`() {
        val overlay = OverlayViewModel()
        rule.setContent { Text("ok") }
        rule.runOnUiThread { overlay.show(Overlay.QuickCreate) }
        rule.waitForIdle()
        check(overlay.current.value is Overlay.QuickCreate)

        rule.runOnUiThread { overlay.dismiss() }
        rule.waitForIdle()
        check(overlay.current.value is Overlay.Hidden)
    }

    @Test
    fun `OverlayViewModel dismiss is idempotent when already Hidden`() {
        val overlay = OverlayViewModel()
        rule.setContent { Text("ok") }
        rule.waitForIdle()
        check(overlay.current.value is Overlay.Hidden)

        rule.runOnUiThread { overlay.dismiss() }
        rule.waitForIdle()
        check(overlay.current.value is Overlay.Hidden)
    }
}