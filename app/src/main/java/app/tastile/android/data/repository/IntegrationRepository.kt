package app.tastile.android.data.repository

import app.tastile.android.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GoogleCalendarIntegrationSettings(
    val connected: Boolean = false,
    @SerialName("can_read") val canRead: Boolean = true,
    @SerialName("can_write") val canWrite: Boolean = true,
    @SerialName("account_email") val accountEmail: String? = null,
    @SerialName("last_synced_at") val lastSyncedAt: String? = null,
)

@Serializable
data class IntegrationSettingsResponse(
    @SerialName("google_calendar") val googleCalendar: GoogleCalendarIntegrationSettings
)

@Serializable
data class RecoveryResetResponse(
    val ok: Boolean,
    val message: String,
    val applied: Int
)

@Serializable
data class SyncStatusResponse(
    val active: Boolean = false,
    val running: Boolean = false,
    @SerialName("last_success_at") val lastSuccessAt: String? = null,
    @SerialName("last_error") val lastError: String? = null
)

@Serializable
data class RuntimePathsResponse(
    @SerialName("profile_name") val profileName: String,
    @SerialName("app_data_dir") val appDataDir: String,
    @SerialName("db_path") val dbPath: String,
    @SerialName("session_path") val sessionPath: String,
    @SerialName("daemon_startup_log_path") val daemonStartupLogPath: String,
    @SerialName("daemon_executable_path") val daemonExecutablePath: String
)

@Serializable
data class TileQuotaResponse(
    val plan: String,
    @SerialName("tile_count") val tileCount: Int,
    @SerialName("max_tiles") val maxTiles: Int,
    @SerialName("remaining_tiles") val remainingTiles: Int,
    @SerialName("limit_reached") val limitReached: Boolean,
    val source: String? = null
)

@Serializable
data class AuthSessionResponse(
    @SerialName("user_id") val userId: String,
    val email: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class OAuthInitResponse(
    @SerialName("flow_id") val flowId: String,
    @SerialName("auth_url") val authUrl: String,
    val provider: String
)

@Serializable
data class OAuthExchangeResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val email: String? = null
)

@Singleton
class IntegrationRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val daemonBaseUrls = defaultDaemonBaseUrls()
    @Volatile
    private var lastSuccessfulBaseUrl: String? = null

    suspend fun getSettings(): IntegrationSettingsResponse {
        val (status, responseText) = executeDaemonRequest(path = "/auth/integrations/settings", method = "GET")
        if (status !in 200..299) {
            throw IllegalStateException("Failed to load integrations: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun updateGoogleCalendarConnected(connected: Boolean): IntegrationSettingsResponse {
        return updateGoogleCalendarIntegration(
            connected = connected,
            accountEmail = if (connected) null else ""
        )
    }

    suspend fun markGoogleCalendarSyncedNow(): IntegrationSettingsResponse {
        return updateGoogleCalendarIntegration(lastSyncedAt = java.time.Instant.now().toString())
    }

    suspend fun updateGoogleCalendarIntegration(
        connected: Boolean? = null,
        canRead: Boolean? = null,
        canWrite: Boolean? = null,
        accountEmail: String? = null,
        lastSyncedAt: String? = null
    ): IntegrationSettingsResponse {
        val payload = buildJsonObject {
            put(
                "google_calendar",
                buildJsonObject {
                    if (connected != null) put("connected", JsonPrimitive(connected))
                    if (canRead != null) put("can_read", JsonPrimitive(canRead))
                    if (canWrite != null) put("can_write", JsonPrimitive(canWrite))
                    if (accountEmail != null) {
                        if (accountEmail.isBlank()) {
                            put("account_email", JsonNull)
                        } else {
                            put("account_email", JsonPrimitive(accountEmail))
                        }
                    }
                    if (lastSyncedAt != null) put("last_synced_at", JsonPrimitive(lastSyncedAt))
                }
            )
        }.toString()
        return postSettingsPayload(payload)
    }

    suspend fun triggerSync() {
        val (status, responseText) = executeDaemonRequest(
            path = "/sync/trigger",
            method = "POST",
            withContentType = false
        )
        if (status !in 200..299 && status != 202) {
            throw IllegalStateException("Failed to trigger sync: HTTP $status $responseText")
        }
    }

    suspend fun triggerTick() {
        val (status, responseText) = executeDaemonRequest(
            path = "/commands/tick",
            method = "POST",
            withContentType = false
        )
        if (status !in 200..299 && status != 202) {
            throw IllegalStateException("Failed to trigger tick: HTTP $status $responseText")
        }
    }

    suspend fun resetLocalSyncData(): RecoveryResetResponse {
        val (status, responseText) = executeDaemonRequest(
            path = "/sync/recovery/reset-local",
            method = "POST",
            withContentType = false
        )
        if (status !in 200..299) {
            throw IllegalStateException("Failed to reset local sync data: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun redownloadRemoteSyncData(): RecoveryResetResponse {
        val (status, responseText) = executeDaemonRequest(
            path = "/sync/recovery/redownload-remote",
            method = "POST",
            withContentType = false
        )
        if (status !in 200..299) {
            throw IllegalStateException("Failed to redownload remote sync data: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun getSyncStatus(): SyncStatusResponse {
        val (status, responseText) = executeDaemonRequest(path = "/sync/status", method = "GET")
        if (status !in 200..299) {
            throw IllegalStateException("Failed to load sync status: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun getRuntimePaths(): RuntimePathsResponse {
        val (status, responseText) = executeDaemonRequest(
            path = "/read/runtime-paths",
            method = "GET",
            requiresAuth = false
        )
        if (status !in 200..299) {
            throw IllegalStateException("Failed to load runtime paths: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun getTileQuota(): TileQuotaResponse {
        val (status, responseText) = executeDaemonRequest(path = "/auth/tile-quota", method = "GET")
        if (status !in 200..299) {
            throw IllegalStateException("Failed to load tile quota: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun getSession(): AuthSessionResponse? {
        val (status, responseText) = executeDaemonRequest(
            path = "/auth/session",
            method = "GET",
            requiresAuth = false
        )
        if (status !in 200..299) {
            return null
        }
        return runCatching { json.decodeFromString<AuthSessionResponse>(responseText) }.getOrNull()
    }

    suspend fun restoreSession(session: AuthSessionResponse): AuthSessionResponse {
        val payload = buildJsonObject {
            put("user_id", JsonPrimitive(session.userId))
            if (!session.email.isNullOrBlank()) put("email", JsonPrimitive(session.email))
            if (!session.accessToken.isNullOrBlank()) put("access_token", JsonPrimitive(session.accessToken))
            if (!session.refreshToken.isNullOrBlank()) put("refresh_token", JsonPrimitive(session.refreshToken))
            if (!session.expiresAt.isNullOrBlank()) put("expires_at", JsonPrimitive(session.expiresAt))
        }.toString()
        val (status, responseText) = executeDaemonRequest(
            path = "/auth/session/restore",
            method = "POST",
            payload = payload,
            requiresAuth = false
        )
        if (status !in 200..299) {
            throw IllegalStateException("Failed to restore session: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun startOAuth(provider: String = "google"): OAuthInitResponse {
        val payload = buildJsonObject { put("provider", JsonPrimitive(provider)) }.toString()
        val (status, responseText) = executeDaemonRequest(
            path = "/auth/oauth/start",
            method = "POST",
            payload = payload,
            requiresAuth = false
        )
        if (status !in 200..299) {
            throw IllegalStateException("Failed to start oauth: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun signInWithOAuth(
        provider: String = "google",
        code: String,
        redirectUri: String? = null,
        state: String? = null
    ): OAuthExchangeResponse {
        val payload = buildJsonObject {
            put("provider", JsonPrimitive(provider))
            put("code", JsonPrimitive(code))
            if (!redirectUri.isNullOrBlank()) put("redirect_uri", JsonPrimitive(redirectUri))
            if (!state.isNullOrBlank()) put("state", JsonPrimitive(state))
        }.toString()
        val (status, responseText) = executeDaemonRequest(
            path = "/auth/oauth/exchange",
            method = "POST",
            payload = payload,
            requiresAuth = false
        )
        if (status !in 200..299) {
            throw IllegalStateException("Failed to exchange oauth code: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun startBrowserAuth(provider: String = "google"): String? {
        return startOAuth(provider).authUrl
    }

    suspend fun isAuthenticated(): Boolean {
        val session = getSession() ?: return false
        if (!session.accessToken.isNullOrBlank()) return true
        val currentAccessToken = client.auth.currentSessionOrNull()?.accessToken
        return !currentAccessToken.isNullOrBlank()
    }

    suspend fun checkHealth(): Boolean {
        val (status, responseText) = executeDaemonRequest(path = "/health", method = "GET", requiresAuth = false)      
        if (status in 200..299) {
            return true
        }
        return responseText.trim().equals("ok", ignoreCase = true)
    }

    suspend fun getEventsRaw(): JsonElement? {
        val (status, responseText) = executeDaemonRequest(path = "/debug/events", method = "GET")
        if (status !in 200..299) {
            return null
        }
        return runCatching { json.parseToJsonElement(responseText) }.getOrNull()
    }

    fun streamStateEvents(): Flow<String> = callbackFlow {
        val accessToken = client.auth.currentSessionOrNull()?.accessToken
            ?: throw IllegalStateException("Not authenticated")
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        var streamReader: java.io.BufferedReader? = null
        var connection: HttpURLConnection? = null
        val readingJob = launch(Dispatchers.IO) {
            try {
                val streamConnection = openStateStreamConnection(accessToken, anonKey)
                connection = streamConnection.first
                streamReader = streamConnection.second
                while (isActive) {
                    val line = streamReader?.readLine() ?: break
                    if (!line.startsWith("data:", ignoreCase = true)) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isNotEmpty()) {
                        trySend(payload)
                    }
                }
            } catch (_: Exception) {
            } finally {
                runCatching { streamReader?.close() }
                connection?.disconnect()
                channel.close()
            }
        }

        awaitClose {
            readingJob.cancel()
            runCatching { streamReader?.close() }
            connection?.disconnect()
        }
    }

    private suspend fun postSettingsPayload(payload: String): IntegrationSettingsResponse {
        val (status, responseText) = executeDaemonRequest(
            path = "/auth/integrations/settings",
            method = "POST",
            payload = payload
        )
        if (status !in 200..299) {
            throw IllegalStateException("Failed to update integrations: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    private suspend fun executeDaemonRequest(
        path: String,
        method: String,
        payload: String? = null,
        withContentType: Boolean = true,
        requiresAuth: Boolean = true
    ): Pair<Int, String> {
        return withContext(Dispatchers.IO) {
            val accessToken = client.auth.currentSessionOrNull()?.accessToken
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            runWithDaemonFallback(daemonBaseCandidates()) { baseUrl ->
                val endpoint = URL("$baseUrl$path")
                val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    doInput = true
                    doOutput = method != "GET"
                    if (requiresAuth) {
                        val token = accessToken ?: throw IllegalStateException("Not authenticated")
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    if (anonKey.isNotBlank()) {
                        setRequestProperty("apikey", anonKey)
                    }
                    if (withContentType) {
                        setRequestProperty("Content-Type", "application/json")
                    }
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }
                if (payload != null) {
                    OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload) }
                }
                val status = connection.responseCode
                val responseText = if (status in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
                if (status in 200..299) {
                    lastSuccessfulBaseUrl = baseUrl
                }
                status to responseText
            }
        }
    }

    private fun daemonBaseCandidates(): List<String> {
        return listOfNotNull(lastSuccessfulBaseUrl, *daemonBaseUrls.toTypedArray()).distinct()
    }

    private fun openStateStreamConnection(
        accessToken: String,
        anonKey: String
    ): Pair<HttpURLConnection, java.io.BufferedReader> {
        return runWithDaemonFallback(daemonBaseCandidates()) { baseUrl ->
            val endpoint = URL("$baseUrl/read/events/state")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                doInput = true
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Authorization", "Bearer $accessToken")
                if (anonKey.isNotBlank()) {
                    setRequestProperty("apikey", anonKey)
                }
                connectTimeout = 15_000
                readTimeout = 0
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                connection.disconnect()
                throw IllegalStateException("Failed to open state event stream: HTTP $status")
            }
            val reader = connection.inputStream.bufferedReader()
            if (status in 200..299) {
                lastSuccessfulBaseUrl = baseUrl
            }
            connection to reader
        }
    }

    fun lastSuccessfulDaemonBaseUrl(): String? = lastSuccessfulBaseUrl
}

internal fun defaultDaemonBaseUrls(overrideBaseUrl: String? = null): List<String> {
    val configured = overrideBaseUrl?.trim()?.takeIf { it.isNotEmpty() }
    val defaults = listOf("http://127.0.0.1:3140", "http://10.0.2.2:3140")
    return listOfNotNull(configured, *defaults.toTypedArray())
        .distinct()
}

internal inline fun <T> runWithDaemonFallback(
    baseUrls: List<String>,
    action: (baseUrl: String) -> T
): T {
    var lastRecoverableError: Exception? = null
    for (baseUrl in baseUrls) {
        try {
            return action(baseUrl)
        } catch (error: Exception) {
            if (
                error is java.net.ConnectException ||
                error is SocketTimeoutException ||
                error is UnknownHostException
            ) {
                lastRecoverableError = error
                continue
            }
            throw error
        }
    }
    throw IllegalStateException(
        "Failed to reach daemon endpoint on all candidates: ${baseUrls.joinToString(", ")}",
        lastRecoverableError
    )
}
