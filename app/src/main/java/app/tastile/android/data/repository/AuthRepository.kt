package app.tastile.android.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import app.tastile.android.BuildConfig
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
 
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) : CurrentUserProvider, AuthRepositoryContract {
    private companion object {
        private const val TAG = "AuthRepository"
    }

    private val oauthRedirectUrl = "tastile://auth/callback"
    private val json = Json { ignoreUnknownKeys = true }

    val currentSession get() = client.auth.currentSessionOrNull()
    override val sessionStatus: StateFlow<SessionStatus> get() = client.auth.sessionStatus

    override fun currentUserId(): String? = currentSession?.user?.id

    private suspend fun signInWithGoogleWithCredentialManager(
        context: Context,
        credentialManager: CredentialManager
    ) {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            throw IllegalStateException("GOOGLE_WEB_CLIENT_ID is missing")
        }
        val idToken = try {
            tryGetGoogleIdToken(
                context = context,
                credentialManager = credentialManager,
                webClientId = webClientId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Credential Manager flow failed", e)
            throw IllegalStateException(
                "Credential Manager failed: ${e.message ?: e.javaClass.simpleName}",
                e
            )
        }

        val session = try {
            withContext(Dispatchers.IO) { exchangeGoogleIdTokenForSession(idToken) }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase token exchange failed", e)
            throw IllegalStateException(
                "Supabase token exchange failed: ${e.message ?: e.javaClass.simpleName}",
                e
            )
        }

        try {
            client.auth.importAuthToken(session.accessToken, session.refreshToken, true, true)
        } catch (e: Exception) {
            Log.e(TAG, "Session import failed", e)
            throw IllegalStateException(
                "Session import failed: ${e.message ?: e.javaClass.simpleName}",
                e
            )
        }
    }

    override suspend fun signInWithGoogle(context: Context) {
        val credentialManager = CredentialManager.create(context)
        try {
            signInWithGoogleWithCredentialManager(context, credentialManager)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithGoogle failed", e)
            throw e
        }
    }

    private suspend fun tryGetGoogleIdToken(
        context: Context,
        credentialManager: CredentialManager,
        webClientId: String
    ): String {
        val authorizedOnlyOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        val authorizedOnlyRequest = GetCredentialRequest.Builder()
            .addCredentialOption(authorizedOnlyOption)
            .build()

        val response = try {
            credentialManager.getCredential(context = context, request = authorizedOnlyRequest)
        } catch (e: NoCredentialException) {
            val broaderOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val broaderRequest = GetCredentialRequest.Builder()
                .addCredentialOption(broaderOption)
                .build()
            try {
                credentialManager.getCredential(context = context, request = broaderRequest)
            } catch (nested: NoCredentialException) {
                val explicitSignInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                val explicitSignInRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(explicitSignInOption)
                    .build()
                credentialManager.getCredential(context = context, request = explicitSignInRequest)
            }
        }

        val idTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
        val idToken = idTokenCredential.idToken
        if (idToken.isBlank()) {
            throw IllegalStateException("Google ID token is empty")
        }
        return idToken
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

        client.auth.exchangeCodeForSession(deeplink, true)
        return true
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

    private fun exchangeGoogleIdTokenForSession(idToken: String): TokenSession {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            throw IllegalStateException("Supabase config is missing")
        }

        val endpoint = URL("$supabaseUrl/auth/v1/token?grant_type=id_token")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer $anonKey")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }

        val payload = buildJsonObject {
            put("provider", JsonPrimitive("google"))
            put("id_token", JsonPrimitive(idToken))
        }.toString()

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload) }

        val status = connection.responseCode
        val responseText = if (status in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }

        if (status !in 200..299) {
            throw IllegalStateException("Supabase ID token exchange failed: HTTP $status $responseText")
        }

        val response = json.parseToJsonElement(responseText).jsonObject
        val accessToken = response["access_token"]?.jsonPrimitive?.contentOrNull
        val refreshToken = response["refresh_token"]?.jsonPrimitive?.contentOrNull
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            throw IllegalStateException("Supabase response missing access/refresh token")
        }
        return TokenSession(accessToken, refreshToken)
    }

    private data class TokenSession(
        val accessToken: String,
        val refreshToken: String
    )
}
