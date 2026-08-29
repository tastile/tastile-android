package app.tastile.android.ui.mobile.sheets

import app.tastile.android.R
import app.tastile.android.data.api.SourceTileDetailRead
import app.tastile.android.ui.mobile.sheets.quickcreate.rewriteTaskId
import app.tastile.android.ui.mobile.sheets.quickcreate.selfTaskTerm
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

/** Web-equivalent quick-create workflow kind. Selects a coherent bundle of
 *  defaults (tile kind, plan role, repeat/when/time-of-day modes) so the
 *  base panel can present a meaningful chip set. Mirrors the web
 *  `workflow-config.ts` ordering: Event, Task, Recurring, Detailed. */
enum class WorkflowKind { Event, Task, Recurring, Detailed }

/** Bundle of defaults for a given [WorkflowKind]. Used by
 *  [QuickCreateStateStore.setWorkflow] to apply a workflow switch. */
data class WorkflowConfig(
    val kind: WorkflowKind,
    val defaultTileKind: QuickCreateTileKind,
    val defaultPlanRole: QuickCreatePlanRole,
    val defaultRepeatMode: QuickCreateRepeatMode,
    val defaultTimeWhenMode: QuickCreateWhenMode,
    val defaultTimeOfDayMode: QuickCreateTimeOfDayMode,
    val labelResId: Int,         // R.string.* placeholder, set to 0 for now
    val descriptionResId: Int,   // R.string.* placeholder, set to 0 for now
    val headingCreateResId: Int, // R.string.* placeholder, set to 0 for now
    val headingEditResId: Int,   // R.string.* placeholder, set to 0 for now
)

/** Web-equivalent workflow config table. Lookup by [WorkflowKind] yields the
 *  defaults applied by [QuickCreateStateStore.setWorkflow]. */
val WORKFLOW_CONFIG: Map<WorkflowKind, WorkflowConfig> = mapOf(
    WorkflowKind.Event to WorkflowConfig(
        kind = WorkflowKind.Event,
        defaultTileKind = QuickCreateTileKind.Placement,
        defaultPlanRole = QuickCreatePlanRole.Executable,
        defaultRepeatMode = QuickCreateRepeatMode.Once,
        defaultTimeWhenMode = QuickCreateWhenMode.Range,
        defaultTimeOfDayMode = QuickCreateTimeOfDayMode.Range,
        labelResId = R.string.quickcreate_workflow_event_label,
        descriptionResId = R.string.quickcreate_workflow_event_description,
        headingCreateResId = R.string.quickcreate_workflow_event_heading_create,
        headingEditResId = R.string.quickcreate_workflow_event_heading_edit,
    ),
    WorkflowKind.Task to WorkflowConfig(
        kind = WorkflowKind.Task,
        defaultTileKind = QuickCreateTileKind.Placement,
        defaultPlanRole = QuickCreatePlanRole.Executable,
        defaultRepeatMode = QuickCreateRepeatMode.Once,
        defaultTimeWhenMode = QuickCreateWhenMode.Day,
        defaultTimeOfDayMode = QuickCreateTimeOfDayMode.Range,
        labelResId = R.string.quickcreate_workflow_task_label,
        descriptionResId = R.string.quickcreate_workflow_task_description,
        headingCreateResId = R.string.quickcreate_workflow_task_heading_create,
        headingEditResId = R.string.quickcreate_workflow_task_heading_edit,
    ),
    WorkflowKind.Recurring to WorkflowConfig(
        kind = WorkflowKind.Recurring,
        defaultTileKind = QuickCreateTileKind.Recurring,
        defaultPlanRole = QuickCreatePlanRole.Executable,
        defaultRepeatMode = QuickCreateRepeatMode.Daily,
        defaultTimeWhenMode = QuickCreateWhenMode.Range,
        defaultTimeOfDayMode = QuickCreateTimeOfDayMode.Range,
        labelResId = R.string.quickcreate_workflow_recurring_label,
        descriptionResId = R.string.quickcreate_workflow_recurring_description,
        headingCreateResId = R.string.quickcreate_workflow_recurring_heading_create,
        headingEditResId = R.string.quickcreate_workflow_recurring_heading_edit,
    ),
    WorkflowKind.Detailed to WorkflowConfig(
        kind = WorkflowKind.Detailed,
        defaultTileKind = QuickCreateTileKind.Placement,
        defaultPlanRole = QuickCreatePlanRole.Executable,
        defaultRepeatMode = QuickCreateRepeatMode.Once,
        defaultTimeWhenMode = QuickCreateWhenMode.None,
        defaultTimeOfDayMode = QuickCreateTimeOfDayMode.Unspecified,
        labelResId = R.string.quickcreate_workflow_detailed_label,
        descriptionResId = R.string.quickcreate_workflow_detailed_description,
        headingCreateResId = R.string.quickcreate_workflow_detailed_heading_create,
        headingEditResId = R.string.quickcreate_workflow_detailed_heading_edit,
    ),
)

/** Display order for the base panel's workflow chip strip. Mirrors the web
 *  `workflow-config.ts` ordering. */
val WORKFLOW_ORDER: List<WorkflowKind> = listOf(
    WorkflowKind.Event,
    WorkflowKind.Task,
    WorkflowKind.Recurring,
    WorkflowKind.Detailed,
)

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
    /** Authoring-time checkbox flag (web parity). UI-only — excluded from the wire. */
    val done: Boolean = false,
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
 * Mirrors `SourceScheduleDefinitionSchema` in the canonical OpenAPI 3.1 spec
 * at the cross-repo submodule `../../openapi/openapi.yaml` (resolved by
 * `openapi.input` in `gradle.properties`; previously vendored as
 * `app/openapi/v1.json`). The four fields exposed in the Mobile QuickCreate
 * panel today:
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
    val workflow: WorkflowKind = WorkflowKind.Event,
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

    /** The current snapshot of the authoring draft. */
    val draft: QuickCreateDraftState get() = mutableState.value

    /** The last submission error message, if any. Clear on mutate. */
    var submitError: String? by mutableStateOf(null)

    fun openSubpanel(panel: QuickCreatePanel) {
        require(panel != QuickCreatePanel.Base) { "Base is not a subpanel" }
        mutate { it.copy(activePanel = panel) }
    }

    fun backToBase() = mutate { it.copy(activePanel = QuickCreatePanel.Base) }
    fun dismissPanel() = mutate { it.copy(activePanel = null) }
    fun reset() { mutableState.value = QuickCreateDraftState() }

    /** Initialise (or re-initialise) the draft for a create flow with a
     *  web-equivalent default bundle for the requested [workflow].
     *  Mirrors the web `openCreate({ workflow, initialAllDay })` path:
     *  - re-seeds the time slice to a coherent preset (task 30 min duration,
     *    event next-15-min slot 90 min, recurring 09:00–17:00 window, etc.)
     *  - clears identity / plan / windows / recurring so the panel opens
     *    in a known state.
     *  - clears mode / editing ids so this is unambiguously a create.
     */
    fun openCreate(workflow: WorkflowKind, initialAllDay: Boolean = false) {
        val base = QuickCreateDraftState()
        val seeded = defaultsForWorkflow(workflow, initialAllDay, base)
        mutableState.value = seeded.copy(
            mode = QuickCreateMode.Create,
            editingTileId = null,
            editingPlacementId = null,
            activePanel = QuickCreatePanel.Base,
            workflow = workflow,
        )
    }

    /** Web `setAllDay`: when [enabled] is true the time-of-day mode flips
     *  to AllDay and the start/end slots collapse to "00:00" / "23:59" so
     *  the wire still carries a valid all-day window. When false the mode
     *  flips back to Range (start/end are left intact). */
    fun setAllDay(enabled: Boolean) {
        mutate { draft ->
            val t = draft.time
            draft.copy(
                time = if (enabled) {
                    t.copy(timeOfDayMode = QuickCreateTimeOfDayMode.AllDay, timeOfDayStart = "00:00", timeOfDayEnd = "23:59")
                } else {
                    t.copy(timeOfDayMode = QuickCreateTimeOfDayMode.Range)
                },
            )
        }
    }

    /** Apply a [WorkflowKind] switch: pulls the default bundle from
     *  [WORKFLOW_CONFIG], writes it into the draft, and returns the panel to
     *  the base view. Unknown kinds are no-ops.
     *
     * Re-seeds the time slice via [defaultsForWorkflow] so the user sees the
     *  web-parity default for the chosen workflow (task 30 min duration,
     *  event next 15-min slot, recurring 09:00–17:00 window, etc.) the
     *  moment they tap a workflow chip. */
    fun setWorkflow(kind: WorkflowKind) {
        val config = WORKFLOW_CONFIG[kind] ?: return
        val seeded = defaultsForWorkflow(kind, draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.AllDay, draft)
        mutate { draft ->
            seeded.copy(
                workflow = kind,
                identity = draft.identity.copy(kind = config.defaultTileKind),
                plan = draft.plan.copy(role = config.defaultPlanRole),
                recurring = seeded.recurring,
                time = seeded.time,
                activePanel = QuickCreatePanel.Base,
            )
        }
    }

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
        workflow: WorkflowKind = WorkflowKind.Event,
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
            workflow = workflow,
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

    // ── Sub-task authoring (web parity with `useQuickCreateStore`) ──

    /** Append a new task with a self-pointing "done" term. Returns its id. */
    fun addTask(title: String = ""): String {
        val id = UUID.randomUUID().toString()
        val task = QuickCreateTaskDefinition(
            id = id,
            content = QuickCreateTaskContent(title = title),
            complete = QuickCreateConditionNode(3, term = selfTaskTerm(id)),
        )
        mutate { draft ->
            draft.copy(
                plan = draft.plan.copy(
                    completion = draft.plan.completion.copy(
                        tasks = draft.plan.completion.tasks + task,
                    ),
                ),
            )
        }
        return id
    }

    /** Upsert a task by id, preserving insertion order. */
    fun updateTask(task: QuickCreateTaskDefinition) = mutate { draft ->
        val tasks = draft.plan.completion.tasks.map { if (it.id == task.id) task else it }
        draft.copy(plan = draft.plan.copy(completion = draft.plan.completion.copy(tasks = tasks)))
    }

    /** Remove a task and any order rules pointing at it. */
    fun removeTask(id: String) = mutate { draft ->
        val remaining = draft.plan.completion.tasks.filterNot { it.id == id }
        draft.copy(
            plan = draft.plan.copy(
                completion = draft.plan.completion.copy(
                    tasks = remaining.map { task ->
                        task.copy(
                            order = JsonArray(task.order.filter { rule ->
                                val obj = rule as? JsonObject
                                val v = obj?.get("targetTaskId") as? JsonPrimitive
                                v?.content?.takeUnless { it == "null" } != id
                            }),
                        )
                    },
                ),
            ),
        )
    }

    /** Move a task within the list (web `reorderTasks`). */
    fun reorderTask(fromIndex: Int, toIndex: Int) = mutate { draft ->
        val tasks = draft.plan.completion.tasks.toMutableList()
        if (fromIndex !in tasks.indices || toIndex !in tasks.indices) return@mutate draft
        val moved = tasks.removeAt(fromIndex)
        tasks.add(toIndex, moved)
        draft.copy(plan = draft.plan.copy(completion = draft.plan.completion.copy(tasks = tasks)))
    }

    /** Duplicate a task, re-pointing any self-references to the copy. */
    fun duplicateTask(id: String) {
        val source = draft.plan.completion.tasks.find { it.id == id } ?: return
        val newId = UUID.randomUUID().toString()
        val copy = source.copy(
            id = newId,
            content = QuickCreateTaskContent(title = source.content.title, note = source.content.note),
            show = rewriteTaskId(source.show, id, newId),
            complete = rewriteTaskId(source.complete, id, newId) ?: QuickCreateConditionNode(3, term = selfTaskTerm(newId)),
            order = source.order,
            done = false,
        )
        mutate { draft ->
            draft.copy(
                plan = draft.plan.copy(
                    completion = draft.plan.completion.copy(
                        tasks = draft.plan.completion.tasks + copy,
                    ),
                ),
            )
        }
    }

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

// ── Web-parity default bundles for create-time seeding ──

private const val DEFAULT_TASK_DURATION_MS = 30L * 60_000L
private const val DEFAULT_EVENT_DURATION_MS = 90L * 60_000L
private const val DEFAULT_RECURRING_DURATION_MS = 30L * 60_000L

/**
 * Compute the next 15-minute-slot ISO instant at or after [now] in the
 * device's local time zone. Mirrors `nextSlotIso(15)` on the web.
 */
private fun nextSlotIso(slotMinutes: Int, now: Instant = Instant.now()): Instant {
    val zone = java.time.ZoneId.systemDefault()
    val zoned = now.atZone(zone)
    val minuteOfDay = zoned.hour * 60 + zoned.minute
    val next = ((minuteOfDay / slotMinutes) + 1) * slotMinutes
    val target = zoned.withHour(next / 60).withMinute(next % 60).withSecond(0).withNano(0)
    return target.toInstant()
}

/** Local-midnight today, encoded as an ISO instant in the device zone. */
private fun todayLocalMidnightIso(now: Instant = Instant.now()): String {
    val zone = java.time.ZoneId.systemDefault()
    return now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toString()
}

private fun addMinutesToHHMM(hhmm: String, delta: Int): String {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val total = (h * 60 + m + delta).coerceIn(0, 24 * 60 - 1)
    return "%02d:%02d".format(total / 60, total % 60)
}

/** Format an [Instant] as `HH:mm` in the device's local time zone. */
private fun instantToHHMM(instant: Instant): String {
    val zone = java.time.ZoneId.systemDefault()
    val zoned = instant.atZone(zone)
    return "%02d:%02d".format(zoned.hour, zoned.minute)
}

/**
 * Web parity: returns a [QuickCreateDraftState] seeded with a coherent
 * bundle for the requested [workflow]. The caller decides whether to
 * preserve any prior state (e.g. via [QuickCreateStateStore.openCreate]).
 */
private fun defaultsForWorkflow(
    workflow: WorkflowKind,
    initialAllDay: Boolean,
    base: QuickCreateDraftState,
    now: Instant = Instant.now(),
): QuickCreateDraftState {
    val config = WORKFLOW_CONFIG[workflow]
    return when (workflow) {
        WorkflowKind.Task -> base.copy(
            time = base.time.copy(
                span = QuickCreateSpan(start = todayLocalMidnightIso(now), end = ""),
                durationMinMax = QuickCreateDurationRange(DEFAULT_TASK_DURATION_MS, DEFAULT_TASK_DURATION_MS),
                whenMode = QuickCreateWhenMode.Day,
                timeOfDayMode = QuickCreateTimeOfDayMode.Unspecified,
                timeOfDayStart = "",
                timeOfDayEnd = "",
            ),
            recurring = base.recurring.copy(
                repeatMode = config?.defaultRepeatMode ?: base.recurring.repeatMode,
            ),
        )
        WorkflowKind.Event -> {
            if (initialAllDay) {
                base.copy(
                    time = base.time.copy(
                        span = QuickCreateSpan(start = todayLocalMidnightIso(now), end = ""),
                        durationMinMax = QuickCreateDurationRange(DEFAULT_EVENT_DURATION_MS, DEFAULT_EVENT_DURATION_MS),
                        whenMode = QuickCreateWhenMode.Day,
                        timeOfDayMode = QuickCreateTimeOfDayMode.AllDay,
                        timeOfDayStart = "00:00",
                        timeOfDayEnd = "23:59",
                    ),
                    recurring = base.recurring.copy(
                        repeatMode = config?.defaultRepeatMode ?: base.recurring.repeatMode,
                    ),
                )
            } else {
                val startInstant = nextSlotIso(15, now)
                val endInstant = startInstant.plusMillis(DEFAULT_EVENT_DURATION_MS)
                val start = startInstant.toString()
                val end = endInstant.toString()
                base.copy(
                    time = base.time.copy(
                        span = QuickCreateSpan(start = start, end = end),
                        durationMinMax = QuickCreateDurationRange(DEFAULT_EVENT_DURATION_MS, DEFAULT_EVENT_DURATION_MS),
                        whenMode = QuickCreateWhenMode.Range,
                        timeOfDayMode = QuickCreateTimeOfDayMode.Range,
                        timeOfDayStart = instantToHHMM(startInstant),
                        timeOfDayEnd = instantToHHMM(endInstant),
                    ),
                    recurring = base.recurring.copy(
                        repeatMode = config?.defaultRepeatMode ?: base.recurring.repeatMode,
                    ),
                )
            }
        }
        WorkflowKind.Recurring -> base.copy(
            time = base.time.copy(
                span = QuickCreateSpan(start = todayLocalMidnightIso(now), end = ""),
                durationMinMax = QuickCreateDurationRange(DEFAULT_RECURRING_DURATION_MS, DEFAULT_RECURRING_DURATION_MS),
                whenMode = QuickCreateWhenMode.Day,
                timeOfDayMode = QuickCreateTimeOfDayMode.Range,
                timeOfDayStart = "09:00",
                timeOfDayEnd = addMinutesToHHMM("09:00", 60),
            ),
            recurring = base.recurring.copy(
                repeatMode = QuickCreateRepeatMode.Daily,
                intervalValue = 1,
                intervalUnit = QuickCreateIntervalUnit.Day,
                weekdayMask = 0b0011111,
            ),
        )
        WorkflowKind.Detailed -> base.copy(
            time = base.time.copy(
                whenMode = QuickCreateWhenMode.None,
                timeOfDayMode = QuickCreateTimeOfDayMode.Unspecified,
            ),
        )
    }
}
