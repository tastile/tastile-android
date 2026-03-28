package app.tastile.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TastileCoreBridgeTest {

    @Test
    fun parseSnapshot_fromJson_returnsTypedSnapshot() {
        val json = """
            {
              "revision": 7,
              "activeTileId": "tile-123",
              "phaseKind": "work",
              "phaseEndsAt": "2026-03-27T09:15:00.000Z",
              "tiles": [
                {"id": "tile-123", "title": "Write tests", "lifecycle": "Started", "semanticRole":"work", "fixedStartAt":"2026-03-27T10:00:00.000Z"}
              ],
              "promptQueue": [
                {"promptId":"end:tile-123","tileId":"tile-123","kind":"end_tile","severity":"critical","reasons":["work_phase_expired"],"actions":["complete_tile"],"scheduledAt":"2026-03-27T09:15:00.000Z","reason":"work_phase_expired","status":"pending"}
              ]
            }
        """.trimIndent()

        val snapshot = CoreSnapshot.fromJson(json)

        assertEquals(7L, snapshot.revision)
        assertEquals("tile-123", snapshot.activeTileId)
        assertEquals("work", snapshot.phaseKind)
        assertEquals("2026-03-27T09:15:00.000Z", snapshot.phaseEndsAt)
        assertEquals(1, snapshot.tiles.size)
        assertEquals("Write tests", snapshot.tiles.first().title)
        assertEquals("work", snapshot.tiles.first().semanticRole)
        assertEquals("2026-03-27T10:00:00.000Z", snapshot.tiles.first().fixedStartAt)
        assertEquals(1, snapshot.promptQueue.size)
        assertEquals("end_tile", snapshot.promptQueue.first().kind)
    }

    @Test
    fun applyCommand_whenNativeEntryPointMissing_throwsTypedError() {
        val bridge = TastileCoreBridge(
            libraryLoader = { },
            nativeBindings = object : TastileCoreBridge.NativeBindings {
                override fun dispatchCommand(commandJson: String): String {
                    throw UnsatisfiedLinkError("dispatchCommand not exported")
                }

                override fun replaceEventLog(eventsJson: String): String = "{}"
                override fun getSnapshot(): String = """{"revision":0,"tiles":[]}"""
            }
        )

        try {
            bridge.applyCommand(CoreCommandRequest(type = "tile.create"))
            fail("Expected NativeMethodUnavailable")
        } catch (error: CoreBridgeError.NativeMethodUnavailable) {
            assertEquals("dispatchCommand", error.methodName)
        }
    }

    @Test
    fun currentSnapshot_whenNativeReturnsMalformedJson_throwsTypedParseError() {
        val bridge = TastileCoreBridge(
            libraryLoader = { },
            nativeBindings = object : TastileCoreBridge.NativeBindings {
                override fun dispatchCommand(commandJson: String): String = "{}"
                override fun replaceEventLog(eventsJson: String): String = "{}"
                override fun getSnapshot(): String = "{ this is not json }"
            }
        )

        try {
            bridge.currentSnapshot()
            fail("Expected SnapshotParseFailed")
        } catch (error: CoreBridgeError.SnapshotParseFailed) {
            assertTrue(error.rawPayload.contains("this is not json"))
        }
    }

    @Test
    fun applyCommand_whenNativeReportsDomainError_throwsCommandFailed() {
        val bridge = TastileCoreBridge(
            libraryLoader = { },
            nativeBindings = object : TastileCoreBridge.NativeBindings {
                override fun dispatchCommand(commandJson: String): String = """
                    {"accepted":false,"error":{"code":"tile_not_found","message":"Tile not found"}}
                """.trimIndent()
                override fun replaceEventLog(eventsJson: String): String = "{}"
                override fun getSnapshot(): String = """{"revision":0,"tiles":[]}"""
            }
        )

        try {
            bridge.applyCommand(CoreCommandRequest(type = "tile.start"))
            fail("Expected CommandFailed")
        } catch (error: CoreBridgeError.CommandFailed) {
            assertEquals("tile_not_found", error.errorCode)
            assertEquals("Tile not found", error.messageText)
        }
    }

    @Test
    fun applyCommand_whenNativeReturnsFailureWithoutError_throwsParseFailure() {
        val bridge = TastileCoreBridge(
            libraryLoader = { },
            nativeBindings = object : TastileCoreBridge.NativeBindings {
                override fun dispatchCommand(commandJson: String): String = """{"accepted":false}"""
                override fun replaceEventLog(eventsJson: String): String = "{}"
                override fun getSnapshot(): String = """{"revision":0,"tiles":[]}"""
            }
        )

        try {
            bridge.applyCommand(CoreCommandRequest(type = "tile.start"))
            fail("Expected SnapshotParseFailed")
        } catch (error: CoreBridgeError.SnapshotParseFailed) {
            assertTrue(error.rawPayload.contains("\"accepted\":false"))
        }
    }

    @Test
    fun applyCommand_whenNativeReturnsMalformedCommandResponse_throwsCommandResponseParseFailure() {
        val bridge = TastileCoreBridge(
            libraryLoader = { },
            nativeBindings = object : TastileCoreBridge.NativeBindings {
                override fun dispatchCommand(commandJson: String): String = "{ malformed"
                override fun replaceEventLog(eventsJson: String): String = "{}"
                override fun getSnapshot(): String = """{"revision":0,"tiles":[]}"""
            }
        )

        try {
            bridge.applyCommand(CoreCommandRequest(type = "tile.create"))
            fail("Expected CommandResponseParseFailed")
        } catch (error: CoreBridgeError.CommandResponseParseFailed) {
            assertTrue(error.rawPayload.contains("{ malformed"))
        }
    }

    @Test
    fun applyCommand_whenNativeReturnsAcceptedAck_returnsAckMetadata() {
        val bridge = TastileCoreBridge(
            libraryLoader = { },
            nativeBindings = object : TastileCoreBridge.NativeBindings {
                override fun dispatchCommand(commandJson: String): String = """
                    {"accepted":true,"requestId":"req-10","commandId":"cmd-10","eventIds":["evt-10"]}
                """.trimIndent()
                override fun replaceEventLog(eventsJson: String): String = "{}"
                override fun getSnapshot(): String = """{"revision":0,"tiles":[]}"""
            }
        )

        val ack = bridge.applyCommand(CoreCommandRequest(type = "tile.start"))

        assertTrue(ack.accepted)
        assertEquals("req-10", ack.requestId)
        assertEquals("cmd-10", ack.commandId)
        assertEquals(listOf("evt-10"), ack.eventIds)
    }
}
