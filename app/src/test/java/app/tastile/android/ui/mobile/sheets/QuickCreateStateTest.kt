package app.tastile.android.ui.mobile.sheets

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickCreateStateTest {

    @Test
    fun `draft preserves every v1 quick-create field`() {
        val populated = populatedDraft()
        val store = QuickCreateStateStore()

        store.updateIdentity(populated.identity)
        store.updatePlan(populated.plan)
        store.updateTime(populated.time)
        store.updateWindows(populated.windows)
        store.updateRecurring(populated.recurring)
        store.updateMeta(populated.meta)

        val draft = store.state.value
        assertEquals(populated.identity, draft.identity)
        assertEquals(populated.plan.references, draft.plan.references)
        assertEquals(populated.plan.completion, draft.plan.completion)
        assertEquals(populated.plan.planning, draft.plan.planning)
        assertEquals(populated.plan.metrics, draft.plan.metrics)
        assertEquals(populated.plan.decisions, draft.plan.decisions)
        assertEquals(populated.time, draft.time)
        assertEquals(populated.windows, draft.windows)
        assertEquals(populated.recurring, draft.recurring)
        assertEquals(populated.meta, draft.meta)
    }

    @Test
    fun `completion tree terms requirements and tasks retain full v1 shapes`() {
        val completion = populatedDraft().plan.completion

        assertEquals(2, completion.root?.kind)
        assertEquals(JsonPrimitive("not"), completion.root?.children?.single()?.term)
        assertEquals(JsonPrimitive("placement"), completion.timeRequirements.single().observation)
        assertEquals(JsonPrimitive(30), completion.timeRequirements.single().required)
        assertEquals(JsonPrimitive(60), completion.timeRequirements.single().preferred)
        assertEquals("Review", completion.tasks.single().content.title)
        assertEquals(JsonPrimitive(true), completion.tasks.single().show)
        assertEquals(JsonPrimitive("task-complete"), completion.tasks.single().complete?.term)
        assertEquals(buildJsonArray { add(JsonPrimitive("first")) }, completion.tasks.single().order)
    }

    @Test
    fun `updates can explicitly clear nullable fields`() {
        val initial = populatedDraft()
        val store = QuickCreateStateStore(initial)

        store.updateIdentity(initial.identity.copy(description = null, externalId = null))
        store.updateTime(initial.time.copy(referenceId = null))
        store.updateWindows(listOf(initial.windows.single().copy(referenceId = null)))
        store.updateRecurring(initial.recurring.copy(frameRules = listOf(initial.recurring.frameRules.single().copy(active = null))))
        store.updateMeta(initial.meta.copy(ownerSubjectId = null))

        assertNull(store.state.value.identity.description)
        assertNull(store.state.value.identity.externalId)
        assertNull(store.state.value.time.referenceId)
        assertNull(store.state.value.windows.single().referenceId)
        assertNull(store.state.value.recurring.frameRules.single().active)
        assertNull(store.state.value.meta.ownerSubjectId)
    }

    @Test
    fun `back and dismiss preserve a complete populated draft`() {
        val populated = populatedDraft()
        val store = QuickCreateStateStore(populated)

        store.openSubpanel(QuickCreatePanel.Recurring)
        store.backToBase()
        assertEquals(populated.copy(activePanel = QuickCreatePanel.Base), store.state.value)

        store.dismissPanel()
        assertEquals(populated.copy(activePanel = null), store.state.value)
    }

    private fun populatedDraft(): QuickCreateDraftState {
        val term = JsonPrimitive("not")
        val root = QuickCreateConditionNode(
            kind = 2,
            children = listOf(QuickCreateConditionNode(kind = 1, term = term)),
            term = JsonNull,
        )
        return QuickCreateDraftState(
            identity = QuickCreateIdentity(QuickCreateTileKind.Recurring, "Parity", "Description", "external", QuickCreateVisual("#123456", "rocket")),
            plan = QuickCreatePlan(
                role = QuickCreatePlanRole.Label,
                intent = QuickCreateIntent.MaximizeWithinInterval,
                references = listOf(QuickCreatePlanReference(JsonPrimitive("tile-1"), JsonPrimitive("selected"))),
                completion = QuickCreatePlanCompletion(
                    root = root,
                    timeRequirements = listOf(QuickCreateTimeRequirement("tr-1", JsonPrimitive("placement"), JsonPrimitive(30), JsonPrimitive(60))),
                    tasks = listOf(QuickCreateTaskDefinition("task-1", QuickCreateTaskContent("Review", "Approve PR"), JsonPrimitive(true), QuickCreateConditionNode(3, term = JsonPrimitive("task-complete")), buildJsonArray { add(JsonPrimitive("first")) })),
                ),
                planning = QuickCreatePlanning(buildJsonArray { add(JsonPrimitive("placement-rule")) }, buildJsonArray { add(JsonPrimitive("nesting-rule")) }, buildJsonArray { add(JsonPrimitive("flow")) }),
                metrics = buildJsonArray { add(JsonPrimitive("metric")) },
                decisions = buildJsonArray { add(JsonPrimitive("decision")) },
            ),
            time = QuickCreateTime(QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T11:00:00Z"), QuickCreateDurationRange(30, 90), QuickCreateWhenMode.Reference, QuickCreateTimeOfDayMode.Range, "09:00", "11:00", "calendar-1", "Focus"),
            windows = listOf(QuickCreateWindow("window-1", JsonPrimitive("owner"), 2, QuickCreateSpan("2026-07-16", "2026-07-17"), buildJsonArray { add(JsonPrimitive("rule")) }, "ref-1")),
            recurring = QuickCreateRecurring(
                life = QuickCreateRecurringLife(QuickCreateSpan("2026-07-16", "2026-08-16"), 1, QuickCreateChanged("2026-07-16T00:00:00Z", buildJsonObject { put("id", JsonPrimitive("self")) })),
                frameRules = listOf(QuickCreateFrameRule("frame-1", JsonPrimitive("active"), 4, buildJsonObject { put("Step", JsonPrimitive(1440)) })),
                rules = buildJsonArray { add(buildJsonObject { put("kind", JsonPrimitive("skip-holiday")) }) },
                repeatMode = QuickCreateRepeatMode.Weekly,
                weekdayMask = 31,
                endDate = "2026-08-16",
            ),
            meta = QuickCreateMeta("owner-1", listOf("parity", "mobile"), "memo"),
        )
    }
}
