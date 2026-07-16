package app.tastile.android.ui.mobile.sheets.quickcreate

import app.tastile.android.data.api.CommandResponse
import app.tastile.android.data.api.CreatePlacementPayload
import app.tastile.android.data.api.CreateTilePayload
import app.tastile.android.data.api.FrameRuleGeneratorPayload
import app.tastile.android.data.api.FrameRulePayload
import app.tastile.android.data.api.FrameRuleStepPayload
import app.tastile.android.data.api.MaterializeRecurringPayload
import app.tastile.android.data.api.PlacementBaselinePayload
import app.tastile.android.data.api.PlacementSpanPayload
import app.tastile.android.data.api.SetPlanPayload
import app.tastile.android.data.api.SourceRefPayload
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1NumericConstants
import app.tastile.android.ui.mobile.sheets.QuickCreateConditionNode
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePlanRole
import app.tastile.android.ui.mobile.sheets.QuickCreateTileKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** Boundary used by the create-sheet flow; it makes the exact v1 sequence testable. */
interface QuickCreateCommandGateway {
    suspend fun createTile(payload: CreateTilePayload): CommandResponse
    suspend fun createPlacement(payload: CreatePlacementPayload): CommandResponse
    suspend fun materializeRecurring(payload: MaterializeRecurringPayload): CommandResponse
    suspend fun setPlan(tileId: String, payload: SetPlanPayload): CommandResponse
}

class V1QuickCreateCommandGateway(private val client: V1ApiClient) : QuickCreateCommandGateway {
    override suspend fun createTile(payload: CreateTilePayload) = client.createTile(payload)
    override suspend fun createPlacement(payload: CreatePlacementPayload) = client.createPlacement(payload)
    override suspend fun materializeRecurring(payload: MaterializeRecurringPayload) = client.materializeRecurring(payload)
    override suspend fun setPlan(tileId: String, payload: SetPlanPayload) = client.setPlan(tileId, payload)
}

sealed interface QuickCreateSubmitResult {
    data class Success(val tileId: String) : QuickCreateSubmitResult
    data class Failure(val message: String) : QuickCreateSubmitResult
}

data class QuickCreateSubmissionValidation(
    val isValid: Boolean,
    val message: String? = null,
    val normalizedStart: String? = null,
    val normalizedEnd: String? = null,
)

/** Mirrors the checks made by tastile-web before its canonical v1 commands. */
fun quickCreateSubmissionValidation(draft: QuickCreateDraftState): QuickCreateSubmissionValidation {
    val title = draft.identity.title.trim()
    if (title.isEmpty()) return QuickCreateSubmissionValidation(false, "Title is required")
    val duration = draft.time.durationMinMax
    if (duration.minMs != null && duration.maxMs != null && duration.minMs > duration.maxMs) {
        return QuickCreateSubmissionValidation(false, "Minimum duration must not exceed maximum duration")
    }
    val start = normalizeSpanInstant(draft.time.span.start) ?: return QuickCreateSubmissionValidation(false, "Start is required")
    val rawEnd = normalizeSpanInstant(draft.time.span.end)
    val end = if (draft.time.timeOfDayMode.name == "AllDay" && (rawEnd == null || !isAfter(rawEnd, start))) {
        nextMidnight(start)
    } else rawEnd
    if (end == null) return QuickCreateSubmissionValidation(false, "End is required")
    if (!isAfter(end, start)) return QuickCreateSubmissionValidation(false, "End must be after start")
    return QuickCreateSubmissionValidation(true, normalizedStart = start, normalizedEnd = end)
}

class QuickCreateSubmissionDispatcher(private val gateway: QuickCreateCommandGateway) {
    suspend fun submit(draft: QuickCreateDraftState): QuickCreateSubmitResult {
        val validation = quickCreateSubmissionValidation(draft)
        if (!validation.isValid) return QuickCreateSubmitResult.Failure(validation.message ?: "Invalid draft")
        val start = requireNotNull(validation.normalizedStart)
        val end = requireNotNull(validation.normalizedEnd)
        return try {
            if (draft.identity.kind == QuickCreateTileKind.Recurring) submitRecurring(draft, start, end)
            else submitPlacement(draft, start, end)
        } catch (error: Exception) {
            QuickCreateSubmitResult.Failure(error.message ?: "Failed to create tile")
        }
    }

    private suspend fun submitPlacement(draft: QuickCreateDraftState, start: String, end: String): QuickCreateSubmitResult {
        val created = gateway.createTile(tilePayload(draft, V1NumericConstants.TileKind.PLACEMENT, null))
        val tileId = created.aggregate?.id ?: return QuickCreateSubmitResult.Failure("Create tile response missing aggregate id")
        val planId = created.aggregateMeta?.planId ?: return QuickCreateSubmitResult.Failure("Create tile response missing aggregate_meta.plan_id")
        gateway.createPlacement(CreatePlacementPayload(tileId, planId, 0, SourceRefPayload.empty(), PlacementBaselinePayload(PlacementSpanPayload(start, end))))
        gateway.setPlan(tileId, planPayload(draft, tileId))
        return QuickCreateSubmitResult.Success(tileId)
    }

    private suspend fun submitRecurring(draft: QuickCreateDraftState, start: String, end: String): QuickCreateSubmitResult {
        val frameId = draft.recurring.frameRules.firstOrNull()?.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val created = gateway.createTile(tilePayload(draft, V1NumericConstants.TileKind.RECURRING, FrameRulePayload(
            id = frameId, rank = 0, generator = FrameRuleGeneratorPayload(FrameRuleStepPayload(recurringStepMs(draft))),
        )))
        val tileId = created.aggregate?.id ?: return QuickCreateSubmitResult.Failure("Create tile response missing aggregate id")
        val assignedFrameId = created.aggregateMeta?.frameRuleId ?: return QuickCreateSubmitResult.Failure("Create tile response missing aggregate_meta.frame_rule_id")
        gateway.materializeRecurring(MaterializeRecurringPayload(tileId, assignedFrameId, start, end))
        gateway.setPlan(tileId, planPayload(draft, tileId))
        return QuickCreateSubmitResult.Success(tileId)
    }

    private fun tilePayload(draft: QuickCreateDraftState, kind: Byte, frameRule: FrameRulePayload?) = CreateTilePayload(
        kind = kind,
        title = draft.identity.title.trim(),
        description = draft.identity.description,
        color = draft.identity.visual.color,
        icon = draft.identity.visual.icon,
        externalId = null, // Web's QuickTileCreate deliberately creates a server-owned external id.
        planRole = role(draft.plan.role),
        ownerSubjectId = null,
        frameRule = frameRule,
    )

    private fun planPayload(draft: QuickCreateDraftState, tileId: String) = SetPlanPayload(
        tileId = tileId,
        role = role(draft.plan.role),
        references = JsonArray(draft.plan.references.map { reference -> buildJsonObject { put("id", reference.id); put("target", snakeCase(reference.target)); put("pick", snakeCase(reference.pick)) } }),
        completion = buildJsonObject {
            put("root", conditionJson(draft.plan.completion.root))
            put("time_requirements", JsonArray(draft.plan.completion.timeRequirements.map { requirement -> buildJsonObject {
                put("id", requirement.id); put("observation", snakeCase(requirement.observation)); put("required", snakeCase(requirement.required)); put("preferred", requirement.preferred?.let(::snakeCase) ?: JsonNull)
            } }))
            put("tasks", JsonArray(draft.plan.completion.tasks.map { task -> buildJsonObject {
                put("id", task.id); put("content", buildJsonObject { put("title", task.content.title); put("note", task.content.note?.let(::JsonPrimitive) ?: JsonNull) }); put("show", task.show?.let(::snakeCase) ?: JsonNull); put("complete", conditionJson(task.complete)); put("order", snakeCase(task.order))
            } }))
        },
        planning = buildJsonObject {
            put("placement_rules", snakeCase(draft.plan.planning.placementRules)); put("nesting_rules", snakeCase(draft.plan.planning.nestingRules)); put("flows", snakeCase(draft.plan.planning.flows))
        },
        metrics = snakeCase(draft.plan.metrics) as JsonArray,
        decisions = snakeCase(draft.plan.decisions) as JsonArray,
    )
}

private fun role(role: QuickCreatePlanRole): Byte = if (role == QuickCreatePlanRole.Label) 1 else V1NumericConstants.PlanRole.EXECUTABLE
private fun conditionJson(node: QuickCreateConditionNode): JsonObject = buildJsonObject {
    put("kind", node.kind); put("children", JsonArray(node.children.map(::conditionJson))); put("term", node.term?.let(::snakeCase) ?: JsonNull)
}
private fun recurringStepMs(draft: QuickCreateDraftState): Long = draft.recurring.frameRules.firstOrNull()?.generator?.value
    ?.let { (it as? JsonObject)?.get("step") as? JsonPrimitive }?.content?.toLongOrNull() ?: 86_400_000L
private fun normalizeSpanInstant(value: String): String? = when {
    value.isBlank() -> null
    Regex("\\d{4}-\\d{2}-\\d{2}").matches(value) -> "${value}T00:00:00Z"
    else -> runCatching { Instant.parse(value).toString() }.getOrNull()
}
private fun isAfter(end: String, start: String): Boolean = Instant.parse(end).isAfter(Instant.parse(start))
private fun nextMidnight(start: String): String = Instant.parse(start).atZone(ZoneOffset.UTC).toLocalDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString()
private fun snakeCase(element: JsonElement): JsonElement = when (element) {
    is JsonArray -> JsonArray(element.map(::snakeCase))
    is JsonObject -> JsonObject(element.entries.associate { (key, value) -> key.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase() to snakeCase(value) })
    else -> element
}
