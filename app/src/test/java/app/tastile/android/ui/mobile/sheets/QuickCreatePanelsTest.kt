package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreatePanelContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class QuickCreatePanelsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `root lists Web panels in order and every row returns without losing title`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }

        rule.onNodeWithTag("quick-create-panel-list").assertIsDisplayed()
        val panels = listOf("Base", "Intent", "Time", "Duration", "Recurring", "References", "Completion", "Meta", "Behavior")
        panels.forEachIndexed { index, panel ->
            rule.onNodeWithTag("quick-create-row-$index").performScrollTo().performClick()
            rule.onNodeWithTag("quick-create-subpanel-$panel").assertIsDisplayed()
            rule.onNodeWithText("Back").performClick()
            rule.onNodeWithTag("quick-create-panel-list").assertIsDisplayed()
        }
    }

    @Test
    fun `recurrence inputs are visible only for recurring tiles and validation blocks invalid drafts`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }

        rule.onNodeWithTag("quick-create-row-4").performClick()
        assertTrue(rule.onAllNodesWithTag("quick-create-recurring-controls").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-0").performClick()
        rule.onNodeWithText("Recurring").performClick()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-4").performClick()
        rule.onNodeWithTag("quick-create-recurring-controls").assertIsDisplayed()

        store.updateIdentity(store.state.value.identity.copy(title = " "))
        store.updateTime(store.state.value.time.copy(durationMinMax = QuickCreateDurationRange(90, 30)))
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-validation-error").performScrollTo().assertIsDisplayed()
    }
}
