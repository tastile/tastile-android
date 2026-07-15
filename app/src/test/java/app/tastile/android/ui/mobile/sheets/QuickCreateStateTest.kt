package app.tastile.android.ui.mobile.sheets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class QuickCreateStateTest {

    @Test
    fun `draft preserves every web quick-create slice including condition trees and arrays`() {
        val store = QuickCreateStateStore()
        val condition = QuickCreateConditionNode.All(
            listOf(QuickCreateConditionNode.Term("task", "review", "completed"))
        )
        val identity = QuickCreateIdentity(
            kind = QuickCreateTileKind.Recurring,
            title = "Ship Android parity",
            description = "Match web",
            externalId = "external-1",
            visual = QuickCreateVisual("#123456", "rocket"),
        )
        val plan = QuickCreatePlan(
            role = QuickCreatePlanRole.Label,
            references = listOf(QuickCreatePlanReference("tile-1", "dependency")),
            completion = QuickCreatePlanCompletion(
                root = condition,
                timeRequirements = listOf(QuickCreateTimeRequirement("tr-1", 30, 90)),
                tasks = listOf(QuickCreateTaskDefinition("review", "Review", "Approve PR")),
            ),
        )
        val time = QuickCreateTime(
            span = QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T11:00:00Z"),
            durationMinMax = QuickCreateDurationRange(30, 90),
            whenMode = QuickCreateWhenMode.Reference,
            timeOfDayMode = QuickCreateTimeOfDayMode.Range,
            timeOfDayStart = "09:00",
            timeOfDayEnd = "11:00",
            referenceId = "calendar-1",
            referenceLabel = "Focus block",
        )
        val recurring = QuickCreateRecurring(
            life = QuickCreateRecurringLife("2026-07-16", "2026-08-16", QuickCreateRecurringLifecycleState.Active),
            frameRules = listOf(QuickCreateFrameRule("frame-1", 1440, 31)),
            rules = listOf("skip-holidays"),
            repeatMode = QuickCreateRepeatMode.Weekly,
            weekdayMask = 31,
            endDate = "2026-08-16",
        )

        store.updateIdentity(identity)
        store.updatePlan(plan)
        store.updateTime(time)
        store.updateWindows(listOf(QuickCreateWindow("weekday", 540, 660)))
        store.updateRecurring(recurring)
        store.updateMeta(QuickCreateMeta("owner-1", listOf("parity", "mobile"), "Do not lose this"))
        store.updateBehavior(QuickCreatePlanRole.Executable)

        val draft = store.state.value
        assertEquals(QuickCreateTileKind.Recurring, draft.identity.kind)
        assertEquals("#123456", draft.identity.visual.color)
        assertEquals(QuickCreatePlanRole.Executable, draft.plan.role)
        assertEquals(condition, draft.plan.completion.root)
        assertEquals("review", draft.plan.completion.tasks.single().id)
        assertEquals("2026-07-16T11:00:00Z", draft.time.span.end)
        assertEquals(90, draft.time.durationMinMax.maxMinutes)
        assertEquals("calendar-1", draft.time.referenceId)
        assertEquals("weekday", draft.windows.single().kind)
        assertEquals(31, draft.recurring.frameRules.single().weekdayMask)
        assertEquals("owner-1", draft.meta.ownerSubjectId)
        assertEquals(listOf("parity", "mobile"), draft.meta.tags)
    }

    @Test
    fun `updates can explicitly clear nullable fields`() {
        val store = QuickCreateStateStore(
            QuickCreateDraftState(
                identity = QuickCreateIdentity(description = "existing", externalId = "external"),
                time = QuickCreateTime(referenceId = "calendar-1", referenceLabel = "Focus"),
                meta = QuickCreateMeta(ownerSubjectId = "owner-1", memo = "existing"),
            )
        )

        store.updateIdentity(store.state.value.identity.copy(description = null, externalId = null))
        store.updateTime(store.state.value.time.copy(referenceId = null, referenceLabel = ""))
        store.updateMeta(store.state.value.meta.copy(ownerSubjectId = null, memo = ""))

        assertNull(store.state.value.identity.description)
        assertNull(store.state.value.identity.externalId)
        assertNull(store.state.value.time.referenceId)
        assertNull(store.state.value.meta.ownerSubjectId)
    }

    @Test
    fun `back and dismiss retain all edits while changing only panel navigation`() {
        val store = QuickCreateStateStore()
        store.updateIdentity(QuickCreateIdentity(title = "Persistent title"))
        store.updateWindows(listOf(QuickCreateWindow("weekday", 540, 660)))
        store.openSubpanel(QuickCreatePanel.Recurring)
        store.updateRecurring(QuickCreateRecurring(endDate = "2026-12-31"))

        store.backToBase()
        assertEquals(QuickCreatePanel.Base, store.state.value.activePanel)
        assertEquals("Persistent title", store.state.value.identity.title)
        assertFalse(store.state.value.windows.isEmpty())
        assertEquals("2026-12-31", store.state.value.recurring.endDate)

        store.dismissPanel()
        assertNull(store.state.value.activePanel)
        assertEquals("Persistent title", store.state.value.identity.title)
        assertEquals("2026-12-31", store.state.value.recurring.endDate)
    }
}
