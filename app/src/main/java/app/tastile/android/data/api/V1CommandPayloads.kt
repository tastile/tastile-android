package app.tastile.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed payload shapes for the v1 command endpoints wired in Macro Step 4.
 * They mirror the Rust definitions in `tastile-core/crates/v1/domain/src/command.rs`.
 *
 * For optional updates (`title`, `description`, `color`, `icon`, `external_id`),
 * `null` means "leave unchanged". This is the Kotlin convention used by Step 4;
 * clearing an existing value is not currently expressible from v0 callers and
 * would be a follow-up if needed. See v0->v1 field mapping notes in
 * `V1CommandDispatcher.kt`.
 *
 * `SetTileLifecyclePayload.bumpExtend=true` carries the `bump_extend` server flag
 * for the extend-phase flow. Other tiles (state=0..2 with `deferredUntil` /
 * `completedAt`) leave it `false`.
 */
@Serializable
data class CreateTilePayload(
    val kind: Byte,
    val title: String,
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
    @SerialName("external_id") val externalId: String? = null,
    @SerialName("plan_role") val planRole: Byte
)

@Serializable
data class ArchiveTilePayload(
    @SerialName("tile_id") val tileId: String
)

@Serializable
data class UpdateTilePayload(
    @SerialName("tile_id") val tileId: String,
    val title: String? = null,
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
    @SerialName("external_id") val externalId: String? = null
)

@Serializable
data class SetTileLifecyclePayload(
    @SerialName("tile_id") val tileId: String,
    val state: Short? = null,
    @SerialName("deferred_until") val deferredUntil: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("bump_extend") val bumpExtend: Boolean = false
)

@Serializable
data class AttachMemoPayload(
    @SerialName("tile_id") val tileId: String,
    val body: String
)

/**
 * Mirrors `domain::StartTilePayload` in `crates/v1/domain/src/command.rs`.
 *
 * `source` / `source_ref` / `baseline` are emitted as numeric constants (or
 * `null` for unknown) per v1/10 §2 — strings are forbidden.
 *
 * Macro Step 5 builds these by reading the tile via `V1ApiClient.readTile`
 * and asking the v1 backend for the canonical `plan_id`. If the backend
 * doesn't return one, we throw `IllegalStateException` because v0's
 * `tile.start` carried no plan context at all and we cannot synthesize one.
 */
@Serializable
data class StartTilePayload(
    @SerialName("tile_id") val tileId: String,
    @SerialName("plan_id") val planId: String,
    val source: Byte,
    @SerialName("source_ref") val sourceRef: String? = null,
    val baseline: StartTileBaseline
)

@Serializable
data class StartTileBaseline(
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String
)

/**
 * `domain::PauseExecutionPayload` and `domain::ResumeExecutionPayload` both
 * hold only `execution_id`.  We keep two distinct Kotlin classes so the
 * `command_kind` envelope field stays unambiguous.
 */
@Serializable
data class PauseExecutionPayload(
    @SerialName("execution_id") val executionId: String
)

@Serializable
data class ResumeExecutionPayload(
    @SerialName("execution_id") val executionId: String
)

/**
 * `domain::AppendChangesPayload`. The full Rust `ChangeSet` is intentionally
 * NOT mirrored here — the only field Step 5 needs to set for `tile.reschedule`
 * is `placement_id` plus a `JsonObject` ChangeSet body. The dispatcher builds
 * the ChangeSet inline (two `Span` Change items at the PLACEMENT layer with
 * rank=USER) so we don't have to hand-translate the dozen-odd Rust structs
 * (`ChangeKey` / `ChangeSourceRef` / `Activation` / `Stamp`) that Step 5 never
 * touches.
 */
@Serializable
data class AppendChangesPayload(
    @SerialName("placement_id") val placementId: String,
    val changeset: kotlinx.serialization.json.JsonObject
)

/**
 * `domain::CreatePromptRequest` (from `handlers/commands.rs::create_prompt`).
 * The handler stores whatever JSON `payload` carries, so we keep it as
 * `JsonObject` rather than locking down a fixed shape.
 *
 * NOTE: this endpoint does NOT use the standard `CommandRequest<T>` envelope.
 * `V1ApiClient.postRawJson` is used to bypass the envelope wrapping. The
 * numeric `kind` is whatever the v1 backend expects for a "tile_specific"
 * prompt — the v1 backend stores it as a smallint (no validation per
 * `commands.rs::create_prompt`), so callers may use 0 as a safe default
 * until a registry is published.
 */
@Serializable
data class CreatePromptRequestPayload(
    val kind: Short,
    val payload: kotlinx.serialization.json.JsonObject
)
