package app.tastile.android.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.repository.AuthRepositoryContract
import app.tastile.android.data.repository.TastileAuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepositoryContract
) : ViewModel() {
    val authState: StateFlow<TastileAuthState> = authRepository.authState
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    fun signInWithCognito(context: Context) {
        viewModelScope.launch {
            if (_isSigningIn.value) return@launch
            try {
                _isSigningIn.value = true
                _error.value = null
                authRepository.signInWithCognito(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to sign in"
            } finally {
                _isSigningIn.value = false
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            if (_isSigningIn.value) return@launch
            try {
                _isSigningIn.value = true
                _error.value = null
                authRepository.signInWithGoogle(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to sign in"
            } finally {
                _isSigningIn.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                _error.value = null
                authRepository.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Unable to sign out"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
