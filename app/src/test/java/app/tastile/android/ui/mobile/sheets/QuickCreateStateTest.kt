package app.tastile.android.ui.mobile.sheets

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class QuickCreateStateTest {

    @Test
    fun `web defaults use a non-vacuous completion and timestamped recurring life`() {
        val draft = QuickCreateDraftState()

        assertEquals(0, draft.plan.completion.root.kind)
        assertEquals(1, draft.plan.completion.root.children.size)
        assertEquals(3, draft.plan.completion.root.children.single().kind)
        assertFalse(draft.plan.completion.timeRequirements.single().id.isBlank())
        assertEquals("task_default", draft.plan.completion.tasks.single().id)
        assertEquals(3, draft.plan.completion.tasks.single().complete.kind)
        assertEquals(30 * 60_000L, draft.time.durationMinMax.minMs)
        assertEquals(90 * 60_000L, draft.time.durationMinMax.maxMs)
        assertEquals(QuickCreateDateRange("", ""), draft.recurring.life.active)
        assertFalse(draft.recurring.life.changed.at.isBlank())
        assertEquals("", draft.recurring.endDate)
        Instant.parse(draft.recurring.life.changed.at)
    }

    @Test
    fun `default time requirement receives a fresh id for each draft`() {
        val first = QuickCreateDraftState()
        val second = QuickCreateDraftState()

        assertFalse(first.plan.completion.timeRequirements.single().id == second.plan.completion.timeRequirements.single().id)
    }

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
        assertEquals("owner", draft.windows.single().owner)
        assertEquals("rule-1", draft.windows.single().rules.single().id)
        assertEquals(populated.plan.completion.root, draft.windows.single().rules.single().`when`)
        assertEquals(populated.recurring, draft.recurring)
        assertEquals("frame-1", draft.recurring.frameRules.single().id)
        assertEquals("step", draft.recurring.frameRules.single().generator.kind)
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
                references = listOf(QuickCreatePlanReference("reference-1", JsonPrimitive("tile-1"), JsonPrimitive("selected"))),
                completion = QuickCreatePlanCompletion(
                    root = root,
                    timeRequirements = listOf(QuickCreateTimeRequirement("tr-1", JsonPrimitive("placement"), JsonPrimitive(30), JsonPrimitive(60))),
                    tasks = listOf(QuickCreateTaskDefinition("task-1", QuickCreateTaskContent("Review", "Approve PR"), JsonPrimitive(true), QuickCreateConditionNode(3, term = JsonPrimitive("task-complete")), buildJsonArray { add(JsonPrimitive("first")) })),
                ),
                planning = QuickCreatePlanning(buildJsonArray { add(JsonPrimitive("placement-rule")) }, buildJsonArray { add(JsonPrimitive("nesting-rule")) }, buildJsonArray { add(JsonPrimitive("flow")) }),
                metrics = buildJsonArray { add(JsonPrimitive("metric")) },
                decisions = buildJsonArray { add(JsonPrimitive("decision")) },
            ),
            time = QuickCreateTime(QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T11:00:00Z"), QuickCreateDurationRange(30_000, 90_000), QuickCreateWhenMode.Reference, QuickCreateTimeOfDayMode.Range, "09:00", "11:00", "calendar-1", "Focus"),
            windows = listOf(QuickCreateWindow("window-1", "owner", 2, QuickCreateSpan("2026-07-16", "2026-07-17"), listOf(QuickCreateWindowRule("rule-1", 31, "09:00", "11:00", 2, QuickCreateDateRange("2026-07-16", "2026-07-17"), root)), "ref-1")),
            recurring = QuickCreateRecurring(
                life = QuickCreateRecurringLife(QuickCreateDateRange("2026-07-16", "2026-08-16"), 1, QuickCreateChanged("2026-07-16T00:00:00Z", QuickCreateActor("self", 0, null))),
                frameRules = listOf(QuickCreateFrameRule("frame-1", QuickCreateFrameGenerator("step", buildJsonObject { put("step", JsonPrimitive(1440)) }), root)),
                rules = listOf(QuickCreateRecurringRule("recurring-rule-1", root, 1, buildJsonArray { add(buildJsonObject { put("kind", JsonPrimitive("skip-holiday")) }) })),
                repeatMode = QuickCreateRepeatMode.Weekly,
                weekdayMask = 31,
                endDate = "2026-08-16",
            ),
            meta = QuickCreateMeta("owner-1", listOf("parity", "mobile"), "memo"),
        )
    }
}
