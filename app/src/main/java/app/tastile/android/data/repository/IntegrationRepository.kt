package app.tastile.android.data.repository

import app.tastile.android.BuildConfig
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import app.tastile.android.data.api.V1TimelineResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
import java.net.URLEncoder
import java.net.UnknownHostException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GoogleCalendarIntegrationSettings(
    val connected: Boolean = false,
    @SerialName("provider_status") val providerStatus: String = "disconnected",
    @SerialName("can_read") val canRead: Boolean = true,
    @SerialName("can_write") val canWrite: Boolean = true,
    @SerialName("account_email") val accountEmail: String? = null,
    @SerialName("selected_calendar_id") val selectedCalendarId: String? = null,
    @SerialName("granted_scopes") val grantedScopes: List<String> = emptyList(),
    @SerialName("sync_mode") val syncMode: String = "push_only",
    @SerialName("read_policy") val readPolicy: String = "import_and_block_scheduling",
    @SerialName("write_policy") val writePolicy: String = "tastile_owned_only",
    @SerialName("last_synced_at") val lastSyncedAt: String? = null,
    @SerialName("last_full_sync_at") val lastFullSyncAt: String? = null,
)

@Serializable
data class IntegrationSettingsResponse(
    @SerialName("google_calendar") val googleCalendar: GoogleCalendarIntegrationSettings
)

@Serializable
data class CalendarSyncPlanPreviewResponse(
    val provider: String,
    @SerialName("selected_calendar_id") val selectedCalendarId: String? = null,
    @SerialName("sync_mode") val syncMode: String = "push_only",
    @SerialName("read_policy") val readPolicy: String = "import_and_block_scheduling",
    @SerialName("write_policy") val writePolicy: String = "tastile_owned_only"
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
    private val authRepository: AuthRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val v1ApiClient: V1ApiClient
) {
    private companion object {
        private const val STREAM_POLL_INTERVAL_MS = 30_000L
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val daemonBaseUrls = defaultDaemonBaseUrls()
    @Volatile
    private var lastSuccessfulBaseUrl: String? = null
    @Volatile
    private var latestReadDiagnostics: String = "source=unknown"

    fun latestReadDiagnostics(): String = latestReadDiagnostics

    suspend fun getSettings(): IntegrationSettingsResponse {
        val (status, responseText) = executeDaemonRequest(path = "/auth/integrations/settings", method = "GET")
        if (status == 404) {
            return IntegrationSettingsResponse(GoogleCalendarIntegrationSettings())
        }
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
        selectedCalendarId: String? = null,
        syncMode: String? = null,
        readPolicy: String? = null,
        writePolicy: String? = null,
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
                    if (selectedCalendarId != null) {
                        if (selectedCalendarId.isBlank()) {
                            put("selected_calendar_id", JsonNull)
                        } else {
                            put("selected_calendar_id", JsonPrimitive(selectedCalendarId))
                        }
                    }
                    if (!syncMode.isNullOrBlank()) put("sync_mode", JsonPrimitive(syncMode))
                    if (!readPolicy.isNullOrBlank()) put("read_policy", JsonPrimitive(readPolicy))
                    if (!writePolicy.isNullOrBlank()) put("write_policy", JsonPrimitive(writePolicy))
                    if (lastSyncedAt != null) put("last_synced_at", JsonPrimitive(lastSyncedAt))
                }
            )
        }.toString()
        return postSettingsPayload(payload)
    }

    suspend fun getCalendarSyncPlanPreview(): CalendarSyncPlanPreviewResponse {
        val (status, responseText) = executeDaemonRequest(
            path = "/auth/integrations/calendar/sync-plan",
            method = "GET"
        )
        if (status !in 200..299) {
            throw IllegalStateException("Failed to load calendar sync plan: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun getCalendarMonthProjection(anchor: String? = null): CalendarProjectionResponse {
        val query = anchor?.takeIf { it.isNotBlank() }?.let {
            "?anchor=${URLEncoder.encode(it, Charsets.UTF_8.name())}"
        }.orEmpty()
        val (status, responseText) = executeDaemonRequest(
            path = "/views/calendar/month$query",
            method = "GET"
        )
        if (status !in 200..299) {
            throw IllegalStateException("Failed to load calendar month projection: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
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
        if (status == 404) {
            return SyncStatusResponse()
        }
        if (status !in 200..299) {
            throw IllegalStateException("Failed to load sync status: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun getRuntimePaths(): RuntimePathsResponse {
        val token = currentUserProvider.currentIdToken()
        if (token.isNullOrBlank()) {
            latestReadDiagnostics = "source=v1_skipped reason=no_token count=0 user_match=true"
            return emptyRuntimePathsResponse()
        }
        return try {
            val response = v1ApiClient.listRuntimePaths()
            val first = response.paths.firstOrNull()
            if (first == null) {
                latestReadDiagnostics = "source=v1 count=0 user_match=true"
                return emptyRuntimePathsResponse()
            }
            latestReadDiagnostics = "source=v1 count=${response.paths.size} user_match=true"
            RuntimePathsResponse(
                profileName = first.profileName,
                appDataDir = first.appDataDir,
                dbPath = first.dbPath,
                sessionPath = first.sessionPath,
                daemonStartupLogPath = first.daemonStartupLogPath,
                daemonExecutablePath = first.daemonExecutablePath
            )
        } catch (e: V1Error) {
            android.util.Log.w("IntegrationRepository", "v1 listRuntimePaths failed: ${e.message}", e)
            latestReadDiagnostics = "source=v1_unavailable count=0 user_match=true"
            emptyRuntimePathsResponse()
        } catch (e: Exception) {
            android.util.Log.w("IntegrationRepository", "v1 listRuntimePaths failed: ${e.message}", e)
            latestReadDiagnostics = "source=v1_unavailable count=0 user_match=true"
            emptyRuntimePathsResponse()
        }
    }

    private fun emptyRuntimePathsResponse(): RuntimePathsResponse =
        RuntimePathsResponse(
            profileName = "cloud",
            appDataDir = "",
            dbPath = "",
            sessionPath = "",
            daemonStartupLogPath = "",
            daemonExecutablePath = ""
        )

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
        return !authRepository.currentIdToken().isNullOrBlank()
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

    // NOTE: Contract change — v1 has no SSE. This used to be an SSE consumer over
    // `/read/events/state` emitting JSON lines per state event. v1 is pull-only,
    // so we poll `GET /v1/timeline?start=&end=` on a 30s cadence and emit the
    // JSON-encoded `V1TimelineResponse` once per cycle. Consumers of
    // `Flow<String>` should treat each emission as a timeline snapshot.
    fun streamStateEvents(): Flow<String> = flow {
        val now = Instant.now()
        val start = now.minusSeconds(24L * 60L * 60L)
        val end = now.plusSeconds(24L * 60L * 60L)
        while (true) {
            val payload = runCatching {
                val response = v1ApiClient.getTimeline(start = start, end = end)
                latestReadDiagnostics = "source=v1 timeline_count=${response.items.size} user_match=true"
                json.encodeToString(V1TimelineResponse.serializer(), response)
            }.getOrElse { e ->
                android.util.Log.w(
                    "IntegrationRepository",
                    "v1 getTimeline failed: ${e.message}",
                    e
                )
                latestReadDiagnostics = "source=v1_unavailable timeline_count=0 user_match=true"
                null
            }
            if (payload != null) {
                emit(payload)
            }
            delay(STREAM_POLL_INTERVAL_MS)
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
            val accessToken = authRepository.currentIdToken()
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

    fun lastSuccessfulDaemonBaseUrl(): String? = lastSuccessfulBaseUrl
}

internal fun defaultDaemonBaseUrls(overrideBaseUrl: String? = null): List<String> {
    val configured = overrideBaseUrl?.trim()?.takeIf { it.isNotEmpty() }
    val core = BuildConfig.TASTILE_CORE_URL.trim().takeIf { it.isNotEmpty() }
    val defaults = listOf("http://127.0.0.1:3140", "http://10.0.2.2:3140")
    return listOfNotNull(configured, core, *defaults.toTypedArray())
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
