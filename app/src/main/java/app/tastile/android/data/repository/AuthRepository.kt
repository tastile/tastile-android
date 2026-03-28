package app.tastile.android.data.repository

import android.content.Intent
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) : CurrentUserProvider, AuthRepositoryContract {
    private val oauthRedirectUrl = "tastile://auth/callback"

    val currentSession get() = client.auth.currentSessionOrNull()
    override val sessionStatus: StateFlow<SessionStatus> get() = client.auth.sessionStatus

    override fun currentUserId(): String? = currentSession?.user?.id

    override suspend fun signInWithGoogle() {
        client.auth.signInWith(Google, redirectUrl = oauthRedirectUrl)
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun handleDeepLink(intent: Intent): Boolean {
        val data = intent.data ?: return false
        val deeplink = data.toString()

        val isSupportedCallback =
            deeplink.startsWith(oauthRedirectUrl) ||
            deeplink.startsWith("https://tastile.app/auth/callback")

        if (!isSupportedCallback) {
            return false
        }

        val fragmentParams = parseParams(data.fragment)
        val queryParams = parseParams(data.query)

        val accessToken = fragmentParams["access_token"] ?: queryParams["access_token"]
        val refreshToken = fragmentParams["refresh_token"] ?: queryParams["refresh_token"]

        if (!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
            client.auth.importAuthToken(accessToken, refreshToken, true, true)
            return true
        }

        return runCatching {
            client.auth.exchangeCodeForSession(deeplink, true)
            true
        }.getOrElse {
            false
        }
    }

    private fun parseParams(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split("&")
            .mapNotNull { part ->
                val idx = part.indexOf("=")
                if (idx <= 0) return@mapNotNull null
                val key = Uri.decode(part.substring(0, idx))
                val value = Uri.decode(part.substring(idx + 1))
                key to value
            }
            .toMap()
    }
}
