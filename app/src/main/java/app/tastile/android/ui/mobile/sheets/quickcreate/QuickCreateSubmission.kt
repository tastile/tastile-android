package app.tastile.android.ui.mobile.sheets.quickcreate

import app.tastile.android.data.api.CommandResponse
import app.tastile.android.data.api.CompletionSchema
import app.tastile.android.data.api.ConditionAny
import app.tastile.android.data.api.ConditionRef
import app.tastile.android.data.api.PlacementRuleEffectSchema
import app.tastile.android.data.api.PlacementRuleSchema
import app.tastile.android.data.api.RangeI64Schema
import app.tastile.android.data.api.ScopeSchema
import app.tastile.android.data.api.CreatePlacementPayload
import app.tastile.android.data.api.CreateTilePayload
import app.tastile.android.data.api.FrameRuleGeneratorPayload
import app.tastile.android.data.api.FrameRulePayload
import app.tastile.android.data.api.FrameRuleStepPayload
import app.tastile.android.data.api.MaterializeRecurringPayload
import app.tastile.android.data.api.PlacementBaselinePayload
import app.tastile.android.data.api.PlacementSpanPayload
import app.tastile.android.data.api.SchedulePlanDefinitionPayloadTyped
import app.tastile.android.data.api.SchedulingPlanningDefinitionSchema
import app.tastile.android.data.api.SetPlanPayload
import app.tastile.android.data.api.SourceGenerationPayload
import app.tastile.android.data.api.SourceRefPayload
import app.tastile.android.data.api.SourceSchedulePayload
import app.tastile.android.data.api.SourceSplitPolicyPayload
import app.tastile.android.data.api.SourceTileDefinitionPayload
import app.tastile.android.data.api.SourceTileWritePayload
import app.tastile.android.data.api.SourceWindowPayload
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import app.tastile.android.data.api.V1NumericConstants
import app.tastile.android.ui.mobile.sheets.QuickCreateConditionNode
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePlacementRule
import app.tastile.android.ui.mobile.sheets.QuickCreatePlanRole
import app.tastile.android.ui.mobile.sheets.QuickCreateRecurring
import app.tastile.android.ui.mobile.sheets.QuickCreateTileKind
import app.tastile.android.ui.mobile.sheets.QuickCreateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

/** Boundary used by the create-sheet flow; it makes the exact v1 sequence testable. */
interface QuickCreateCommandGateway {
    suspend fun createTile(payload: CreateTilePayload): CommandResponse
    suspend fun createPlacement(payload: CreatePlacementPayload): CommandResponse
    suspend fun materializeRecurring(payload: MaterializeRecurringPayload): CommandResponse
    suspend fun setPlan(tileId: String, payload: SetPlanPayload): CommandResponse
    suspend fun createSourceTile(payload: SourceTileWritePayload): CommandResponse
    suspend fun updateSourceTile(
        sourceTileId: String,
        payload: SourceTileWritePayload,
        expectedRevision: Long,
    ): CommandResponse
}

class V1QuickCreateCommandGateway(private val client: V1ApiClient) : QuickCreateCommandGateway {
    override suspend fun createTile(payload: CreateTilePayload) = client.createTile(payload)
    override suspend fun createPlacement(payload: CreatePlacementPayload) = client.createPlacement(payload)
    override suspend fun materializeRecurring(payload: MaterializeRecurringPayload) = client.materializeRecurring(payload)
    override suspend fun setPlan(tileId: String, payload: SetPlanPayload) = client.setPlan(tileId, payload)
    override suspend fun createSourceTile(payload: SourceTileWritePayload) = client.createSourceTile(payload)
    override suspend fun updateSourceTile(
        sourceTileId: String,
        payload: SourceTileWritePayload,
        expectedRevision: Long,
    ) = client.updateSourceTile(sourceTileId, payload, expectedRevision)
}

sealed interface QuickCreateSubmitResult {
    data class Success(val tileId: String) : QuickCreateSubmitResult
    data class Failure(val message: String) : QuickCreateSubmitResult
}

/**
 * Tri-state outcome of the v1 `POST /v1/source-tiles` attempt:
 *  - [Success] — tile was created; the caller returns immediately.
 *  - [Failure] — v1 rejected the payload for a reason that should surface to
 *    the user (validation, conflict, etc.); do not silently retry on v0.
 *  - [Fallback] — the v1 endpoint is missing (`NOT_FOUND`); the caller retries
 *    the v0 thin-create + `setPlan` path.
 */
private sealed interface V1SubmitOutcome {
    data class Success(val tileId: String) : V1SubmitOutcome
    data class Failure(val message: String) : V1SubmitOutcome
    data object Fallback : V1SubmitOutcome
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
            // Primary: v1 canonical `POST /v1/source-tiles`. Carries the
            // priority / split_policy / offset_min / excluded_dates fields
            // surfaced in the new Schedule subpanel.
            when (val v1Result = submitSourceTile(draft, start, end)) {
                is V1SubmitOutcome.Success -> return QuickCreateSubmitResult.Success(v1Result.tileId)
                is V1SubmitOutcome.Failure -> return QuickCreateSubmitResult.Failure(v1Result.message)
                V1SubmitOutcome.Fallback -> {
                    // Fallback: legacy v0 `POST /v1/tiles` + `POST /v1/tiles/{id}/plan`.
                    // Triggered only when the v1 endpoint rejects the payload (e.g. a
                    // backing server that has not yet rolled out the source-tile
                    // handler). The rich plan shape is round-tripped through
                    // SetPlanPayload on this path; see
                    // docs/ux-fix-v1-source-tile-wiring.md for caveats.
                }
            }

            if (draft.identity.kind == QuickCreateTileKind.Recurring) submitRecurring(draft, start, end)
            else submitPlacement(draft, start, end)
        } catch (error: Exception) {
            QuickCreateSubmitResult.Failure(error.message ?: "Failed to create tile")
        }
    }

    /**
     * Build a v1 [SourceTileWritePayload] from the draft and POST it.
     *
     * Returns a tri-state:
     *  - [V1SubmitOutcome.Success] on success (return immediately).
     *  - [V1SubmitOutcome.Fallback] when the v1 endpoint is missing /
     *    not-yet-rolled-out (try the v0 path).
     *  - [V1SubmitOutcome.Failure] on every other v1 error (surface it).
     */
    private suspend fun submitSourceTile(draft: QuickCreateDraftState, start: String, end: String): V1SubmitOutcome {
        return try {
            val payload = buildSourceTileWritePayload(draft, start, end)
            val created = gateway.createSourceTile(payload)
            val tileId = created.aggregate?.id
                ?: return V1SubmitOutcome.Failure("v1 create source-tile response missing aggregate id")
            V1SubmitOutcome.Success(tileId)
        } catch (error: V1Error) {
            // `NOT_FOUND` on the source-tiles endpoint = backend doesn't
            // expose the handler yet. Let the dispatcher fall back to v0.
            // Surface every other error so the user sees it.
            val kind = (error as? V1Error.Api)?.kindValue
            if (kind == V1NumericConstants.ApiErrorKind.NOT_FOUND) V1SubmitOutcome.Fallback
            else V1SubmitOutcome.Failure(error.message ?: "v1 source-tile create failed")
        }
    }

    /**
     * Build a [SourceTileWritePayload] from the draft.
     *
     * Caveat: the typed `SchedulePlanDefinitionPayloadTyped` requires an
     * externally-tagged Condition AST. The Mobile QuickCreate draft stores
     * an internally-tagged `QuickCreateConditionNode`, which the v0 path
     * round-trips through SetPlanPayload as raw JSON. For the v1 path we
     * emit a minimal valid plan (`Any([])` root + empty references /
     * planning / metrics / decisions) and rely on the rich plan fields to
     * survive via SetPlanPayload only on the v0 fallback. Mapping the
     * internal-tagged tree to external-tagged `ConditionAny/All/Not/Term`
     * is documented as a follow-up.
     */
    private fun buildSourceTileWritePayload(draft: QuickCreateDraftState, start: String, end: String): SourceTileWritePayload {
        val identity = draft.identity
        val schedule = draft.schedule
        val time = draft.time
        val recurring = draft.recurring
        val tile = SourceTileDefinitionPayload(
            title = identity.title.trim(),
            description = identity.description,
            color = identity.visual.color,
            icon = identity.visual.icon,
            externalId = identity.externalId,
        )
        val plan = SchedulePlanDefinitionPayloadTyped(
            role = role(draft.plan.role).toShort(),
            references = emptyList(),
            completion = CompletionSchema(
                root = ConditionRef(ConditionAny(emptyList())),
                time_requirements = emptyList(),
                tasks = emptyList(),
            ),
            planning = SchedulingPlanningDefinitionSchema(
                placement_rules = draft.plan.planning.placementRules.map { rule ->
                    PlacementRuleSchema(
                        id = rule.id,
                        rank = rule.rank,
                        effect = PlacementRuleEffectSchema(
                            kind = rule.effect.kind,
                            record = rule.effect.record,
                            scope = if (rule.effect.kind in 0..3) ScopeSchema(rule.effect.scopeKind, rule.effect.scopeParent) else null,
                            score = rule.effect.score,
                            span = rule.effect.span?.let { RangeI64Schema(it.minMs, it.maxMs) },
                        ),
                        `when` = rule.`when`,
                    )
                },
                nesting_rules = emptyList(),
            ),
            metrics = emptyList(),
            decisions = emptyList(),
        )
        val generation = buildGenerationPayload(recurring, time, schedule.offsetMin, schedule.excludedDates)
        val requiredDurationMs = time.durationMinMax.minMs
            ?: time.durationMinMax.maxMs
            ?: 0L
        val window = SourceWindowPayload(
            startOffsetMs = timeOfDayOffsetMs(time.timeOfDayStart),
            endOffsetMs = timeOfDayOffsetMs(time.timeOfDayEnd),
        )
        val splitPolicy = SourceSplitPolicyPayload(
            kind = schedule.splitPolicyKind,
            minSegmentMs = schedule.splitPolicyMinSegmentMs,
            maxSegmentMs = schedule.splitPolicyMaxSegmentMs,
            maxSegments = schedule.splitPolicyMaxSegments,
        )
        val sourceSchedule = SourceSchedulePayload(
            requiredDurationMs = requiredDurationMs,
            generation = generation,
            window = window,
            splitPolicy = splitPolicy,
            priority = schedule.priority,
        )
        return SourceTileWritePayload(
            tile = tile,
            plan = plan,
            flows = emptyList(),
            schedule = sourceSchedule,
            horizon = PlacementSpanPayload(start, end),
        )
    }

    private fun buildGenerationPayload(
        recurring: QuickCreateRecurring,
        time: QuickCreateTime,
        offsetMin: Int,
        excludedDates: List<String>,
    ): SourceGenerationPayload {
        // Map the Web's repeat-mode + weekday-mask onto the typed v1 generation.
        // 0 = OneTime, 1 = Recurring, 2 = DemandDriven (per openapi.rs:728).
        val kind: Short = when (recurring.repeatMode) {
            app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode.Once -> 0
            app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode.Condition -> 2
            else -> 1
        }
        val weekdayMask: Byte? = if (recurring.repeatMode == app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode.Weekly) recurring.weekdayMask.toByte() else null
        val intervalMs: Long? = when (recurring.repeatMode) {
            app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode.Daily,
            app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode.Weekly -> 86_400_000L
            app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode.Interval -> {
                val unitMs = when (recurring.intervalUnit) {
                    app.tastile.android.ui.mobile.sheets.QuickCreateIntervalUnit.Minute -> 60_000L
                    app.tastile.android.ui.mobile.sheets.QuickCreateIntervalUnit.Hour -> 3_600_000L
                    app.tastile.android.ui.mobile.sheets.QuickCreateIntervalUnit.Day -> 86_400_000L
                }
                recurring.intervalValue.coerceAtLeast(1) * unitMs
            }
            else -> null
        }
        val startsAt: String? = if (kind == 1.toShort()) {
            normalizeSpanInstant(time.span.start)
        } else null
        val endsAt: String? = if (recurring.endDate.isNotBlank()) {
            runCatching { Instant.parse("${recurring.endDate}T23:59:59Z").toString() }.getOrNull()
        } else null
        return SourceGenerationPayload(
            kind = kind,
            at = null,
            startsAt = startsAt,
            intervalMs = intervalMs,
            endsAt = endsAt,
            weekdayMask = weekdayMask,
            dateRangeStart = null,
            dateRangeEnd = null,
            excludedDates = excludedDates,
            offsetMin = offsetMin,
        )
    }

    private fun timeOfDayOffsetMs(value: String): Long {
        if (value.isBlank()) return 0L
        val parsed = runCatching { LocalTime.parse(value) }.getOrNull() ?: return 0L
        return parsed.toNanoOfDay() / 1_000_000L
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
            put("placement_rules", JsonArray(draft.plan.planning.placementRules.map(::placementRuleJson))); put("nesting_rules", snakeCase(draft.plan.planning.nestingRules)); put("flows", snakeCase(draft.plan.planning.flows))
        },
        metrics = snakeCase(draft.plan.metrics) as JsonArray,
        decisions = snakeCase(draft.plan.decisions) as JsonArray,
    )

    /**
     * Update an existing tile in-place. The draft must already be hydrated
     * via [QuickCreateStateStore.hydrateForEdit] so [QuickCreateDraftState.editingTileId]
     * carries the target id and [QuickCreateDraftState.editingPlacementId] (when
     * present) selects a placement to reschedule as part of the same save.
     *
     * The update mirrors tastile-web's `submitUpdateTile` (see
     * `tastile-web/src/shared/api/v1/submit.ts`):
     *   1. PATCH the source-tile envelope (PUT `/v1/source-tiles/{id}`) using
     *      the same `SourceTileWritePayload` shape as the create path.
     *   2. If a placement id is present and the user edited the span, PATCH
     *      `/v1/placements/{id}/changes` with a `Span` ChangeSet. We do not
     *      carry the full v1 ChangeSet wire shape in the store; the placement
     *      branch is delegated to [dispatchPlacementSpanChanges] when the
     *      caller has a dispatcher at hand. For now we leave the reschedule
     *      step to the caller (the TileEditSheet wires
     *      [DashboardViewModel.reschedulePlacement] for that), and this
     *      method only owns the source-tile update.
     */
    suspend fun submitUpdate(
        draft: QuickCreateDraftState,
        expectedRevision: Long,
    ): QuickCreateSubmitResult {
        val tileId = draft.editingTileId
            ?: return QuickCreateSubmitResult.Failure("Missing editing tile id")
        val title = draft.identity.title.trim()
        if (title.isEmpty()) return QuickCreateSubmitResult.Failure("Title is required")
        return try {
            // In edit mode the span comes from the hydrated source-tile detail
            // and may legitimately be empty for tiles without a placement
            // (e.g. label-only tiles). We still need a non-blank horizon for
            // `PlacementSpanPayload`; the server treats it as the audit
            // window for the change so we fall back to the current instant.
            val start = draft.time.span.start.takeIf { it.isNotBlank() }
                ?: Instant.now().toString()
            val end = draft.time.span.end.takeIf { it.isNotBlank() } ?: start
            val payload = buildSourceTileWritePayload(
                draft = draft,
                start = start,
                end = end,
            )
            val updated = gateway.updateSourceTile(
                sourceTileId = tileId,
                payload = payload,
                expectedRevision = expectedRevision,
            )
            val aggregateId = updated.aggregate?.id ?: tileId
            QuickCreateSubmitResult.Success(aggregateId)
        } catch (error: Exception) {
            QuickCreateSubmitResult.Failure(error.message ?: "Failed to update tile")
        }
    }
}

private fun role(role: QuickCreatePlanRole): Byte = if (role == QuickCreatePlanRole.Label) 1 else V1NumericConstants.PlanRole.EXECUTABLE
private fun conditionJson(node: QuickCreateConditionNode): JsonObject = buildJsonObject {
    put("kind", node.kind); put("children", JsonArray(node.children.map(::conditionJson))); put("term", node.term?.let(::snakeCase) ?: JsonNull)
}
private fun placementRuleJson(rule: QuickCreatePlacementRule): JsonObject = buildJsonObject {
    put("id", rule.id)
    put("when", rule.`when` ?: JsonNull)
    put("rank", rule.rank)
    put("effect", buildJsonObject {
        put("kind", rule.effect.kind)
        put("scope", if (rule.effect.kind in 0..3) buildJsonObject {
            put("kind", rule.effect.scopeKind)
            put("parent", rule.effect.scopeParent?.let(::JsonPrimitive) ?: JsonNull)
        } else JsonNull)
        put("span", rule.effect.span?.let { buildJsonObject {
            put("min", it.minMs?.let(::JsonPrimitive) ?: JsonNull)
            put("max", it.maxMs?.let(::JsonPrimitive) ?: JsonNull)
        } } ?: JsonNull)
        put("score", rule.effect.score?.let(::JsonPrimitive) ?: JsonNull)
        put("record", rule.effect.record?.let(::JsonPrimitive) ?: JsonNull)
    })
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
