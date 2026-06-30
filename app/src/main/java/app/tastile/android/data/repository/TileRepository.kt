package app.tastile.android.data.repository

import app.tastile.android.core.CoreCommandAck
import app.tastile.android.core.CoreSnapshot
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.core.CoreTileSnapshot
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import app.tastile.android.data.api.toTiles
import app.tastile.android.data.command.V1CommandDispatcher
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
    // Macro Step 5: every command now routes to v1 through
    // [v1CommandDispatcher].  No more v0 command-name strings, no more
    // CoreRuntimeService.applyCommand / currentSnapshot fallbacks.

    @Volatile
    private var latestReadDiagnostics: String = "source=unknown"
    @Volatile
    private var latestCloudTiles: List<Tile> = emptyList()

    suspend fun getTiles(userId: String): List<Tile> {
        readCloudTiles()?.let { tiles ->
            // readCloudTiles() already set "source=v1 ..." on success — preserve it.
            latestCloudTiles = tiles
            return tiles
        }

        if (canUseSnapshotForUser(userId)) {
            projectedSnapshotTiles()?.let {
                val snapshot = currentSnapshotOrNull()
                latestReadDiagnostics = buildString {
                    append("source=core")
                    append(" revision=${snapshot?.revision ?: 0}")
                    append(" snapshot_tiles=${it.size}")
                    append(" user_match=true")
                }
                return it
            }
        }

        // Neither v1 nor snapshot could answer.
        // Preserve the "source=v1_unavailable" diagnostic set by readCloudTiles() if v1 failed;
        // otherwise emit a baseline cloud_unavailable entry.
        if (!latestReadDiagnostics.startsWith("source=v1_unavailable")) {
            latestReadDiagnostics = "source=cloud_unavailable count=0 user_match=${canUseSnapshotForUser(userId)}"
        }
        latestCloudTiles = emptyList()
        return emptyList()
    }

    private suspend fun readCloudTiles(): List<Tile>? {
        val token = currentUserProvider.currentIdToken()
        if (token.isNullOrBlank()) return null
        return try {
            val v1Tiles = v1ApiClient.listTiles().toTiles(userId = currentUserProvider.currentUserId().orEmpty())
            latestReadDiagnostics = "source=v1 count=${v1Tiles.size} user_match=true"
            v1Tiles
        } catch (e: V1Error) {
            android.util.Log.w("TileRepository", "v1 listTiles failed: ${e.message}", e)
            latestReadDiagnostics = "source=v1_unavailable count=0 user_match=true"
            null
        } catch (e: Exception) {
            android.util.Log.w("TileRepository", "v1 listTiles failed: ${e.message}", e)
            latestReadDiagnostics = "source=v1_unavailable count=0 user_match=true"
            null
        }
    }

    suspend fun getTiles(): TilesResponse {
        return getTiles(viewMode = "all", lifecycle = null, limit = null, search = null)
    }

    suspend fun getTiles(
        viewMode: String,
        lifecycle: String? = null,
        limit: Int? = null,
        search: String? = null
    ): TilesResponse {
        val userId = currentUserProvider.currentUserId().orEmpty()
        val base = if (userId.isBlank()) {
            currentSnapshotOrNull()?.tiles?.map { it.toTile() }.orEmpty()
        } else {
            getTiles(userId)
        }
        val filtered = base
            .let { tiles ->
                if (lifecycle.isNullOrBlank()) tiles
                else tiles.filter { it.lifecycle.equals(lifecycle, ignoreCase = true) }
            }
            .let { tiles ->
                if (search.isNullOrBlank()) tiles
                else tiles.filter {
                    it.title.contains(search, ignoreCase = true) ||
                        (it.nextAction?.contains(search, ignoreCase = true) == true)
                }
            }
            .let { tiles ->
                if (viewMode.equals("in_progress", ignoreCase = true)) {
                    tiles.filter { it.lifecycle.equals(TileLifecycle.STARTED.value, ignoreCase = true) }
                } else {
                    tiles
                }
            }
            .let { tiles -> if (limit == null) tiles else tiles.take(limit) }
        return TilesResponse(tiles = filtered)
    }

    suspend fun getTileById(tileId: String): Tile? {
        findSnapshotTile(tileId)?.let { return it }
        val userId = currentUserProvider.currentUserId().orEmpty()
        if (userId.isBlank()) return null
        if (latestCloudTiles.isEmpty()) {
            latestCloudTiles = readCloudTiles().orEmpty()
        }
        return latestCloudTiles.firstOrNull { it.id == tileId }
    }

    suspend fun getEditableTileById(tileId: String): Tile? {
        return getTileById(tileId)
    }

    suspend fun getTilesInProgress(): TilesInProgressResponse {
        val snapshot = currentSnapshotOrNull()
        val tiles = when {
            snapshot != null -> snapshot.inProgressTiles.map { inProgress ->
                snapshot.tiles.firstOrNull { it.id == inProgress.tileId }?.toTile(
                    activeTileId = snapshot.activeTileId,
                    phaseStartedAt = snapshot.phaseStartedAt
                ) ?: Tile(
                    id = inProgress.tileId,
                    localTileId = inProgress.tileId,
                    title = inProgress.title,
                    lifecycle = TileLifecycle.STARTED.value,
                    updatedAt = inProgress.startedAt
                )
            }
            else -> {
                readCloudTiles().orEmpty()
                    .filter { it.lifecycle.equals(TileLifecycle.STARTED.value, ignoreCase = true) }
            }
        }
        return TilesInProgressResponse(tiles = tiles, count = tiles.size)
    }

    suspend fun getActiveTile(): ActiveTileResponse {
        val snapshot = currentSnapshotOrNull()
        val active = snapshot?.activeTileId?.let { id ->
            snapshot.tiles.firstOrNull { it.id == id }?.toTile(
                activeTileId = snapshot.activeTileId,
                phaseStartedAt = snapshot.phaseStartedAt
            )
        }
        val phaseKind = snapshot?.phaseKind ?: if (active != null) "working" else "idle"
        return ActiveTileResponse(
            tile = active,
            phase = phaseKind,
            phaseStartedAt = snapshot?.phaseStartedAt,
            phaseEndsAt = snapshot?.phaseEndsAt,
            nextVisibleAction = when (phaseKind.lowercase()) {
                "break", "on_break" -> "end_break"
                "working", "work" -> "complete"
                else -> "start"
            }
        )
    }

    suspend fun getExecution(): ExecutionResponse {
        val snapshot = currentSnapshotOrNull()
        return ExecutionResponse(
            activeTileId = snapshot?.activeTileId,
            phaseKind = snapshot?.phaseKind ?: "idle",
            phaseStartedAt = snapshot?.phaseStartedAt,
            phaseEndsAt = snapshot?.phaseEndsAt,
            pendingPromptId = snapshot?.promptQueue?.firstOrNull { it.status.equals("pending", true) }?.promptId,
            tileCount = snapshot?.tiles?.size ?: 0,
            eventCount = 0
        )
    }

    suspend fun getExecutionView(): ExecutionViewResponse {
        val snapshot = currentSnapshotOrNull()
        val inProgress = getTilesInProgress().tiles
        val main = snapshot?.activeTileId?.let { id -> inProgress.firstOrNull { it.id == id } }
            ?: inProgress.firstOrNull()
        val phase = snapshot?.phaseKind.orEmpty().lowercase()
        val isOnBreak = phase.contains("break")
        val isWorking = main != null && !isOnBreak
        return ExecutionViewResponse(
            tilesInProgress = inProgress,
            mainTile = main,
            isWorking = isWorking,
            isOnBreak = isOnBreak,
            isIdle = !isWorking && !isOnBreak,
            mainTileStartedAt = snapshot?.phaseStartedAt,
            mainTileEndsAt = snapshot?.phaseEndsAt,
            pendingPromptId = snapshot?.promptQueue?.firstOrNull { it.status.equals("pending", true) }?.promptId,
            tileCount = snapshot?.tiles?.size ?: 0,
            eventCount = 0
        )
    }

    override suspend fun getPendingPrompt(): PromptViewResponse? {
        val snapshot = currentSnapshotOrNull() ?: return null
        val pending = snapshot.promptQueue.firstOrNull { it.status.equals("pending", true) } ?: return null
        val title = pending.tileId?.let { id -> snapshot.tiles.firstOrNull { it.id == id }?.title } ?: "Prompt"
        return PromptViewResponse(
            promptId = pending.promptId,
            kind = pending.kind,
            severity = pending.severity,
            tileId = pending.tileId,
            title = title,
            body = pending.reason,
            why = pending.reason,
            suggestedMinutes = pending.suggestedMinutes,
            actions = pending.actions.map { PromptActionViewResponse(id = it, label = it.replace("_", " ")) },
            createdAt = pending.scheduledAt,
            stale = false
        )
    }

    suspend fun getPendingPromptResponse(): PendingPromptResponse {
        return PendingPromptResponse(prompt = getPendingPrompt())
    }

    suspend fun getTodayTimelineView(): TimelineTodayResponse {
        val timeline = getTimeline()
        return TimelineTodayResponse(
            items = timeline.map { item ->
                val durationMin = if (!item.endAt.isNullOrBlank()) {
                    runCatching {
                        val start = parseIsoInstant(item.startAt) ?: return@runCatching 0L
                        val end = parseIsoInstant(item.endAt) ?: return@runCatching 0L
                        ((end.epochSecond - start.epochSecond) / 60L).coerceAtLeast(0L)
                    }.getOrDefault(0L)
                } else {
                    0L
                }
                TimelineItemViewResponse(
                    kind = item.type,
                    tileId = item.tileId,
                    semanticRole = item.type,
                    title = item.title,
                    startedAt = item.startAt,
                    endedAt = item.endAt,
                    durationMin = durationMin,
                    isActive = item.status.equals("active", ignoreCase = true) ||
                        item.status.equals("started", ignoreCase = true)
                )
            }
        )
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
        val ack = v1CommandDispatcher.dispatchTileStart(tileId)
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

    override suspend fun pauseTile(tileId: String) {
        val ack = v1CommandDispatcher.dispatchTilePause(tileId)
            ?: throw IllegalStateException("Cloud command rejected: pause tile")
        refreshCloudCacheAfterCommand(ack)
    }

    override suspend fun getActiveStartedTile(userId: String): Tile? {
        val snapshot = if (canUseSnapshotForUser(userId)) currentSnapshotOrNull() else null
        snapshot?.activeTileId?.let { activeId ->
            snapshot.tiles.firstOrNull { it.id == activeId }
                ?.let { return it.toTile(activeTileId = snapshot.activeTileId, phaseStartedAt = snapshot.phaseStartedAt) }
        }
        snapshot?.tiles?.firstOrNull { it.lifecycle == TileLifecycle.STARTED.value }
            ?.let { return it.toTile(activeTileId = snapshot.activeTileId, phaseStartedAt = snapshot.phaseStartedAt) }
        if (snapshot != null && snapshot.revision > 0) return null

        return readCloudTiles().orEmpty()
            .firstOrNull { it.lifecycle.equals(TileLifecycle.STARTED.value, ignoreCase = true) }
    }

    override suspend fun continueTile(tileId: String) {
        val ack = v1CommandDispatcher.dispatchTileContinue(tileId)
            ?: throw IllegalStateException("Cloud command rejected: continue tile")
        refreshCloudCacheAfterCommand(ack)
    }

    suspend fun deferTile(tileId: String, reason: String? = null, minutes: Int? = null) {
        val ack = v1CommandDispatcher.dispatchTileDefer(
            tileId = tileId,
            reason = reason,
            minutes = minutes
        ) ?: run {
            // v0 used to fall through to pauseTile on defer failure; with v1
            // we just throw — the UI surfaces the failure.
            throw IllegalStateException("Cloud command rejected: defer tile")
        }
        refreshCloudCacheAfterCommand(ack)
    }

    suspend fun startBreak(breakMin: Int, insertionMode: String? = null) {
        // v1 has no break endpoint. Per v1/10 §10 ("breaks are not special"),
        // breaks are Flow+Window constructs in v1. The UI still calls this,
        // so we throw a clear UnsupportedOperationException.
        throw UnsupportedOperationException(
            "break.start is not supported in v1 — breaks are Flow + Window constructs. " +
                "Use the v1 Flow + Window commands instead."
        )
    }

    suspend fun endBreak() {
        throw UnsupportedOperationException(
            "break.end is not supported in v1 — breaks are Flow + Window constructs. " +
                "Use the v1 Flow + Window commands instead."
        )
    }

    suspend fun extendTile(extendMin: Int) {
        // v0 carried no tile_id for extend, so v1's /v1/tiles/{id}/extend-phase
        // cannot be addressed. We can't proceed without a tile_id. Throw a
        // clear error — the UI's extend button must be re-wired to capture
        // the active tile id before calling this.
        throw UnsupportedOperationException(
            "tile.extend requires an active tile_id; v0 callers did not pass one. " +
                "Wire the UI's extend action to pass the currently active tile id."
        )
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

    suspend fun requestPromptResponse(tileId: String): RequestPromptResponse {
        val ok = requestPrompt(tileId)
        return RequestPromptResponse(
            ok = ok,
            prompt = getPendingPrompt(),
            error = if (ok) null else "command_rejected"
        )
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

    suspend fun getTimeline(): List<CoreTimelineItem> {
        val snapshotTimeline = currentSnapshotOrNull()?.timeline.orEmpty()
        if (snapshotTimeline.isNotEmpty()) {
            val now = Instant.now()
            val normalized = normalizeCoreTimeline(snapshotTimeline, now, ZoneId.systemDefault())
            val syntheticBreaks = snapshotTimeline.count { it.tileId?.startsWith("synthetic:break:") == true }
            if (shouldUseCoreTimeline(snapshotTimeline, normalized)) {
                latestReadDiagnostics = buildString {
                    append(latestReadDiagnostics)
                    append(" timeline_source=core")
                    append(" timeline_count=${normalized.size}")
                }
                return normalized
            }
            latestReadDiagnostics = buildString {
                append(latestReadDiagnostics)
                append(" timeline_source=cloud_fallback")
                append(" core_timeline_count=${snapshotTimeline.size}")
                append(" core_synthetic_breaks=$syntheticBreaks")
            }
        }
        if (latestCloudTiles.isEmpty()) {
            latestCloudTiles = readCloudTiles().orEmpty()
        }
        val fallback = buildTimelineFromTiles(latestCloudTiles, Instant.now())
        latestReadDiagnostics = buildString {
            append(latestReadDiagnostics)
            append(" fallback_timeline_count=${fallback.size}")
        }
        return fallback
    }

    suspend fun rescheduleTile(tileId: String, startAtIso: String, endAtIso: String) {
        val ack = v1CommandDispatcher.dispatchTileReschedule(
            tileId = tileId,
            startAt = startAtIso,
            endAt = endAtIso
        ) ?: throw IllegalStateException("Cloud command rejected: reschedule tile")
        refreshCloudCacheAfterCommand(ack)
    }

    override suspend fun getRecentTiles(userId: String, limit: Int): List<Tile> {
        if (canUseSnapshotForUser(userId)) {
            projectedSnapshotTiles()?.take(limit)?.let { return it }
        }

        if (latestCloudTiles.isEmpty()) {
            latestCloudTiles = readCloudTiles().orEmpty()
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

    private fun projectedSnapshotTiles(): List<Tile>? {
        // Macro Step 5: no v0 snapshot anymore — v1 read is the only source.
        return null
    }

    private fun canUseSnapshotForUser(userId: String): Boolean {
        val currentUserId = currentUserProvider.currentUserId()
        return !currentUserId.isNullOrBlank() && currentUserId == userId
    }

    private fun findSnapshotTile(tileId: String): Tile? {
        // Macro Step 5: no v0 snapshot to search. Always null; callers fall
        // through to the v1 read path.
        return null
    }

    private fun currentSnapshotOrNull(): CoreSnapshot? {
        // Macro Step 5: the v0 CoreRuntimeService is no longer injected into
        // TileRepository. Snapshot-backed UI surfaces (getActiveTile,
        // getExecution, getExecutionView, getTilesInProgress) degrade to
        // v1-only reads. A future rewrite of the mobile UI will replace these
        // surfaces with v1 Execution / Timeline views.
        return null
    }

    fun latestReadDiagnostics(): String = latestReadDiagnostics

    private suspend fun refreshCloudCacheAfterCommand(@Suppress("UNUSED_PARAMETER") ack: CoreCommandAck) {
        // After a v1 mutation, re-read the cloud tile list so subsequent
        // getTiles() / getTileById() calls see the new state without an
        // explicit refresh.
        latestCloudTiles = readCloudTiles().orEmpty()
        executionNotificationCoordinator.syncOnce()
    }

    private suspend fun readCloudTileById(tileId: String): Tile? {
        if (latestCloudTiles.isEmpty()) {
            latestCloudTiles = readCloudTiles().orEmpty()
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
