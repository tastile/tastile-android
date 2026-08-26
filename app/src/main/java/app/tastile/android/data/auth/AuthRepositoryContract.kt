package app.tastile.android.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Data-layer entry point for native BetterAuth sign-in / sign-out. The
 * concrete implementation ([AuthRepository]) drives `TastileAuthState` and
 * is the only owner of the [EncryptedTokenStorage] session token + the
 * `ApiTokenCache` mint chain.
 *
 * Replaces the previous Cognito-PKCE contract. Social providers (Google /
 * Apple) still defer to the web login page until the native OAuth bridge
 * is implemented in a follow-up — see `LoginScreen.signInWithProvider`.
 */
interface AuthRepositoryContract {
    val authState: StateFlow<TastileAuthState>

    /** Native email + password sign-in. Throws on auth failure. */
    suspend fun signInWithEmail(email: String, password: String)

    /** Native email + password sign-up. Throws on validation / network failure. */
    suspend fun signUpWithEmail(email: String, password: String, name: String)

    /**
     * Opens the web login page in the system browser for OAuth flows whose
     * native callback is not yet implemented. Kept here so the existing
     * "Continue with Google" affordance still works on MVP while the native
     * social bridge lands in a follow-up PR.
     */
    suspend fun signInWithProvider(provider: String)

    /** Server-side revoke + on-device token wipe. Idempotent. */
    suspend fun signOut()

    /** Bootstrap credential for [EncryptedTokenStorage.apiTokenPrefs]. */
    fun currentSessionToken(): String?
}
