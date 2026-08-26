package app.tastile.android.ui.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.R
import app.tastile.android.data.model.Plan
import app.tastile.android.data.model.Profile
import app.tastile.android.data.auth.AuthRepository
import app.tastile.android.data.auth.TastileAuthState
import app.tastile.android.data.user.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val context: Context,
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
                val authState = authRepository.authState.value as? TastileAuthState.Authenticated
                val userId = authState?.userId ?: authRepository.currentUserId()
                _email.value = authState?.email ?: authRepository.currentEmail().orEmpty()

                if (userId != null) {
                    _profile.value = profileRepository.getProfile(userId)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: context.getString(R.string.account_error_load_profile)
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
                        _error.value = context.getString(R.string.account_error_update_display_name)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: context.getString(R.string.account_error_update_display_name)
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
                _error.value = e.message ?: context.getString(R.string.account_error_sign_out)
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
