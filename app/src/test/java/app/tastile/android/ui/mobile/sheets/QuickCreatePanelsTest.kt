package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsProperties
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

        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-0").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-title").performTextReplacement(" ")
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-2").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-start").performTextReplacement("2026-07-16T11:00:00Z")
        rule.onNodeWithTag("quick-create-end").performTextReplacement("2026-07-16T09:00:00Z")
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-3").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-duration-min").performTextReplacement("90")
        rule.onNodeWithTag("quick-create-duration-max").performTextReplacement("30")
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-validation-error").performScrollTo().assertIsDisplayed()
        assertTrue(rule.onNodeWithText("Create").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled))
    }

    @Test
    fun `reference completion window and recurring lists round trip through their panels`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }

        rule.onNodeWithTag("quick-create-row-5").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-add-reference").performClick()
        rule.onNodeWithTag("quick-create-reference-id-0").performTextReplacement("ref-1")
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-5").performScrollTo().performClick()
        rule.onNodeWithText("ref-1").assertIsDisplayed()

        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-2").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-add-window").performScrollTo().performClick()
        rule.waitForIdle()
        assertTrue(store.state.value.windows.isNotEmpty())
        rule.onNodeWithText("Back").performScrollTo().performClick()
        rule.waitForIdle()
        assertTrue(store.state.value.activePanel == QuickCreatePanel.Base)
        rule.onNodeWithTag("quick-create-row-4").performScrollTo().performClick()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-0").performScrollTo().performClick()
        rule.onNodeWithText("Recurring").performClick()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-4").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-add-frame-rule").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("quick-create-add-recurring-rule").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("quick-create-frame-rule-id-0").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("quick-create-recurring-rule-id-0").performScrollTo().assertIsDisplayed()
    }
}
