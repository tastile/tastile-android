package app.tastile.android.data.auth

sealed interface TastileAuthState {
    data object Loading : TastileAuthState
    data object Unauthenticated : TastileAuthState

    /**
     * The user has completed native BetterAuth sign-in. The Tastile API
     * token is lazily minted on first v1 call from the session token stored
     * in [EncryptedTokenStorage.sessionTokenPrefs]; this state only tracks
     * the BetterAuth session identity so the UI can render the signed-in
     * shell immediately.
     */
    data class Authenticated(
        val userId: String,
        val email: String?,
    ) : TastileAuthState
}
