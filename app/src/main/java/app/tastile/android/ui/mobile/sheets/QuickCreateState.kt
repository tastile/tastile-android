package app.tastile.android.ui.mobile.sheets

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Panels used by the Web quick-create flow. The bottom sheet owns presentation only. */
enum class QuickCreatePanel {
    Base,
    Identity,
    Plan,
    Time,
    Duration,
    Recurring,
    References,
    Completion,
    Meta,
    Behavior,
}

enum class QuickCreatePlanRole { Executable, Label }

enum class QuickCreateIntent { FinishOnce, Recurring, MaximizeWithinInterval, LabelOnly }

data class QuickCreateVisual(
    val color: String = "#3b82f6",
    val icon: String = "check-circle",
)

data class QuickCreateIdentity(
    val title: String = "",
    val description: String? = null,
    val externalId: String? = null,
    val visual: QuickCreateVisual = QuickCreateVisual(),
)

data class QuickCreatePlan(
    val role: QuickCreatePlanRole = QuickCreatePlanRole.Executable,
    val intent: QuickCreateIntent = QuickCreateIntent.FinishOnce,
)

data class QuickCreateTime(
    val spanStart: String? = null,
    val spanEnd: String? = null,
    val allDay: Boolean = true,
)

data class QuickCreateDuration(
    val minMinutes: Int? = 30,
    val maxMinutes: Int? = 90,
)

data class QuickCreateFrameRule(
    val stepMinutes: Int = 24 * 60,
    val weekdayMask: Int = 0b0011111,
)

data class QuickCreateRecurring(
    val enabled: Boolean = false,
    val activeFrom: String? = null,
    val activeUntil: String? = null,
    val windowStartMinute: Int? = null,
    val windowEndMinute: Int? = null,
    val frameRule: QuickCreateFrameRule? = null,
)

data class QuickCreateReference(
    val id: String,
    val label: String,
    val kind: String,
)

data class QuickCreateCompletion(
    val manualDescription: String = "",
    val taskTitles: List<String> = emptyList(),
    val timeRequirementMinMinutes: Int? = null,
    val timeRequirementMaxMinutes: Int? = null,
)

data class QuickCreateMeta(
    val project: String = "",
    val tags: List<String> = emptyList(),
    val memo: String = "",
)

data class QuickCreateBehavior(
    val promptOnStart: Boolean = false,
    val promptOnEnd: Boolean = true,
    val breakSplitsWork: Boolean = true,
    val autoStartAllowed: Boolean = false,
    val autoEndAllowed: Boolean = false,
)

/**
 * Web-compatible draft for a new tile. This is deliberately UI-free: Compose panels
 * render and mutate this one state source, and navigation never rebuilds the draft.
 */
data class QuickCreateDraftState(
    val activePanel: QuickCreatePanel? = QuickCreatePanel.Base,
    val identity: QuickCreateIdentity = QuickCreateIdentity(),
    val plan: QuickCreatePlan = QuickCreatePlan(),
    val time: QuickCreateTime = QuickCreateTime(),
    val duration: QuickCreateDuration = QuickCreateDuration(),
    val recurring: QuickCreateRecurring = QuickCreateRecurring(),
    val references: List<QuickCreateReference> = emptyList(),
    val completion: QuickCreateCompletion = QuickCreateCompletion(),
    val meta: QuickCreateMeta = QuickCreateMeta(),
    val behavior: QuickCreateBehavior = QuickCreateBehavior(),
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

    fun updateIdentity(title: String? = null, description: String? = null, color: String? = null, icon: String? = null, externalId: String? = null) =
        mutate { draft ->
            draft.copy(identity = draft.identity.copy(
                title = title ?: draft.identity.title,
                description = description ?: draft.identity.description,
                externalId = externalId ?: draft.identity.externalId,
                visual = draft.identity.visual.copy(color = color ?: draft.identity.visual.color, icon = icon ?: draft.identity.visual.icon),
            ))
        }

    fun updatePlan(role: QuickCreatePlanRole? = null, intent: QuickCreateIntent? = null) =
        mutate { it.copy(plan = it.plan.copy(role = role ?: it.plan.role, intent = intent ?: it.plan.intent)) }

    fun updateTime(spanStart: String? = null, spanEnd: String? = null, allDay: Boolean? = null) =
        mutate { it.copy(time = it.time.copy(spanStart = spanStart ?: it.time.spanStart, spanEnd = spanEnd ?: it.time.spanEnd, allDay = allDay ?: it.time.allDay)) }

    fun updateDuration(minMinutes: Int? = null, maxMinutes: Int? = null) =
        mutate { it.copy(duration = it.duration.copy(minMinutes = minMinutes ?: it.duration.minMinutes, maxMinutes = maxMinutes ?: it.duration.maxMinutes)) }

    fun updateRecurring(enabled: Boolean? = null, activeFrom: String? = null, activeUntil: String? = null, windowStartMinute: Int? = null, windowEndMinute: Int? = null, frameRule: QuickCreateFrameRule? = null) =
        mutate { it.copy(recurring = it.recurring.copy(enabled = enabled ?: it.recurring.enabled, activeFrom = activeFrom ?: it.recurring.activeFrom, activeUntil = activeUntil ?: it.recurring.activeUntil, windowStartMinute = windowStartMinute ?: it.recurring.windowStartMinute, windowEndMinute = windowEndMinute ?: it.recurring.windowEndMinute, frameRule = frameRule ?: it.recurring.frameRule)) }

    fun updateReferences(references: List<QuickCreateReference>) = mutate { it.copy(references = references) }

    fun updateCompletion(completion: QuickCreateCompletion) = mutate { it.copy(completion = completion) }

    fun updateMeta(project: String? = null, tags: List<String>? = null, memo: String? = null) =
        mutate { it.copy(meta = it.meta.copy(project = project ?: it.meta.project, tags = tags ?: it.meta.tags, memo = memo ?: it.meta.memo)) }

    fun updateBehavior(behavior: QuickCreateBehavior) = mutate { it.copy(behavior = behavior) }

    private inline fun mutate(transform: (QuickCreateDraftState) -> QuickCreateDraftState) {
        mutableState.value = transform(mutableState.value)
    }
}
