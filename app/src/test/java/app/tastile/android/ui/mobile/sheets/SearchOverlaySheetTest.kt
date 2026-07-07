package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import androidx.compose.ui.test.isEditable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchOverlaySheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `SearchOverlaySheet shows unfiltered catalog entries when opened`() {
        val overlay = OverlayViewModel()

        rule.setContent {
            SearchOverlaySheet(overlay = overlay)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Start tile").assertDoesNotExist()

        rule.runOnUiThread {
            overlay.show(Overlay.Search)
        }
        rule.waitForIdle()

        // "Start tile" is one of the 22 catalog entries shown initially
        // (catalog is capped at 8 in the UI, but "Start tile" is alphabetically first).
        rule.onNodeWithText("Start tile").assertIsDisplayed()
    }

    @Test
    fun `SearchOverlaySheet filters catalog as user types`() {
        val overlay = OverlayViewModel()

        rule.setContent {
            SearchOverlaySheet(overlay = overlay)
        }
        rule.runOnUiThread {
            overlay.show(Overlay.Search)
        }
        rule.waitForIdle()
        rule.waitForIdle()

        // Sanity: "Start tile" is visible before any filtering
        rule.onNodeWithText("Start tile").assertIsDisplayed()

        // Type "break" - only "Start break" should match (no other label/operationId
        // contains "break" in the catalog).
        rule.onNode(isEditable()).performTextInput("break")
        rule.waitForIdle()

        rule.onNodeWithText("Start break").assertIsDisplayed()
        rule.onNodeWithText("Start tile").assertDoesNotExist()

        // Exactly one entry should match "break"
        val matches = rule.onAllNodesWithText("Start break").fetchSemanticsNodes()
        assert(matches.size == 1) {
            "Expected exactly 1 visible 'Start break' node, got ${matches.size}"
        }
    }

    @Test
    fun `SearchOverlaySheet does not render content when overlay is Hidden`() {
        val overlay = OverlayViewModel() // starts Hidden

        rule.setContent {
            SearchOverlaySheet(overlay = overlay)
        }

        rule.onNodeWithText("Search").assertDoesNotExist()
    }
}
