package app.tastile.android.data.repository

sealed interface TastileAuthState {
    data object Loading : TastileAuthState
    data object Unauthenticated : TastileAuthState

    data class Authenticated(
        val userId: String,
        val email: String?,
        val idToken: String,
        val accessToken: String,
        val refreshToken: String?
    ) : TastileAuthState
}
