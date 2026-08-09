package app.tastile.android.ui.mobile.sheets

import app.tastile.android.data.api.SourceTileDetailRead
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.util.UUID

/** The Web quick-create base composition and its routed detail panels. */
enum class QuickCreatePanel { Base, Intent, Identity, Time, Duration, Recurring, References, Completion, PlacementRules, Meta, Schedule }

/** Form mode: drives whether [QuickCreateStateStore.submit] creates a new tile
 *  or updates the one referenced by [editingTileId]. */
enum class QuickCreateMode { Create, Edit }

enum class QuickCreateTileKind { Recurring, Placement }
enum class QuickCreatePlanRole { Executable, Label }
enum class QuickCreateWhenMode { None, Day, Range, Reference }
enum class QuickCreateTimeOfDayMode { AllDay, Range, Unspecified }
enum class QuickCreateRepeatMode { Once, Daily, Weekly, Interval, Condition }

data class QuickCreateVisual(val color: String = "#3b82f6", val icon: String = "check-circle")

data class QuickCreateIdentity(
    val kind: QuickCreateTileKind = QuickCreateTileKind.Placement,
    val title: String = "",
    val description: String? = null,
    val externalId: String? = null,
    val visual: QuickCreateVisual = QuickCreateVisual(),
)

data class QuickCreatePlanReference(val id: String, val target: JsonElement, val pick: JsonElement)

/** Open v1 condition node: numeric kind and JSON term preserve ALL/ANY/NOT and typed terms. */
data class QuickCreateConditionNode(
    val kind: Int,
    val children: List<QuickCreateConditionNode> = emptyList(),
    val term: JsonElement? = null,
)

data class QuickCreateTimeRequirement(
    val id: String,
    val observation: JsonElement,
    val required: JsonElement,
    val preferred: JsonElement? = null,
)

data class QuickCreateTaskContent(val title: String, val note: String? = null)
data class QuickCreateTaskDefinition(
    val id: String,
    val content: QuickCreateTaskContent,
    val show: JsonElement? = null,
    val complete: QuickCreateConditionNode = defaultTermCondition(),
    val order: JsonArray = JsonArray(emptyList()),
)

data class QuickCreatePlanCompletion(
    val root: QuickCreateConditionNode = defaultAllCondition(),
    val timeRequirements: List<QuickCreateTimeRequirement> = listOf(defaultTimeRequirement()),
    val tasks: List<QuickCreateTaskDefinition> = listOf(defaultTaskDefinition()),
)

data class QuickCreatePlacementEffect(
    val kind: Int = 0,
    val scopeKind: Int = 0,
    val scopeParent: String? = null,
    val span: QuickCreateDurationRange? = null,
    val score: Int? = null,
    val record: Int? = null,
)

data class QuickCreatePlacementRule(
    val id: String,
    val `when`: JsonElement? = null,
    val rank: Int = 0,
    val effect: QuickCreatePlacementEffect = QuickCreatePlacementEffect(),
)

data class QuickCreatePlanning(
    val placementRules: List<QuickCreatePlacementRule> = emptyList(),
    val nestingRules: JsonArray = JsonArray(emptyList()),
    val flows: JsonArray = JsonArray(emptyList()),
)

data class QuickCreatePlan(
    val role: QuickCreatePlanRole = QuickCreatePlanRole.Executable,
    val references: List<QuickCreatePlanReference> = emptyList(),
    val completion: QuickCreatePlanCompletion = QuickCreatePlanCompletion(),
    val planning: QuickCreatePlanning = QuickCreatePlanning(),
    val metrics: JsonArray = JsonArray(emptyList()),
    val decisions: JsonArray = JsonArray(emptyList()),
)

data class QuickCreateSpan(val start: String = "", val end: String = "")
data class QuickCreateDurationRange(val minMs: Long? = 30 * 60_000L, val maxMs: Long? = 90 * 60_000L)

data class QuickCreateTime(
    val span: QuickCreateSpan = QuickCreateSpan(),
    val durationMinMax: QuickCreateDurationRange = QuickCreateDurationRange(),
    val whenMode: QuickCreateWhenMode = QuickCreateWhenMode.None,
    val timeOfDayMode: QuickCreateTimeOfDayMode = QuickCreateTimeOfDayMode.Unspecified,
    val timeOfDayStart: String = "",
    val timeOfDayEnd: String = "",
    val referenceId: String? = null,
    val referenceLabel: String = "",
)

data class QuickCreateWindow(
    val id: String,
    val owner: String,
    val kind: Int,
    val bounds: QuickCreateSpan,
    val rules: List<QuickCreateWindowRule> = emptyList(),
    val referenceId: String? = null,
)

data class QuickCreateDateRange(val startDate: String, val endDate: String)

data class QuickCreateWindowRule(
    val id: String,
    val weekdayMask: Int? = null,
    val timeStart: String? = null,
    val timeEnd: String? = null,
    val holidayKind: Int? = null,
    val dateRange: QuickCreateDateRange? = null,
    val `when`: QuickCreateConditionNode? = null,
)

data class QuickCreateActor(val id: String = "self", val kind: Int = 0, val ownerId: String? = null)
data class QuickCreateChanged(val at: String = Instant.now().toString(), val actor: QuickCreateActor = QuickCreateActor())

data class QuickCreateRecurringLife(
    val active: QuickCreateDateRange = QuickCreateDateRange("", ""),
    val state: Int = 0,
    val changed: QuickCreateChanged = QuickCreateChanged(),
)

data class QuickCreateFrameGenerator(val kind: String, val value: JsonElement)
data class QuickCreateFrameRule(
    val id: String,
    val generator: QuickCreateFrameGenerator,
    val active: QuickCreateConditionNode? = null,
)

data class QuickCreateRecurringRule(
    val id: String,
    val `when`: QuickCreateConditionNode? = null,
    val rank: Int,
    val outputs: JsonArray = JsonArray(emptyList()),
)

data class QuickCreateRecurring(
    val life: QuickCreateRecurringLife = QuickCreateRecurringLife(),
    val frameRules: List<QuickCreateFrameRule> = emptyList(),
    val rules: List<QuickCreateRecurringRule> = emptyList(),
    val repeatMode: QuickCreateRepeatMode = QuickCreateRepeatMode.Once,
    val weekdayMask: Int = 0b0011111,
    val endDate: String = "",
    val intervalValue: Int = 1,
    val intervalUnit: QuickCreateIntervalUnit = QuickCreateIntervalUnit.Day,
)

enum class QuickCreateIntervalUnit { Minute, Hour, Day }

data class QuickCreateMeta(
    val ownerSubjectId: String? = null,
    val tags: List<String> = emptyList(),
    val memo: String = "",
)

/**
 * Schedule-authoring slice for the v1 source-tile wire.
 *
 * Mirrors `SourceScheduleDefinitionSchema` (see `app/openapi/v1.json` lines
 * 4066-4343). The four fields exposed in the Mobile QuickCreate panel today:
 *  - [priority] (i32, 0..10) → `schedule.priority`
 *  - [splitPolicyKind] (i16, 0=unsplit / 1=split) → `schedule.split_policy.kind`
 *  - [splitPolicyMinSegmentMs] / [splitPolicyMaxSegmentMs] / [splitPolicyMaxSegments]
 *    → `schedule.split_policy.{min,max}_segment_ms`, `max_segments`
 *  - [offsetMin] (i32 UTC minutes) → `schedule.generation.offset_min`
 *  - [excludedDates] (list of ISO-8601 calendar dates) →
 *    `schedule.generation.excluded_dates`
 *
 * Defaults per the wiring PR scope: priority=5, split_policy.kind=0 (unsplit,
 * "not allowed to split"), min_segment_ms=0, max_segment_ms=Long.MAX_VALUE,
 * max_segments=1, offset_min=0 (UTC), excluded_dates=empty.
 */
data class QuickCreateSchedule(
    val priority: Int = 5,
    val splitPolicyKind: Short = 0,
    val splitPolicyMinSegmentMs: Long = 0L,
    val splitPolicyMaxSegmentMs: Long = Long.MAX_VALUE,
    val splitPolicyMaxSegments: Int = 1,
    val offsetMin: Int = 0,
    val excludedDates: List<String> = emptyList(),
)

/** A workspace summary used by the Meta project's catalog. */
data class QuickCreateProject(val id: String, val displayName: String)

/**
 * UI-free Web-equivalent quick-create draft. Panels mutate this one StateFlow;
 * panel navigation never reconstructs or clears field values.
 *
 * When [mode] is [QuickCreateMode.Edit] the store represents an in-place edit
 * of the tile referenced by [editingTileId]; [editingPlacementId] is the
 * optional placement being edited (calendar occurrence) so the submit flow
 * can reschedule that placement as part of the same save.
 */
data class QuickCreateDraftState(
    val mode: QuickCreateMode = QuickCreateMode.Create,
    val editingTileId: String? = null,
    val editingPlacementId: String? = null,
    val activePanel: QuickCreatePanel? = QuickCreatePanel.Base,
    val identity: QuickCreateIdentity = QuickCreateIdentity(),
    val plan: QuickCreatePlan = QuickCreatePlan(),
    val time: QuickCreateTime = QuickCreateTime(),
    val windows: List<QuickCreateWindow> = emptyList(),
    val recurring: QuickCreateRecurring = QuickCreateRecurring(),
    val meta: QuickCreateMeta = QuickCreateMeta(),
    val schedule: QuickCreateSchedule = QuickCreateSchedule(),
)

class QuickCreateStateStore(initial: QuickCreateDraftState = QuickCreateDraftState()) {
    private val mutableState = MutableStateFlow(initial)
    val state: StateFlow<QuickCreateDraftState> = mutableState.asStateFlow()

    fun openSubpanel(panel: QuickCreatePanel) {
        require(panel != QuickCreatePanel.Base) { "Base is not a subpanel" }
        mutate { it.copy(activePanel = panel) }
    }

    fun backToBase() = mutate { it.copy(activePanel = QuickCreatePanel.Base) }
    fun dismissPanel() = mutate { it.copy(activePanel = null) }
    fun reset() { mutableState.value = QuickCreateDraftState() }

    /**
     * Switch the store into [QuickCreateMode.Edit] and prefill the form from
     * an existing v1 [SourceTileDetailRead] payload. The supplied
     * [placementId] (if any) drives the reschedule path of [submitUpdate] —
     * when present, the user-edited time span is written back to that
     * placement via `POST /v1/placements/{id}/changes`.
     */
    fun hydrateForEdit(
        tileId: String,
        placementId: String?,
        detail: SourceTileDetailRead,
    ) {
        val source = detail.source
        val firstPlacement = detail.placements.firstOrNull()
        val span = QuickCreateSpan(
            start = firstPlacement?.start.orEmpty(),
            end = firstPlacement?.end.orEmpty(),
        )
        val whenMode = when {
            span.start.isBlank() && span.end.isBlank() -> QuickCreateWhenMode.None
            span.end.isBlank() -> QuickCreateWhenMode.Day
            else -> QuickCreateWhenMode.Range
        }
        mutableState.value = QuickCreateDraftState(
            mode = QuickCreateMode.Edit,
            editingTileId = tileId,
            editingPlacementId = placementId,
            activePanel = QuickCreatePanel.Base,
            identity = QuickCreateIdentity(
                kind = QuickCreateTileKind.Placement,
                title = source.title,
                description = source.description,
                externalId = source.externalId,
                visual = QuickCreateVisual(
                    color = source.color ?: "#3b82f6",
                    icon = source.icon ?: "check-circle",
                ),
            ),
            plan = QuickCreatePlan(
                role = if (source.planRole.toInt() == 1) QuickCreatePlanRole.Label else QuickCreatePlanRole.Executable,
            ),
            time = QuickCreateTime(
                span = span,
                whenMode = whenMode,
            ),
            schedule = QuickCreateSchedule(
                priority = source.schedule.priority,
                splitPolicyKind = source.schedule.splitPolicy.kind,
                splitPolicyMinSegmentMs = source.schedule.splitPolicy.minSegmentMs ?: 0L,
                splitPolicyMaxSegmentMs = source.schedule.splitPolicy.maxSegmentMs ?: Long.MAX_VALUE,
                splitPolicyMaxSegments = source.schedule.splitPolicy.maxSegments ?: 1,
                offsetMin = source.schedule.generation.offsetMin ?: 0,
                excludedDates = source.schedule.generation.excludedDates,
            ),
            meta = QuickCreateMeta(),
        )
    }

    // Whole-slice updates make nullable clears explicit instead of retaining stale values.
    fun updateIdentity(identity: QuickCreateIdentity) = mutate { it.copy(identity = identity) }
    fun updatePlan(plan: QuickCreatePlan) = mutate { it.copy(plan = plan) }
    fun appendCompletionTerm(term: JsonElement) = mutate { draft ->
        val completion = draft.plan.completion
        val root = completion.root
        draft.copy(plan = draft.plan.copy(completion = completion.copy(root = root.copy(children = root.children + QuickCreateConditionNode(3, term = term)))))
    }
    fun updateTime(time: QuickCreateTime) = mutate { it.copy(time = time) }
    fun updateWindows(windows: List<QuickCreateWindow>) = mutate { it.copy(windows = windows) }
    fun updateRecurring(recurring: QuickCreateRecurring) = mutate { it.copy(recurring = recurring) }
    fun updateMeta(meta: QuickCreateMeta) = mutate { it.copy(meta = meta) }
    fun updateSchedule(schedule: QuickCreateSchedule) = mutate { it.copy(schedule = schedule) }

    /** Toggle whether this tile is rendered as a label. */
    fun updateRole(role: QuickCreatePlanRole) = mutate { it.copy(plan = it.plan.copy(role = role)) }

    private inline fun mutate(transform: (QuickCreateDraftState) -> QuickCreateDraftState) {
        mutableState.value = transform(mutableState.value)
    }
}

private const val DEFAULT_TASK_ID = "task_default"
/** Internal default task title; localized at the UI layer via R.string.quick_create_default_task_title. */
private const val DEFAULT_TASK_TITLE = "Task complete"

private fun defaultAllCondition() = QuickCreateConditionNode(
    kind = 0,
    children = listOf(defaultTermCondition()),
)

private fun defaultTermCondition() = QuickCreateConditionNode(
    kind = 3,
    term = JsonObject(
        mapOf(
            "kind" to JsonPrimitive("task"),
            "value" to JsonObject(mapOf("taskId" to JsonPrimitive(DEFAULT_TASK_ID), "state" to JsonPrimitive(2))),
        )
    ),
)

private fun defaultTimeRequirement() = QuickCreateTimeRequirement(
    id = "tr_${UUID.randomUUID()}",
    observation = JsonObject(mapOf("scope" to JsonPrimitive(1), "source" to JsonPrimitive(0), "aggregate" to JsonPrimitive(0), "quantifier" to JsonPrimitive(0))),
    required = JsonObject(mapOf("minMs" to JsonPrimitive(30 * 60_000L), "maxMs" to JsonPrimitive(90 * 60_000L))),
    preferred = null,
)

private fun defaultTaskDefinition() = QuickCreateTaskDefinition(
    id = DEFAULT_TASK_ID,
    content = QuickCreateTaskContent(title = DEFAULT_TASK_TITLE),
    complete = defaultTermCondition(),
)
