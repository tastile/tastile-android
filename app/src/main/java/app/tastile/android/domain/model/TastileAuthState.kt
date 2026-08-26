package app.tastile.android.domain.model

/**
 * Pure-Kotlin mirror of the auth state surfaced by the data layer
 * ([app.tastile.android.data.auth.TastileAuthState]). Kept as a
 * sealed interface so domain callers can pattern-match without
 * importing data-layer types.
 */
sealed interface TastileAuthState {
    /** Auth state has not been resolved yet (initial load / refresh). */
    data object Loading : TastileAuthState

    /** No valid BetterAuth session is available. */
    data object Unauthenticated : TastileAuthState

    /**
     * Valid BetterAuth session. The data layer also holds the raw session
     * token for refresh purposes; callers that need that token should
     * consult [app.tastile.android.data.auth.CurrentUserProvider] rather
     * than the sealed type.
     */
    data class Authenticated(
        /** BetterAuth user id (mirrors `session.user.id`). */
        val userId: String,
        /** Email recorded at sign-in, or null when not granted by the IdP. */
        val email: String?,
    ) : TastileAuthState
}
