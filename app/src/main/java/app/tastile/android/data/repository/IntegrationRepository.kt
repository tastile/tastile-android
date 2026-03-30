package app.tastile.android.data.repository

import app.tastile.android.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
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

@Singleton
class IntegrationRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val daemonBaseUrl = "http://127.0.0.1:3140"

    suspend fun getSettings(): IntegrationSettingsResponse {
        val connection = openDaemonConnection("/auth/integrations/settings", "GET")
        val status = connection.responseCode
        val responseText = if (status in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        if (status !in 200..299) {
            throw IllegalStateException("Failed to load integrations: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    suspend fun updateGoogleCalendarConnected(connected: Boolean): IntegrationSettingsResponse {
        val payload = buildJsonObject {
            put(
                "google_calendar",
                buildJsonObject {
                    put("connected", JsonPrimitive(connected))
                    if (!connected) {
                        put("account_email", JsonNull)
                    }
                }
            )
        }.toString()
        return postSettingsPayload(payload)
    }

    suspend fun markGoogleCalendarSyncedNow(): IntegrationSettingsResponse {
        val payload = buildJsonObject {
            put(
                "google_calendar",
                buildJsonObject {
                    put("last_synced_at", JsonPrimitive(java.time.Instant.now().toString()))
                }
            )
        }.toString()
        return postSettingsPayload(payload)
    }

    suspend fun triggerSync() {
        val connection = openDaemonConnection("/sync/trigger", "POST", withContentType = false)
        val status = connection.responseCode
        if (status !in 200..299 && status != 202) {
            val responseText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException("Failed to trigger sync: HTTP $status $responseText")
        }
    }

    private fun postSettingsPayload(payload: String): IntegrationSettingsResponse {
        val connection = openDaemonConnection("/auth/integrations/settings", "POST")
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload) }
        val status = connection.responseCode
        val responseText = if (status in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        if (status !in 200..299) {
            throw IllegalStateException("Failed to update integrations: HTTP $status $responseText")
        }
        return json.decodeFromString(responseText)
    }

    private fun openDaemonConnection(
        path: String,
        method: String,
        withContentType: Boolean = true
    ): HttpURLConnection {
        val accessToken = client.auth.currentSessionOrNull()?.accessToken
            ?: throw IllegalStateException("Not authenticated")
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val endpoint = URL("$daemonBaseUrl$path")
        return (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            doInput = true
            doOutput = method != "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            if (anonKey.isNotBlank()) {
                setRequestProperty("apikey", anonKey)
            }
            if (withContentType) {
                setRequestProperty("Content-Type", "application/json")
            }
            connectTimeout = 15_000
            readTimeout = 15_000
        }
    }
}
