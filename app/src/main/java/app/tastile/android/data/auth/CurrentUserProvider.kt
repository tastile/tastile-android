package app.tastile.android.data.auth

interface CurrentUserProvider {
    fun currentUserId(): String?
    fun currentIdToken(): String? = null
    fun currentAccessToken(): String? = null
}
