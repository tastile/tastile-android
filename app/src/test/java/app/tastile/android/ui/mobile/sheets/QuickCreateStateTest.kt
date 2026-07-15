package app.tastile.android.ui.mobile.sheets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class QuickCreateStateTest {

    @Test
    fun `draft stores every web-equivalent quick-create field`() {
        val store = QuickCreateStateStore()

        store.updateIdentity(title = "Ship Android parity", description = "Match web", color = "#123456", icon = "rocket", externalId = "external-1")
        store.updatePlan(role = QuickCreatePlanRole.Label, intent = QuickCreateIntent.MaximizeWithinInterval)
        store.updateTime(spanStart = "2026-07-16T09:00:00Z", spanEnd = "2026-07-16T11:00:00Z", allDay = false)
        store.updateDuration(minMinutes = 30, maxMinutes = 90)
        store.updateRecurring(
            enabled = true,
            activeFrom = "2026-07-16",
            activeUntil = "2026-08-16",
            windowStartMinute = 540,
            windowEndMinute = 660,
            frameRule = QuickCreateFrameRule(stepMinutes = 1440, weekdayMask = 31),
        )
        store.updateReferences(listOf(QuickCreateReference("tile-1", "Depends on", "dependency")))
        store.updateCompletion(QuickCreateCompletion(manualDescription = "Merged", taskTitles = listOf("Review")))
        store.updateMeta(project = "Android", tags = listOf("parity", "mobile"), memo = "Do not lose this")
        store.updateBehavior(QuickCreateBehavior(promptOnStart = true, promptOnEnd = false, breakSplitsWork = false))

        val draft = store.state.value
        assertEquals("Ship Android parity", draft.identity.title)
        assertEquals("#123456", draft.identity.visual.color)
        assertEquals(QuickCreatePlanRole.Label, draft.plan.role)
        assertEquals(QuickCreateIntent.MaximizeWithinInterval, draft.plan.intent)
        assertEquals("2026-07-16T11:00:00Z", draft.time.spanEnd)
        assertEquals(90, draft.duration.maxMinutes)
        assertEquals(31, draft.recurring.frameRule?.weekdayMask)
        assertEquals("dependency", draft.references.single().kind)
        assertEquals(listOf("Review"), draft.completion.taskTitles)
        assertEquals("Android", draft.meta.project)
        assertFalse(draft.behavior.breakSplitsWork)
    }

    @Test
    fun `returning from subpanel preserves draft edits and dismiss clears only navigation`() {
        val store = QuickCreateStateStore()
        store.updateIdentity(title = "Persistent title")
        store.openSubpanel(QuickCreatePanel.Recurring)
        store.updateRecurring(enabled = true, activeUntil = "2026-12-31")

        store.backToBase()
        assertEquals(QuickCreatePanel.Base, store.state.value.activePanel)
        assertEquals("Persistent title", store.state.value.identity.title)
        assertEquals("2026-12-31", store.state.value.recurring.activeUntil)

        store.dismissPanel()
        assertNull(store.state.value.activePanel)
        assertEquals("Persistent title", store.state.value.identity.title)
        assertEquals("2026-12-31", store.state.value.recurring.activeUntil)
    }
}
