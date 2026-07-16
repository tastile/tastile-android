package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreatePanelContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Robolectric Compose coverage for Web-shaped quick-create navigation. */
@RunWith(AndroidJUnit4::class)
class QuickCreatePanelsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val projects = listOf(
        QuickCreateProject("workspace-focus", "Focus"),
        QuickCreateProject("workspace-home", "Home"),
    )

    @Test
    fun `base renders Web order and routes its essential and behavior controls`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store, {}, projects, listOf("health", "weekly")) }

        rule.onNodeWithTag("quick-create-base").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-title").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-organize-row").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-tile-kind").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-essential-time").performClick()
        rule.onNodeWithTag("quick-create-subpanel-Time").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-essential-duration").performClick()
        rule.onNodeWithTag("quick-create-subpanel-Duration").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-essential-repeat").performClick()
        rule.onNodeWithTag("quick-create-subpanel-Recurring").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-tasks-header").performClick()
        rule.onNodeWithTag("quick-create-subpanel-Completion").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("quick-create-behavior-card").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(QuickCreatePanel.Behavior, store.state.value.activePanel)
        assertTrue(rule.onAllNodesWithTag("quick-create-row-0").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `meta catalog suggestions clear apply and panel routing retain selections`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store, {}, projects, listOf("health", "weekly")) }

        rule.onNodeWithTag("quick-create-organize").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-subpanel-Meta").assertIsDisplayed()
        rule.onNodeWithTag("meta-project-workspace-focus").performScrollTo().performClick()
        rule.onNodeWithTag("meta-tag-suggestion-health").performClick()
        rule.onNodeWithTag("meta-tag-input").performTextReplacement("deep work")
        rule.onNodeWithTag("meta-tag-add").performClick()
        rule.onNodeWithTag("meta-memo").performTextReplacement("Protect this focus block")
        assertEquals("workspace-focus", store.state.value.meta.ownerSubjectId)
        assertEquals(listOf("health", "deep work"), store.state.value.meta.tags)
        rule.onNodeWithTag("meta-clear").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(null, store.state.value.meta.ownerSubjectId)
        assertTrue(store.state.value.meta.tags.isEmpty())
        rule.onNodeWithTag("meta-project-workspace-focus").performScrollTo().performClick()
        rule.onNodeWithTag("meta-tag-suggestion-weekly").performClick()
        rule.onNodeWithTag("meta-apply").performClick()
        rule.waitForIdle()
        assertEquals(QuickCreatePanel.Base, store.state.value.activePanel)
    }

    @Test
    fun `title and time survive subpanel navigation while validation reflects the base draft`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store, {}, projects) }
        rule.onNodeWithTag("quick-create-title").performTextReplacement("Plan review")
        rule.onNodeWithTag("quick-create-essential-time").performClick()
        rule.onNodeWithTag("quick-create-when-day").performClick()
        rule.onNodeWithTag("quick-create-start").performTextReplacement("2026-07-16T09:00:00+09:00")
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("Plan review").assertIsDisplayed()
        rule.onNodeWithText("2026-07-16T09:00:00+09:00").assertIsDisplayed()
        rule.onNodeWithText("Create").assertIsEnabled()
    }
}
