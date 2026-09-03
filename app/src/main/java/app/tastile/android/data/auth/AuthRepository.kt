package app.tastile.android.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import app.tastile.android.BuildConfig
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
        // The web login page reads `next` from the query string and forwards
        // it as the BetterAuth `callbackURL` (see tastile-web
        // login-panel.tsx → handleSocial). After BetterAuth completes the
        // OAuth dance, the bridge route at `/api/auth/bridge?next=...`
        // redirects to our `/auth/callback` hand-off, which 302s to
        // `tastile://auth/callback?session=...&v1_token=...`. Without
        // `next=/auth/callback` the flow lands on `/dashboard` and the
        // user has no way to return to the Android app.
        val authUrl =
            "${BuildConfig.WEB_BASE_URL.trim().trimEnd('/')}/login" +
                "?provider=$normalized" +
                "&next=${android.net.Uri.encode(NATIVE_CALLBACK_PATH)}"
        context.startActivity(
            Intent(Intent.ACTION_VIEW, authUrl.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override suspend fun signInWithGoogle() {
        val idToken = googleSignInLauncher.getIdToken()
        val session = httpClient.signInWithGoogleIdToken(idToken)
        persistSession(session)
    }

    /**
     * Parses the OAuth handoff [Uri] the system delivers to MainActivity
     * via the `tastile://auth/callback` intent filter. Persists the
     * BetterAuth session, adopts the server-minted v1 API token, and flips
     * [authState] to [TastileAuthState.Authenticated] so the auth gate in
     * [app.tastile.android.ui.mobile.MobileNavGraph] renders the dashboard.
     *
     * Validation rules:
     *   - scheme must be `tastile`, host must be `auth`, path must be
     *     `/callback` (matches the manifest intent filter);
     *   - `session` and `user_id` query params are required;
     *   - `v1_token` is required for the dashboard to come up
     *     authenticated against the v1 backend (without it the first v1
     *     call would still need a mint round-trip, but the rest of the
     *     state is still usable);
     *   - any other parse error returns `false` and leaves existing state
     *     untouched.
     *
     * Returns `true` only on a complete, accepted handoff.
     */
    override fun completeAuthFromCallback(uri: Uri): Boolean {
        if (!isAuthCallbackUri(uri)) {
            Log.w(TAG, "completeAuthFromCallback: rejected non-callback URI: $uri")
            return false
        }
        val sessionToken = uri.getQueryParameter("session")?.trim().orEmpty()
        val userId = uri.getQueryParameter("user_id")?.trim().orEmpty()
        val email = uri.getQueryParameter("email")?.trim()?.takeIf { it.isNotBlank() }
        val expiresAtRaw = uri.getQueryParameter("expires_at")?.trim().orEmpty()
        val v1Token = uri.getQueryParameter("v1_token")?.trim().orEmpty()

        if (sessionToken.isEmpty() || userId.isEmpty() || v1Token.isEmpty()) {
            Log.w(
                TAG,
                "completeAuthFromCallback: missing required field(s) — " +
                    "session=${sessionToken.isNotEmpty()} " +
                    "user_id=${userId.isNotEmpty()} " +
                    "v1_token=${v1Token.isNotEmpty()}",
            )
            return false
        }
        val expiresAtEpochSeconds = expiresAtRaw.toLongOrNull()?.takeIf { it > 0 }

        // 1. Persist session + identity. Mirrors persistSession so the
        //    stored shape matches what email sign-in produces, but does not
        //    round-trip through BetterAuthHttpClient (the session is already
        //    established on the server — the URI carries it).
        sessionPrefs.edit {
            putString(KEY_SESSION_TOKEN, sessionToken)
            putString(KEY_USER_ID, userId)
            if (email != null) putString(KEY_EMAIL, email) else remove(KEY_EMAIL)
            if (expiresAtEpochSeconds != null) {
                putLong(KEY_EXPIRES_AT, expiresAtEpochSeconds)
            } else {
                remove(KEY_EXPIRES_AT)
            }
        }

        // 2. Adopt the v1 token minted by /auth/callback so the first v1
        //    call skips `POST /api/mobile/api-token`. invalidate() drops
        //    any stale in-memory token; adopt() writes the new one.
        apiTokenCache.get().invalidate()
        apiTokenCache.get().adopt(token = v1Token, label = "android-client/web-oauth")

        // 3. Flip the auth state so the NavGraph auth gate re-renders onto
        //    the dashboard. We do this on the main thread because
        //    collectAsStateWithLifecycle drives composition.
        _authState.value = TastileAuthState.Authenticated(
            userId = userId,
            email = email,
        )
        return true
    }

    private fun isAuthCallbackUri(uri: Uri): Boolean {
        if (uri.scheme != "tastile") return false
        if (uri.host != "auth") return false
        if (uri.path != "/callback") return false
        return true
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

        /**
         * Server-side hand-off path. The web login page routes BetterAuth's
         * `callbackURL` through `/api/auth/bridge?next=<this>`, which then
         * 302s to `tastile://auth/callback?session=...&v1_token=...`. The
         * native app's intent filter for the `tastile` scheme picks that
         * final URI up. Must be a `/`-prefixed path so the bridge route's
         * `safeNextPath` validator accepts it.
         */
        const val NATIVE_CALLBACK_PATH = "/auth/callback"

        const val TAG = "AuthRepository"
    }
}
