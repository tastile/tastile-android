package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SidePanelSheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `SidePanelSheet renders section title and toggle group for Calendar`() {
        val overlay = OverlayViewModel()
        rule.setContent { SidePanelSheet(overlay = overlay) }
        rule.runOnUiThread { overlay.show(Overlay.SidePanel(SidePanelSection.Calendar)) }
        rule.waitForIdle()

        rule.onNodeWithText("Calendar").assertIsDisplayed()
        // "Day" is the default selection; active label uses primary color +
        // titleSmall style (no brackets), so plain "Day" matches exactly.
        rule.onNodeWithText("Day").assertIsDisplayed()
        rule.onNodeWithText("Week").assertIsDisplayed()
        rule.onNodeWithText("Month").assertIsDisplayed()
    }

    @Test
    fun `SidePanelSheet does not render content when overlay is Hidden`() {
        val overlay = OverlayViewModel() // starts Hidden
        rule.setContent { SidePanelSheet(overlay = overlay) }

        rule.onNodeWithText("Calendar").assertDoesNotExist()
        rule.onNodeWithText("Day").assertDoesNotExist()
    }
}
