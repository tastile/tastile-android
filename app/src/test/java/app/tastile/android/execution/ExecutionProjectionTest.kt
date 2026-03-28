package app.tastile.android.execution

import app.tastile.android.data.repository.EventRow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExecutionProjectionTest {

    @Test
    fun project_reconstructsActiveWorkExecutionFromEvents() {
        val projection = ExecutionStateProjector.project(
            rows = listOf(
                tileCreatedEvent(
                    tileId = "tile-1",
                    title = "Deep work",
                    semanticRole = "work",
                    targetWorkMin = 25,
                    targetRestMin = null
                ),
                tileStartedEvent("tile-1"),
                segmentStartedEvent(
                    tileId = "tile-1",
                    segmentId = "segment-1",
                    startedAt = "2026-03-27T01:00:00Z"
                )
            )
        )

        assertEquals("tile-1", projection.activeTile?.id)
        assertEquals("Deep work", projection.activeTile?.title)
        assertEquals("work", projection.activeExecution?.semanticRole)
        assertEquals(25, projection.activeExecution?.targetMinutes)
        assertEquals("segment-1", projection.activeExecution?.segmentId)
    }

    @Test
    fun project_usesBreakRoleAndRestTargetForBreakTile() {
        val projection = ExecutionStateProjector.project(
            rows = listOf(
                tileCreatedEvent(
                    tileId = "tile-break",
                    title = "Break (5min)",
                    semanticRole = "break",
                    targetWorkMin = null,
                    targetRestMin = 5
                ),
                tileStartedEvent("tile-break"),
                segmentStartedEvent(
                    tileId = "tile-break",
                    segmentId = "segment-break",
                    startedAt = "2026-03-27T02:00:00Z"
                )
            )
        )

        assertEquals("break", projection.activeExecution?.semanticRole)
        assertEquals(5, projection.activeExecution?.targetMinutes)
    }

    @Test
    fun project_clearsActiveExecutionAfterCompletion() {
        val projection = ExecutionStateProjector.project(
            rows = listOf(
                tileCreatedEvent(
                    tileId = "tile-1",
                    title = "Deep work",
                    semanticRole = "work",
                    targetWorkMin = 25,
                    targetRestMin = null
                ),
                tileStartedEvent("tile-1"),
                segmentStartedEvent(
                    tileId = "tile-1",
                    segmentId = "segment-1",
                    startedAt = "2026-03-27T01:00:00Z"
                ),
                segmentEndedEvent(
                    tileId = "tile-1",
                    segmentId = "segment-1",
                    endedAt = "2026-03-27T01:25:00Z"
                ),
                tileCompletedEvent("tile-1")
            )
        )

        assertNull(projection.activeTile)
        assertNull(projection.activeExecution)
    }

    @Test
    fun project_keepsProjectedTileMetadataAvailable() {
        val projection = ExecutionStateProjector.project(
            rows = listOf(
                tileCreatedEvent(
                    tileId = "tile-1",
                    title = "Deep work",
                    semanticRole = "work",
                    targetWorkMin = 25,
                    targetRestMin = null
                )
            )
        )

        val tile = projection.tiles.firstOrNull()
        assertNotNull(tile)
        assertEquals("Deep work", tile?.title)
        assertEquals("Ready", tile?.lifecycle)
    }

    private fun tileCreatedEvent(
        tileId: String,
        title: String,
        semanticRole: String,
        targetWorkMin: Int?,
        targetRestMin: Int?
    ): EventRow {
        val payload = buildJsonObject {
            put("tile", buildJsonObject {
                put("core", buildJsonObject {
                    put("id", JsonPrimitive(tileId))
                    put("title", JsonPrimitive(title))
                })
                put("objective", buildJsonObject {
                    if (targetWorkMin != null) put("targetWorkMin", JsonPrimitive(targetWorkMin))
                    if (targetRestMin != null) put("targetRestMin", JsonPrimitive(targetRestMin))
                })
                put("annotation", buildJsonObject {
                    put("semanticRole", JsonPrimitive(semanticRole))
                })
                put("work", buildJsonObject {})
                put("temporal", buildJsonObject {})
                put("interruption", buildJsonObject {})
                put("automation", buildJsonObject {})
            })
        }
        return eventRow("tile_created", payload)
    }

    private fun tileStartedEvent(tileId: String): EventRow = eventRow(
        "tile_started",
        buildJsonObject { put("tile_id", JsonPrimitive(tileId)) }
    )

    private fun tileCompletedEvent(tileId: String): EventRow = eventRow(
        "tile_completed",
        buildJsonObject { put("tile_id", JsonPrimitive(tileId)) }
    )

    private fun segmentStartedEvent(tileId: String, segmentId: String, startedAt: String): EventRow = eventRow(
        "segment_started",
        buildJsonObject {
            put("tile_id", JsonPrimitive(tileId))
            put("segment_id", JsonPrimitive(segmentId))
            put("mode", JsonPrimitive("work"))
            put("started_at", JsonPrimitive(startedAt))
        }
    )

    private fun segmentEndedEvent(tileId: String, segmentId: String, endedAt: String): EventRow = eventRow(
        "segment_ended",
        buildJsonObject {
            put("tile_id", JsonPrimitive(tileId))
            put("segment_id", JsonPrimitive(segmentId))
            put("ended_at", JsonPrimitive(endedAt))
        }
    )

    private fun eventRow(eventType: String, payload: JsonObject): EventRow {
        return EventRow(
            eventId = "$eventType-id",
            aggregateId = "aggregate",
            eventType = eventType,
            eventPayload = payload,
            payloadJson = payload,
            occurredAt = "2026-03-27T01:00:00Z",
            actorType = "human",
            actorId = "self",
            sequenceNumber = 1
        )
    }
}
