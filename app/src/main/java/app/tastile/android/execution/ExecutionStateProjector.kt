package app.tastile.android.execution

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.EventRow
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ProjectedTile(
    val id: String,
    val title: String,
    val lifecycle: String,
    val nextAction: String? = null,
    val doneDefinition: String? = null,
    val semanticRole: String = "work",
    val targetWorkMin: Int? = null,
    val targetRestMin: Int? = null,
    val temporalConditions: JsonObject? = null,
    val objectiveConditions: JsonObject? = null,
    val interruptionConditions: JsonObject? = null,
    val automationConditions: JsonObject? = null,
    val annotationConditions: JsonObject? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class ProjectedExecution(
    val tile: ProjectedTile,
    val segmentId: String,
    val startedAt: Instant,
    val semanticRole: String,
    val targetMinutes: Int?
)

data class ExecutionProjection(
    val tiles: List<ProjectedTile>,
    val activeTile: ProjectedTile?,
    val activeExecution: ProjectedExecution?,
    val openSegmentIdsByTileId: Map<String, String>
)

object ExecutionStateProjector {

    fun project(rows: List<EventRow>): ExecutionProjection {
        val tiles = linkedMapOf<String, ProjectedTile>()
        val openSegments = linkedMapOf<String, OpenSegment>()
        var activeTileId: String? = null

        for (row in rows) {
            val payload = row.eventPayload ?: row.payloadJson ?: continue
            when (row.eventType) {
                "tile_created" -> {
                    parseTileFromPayload(payload, row.occurredAt)?.let { tiles[it.id] = it }
                }
                "tile_started" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    val tile = tiles[tileId] ?: continue
                    tiles[tileId] = tile.copy(
                        lifecycle = TileLifecycle.STARTED.value,
                        updatedAt = payload.string("started_at") ?: row.occurredAt
                    )
                    activeTileId = tileId
                }
                "segment_started" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    val segmentId = payload.string("segment_id") ?: continue
                    val startedAt = payload.string("started_at") ?: row.occurredAt
                    openSegments[tileId] = OpenSegment(segmentId = segmentId, startedAt = startedAt)
                }
                "segment_ended" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    openSegments.remove(tileId)
                }
                "tile_completed" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    val tile = tiles[tileId] ?: continue
                    tiles[tileId] = tile.copy(
                        lifecycle = TileLifecycle.DONE.value,
                        updatedAt = payload.string("completed_at") ?: row.occurredAt
                    )
                    openSegments.remove(tileId)
                    if (activeTileId == tileId) activeTileId = null
                }
                "tile_deferred" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    val tile = tiles[tileId] ?: continue
                    tiles[tileId] = tile.copy(
                        lifecycle = TileLifecycle.READY.value,
                        updatedAt = payload.string("deferred_at") ?: row.occurredAt
                    )
                    openSegments.remove(tileId)
                    if (activeTileId == tileId) activeTileId = null
                }
                "tile_deleted" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    tiles.remove(tileId)
                    openSegments.remove(tileId)
                    if (activeTileId == tileId) activeTileId = null
                }
            }
        }

        val activeTile = activeTileId?.let(tiles::get)
        val activeExecution = activeTile?.let { tile ->
            val openSegment = openSegments[tile.id] ?: return@let null
            val startedAt = runCatching { Instant.parse(openSegment.startedAt) }.getOrNull() ?: return@let null
            ProjectedExecution(
                tile = tile,
                segmentId = openSegment.segmentId,
                startedAt = startedAt,
                semanticRole = tile.semanticRole,
                targetMinutes = if (tile.semanticRole == "break") tile.targetRestMin else tile.targetWorkMin
            )
        }

        return ExecutionProjection(
            tiles = tiles.values.toList(),
            activeTile = activeTile,
            activeExecution = activeExecution,
            openSegmentIdsByTileId = openSegments.mapValues { it.value.segmentId }
        )
    }

    private fun parseTileFromPayload(payload: JsonObject, occurredAt: String): ProjectedTile? {
        val tile = payload.obj("tile") ?: return null
        val core = tile.obj("core")
        val annotation = tile.obj("annotation")
        val objective = tile.obj("objective")
        val temporal = tile.obj("temporal")
        val interruption = tile.obj("interruption")
        val automation = tile.obj("automation")
        val work = tile.obj("work")

        val tileId = core?.string("id") ?: return null
        val title = core.string("title") ?: return null
        val semanticRole = annotation?.string("semanticRole") ?: "work"
        val labels = annotation?.array("labels") ?: JsonArray(emptyList())
        val timedLabels = annotation?.array("timedLabels") ?: JsonArray(emptyList())
        val segments = work?.array("segments") ?: JsonArray(emptyList())

        return ProjectedTile(
            id = tileId,
            title = title,
            lifecycle = TileLifecycle.READY.value,
            nextAction = core.string("nextAction"),
            doneDefinition = core.string("doneDefinition"),
            semanticRole = semanticRole,
            targetWorkMin = objective?.int("targetWorkMin"),
            targetRestMin = objective?.int("targetRestMin"),
            temporalConditions = temporal,
            objectiveConditions = objective,
            interruptionConditions = interruption,
            automationConditions = automation,
            annotationConditions = buildJsonObject {
                put("semanticRole", JsonPrimitive(semanticRole))
                put("labels", labels)
                put("timedLabels", timedLabels)
                put("segments", segments)
            },
            createdAt = occurredAt,
            updatedAt = occurredAt
        )
    }
}

fun ProjectedTile.toTile(): Tile {
    return Tile(
        id = id,
        userId = "",
        localTileId = id,
        title = title,
        nextAction = nextAction,
        doneDefinition = doneDefinition,
        temporalConditions = temporalConditions,
        objectiveConditions = objectiveConditions,
        interruptionConditions = interruptionConditions,
        automationConditions = automationConditions,
        lifecycle = lifecycle,
        annotationConditions = annotationConditions,
        createdAt = createdAt,
        updatedAt = updatedAt,
        localCreatedAt = createdAt,
        localUpdatedAt = updatedAt,
        deletedAt = null
    )
}

private data class OpenSegment(
    val segmentId: String,
    val startedAt: String
)

private fun JsonObject?.string(key: String): String? = this?.get(key)?.jsonPrimitive?.contentOrNull

private fun JsonObject?.int(key: String): Int? = this?.get(key)?.jsonPrimitive?.contentOrNull?.toIntOrNull()

private fun JsonObject?.obj(key: String): JsonObject? = this?.get(key) as? JsonObject

private fun JsonObject?.array(key: String): JsonArray? = this?.get(key) as? JsonArray
