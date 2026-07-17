package app.tastile.android.ui.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shell-level UI state for the [androidx.activity.ComponentActivity] host.
 *
 * Owns cross-screen shell concerns (top-bar visibility, system-bar contrast
 * enforcement) so [app.tastile.android.MainActivity] can stay a thin host
 * instead of being a controller. Screen-level state belongs in screen-level
 * ViewModels; this class deliberately does not duplicate that surface.
 */
data class AppShellUiState(
    val isTopBarVisible: Boolean = true,
    val isSystemBarsContrastEnforced: Boolean = false,
)

@HiltViewModel
class AppShellViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AppShellUiState())
    val uiState: StateFlow<AppShellUiState> = _uiState.asStateFlow()

    fun setTopBarVisible(visible: Boolean) {
        if (_uiState.value.isTopBarVisible == visible) return
        _uiState.value = _uiState.value.copy(isTopBarVisible = visible)
    }

    fun setSystemBarsContrastEnforced(enforced: Boolean) {
        if (_uiState.value.isSystemBarsContrastEnforced == enforced) return
        _uiState.value = _uiState.value.copy(isSystemBarsContrastEnforced = enforced)
    }
}