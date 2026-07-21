@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.tastile.android.data.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Polymorphic mirror of the v1/05 Condition + Term + leaf sub-enums,
 * serialised via `JsonEncoder.encodeJsonElement()` to produce the exact
 * externally-tagged wire JSON that Core emits.
 *
 * Wire shape examples:
 *   Condition::All(List<...>)   ->  {"All":[...]}
 *   Condition::Not(ConditionRef) ->  {"Not":<recursed>}
 *   Condition::Term(TermRef)     ->  {"Term":{"Calendar":{...}}}
 *   Term::Calendar(...)          ->  {"Calendar":{...}}
 *   MomentTarget::Reference(uuid)->  {"Reference":"<uuid>"}
 *
 * The trick: each polymorphic level (Condition, Term, MomentTarget,
 * MomentComparison, FactComparison, MetricComparison, LifeTarget) has
 * a custom `KSerializer` that emits a raw `JsonObject` directly via
 * `JsonEncoder.encodeJsonElement(JsonObject(...))` rather than going
 * through the structure-encoder / element-index machinery.
 *
 * Each variant is still a normal `@Serializable data class` for the
 * default value-based serialisation when nested in the raw `JsonObject`
 * payload -- we just hand-build the wrapping `JsonObject` ourselves.
 */

// ============ Condition AST ============

sealed class ConditionSchema

@Serializable
data class ConditionAll(val items: List<ConditionRef>) : ConditionSchema()

@Serializable
data class ConditionAny(val items: List<ConditionRef>) : ConditionSchema()

@Serializable
data class ConditionNot(val value: ConditionRef) : ConditionSchema()

@Serializable
data class ConditionTerm(val value: TermRef) : ConditionSchema()

@Serializable(with = ConditionRefSerializer::class)
data class ConditionRef(val inner: ConditionSchema)

object ConditionRefSerializer : KSerializer<ConditionRef> {
    private val d = buildClassSerialDescriptor("ConditionRef")
    override val descriptor: SerialDescriptor = d

    override fun serialize(encoder: Encoder, value: ConditionRef) {
        val je = encoder as JsonEncoder
        val payload: JsonElement = when (val v = value.inner) {
            is ConditionAll -> wrapArray("All", v.items)
            is ConditionAny -> wrapArray("Any", v.items)
            is ConditionNot -> wrapSingleRef("Not", v.value)
            is ConditionTerm -> wrapSingleTerm("Term", v.value)
        }
        je.encodeJsonElement(payload)
    }

    override fun deserialize(decoder: Decoder): ConditionRef {
        val je = decoder as JsonDecoder
        return ConditionRef(decodeCondition(je.decodeJsonElement()))
    }

    private fun wrapSingleRef(key: String, ref: ConditionRef): JsonObject = JsonObject(
        mapOf(key to Json.encodeToJsonElement(this, ref))
    )

    private fun wrapSingleTerm(key: String, ref: TermRef): JsonObject = JsonObject(
        mapOf(key to Json.encodeToJsonElement(TermRefSerializer, ref))
    )

    private fun wrapArray(key: String, items: List<ConditionRef>): JsonObject = JsonObject(
        mapOf(key to JsonArray(items.map { Json.encodeToJsonElement(this, it) }))
    )
}

private fun decodeCondition(element: JsonElement): ConditionSchema {
    val obj = element as JsonObject
    val (key, value) = obj.entries.first()
    val v: JsonElement = value
    val result: ConditionSchema = when (key) {
        "All" -> {
            val arr = v as JsonArray
            val items: List<ConditionRef> = arr.map { ConditionRef(decodeCondition(it)) }
            ConditionAll(items)
        }
        "Any" -> {
            val arr = v as JsonArray
            val items: List<ConditionRef> = arr.map { ConditionRef(decodeCondition(it)) }
            ConditionAny(items)
        }
        "Not" -> ConditionNot(ConditionRef(decodeCondition(v as JsonObject)))
        "Term" -> ConditionTerm(TermRef(decodeTerm(v)))
        else -> throw IllegalStateException("Unknown Condition variant: " + key)
    }
    return result
}



// ============ Term AST ============

sealed class TermSchema

@Serializable
data class TermCalendar(
    val weekday_mask: Short,
    val time_start: TimeOfDay? = null,
    val time_end: TimeOfDay? = null,
    val holiday_kind: Short,
    val date_range: DateRange? = null,
    val offset_min: Short,
) : TermSchema()

@Serializable
data class TermMoment(
    val point: Short,
    val target: MomentTarget,
    val offset: Long,
    val comparison: MomentComparison,
) : TermSchema()

@Serializable
data class TermRelation(
    val reference_id: String,
    val relation: Int,
    val window_kind: RelationWindowKind,
) : TermSchema()

@Serializable
data class TermGap(
    val scope: ScopeAst,
    val left_anchor: AnchorSelector,
    val right_anchor: AnchorSelector,
    val size: RangeI64? = null,
) : TermSchema()

@Serializable
data class TermRequirement(val time_requirement: String, val state: RequirementState) : TermSchema()

@Serializable
data class TermTask(val task_id: String, val state: TaskStateKind) : TermSchema()

@Serializable
data class TermFact(val key: String, val comparison: FactComparison) : TermSchema()

@Serializable
data class TermMetric(val metric_id: String, val comparison: MetricComparison) : TermSchema()

@Serializable
data class TermFeedback(val feedback_txn: String, val state: FeedbackState) : TermSchema()

@Serializable
data class TermLife(val target: LifeTarget, val state: LifeState) : TermSchema()

@Serializable(with = TermRefSerializer::class)
data class TermRef(val inner: TermSchema)

object TermRefSerializer : KSerializer<TermRef> {
    private val d = buildClassSerialDescriptor("TermRef")
    override val descriptor: SerialDescriptor = d

    override fun serialize(encoder: Encoder, value: TermRef) {
        val je = encoder as JsonEncoder
        val payload: JsonElement = when (val v = value.inner) {
            is TermCalendar -> wrap("Calendar", Json.encodeToJsonElement(TermCalendar.serializer(), v))
            is TermMoment -> wrap("Moment", Json.encodeToJsonElement(TermMoment.serializer(), v))
            is TermRelation -> wrap("Relation", Json.encodeToJsonElement(TermRelation.serializer(), v))
            is TermGap -> wrap("Gap", Json.encodeToJsonElement(TermGap.serializer(), v))
            is TermRequirement -> wrap("Requirement", Json.encodeToJsonElement(TermRequirement.serializer(), v))
            is TermTask -> wrap("Task", Json.encodeToJsonElement(TermTask.serializer(), v))
            is TermFact -> wrap("Fact", Json.encodeToJsonElement(TermFact.serializer(), v))
            is TermMetric -> wrap("Metric", Json.encodeToJsonElement(TermMetric.serializer(), v))
            is TermFeedback -> wrap("Feedback", Json.encodeToJsonElement(TermFeedback.serializer(), v))
            is TermLife -> wrap("Life", Json.encodeToJsonElement(TermLife.serializer(), v))
        }
        je.encodeJsonElement(payload)
    }

    override fun deserialize(decoder: Decoder): TermRef {
        val je = decoder as JsonDecoder
        return TermRef(decodeTerm(je.decodeJsonElement()))
    }

    private fun wrap(key: String, payload: JsonElement): JsonObject =
        JsonObject(mapOf(key to payload))
}

private fun decodeTerm(element: JsonElement): TermSchema {
    val obj = element as JsonObject
    val (key, value) = obj.entries.first()
    val v: JsonElement = value
    val result: TermSchema = when (key) {
        "Calendar" -> Json.decodeFromJsonElement(TermCalendar.serializer(), v) as TermSchema
        "Moment" -> Json.decodeFromJsonElement(TermMoment.serializer(), v) as TermSchema
        "Relation" -> Json.decodeFromJsonElement(TermRelation.serializer(), v) as TermSchema
        "Gap" -> Json.decodeFromJsonElement(TermGap.serializer(), v) as TermSchema
        "Requirement" -> Json.decodeFromJsonElement(TermRequirement.serializer(), v) as TermSchema
        "Task" -> Json.decodeFromJsonElement(TermTask.serializer(), v) as TermSchema
        "Fact" -> Json.decodeFromJsonElement(TermFact.serializer(), v) as TermSchema
        "Metric" -> Json.decodeFromJsonElement(TermMetric.serializer(), v) as TermSchema
        "Feedback" -> Json.decodeFromJsonElement(TermFeedback.serializer(), v) as TermSchema
        "Life" -> Json.decodeFromJsonElement(TermLife.serializer(), v) as TermSchema
        else -> throw IllegalStateException("Unknown Term variant: " + key)
    }
    return result
}

// ============ Leaf types shared by the AST ============

@Serializable
data class TimeOfDay(val hour: Int, val minute: Int, val second: Int, val nanos: Int)

@Serializable
data class DateRange(val start: String, val end: String)

@Serializable
data class RangeI64(
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val min: Long? = null,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val max: Long? = null,
)

@Serializable(with = RelationWindowKindSerializer::class)
data class RelationWindowKind(val value: String)

@Serializable
data class ScopeAst(val kind: Int, @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val parent: String? = null)

@Serializable(with = AnchorSelectorSerializer::class)
data class AnchorSelector(
    val `when`: ConditionRef,
    val pick: PickAst,
)

object AnchorSelectorSerializer : KSerializer<AnchorSelector> {
    private val d = buildClassSerialDescriptor("AnchorSelector")
    override val descriptor: SerialDescriptor = d
    override fun serialize(encoder: Encoder, value: AnchorSelector) {
        val je = encoder as JsonEncoder
        val obj = JsonObject(
            mapOf(
                "when" to Json.encodeToJsonElement(ConditionRefSerializer, value.`when`),
                "pick" to Json.encodeToJsonElement(PickAst.serializer(), value.pick),
            )
        )
        je.encodeJsonElement(obj)
    }
    override fun deserialize(decoder: Decoder): AnchorSelector {
        val je = decoder as JsonDecoder
        val el = je.decodeJsonElement() as JsonObject
        return AnchorSelector(
            `when` = Json.decodeFromJsonElement(ConditionRefSerializer, el["when"]!!),
            pick = Json.decodeFromJsonElement(PickAst.serializer(), el["pick"]!!),
        )
    }
}

@Serializable
data class PickAst(
    val kind: Short,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS) val at: MomentRef? = null,
)

@Serializable(with = MomentRefSerializer::class)
data class MomentRef(val inner: MomentAst)

sealed class MomentAst

@Serializable
data class MomentAstAbsolute(val at: String) : MomentAst()

@Serializable
data class MomentAstReference(val kind: Int, val target: JsonElement, val offset: Long) : MomentAst()

object MomentRefSerializer : KSerializer<MomentRef> {
    private val d = buildClassSerialDescriptor("MomentRef")
    override val descriptor: SerialDescriptor = d
    override fun serialize(encoder: Encoder, value: MomentRef) {
        val je = encoder as JsonEncoder
        val payload: JsonElement = when (val v = value.inner) {
            is MomentAstAbsolute -> JsonObject(mapOf("Absolute" to JsonPrimitive(v.at)))
            is MomentAstReference -> JsonObject(mapOf("Reference" to Json.encodeToJsonElement(MomentAstReference.serializer(), v)))
        }
        je.encodeJsonElement(payload)
    }
    override fun deserialize(decoder: Decoder): MomentRef {
        val je = decoder as JsonDecoder
        return MomentRef(decodeMoment(je.decodeJsonElement()))
    }
}

private fun decodeMoment(element: JsonElement): MomentAst {
    val obj = element as JsonObject
    val (key, value) = obj.entries.first()
    return when (key) {
        "Absolute" -> MomentAstAbsolute((value as JsonPrimitive).content)
        "Reference" -> Json.decodeFromJsonElement(MomentAstReference.serializer(), value)
        else -> error("Unknown MomentAst variant: " + key)
    }
}

@Serializable(with = MomentTargetSerializer::class)
sealed class MomentTarget

@Serializable @SerialName("Context")
class MomentTargetContext(val kind: Int) : MomentTarget()

@Serializable @SerialName("Reference")
class MomentTargetReference(val id: String) : MomentTarget()

@Serializable @SerialName("Placement")
class MomentTargetPlacement(val id: String) : MomentTarget()

@Serializable @SerialName("Execution")
class MomentTargetExecution(val id: String) : MomentTarget()

@Serializable @SerialName("Frame")
class MomentTargetFrame(val id: String) : MomentTarget()

object MomentTargetSerializer : KSerializer<MomentTarget> {
    private val d = buildClassSerialDescriptor("MomentTarget")
    override val descriptor: SerialDescriptor = d
    override fun serialize(encoder: Encoder, value: MomentTarget) {
        val je = encoder as JsonEncoder
        val (key, v) = when (value) {
            is MomentTargetContext -> "Context" to JsonPrimitive(value.kind)
            is MomentTargetReference -> "Reference" to JsonPrimitive(value.id)
            is MomentTargetPlacement -> "Placement" to JsonPrimitive(value.id)
            is MomentTargetExecution -> "Execution" to JsonPrimitive(value.id)
            is MomentTargetFrame -> "Frame" to JsonPrimitive(value.id)
        }
        je.encodeJsonElement(JsonObject(mapOf(key to v)))
    }
    override fun deserialize(decoder: Decoder): MomentTarget {
        val je = decoder as JsonDecoder
        val obj = je.decodeJsonElement() as JsonObject
        val (key, value) = obj.entries.first()
        val v = (value as JsonPrimitive).content
        return when (key) {
            "Context" -> MomentTargetContext(v.toInt())
            "Reference" -> MomentTargetReference(v)
            "Placement" -> MomentTargetPlacement(v)
            "Execution" -> MomentTargetExecution(v)
            "Frame" -> MomentTargetFrame(v)
            else -> error("Unknown MomentTarget variant: " + key)
        }
    }
}

@Serializable(with = MomentComparisonSerializer::class)
sealed class MomentComparison

@Serializable @SerialName("At")
class MomentComparisonAt(val at: String) : MomentComparison()

@Serializable @SerialName("Before")
class MomentComparisonBefore(val target: MomentTarget) : MomentComparison()

@Serializable @SerialName("After")
class MomentComparisonAfter(val target: MomentTarget) : MomentComparison()

@Serializable @SerialName("Within")
class MomentComparisonWithin(val range: RangeI64) : MomentComparison()

object MomentComparisonSerializer : KSerializer<MomentComparison> {
    private val d = buildClassSerialDescriptor("MomentComparison")
    override val descriptor: SerialDescriptor = d
    override fun serialize(encoder: Encoder, value: MomentComparison) {
        val je = encoder as JsonEncoder
        val (key, v) = when (value) {
            is MomentComparisonAt -> "At" to JsonPrimitive(value.at)
            is MomentComparisonBefore -> "Before" to Json.encodeToJsonElement(MomentTargetSerializer, value.target)
            is MomentComparisonAfter -> "After" to Json.encodeToJsonElement(MomentTargetSerializer, value.target)
            is MomentComparisonWithin -> "Within" to Json.encodeToJsonElement(RangeI64.serializer(), value.range)
        }
        je.encodeJsonElement(JsonObject(mapOf(key to v)))
    }
    override fun deserialize(decoder: Decoder): MomentComparison {
        val je = decoder as JsonDecoder
        val obj = je.decodeJsonElement() as JsonObject
        val (key, value) = obj.entries.first()
        return when (key) {
            "At" -> MomentComparisonAt((value as JsonPrimitive).content)
            "Before" -> MomentComparisonBefore(Json.decodeFromJsonElement(MomentTargetSerializer, value))
            "After" -> MomentComparisonAfter(Json.decodeFromJsonElement(MomentTargetSerializer, value))
            "Within" -> MomentComparisonWithin(Json.decodeFromJsonElement(RangeI64.serializer(), value))
            else -> error("Unknown MomentComparison variant: " + key)
        }
    }
}

@Serializable(with = RequirementStateSerializer::class)
data class RequirementState(val value: String)

@Serializable(with = TaskStateKindSerializer::class)
data class TaskStateKind(val value: String)

@Serializable(with = FactComparisonSerializer::class)
sealed class FactComparison

@Serializable @SerialName("Equal")
class FactComparisonEqual(val s: String) : FactComparison()

@Serializable @SerialName("NotEqual")
class FactComparisonNotEqual(val s: String) : FactComparison()

@Serializable @SerialName("GreaterThan")
class FactComparisonGreaterThan(val v: Long) : FactComparison()

@Serializable @SerialName("LessThan")
class FactComparisonLessThan(val v: Long) : FactComparison()

@Serializable @SerialName("InRange")
class FactComparisonInRange(val range: RangeI64) : FactComparison()

@Serializable @SerialName("Exists")
object FactComparisonExists : FactComparison()

@Serializable @SerialName("Missing")
object FactComparisonMissing : FactComparison()

object FactComparisonSerializer : KSerializer<FactComparison> {
    private val d = buildClassSerialDescriptor("FactComparison")
    override val descriptor: SerialDescriptor = d
    override fun serialize(encoder: Encoder, value: FactComparison) {
        val je = encoder as JsonEncoder
        val (key, v) = when (value) {
            is FactComparisonEqual -> "Equal" to JsonPrimitive(value.s)
            is FactComparisonNotEqual -> "NotEqual" to JsonPrimitive(value.s)
            is FactComparisonGreaterThan -> "GreaterThan" to JsonPrimitive(value.v)
            is FactComparisonLessThan -> "LessThan" to JsonPrimitive(value.v)
            is FactComparisonInRange -> "InRange" to Json.encodeToJsonElement(RangeI64.serializer(), value.range)
            FactComparisonExists -> "Exists" to JsonObject(emptyMap())
            FactComparisonMissing -> "Missing" to JsonObject(emptyMap())
        }
        je.encodeJsonElement(JsonObject(mapOf(key to v)))
    }
    override fun deserialize(decoder: Decoder): FactComparison {
        val je = decoder as JsonDecoder
        val obj = je.decodeJsonElement() as JsonObject
        val (key, value) = obj.entries.first()
        return when (key) {
            "Equal" -> FactComparisonEqual((value as JsonPrimitive).content)
            "NotEqual" -> FactComparisonNotEqual((value as JsonPrimitive).content)
            "GreaterThan" -> FactComparisonGreaterThan((value as JsonPrimitive).content.toLong())
            "LessThan" -> FactComparisonLessThan((value as JsonPrimitive).content.toLong())
            "InRange" -> FactComparisonInRange(Json.decodeFromJsonElement(RangeI64.serializer(), value))
            "Exists" -> FactComparisonExists
            "Missing" -> FactComparisonMissing
            else -> error("Unknown FactComparison variant: " + key)
        }
    }
}

@Serializable(with = MetricComparisonSerializer::class)
sealed class MetricComparison

@Serializable @SerialName("Equal")
class MetricComparisonEqual(val v: Long) : MetricComparison()

@Serializable @SerialName("NotEqual")
class MetricComparisonNotEqual(val v: Long) : MetricComparison()

@Serializable @SerialName("GreaterThan")
class MetricComparisonGreaterThan(val v: Long) : MetricComparison()

@Serializable @SerialName("LessThan")
class MetricComparisonLessThan(val v: Long) : MetricComparison()

@Serializable @SerialName("InRange")
class MetricComparisonInRange(val range: RangeI64) : MetricComparison()

@Serializable @SerialName("Exists")
object MetricComparisonExists : MetricComparison()

@Serializable @SerialName("Missing")
object MetricComparisonMissing : MetricComparison()

object MetricComparisonSerializer : KSerializer<MetricComparison> {
    private val d = buildClassSerialDescriptor("MetricComparison")
    override val descriptor: SerialDescriptor = d
    override fun serialize(encoder: Encoder, value: MetricComparison) {
        val je = encoder as JsonEncoder
        val (key, v) = when (value) {
            is MetricComparisonEqual -> "Equal" to JsonPrimitive(value.v)
            is MetricComparisonNotEqual -> "NotEqual" to JsonPrimitive(value.v)
            is MetricComparisonGreaterThan -> "GreaterThan" to JsonPrimitive(value.v)
            is MetricComparisonLessThan -> "LessThan" to JsonPrimitive(value.v)
            is MetricComparisonInRange -> "InRange" to Json.encodeToJsonElement(RangeI64.serializer(), value.range)
            MetricComparisonExists -> "Exists" to JsonObject(emptyMap())
            MetricComparisonMissing -> "Missing" to JsonObject(emptyMap())
        }
        je.encodeJsonElement(JsonObject(mapOf(key to v)))
    }
    override fun deserialize(decoder: Decoder): MetricComparison {
        val je = decoder as JsonDecoder
        val obj = je.decodeJsonElement() as JsonObject
        val (key, value) = obj.entries.first()
        val longVal = (value as JsonPrimitive).content.toLong()
        return when (key) {
            "Equal" -> MetricComparisonEqual(longVal)
            "NotEqual" -> MetricComparisonNotEqual(longVal)
            "GreaterThan" -> MetricComparisonGreaterThan(longVal)
            "LessThan" -> MetricComparisonLessThan(longVal)
            "InRange" -> MetricComparisonInRange(Json.decodeFromJsonElement(RangeI64.serializer(), value))
            "Exists" -> MetricComparisonExists
            "Missing" -> MetricComparisonMissing
            else -> error("Unknown MetricComparison variant: " + key)
        }
    }
}

@Serializable(with = FeedbackStateSerializer::class)
data class FeedbackState(val value: String)

@Serializable(with = LifeTargetSerializer::class)
sealed class LifeTarget

@Serializable @SerialName("Tile")
class LifeTargetTile(val id: String) : LifeTarget()

@Serializable @SerialName("Placement")
class LifeTargetPlacement(val id: String) : LifeTarget()

@Serializable @SerialName("Execution")
class LifeTargetExecution(val id: String) : LifeTarget()

@Serializable @SerialName("Recurring")
class LifeTargetRecurring(val id: String) : LifeTarget()

object LifeTargetSerializer : KSerializer<LifeTarget> {
    private val d = buildClassSerialDescriptor("LifeTarget")
    override val descriptor: SerialDescriptor = d
    override fun serialize(encoder: Encoder, value: LifeTarget) {
        val je = encoder as JsonEncoder
        val (key, id) = when (value) {
            is LifeTargetTile -> "Tile" to value.id
            is LifeTargetPlacement -> "Placement" to value.id
            is LifeTargetExecution -> "Execution" to value.id
            is LifeTargetRecurring -> "Recurring" to value.id
        }
        je.encodeJsonElement(JsonObject(mapOf(key to JsonPrimitive(id))))
    }
    override fun deserialize(decoder: Decoder): LifeTarget {
        val je = decoder as JsonDecoder
        val obj = je.decodeJsonElement() as JsonObject
        val (key, value) = obj.entries.first()
        val id = (value as JsonPrimitive).content
        return when (key) {
            "Tile" -> LifeTargetTile(id)
            "Placement" -> LifeTargetPlacement(id)
            "Execution" -> LifeTargetExecution(id)
            "Recurring" -> LifeTargetRecurring(id)
            else -> error("Unknown LifeTarget variant: " + key)
        }
    }
}

@Serializable(with = LifeStateSerializer::class)
data class LifeState(val value: String)


object RelationWindowKindSerializer : KSerializer<RelationWindowKind> {
    override val descriptor: SerialDescriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("RelationWindowKind", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: RelationWindowKind) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): RelationWindowKind = RelationWindowKind(decoder.decodeString())
}

object RequirementStateSerializer : KSerializer<RequirementState> {
    override val descriptor: SerialDescriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("RequirementState", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: RequirementState) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): RequirementState = RequirementState(decoder.decodeString())
}

object TaskStateKindSerializer : KSerializer<TaskStateKind> {
    override val descriptor: SerialDescriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("TaskStateKind", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: TaskStateKind) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): TaskStateKind = TaskStateKind(decoder.decodeString())
}

object FeedbackStateSerializer : KSerializer<FeedbackState> {
    override val descriptor: SerialDescriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("FeedbackState", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: FeedbackState) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): FeedbackState = FeedbackState(decoder.decodeString())
}

object LifeStateSerializer : KSerializer<LifeState> {
    override val descriptor: SerialDescriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("LifeState", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LifeState) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): LifeState = LifeState(decoder.decodeString())
}
