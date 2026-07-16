package app.tastile.android.ui.mobile.sheets

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
enum class QuickCreatePanel { Base, Intent, Time, Duration, Recurring, References, Completion, Meta, Behavior }

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

data class QuickCreatePlanning(
    val placementRules: JsonArray = JsonArray(emptyList()),
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
)

data class QuickCreateMeta(
    val ownerSubjectId: String? = null,
    val tags: List<String> = emptyList(),
    val memo: String = "",
)

/** A workspace summary used by the Meta project's catalog. */
data class QuickCreateProject(val id: String, val displayName: String)

/**
 * UI-free Web-equivalent quick-create draft. Panels mutate this one StateFlow;
 * panel navigation never reconstructs or clears field values.
 */
data class QuickCreateDraftState(
    val activePanel: QuickCreatePanel? = QuickCreatePanel.Base,
    val identity: QuickCreateIdentity = QuickCreateIdentity(),
    val plan: QuickCreatePlan = QuickCreatePlan(),
    val time: QuickCreateTime = QuickCreateTime(),
    val windows: List<QuickCreateWindow> = emptyList(),
    val recurring: QuickCreateRecurring = QuickCreateRecurring(),
    val meta: QuickCreateMeta = QuickCreateMeta(),
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

    /** The behavior panel selects the plan's role; it does not introduce a second behavior model. */
    fun updateBehavior(role: QuickCreatePlanRole) = mutate { it.copy(plan = it.plan.copy(role = role)) }

    private inline fun mutate(transform: (QuickCreateDraftState) -> QuickCreateDraftState) {
        mutableState.value = transform(mutableState.value)
    }
}

private const val DEFAULT_TASK_ID = "task_default"

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
    content = QuickCreateTaskContent(title = "作業完了"),
    complete = defaultTermCondition(),
)
