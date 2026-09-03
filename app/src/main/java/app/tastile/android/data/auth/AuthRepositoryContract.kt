package app.tastile.android.data.auth

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/**
 * Data-layer entry point for native BetterAuth sign-in / sign-out. The
 * concrete implementation ([AuthRepository]) drives `TastileAuthState` and
 * is the only owner of the [EncryptedTokenStorage] session token + the
 * `ApiTokenCache` mint chain.
 *
 * Replaces the previous Cognito-PKCE contract. Native Google Sign-In is
 * wired here via [signInWithGoogle]; Apple sign-in still defers to the
 * web login page until the native OAuth bridge is implemented in a
 * follow-up — see [signInWithProvider].
 */
interface AuthRepositoryContract {
    val authState: StateFlow<TastileAuthState>

    /** Native email + password sign-in. Throws on auth failure. */
    suspend fun signInWithEmail(email: String, password: String)

    /** Native email + password sign-up. Throws on validation / network failure. */
    suspend fun signUpWithEmail(email: String, password: String, name: String)

    /**
     * Native Google Sign-In via Credential Manager. Exchanges the Google
     * idToken against BetterAuth's /api/auth/sign-in/social idToken branch,
     * then persists the resulting session identically to email sign-in.
     *
     * Throws [GoogleSignInUnavailableException] when no Google account is
     * available — callers should fall back to
     * [signInWithProvider]("google") which opens the web OAuth handoff.
     */
    suspend fun signInWithGoogle()

    /**
     * Opens the web login page in the system browser for OAuth flows whose
     * native callback is not yet implemented. Kept here so the existing
     * "Continue with Google" affordance still works on MVP while the native
     * social bridge lands in a follow-up PR.
     */
    suspend fun signInWithProvider(provider: String)

    /**
     * Completes a web OAuth handoff by parsing the [Uri] the system delivers
     * to [app.tastile.android.MainActivity] when the `tastile://auth/callback`
     * intent filter fires (cold start) or `onNewIntent` delivers (warm
     * return).
     *
     * The URI carries the same bundle the web bridge route forwards after
     * BetterAuth completes the social sign-in: `session` (BetterAuth bearer),
     * `user_id`, optional `email` / `expires_at`, and `v1_token` (the long-
     * lived API token, already minted server-side via the bridge secret).
     *
     * On success the call:
     *   1. persists the session + identity to encrypted SharedPreferences,
     *   2. adopts the v1 token into [ApiTokenCache] so the next v1 call
     *      skips the `POST /api/mobile/api-token` mint round-trip,
     *   3. flips [authState] to [TastileAuthState.Authenticated], which
     *      triggers the auth gate in [app.tastile.android.ui.mobile.MobileNavGraph]
     *      to render the dashboard.
     *
     * Returns `true` only when the URI carries a usable session + v1 token
     * pair. Malformed URIs (missing fields, wrong host) return `false` and
     * leave existing state untouched — callers should treat a `false`
     * return as a no-op and let the user retry from the login screen.
     */
    fun completeAuthFromCallback(uri: Uri): Boolean

    /** Server-side revoke + on-device token wipe. Idempotent. */
    suspend fun signOut()

    /** Bootstrap credential for [EncryptedTokenStorage.apiTokenPrefs]. */
    fun currentSessionToken(): String?
}
