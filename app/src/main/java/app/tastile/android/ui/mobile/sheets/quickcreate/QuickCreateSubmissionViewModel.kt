package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickCreateSubmissionUiState(val isSubmitting: Boolean = false, val error: String? = null, val createdTileId: String? = null)

@HiltViewModel
class QuickCreateSubmissionViewModel @Inject constructor(client: V1ApiClient) : ViewModel() {
    private val dispatcher = QuickCreateSubmissionDispatcher(V1QuickCreateCommandGateway(client))
    private val mutableState = MutableStateFlow(QuickCreateSubmissionUiState())
    val state = mutableState.asStateFlow()

    fun submit(draft: QuickCreateDraftState) {
        if (mutableState.value.isSubmitting) return
        val validation = quickCreateSubmissionValidation(draft)
        if (!validation.isValid) {
            mutableState.value = QuickCreateSubmissionUiState(error = validation.message)
            return
        }
        viewModelScope.launch {
            mutableState.value = QuickCreateSubmissionUiState(isSubmitting = true)
            mutableState.value = when (val result = dispatcher.submit(draft)) {
                is QuickCreateSubmitResult.Success -> QuickCreateSubmissionUiState(createdTileId = result.tileId)
                is QuickCreateSubmitResult.Failure -> QuickCreateSubmissionUiState(error = result.message)
            }
        }
    }

    fun consumeCreatedTile() { mutableState.value = QuickCreateSubmissionUiState() }
}
