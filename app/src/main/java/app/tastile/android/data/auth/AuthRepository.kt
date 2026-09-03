package app.tastile.android.data.auth

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.core.net.toUri
import app.tastile.android.BuildConfig
import app.tastile.android.data.auth.GoogleSignInLauncher
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: BetterAuthHttpClient,
    private val apiTokenCache: Lazy<ApiTokenCache>,
    private val googleSignInLauncher: GoogleSignInLauncher,
) : CurrentUserProvider, AuthRepositoryContract {

    private val sessionPrefs = EncryptedTokenStorage.sessionTokenPrefs(context)
    private val _authState = MutableStateFlow(loadStoredAuthState())
    override val authState: StateFlow<TastileAuthState> = _authState.asStateFlow()

    val fallbackUserId: String? get() = currentUserId()
    val fallbackEmail: String? get() = currentEmail()

    @Suppress("unused")
    val currentSession: Any? get() = null

    override fun currentUserId(): String? =
        (_authState.value as? TastileAuthState.Authenticated)?.userId
            ?: sessionPrefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }

    override fun currentEmail(): String? =
        (_authState.value as? TastileAuthState.Authenticated)?.email
            ?: sessionPrefs.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() }

    override fun currentSessionToken(): String? =
        sessionPrefs.getString(KEY_SESSION_TOKEN, null)?.takeIf { it.isNotBlank() }

    override suspend fun signInWithEmail(email: String, password: String) {
        val session = httpClient.signIn(email = email, password = password)
        persistSession(session)
    }

    override suspend fun signUpWithEmail(email: String, password: String, name: String) {
        val session = httpClient.signUp(email = email, password = password, name = name)
        persistSession(session)
    }

    override suspend fun signInWithProvider(provider: String) {
        val normalized = provider.trim().lowercase().takeIf { it.isNotBlank() } ?: return
        val authUrl = "${BuildConfig.WEB_BASE_URL.trim().trimEnd('/')}/login?provider=$normalized"
        context.startActivity(
            Intent(Intent.ACTION_VIEW, authUrl.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override suspend fun signInWithGoogle() {
        val idToken = googleSignInLauncher.getIdToken()
        val session = httpClient.signInWithGoogleIdToken(idToken)
        persistSession(session)
    }

    override suspend fun signOut() {
        val token = currentSessionToken()
        if (!token.isNullOrBlank()) {
            runCatching { httpClient.signOut(token) }
        }
        apiTokenCache.get().signOut()
        sessionPrefs.edit { clear() }
        _authState.value = TastileAuthState.Unauthenticated
    }

    private fun persistSession(session: BetterAuthHttpClient.BetterAuthSession) {
        sessionPrefs.edit {
            putString(KEY_SESSION_TOKEN, session.sessionToken)
            putString(KEY_USER_ID, session.userId)
            putString(KEY_EMAIL, session.email)
            session.expiresAtEpochSeconds?.let { putLong(KEY_EXPIRES_AT, it) }
                ?: remove(KEY_EXPIRES_AT)
        }
        apiTokenCache.get().invalidate()
        _authState.value = TastileAuthState.Authenticated(
            userId = session.userId,
            email = session.email,
        )
    }

    private fun loadStoredAuthState(): TastileAuthState {
        val token = sessionPrefs.getString(KEY_SESSION_TOKEN, null)?.takeIf { it.isNotBlank() }
            ?: return TastileAuthState.Unauthenticated
        val userId = sessionPrefs.getString(KEY_USER_ID, null)
        if (userId.isNullOrBlank()) return TastileAuthState.Unauthenticated
        return TastileAuthState.Authenticated(
            userId = userId,
            email = sessionPrefs.getString(KEY_EMAIL, null),
        )
    }

    private companion object {
        const val KEY_SESSION_TOKEN = "session_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
