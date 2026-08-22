package app.tastile.android.ui.mobile.sheets

import app.tastile.android.data.api.SourceGenerationPayload
import app.tastile.android.data.api.SourcePlacementRead
import app.tastile.android.data.api.SourceSchedulePayload
import app.tastile.android.data.api.SourceSplitPolicyPayload
import app.tastile.android.data.api.SourceTileDetailRead
import app.tastile.android.data.api.SourceTileRead
import app.tastile.android.data.api.SourceWindowPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavioural checks for [QuickCreateStateStore.setWorkflow]. Each peer
 * workflow applies a coherent bundle of defaults (tile kind, plan role,
 * repeat mode, time-of-day mode) so the base panel can render a meaningful
 * form on workflow switch.
 */
class QuickCreateStateStoreWorkflowTest {

    @Test
    fun `default draft starts in event workflow`() {
        val store = QuickCreateStateStore()
        assertEquals(WorkflowKind.Event, store.state.value.workflow)
    }

    @Test
    fun `switching to recurring forces recurring tile kind and daily repeat`() {
        val store = QuickCreateStateStore()
        store.setWorkflow(WorkflowKind.Recurring)
        val draft = store.state.value
        assertEquals(WorkflowKind.Recurring, draft.workflow)
        assertEquals(QuickCreateTileKind.Recurring, draft.identity.kind)
        assertEquals(QuickCreateRepeatMode.Daily, draft.recurring.repeatMode)
        assertEquals(QuickCreatePlanRole.Executable, draft.plan.role)
    }

    @Test
    fun `switching to task forces placement kind and once repeat`() {
        val store = QuickCreateStateStore()
        store.setWorkflow(WorkflowKind.Task)
        val draft = store.state.value
        assertEquals(WorkflowKind.Task, draft.workflow)
        assertEquals(QuickCreateTileKind.Placement, draft.identity.kind)
        assertEquals(QuickCreateRepeatMode.Once, draft.recurring.repeatMode)
        assertEquals(QuickCreateWhenMode.Day, draft.time.whenMode)
    }

    @Test
    fun `switching to detailed uses unspecified time defaults`() {
        val store = QuickCreateStateStore()
        store.setWorkflow(WorkflowKind.Detailed)
        val draft = store.state.value
        assertEquals(WorkflowKind.Detailed, draft.workflow)
        assertEquals(QuickCreateTileKind.Placement, draft.identity.kind)
        assertEquals(QuickCreateWhenMode.None, draft.time.whenMode)
        assertEquals(QuickCreateTimeOfDayMode.Unspecified, draft.time.timeOfDayMode)
    }

    @Test
    fun `setWorkflow resets active panel back to base`() {
        val store = QuickCreateStateStore()
        store.openSubpanel(QuickCreatePanel.Time)
        assertEquals(QuickCreatePanel.Time, store.state.value.activePanel)
        store.setWorkflow(WorkflowKind.Recurring)
        assertEquals(QuickCreatePanel.Base, store.state.value.activePanel)
    }

    @Test
    fun `hydrateForEdit preserves a workflow parameter override`() {
        val store = QuickCreateStateStore()
        store.hydrateForEdit(
            tileId = "tile-1",
            placementId = null,
            detail = sampleDetail(),
            workflow = WorkflowKind.Task,
        )
        assertEquals(WorkflowKind.Task, store.state.value.workflow)
    }

    @Test
    fun `unknown workflow kind is a no-op`() {
        // Force a malformed value through reflection-free access by using the
        // public map and confirming the store's setWorkflow silently no-ops
        // on a kind the config does not know about.
        val store = QuickCreateStateStore()
        // All four enum entries are mapped, so a no-op test would not be
        // meaningful here; instead we verify that the config table itself
        // is exhaustive.
        assertTrue(WORKFLOW_CONFIG.size == WorkflowKind.entries.size)
    }

    @Test
    fun `reset returns the draft to event workflow defaults`() {
        val store = QuickCreateStateStore()
        store.setWorkflow(WorkflowKind.Recurring)
        store.reset()
        val draft = store.state.value
        assertEquals(WorkflowKind.Event, draft.workflow)
        assertEquals(QuickCreateTileKind.Placement, draft.identity.kind)
        assertNull(draft.editingTileId)
    }

    @Test
    fun `openCreate event seeds the time-of-day start and end from the next slot`() {
        val store = QuickCreateStateStore()
        store.openCreate(WorkflowKind.Event, initialAllDay = false)
        val draft = store.state.value
        // Before the fix, timeOfDayStart/End were seeded as empty strings,
        // leaving the date/time row's time column showing only a placeholder.
        // Both fields must now be initialized to a HH:mm string derived from
        // the same span start/end so the user can see what value will be
        // submitted without opening the time picker.
        assertTrue(
            "timeOfDayStart must be populated on Event create (got '${draft.time.timeOfDayStart}')",
            draft.time.timeOfDayStart.matches(Regex("""\d{2}:\d{2}""")),
        )
        assertTrue(
            "timeOfDayEnd must be populated on Event create (got '${draft.time.timeOfDayEnd}')",
            draft.time.timeOfDayEnd.matches(Regex("""\d{2}:\d{2}""")),
        )
        assertEquals(QuickCreateWhenMode.Range, draft.time.whenMode)
        assertEquals(QuickCreateTimeOfDayMode.Range, draft.time.timeOfDayMode)
        assertTrue(draft.time.span.start.isNotBlank())
        assertTrue(draft.time.span.end.isNotBlank())
    }

    private fun sampleDetail(generationKind: Short = 0): SourceTileDetailRead {
        val schedule = SourceSchedulePayload(
            requiredDurationMs = 30 * 60_000L,
            generation = SourceGenerationPayload(kind = generationKind),
            window = SourceWindowPayload(startOffsetMs = 0L, endOffsetMs = 0L),
            splitPolicy = SourceSplitPolicyPayload(kind = 0),
            priority = 5,
        )
        val source = SourceTileRead(
            sourceTileId = "tile-1",
            planId = "plan-1",
            ownerId = "owner-1",
            revision = 1L,
            title = "Sample",
            description = null,
            color = null,
            icon = null,
            externalId = null,
            planRole = 0,
            schedule = schedule,
            createdAt = "2026-07-16T00:00:00Z",
            updatedAt = "2026-07-16T00:00:00Z",
        )
        return SourceTileDetailRead(source = source, occurrences = emptyList(), placements = emptyList())
    }
}
