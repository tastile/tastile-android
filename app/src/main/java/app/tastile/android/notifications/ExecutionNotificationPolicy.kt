package app.tastile.android.notifications

import app.tastile.android.execution.ProjectedExecution
import kotlinx.datetime.Instant
import kotlin.math.max

enum class NotificationMilestone {
    WORK_GENTLE_NUDGE,
    WORK_INTERVENTION,
    BREAK_ENDED
}

data class ExecutionNotificationDecision(
    val milestone: NotificationMilestone?,
    val elapsedMinutes: Int,
    val targetMinutes: Int?,
    val statusTitle: String,
    val statusText: String,
    val milestoneKey: String?
)

object ExecutionNotificationPolicy {
    private const val WORK_GENTLE_THRESHOLD_MIN = 15
    private const val WORK_INTERVENTION_THRESHOLD_MIN = 25

    fun evaluate(
        execution: ProjectedExecution,
        now: Instant,
        emittedMilestones: Set<String>
    ): ExecutionNotificationDecision {
        val elapsedMinutes = max(0, ((now.toEpochMilliseconds() - execution.startedAt.toEpochMilliseconds()) / 60_000L).toInt())
        val targetMinutes = execution.targetMinutes
        val isBreak = execution.semanticRole == "break"
        val statusTitle = if (isBreak) "Break" else "Executing"
        val statusText = if (targetMinutes != null) {
            "${execution.tile.title}  ${elapsedMinutes}/${targetMinutes} min"
        } else {
            "${execution.tile.title}  ${elapsedMinutes} min"
        }

        val milestone = when {
            isBreak && targetMinutes != null && elapsedMinutes >= targetMinutes -> NotificationMilestone.BREAK_ENDED
            !isBreak && elapsedMinutes >= WORK_INTERVENTION_THRESHOLD_MIN -> NotificationMilestone.WORK_INTERVENTION
            !isBreak && elapsedMinutes >= WORK_GENTLE_THRESHOLD_MIN -> NotificationMilestone.WORK_GENTLE_NUDGE
            else -> null
        }
        val milestoneKey = milestone?.let { "${execution.segmentId}:${it.name}" }
        val filteredMilestone = milestone.takeUnless { milestoneKey != null && emittedMilestones.contains(milestoneKey) }

        return ExecutionNotificationDecision(
            milestone = filteredMilestone,
            elapsedMinutes = elapsedMinutes,
            targetMinutes = targetMinutes,
            statusTitle = statusTitle,
            statusText = statusText,
            milestoneKey = if (filteredMilestone != null) milestoneKey else null
        )
    }
}
