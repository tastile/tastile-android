package app.tastile.android.data.command

import android.util.Log
import app.tastile.android.core.CoreCommandAck
import app.tastile.android.core.CoreCommandResponse
import app.tastile.android.data.api.AppendChangesPayload
import app.tastile.android.data.api.ArchiveTilePayload
import app.tastile.android.data.api.AttachMemoPayload
import app.tastile.android.data.api.CommandResponse
import app.tastile.android.data.api.ClosePlacementPayload
import app.tastile.android.data.api.ExecutionFinishPayload
import app.tastile.android.data.api.CreatePromptRequestPayload
import app.tastile.android.data.api.CreateTilePayload
import app.tastile.android.data.api.SetTileLifecyclePayload
import app.tastile.android.data.api.StartTileBaseline
import app.tastile.android.data.api.StartTilePayload
import app.tastile.android.data.api.StartExecutionPayload
import app.tastile.android.data.api.PlacementSpanPayload
import app.tastile.android.data.api.SourceRefPayload
import app.tastile.android.data.api.UpdateTilePayload
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1NumericConstants
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed v1 command dispatcher. Each method builds the v1 typed payload,
 * posts via `V1ApiClient.postCommand` (or `deleteCommand` for archive),
 * and maps the v1 [CommandResponse] back to the v0 [CoreCommandAck] shape
 * that `TileRepository` already understands. The dispatcher returns `null`
 * on v1 errors so callers can surface a clear failure rather than silently
 * dropping the request. The v0 `tryApplyCoreCommand` / v0 local runtime
 * fallback path was removed in Step 5.
 *
 * Field-mapping conventions (documented so future changes don't have to guess):
 *   - tile.create: v0 `title` → v1 `title`. v0 had no `kind`, so we default
 *     to `TileKind.PLACEMENT (1)`. v0 had no `plan_role`, so we default to
 *     `PlanRole.EXECUTABLE (0)`. Optional color / icon / description are
 *     passed through only if present in v0.
 *   - tile.update: recognised v0 keys are `title`, `description`, `color`,
 *     `icon`, `external_id`. Unknown keys are ignored.
 *   - tile.defer: v0 `minutes` → `deferred_until = now + minutes`. State=1 (deferred).
 *   - tile.complete: state=2, `completed_at = now`. The server is allowed to
 *     overwrite this if domain invariants demand a different timestamp.
 *   - memo.attach: v0 `text` → v1 `body`.
 */
@Singleton
class V1CommandDispatcher @Inject constructor(
    private val v1ApiClient: V1ApiClient
) {
    suspend fun dispatchPlacementClose(placementId: String): CoreCommandAck? = runCatching {
        v1ApiClient.postCommandNoResponse("/v1/placements/$placementId/close", ClosePlacementPayload(placementId), ClosePlacementPayload.serializer())
        CoreCommandResponse(true, null, null, emptyList(), buildJsonObject { put("placementId", JsonPrimitive(placementId)) }, null)
    }.getOrNull()
    /**
     * Kept only to bridge the paused-execution hole in the current read API.
     * A resumed process revalidates it through GET /v1/executions/{id}; after
     * process death the backend currently exposes only active (not paused)
     * executions via /v1/active-tile.
     */
    private val executionIdsByTile = mutableMapOf<String, String>()
    suspend fun dispatchTileCreate(
        v0Payload: JsonObject,
        userId: String
    ): CoreCommandAck? {
        return runCatching {
            val title = v0Payload["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (title.isBlank()) return@runCatching null
            val kind = v0Payload["kind"]?.jsonPrimitive?.contentOrNull?.toByteOrNull()
                ?: V1NumericConstants.TileKind.PLACEMENT
            val planRole = V1NumericConstants.PlanRole.EXECUTABLE
            val note = v0Payload["next_action"]?.jsonPrimitive?.contentOrNull
                ?: v0Payload["done_definition"]?.jsonPrimitive?.contentOrNull
            val payload = CreateTilePayload(
                kind = kind,
                title = title,
                description = note,
                externalId = null,
                planRole = planRole
            )
            val response = v1ApiClient.postCommand(
                path = "/v1/tiles",
                payload = payload,
                payloadSerializer = CreateTilePayload.serializer(),
                responseSerializer = CommandResponse.serializer()
            )
            response.toCoreAck()
        }.recover { error ->
            logFailure("tile.create", error)
            null
        }.getOrNull()
    }

    suspend fun dispatchTileDelete(tileId: String): CoreCommandAck? {
        return runCatching {
            v1ApiClient.deleteCommand(
                path = "/v1/tiles/$tileId",
                payload = ArchiveTilePayload(tileId),
                payloadSerializer = ArchiveTilePayload.serializer(),
            )
            CoreCommandResponse(accepted = true, requestId = null, commandId = null, eventIds = emptyList(), metadata = buildJsonObject { put("tileId", JsonPrimitive(tileId)) }, error = null)
        }.recover { error ->
            logFailure("tile.delete", error)
            null
        }.getOrNull()
    }

    suspend fun dispatchTileUpdate(
        tileId: String,
        v0Payload: JsonObject
    ): CoreCommandAck? {
        return runCatching {
            val title = v0Payload["title"]?.jsonPrimitive?.contentOrNull
            val description = v0Payload["description"]?.jsonPrimitive?.contentOrNull
                ?: v0Payload["note"]?.jsonPrimitive?.contentOrNull
            val color = v0Payload["color"]?.jsonPrimitive?.contentOrNull
            val icon = v0Payload["icon"]?.jsonPrimitive?.contentOrNull
            val externalId = v0Payload["external_id"]?.jsonPrimitive?.contentOrNull
                ?: v0Payload["externalId"]?.jsonPrimitive?.contentOrNull
            val payload = UpdateTilePayload(
                tileId = tileId,
                title = title,
                description = description,
                color = color,
                icon = icon,
                externalId = externalId
            )
            val response = v1ApiClient.postCommand(
                path = "/v1/tiles/$tileId/update",
                payload = payload,
                payloadSerializer = UpdateTilePayload.serializer(),
                responseSerializer = CommandResponse.serializer()
            )
            response.toCoreAck()
        }.recover { error ->
            logFailure("tile.update", error)
            null
        }.getOrNull()
    }

    suspend fun dispatchTileComplete(
        effectiveTileId: String?,
        nextTileId: String?,
        scope: String?
    ): CoreCommandAck? {
        if (effectiveTileId.isNullOrBlank()) return null
        return runCatching {
            val executionId = findExecutionIdForTile(effectiveTileId)
                ?: throw IllegalStateException("tile.complete: no active execution for tile $effectiveTileId")
            val finish = v1ApiClient.postCommand(
                path = "/v1/executions/$executionId/finish",
                payload = ExecutionFinishPayload(),
                payloadSerializer = ExecutionFinishPayload.serializer(),
                responseSerializer = CommandResponse.serializer()
            )
            if (!finish.toCoreAck().accepted) return@runCatching null
            val payload = SetTileLifecyclePayload(
                tileId = effectiveTileId,
                state = 2.toShort(),
                deferredUntil = null,
                completedAt = Instant.now().toString(),
                bumpExtend = false
            )
            val response = v1ApiClient.postCommand(
                path = "/v1/tiles/$effectiveTileId/complete",
                payload = payload,
                payloadSerializer = SetTileLifecyclePayload.serializer(),
                responseSerializer = CommandResponse.serializer()
            )
            response.toCoreAck()
        }.recover { error ->
            logFailure("tile.complete", error)
            null
        }.getOrNull()
    }

    suspend fun dispatchTileDefer(tileId: String, deferredUntil: String): CoreCommandAck? {
        return runCatching {
            val payload = SetTileLifecyclePayload(
                tileId = tileId,
                state = 1.toShort(),
                deferredUntil = deferredUntil,
                completedAt = null,
                bumpExtend = false
            )
            val response = v1ApiClient.postCommand(
                path = "/v1/tiles/$tileId/defer",
                payload = payload,
                payloadSerializer = SetTileLifecyclePayload.serializer(),
                responseSerializer = CommandResponse.serializer()
            )
            response.toCoreAck()
        }.recover { error ->
            logFailure("tile.defer", error)
            null
        }.getOrNull()
    }

    /**
     * v0 `tile.extend` did not carry a `tile_id`, so we have no way to address
     * the v1 `/v1/tiles/{id}/extend-phase` endpoint from the v0 payload. Step 4
     * leaves extend on the v0 fallback path; this method is kept for symmetry
     * but always returns null so `tryApplyCoreCommand` falls through to v0.
     */
    suspend fun dispatchTileExtend(deltaMin: Int): CoreCommandAck? {
        logFailure("tile.extend", IllegalStateException("no tile_id in v0 payload; falling back to v0"))
        return null
    }

    suspend fun dispatchMemoAttach(tileId: String?, body: String?): CoreCommandAck? {
        if (tileId.isNullOrBlank() || body.isNullOrBlank()) return null
        return runCatching {
            val payload = AttachMemoPayload(tileId = tileId, body = body)
            val response = v1ApiClient.postCommand(
                path = "/v1/tiles/$tileId/memos",
                payload = payload,
                payloadSerializer = AttachMemoPayload.serializer(),
                responseSerializer = CommandResponse.serializer()
            )
            response.toCoreAck()
        }.recover { error ->
            logFailure("memo.attach", error)
            null
        }.getOrNull()
    }

    // --- Step 5: commands that need a v1 lookup -------------------------

    /**
     * `tile.start` → `POST /v1/tiles/{id}/start`. The v1 endpoint requires
     * `plan_id`, `source`, `source_ref`, `baseline` — none of which v0
     * carried. We fetch the tile detail and pull `plan_id` from the
     * backend; if the backend hasn't attached a plan yet, we throw
     * `IllegalStateException` so the UI can surface a clear "tile has no
     * plan" error instead of silently dropping the request.
     *
     * `source` defaults to MANUAL (0) and `source_ref` to `null` because v0
     * never set either. `baseline` uses "now" for both ends as a fallback —
     * the v1 backend's PlanCompletion resolver will compute the canonical
     * span once a Plan exists, so this is only the wire bootstrap.
     */
    suspend fun dispatchTileStart(tileId: String, targetWorkMinutes: Long? = null): CoreCommandAck? {
        return runCatching {
            val tile = v1ApiClient.readTile(tileId)
            val planId = tile.planId
                ?: throw IllegalStateException("tile.start requires plan_id; v1 backend did not return one for tile $tileId")
            val start = Instant.now()
            val end = start.plusSeconds((targetWorkMinutes ?: 25L).coerceAtLeast(1L) * 60)
            val payload = StartTilePayload(
                tileId = tileId,
                planId = planId,
                source = V1NumericConstants.PlacementSource.MANUAL,
                sourceRef = SourceRefPayload.empty(),
                baseline = StartTileBaseline(span = PlacementSpanPayload(start = start.toString(), end = end.toString()))
            )
            val placement = v1ApiClient.postCommand(
                path = "/v1/tiles/$tileId/start",
                payload = payload,
                payloadSerializer = StartTilePayload.serializer(),
                responseSerializer = CommandResponse.serializer()
            )
            val placementId = placement.aggregate?.id
                ?: throw IllegalStateException("tile.start response missing placement aggregate for tile $tileId")
            val execution = v1ApiClient.postCommand(
                path = "/v1/placements/$placementId/executions",
                payload = StartExecutionPayload(placementId),
                payloadSerializer = StartExecutionPayload.serializer(),
                responseSerializer = CommandResponse.serializer()
            )
            val executionId = execution.aggregate?.id
                ?: throw IllegalStateException("start execution response missing execution aggregate for tile $tileId")
            executionIdsByTile[tileId] = executionId
            execution.toCoreAck()
        }.recover { error ->
            if (error is IllegalStateException) throw error
            logFailure("tile.start", error)
            null
        }.getOrNull()
    }

    /**
     * `tile.pause` → `POST /v1/executions/{id}/pause`. v1 pause is at the
     * Execution level, not the Tile, so we need to find the active execution
     * for the tile. We start with `listPlacements()` (cheap, single round
     * trip) and look for the most recent open placement for this tile. If
     * no placement exists we throw — the user can't pause something that
     * was never started in v1.
     */
    suspend fun dispatchTilePause(tileId: String): CoreCommandAck? {
        return runCatching {
            val executionId = findExecutionIdForTile(tileId)
                ?: throw IllegalStateException("tile.pause: no active execution for tile $tileId")
            val response = v1ApiClient.postNullCommand(
                path = "/v1/executions/$executionId/pause",
                responseSerializer = CommandResponse.serializer()
            )
            response.toCoreAck()
        }.recover { error ->
            if (error is IllegalStateException) throw error
            logFailure("tile.pause", error)
            null
        }.getOrNull()
    }

    /**
     * `tile.continue` → `POST /v1/executions/{id}/resume`. Same lookup path as
     * [dispatchTilePause] — there is no "tile.continue" endpoint at the Tile
     * level, only at the Execution level.
     */
    suspend fun dispatchTileContinue(tileId: String): CoreCommandAck? {
        return runCatching {
            val executionId = findExecutionIdForTile(tileId)
                ?: throw IllegalStateException("tile.continue: no active execution for tile $tileId")
            val response = v1ApiClient.postNullCommand(
                path = "/v1/executions/$executionId/resume",
                responseSerializer = CommandResponse.serializer()
            )
            response.toCoreAck()
        }.recover { error ->
            if (error is IllegalStateException) throw error
            logFailure("tile.continue", error)
            null
        }.getOrNull()
    }

    /**
     * `tile.reschedule` → `POST /v1/placements/{id}/changes`. v1 reschedule
     * is a Placement-level ChangeSet append. We look up the placement for
     * the tile (newest open span), then POST a PLACEMENT-layer ChangeSet
     * with two `Span` Change items: SPAN_START (group=5, part=0) and
     * SPAN_END (group=5, part=1).
     *
     * The full Rust `ChangeSet` struct carries `id` / `owner_id` / `target`
     * / `activation` / `source` / `source_ref` / `created_at` / `created_by`
     * fields we don't model in Kotlin. The server accepts whatever JSON
     * `serde_json::Value` deserializes against the full struct; per the
     * Android sends the same full audit envelope as the web client. This is
     * important: partial ChangeSets are not a stable public API contract.
     */
    suspend fun dispatchTileReschedule(
        tileId: String,
        startAt: String,
        endAt: String,
        ownerId: String,
    ): CoreCommandAck? {
        return runCatching {
            val placement = findPlacementForTile(tileId)
                ?: throw IllegalStateException("tile.reschedule: no placement for tile $tileId")
            val now = Instant.now().toString()
            val commandId = UUID.randomUUID().toString()
            val changeSet = buildJsonObject {
                put("id", JsonPrimitive(UUID.randomUUID().toString()))
                put("owner_id", JsonPrimitive(ownerId))
                put("target", buildJsonObject { put("Placement", JsonPrimitive(placement.placementId)) })
                put("layer", JsonPrimitive(V1NumericConstants.ChangeLayer.PLACEMENT))
                put("rank", JsonPrimitive(0))
                put("changes", kotlinx.serialization.json.JsonArray(listOf(
                    buildInstantChange(placement.placementId, 0, startAt),
                    buildInstantChange(placement.placementId, 1, endAt)
                )))
                put("activation", buildJsonObject {
                    put("when", JsonNull)
                    put("until", JsonNull)
                })
                put("revoked", JsonNull)
                put("source", JsonPrimitive(V1NumericConstants.ChangeSource.USER))
                put("source_ref", JsonNull)
                put("created_at", JsonPrimitive(now))
                put("created_by", buildJsonObject {
                    put("at", JsonPrimitive(now))
                    put("actor", JsonPrimitive(ownerId))
                    put("actor_kind", JsonPrimitive(V1NumericConstants.ActorKind.USER))
                    put("command_id", JsonPrimitive(commandId))
                })
            }
            val payload = AppendChangesPayload(
                placementId = placement.placementId,
                changeset = changeSet
            )
            val response = v1ApiClient.postCommand(
                path = "/v1/placements/${placement.placementId}/changes",
                payload = payload,
                payloadSerializer = AppendChangesPayload.serializer(),
                responseSerializer = CommandResponse.serializer()
            )
            response.toCoreAck()
        }.recover { error ->
            if (error is IllegalStateException) throw error
            logFailure("tile.reschedule", error)
            null
        }.getOrNull()
    }

    /**
     * `prompt.request` → `POST /v1/prompts`. The endpoint is NOT a
     * CommandRequest envelope — it takes `{kind, payload}` directly.
     * Use [postRawJson] to bypass the envelope.
     *
     * `kind` is sent as 0 — the v1 backend treats it as a smallint and
     * currently has no client-facing registry; future spec revisions will
     * publish the numeric prompt kinds and we'll update this.
     */
    suspend fun dispatchPromptRequest(tileId: String): CoreCommandAck? {
        return runCatching {
            val payload = CreatePromptRequestPayload(
                kind = 0,
                payload = buildJsonObject { put("tile_id", JsonPrimitive(tileId)) }
            )
            // The endpoint returns `{ "id": "<uuidv7>" }`, not a
            // CommandResponse envelope — so we wrap the result in a synthetic
            // CoreCommandAck with `accepted=true` (200 means the prompt was
            // accepted for processing) and put the new prompt id in metadata.
            val body = kotlinx.serialization.json.JsonObject(
                mapOf(
                    "kind" to kotlinx.serialization.json.JsonPrimitive(payload.kind.toInt()),
                    "payload" to payload.payload
                )
            )
            val response = v1ApiClient.postRawJson(
                path = "/v1/prompts",
                body = body,
                responseSerializer = kotlinx.serialization.json.JsonObject.serializer()
            )
            val promptId = response["id"]?.jsonPrimitive?.contentOrNull
            CoreCommandResponse(
                accepted = true,
                requestId = null,
                commandId = promptId,
                eventIds = emptyList(),
                metadata = buildJsonObject {
                    if (promptId != null) put("promptId", JsonPrimitive(promptId))
                    put("tileId", JsonPrimitive(tileId))
                },
                error = null
            )
        }.recover { error ->
            logFailure("prompt.request", error)
            null
        }.getOrNull()
    }

    /**
     * `prompt.respond_startup_recovery` → `POST /v1/prompts/startup-recovery`.
     * The handler accepts freeform JSON and just records the body plus (if
     * present) resolves `prompt_id` in `v1_prompt`. v0's payload shape
     * `{prompt_id, tile_id, action_id, stop_at?}` passes through verbatim.
     */
    suspend fun dispatchStartupRecoveryPrompt(
        promptId: String,
        tileId: String,
        actionId: String,
        stopAtIso: String?
    ): CoreCommandAck? {
        return runCatching {
            val body = buildJsonObject {
                put("prompt_id", JsonPrimitive(promptId))
                put("tile_id", JsonPrimitive(tileId))
                put("action_id", JsonPrimitive(actionId))
                if (!stopAtIso.isNullOrBlank()) put("stop_at", JsonPrimitive(stopAtIso))
            }
            val response = v1ApiClient.postRawJson(
                path = "/v1/prompts/startup-recovery",
                body = body,
                responseSerializer = kotlinx.serialization.json.JsonObject.serializer()
            )
            val accepted = response["accepted"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true
            CoreCommandResponse(
                accepted = accepted,
                requestId = null,
                commandId = null,
                eventIds = emptyList(),
                metadata = buildJsonObject {
                    put("promptId", JsonPrimitive(promptId))
                    put("tileId", JsonPrimitive(tileId))
                },
                error = null
            )
        }.recover { error ->
            logFailure("prompt.respond_startup_recovery", error)
            null
        }.getOrNull()
    }

    // --- helpers ------------------------------------------------------

    /**
     * Find the most recent placement for [tileId] via `listPlacements()`.
     * Returns `null` if the tile has no placement yet (the caller throws
     * `IllegalStateException`). Step 5 keeps this minimal — we don't try to
     * filter by status because v0 callers didn't carry that info and the
     * server's span-end vs span-start ordering lets us pick the freshest
     * open placement just by ordering.
     */
    private suspend fun findPlacementForTile(tileId: String): app.tastile.android.data.api.V1PlacementListItem? {
        val placements = v1ApiClient.listPlacements()
        return placements.firstOrNull { it.tileId == tileId }
    }

    private suspend fun findExecutionIdForTile(tileId: String): String? {
        executionIdsByTile[tileId]?.let { cached ->
            val execution = runCatching { v1ApiClient.readExecution(cached) }.getOrNull()
            if (execution?.tileId == tileId && execution.state in ACTIVE_EXECUTION_STATES) return cached
            // A failed or mismatched validation is never a reason to keep
            // offering Resume. Discard the volatile id before falling back
            // to the authoritative active-tile lookup.
            executionIdsByTile.remove(tileId)
        }
        val active = v1ApiClient.getActiveTile()
        val executionId = active?.takeIf { it.tileId == tileId }?.executionId ?: return null
        val execution = runCatching { v1ApiClient.readExecution(executionId) }.getOrNull() ?: return null
        return executionId.takeIf {
            execution.tileId == tileId && execution.state in ACTIVE_EXECUTION_STATES
        }?.also { executionIdsByTile[tileId] = it }
    }

    /**
     * Reads the current state for a known execution. The active-tile endpoint
     * only reports ACTIVE executions, while a cached paused execution remains
     * readable through its execution id; callers must not infer Resume when
     * this returns null.
     */
    suspend fun executionStateForTile(tileId: String): Int? = runCatching {
        val executionId = findExecutionIdForTile(tileId) ?: return@runCatching null
        v1ApiClient.readExecution(executionId)
            .takeIf { it.tileId == tileId }
            ?.state
    }.getOrNull()

    private fun buildInstantChange(
        placementId: String,
        groupPart: Int,
        instant: String,
    ): JsonObject = buildJsonObject {
        put("id", JsonPrimitive(UUID.randomUUID().toString()))
        put("key", buildJsonObject {
            put("group", JsonPrimitive(5))
            put("item", JsonPrimitive(placementId))
            put("part", JsonPrimitive(groupPart))
        })
        put("kind", JsonPrimitive(V1NumericConstants.ChangeKind.SET))
        put("value", buildJsonObject { put("Instant", JsonPrimitive(instant)) })
        put("merge", JsonPrimitive(V1NumericConstants.MergeMode.OVERRIDE))
        put("source", JsonPrimitive(V1NumericConstants.ChangeSource.USER))
        put("source_ref", JsonNull)
        put("rank", JsonPrimitive(0))
    }

    private fun logFailure(operation: String, e: Throwable) {
        Log.w(TAG, "v1 $operation failed: ${e.message}", e)
    }

    /**
     * Map a v1 `CommandResponse` (typed envelope with `result` byte 0/1/2) to
     * the v0 `CoreCommandAck` shape that callers already consume. `accepted`
     * is true iff the server returned `APPLIED` (0) or `ALREADY_APPLIED` (1);
     * `ACCEPTED` (2) means the command is queued but not yet applied, so the
     * dispatcher signals "not (yet) accepted" via `accepted=false` — callers
     * that need finer semantics should reach for the v1 layer directly.
     */
    private fun CommandResponse.toCoreAck(): CoreCommandAck {
        val accepted = result == V1NumericConstants.CommandResult.APPLIED ||
            result == V1NumericConstants.CommandResult.ALREADY_APPLIED
        val tileId = aggregate?.id
        return CoreCommandResponse(
            accepted = accepted,
            requestId = null,
            commandId = commandId,
            eventIds = emptyList(),
            metadata = buildJsonObject {
                if (tileId != null) put("tileId", JsonPrimitive(tileId))
                if (revision != null) put("revision", JsonPrimitive(revision))
            },
            error = null
        )
    }

    private companion object {
        private const val TAG = "V1CommandDispatcher"
        private val ACTIVE_EXECUTION_STATES = setOf(0, 1)
    }
}
