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
    fun `recurrence mirrors Web controls and validation blocks invalid drafts`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }

        rule.onNodeWithTag("quick-create-row-4").performClick()
        rule.onNodeWithTag("quick-create-recurring-controls").assertIsDisplayed()

        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-0").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-title").performTextReplacement(" ")
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("quick-create-row-2").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-when-range").performClick()
        rule.onNodeWithTag("quick-create-start").performTextReplacement("2026-07-16T11:00:00Z")
        rule.onNodeWithTag("quick-create-end").performTextReplacement("2026-07-16T09:00:00Z")
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
        rule.onNodeWithTag("quick-create-when-day").performClick()
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
        rule.onNodeWithTag("quick-create-when-range").performClick()
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
        val moment = store.state.value.plan.completion.root.children.first().term!!.jsonObject
        assertTrue(moment["value"]?.jsonObject?.get("referenceId")?.jsonPrimitive?.content == "tile-1")
        assertTrue(moment["referenceId"] == null)
    }

    @Test
    fun `every exposed condition term keeps its v1 data inside value`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-6").performScrollTo().performClick()

        listOf("calendar", "moment", "relation", "gap", "requirement", "task", "fact", "metric", "life").forEach { kind ->
            rule.onNodeWithTag("condition-root-0-term-$kind").performScrollTo().performClick()
            val term = store.state.value.plan.completion.root.children.first().term!!.jsonObject
            assertTrue("$kind must have a value object", term["value"] is kotlinx.serialization.json.JsonObject)
            assertTrue("$kind must only expose kind and value", term.keys == setOf("kind", "value"))
            if (kind == "calendar") {
                assertTrue(term["value"]?.jsonObject?.get("holidayKind")?.jsonPrimitive?.content == "2")
            }
        }
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

    @Test
    fun `time requirement editor adds edits minimum minutes removes and retains state across panels`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }

        rule.onNodeWithTag("quick-create-row-6").performScrollTo().performClick()
        rule.onNodeWithTag("time-requirement-0-required-minutes").performScrollTo().performTextReplacement("45")
        rule.onNodeWithTag("quick-create-add-time-requirement").performScrollTo().performClick()
        val added = store.state.value.plan.completion.timeRequirements[1]
        assertTrue(added.observation.jsonObject["scope"]?.jsonPrimitive?.content == "0")
        assertTrue(added.required.jsonObject["minMs"]?.jsonPrimitive?.content == "1800000")
        rule.onNodeWithTag("time-requirement-1-remove").performScrollTo().performClick()

        rule.onNodeWithText("Back").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-row-2").performScrollTo().performClick()
        rule.onNodeWithText("Back").performScrollTo().performClick()
        rule.onNodeWithTag("quick-create-row-6").performScrollTo().performClick()

        val requirement = store.state.value.plan.completion.timeRequirements.single()
        val observation = requirement.observation.jsonObject
        val required = requirement.required.jsonObject
        assertTrue(observation["scope"]?.jsonPrimitive?.content == "1")
        assertTrue(required["minMs"]?.jsonPrimitive?.content == "2700000")
    }

    @Test
    fun `time panel mirrors Web conditional modes and clears only the Web none fields`() {
        val store = QuickCreateStateStore(
            QuickCreateDraftState(
                time = QuickCreateTime(
                    span = QuickCreateSpan("2026-07-16", "2026-07-17"),
                    timeOfDayMode = QuickCreateTimeOfDayMode.Range,
                    timeOfDayStart = "09:00",
                    timeOfDayEnd = "18:00",
                ),
            ),
        )
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-2").performClick()

        rule.onNodeWithTag("quick-create-when-none").assertIsDisplayed().performClick()
        assertTrue(store.state.value.time.whenMode == QuickCreateWhenMode.None)
        assertTrue(store.state.value.time.span == QuickCreateSpan())
        assertTrue(store.state.value.time.timeOfDayMode == QuickCreateTimeOfDayMode.Unspecified)
        assertTrue(rule.onAllNodesWithTag("quick-create-calendar").fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithTag("quick-create-time-of-day").fetchSemanticsNodes().isEmpty())

        rule.onNodeWithTag("quick-create-when-day").performClick()
        rule.onNodeWithTag("quick-create-calendar").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithTag("quick-create-reference-id").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("quick-create-when-reference").performClick()
        rule.onNodeWithTag("quick-create-reference-id").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithTag("quick-create-calendar").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `time range exposes range-only time controls and quick selections`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-2").performClick()
        rule.onNodeWithTag("quick-create-when-range").performClick()
        rule.onNodeWithTag("quick-create-time-of-day-range").performClick()
        rule.onNodeWithTag("quick-create-time-of-day-start").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-time-quick-midday").performClick()
        assertTrue(store.state.value.time.timeOfDayStart == "09:00")
        assertTrue(store.state.value.time.timeOfDayEnd == "18:00")
    }

    @Test
    fun `duration mirrors Web single bounded minutes control and none card`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-3").performClick()
        rule.onNodeWithTag("quick-create-duration-minutes").performTextReplacement("999")
        assertTrue(store.state.value.time.durationMinMax == QuickCreateDurationRange(720 * 60_000L, 720 * 60_000L))
        assertTrue(rule.onAllNodesWithTag("quick-create-duration-min").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("quick-create-duration-completion-link").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-duration-none").performClick()
        assertTrue(store.state.value.time.durationMinMax == QuickCreateDurationRange())
    }

    @Test
    fun `window mirrors Web conditional reference and hides internal v1 fields`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-2").performClick()
        rule.onNodeWithTag("quick-create-add-window").performClick()
        assertTrue(rule.onAllNodesWithTag("quick-create-window-0-reference").fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithTag("quick-create-window-id-0").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("quick-create-window-0-kind-2").performClick()
        rule.onNodeWithTag("quick-create-window-0-reference").performTextReplacement("parent-span")
        assertTrue(store.state.value.windows.single().kind == 2)
        assertTrue(store.state.value.windows.single().referenceId == "parent-span")
    }

    @Test
    fun `recurrence promotes tile kind and conditionally enables weekday and end controls`() {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store = store, onClose = {}) }
        rule.onNodeWithTag("quick-create-row-4").performClick()
        rule.onNodeWithTag("quick-create-repeat-daily").performClick()
        assertTrue(store.state.value.identity.kind == QuickCreateTileKind.Recurring)
        assertTrue(rule.onAllNodesWithTag("quick-create-weekday-0").fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithTag("quick-create-repeat-weekly").performClick()
        rule.onNodeWithTag("quick-create-weekday-0").performClick()
        assertTrue(store.state.value.recurring.weekdayMask and 1 == 0)
        assertTrue(rule.onAllNodesWithTag("quick-create-recurring-end-date").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("quick-create-recurring-end-switch").performClick()
        rule.onNodeWithTag("quick-create-recurring-end-date").assertIsDisplayed()
    }
}
