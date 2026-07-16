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
        assertTrue(rule.onAllNodesWithTag("quick-create-add-frame-rule").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `window rules and completion task v1 fields round trip from UI`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-2").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-add-window").performScrollTo().performClick()
        rule.waitForIdle()
        assertTrue(store.state.value.windows.single().rules.isEmpty())
        rule.onNodeWithText("Back").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("quick-create-row-6").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-task-show-0").performScrollTo().performTextReplacement("true")
        rule.onNodeWithTag("quick-create-task-complete-0").performScrollTo().performTextReplacement("{\"kind\":3,\"children\":[],\"term\":\"done\"}")
        rule.onNodeWithTag("quick-create-task-order-0").performScrollTo().performTextReplacement("[\"first\"]")
        assertTrue(store.state.value.plan.completion.tasks.single().show == kotlinx.serialization.json.JsonPrimitive(true))
        assertTrue(store.state.value.plan.completion.tasks.single().complete.term == kotlinx.serialization.json.JsonPrimitive("done"))
        assertTrue(store.state.value.plan.completion.tasks.single().order.size == 1)
    }

    @Test
    fun `base exposes only Web tile kinds and retains title with a time field`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-0").performClick()
        rule.onNodeWithText("Placement").assertIsDisplayed()
        rule.onNodeWithText("Recurring").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithTag("quick-create-kind-Execution").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("quick-create-title").performTextReplacement("Plan review")
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-2").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-start").performTextReplacement("2026-07-16T09:00:00+09:00")
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-0").performScrollTo().performClick()
        rule.onNodeWithText("Plan review").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-2").performScrollTo().performClick()
        rule.onNodeWithText("2026-07-16T09:00:00+09:00").assertIsDisplayed()
    }

    @Test
    fun `malformed non-padded and reversed offset spans disable Create`() {
        val store = QuickCreateStateStore(QuickCreateDraftState(identity = QuickCreateIdentity(title = "Ready")))
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-2").performClick()
        rule.onNodeWithTag("quick-create-start").performTextReplacement("2026-7-6T09:00:00+09:00")
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-validation-error").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("quick-create-row-2").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-start").performTextReplacement("2026-07-16T10:00:00+09:00")
        rule.onNodeWithTag("quick-create-end").performTextReplacement("2026-07-16T00:30:00Z")
        rule.onNodeWithText("Back").performClick()
        assertTrue(rule.onNodeWithText("Create").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled))
    }
}
