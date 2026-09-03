package app.tastile.android.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.R
import app.tastile.android.data.auth.AuthRepositoryContract
import app.tastile.android.data.auth.GoogleSignInUnavailableException
import app.tastile.android.data.auth.TastileAuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepositoryContract,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val authState: StateFlow<TastileAuthState> = authRepository.authState
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    private val _isGoogleSigningIn = MutableStateFlow(false)
    val isGoogleSigningIn: StateFlow<Boolean> = _isGoogleSigningIn.asStateFlow()

    fun onEmailChange(value: String) {
        _email.value = value
    }

    fun onPasswordChange(value: String) {
        _password.value = value
    }

    fun signInWithEmail(@Suppress("UNUSED_PARAMETER") context: Context) {
        val email = _email.value.trim()
        val password = _password.value
        if (email.isBlank() || password.isBlank()) {
            _error.value = this.context.getString(R.string.login_error_sign_in_failed)
            return
        }
        viewModelScope.launch {
            if (_isSigningIn.value) return@launch
            try {
                _isSigningIn.value = true
                _error.value = null
                authRepository.signInWithEmail(email = email, password = password)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: this@LoginViewModel.context.getString(R.string.login_error_sign_in_failed)
            } finally {
                _isSigningIn.value = false
            }
        }
    }

    fun signUp(@Suppress("UNUSED_PARAMETER") context: Context) {
        val email = _email.value.trim()
        val password = _password.value
        if (email.isBlank() || password.isBlank()) {
            _error.value = this.context.getString(R.string.login_error_sign_in_failed)
            return
        }
        viewModelScope.launch {
            if (_isSigningIn.value) return@launch
            try {
                _isSigningIn.value = true
                _error.value = null
                authRepository.signUpWithEmail(
                    email = email,
                    password = password,
                    name = email.substringBefore('@').ifBlank { email },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: this@LoginViewModel.context.getString(R.string.login_error_sign_in_failed)
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
                _error.value = context.getString(R.string.login_error_sign_out_failed)
            }
        }
    }

    fun signInWithGoogle(@Suppress("UNUSED_PARAMETER") context: Context) {
        if (_isGoogleSigningIn.value) return
        viewModelScope.launch {
            try {
                _isGoogleSigningIn.value = true
                _error.value = null
                authRepository.signInWithGoogle()
            } catch (e: CancellationException) {
                throw e
            } catch (e: GoogleSignInUnavailableException) {
                // No Google account on device / Play Services missing / user
                // cancel. Fall back to the existing web OAuth handoff so the
                // user still gets a path to Google sign-in.
                try {
                    authRepository.signInWithProvider("google")
                } catch (fallback: Exception) {
                    _error.value = this@LoginViewModel.context.getString(
                        R.string.login_error_google_unavailable,
                    )
                }
            } catch (e: Exception) {
                _error.value = e.message
                    ?: this@LoginViewModel.context.getString(R.string.login_error_google_failed)
            } finally {
                _isGoogleSigningIn.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
