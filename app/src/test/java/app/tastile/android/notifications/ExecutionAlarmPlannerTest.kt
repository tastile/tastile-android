package app.tastile.android.notifications

import app.tastile.android.core.CorePromptQueueItem
import app.tastile.android.core.CoreSnapshot
import app.tastile.android.core.CoreTileSnapshot
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionAlarmPlannerTest {

    @Test
    fun plan_schedulesPromptAlarmForActivePhaseEnd() {
        val alarms = ExecutionAlarmPlanner.plan(
            snapshot = CoreSnapshot(
                revision = 3,
                activeTileId = "tile-1",
                phaseKind = "work",
                phaseEndsAt = "2026-03-27T01:15:00Z",
                tiles = listOf(
                    CoreTileSnapshot(
                        id = "tile-1",
                        title = "Deep work",
                        lifecycle = "Started"
                    )
                )
            ),
            now = Instant.parse("2026-03-27T01:05:00Z")
        )

        assertEquals(listOf(AlarmTriggerType.PROMPT), alarms.map { it.type })
        assertEquals("2026-03-27T01:15:00Z", alarms.single().triggerAt.toString())
        assertEquals("tile-1", alarms.single().tileId)
    }

    @Test
    fun plan_skipsPhaseEndAlarmWhenPromptAlreadyPending() {
        val alarms = ExecutionAlarmPlanner.plan(
            snapshot = CoreSnapshot(
                revision = 4,
                activeTileId = "tile-1",
                phaseKind = "work",
                phaseEndsAt = "2026-03-27T01:15:00Z",
                tiles = listOf(
                    CoreTileSnapshot(
                        id = "tile-1",
                        title = "Deep work",
                        lifecycle = "Started"
                    )
                ),
                promptQueue = listOf(
                    CorePromptQueueItem(
                        promptId = "prompt-1",
                        tileId = "tile-1",
                        kind = "end_tile",
                        severity = "critical",
                        scheduledAt = "2026-03-27T01:15:00Z",
                        reason = "work_phase_expired",
                        status = "pending"
                    )
                )
            ),
            now = Instant.parse("2026-03-27T01:05:00Z")
        )

        assertTrue(alarms.isEmpty())
    }

    @Test
    fun plan_schedulesFixedStartForReadyTile() {
        val alarms = ExecutionAlarmPlanner.plan(
            snapshot = CoreSnapshot(
                revision = 5,
                tiles = listOf(
                    CoreTileSnapshot(
                        id = "tile-scheduled",
                        title = "Morning review",
                        lifecycle = "Ready",
                        fixedStartAt = "2026-03-27T03:00:00Z"
                    )
                )
            ),
            now = Instant.parse("2026-03-27T02:00:00Z")
        )

        assertEquals(listOf(AlarmTriggerType.FIXED_START), alarms.map { it.type })
        assertEquals("2026-03-27T03:00:00Z", alarms.single().triggerAt.toString())
        assertEquals("tile-scheduled", alarms.single().tileId)
    }
}
