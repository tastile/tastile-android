package app.tastile.android.data.repository

import app.tastile.android.core.CoreBridgeError
import app.tastile.android.core.CoreCommandRequest
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.core.CoreSnapshot
import app.tastile.android.core.CoreTileSnapshot
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileRepository @Inject constructor(
    private val client: SupabaseClient,
    private val coreRuntimeService: CoreRuntimeService,
    private val executionNotificationCoordinator: ExecutionNotificationCoordinator,
    private val eventRepository: EventRepository,
    private val currentUserProvider: CurrentUserProvider
) : PromptTileRepository, MemoTileRepository {
    companion object {
        private const val TABLE_TILES = "tiles"
        private const val COMMAND_TILE_CREATE = "tile.create"
        private const val COMMAND_TILE_START = "tile.start"
        private const val COMMAND_TILE_COMPLETE = "tile.complete"
        private const val COMMAND_TILE_DELETE = "tile.delete"
        private const val COMMAND_TILE_PAUSE = "tile.pause"
        private const val COMMAND_TILE_CONTINUE = "tile.continue"
        private const val COMMAND_MEMO_ATTACH = "memo.attach"
    }

    suspend fun getTiles(userId: String): List<Tile> {
        projectedSnapshotTiles()?.let { return it }

        return client.from(TABLE_TILES)
            .select {
                filter {
                    eq("user_id", userId)
                    exact("deleted_at", null)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Tile>()
    }

    suspend fun createTile(userId: String, title: String): Tile {
        return createTile(
            userId = userId,
            payload = buildJsonObject {
                put("title", JsonPrimitive(title.trim()))
                put("next_action", JsonNull)
                put("done_definition", JsonNull)
            }
        )
    }

    suspend fun createTile(userId: String, payload: kotlinx.serialization.json.JsonObject): Tile {
        val trimmedTitle = payload["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val snapshotBefore = currentSnapshotOrNull()
        val existingIds = snapshotBefore?.tiles?.mapTo(mutableSetOf()) { it.id } ?: mutableSetOf()

        val ack = tryApplyCoreCommand(COMMAND_TILE_CREATE, payload)
        if (ack != null) {
            persistEmittedEvents(userId, ack)
            currentSnapshotOrNull()
                ?.tiles
                ?.firstOrNull { it.id !in existingIds || it.title == trimmedTitle }
                ?.let { return it.toTile() }
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        val tile = Tile(
            userId = userId,
            localTileId = UUID.randomUUID().toString(),
            title = trimmedTitle,
            lifecycle = "Ready",
            localCreatedAt = now,
            localUpdatedAt = now
        )
        
        return client.from(TABLE_TILES)
            .insert(tile) {
                select()
            }
            .decodeSingle<Tile>()
    }

    suspend fun startTile(tileId: String): Tile {
        val ack = tryApplyCoreCommand(
                COMMAND_TILE_START,
                buildJsonObject { put("tile_id", JsonPrimitive(tileId)) }
            )
        if (ack != null) {
            persistEmittedEvents(currentUserProvider.currentUserId(), ack)
            findSnapshotTile(tileId)?.let { return it }
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        
        return client.from(TABLE_TILES)
            .update({
                set("lifecycle", "Started")
                set("updated_at", now)
            }) {
                filter {
                    eq("id", tileId)
                }
                select()
            }
            .decodeSingle<Tile>()
    }

    override suspend fun completeTile(tileId: String): Tile {
        val ack = tryApplyCoreCommand(
                COMMAND_TILE_COMPLETE,
                buildJsonObject { put("tile_id", JsonPrimitive(tileId)) }
            )
        if (ack != null) {
            persistEmittedEvents(currentUserProvider.currentUserId(), ack)
            findSnapshotTile(tileId)?.let { return it }
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        
        return client.from(TABLE_TILES)
            .update({
                set("lifecycle", "Done")
                set("updated_at", now)
            }) {
                filter {
                    eq("id", tileId)
                }
                select()
            }
            .decodeSingle<Tile>()
    }

    suspend fun deleteTile(tileId: String) {
        val ack = tryApplyCoreCommand(
                COMMAND_TILE_DELETE,
                buildJsonObject { put("tile_id", JsonPrimitive(tileId)) }
            )
        if (ack != null) {
            persistEmittedEvents(currentUserProvider.currentUserId(), ack)
            return
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        
        client.from(TABLE_TILES)
            .update({
                set("deleted_at", now)
            }) {
                filter {
                    eq("id", tileId)
                }
            }
    }

    override suspend fun pauseTile(tileId: String) {
        val ack = tryApplyCoreCommand(
                COMMAND_TILE_PAUSE,
                buildJsonObject { put("tile_id", JsonPrimitive(tileId)) }
            )
        if (ack != null) {
            persistEmittedEvents(currentUserProvider.currentUserId(), ack)
            return
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        
        client.from(TABLE_TILES)
            .update({
                set("lifecycle", "Ready")
                set("updated_at", now)
            }) {
                filter {
                    eq("id", tileId)
                }
            }
    }

    override suspend fun getActiveStartedTile(userId: String): Tile? {
        val snapshot = currentSnapshotOrNull()
        snapshot?.activeTileId?.let { activeId ->
            snapshot.tiles.firstOrNull { it.id == activeId }
                ?.let { return it.toTile(activeTileId = snapshot.activeTileId, phaseStartedAt = snapshot.phaseStartedAt) }
        }
        snapshot?.tiles?.firstOrNull { it.lifecycle == TileLifecycle.STARTED.value }
            ?.let { return it.toTile(activeTileId = snapshot.activeTileId, phaseStartedAt = snapshot.phaseStartedAt) }
        if (snapshot != null && snapshot.revision > 0) return null

        val tiles = client.from(TABLE_TILES)
            .select {
                filter {
                    eq("user_id", userId)
                    eq("lifecycle", TileLifecycle.STARTED.value)
                    exact("deleted_at", null)
                }
                order("updated_at", Order.DESCENDING)
                limit(1)
            }
            .decodeList<Tile>()
        return tiles.firstOrNull()
    }

    override suspend fun continueTile(tileId: String) {
        val ack = tryApplyCoreCommand(
                COMMAND_TILE_CONTINUE,
                buildJsonObject { put("tile_id", JsonPrimitive(tileId)) }
            )
        if (ack != null) {
            persistEmittedEvents(currentUserProvider.currentUserId(), ack)
            return
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        client.from(TABLE_TILES)
            .update({
                set("updated_at", now)
            }) {
                filter {
                    eq("id", tileId)
                }
            }
    }

    override suspend fun getRecentTiles(userId: String, limit: Int): List<Tile> {
        projectedSnapshotTiles()?.take(limit)?.let { return it }

        return client.from(TABLE_TILES)
            .select {
                filter {
                    eq("user_id", userId)
                    exact("deleted_at", null)
                }
                order("updated_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<Tile>()
    }

    override suspend fun saveMemo(tileId: String, note: String) {
        val ack = tryApplyCoreCommand(
                COMMAND_MEMO_ATTACH,
                buildJsonObject {
                    put("tile_id", JsonPrimitive(tileId))
                    put("text", JsonPrimitive(note))
                }
            )
        if (ack != null) {
            persistEmittedEvents(currentUserProvider.currentUserId(), ack)
            return
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        val currentAnnotations = client.from(TABLE_TILES)
            .select {
                filter {
                    eq("id", tileId)
                }
                limit(1)
            }
            .decodeList<Tile>()
            .firstOrNull()
            ?.annotationConditions
            ?: buildJsonObject { }
        val updatedAnnotations = buildJsonObject {
            currentAnnotations.entries.forEach { (key, value) ->
                put(key, value)
            }
            put("note", JsonPrimitive(note))
        }

        client.from(TABLE_TILES)
            .update({
                set("annotation_conditions", updatedAnnotations)
                set("updated_at", now)
            }) {
                filter {
                    eq("id", tileId)
                }
            }
    }

    private fun projectedSnapshotTiles(): List<Tile>? {
        val snapshot = currentSnapshotOrNull() ?: return null
        if (snapshot.revision <= 0 && snapshot.tiles.isEmpty()) return null
        return snapshot.tiles.map {
            it.toTile(activeTileId = snapshot.activeTileId, phaseStartedAt = snapshot.phaseStartedAt)
        }
    }

    private fun findSnapshotTile(tileId: String): Tile? {
        val snapshot = currentSnapshotOrNull() ?: return null
        return snapshot.tiles.firstOrNull { it.id == tileId }
            ?.toTile(activeTileId = snapshot.activeTileId, phaseStartedAt = snapshot.phaseStartedAt)
    }

    private fun currentSnapshotOrNull(): CoreSnapshot? {
        return try {
            coreRuntimeService.currentSnapshot()
        } catch (_: CoreBridgeError.LibraryLoadFailed) {
            null
        } catch (_: CoreBridgeError.NativeMethodUnavailable) {
            null
        } catch (_: CoreBridgeError.SnapshotParseFailed) {
            null
        }
    }

    private fun tryApplyCoreCommand(type: String, payload: kotlinx.serialization.json.JsonObject): app.tastile.android.core.CoreCommandAck? {
        return try {
            val ack = coreRuntimeService.applyCommand(CoreCommandRequest(type = type, payload = payload))
            executionNotificationCoordinator.syncOnce()
            ack
        } catch (_: CoreBridgeError.LibraryLoadFailed) {
            null
        } catch (_: CoreBridgeError.NativeMethodUnavailable) {
            null
        } catch (_: CoreBridgeError.CommandResponseParseFailed) {
            null
        } catch (_: CoreBridgeError.SnapshotParseFailed) {
            null
        }
    }

    private suspend fun persistEmittedEvents(userId: String?, ack: app.tastile.android.core.CoreCommandAck) {
        if (userId.isNullOrBlank()) return
        ack.emittedEvents().forEachIndexed { index, event ->
            eventRepository.appendEmittedEvent(
                userId = userId,
                envelope = event,
                sequenceNumber = System.currentTimeMillis() + index
            )
        }
    }

    private fun CoreTileSnapshot.toTile(
        activeTileId: String? = null,
        phaseStartedAt: String? = null
    ): Tile {
        val lifecycleValue = when {
            lifecycle.isNotBlank() -> lifecycle
            activeTileId == id -> TileLifecycle.STARTED.value
            else -> TileLifecycle.READY.value
        }
        return Tile(
            id = id,
            localTileId = id,
            title = title,
            lifecycle = lifecycleValue,
            updatedAt = if (activeTileId == id) phaseStartedAt else null
        )
    }
}
