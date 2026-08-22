package app.tastile.android.ui.mobile.sheets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invariant checks for [WORKFLOW_CONFIG] and [WORKFLOW_ORDER]. Mirrors the
 * web `tastile-web/src/features/create-tile/model/workflow-config.ts`
 * (4 peer workflows: Event / Task / Recurring / Detailed). If any of these
 * invariants break, the mobile QuickCreate base panel will dispatch into
 * the wrong workflow form, so the test fails loudly.
 */
class WorkflowConfigTest {

    @Test
    fun `all four workflow kinds are configured`() {
        assertEquals(4, WORKFLOW_CONFIG.size)
        assertNotNull(WORKFLOW_CONFIG[WorkflowKind.Event])
        assertNotNull(WORKFLOW_CONFIG[WorkflowKind.Task])
        assertNotNull(WORKFLOW_CONFIG[WorkflowKind.Recurring])
        assertNotNull(WORKFLOW_CONFIG[WorkflowKind.Detailed])
    }

    @Test
    fun `workflow order matches web workflow-config-ts`() {
        assertEquals(
            listOf(
                WorkflowKind.Event,
                WorkflowKind.Task,
                WorkflowKind.Recurring,
                WorkflowKind.Detailed,
            ),
            WORKFLOW_ORDER,
        )
    }

    @Test
    fun `event workflow forces placement kind and once repeat`() {
        val config = WORKFLOW_CONFIG.getValue(WorkflowKind.Event)
        assertEquals(QuickCreateTileKind.Placement, config.defaultTileKind)
        assertEquals(QuickCreatePlanRole.Executable, config.defaultPlanRole)
        assertEquals(QuickCreateRepeatMode.Once, config.defaultRepeatMode)
        assertEquals(QuickCreateWhenMode.Range, config.defaultTimeWhenMode)
        assertEquals(QuickCreateTimeOfDayMode.Range, config.defaultTimeOfDayMode)
    }

    @Test
    fun `task workflow forces placement kind and once repeat`() {
        val config = WORKFLOW_CONFIG.getValue(WorkflowKind.Task)
        assertEquals(QuickCreateTileKind.Placement, config.defaultTileKind)
        assertEquals(QuickCreatePlanRole.Executable, config.defaultPlanRole)
        assertEquals(QuickCreateRepeatMode.Once, config.defaultRepeatMode)
        assertEquals(QuickCreateWhenMode.Day, config.defaultTimeWhenMode)
    }

    @Test
    fun `recurring workflow forces recurring kind and daily repeat`() {
        val config = WORKFLOW_CONFIG.getValue(WorkflowKind.Recurring)
        assertEquals(QuickCreateTileKind.Recurring, config.defaultTileKind)
        assertEquals(QuickCreatePlanRole.Executable, config.defaultPlanRole)
        assertEquals(QuickCreateRepeatMode.Daily, config.defaultRepeatMode)
    }

    @Test
    fun `detailed workflow preserves placement and once defaults`() {
        val config = WORKFLOW_CONFIG.getValue(WorkflowKind.Detailed)
        assertEquals(QuickCreateTileKind.Placement, config.defaultTileKind)
        assertEquals(QuickCreatePlanRole.Executable, config.defaultPlanRole)
        assertEquals(QuickCreateRepeatMode.Once, config.defaultRepeatMode)
    }

    @Test
    fun `every workflow has non-zero label and description resource ids`() {
        WORKFLOW_CONFIG.values.forEach { config ->
            assertTrue("labelResId for ${config.kind} must be non-zero", config.labelResId != 0)
            assertTrue("descriptionResId for ${config.kind} must be non-zero", config.descriptionResId != 0)
            assertTrue("headingCreateResId for ${config.kind} must be non-zero", config.headingCreateResId != 0)
            assertTrue("headingEditResId for ${config.kind} must be non-zero", config.headingEditResId != 0)
        }
    }
}
