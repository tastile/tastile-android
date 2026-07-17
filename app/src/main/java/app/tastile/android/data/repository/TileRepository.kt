package app.tastile.android.data.repository

import app.tastile.android.core.CoreCommandAck
import app.tastile.android.core.CoreSnapshot
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.core.CoreTileSnapshot
import app.tastile.android.data.api.TimelineItem
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import app.tastile.android.data.api.toTiles
import app.tastile.android.data.command.V1CommandDispatcher
import app.tastile.android.data.command.ExecutionStateLookup
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileRepository @Inject constructor(
    private val executionNotificationCoordinator: ExecutionNotificationCoordinator,
    private val eventRepository: EventRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val v1ApiClient: V1ApiClient,
    private val v1CommandDispatcher: V1CommandDispatcher
) : PromptTileRepository, MemoTileRepository {
    // C3 (web→android parity sweep): the read path now goes through
    // [getTiles] honoring the `GET /v1/tiles?view_mode=...&limit=...`
    // query contract that `tastile-web/src/lib/hooks/use-tile-list.ts`
    // uses. The two `next_actionable_*` fields on the wire response
    // thread through `TilesResponse` to the dashboard.
    //
    // Every command still routes to v1 through [v1CommandDispatcher]
    // (Macro Step 5). No more v0 command-name strings, no more
    // CoreRuntimeService.applyCommand / currentSnapshot fallbacks.

    @Volatile
    private var latestReadDiagnostics: String = "source=unknown"
    @Volatile
    private var latestCloudTiles: List<Tile> = emptyList()

    suspend fun getTiles(filter: TileFilter = TileFilter.DEFAULT): TilesResponse {
        val token = currentUserProvider.currentIdToken()
        if (token.isNullOrBlank()) {
            latestReadDiagnostics = "source=v1_skipped reason=no_token count=0 user_match=true"
            return TilesResponse(emptyList(), null, null)
        }
        return try {
            val userId = currentUserProvider.currentUserId().orEmpty()
            val resp = v1ApiClient.getTiles(filter)
            val tiles = resp.toTiles(userId = userId)
            latestCloudTiles = tiles
            latestReadDiagnostics = buildString {
                append("source=v1")
                append(" count=${tiles.size}")
                append(" user_match=true")
                if (!resp.nextActionableTileId.isNullOrBlank()) {
                    append(" next_tile=${resp.nextActionableTileId}")
                }
                if (!resp.nextActionableStartAt.isNullOrBlank()) {
                    append(" next_at=${resp.nextActionableStartAt}")
                }
            }
            TilesResponse(tiles, resp.nextActionableTileId, resp.nextActionableStartAt)
        } catch (e: V1Error) {
            android.util.Log.w("TileRepository", "v1 getTiles failed: ${e.message}", e)
            latestReadDiagnostics = "source=v1_unavailable count=0 user_match=true"
            TilesResponse(emptyList(), null, null)
        } catch (e: Exception) {
            android.util.Log.w("TileRepository", "v1 getTiles failed: ${e.message}", e)
            latestReadDiagnostics = "source=v1_unavailable count=0 user_match=true"
            TilesResponse(emptyList(), null, null)
        }
    }

    suspend fun getTileById(tileId: String): Tile? {
        val userId = currentUserProvider.currentUserId().orEmpty()
        if (userId.isBlank()) return null
        if (latestCloudTiles.isEmpty()) {
            // Refresh the cache against the unfiltered list so callers
            // can still resolve any tile id without re-passing a filter.
            latestCloudTiles = readCloudTilesUnfiltered()
        }
        return latestCloudTiles.firstOrNull { it.id == tileId }
    }

    suspend fun getEditableTileById(tileId: String): Tile? {
        return getTileById(tileId)
    }

    private suspend fun readCloudTilesUnfiltered(): List<Tile> {
        val token = currentUserProvider.currentIdToken()
        if (token.isNullOrBlank()) return emptyList()
        return try {
            val userId = currentUserProvider.currentUserId().orEmpty()
            v1ApiClient.getTiles().toTiles(userId = userId)
        } catch (e: V1Error) {
            android.util.Log.w("TileRepository", "v1 getTiles(unfiltered) failed: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            android.util.Log.w("TileRepository", "v1 getTiles(unfiltered) failed: ${e.message}", e)
            emptyList()
        }
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

        val ack = v1CommandDispatcher.dispatchTileCreate(payload, userId)
        if (ack != null) {
            persistEmittedEvents(userId, ack)
            val generatedId = ack.generatedTileId()
            val fromCloud = latestCloudTiles.firstOrNull { tile ->
                generatedId?.let { tile.id == it } == true ||
                    (generatedId == null && tile.id !in existingIds)
            }
            if (fromCloud != null) return fromCloud
            val snapshotAfter = currentSnapshotOrNull()
            val createdTile = when {
                generatedId != null ->
                    snapshotAfter?.tiles?.firstOrNull { it.id == generatedId }
                else ->
                    snapshotAfter?.tiles?.firstOrNull { it.id !in existingIds }
            }
            createdTile?.let { return it.toTile() }
            if (generatedId != null && ack.accepted) {
                return Tile(
                    id = generatedId,
                    localTileId = generatedId,
                    userId = userId,
                    title = trimmedTitle,
                    lifecycle = TileLifecycle.READY.value
                )
            }
        }

        throw IllegalStateException("Cloud command rejected: create tile")
    }

    suspend fun startTile(tileId: String): Tile {
        // Step 5: tile.start now requires plan_id from v1. The dispatcher
        // throws IllegalStateException with a clear message if the backend
        // hasn't attached a plan to the tile. We propagate the throw.
        val targetWorkMinutes = latestCloudTiles.firstOrNull { it.id == tileId }?.targetWorkMin
        val ack = v1CommandDispatcher.dispatchTileStart(tileId, targetWorkMinutes)
            ?: throw IllegalStateException("Cloud command rejected: start tile")
        refreshCloudCacheAfterCommand(ack)
        return findSnapshotTile(tileId) ?: readCloudTileById(tileId)
            ?: throw IllegalStateException("start tile: cloud has no tile $tileId after dispatch")
    }

    override suspend fun completeTile(tileId: String): Tile {
        return completeTile(tileId = tileId, nextTileId = null, scope = null)
            ?: throw IllegalStateException("Failed to complete tile: $tileId")
    }

    suspend fun completeTile(
        tileId: String? = null,
        nextTileId: String? = null,
        scope: String? = null
    ): Tile? {
        val effectiveTileId = tileId ?: currentSnapshotOrNull()?.activeTileId
        val ack = v1CommandDispatcher.dispatchTileComplete(
            effectiveTileId = effectiveTileId,
            nextTileId = nextTileId,
            scope = scope
        ) ?: run {
            if (effectiveTileId.isNullOrBlank()) return null
            throw IllegalStateException("Cloud command rejected: complete tile")
        }
        refreshCloudCacheAfterCommand(ack)
        if (!effectiveTileId.isNullOrBlank()) {
            findSnapshotTile(effectiveTileId)?.let { return it }
        }
        return null
    }

    suspend fun deleteTile(tileId: String) {
        val ack = v1CommandDispatcher.dispatchTileDelete(tileId)
            ?: throw IllegalStateException("Cloud command rejected: delete tile")
        refreshCloudCacheAfterCommand(ack)
    }

    suspend fun closePlacement(placementId: String) {
        val ack = v1CommandDispatcher.dispatchPlacementClose(placementId)
            ?: throw IllegalStateException("Cloud command rejected: close placement")
        refreshCloudCacheAfterCommand(ack)
    }

    suspend fun startExecution(tileId: String) {
        val ack = v1CommandDispatcher.dispatchPlacementExecutionStart(tileId)
            ?: throw IllegalStateException("Cloud command rejected: start execution")
        refreshCloudCacheAfterCommand(ack)
    }

    suspend fun finishExecution(tileId: String) {
        val ack = v1CommandDispatcher.dispatchExecutionFinish(tileId)
            ?: throw IllegalStateException("Cloud command rejected: finish execution")
        refreshCloudCacheAfterCommand(ack)
    }

    override suspend fun pauseTile(tileId: String) {
        val ack = v1CommandDispatcher.dispatchTilePause(tileId)
            ?: throw IllegalStateException("Cloud command rejected: pause tile")
        refreshCloudCacheAfterCommand(ack)
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

        // Snapshot is absent or empty — read cloud tiles and find a STARTED one.
        // Preserve the v1 cache so subsequent calls don't re-fetch.
        val cloudTiles = if (latestCloudTiles.isEmpty()) {
            readCloudTilesUnfiltered()
        } else {
            latestCloudTiles
        }
        return cloudTiles.firstOrNull { it.lifecycle.equals(TileLifecycle.STARTED.value, ignoreCase = true) }
    }

    override suspend fun continueTile(tileId: String) {
        val ack = v1CommandDispatcher.dispatchTileContinue(tileId)
            ?: throw IllegalStateException("Cloud command rejected: continue tile")
        refreshCloudCacheAfterCommand(ack)
    }

    /** v1 execution state for a visible started tile; null means unknown, not paused. */
    suspend fun executionStateForTile(tileId: String): Int? =
        v1CommandDispatcher.executionStateForTile(tileId)

    /** Preserves a failed cached-execution validation separately from no active execution. */
    suspend fun executionStateLookupForTile(tileId: String): ExecutionStateLookup =
        v1CommandDispatcher.executionStateLookupForTile(tileId)

    fun clearExecutionCacheForTile(tileId: String) {
        v1CommandDispatcher.clearExecutionCacheForTile(tileId)
    }

    suspend fun deferTile(tileId: String, deferredUntil: String) {
        val ack = v1CommandDispatcher.dispatchTileDefer(
            tileId = tileId,
            deferredUntil = deferredUntil,
        ) ?: run {
            // v0 used to fall through to pauseTile on defer failure; with v1
            // we just throw — the UI surfaces the failure.
            throw IllegalStateException("Cloud command rejected: defer tile")
        }
        refreshCloudCacheAfterCommand(ack)
    }

    suspend fun updateTile(tileId: String, payload: JsonObject) {
        val ack = v1CommandDispatcher.dispatchTileUpdate(tileId, payload)
            ?: throw IllegalStateException("Cloud command rejected: update tile")
        refreshCloudCacheAfterCommand(ack)
    }

    override suspend fun requestPrompt(tileId: String): Boolean {
        val ack = v1CommandDispatcher.dispatchPromptRequest(tileId) ?: return false
        persistEmittedEvents(currentUserProvider.currentUserId(), ack)
        return true
    }

    suspend fun respondStartupRecoveryPrompt(
        promptId: String,
        tileId: String,
        actionId: String,
        stopAtIso: String? = null
    ): Boolean {
        val ack = respondStartupRecoveryPromptResponse(
            promptId = promptId,
            tileId = tileId,
            actionId = actionId,
            stopAtIso = stopAtIso
        ) ?: return false
        persistEmittedEvents(currentUserProvider.currentUserId(), ack)
        return true
    }

    suspend fun respondStartupRecoveryPromptResponse(
        promptId: String,
        tileId: String,
        actionId: String,
        stopAtIso: String? = null
    ): CoreCommandAck? {
        val ack = v1CommandDispatcher.dispatchStartupRecoveryPrompt(
            promptId = promptId,
            tileId = tileId,
            actionId = actionId,
            stopAtIso = stopAtIso
        ) ?: return null
        persistEmittedEvents(currentUserProvider.currentUserId(), ack)
        return ack
    }

    suspend fun getTimeline(start: Instant, end: Instant, ownerIds: List<String> = emptyList()): List<CoreTimelineItem> {
        readCloudTimeline(start, end, ownerIds)?.let { v1Items ->
            if (v1Items.isNotEmpty()) {
                latestReadDiagnostics = buildString {
                    append(latestReadDiagnostics)
                    append(" timeline_source=v1")
                    append(" timeline_count=${v1Items.size}")
                }
                return v1Items
            }
        }
        if (latestCloudTiles.isEmpty()) {
            latestCloudTiles = readCloudTilesUnfiltered()
        }
        val fallback = buildTimelineFromTiles(latestCloudTiles, Instant.now())
        latestReadDiagnostics = buildString {
            append(latestReadDiagnostics)
            append(" timeline_source=cloud_fallback")
            append(" fallback_timeline_count=${fallback.size}")
        }
        return fallback
    }

    private suspend fun readCloudTimeline(
        start: Instant,
        end: Instant,
        ownerIds: List<String>,
    ): List<CoreTimelineItem>? {
        val token = currentUserProvider.currentIdToken()
        if (token.isNullOrBlank()) return null
        return try {
            val response = v1ApiClient.getTimeline(start, end, ownerIds)
            val mapped = response.mapNotNull { it.toCoreTimelineItem(start, end) }
            android.util.Log.d("TileRepository", "v1 timeline: ${response.size} items, mapped=${mapped.size}")
            mapped
        } catch (e: V1Error) {
            android.util.Log.w("TileRepository", "v1 getTimeline failed: ${e.message}", e)
            latestReadDiagnostics = buildString {
                append(latestReadDiagnostics)
                append(" timeline_source=v1_unavailable")
            }
            null
        } catch (e: Exception) {
            android.util.Log.w("TileRepository", "v1 getTimeline failed: ${e.message}", e)
            latestReadDiagnostics = buildString {
                append(latestReadDiagnostics)
                append(" timeline_source=v1_unavailable")
            }
            null
        }
    }

    private fun TimelineItem.toCoreTimelineItem(rangeStart: Instant, rangeEnd: Instant): CoreTimelineItem? {
        val startInstant = parseIsoInstant(span.startAt) ?: return null
        val endInstant = parseIsoInstant(span.endAt ?: span.startAt)
        if (startInstant.isBefore(rangeStart) || !startInstant.isBefore(rangeEnd)) return null
        return CoreTimelineItem(
            id = placementId,
            tileId = tileId,
            sourceKind = source.value.toInt(),
            title = content.title.ifBlank { "Untitled" },
            type = role.toRoleName(),
            status = resolution.state.toStatusName(),
            startAt = startInstant.toString(),
            endAt = endInstant?.toString() ?: startInstant.plusSeconds(60).toString(),
        )
    }

    private fun Byte.toRoleName(): String = when (toInt()) {
        // Match tastile-web constants where reasonable; default covers unknown values.
        'w'.code, 0 -> "work"
        'b'.code, 1 -> "break"
        'l'.code, 2 -> "label"
        'f'.code, 3 -> "fixed"
        's'.code, 4 -> "scheduled"
        else -> "work"
    }

    private fun Byte.toStatusName(): String = when (toInt()) {
        'p'.code, 0 -> "scheduled"
        'a'.code, 1 -> "active"
        's'.code, 2 -> "started"
        'd'.code, 3 -> "done"
        'c'.code, 4 -> "completed"
        else -> "scheduled"
    }

    suspend fun rescheduleTile(tileId: String, startAtIso: String, endAtIso: String) {
        val ownerId = currentUserProvider.currentUserId()
            ?: throw IllegalStateException("Cannot reschedule tile without the current user")
        val ack = v1CommandDispatcher.dispatchTileReschedule(
            tileId = tileId,
            startAt = startAtIso,
            endAt = endAtIso,
            ownerId = ownerId,
        ) ?: throw IllegalStateException("Cloud command rejected: reschedule tile")
        refreshCloudCacheAfterCommand(ack)
    }

    override suspend fun getRecentTiles(userId: String, limit: Int): List<Tile> {
        if (latestCloudTiles.isEmpty()) {
            latestCloudTiles = readCloudTilesUnfiltered()
        }
        return latestCloudTiles.take(limit)
    }

    override suspend fun saveMemo(tileId: String, note: String) {
        attachMemo(tileId = tileId, text = note, memoKind = null)
    }

    suspend fun attachMemo(tileId: String?, text: String, memoKind: String? = null) {
        val ack = v1CommandDispatcher.dispatchMemoAttach(
            tileId = tileId,
            body = text
        ) ?: run {
            if (tileId.isNullOrBlank()) return
            throw IllegalStateException("Cloud command rejected: attach memo")
        }
        refreshCloudCacheAfterCommand(ack)
    }

    private fun findSnapshotTile(tileId: String): Tile? {
        // Macro Step 5: no v0 snapshot to search. Always null; callers fall
        // through to the v1 read path.
        return null
    }

    private fun currentSnapshotOrNull(): CoreSnapshot? {
        // Macro Step 5: the v0 CoreRuntimeService is no longer injected into
        // TileRepository. The remaining callers in this file (getActiveStartedTile,
        // createTile) tolerate a null snapshot and degrade to v1-only reads.
        return null
    }

    fun latestReadDiagnostics(): String = latestReadDiagnostics

    private suspend fun refreshCloudCacheAfterCommand(@Suppress("UNUSED_PARAMETER") ack: CoreCommandAck) {
        // After a v1 mutation, re-read the cloud tile list so subsequent
        // getTiles() / getTileById() calls see the new state without an
        // explicit refresh.
        latestCloudTiles = readCloudTilesUnfiltered()
        executionNotificationCoordinator.syncOnce()
    }

    private suspend fun readCloudTileById(tileId: String): Tile? {
        if (latestCloudTiles.isEmpty()) {
            latestCloudTiles = readCloudTilesUnfiltered()
        }
        return latestCloudTiles.firstOrNull { it.id == tileId }
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

@Serializable
internal data class SnapshotTileRow(
    @SerialName("tile_id") val tileId: String,
    val title: String = "",
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("tile_json") val tileJson: JsonObject? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

internal fun snapshotRowToTile(row: SnapshotTileRow, userId: String): Tile {
    val core = row.tileJson?.get("core")?.jsonObject
    val temporal = row.tileJson?.get("temporal")?.jsonObject
    val objective = row.tileJson?.get("objective")?.jsonObject
    val title = core?.get("title")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: row.title
    val nextAction = core?.get("nextAction")?.jsonPrimitive?.contentOrNull
    val doneDefinition = core?.get("doneDefinition")?.jsonPrimitive?.contentOrNull
    val startedAt = readIsoValue(core, "startedAt", "started_at")
    val completedAt = readIsoValue(core, "completedAt", "completed_at") ?: row.closedAt
    val fixedStart = readIsoValue(temporal, "fixedStart", "fixed_start")
    val activeStart = readIsoValue(temporal, "activeStart", "active_start")
    val fallbackStartAt = startedAt ?: fixedStart ?: activeStart ?: row.updatedAt ?: row.createdAt
    val lifecycle = when {
        !completedAt.isNullOrBlank() -> TileLifecycle.DONE.value
        !startedAt.isNullOrBlank() -> TileLifecycle.STARTED.value
        else -> TileLifecycle.READY.value
    }
    return Tile(
        id = row.tileId,
        userId = userId,
        localTileId = row.tileId,
        title = title,
        nextAction = nextAction,
        doneDefinition = doneDefinition,
        temporalConditions = normalizeTemporalConditions(temporal),
        objectiveConditions = normalizeObjectiveConditions(objective),
        lifecycle = lifecycle,
        createdAt = fallbackStartAt,
        updatedAt = completedAt ?: startedAt ?: row.updatedAt ?: row.createdAt
    )
}

private fun readIsoValue(source: JsonObject?, vararg keys: String): String? {
    if (source == null) return null
    for (key in keys) {
        val value = source[key]?.jsonPrimitive?.contentOrNull
        if (!value.isNullOrBlank()) return value
    }
    return null
}

private fun normalizeTemporalConditions(temporal: JsonObject?): JsonObject? {
    if (temporal == null) return null
    val fixedStart = readIsoValue(temporal, "fixed_start", "fixedStart")
    val activeStart = readIsoValue(temporal, "active_start", "activeStart")
    val fixedEnd = readIsoValue(temporal, "fixed_end", "fixedEnd")
    val activeEnd = readIsoValue(temporal, "active_end", "activeEnd")
    if (fixedStart == null && activeStart == null && fixedEnd == null && activeEnd == null) return null
    return buildJsonObject {
        fixedStart?.let { put("fixed_start", JsonPrimitive(it)) }
        activeStart?.let { put("active_start", JsonPrimitive(it)) }
        fixedEnd?.let { put("fixed_end", JsonPrimitive(it)) }
        activeEnd?.let { put("active_end", JsonPrimitive(it)) }
    }
}

private fun normalizeObjectiveConditions(objective: JsonObject?): JsonObject? {
    if (objective == null) return null
    val targetWorkMin = objective["target_work_min"]?.jsonPrimitive?.contentOrNull
        ?: objective["targetWorkMin"]?.jsonPrimitive?.contentOrNull
    if (targetWorkMin.isNullOrBlank()) return null
    return buildJsonObject {
        put("target_work_min", JsonPrimitive(targetWorkMin))
    }
}

private fun Tile.temporalIso(vararg keys: String): String? {
    val temporal = temporalConditions ?: return null
    for (key in keys) {
        val value = temporal[key]?.jsonPrimitive?.contentOrNull
        if (!value.isNullOrBlank()) return value
    }
    return null
}

private fun Tile.targetWorkMin(): Long? {
    return objectiveConditions
        ?.get("target_work_min")
        ?.jsonPrimitive
        ?.contentOrNull
        ?.toLongOrNull()
        ?.takeIf { it > 0 }
}

internal fun normalizeCoreTimeline(
    timeline: List<CoreTimelineItem>,
    now: Instant,
    zoneId: ZoneId
): List<CoreTimelineItem> {
    val dayStart = now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
    val dayEnd = dayStart.plusSeconds(24L * 60L * 60L)
    return timeline
        .asSequence()
        .filterNot { item -> item.tileId?.startsWith("synthetic:break:") == true }
        .mapNotNull { item ->
            val start = parseIsoInstant(item.startAt) ?: return@mapNotNull null
            val parsedEnd = parseIsoInstant(item.endAt ?: item.startAt)
            val end = when {
                parsedEnd == null -> start.plusSeconds(60)
                parsedEnd.isAfter(start) -> parsedEnd
                else -> start.plusSeconds(60)
            }
            if (!start.isBefore(dayEnd) || !end.isAfter(dayStart)) return@mapNotNull null
            item.copy(
                startAt = if (start.isBefore(dayStart)) dayStart.toString() else start.toString(),
                endAt = (if (end.isAfter(dayEnd)) dayEnd else end).toString()
            )
        }
        .sortedBy { it.startAt }
        .toList()
}

private fun parseIsoInstant(value: String): Instant? {
    return try {
        Instant.parse(value)
    } catch (_: Exception) {
        try {
            ZonedDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toInstant()
        } catch (_: Exception) {
            null
        }
    }
}

internal fun shouldUseCoreTimeline(
    rawTimeline: List<CoreTimelineItem>,
    normalizedTimeline: List<CoreTimelineItem>
): Boolean {
    if (normalizedTimeline.isEmpty()) return false
    val syntheticBreakCount = rawTimeline.count { it.tileId?.startsWith("synthetic:break:") == true }
    val realWorkOrFixedCount = normalizedTimeline.count {
        val tileId = it.tileId
        tileId != null && !tileId.startsWith("synthetic:") && it.type != "break"
    }
    if (realWorkOrFixedCount == 0 && syntheticBreakCount > 0) return false
    return true
}

internal fun buildTimelineFromTiles(tiles: List<Tile>, now: Instant): List<CoreTimelineItem> {
    return tiles.mapNotNull { tile ->
        val scheduledStart = tile.temporalIso("fixed_start", "active_start")
        val scheduledEnd = tile.temporalIso("fixed_end", "active_end")
        val startAt = scheduledStart
            ?: when (tile.lifecycle) {
                TileLifecycle.DONE.value, TileLifecycle.STARTED.value -> tile.createdAt ?: tile.updatedAt
                else -> null
            }
            ?: return@mapNotNull null
        val targetEnd = tile.targetWorkMin()?.let { minutes ->
            parseIsoInstant(startAt)?.plusSeconds(minutes * 60)?.toString()
        }
        val endAt = when (tile.lifecycle) {
            TileLifecycle.DONE.value -> tile.updatedAt ?: scheduledEnd ?: targetEnd ?: startAt
            TileLifecycle.STARTED.value -> scheduledEnd ?: now.toString()
            else -> scheduledEnd ?: targetEnd ?: return@mapNotNull null
        }
        val status = when (tile.lifecycle) {
            TileLifecycle.DONE.value -> "done"
            TileLifecycle.STARTED.value -> "active"
            else -> "scheduled"
        }
        val type = when (tile.lifecycle) {
            TileLifecycle.STARTED.value -> "work"
            TileLifecycle.DONE.value -> "work"
            else -> "fixed"
        }
        CoreTimelineItem(
            id = "fallback-${tile.id}",
            tileId = tile.id,
            title = tile.title,
            type = type,
            status = status,
            startAt = startAt,
            endAt = endAt
        )
    }.sortedBy { it.startAt }
}
