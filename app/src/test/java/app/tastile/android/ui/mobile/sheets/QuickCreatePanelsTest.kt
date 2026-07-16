package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreatePanelContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
        rule.onNodeWithText("Add condition").performScrollTo().performClick()
        assertTrue(store.state.value.plan.completion.root.children.isNotEmpty())
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

    @Test
    fun `calendar and moment condition fields retain typed values`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-6").performScrollTo().performClick()
        rule.onNodeWithText("Add condition").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertTrue(store.state.value.plan.completion.root.children.size == 2)
        assertTrue(store.state.value.plan.completion.root.children[1].term?.jsonObject?.get("kind")?.jsonPrimitive?.content == "calendar")
        rule.onNodeWithTag("condition-root-1-calendar-weekday-mask").performTextReplacement("31")
        rule.onNodeWithTag("condition-root-1-calendar-offset").performTextReplacement("15")
        rule.onNodeWithTag("condition-root-0-term-moment").performClick()
        rule.onNodeWithTag("condition-moment-reference").performTextReplacement("tile-1")
        rule.onNodeWithTag("condition-moment-offset").performTextReplacement("60000")
        assertTrue(store.state.value.plan.completion.root.children.first().term?.jsonObject?.get("kind")?.jsonPrimitive?.content == "moment")
        assertTrue(store.state.value.plan.completion.root.children.first().term?.jsonObject?.get("referenceId")?.jsonPrimitive?.content == "tile-1")
    }

    @Test
    fun `remaining Web condition terms retain their typed v1 values`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-6").performScrollTo().performClick()

        rule.onNodeWithTag("condition-root-0-term-requirement").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithTag("condition-root-0-requirement-id").performTextReplacement("required-focus")
        rule.onNodeWithTag("condition-root-0-requirement-state").performTextReplacement("1")
        val requirement = store.state.value.plan.completion.root.children.first().term!!.jsonObject
        assertTrue(requirement["value"]?.jsonObject?.get("requirementId")?.jsonPrimitive?.content == "required-focus")
        assertTrue(requirement["value"]?.jsonObject?.get("state")?.jsonPrimitive?.content == "1")
        rule.onNodeWithTag("condition-root-0-term-fact").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithTag("condition-root-0-fact-id").performTextReplacement("fact-energy")
        rule.onNodeWithTag("condition-root-0-fact-op").performTextReplacement("4")
        rule.onNodeWithTag("condition-root-0-fact-value").performTextReplacement("3.5")
        val fact = store.state.value.plan.completion.root.children.first().term!!.jsonObject
        assertTrue(fact["value"]?.jsonObject?.get("factId")?.jsonPrimitive?.content == "fact-energy")
        assertTrue(fact["value"]?.jsonObject?.get("value")?.jsonPrimitive?.content == "3.5")
        rule.onNodeWithTag("condition-root-0-term-metric").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithTag("condition-root-0-metric-id").performTextReplacement("metric-steps")
        rule.onNodeWithTag("condition-root-0-metric-value").performTextReplacement("10000")
        val metric = store.state.value.plan.completion.root.children.first().term!!.jsonObject
        assertTrue(metric["value"]?.jsonObject?.get("metricId")?.jsonPrimitive?.content == "metric-steps")
        assertTrue(metric["value"]?.jsonObject?.get("value")?.jsonPrimitive?.content == "10000")
        rule.onNodeWithTag("condition-root-0-term-life").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithTag("condition-root-0-life-target").performTextReplacement("tile-123")
        rule.onNodeWithTag("condition-root-0-life-state").performTextReplacement("2")

        val life = store.state.value.plan.completion.root.children.first().term!!.jsonObject
        assertTrue(life["kind"]?.jsonPrimitive?.content == "life")
        assertTrue(life["value"]?.jsonObject?.get("target")?.jsonPrimitive?.content == "tile-123")
        assertTrue(life["value"]?.jsonObject?.get("state")?.jsonPrimitive?.content == "2")

        rule.onNodeWithTag("condition-root-0-term-gap").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithTag("condition-root-0-gap-placeholder").assertIsDisplayed()
        val gap = store.state.value.plan.completion.root.children.first().term!!.jsonObject
        assertTrue(gap["value"]?.jsonObject?.get("leftAnchor")?.jsonObject?.containsKey("referenceId") == true)
        assertTrue(gap["value"]?.jsonObject?.get("size")?.jsonObject?.containsKey("minMs") == true)
    }
}
