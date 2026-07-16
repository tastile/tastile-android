package app.tastile.android.data.repository

import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Surfacing v1 runtime path metadata for the diagnostic footer.
 *
 * The legacy integration repository also proxied Google Calendar,
 * daemon-side sync (sync/trigger, sync/recovery/...), calendar projections
 * (views/calendar/...), tile quota, OAuth, health, and debug events.
 * All of those live on the daemon surface that the v1-only build dropped
 * (`docs/agent-handoff/PROJECT-TRUTH.md`); see plan §4.C2.
 */
@Serializable
data class RuntimePathsResponse(
    @SerialName("profile_name") val profileName: String,
    @SerialName("app_data_dir") val appDataDir: String,
    @SerialName("db_path") val dbPath: String,
    @SerialName("session_path") val sessionPath: String,
    @SerialName("daemon_startup_log_path") val daemonStartupLogPath: String,
    @SerialName("daemon_executable_path") val daemonExecutablePath: String
)

@Singleton
class DefaultIntegrationRepository @Inject constructor(
    private val currentUserProvider: CurrentUserProvider,
    private val v1ApiClient: V1ApiClient,
) {
    @Volatile
    private var latestReadDiagnostics: String = "source=unknown"

    fun latestReadDiagnostics(): String = latestReadDiagnostics

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
}
