@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.tastile.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Partial typed mirror of the v1 SourceTile plan payload.
 *
 * Scope of this batch:
 *  - Removes `JsonObject` / `JsonArray` from the four top-level plan
 *    arrays (`references`, `planning.placement_rules`, `planning.nesting_rules`,
 *    `metrics`, `decisions`) by giving them concrete typed shapes.
 *  - `completion.root` remains `JsonElement` because the v1 Condition AST
 *    is an externally-tagged recursive union (4 Condition variants * 10
 *    Term variants * 5 sub-structures); a hand-rolled Kotlin mirror
 *    would need custom PolymorphicSerializers per variant and is
 *    documented as the next batch.
 *
 * Wire compatibility is verified by
 * `SourceTileApiContractTest.plan_payload_serializes_to_canonical_wire_form`
 * against the exact JSON the Core OpenAPI emits.
 */

@Serializable
data class SpanSchema(val start: String, val end: String)

@Serializable
data class RangeI64Schema(
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val min: Long? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val max: Long? = null,
)

@Serializable
data class TimeOfDaySchema(val hour: Int, val minute: Int, val second: Int, val nanos: Int)

@Serializable
data class DateRangeSchema(val start: String, val end: String)

@Serializable
data class ReferenceSchema(val id: String, val target: Int, val pick: PickSchema)

@Serializable
data class PickSchema(val kind: Short, val at: MomentSchema? = null)

@Serializable
data class MomentSchema(
    @SerialName("Absolute") val absolute: String? = null,
    @SerialName("Reference") val reference: MomentRefSchema? = null,
)

@Serializable
data class MomentRefSchema(val kind: Int, val target: JsonElement, val offset: Long)

@Serializable
data class ReferenceDefSchema(
    val id: String,
    val target: Short,
    val pick: PickSchema,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val `when`: JsonElement? = null,
)

@Serializable
data class ScopeSchema(val kind: Int, val parent: String? = null)

@Serializable
data class PlacementRuleEffectSchema(
    val kind: Int,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val record: Int? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val scope: ScopeSchema? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val score: Int? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val span: RangeI64Schema? = null,
)

@Serializable
data class PlacementRuleSchema(
    val id: String,
    val rank: Int,
    val effect: PlacementRuleEffectSchema,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val `when`: JsonElement? = null,
)

@Serializable
data class NestingRuleSchema(
    val id: String,
    val direction: Int,
    val rank: Int,
    val target: ReferenceSchema,
    val scope: ScopeSchema,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val `when`: JsonElement? = null,
)

@Serializable
data class SchedulingPlanningDefinitionSchema(
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    val placement_rules: List<PlacementRuleSchema> = emptyList(),
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    val nesting_rules: List<NestingRuleSchema> = emptyList(),
)

@Serializable
data class AggregateExprSchema(
    val kind: Int,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val quantifier: Int? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val reference: String? = null,
    val scope: Int,
    val source: Int,
)

@Serializable
data class TimeObservationSchema(
    val scope: Int,
    val source: Int,
    val aggregate: Int,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val quantifier: Int? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val reference: String? = null,
)

@Serializable
data class TimeRequirementSchema(
    val id: String,
    val observation: TimeObservationSchema,
    val required: RangeI64Schema,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val preferred: RangeI64Schema? = null,
)

@Serializable
data class MetricDefSchema(
    val id: String,
    val output: Int,
    /**
     * Scalar expression AST (Literal / Read / Aggregate / Operate / Choose)
     * is externally tagged. The Android client deliberately keeps this as
     * `JsonElement` because each variant carries different field shapes
     * (e.g. Read uses a TargetSelector registry, Operate takes a list
     * of operands). Custom PolymorphicSerializer implementation lives in
     * a follow-up batch.
     */
    val expression: JsonElement,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val limit: RangeI64Schema? = null,
)

@Serializable
data class DecisionCandidateSchema(
    val id: String,
    val rank: Int,
    /**
     * Candidate `when` and `effects` use the recursively-externally-tagged
     * Condition / ChangeValue schemas; kept as JsonElement for now.
     */
    val `when`: JsonElement,
    val effects: JsonElement,
)

@Serializable
data class FeedbackSelectorSchema(
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val all: String? = null,
    @SerialName("Feedback") val feedback: String? = null,
    @SerialName("Within") val within: Long? = null,
)

@Serializable
data class FeedbackMappingSchema(
    val id: String,
    val target: String,
    val key: String,
    val kind: Int,
    /**
     * Mapping `value` is a `ChangeValueSchema` (externally tagged enum).
     * Kept as JsonElement for now.
     */
    val value: JsonElement? = null,
)

@Serializable
data class FeedbackReuseRuleSchema(
    val id: String,
    val `when`: JsonElement,
    val source: FeedbackSelectorSchema,
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    val apply: List<FeedbackMappingSchema> = emptyList(),
)

@Serializable
data class InteractionViewSchema(
    val title: String,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val body: String? = null,
)

@Serializable
data class InteractionInputSchema(
    val id: String,
    val current: List<JsonElement> = emptyList(),
    val options: List<JsonElement> = emptyList(),
    val acceptance: Int,
)

@Serializable
data class InteractionNodeSchema(
    val id: String,
    val view: InteractionViewSchema,
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    val inputs: List<InteractionInputSchema> = emptyList(),
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    val children: List<JsonElement> = emptyList(),
)

@Serializable
data class DecisionDefSchema(
    val id: String,
    val observe: Int,
    val candidates: List<DecisionCandidateSchema> = emptyList(),
    val reuse: List<FeedbackReuseRuleSchema> = emptyList(),
    val dialog: InteractionNodeSchema,
    /**
     * Top-level decision `when` is a recursive ConditionSchema.
     * Kept as JsonElement until the polymorphic AST mirror lands.
     */
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val `when`: JsonElement? = null,
)

@Serializable
data class CompletionSchema(
    /**
     * Recursive Condition AST (All / Any / Not / Term with 10 term
     * variants). Kept as JsonElement to avoid a custom polymorphic
     * serializer; documented as the next-batch task.
     */
    val root: ConditionRef,
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    val time_requirements: List<TimeRequirementSchema> = emptyList(),
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    val tasks: List<JsonElement> = emptyList(),
)

@Serializable
data class SchedulePlanDefinitionPayloadTyped(
    val role: Short,
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    val references: List<ReferenceDefSchema> = emptyList(),
    val completion: CompletionSchema,
    val planning: SchedulingPlanningDefinitionSchema,
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    val metrics: List<MetricDefSchema> = emptyList(),
        @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    val decisions: List<DecisionDefSchema> = emptyList(),
)
