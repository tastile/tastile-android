@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.tastile.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault

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
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val description: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val color: String? = "#3b82f6",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val icon: String? = "check-circle",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("external_id") val externalId: String? = null,
    @SerialName("plan_role") val planRole: Byte,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("owner_subject_id") val ownerSubjectId: String? = null,
    @SerialName("frame_rule") val frameRule: FrameRulePayload? = null,
)

@Serializable
data class FrameRulePayload(
    val id: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: FrameRuleConditionPayload? = null,
    val rank: Int,
    val generator: FrameRuleGeneratorPayload,
)

/** Web's externally tagged `FrameGenerator` form. The create panel uses `Step`. */
@Serializable
data class FrameRuleGeneratorPayload(
    @SerialName("Step") val step: FrameRuleStepPayload,
)

@Serializable
data class FrameRuleStepPayload(
    val step: Long,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val origin: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val bounds: PlacementSpanPayload? = null,
)

/** Frame-rule creation currently sends `active: null`; keep that null typed. */
@Serializable
data class FrameRuleConditionPayload(
    val kind: String,
    val value: kotlinx.serialization.json.JsonElement,
)

@Serializable
data class SourceRefPayload(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val created: SourceStampPayload? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val recurring: SourceVersionRefPayload? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val flow: SourceVersionRefPayload? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val frame: SourceVersionRefPayload? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val proposal: SourceProposalKeyPayload? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("source_text") val sourceText: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("external_id") val externalId: String? = null,
) {
    companion object {
        fun empty() = SourceRefPayload()
    }
}

@Serializable
data class SourceStampPayload(
    val at: String,
    val actor: String,
    @SerialName("actor_kind") val actorKind: Byte,
    @SerialName("command_id") val commandId: String,
)

@Serializable
data class SourceVersionRefPayload(
    val id: String,
    val revision: Long,
)

@Serializable
data class SourceProposalKeyPayload(
    @SerialName("producer_id") val producerId: String,
    @SerialName("local_id") val localId: String,
)

/** Exact body for `POST /v1/tiles/{tileId}/plan`. */
@Serializable
data class SetPlanPayload(
    @SerialName("tile_id") val tileId: String,
    val role: Byte,
    val references: kotlinx.serialization.json.JsonElement,
    val completion: kotlinx.serialization.json.JsonElement,
    val planning: kotlinx.serialization.json.JsonElement,
    val metrics: kotlinx.serialization.json.JsonArray,
    val decisions: kotlinx.serialization.json.JsonArray,
)

/** Exact body for `POST /v1/placements`. */
@Serializable
data class CreatePlacementPayload(
    @SerialName("tile_id") val tileId: String,
    @SerialName("plan_id") val planId: String,
    val source: Byte,
    @SerialName("source_ref") val sourceRef: SourceRefPayload,
    val baseline: PlacementBaselinePayload,
)

@Serializable
data class PlacementBaselinePayload(
    val span: PlacementSpanPayload,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val inside: kotlinx.serialization.json.JsonObject? = null,
)

@Serializable
data class PlacementSpanPayload(
    val start: String,
    val end: String,
)

/** Exact body for recurring-frame materialization. */
@Serializable
data class MaterializeRecurringPayload(
    @SerialName("recurring_id") val recurringId: String,
    @SerialName("frame_rule_id") val frameRuleId: String,
    @SerialName("range_start") val rangeStart: String,
    @SerialName("range_end") val rangeEnd: String,
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
    @SerialName("source_ref") val sourceRef: SourceRefPayload,
    val baseline: StartTileBaseline
)

@Serializable
data class StartTileBaseline(
    val span: PlacementSpanPayload,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val inside: kotlinx.serialization.json.JsonObject? = null,
)

@Serializable
data class StartExecutionPayload(@SerialName("placement_id") val placementId: String)

@Serializable
data class ClosePlacementPayload(@SerialName("placement_id") val placementId: String)

@Serializable
data class ExecutionFinishPayload(
    val kind: Int = 0,
    val note: String? = null,
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

@Serializable
data class SourceSchedulePayload(
    @SerialName("required_duration_ms") val requiredDurationMs: Long,
    val generation: SourceGenerationPayload,
    val window: SourceWindowPayload,
    @SerialName("split_policy") val splitPolicy: SourceSplitPolicyPayload,
    val priority: Int,
)

/** Canonical body shared by SourceTile create and update commands. */
@Serializable
data class SourceTileWritePayload(
    val tile: SourceTileDefinitionPayload,
    val plan: SchedulePlanDefinitionPayloadTyped,
    val flows: List<kotlinx.serialization.json.JsonObject>,
    val schedule: SourceSchedulePayload,
    val horizon: PlacementSpanPayload,
)

@Serializable
data class SourceTileDefinitionPayload(
    val title: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val description: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val color: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val icon: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("external_id") val externalId: String? = null,
)

/**
 * Plan sub-definitions are a recursive Core AST. Android deliberately does
 * not fabricate a partial AST: callers must supply validated Core wire
 * objects. This preserves fields added by Core without silently dropping
 * them, while rejecting the only incomplete shapes Android can detect.
 */
@Serializable
data class SourcePlanDefinitionPayload(
    val role: Short,
    val references: kotlinx.serialization.json.JsonArray,
    val completion: kotlinx.serialization.json.JsonObject,
    val planning: kotlinx.serialization.json.JsonObject,
    val metrics: kotlinx.serialization.json.JsonArray,
    val decisions: kotlinx.serialization.json.JsonArray,
) {
    init {
        require(completion["root"] is kotlinx.serialization.json.JsonObject) {
            "SourceTile plan completion must include the Core condition root"
        }
        require(planning["placement_rules"] is kotlinx.serialization.json.JsonArray) {
            "SourceTile plan planning.placement_rules must be an array"
        }
        require(planning["nesting_rules"] is kotlinx.serialization.json.JsonArray) {
            "SourceTile plan planning.nesting_rules must be an array"
        }
    }
}

@Serializable
data class SourceGenerationPayload(
    val kind: Short,
    val at: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("interval_ms") val intervalMs: Long? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("weekday_mask") val weekdayMask: Byte? = null,
    @SerialName("date_range_start") val dateRangeStart: String? = null,
    @SerialName("date_range_end") val dateRangeEnd: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("excluded_dates") val excludedDates: List<String> = emptyList(),
)

@Serializable
data class SourceWindowPayload(
    @SerialName("start_offset_ms") val startOffsetMs: Long,
    @SerialName("end_offset_ms") val endOffsetMs: Long,
)

@Serializable
data class SourceSplitPolicyPayload(
    val kind: Short,
    @SerialName("min_segment_ms") val minSegmentMs: Long? = null,
    @SerialName("max_segment_ms") val maxSegmentMs: Long? = null,
    @SerialName("max_segments") val maxSegments: Int? = null,
)

@Serializable
data class ReflowSourceTilePayload(val range: PlacementSpanPayload)
