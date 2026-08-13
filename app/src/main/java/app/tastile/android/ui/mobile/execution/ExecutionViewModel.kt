package app.tastile.android.ui.mobile.execution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.api.ExecutionView
import app.tastile.android.data.execution.ExecutionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExecutionUiState(
    val execution: ExecutionView? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ExecutionViewModel @Inject constructor(
    private val repository: ExecutionRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ExecutionUiState())
    val state = mutableState.asStateFlow()

    init { restore() }

    fun restore() = mutate { repository.restoreActiveExecution() }
    fun start(placementId: String) = mutate { repository.start(placementId) }
    fun pause() = state.value.execution?.id?.let { id -> mutate { repository.pause(id) } }
    fun resume() = state.value.execution?.id?.let { id -> mutate { repository.resume(id) } }

    fun finish(kind: Int = 0, note: String? = null) {
        val id = state.value.execution?.id ?: return
        viewModelScope.launch {
            mutableState.value = state.value.copy(loading = true, error = null)
            runCatching { repository.finish(id, kind, note) }
                .onSuccess { mutableState.value = ExecutionUiState() }
                .onFailure { mutableState.value = state.value.copy(loading = false, error = it.message) }
        }
    }

    private fun mutate(block: suspend () -> ExecutionView?) {
        viewModelScope.launch {
            mutableState.value = state.value.copy(loading = true, error = null)
            runCatching { block() }
                .onSuccess { mutableState.value = ExecutionUiState(execution = it) }
                .onFailure { mutableState.value = state.value.copy(loading = false, error = it.message) }
        }
    }
}
