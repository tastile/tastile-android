package app.tastile.android.data.command

import android.util.Log
import app.tastile.android.core.CoreCommandAck
import app.tastile.android.core.CoreCommandResponse
import app.tastile.android.data.api.ArchiveTilePayload
import app.tastile.android.data.api.AttachMemoPayload
import app.tastile.android.data.api.CommandResponse
import app.tastile.android.data.api.CreateTilePayload
import app.tastile.android.data.api.SetTileLifecyclePayload
import app.tastile.android.data.api.UpdateTilePayload
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1NumericConstants
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes v0 freeform `TileRepository.tryApplyCoreCommand(...)` callers to typed
 * v1 command endpoints. For each v0 command type whose payload carries enough
 * information to construct a v1 payload, this dispatcher:
 *
 *   1. Builds the v1 typed payload from the v0 `JsonObject`.
 *   2. Posts via `V1ApiClient.postCommand` (or `deleteCommand` for archive).
 *   3. Maps the v1 [CommandResponse] back to the v0 [CoreCommandAck] shape that
 *      `TileRepository` already understands. The dispatcher returns `null` on
 *      any v1 error so callers can fall through to the v0 local runtime for
 *      non-migrated command types.
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
 *   - tile.extend: NOT routed to v1 — the v0 payload carried no `tile_id`, so
 *     we can't address `/v1/tiles/{id}/extend-phase`. Step 4 returns null and
 *     callers fall through to v0 (`coreRuntimeService.applyCommand`).
 *   - memo.attach: v0 `text` → v1 `body`.
 */
@Singleton
class V1CommandDispatcher @Inject constructor(
    private val v1ApiClient: V1ApiClient
) {
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
                color = null,
                icon = null,
                externalId = null,
                planRole = planRole
            )
            val response = v1ApiClient.postCommand(
                path = "/v1/tiles",
                commandKind = "CreateTile",
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
            val response = v1ApiClient.deleteCommand(
                path = "/v1/tiles/$tileId",
                responseSerializer = CommandResponse.serializer()
            )
            response.toCoreAck()
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
                commandKind = "UpdateTile",
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
            val payload = SetTileLifecyclePayload(
                tileId = effectiveTileId,
                state = 2.toShort(),
                deferredUntil = null,
                completedAt = Instant.now().toString(),
                bumpExtend = false
            )
            val response = v1ApiClient.postCommand(
                path = "/v1/tiles/$effectiveTileId/complete",
                commandKind = "SetTileLifecycle",
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

    suspend fun dispatchTileDefer(
        tileId: String,
        reason: String?,
        minutes: Int?
    ): CoreCommandAck? {
        return runCatching {
            val mins = minutes ?: 60
            val deferredUntil = Instant.now().plusSeconds(mins * 60L).toString()
            val payload = SetTileLifecyclePayload(
                tileId = tileId,
                state = 1.toShort(),
                deferredUntil = deferredUntil,
                completedAt = null,
                bumpExtend = false
            )
            val response = v1ApiClient.postCommand(
                path = "/v1/tiles/$tileId/defer",
                commandKind = "SetTileLifecycle",
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
                commandKind = "AttachMemo",
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

    // --- helpers ------------------------------------------------------

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
    }
}
