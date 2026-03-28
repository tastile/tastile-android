package app.tastile.android.sync

import app.tastile.android.core.CoreCommandRequest
import app.tastile.android.core.CoreRuntimeService
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    private val coreRuntimeService: CoreRuntimeService,
    private val coreEventSyncService: CoreEventSyncService
) {
    private var lastSessionFingerprint: String? = null
    private var coreBridgeUnavailable: Boolean = false

    suspend fun onSessionAvailable(userId: String, accessToken: String, refreshToken: String) {
        if (coreBridgeUnavailable) return

        val fingerprint = "$userId|$accessToken|$refreshToken"
        if (fingerprint == lastSessionFingerprint) return

        coreRuntimeService.applyCommand(
            CoreCommandRequest(
                type = COMMAND_AUTH_SET_SESSION,
                payload = buildJsonObject {
                    put("userId", userId)
                    put("accessToken", accessToken)
                    put("refreshToken", refreshToken)
                }
            )
        )

        coreEventSyncService.syncUserEvents(userId)

        lastSessionFingerprint = fingerprint
    }

    fun markCoreBridgeUnavailable() {
        coreBridgeUnavailable = true
    }

    companion object {
        const val COMMAND_AUTH_SET_SESSION = "auth.set_session"
    }
}

