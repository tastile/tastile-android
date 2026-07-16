package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreatePanelContent
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        rule.waitForIdle()
        rule.onNodeWithTag("quick-create-tasks-header").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-subpanel-Completion").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("quick-create-behavior-card").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(QuickCreatePanel.Meta, store.state.value.activePanel)
        rule.onNodeWithTag("behavior-role").performScrollTo().assertIsDisplayed()
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
        rule.onNodeWithTag("meta-memo").performTextReplacement("Protect this focus block")
        assertEquals("workspace-focus", store.state.value.meta.ownerSubjectId)
        assertEquals(listOf("health"), store.state.value.meta.tags)
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
        rule.onNodeWithTag("quick-create-start").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("Plan review").assertIsDisplayed()
        rule.onNodeWithText("Create").assertIsNotEnabled()
    }

    @Test
    fun `create dispatches only valid drafts and blocks duplicate submission`() {
        val store = QuickCreateStateStore(
            QuickCreateDraftState(
                identity = QuickCreateIdentity(title = "Plan review"),
                time = QuickCreateTime(span = QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T10:00:00Z")),
            ),
        )
        var submitted: QuickCreateDraftState? = null
        val submitting = mutableStateOf(false)
        rule.setContent { QuickCreatePanelContent(store, {}, projects, onSubmit = { submitted = it }, isSubmitting = submitting.value) }

        rule.onNodeWithTag("quick-create-submit").performScrollTo().assertIsEnabled().performClick()
        assertEquals("Plan review", submitted?.identity?.title)

        rule.runOnUiThread { submitting.value = true }
        rule.onNodeWithTag("quick-create-submit").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Creating…").assertIsDisplayed()
    }

    @Test
    fun `submission errors remain visible and invalid draft does not dispatch`() {
        val store = QuickCreateStateStore()
        var submits = 0
        rule.setContent { QuickCreatePanelContent(store, {}, projects, onSubmit = { submits++ }, submitError = "Plan unavailable") }

        rule.onNodeWithTag("quick-create-submit").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("quick-create-submit-error").assertIsDisplayed()
        assertEquals(0, submits)
    }

    @Test
    fun `base condition card routes through intent and intent routes to Web panels`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store, {}, projects) }

        rule.onNodeWithTag("quick-create-condition-add").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-subpanel-Intent").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-intent-time").performClick()
        rule.onNodeWithTag("quick-create-subpanel-Time").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-condition-add").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-intent-completion").performClick()
        rule.onNodeWithTag("quick-create-subpanel-Completion").assertIsDisplayed()
    }

    @Test
    fun `duration none references and completion quick adds preserve Web v1 state`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store, {}, projects) }

        rule.onNodeWithTag("quick-create-essential-duration").performClick()
        rule.onNodeWithTag("quick-create-duration-none").performClick()
        assertNull(store.state.value.time.durationMinMax.minMs)
        assertNull(store.state.value.time.durationMinMax.maxMs)
        rule.onNodeWithText("Back").performClick()

        rule.onNodeWithTag("quick-create-references-link").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-add-reference").performClick()
        val reference = store.state.value.plan.references.single()
        assertEquals("", reference.id)
        assertEquals("0", reference.target.jsonObjectOrEmptyForTest().getValue("kind").jsonPrimitive.content)
        assertEquals("4", reference.pick.jsonObjectOrEmptyForTest().getValue("kind").jsonPrimitive.content)
        assertEquals("10", reference.pick.jsonObjectOrEmptyForTest().getValue("momentId").jsonPrimitive.content)
        rule.onNodeWithText("Back").performClick()
        rule.waitForIdle()

        rule.onNodeWithTag("quick-create-tasks-header").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("quick-create-completion-add-task").performScrollTo().assertIsDisplayed().performClick()
        rule.waitForIdle()
        assertEquals(2, store.state.value.plan.completion.root.children.size)
        rule.onNodeWithTag("quick-create-completion-add-relation").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("quick-create-completion-add-metric").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(4, store.state.value.plan.completion.root.children.size)
        rule.onNodeWithTag("quick-create-completion-clear").performScrollTo().performClick()
        assertEquals(0, store.state.value.plan.completion.root.kind)
        assertTrue(store.state.value.plan.completion.root.children.isEmpty())
    }
}

private fun kotlinx.serialization.json.JsonElement.jsonObjectOrEmptyForTest() = this as kotlinx.serialization.json.JsonObject
