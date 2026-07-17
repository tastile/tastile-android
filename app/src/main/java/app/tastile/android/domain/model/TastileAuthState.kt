package app.tastile.android.domain.model

/**
 * Pure-Kotlin mirror of the auth state surfaced by the data layer
 * ([app.tastile.android.data.repository.TastileAuthState]). Kept as a
 * sealed interface so domain callers can pattern-match without
 * importing data-layer types.
 */
sealed interface TastileAuthState {
    /** Auth state has not been resolved yet (initial load / refresh). */
    data object Loading : TastileAuthState

    /** No valid Cognito session is available. */
    data object Unauthenticated : TastileAuthState

    /** Valid Cognito session with id / access / refresh tokens. */
    data class Authenticated(
        /** Cognito `sub` claim; UUIDv5-derived owner_id elsewhere. */
        val userId: String,
        /** Email recorded at sign-in, or null when not granted by the IdP. */
        val email: String?,
        /** Cognito id_token (JWT). */
        val idToken: String,
        /** Cognito access_token. */
        val accessToken: String,
        /** Cognito refresh_token (null when the IdP did not return one). */
        val refreshToken: String?,
    ) : TastileAuthState
}
