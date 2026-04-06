package app.tastile.android.data.repository

import app.tastile.android.core.CoreCommandAck
import app.tastile.android.core.CoreCommandRequest
import app.tastile.android.core.CoreEventEnvelopeRecord
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.core.CoreSnapshot
import app.tastile.android.core.CoreTileSnapshot
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import io.github.jan.supabase.SupabaseClient
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TileRepositoryCoreCommandTest {

    @Test
    fun startTile_usesCoreCommandAndReturnsProjectedSnapshotTile() = runTest {
        val service = RecordingCoreRuntimeService(
            snapshotBeforeCommand = CoreSnapshot(
                revision = 2,
                activeTileId = "tile-123",
                tiles = listOf(
                    CoreTileSnapshot(id = "tile-123", title = "Focus block", lifecycle = "Started")
                )
            )
        )
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = service,
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        val tile = repository.startTile("tile-123")

        assertEquals("tile.start", service.lastCommand?.type)
        assertEquals("tile-123", service.lastCommand?.payload?.string("tile_id"))
        assertEquals("tile-123", tile.id)
        assertEquals("Started", tile.lifecycle)
        assertEquals("Focus block", tile.title)
    }

    @Test
    fun getActiveStartedTile_prefersCoreSnapshotProjection() = runTest {
        val service = RecordingCoreRuntimeService(
            snapshotBeforeCommand = CoreSnapshot(
                revision = 8,
                activeTileId = "tile-active",
                tiles = listOf(
                    CoreTileSnapshot(id = "tile-ready", title = "Inbox", lifecycle = "Ready"),
                    CoreTileSnapshot(id = "tile-active", title = "Deep work", lifecycle = "Started")
                )
            )
        )
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = service,
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        val active = repository.getActiveStartedTile("user-1")

        assertEquals("tile-active", active?.id)
        assertEquals("Deep work", active?.title)
        assertEquals("Started", active?.lifecycle)
    }

    @Test
    fun getActiveStartedTile_returnsNullWhenSnapshotHasNoActiveTile() = runTest {
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = RecordingCoreRuntimeService(
                snapshotBeforeCommand = CoreSnapshot(revision = 1, tiles = emptyList())
            ),
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        val active = repository.getActiveStartedTile("user-1")

        assertNull(active)
    }

    @Test
    fun continueTile_usesCoreCommand() = runTest {
        val service = RecordingCoreRuntimeService(
            snapshotBeforeCommand = CoreSnapshot(revision = 3, activeTileId = "tile-continue")
        )
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = service,
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        repository.continueTile("tile-continue")

        assertEquals("tile.continue", service.lastCommand?.type)
        assertEquals("tile-continue", service.lastCommand?.payload?.string("tile_id"))
    }

    @Test
    fun deferTile_usesCoreCommand() = runTest {
        val service = RecordingCoreRuntimeService(snapshotBeforeCommand = CoreSnapshot(revision = 3))
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = service,
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        repository.deferTile("tile-1", reason = "meeting", minutes = 15)

        assertEquals("tile.defer", service.lastCommand?.type)
        assertEquals("tile-1", service.lastCommand?.payload?.string("tile_id"))
        assertEquals("meeting", service.lastCommand?.payload?.string("reason"))
        assertEquals("15", service.lastCommand?.payload?.string("minutes"))
    }

    @Test
    fun startBreak_usesCoreCommand() = runTest {
        val service = RecordingCoreRuntimeService(snapshotBeforeCommand = CoreSnapshot(revision = 3))
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = service,
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        repository.startBreak(breakMin = 10, insertionMode = "after_current")

        assertEquals("break.start", service.lastCommand?.type)
        assertEquals("10", service.lastCommand?.payload?.string("break_min"))
        assertEquals("after_current", service.lastCommand?.payload?.string("insertion_mode"))
    }

    @Test
    fun endBreak_usesCoreCommand() = runTest {
        val service = RecordingCoreRuntimeService(snapshotBeforeCommand = CoreSnapshot(revision = 3))
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = service,
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        repository.endBreak()

        assertEquals("break.end", service.lastCommand?.type)
    }

    @Test
    fun extendTile_usesCoreCommand() = runTest {
        val service = RecordingCoreRuntimeService(snapshotBeforeCommand = CoreSnapshot(revision = 3))
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = service,
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        repository.extendTile(extendMin = 20)

        assertEquals("tile.extend", service.lastCommand?.type)
        assertEquals("20", service.lastCommand?.payload?.string("delta_min"))
    }

    @Test
    fun completeTile_withScopeAndNextTile_usesCoreCommandPayload() = runTest {
        val service = RecordingCoreRuntimeService(snapshotBeforeCommand = CoreSnapshot(revision = 3))
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = service,
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        repository.completeTile(tileId = "tile-1", nextTileId = "tile-2", scope = "phase")

        assertEquals("tile.complete", service.lastCommand?.type)
        assertEquals("tile-1", service.lastCommand?.payload?.string("tile_id"))
        assertEquals("tile-2", service.lastCommand?.payload?.string("next_tile_id"))
        assertEquals("phase", service.lastCommand?.payload?.string("scope"))
    }

    @Test
    fun attachMemo_withMemoKind_usesCoreCommandPayload() = runTest {
        val service = RecordingCoreRuntimeService(snapshotBeforeCommand = CoreSnapshot(revision = 3))
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = service,
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        repository.attachMemo(tileId = null, text = "memo text", memoKind = "reflection")

        assertEquals("memo.attach", service.lastCommand?.type)
        assertEquals("memo text", service.lastCommand?.payload?.string("text"))
        assertEquals("reflection", service.lastCommand?.payload?.string("memo_kind"))
    }

    @Test
    fun createTile_prefersGeneratedTileIdOverMatchingTitle() = runTest {
        val service = RecordingCoreRuntimeService(
            snapshotBeforeCommand = CoreSnapshot(
                revision = 1,
                tiles = listOf(
                    CoreTileSnapshot(id = "tile-existing", title = "Inbox", lifecycle = "Ready")
                )
            ),
            snapshotAfterCommand = CoreSnapshot(
                revision = 2,
                tiles = listOf(
                    CoreTileSnapshot(id = "tile-existing", title = "Inbox", lifecycle = "Ready"),
                    CoreTileSnapshot(id = "tile-created", title = "Inbox", lifecycle = "Ready")
                )
            ),
            ack = CoreCommandAck(
                accepted = true,
                commandId = "cmd-create",
                metadata = buildJsonObject { put("tileId", "tile-created") }
            )
        )
        val repository = TileRepository(
            client = mockk<SupabaseClient>(relaxed = true),
            coreRuntimeService = service,
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
            }
        )

        val tile = repository.createTile("user-1", "Inbox")

        assertEquals("tile-created", tile.id)
    }

    @Test
    fun recordingCoreRuntimeService_canReturnRejectedAck() {
        val rejectedAck = CoreCommandAck(
            accepted = false,
            commandId = "cmd-rejected",
            metadata = buildJsonObject { put("reason", "duplicate") }
        )
        val service = RecordingCoreRuntimeService(
            snapshotBeforeCommand = CoreSnapshot(revision = 1),
            ack = rejectedAck
        )

        val ack = service.applyCommand(
            CoreCommandRequest(
                type = "tile.create",
                payload = buildJsonObject { put("title", "Inbox") }
            )
        )

        assertEquals(false, ack.accepted)
        assertEquals("cmd-rejected", ack.commandId)
        assertEquals("duplicate", ack.metadata?.get("reason")?.toString()?.trim('"'))
    }

    private class RecordingCoreRuntimeService(
        private val snapshotBeforeCommand: CoreSnapshot,
        private val snapshotAfterCommand: CoreSnapshot = snapshotBeforeCommand,
        private val ack: CoreCommandAck = CoreCommandAck(accepted = true, commandId = "cmd-1")
    ) : CoreRuntimeService {
        var lastCommand: CoreCommandRequest? = null
        private var commandApplied = false

        override fun applyCommand(command: CoreCommandRequest): CoreCommandAck {
            lastCommand = command
            commandApplied = true
            return ack
        }

        override fun replaceEventLog(events: List<CoreEventEnvelopeRecord>): CoreCommandAck {
            return CoreCommandAck(accepted = true, commandId = "sync.replace_event_log")
        }

        override fun currentSnapshot(): CoreSnapshot {
            return if (commandApplied) snapshotAfterCommand else snapshotBeforeCommand
        }
    }

    private fun JsonObject.string(key: String): String? = this[key]?.toString()?.trim('"')
}
