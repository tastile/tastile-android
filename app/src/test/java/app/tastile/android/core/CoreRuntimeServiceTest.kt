package app.tastile.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CoreRuntimeServiceTest {

    @Test
    fun applyCommand_delegatesToBridgeAndReturnsAck() {
        val expected = CoreCommandAck(
            accepted = true,
            requestId = "req-1",
            commandId = "cmd-1",
            eventIds = listOf("evt-1", "evt-2")
        )

        val bridge = TastileCoreBridge(
            libraryLoader = { },
            nativeBindings = object : TastileCoreBridge.NativeBindings {
                override fun dispatchCommand(commandJson: String): String = """
                    {"accepted":true,"requestId":"req-1","commandId":"cmd-1","eventIds":["evt-1","evt-2"]}
                """.trimIndent()
                override fun replaceEventLog(eventsJson: String): String = """
                    {"accepted":true,"commandId":"sync.replace_event_log","eventIds":[]}
                """.trimIndent()
                override fun getSnapshot(): String = """{"revision":11,"activeTileId":"tile-42","tiles":[{"id":"tile-42","title":"Delegate","lifecycle":"Ready"}]}"""
            }
        )
        val service: CoreRuntimeService = DefaultCoreRuntimeService(bridge)

        val result = service.applyCommand(CoreCommandRequest(type = "tile.create"))

        assertEquals(expected, result)
    }

    @Test
    fun applyCommand_propagatesTypedBridgeErrorsWithoutFallback() {
        val service: CoreRuntimeService = DefaultCoreRuntimeService(
            TastileCoreBridge(
                libraryLoader = { throw UnsatisfiedLinkError("library missing") },
                nativeBindings = object : TastileCoreBridge.NativeBindings {
                    override fun dispatchCommand(commandJson: String): String = "{}"
                    override fun replaceEventLog(eventsJson: String): String = "{}"
                    override fun getSnapshot(): String = "{}"
                }
            )
        )

        try {
            service.applyCommand(CoreCommandRequest(type = "tile.create"))
            fail("Expected LibraryLoadFailed")
        } catch (error: CoreBridgeError.LibraryLoadFailed) {
            assertEquals("tastile_core", error.libraryName)
        }
    }

    @Test
    fun applyCommand_persistsCreateCommandsWithNormalizedTileId() {
        val store = InMemoryCoreCommandStore()
        val service: CoreRuntimeService = PersistentCoreRuntimeService(
            bridge = TastileCoreBridge(
                libraryLoader = { },
                nativeBindings = object : TastileCoreBridge.NativeBindings {
                    override fun dispatchCommand(commandJson: String): String = """
                        {"accepted":true,"commandId":"tile.create","eventIds":["evt-1"],"metadata":{"tileId":"tile-generated"}}
                    """.trimIndent()

                    override fun replaceEventLog(eventsJson: String): String = """
                        {"accepted":true,"commandId":"sync.replace_event_log","eventIds":[]}
                    """.trimIndent()

                    override fun getSnapshot(): String = """{"revision":1,"tiles":[]}"""
                }
            ),
            commandStore = store
        )

        service.applyCommand(
            CoreCommandRequest(
                type = "tile.create",
                payload = buildJsonObject { put("title", JsonPrimitive("Write docs")) }
            )
        )

        assertEquals(1, store.saved.size)
        assertEquals("tile.create", store.saved.single().type)
        assertEquals(
            "tile-generated",
            store.saved.single().payload["tile_id"]?.let { (it as JsonPrimitive).content }
        )
    }

    @Test
    fun currentSnapshot_replaysPersistedCommandsBeforeReadingSnapshot() {
        val store = InMemoryCoreCommandStore(
            initial = listOf(
                CoreCommandRequest(
                    type = "tile.create",
                    payload = buildJsonObject {
                        put("tile_id", JsonPrimitive("tile-1"))
                        put("title", JsonPrimitive("Replay me"))
                    }
                )
            )
        )
        val dispatched = mutableListOf<String>()
        val service: CoreRuntimeService = PersistentCoreRuntimeService(
            bridge = TastileCoreBridge(
                libraryLoader = { },
                nativeBindings = object : TastileCoreBridge.NativeBindings {
                    override fun dispatchCommand(commandJson: String): String {
                        dispatched += commandJson
                        return """{"accepted":true,"commandId":"ok","eventIds":["evt-1"]}"""
                    }

                    override fun replaceEventLog(eventsJson: String): String = """
                        {"accepted":true,"commandId":"sync.replace_event_log","eventIds":[]}
                    """.trimIndent()

                    override fun getSnapshot(): String {
                        return """{"revision":2,"activeTileId":"tile-1","phaseKind":"work","tiles":[{"id":"tile-1","title":"Replay me","lifecycle":"Started"}]}"""
                    }
                }
            ),
            commandStore = store
        )

        val snapshot = service.currentSnapshot()

        assertEquals(1, dispatched.size)
        assertTrue(dispatched.single().contains("\"type\":\"tile.create\""))
        assertEquals("tile-1", snapshot.activeTileId)
    }

    @Test
    fun replaceEventLog_persistsImportedEventsAndRehydratesBeforeSnapshot() {
        val store = InMemoryCoreCommandStore()
        val imported = mutableListOf<String>()
        val service: CoreRuntimeService = PersistentCoreRuntimeService(
            bridge = TastileCoreBridge(
                libraryLoader = { },
                nativeBindings = object : TastileCoreBridge.NativeBindings {
                    override fun dispatchCommand(commandJson: String): String = """
                        {"accepted":true,"commandId":"ok","eventIds":["evt-1"]}
                    """.trimIndent()

                    override fun replaceEventLog(eventsJson: String): String {
                        imported += eventsJson
                        return """{"accepted":true,"commandId":"sync.replace_event_log","eventIds":[]}"""
                    }

                    override fun getSnapshot(): String = """
                        {"revision":3,"activeTileId":"tile-remote","phaseKind":"work","tiles":[{"id":"tile-remote","title":"Remote","lifecycle":"Started"}]}
                    """.trimIndent()
                }
            ),
            commandStore = store
        )

        service.replaceEventLog(
            listOf(
                CoreEventEnvelopeRecord(
                    eventId = "11111111-1111-1111-1111-111111111111",
                    aggregateId = "tile:tile-remote",
                    occurredAt = "2026-03-27T09:00:00Z",
                    actor = CoreActorRecord(
                        actorType = "system",
                        actorId = "22222222-2222-2222-2222-222222222222"
                    ),
                    event = buildJsonObject {
                        put("type", JsonPrimitive("tile_created"))
                        put(
                            "tile",
                            buildJsonObject {
                                put(
                                    "core",
                                    buildJsonObject {
                                        put("id", JsonPrimitive("tile-remote"))
                                        put("title", JsonPrimitive("Remote"))
                                    }
                                )
                            }
                        )
                    }
                )
            )
        )

        val rehydrated = PersistentCoreRuntimeService(
            bridge = TastileCoreBridge(
                libraryLoader = { },
                nativeBindings = object : TastileCoreBridge.NativeBindings {
                    override fun dispatchCommand(commandJson: String): String = """
                        {"accepted":true,"commandId":"ok","eventIds":["evt-1"]}
                    """.trimIndent()

                    override fun replaceEventLog(eventsJson: String): String {
                        imported += "rehydrated:$eventsJson"
                        return """{"accepted":true,"commandId":"sync.replace_event_log","eventIds":[]}"""
                    }

                    override fun getSnapshot(): String = """
                        {"revision":3,"activeTileId":"tile-remote","phaseKind":"work","tiles":[{"id":"tile-remote","title":"Remote","lifecycle":"Started"}]}
                    """.trimIndent()
                }
            ),
            commandStore = store
        )

        val snapshot = rehydrated.currentSnapshot()

        assertEquals(2, imported.size)
        assertTrue(imported[0].contains("\"tile-remote\""))
        assertTrue(imported[1].contains("\"tile-remote\""))
        assertEquals("tile-remote", snapshot.activeTileId)
    }

    @Test
    fun replaceEventLog_serializesEnvelopeWithSnakeCaseKeysForCoreRuntime() {
        val imported = mutableListOf<String>()
        val service: CoreRuntimeService = PersistentCoreRuntimeService(
            bridge = TastileCoreBridge(
                libraryLoader = { },
                nativeBindings = object : TastileCoreBridge.NativeBindings {
                    override fun dispatchCommand(commandJson: String): String = """
                        {"accepted":true,"commandId":"ok","eventIds":["evt-1"]}
                    """.trimIndent()

                    override fun replaceEventLog(eventsJson: String): String {
                        imported += eventsJson
                        return """{"accepted":true,"commandId":"sync.replace_event_log","eventIds":[]}"""
                    }

                    override fun getSnapshot(): String = """{"revision":0,"tiles":[]}"""
                }
            ),
            commandStore = InMemoryCoreCommandStore()
        )

        service.replaceEventLog(
            listOf(
                CoreEventEnvelopeRecord(
                    eventId = "11111111-1111-1111-1111-111111111111",
                    aggregateId = "tile:tile-remote",
                    occurredAt = "2026-03-27T09:00:00Z",
                    actor = CoreActorRecord(
                        actorType = "system",
                        actorId = "22222222-2222-2222-2222-222222222222"
                    ),
                    causedByCommandId = "33333333-3333-3333-3333-333333333333",
                    requestId = "44444444-4444-4444-4444-444444444444",
                    event = buildJsonObject {
                        put("type", JsonPrimitive("tile_created"))
                    }
                )
            )
        )

        val payload = imported.single()
        assertTrue(payload.contains("\"event_id\""))
        assertTrue(payload.contains("\"aggregate_id\""))
        assertTrue(payload.contains("\"occurred_at\""))
        assertTrue(payload.contains("\"caused_by_command_id\""))
        assertTrue(payload.contains("\"request_id\""))
        assertTrue(payload.contains("\"actor_type\""))
        assertTrue(payload.contains("\"actor_id\""))
    }
}

private class InMemoryCoreCommandStore(
    initial: List<CoreCommandRequest> = emptyList()
) : CoreCommandStore {
    val saved = initial.toMutableList()
    var importedEvents: List<CoreEventEnvelopeRecord> = emptyList()

    override fun load(): List<CoreCommandRequest> = saved.toList()

    override fun save(commands: List<CoreCommandRequest>) {
        saved.clear()
        saved += commands
    }

    override fun loadImportedEvents(): List<CoreEventEnvelopeRecord> = importedEvents

    override fun saveImportedEvents(events: List<CoreEventEnvelopeRecord>) {
        importedEvents = events.toList()
    }
}
