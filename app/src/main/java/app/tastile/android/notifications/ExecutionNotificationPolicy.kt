package app.tastile.android.notifications

import app.tastile.android.execution.ProjectedExecution
import kotlinx.datetime.Instant
import kotlin.math.max

enum class NotificationMilestone {
    WORK_GENTLE_NUDGE,
    WORK_INTERVENTION,
    BREAK_ENDED
}

/**
 * Status surface produced by [ExecutionNotificationPolicy.evaluate]. The two
 * text fields are raw English tokens ("Break" / "Executing") and a numeric
 * template — consumers (notification renderers / Compose UI) MUST resolve
 * them via `stringResource(...)` against
 * `system_notifications.xml::notif_status_*` so locale switching flows
 * through the standard Android resource pipeline.
 */
data class ExecutionNotificationDecision(
    val milestone: NotificationMilestone?,
    val elapsedMinutes: Int,
    val targetMinutes: Int?,
    /**
     * Token discriminator for the status title. Render as
     * `stringResource(if (statusRole == StatusRole.BREAK) R.string.notif_status_break else R.string.notif_status_executing)`.
     */
    val statusRole: StatusRole,
    val statusText: String,
    val milestoneKey: String?
)

enum class StatusRole { BREAK, EXECUTING }

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
        val statusRole = if (isBreak) StatusRole.BREAK else StatusRole.EXECUTING
        // TODO(i18n-adr-0003): replace with `context.getString(R.string.notif_status_with_target, …)`
        // once [ExecutionNotificationPolicy.evaluate] takes a Context. Today this formatter runs in
        // a pure-data layer with no Context; plumbing it requires updating the four callers in
        // [ExecutionNotificationPolicyTest] (test/** is WIP-protected by the parent ADR, so this
        // is deferred). The exposed `statusText` is meant to be re-rendered by the notification
        // renderer with [StatusRole] as the key.
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
            statusRole = statusRole,
            statusText = statusText,
            milestoneKey = if (filteredMilestone != null) milestoneKey else null
        )
    }
}
