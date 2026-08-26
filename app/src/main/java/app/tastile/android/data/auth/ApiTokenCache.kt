package app.tastile.android.data.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1ApiTokenCreateRequest
import app.tastile.android.data.api.V1ApiTokenCreateResponse
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the *Tastile* API token (the second authentication concern per
 * `docs/agent-handoff/PROJECT-TRUTH.md`). The token is minted lazily once
 * per user session through `POST /api/mobile/api-token` and is then cached in
 * [EncryptedTokenStorage]'s Keystore-backed preferences. All v1 API calls
 * (`V1ApiClient` → `tokenProvider`) read the cached token from this cache.
 *
 * Bootstrap uses the public-client web endpoint with the user's BetterAuth
 * session token. The web server verifies that token against the BetterAuth
 * session and keeps the Core bridge secret server-side; no shared credential
 * is embedded in the APK.
 *
 * Concurrency: a single [Mutex] guards the mint path so multiple parallel
 * first-use callers issue exactly one mint request.
 */
@Singleton
class ApiTokenCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val v1ApiClient: Lazy<V1ApiClient>,
    private val currentUser: CurrentUserProvider,
) {
    private val mutex = Mutex()

    @Volatile
    private var cachedToken: String? = loadCachedToken()

    /**
     * Returns the cached Tastile API token, minting one on first use if
     * possible. When a mint attempt fails, the optional [onMintFailed]
     * callback receives the underlying throwable so callers can surface a
     * one-line toast instead of a silent fallback. The exception is still
     * swallowed and `null` is returned — the existing contract that "no
     * token" is signaled by `null` is preserved.
     */
    suspend fun getOrMint(
        onMintFailed: ((Throwable) -> Unit)? = null,
    ): String? {
        cachedToken?.takeIf { it.isNotBlank() }?.let { return it }
        return mutex.withLock {
            cachedToken?.takeIf { it.isNotBlank() }?.let { return@withLock it }
            val sessionToken = currentUser.currentSessionToken() ?: return@withLock null
            val response = try {
                v1ApiClient.get().mintApiTokenViaWeb(
                    sessionToken = sessionToken,
                    request = V1ApiTokenCreateRequest(label = "android-client"),
                )
            } catch (e: Exception) {
                Log.w(TAG, "mintApiToken failed: ${e.message}")
                onMintFailed?.invoke(e)
                null
            } ?: return@withLock null
            persistToken(response)
            cachedToken = response.token
            response.token
        }
    }

    /**
     * Best-effort synchronous read of the cached token. Returns `null` if
     * no token has been minted yet. Used by the auth-state observer in
     * [app.tastile.android.MainActivity] to hand the bearer to
     * [app.tastile.android.sync.SyncCoordinator] without triggering a
     * network round-trip.
     */
    fun currentCachedToken(): String? = cachedToken?.takeIf { it.isNotBlank() }

    /** Drops the in-memory token. Does NOT clear the encrypted prefs; call [signOut] for that. */
    fun invalidate() {
        cachedToken = null
    }

    /** Clears both the in-memory cache and the encrypted prefs entry. */
    fun signOut() {
        cachedToken = null
        EncryptedTokenStorage.apiTokenPrefs(context).edit { clear() }
    }

    private fun persistToken(response: V1ApiTokenCreateResponse) {
        EncryptedTokenStorage.apiTokenPrefs(context).edit {
            putString(KEY_API_TOKEN, response.token)
            putString(KEY_TOKEN_ID, response.tokenId)
            putString(KEY_LABEL, response.label)
            putString(KEY_MINTED_AT, System.currentTimeMillis().toString())
        }
    }

    private fun loadCachedToken(): String? =
        EncryptedTokenStorage.apiTokenPrefs(context).getString(KEY_API_TOKEN, null)
            ?.takeIf { it.isNotBlank() }

    private companion object {
        private const val TAG = "ApiTokenCache"
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_TOKEN_ID = "token_id"
        private const val KEY_LABEL = "label"
        private const val KEY_MINTED_AT = "minted_at"
    }
}
