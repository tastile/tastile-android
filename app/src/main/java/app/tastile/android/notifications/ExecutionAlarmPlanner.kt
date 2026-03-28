package app.tastile.android.notifications

import app.tastile.android.core.CoreSnapshot
import app.tastile.android.core.CoreTileSnapshot
import kotlinx.datetime.Instant

enum class AlarmTriggerType {
    PROMPT,
    FIXED_START
}

data class ScheduledAlarmSpec(
    val id: String,
    val type: AlarmTriggerType,
    val triggerAt: Instant,
    val tileId: String,
    val tileTitle: String
)

object ExecutionAlarmPlanner {
    private const val MAX_ALARMS = 32

    fun plan(
        snapshot: CoreSnapshot,
        now: Instant
    ): List<ScheduledAlarmSpec> {
        val alarms = buildList {
            planPhaseEnd(snapshot, now)?.let(::add)
            snapshot.tiles
                .asSequence()
                .filter { it.lifecycle == "Ready" }
                .filter { it.fixedStartAt != null }
                .mapNotNull { tile ->
                    val fixedStart = runCatching { Instant.parse(tile.fixedStartAt!!) }.getOrNull()
                        ?: return@mapNotNull null
                    if (fixedStart <= now) return@mapNotNull null
                    ScheduledAlarmSpec(
                        id = "fixed-start:${tile.id}:$fixedStart",
                        type = AlarmTriggerType.FIXED_START,
                        triggerAt = fixedStart,
                        tileId = tile.id,
                        tileTitle = tile.title
                    )
                }
                .forEach(::add)
        }

        return alarms
            .sortedBy { it.triggerAt.toEpochMilliseconds() }
            .take(MAX_ALARMS)
    }

    private fun planPhaseEnd(snapshot: CoreSnapshot, now: Instant): ScheduledAlarmSpec? {
        if (snapshot.promptQueue.isNotEmpty()) return null

        val activeTileId = snapshot.activeTileId ?: return null
        val phaseEndsAt = snapshot.phaseEndsAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        if (phaseEndsAt <= now) return null
        val tile = snapshot.tiles.firstOrNull { it.id == activeTileId } ?: return null
        return ScheduledAlarmSpec(
            id = "phase-end:${tile.id}:$phaseEndsAt",
            type = AlarmTriggerType.PROMPT,
            triggerAt = phaseEndsAt,
            tileId = tile.id,
            tileTitle = tile.title
        )
    }
}
