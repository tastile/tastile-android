package app.tastile.android.ui.mobile.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.repository.AccountProfile
import app.tastile.android.data.repository.AccountRepository
import app.tastile.android.data.repository.AccountTokenView
import app.tastile.android.data.repository.AccountTokenWithSecret
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the mobile `AccountSheet` (profile + change-email + login
 * methods) and `TokensSheet` (list + create + revoke). Mirrors the React
 * state in `tastile-web/src/app/dashboard/preferences/account/page.tsx`
 * (profile/loading/notice) and the React state in
 * `tastile-web/src/components/account/AccessTokenSection.tsx`
 * (tokens/createdToken/loading/error).
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    data class ProfileState(
        val profile: AccountProfile? = null,
        val loading: Boolean = false,
        val error: String? = null,
        val pendingEmail: String = "",
        val verificationCode: String = "",
        val submitting: Boolean = false,
        val notice: Notice? = null,
    )

    sealed interface Notice {
        data class Success(val message: String) : Notice
        data class Error(val message: String) : Notice
    }

    data class TokenState(
        val tokens: List<AccountTokenView> = emptyList(),
        val loading: Boolean = false,
        val submitting: Boolean = false,
        val error: String? = null,
        val created: AccountTokenWithSecret? = null,
    )

    private val _profile = MutableStateFlow(ProfileState())
    val profile: StateFlow<ProfileState> = _profile.asStateFlow()

    private val _tokens = MutableStateFlow(TokenState())
    val tokens: StateFlow<TokenState> = _tokens.asStateFlow()

    init {
        loadProfile()
        loadTokens()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profile.update { it.copy(loading = true, error = null, notice = null) }
            runCatching { accountRepository.loadProfile() }
                .onSuccess { profile ->
                    _profile.update {
                        it.copy(
                            profile = profile,
                            loading = false,
                            error = null,
                            pendingEmail = profile.email.orEmpty(),
                        )
                    }
                }
                .onFailure { err ->
                    _profile.update {
                        it.copy(loading = false, error = err.message ?: "load failed")
                    }
                }
        }
    }

    fun updatePendingEmail(value: String) {
        _profile.update { it.copy(pendingEmail = value) }
    }

    fun updateVerificationCode(value: String) {
        _profile.update { it.copy(verificationCode = value) }
    }

    fun sendEmailCode() {
        val email = _profile.value.pendingEmail
        if (email.isBlank()) return
        viewModelScope.launch {
            _profile.update { it.copy(submitting = true, notice = null) }
            runCatching { accountRepository.startEmailChange(email) }
                .onSuccess {
                    _profile.update {
                        it.copy(submitting = false, notice = Notice.Success("code_sent"))
                    }
                }
                .onFailure { err ->
                    _profile.update {
                        it.copy(
                            submitting = false,
                            notice = Notice.Error(err.message ?: "send_failed"),
                        )
                    }
                }
        }
    }

    fun verifyEmailCode() {
        val code = _profile.value.verificationCode
        if (code.isBlank()) return
        viewModelScope.launch {
            _profile.update { it.copy(submitting = true, notice = null) }
            runCatching { accountRepository.verifyEmailChange(code) }
                .onSuccess {
                    _profile.update {
                        it.copy(
                            submitting = false,
                            verificationCode = "",
                            notice = Notice.Success("email_updated"),
                        )
                    }
                    loadProfile()
                }
                .onFailure { err ->
                    _profile.update {
                        it.copy(
                            submitting = false,
                            notice = Notice.Error(err.message ?: "verify_failed"),
                        )
                    }
                }
        }
    }

    fun loadTokens() {
        viewModelScope.launch {
            _tokens.update { it.copy(loading = true, error = null) }
            runCatching { accountRepository.listTokens() }
                .onSuccess { list ->
                    _tokens.update { it.copy(tokens = list, loading = false, error = null) }
                }
                .onFailure { err ->
                    _tokens.update {
                        it.copy(loading = false, error = err.message ?: "load_failed")
                    }
                }
        }
    }

    fun createToken(label: String?) {
        viewModelScope.launch {
            _tokens.update { it.copy(submitting = true, error = null, created = null) }
            runCatching { accountRepository.createToken(label?.takeIf { it.isNotBlank() }) }
                .onSuccess { created ->
                    _tokens.update { it.copy(submitting = false, created = created) }
                    loadTokens()
                }
                .onFailure { err ->
                    _tokens.update {
                        it.copy(submitting = false, error = err.message ?: "create_failed")
                    }
                }
        }
    }

    fun revokeToken(id: String) {
        viewModelScope.launch {
            _tokens.update { it.copy(submitting = true, error = null) }
            runCatching { accountRepository.revokeToken(id) }
                .onSuccess {
                    _tokens.update { it.copy(submitting = false) }
                    loadTokens()
                }
                .onFailure { err ->
                    _tokens.update {
                        it.copy(submitting = false, error = err.message ?: "revoke_failed")
                    }
                }
        }
    }

    fun dismissCreatedToken() {
        _tokens.update { it.copy(created = null) }
    }

    fun dismissNotice() {
        _profile.update { it.copy(notice = null) }
    }
}
