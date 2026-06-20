package app.tastile.android.data.repository

interface CurrentUserProvider {
    fun currentUserId(): String?
    fun currentIdToken(): String? = null
}
