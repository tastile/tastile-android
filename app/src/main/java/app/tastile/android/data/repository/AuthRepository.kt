package app.tastile.android.data.repository

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.compose.auth.composeAuth

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) {
    val currentSession get() = client.auth.currentSessionOrNull()
    val sessionStatus: StateFlow<SessionStatus> get() = client.auth.sessionStatus

    suspend fun signInWithGoogle() {
        client.auth.signInWith(Google) {
            // Uses Chrome Custom Tabs
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun handleDeepLink(intent: Intent) {
        // TODO: Implement deep link handling for OAuth  
        // Note: handleDeeplinks API changed in Supabase 3.x
        // Use auth.exchangeCodeForSession() with the code from intent data
    }
}
