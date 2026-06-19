package app.tastile.android.data.repository

import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

interface AuthRepositoryContract {
    val sessionStatus: StateFlow<SessionStatus>
    suspend fun signInWithGoogle()
    suspend fun signOut()
}
