package app.tastile.android.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.repository.AuthRepositoryContract
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepositoryContract
) : ViewModel() {

    val sessionStatus: StateFlow<SessionStatus> = authRepository.sessionStatus
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signInWithGoogle() {
        viewModelScope.launch {
            try {
                _error.value = null
                authRepository.signInWithGoogle()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to sign in"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                _error.value = null
                authRepository.signOut()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to sign out"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
