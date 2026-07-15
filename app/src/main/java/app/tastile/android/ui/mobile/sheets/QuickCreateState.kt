package app.tastile.android.ui.mobile.sheets

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The Web quick-create base panel and its eight subpanels. */
enum class QuickCreatePanel { Base, Intent, Time, Duration, Recurring, References, Completion, Meta, Behavior }

enum class QuickCreateTileKind { Recurring, Placement, Execution }
enum class QuickCreatePlanRole { Executable, Label }
enum class QuickCreateIntent { FinishOnce, Recurring, MaximizeWithinInterval, LabelOnly }
enum class QuickCreateWhenMode { None, Day, Range, Reference }
enum class QuickCreateTimeOfDayMode { AllDay, Range, Unspecified }
enum class QuickCreateRecurringLifecycleState { Active, Paused, Archived }
enum class QuickCreateRepeatMode { Once, Daily, Weekly, Interval, Condition }

data class QuickCreateVisual(val color: String = "#3b82f6", val icon: String = "check-circle")

data class QuickCreateIdentity(
    val kind: QuickCreateTileKind = QuickCreateTileKind.Placement,
    val title: String = "",
    val description: String? = null,
    val externalId: String? = null,
    val visual: QuickCreateVisual = QuickCreateVisual(),
)

data class QuickCreatePlanReference(val id: String, val kind: String)

sealed interface QuickCreateConditionNode {
    data class All(val children: List<QuickCreateConditionNode>) : QuickCreateConditionNode
    data class Any(val children: List<QuickCreateConditionNode>) : QuickCreateConditionNode
    data class Term(val kind: String, val value: String, val state: String) : QuickCreateConditionNode
}

data class QuickCreateTimeRequirement(val id: String, val minMinutes: Int?, val maxMinutes: Int?)
data class QuickCreateTaskDefinition(val id: String, val title: String, val note: String? = null)

data class QuickCreatePlanCompletion(
    val root: QuickCreateConditionNode? = null,
    val timeRequirements: List<QuickCreateTimeRequirement> = emptyList(),
    val tasks: List<QuickCreateTaskDefinition> = emptyList(),
)

data class QuickCreatePlan(
    val role: QuickCreatePlanRole = QuickCreatePlanRole.Executable,
    val intent: QuickCreateIntent = QuickCreateIntent.FinishOnce,
    val references: List<QuickCreatePlanReference> = emptyList(),
    val completion: QuickCreatePlanCompletion = QuickCreatePlanCompletion(),
)

data class QuickCreateSpan(val start: String = "", val end: String = "")
data class QuickCreateDurationRange(val minMinutes: Int? = 30, val maxMinutes: Int? = 90)

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

data class QuickCreateWindow(val kind: String, val startOffsetMinute: Int, val endOffsetMinute: Int)

data class QuickCreateRecurringLife(
    val activeFrom: String? = null,
    val activeUntil: String? = null,
    val state: QuickCreateRecurringLifecycleState = QuickCreateRecurringLifecycleState.Active,
)

data class QuickCreateFrameRule(val id: String, val stepMinutes: Int, val weekdayMask: Int)

data class QuickCreateRecurring(
    val life: QuickCreateRecurringLife = QuickCreateRecurringLife(),
    val frameRules: List<QuickCreateFrameRule> = emptyList(),
    val rules: List<String> = emptyList(),
    val repeatMode: QuickCreateRepeatMode = QuickCreateRepeatMode.Once,
    val weekdayMask: Int = 0b0011111,
    val endDate: String? = null,
)

data class QuickCreateMeta(
    val ownerSubjectId: String? = null,
    val tags: List<String> = emptyList(),
    val memo: String = "",
)

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
