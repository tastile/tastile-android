package app.tastile.android.ui.mobile.decision

import app.tastile.android.data.api.FeedbackChangeDto
import app.tastile.android.data.api.PendingSessionView
import app.tastile.android.data.api.SessionDetailView
import app.tastile.android.data.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

fun interface TimelineRefresh {
    suspend fun refresh()
}

data class DecisionUiState(
    val pending: List<PendingSessionView> = emptyList(),
    val selected: SessionDetailView? = null,
    val submitting: Boolean = false,
    val error: String? = null,
)

class DecisionViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val timelineRefresh: TimelineRefresh,
) {
    private val mutableState = MutableStateFlow(DecisionUiState())
    val state = mutableState.asStateFlow()

    suspend fun loadPending() {
        runCatching { sessions.pending() }
            .onSuccess { mutableState.value = state.value.copy(pending = it, error = null) }
            .onFailure { mutableState.value = state.value.copy(error = it.message) }
    }

    suspend fun select(sessionId: String) {
        runCatching { sessions.detail(sessionId) }
            .onSuccess { mutableState.value = state.value.copy(selected = it, error = null) }
            .onFailure { mutableState.value = state.value.copy(error = it.message) }
    }

    suspend fun answer(sessionId: String, baseRevision: Long, changes: List<FeedbackChangeDto>) {
        mutableState.value = state.value.copy(submitting = true, error = null)
        runCatching {
            sessions.submitFeedback(sessionId, baseRevision, changes)
            timelineRefresh.refresh()
            sessions.pending()
        }.onSuccess {
            mutableState.value = DecisionUiState(pending = it)
        }.onFailure {
            mutableState.value = state.value.copy(submitting = false, error = it.message)
        }
    }
}
