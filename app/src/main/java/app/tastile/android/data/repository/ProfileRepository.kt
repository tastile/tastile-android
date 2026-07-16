package app.tastile.android.data.repository

import app.tastile.android.data.model.Profile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultProfileRepository @Inject constructor() {
    suspend fun getProfile(userId: String): Profile? {
        return Profile(id = userId)
    }

    suspend fun updateDisplayName(userId: String, displayName: String): Profile? {
        return Profile(id = userId, displayName = displayName)
    }
}
