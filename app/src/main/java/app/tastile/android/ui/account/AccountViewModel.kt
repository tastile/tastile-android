package app.tastile.android.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.model.Plan
import app.tastile.android.data.model.Profile
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.TastileAuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _email = MutableStateFlow<String>("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val authState = authRepository.getAuthStateStream.value as? TastileAuthState.Authenticated
                val legacySession = authRepository.currentSession
                val userId = authState?.userId ?: legacySession.readNestedString("user", "id")
                
                _email.value = authState?.email ?: legacySession.readNestedString("user", "email").orEmpty()
                
                if (userId != null) {
                    _profile.value = profileRepository.getProfile(userId)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load profile"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            try {
                _error.value = null
                val userId = authRepository.currentUserId()
                
                if (userId != null) {
                    val updatedProfile = profileRepository.updateDisplayName(userId, name)
                    if (updatedProfile != null) {
                        _profile.value = updatedProfile
                    } else {
                        _error.value = "Failed to update display name"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update display name"
                e.printStackTrace()
            }
        }
    }

    fun toggleEditing() {
        _isEditing.value = !_isEditing.value
    }

    fun signOut(onSignOut: () -> Unit) {
        viewModelScope.launch {
            try {
                _error.value = null
                authRepository.signOut()
                onSignOut()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to sign out"
                e.printStackTrace()
            }
        }
    }

    fun isProUser(): Boolean {
        return _profile.value?.plan == Plan.PRO.value
    }

    fun clearError() {
        _error.value = null
    }
}

private fun Any?.readNestedString(vararg propertyNames: String): String? {
    var current: Any? = this
    for (propertyName in propertyNames) {
        current = current?.javaClass?.methods
            ?.firstOrNull { method ->
                method.parameterCount == 0 &&
                    method.name.equals(
                        "get${propertyName.replaceFirstChar { it.uppercase() }}",
                        ignoreCase = true
                    )
            }
            ?.invoke(current)
    }
    return current as? String
}
