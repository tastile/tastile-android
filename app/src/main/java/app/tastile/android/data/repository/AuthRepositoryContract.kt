package app.tastile.android.data.repository

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface AuthRepositoryContract {
    val authState: StateFlow<TastileAuthState>
    suspend fun signInWithCognito(context: Context)
    suspend fun signInWithGoogle(context: Context)
    suspend fun signOut()
    fun currentIdToken(): String?
}
