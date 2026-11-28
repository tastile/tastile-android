package app.tastile.android.data.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import app.tastile.android.data.api.TokenRefresher
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1ApiTokenCreateRequest
import app.tastile.android.data.api.V1ApiTokenCreateResponse
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
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
 *
 * Implements [TokenRefresher] to handle Core 401 responses: discards the
 * rejected token, re-mints via `POST /api/mobile/api-token` under the same
 * single-flight mutex — one bounded round of [MAX_REMINT_ATTEMPTS] attempts
 * with 500ms/1000ms backoff — and returns the replacement. When the round
 * is exhausted the cache returns `null` and forces the explicit
 * unauthenticated state through
 * [AuthRepositoryContract.markUnauthenticated]; while unauthenticated,
 * further refreshes fail fast until a mint or token adoption succeeds
 * (android#92).
 */
@Singleton
class ApiTokenCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val v1ApiClient: Lazy<V1ApiClient>,
    private val authRepository: AuthRepositoryContract,
) : TokenRefresher {
    private val mutex = Mutex()

    @Volatile
    private var cachedToken: String? = loadCachedToken()

    /**
     * Set when a 401-triggered re-mint round exhausted its bounded retries
     * and the auth state was forced to unauthenticated. While set,
     * [refreshAfterUnauthorized] fails fast without minting, so concurrent
     * or later 401s cannot start new retry rounds. Cleared when a mint or
     * token adoption succeeds ([getOrMint], [adopt], [invalidate],
     * [signOut]).
     */
    @Volatile
    private var refreshGaveUp: Boolean = false

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
            mintUnderLock(onMintFailed)
        }
    }

    /**
     * Single mint attempt; the caller must hold [mutex]. Returns the cached
     * token when one is present (a parallel refresher may have won the
     * race), or `null` on any failure — missing BetterAuth session, network
     * error, or 4xx/5xx from `POST /api/mobile/api-token` — without
     * throwing. [onMintFailed] receives the underlying throwable and is
     * used by the first-use toast path only; the 401 refresh path passes
     * `null` and surfaces [V1Error.Auth] upstream instead.
     */
    private suspend fun mintUnderLock(onMintFailed: ((Throwable) -> Unit)? = null): String? {
        cachedToken?.takeIf { it.isNotBlank() }?.let { return it }
        val sessionToken = authRepository.currentSessionToken() ?: return null
        val response = try {
            v1ApiClient.get().mintApiTokenViaWeb(
                sessionToken = sessionToken,
                request = V1ApiTokenCreateRequest(label = "android-client"),
            )
        } catch (e: Exception) {
            Log.w(TAG, "mintApiToken failed: ${e.message}")
            onMintFailed?.invoke(e)
            return null
        }
        persistToken(response)
        cachedToken = response.token
        refreshGaveUp = false
        return response.token
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
        refreshGaveUp = false
    }

    /**
     * Installs a v1 API token minted outside the normal mint path. Used by
     * the web OAuth handoff — the `/auth/callback` route hands the freshly
     * minted token back to the app via a `tastile://auth/callback?v1_token=...`
     * deep link, and [AuthRepository.completeAuthFromCallback] adopts it
     * here so the first v1 call after sign-in skips the mint round-trip.
     *
     * The token is persisted under the same encrypted prefs key as a normal
     * mint ([KEY_API_TOKEN]); [tokenId], [label], and [mintedAtEpochMillis]
     * are optional metadata (the web route does not currently expose the
     * token id, so callers pass `null`). Passing a blank token is a no-op so
     * a route that forgets to mint one does not poison the cache.
     */
    fun adopt(
        token: String,
        tokenId: String? = null,
        label: String? = null,
        mintedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        if (token.isBlank()) return
        EncryptedTokenStorage.apiTokenPrefs(context).edit {
            putString(KEY_API_TOKEN, token)
            if (tokenId != null) putString(KEY_TOKEN_ID, tokenId)
            if (label != null) putString(KEY_LABEL, label)
            putString(KEY_MINTED_AT, mintedAtEpochMillis.toString())
        }
        cachedToken = token
        refreshGaveUp = false
    }

    /** Clears both the in-memory cache and the encrypted prefs entry. */
    fun signOut() {
        cachedToken = null
        refreshGaveUp = false
        EncryptedTokenStorage.apiTokenPrefs(context).edit { clear() }
    }

    /**
     * Discards the [staleToken] Core just rejected with 401 and re-mints a
     * replacement in one bounded round, entirely under [mutex] so parallel
     * 401s wait for this outcome instead of stampeding the mint endpoint
     * (single-flight covers success and failure alike).
     *
     * Round contract (android#92 AC2):
     *  - up to [MAX_REMINT_ATTEMPTS] attempts, backing off
     *    [REMINT_BACKOFF_BASE_MS] then `2 × base` ms between attempts;
     *  - success returns the replacement token;
     *  - exhaustion returns `null` after forcing the explicit
     *    unauthenticated state via
     *    [AuthRepositoryContract.markUnauthenticated];
     *  - while unauthenticated, further refreshes fail fast (`null`, no
     *    mint attempts) until [getOrMint], [adopt], [invalidate], or
     *    [signOut] restores a usable token.
     */
    override suspend fun refreshAfterUnauthorized(staleToken: String): String? {
        mutex.withLock {
            // Discard the rejected token from memory and persistent storage
            // so a subsequent cold start doesn't resurrect it.
            if (cachedToken == staleToken) {
                cachedToken = null
            }
            val prefs = EncryptedTokenStorage.apiTokenPrefs(context)
            if (prefs.getString(KEY_API_TOKEN, null) == staleToken) {
                prefs.edit { clear() }
            }
            if (refreshGaveUp) {
                // Already unauthenticated: fail fast, no new mint round.
                return null
            }
            var attempt = 1
            while (true) {
                mintUnderLock()?.let { return it }
                if (attempt >= MAX_REMINT_ATTEMPTS) break
                delay(REMINT_BACKOFF_BASE_MS * attempt)
                attempt += 1
            }
            refreshGaveUp = true
        }
        authRepository.markUnauthenticated()
        return null
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

        /** Upper bound of one 401-triggered re-mint round (AC2: no infinite retry). */
        private const val MAX_REMINT_ATTEMPTS = 3

        /** Backoff before attempt n+1 is n × base → 500ms, then 1000ms (AC2). */
        private const val REMINT_BACKOFF_BASE_MS = 500L
    }
}
