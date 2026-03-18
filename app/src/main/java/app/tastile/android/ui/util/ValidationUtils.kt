package app.tastile.android.ui.util

data class QuickCreateValidation(
    val temporalOrderValid: Boolean,
    val durationReady: Boolean,
    val recurrenceReady: Boolean
) {
    val canSubmit: Boolean = temporalOrderValid && durationReady && recurrenceReady
}

fun validateQuickCreate(
    tileKind: String,
    objectiveMode: String,
    hasAnyTemporalConstraint: Boolean,
    workTargetMin: Int?,
    temporalOrderValid: Boolean,
    recurrenceInterval: Int
): QuickCreateValidation {
    val isRecurring = objectiveMode == "recurring"
    val durationReady = when {
        tileKind != "work" -> true
        isRecurring -> (workTargetMin ?: 0) > 0
        else -> !hasAnyTemporalConstraint || (workTargetMin ?: 0) > 0
    }
    val recurrenceReady = !isRecurring || recurrenceInterval > 0
    return QuickCreateValidation(
        temporalOrderValid = temporalOrderValid,
        durationReady = durationReady,
        recurrenceReady = recurrenceReady
    )
}
