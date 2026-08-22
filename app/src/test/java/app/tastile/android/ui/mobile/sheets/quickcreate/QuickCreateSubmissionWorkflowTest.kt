package app.tastile.android.ui.mobile.sheets.quickcreate

import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateSpan
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskContent
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskDefinition
import app.tastile.android.ui.mobile.sheets.WorkflowKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Workflow-aware validation and payload shape tests for
 * [quickCreateSubmissionValidation] and [QuickCreateSubmissionDispatcher].
 *
 * Mirrors the web `tastile-web/src/features/create-tile/ui/QuickCreateTask.tsx`
 * rule "Task workflow requires at least one task" (the wire submission
 * rejects an empty `tasks` array for task workflow).
 */
class QuickCreateSubmissionWorkflowTest {

    @Test
    fun `task workflow with no tasks fails validation`() {
        val draft = taskDraft(tasks = emptyList())
        val result = quickCreateSubmissionValidation(draft)
        assertFalse(result.isValid)
        assertNotNull(result.message)
        assertTrue(result.message!!.contains("task", ignoreCase = true))
    }

    @Test
    fun `task workflow with at least one task passes validation`() {
        val draft = taskDraft(tasks = listOf(simpleTask()))
        val result = quickCreateSubmissionValidation(draft)
        assertTrue(result.isValid)
    }

    @Test
    fun `event workflow does not require tasks`() {
        val draft = QuickCreateDraftState(
            workflow = WorkflowKind.Event,
            identity = QuickCreateDraftState().identity.copy(title = "Standup"),
            time = QuickCreateDraftState().time.copy(span = QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T09:30:00Z")),
        )
        val result = quickCreateSubmissionValidation(draft)
        assertTrue(result.isValid)
    }

    @Test
    fun `recurring workflow submission validates even with empty tasks`() {
        // The wire-side guard is on the dispatcher, not validation; for a
        // Recurring workflow an empty task list must still pass validation
        // (the recurring panel does not require tasks to be authored
        // before submission).
        val draft = QuickCreateDraftState(
            workflow = WorkflowKind.Recurring,
            identity = QuickCreateDraftState().identity.copy(title = "Daily"),
            time = QuickCreateDraftState().time.copy(span = QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T10:00:00Z")),
        )
        assertTrue(quickCreateSubmissionValidation(draft).isValid)
    }

    private fun taskDraft(tasks: List<QuickCreateTaskDefinition>): QuickCreateDraftState {
        val base = QuickCreateDraftState(workflow = WorkflowKind.Task)
        return base.copy(
            identity = base.identity.copy(title = "Submit report"),
            time = base.time.copy(span = QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T10:00:00Z")),
            plan = base.plan.copy(
                completion = base.plan.completion.copy(tasks = tasks),
            ),
        )
    }

    private fun simpleTask(): QuickCreateTaskDefinition = QuickCreateTaskDefinition(
        id = "task-1",
        content = QuickCreateTaskContent(title = "Draft"),
    )
}
