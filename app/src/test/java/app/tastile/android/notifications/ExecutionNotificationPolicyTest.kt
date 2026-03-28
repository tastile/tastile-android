package app.tastile.android.notifications

import app.tastile.android.execution.ProjectedExecution
import app.tastile.android.execution.ProjectedTile
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExecutionNotificationPolicyTest {

    @Test
    fun evaluate_returnsGentleNudgeAfter15MinutesOfWork() {
        val decision = ExecutionNotificationPolicy.evaluate(
            execution = activeExecution(
                semanticRole = "work",
                targetMinutes = 25,
                startedAt = "2026-03-27T01:00:00Z"
            ),
            now = Instant.parse("2026-03-27T01:16:00Z"),
            emittedMilestones = emptySet()
        )

        assertEquals(NotificationMilestone.WORK_GENTLE_NUDGE, decision.milestone)
    }

    @Test
    fun evaluate_returnsInterventionAfter25MinutesOfWork() {
        val decision = ExecutionNotificationPolicy.evaluate(
            execution = activeExecution(
                semanticRole = "work",
                targetMinutes = 25,
                startedAt = "2026-03-27T01:00:00Z"
            ),
            now = Instant.parse("2026-03-27T01:26:00Z"),
            emittedMilestones = emptySet()
        )

        assertEquals(NotificationMilestone.WORK_INTERVENTION, decision.milestone)
    }

    @Test
    fun evaluate_returnsBreakEndedWhenRestTargetReached() {
        val decision = ExecutionNotificationPolicy.evaluate(
            execution = activeExecution(
                semanticRole = "break",
                targetMinutes = 5,
                startedAt = "2026-03-27T01:00:00Z"
            ),
            now = Instant.parse("2026-03-27T01:06:00Z"),
            emittedMilestones = emptySet()
        )

        assertEquals(NotificationMilestone.BREAK_ENDED, decision.milestone)
    }

    @Test
    fun evaluate_suppressesAlreadyEmittedMilestone() {
        val decision = ExecutionNotificationPolicy.evaluate(
            execution = activeExecution(
                semanticRole = "work",
                targetMinutes = 25,
                startedAt = "2026-03-27T01:00:00Z"
            ),
            now = Instant.parse("2026-03-27T01:26:00Z"),
            emittedMilestones = setOf("segment-1:${NotificationMilestone.WORK_INTERVENTION.name}")
        )

        assertNull(decision.milestone)
    }

    private fun activeExecution(
        semanticRole: String,
        targetMinutes: Int?,
        startedAt: String
    ): ProjectedExecution {
        return ProjectedExecution(
            tile = ProjectedTile(
                id = "tile-1",
                title = if (semanticRole == "break") "Break (5min)" else "Deep work",
                lifecycle = "Started",
                semanticRole = semanticRole,
                targetWorkMin = if (semanticRole == "work") targetMinutes else null,
                targetRestMin = if (semanticRole == "break") targetMinutes else null
            ),
            segmentId = "segment-1",
            startedAt = Instant.parse(startedAt),
            semanticRole = semanticRole,
            targetMinutes = targetMinutes
        )
    }
}
