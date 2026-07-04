package app.tastile.android.data.repository

import android.content.Context
import android.util.Log
import app.tastile.android.BuildConfig
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
 * per user session by calling `POST /v1/api-tokens` and is then cached in
 * [EncryptedTokenStorage]'s Keystore-backed preferences. All v1 API calls
 * (`V1ApiClient` → `tokenProvider`) read the cached token from this manager.
 *
 * The v1 daemon accepts two mint bootstrap paths:
 *   1. The "web bridge" — `x-tastile-web-bridge-secret` +
 *      `x-tastile-web-session-user: <cognito_sub>` (what `tastile-web`
 *      uses via its Next.js API route).
 *   2. An existing Tastile API token in `Authorization: Bearer …` —
 *      chicken-and-egg, not useful for first-time mint.
 *
 * We prefer the bridge path because it does not require the daemon to
 * recognize the Cognito `id_token` as a v1 bearer (it currently doesn't
 * — `common.rs::authenticate` only hashes the bearer into the
 * `v1_api_token` table). The bridge secret is supplied at build time via
 * the `TASTILE_WEB_BRIDGE_SECRET` Gradle property (see
 * `app/build.gradle.kts`). When the property is empty we fall back to
 * the Cognito `id_token` bearer so unit tests and the CI dev surface
 * still exercise the call site.
 *
 * Concurrency: a single [Mutex] guards the mint path so multiple parallel
 * first-use callers issue exactly one mint request.
 */
@Singleton
class ApiTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val v1ApiClient: Lazy<V1ApiClient>,
    private val currentUser: CurrentUserProvider,
) {
    private val mutex = Mutex()

    @Volatile
    private var cachedToken: String? = loadCachedToken()

    /** Returns the cached Tastile API token, minting one on first use if possible. */
    suspend fun getOrMint(): String? {
        cachedToken?.takeIf { it.isNotBlank() }?.let { return it }
        return mutex.withLock {
            cachedToken?.takeIf { it.isNotBlank() }?.let { return@withLock it }
            val bridgeSecret = BuildConfig.TASTILE_WEB_BRIDGE_SECRET.trim()
            val response = try {
                if (bridgeSecret.isNotEmpty()) {
                    val userSub = currentUser.currentUserId()?.takeIf { it.isNotBlank() }
                        ?: return@withLock null
                    v1ApiClient.get().mintApiTokenViaBridge(
                        bridgeSecret = bridgeSecret,
                        userSub = userSub,
                        request = V1ApiTokenCreateRequest(label = "android-client"),
                    )
                } else {
                    val bootstrap = currentUser.currentIdToken() ?: return@withLock null
                    v1ApiClient.get().mintApiToken(
                        bootstrapToken = bootstrap,
                        request = V1ApiTokenCreateRequest(label = "android-client"),
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "mintApiToken failed: ${e.message}")
                null
            } ?: return@withLock null
            persistToken(response)
            cachedToken = response.token
            response.token
        }
    }

    /** Drops the in-memory token. Does NOT clear the encrypted prefs; call [signOut] for that. */
    fun invalidate() {
        cachedToken = null
    }

    /** Clears both the in-memory cache and the encrypted prefs entry. */
    fun signOut() {
        cachedToken = null
        EncryptedTokenStorage.apiTokenPrefs(context).edit().clear().apply()
    }

    private fun persistToken(response: V1ApiTokenCreateResponse) {
        EncryptedTokenStorage.apiTokenPrefs(context).edit()
            .putString(KEY_API_TOKEN, response.token)
            .putString(KEY_TOKEN_ID, response.tokenId)
            .putString(KEY_LABEL, response.label)
            .putString(KEY_MINTED_AT, System.currentTimeMillis().toString())
            .apply()
    }

    private fun loadCachedToken(): String? =
        EncryptedTokenStorage.apiTokenPrefs(context).getString(KEY_API_TOKEN, null)
            ?.takeIf { it.isNotBlank() }

    private companion object {
        private const val TAG = "ApiTokenManager"
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_TOKEN_ID = "token_id"
        private const val KEY_LABEL = "label"
        private const val KEY_MINTED_AT = "minted_at"
    }
}
