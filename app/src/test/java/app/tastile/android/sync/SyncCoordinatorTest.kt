package app.tastile.android.sync

import app.tastile.android.core.CoreCommandAck
import app.tastile.android.core.CoreCommandRequest
import app.tastile.android.core.CoreEventEnvelopeRecord
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.core.CoreSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCoordinatorTest {

    @Test
    fun onSessionAvailable_sendsAuthThenImportsRemoteEvents() = runTest {
        val runtime = RecordingCoreRuntimeService()
        val syncService = RecordingCoreEventSyncService()
        val coordinator = SyncCoordinator(runtime, syncService)

        coordinator.onSessionAvailable(
            userId = "user-1",
            accessToken = "access-1",
            refreshToken = "refresh-1"
        )

        assertEquals(1, runtime.commands.size)
        assertEquals(SyncCoordinator.COMMAND_AUTH_SET_SESSION, runtime.commands[0].type)
        assertEquals(
            "user-1",
            runtime.commands[0].payload["userId"]?.toString()?.trim('"')
        )
        assertEquals(listOf("user-1"), syncService.userIds)
    }

    @Test
    fun onSessionAvailable_sameSessionIsDeduplicated() = runTest {
        val runtime = RecordingCoreRuntimeService()
        val syncService = RecordingCoreEventSyncService()
        val coordinator = SyncCoordinator(runtime, syncService)

        coordinator.onSessionAvailable("user-1", "access-1", "refresh-1")
        coordinator.onSessionAvailable("user-1", "access-1", "refresh-1")

        assertEquals(1, runtime.commands.size)
        assertEquals(listOf("user-1"), syncService.userIds)
    }

    @Test
    fun markCoreBridgeUnavailable_stopsFutureDispatch() = runTest {
        val runtime = RecordingCoreRuntimeService()
        val syncService = RecordingCoreEventSyncService()
        val coordinator = SyncCoordinator(runtime, syncService)
        coordinator.markCoreBridgeUnavailable()

        coordinator.onSessionAvailable("user-1", "access-1", "refresh-1")

        assertTrue(runtime.commands.isEmpty())
        assertTrue(syncService.userIds.isEmpty())
    }

    private class RecordingCoreRuntimeService : CoreRuntimeService {
        val commands = mutableListOf<CoreCommandRequest>()

        override fun applyCommand(command: CoreCommandRequest): CoreCommandAck {
            commands += command
            return CoreCommandAck(accepted = true)
        }

        override fun replaceEventLog(events: List<CoreEventEnvelopeRecord>): CoreCommandAck {
            return CoreCommandAck(accepted = true)
        }

        override fun currentSnapshot(): CoreSnapshot = CoreSnapshot(revision = 0)
    }

    private class RecordingCoreEventSyncService : CoreEventSyncService {
        val userIds = mutableListOf<String>()

        override suspend fun syncUserEvents(userId: String) {
            userIds += userId
        }
    }
}

