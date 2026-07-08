package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection
import io.mockk.mockk
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
    fun `SidePanelSheet renders section navigation for Calendar section`() {
        val overlay = OverlayViewModel()
        rule.setContent {
            SidePanelSheet(
                overlay = overlay,
                dashboardViewModel = mockk(relaxed = true),
                onNavigate = {},
            )
        }
        rule.runOnUiThread { overlay.show(Overlay.SidePanel(SidePanelSection.Calendar)) }
        rule.waitForIdle()

        rule.onNodeWithText("Navigation").assertIsDisplayed()
        rule.onAllNodesWithText("Timeline", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Tasks", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Projects", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `SidePanelSheet does not render content when overlay is Hidden`() {
        val overlay = OverlayViewModel() // starts Hidden
        rule.setContent {
            SidePanelSheet(
                overlay = overlay,
                dashboardViewModel = mockk(relaxed = true),
                onNavigate = {},
            )
        }

        rule.onNodeWithText("Navigation").assertDoesNotExist()
        rule.onNodeWithText("Timeline").assertDoesNotExist()
    }
}
