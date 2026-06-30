package app.tastile.android.data.command

import app.tastile.android.core.CoreCommandAck
import app.tastile.android.data.api.AggregateRef
import app.tastile.android.data.api.CommandResponse
import app.tastile.android.data.api.PendingWork
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import app.tastile.android.data.api.V1NumericConstants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant

class V1CommandDispatcherTest {

    private fun okResponse(tileId: String = "t-123"): CommandResponse =
        CommandResponse(
            commandId = "cmd-1",
            acceptedAt = Instant.now().toString(),
            aggregate = AggregateRef(kind = V1NumericConstants.AggregateKind.PLACEMENT, id = tileId),
            revision = 1L,
            result = V1NumericConstants.CommandResult.APPLIED,
            pending = emptyList()
        )

    /**
     * Use a relaxed mock with explicit stubs on the call we care about. mockk
     * can stub a generic method as long as each `any()` arg carries an explicit
     * type — `KSerializer<*>` works because the dispatcher passes a specific
     * serializer instance at runtime.
     */
    private fun newApiClient(): V1ApiClient = mockk<V1ApiClient>(relaxed = true)

    // --- CreateTile ----------------------------------------------------

    @Test
    fun dispatchTileCreate_postsToV1AndReturnsAckWithTileId() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles",
                commandKind = "CreateTile",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns okResponse("t-123")

        val dispatcher = V1CommandDispatcher(apiClient)
        val payload = buildJsonObject {
            put("title", JsonPrimitive("Walk dog"))
        }
        val ack: CoreCommandAck? = dispatcher.dispatchTileCreate(payload, userId = "user-1")

        assertNotNull(ack)
        assertTrue(ack!!.accepted)
        assertEquals("cmd-1", ack.commandId)
        assertEquals("t-123", ack.generatedTileId())
        coVerify(exactly = 1) {
            apiClient.postCommand(
                path = "/v1/tiles",
                commandKind = "CreateTile",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
    }

    @Test
    fun dispatchTileCreate_returnsNullOnV1Error_network() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles",
                commandKind = "CreateTile",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } throws V1Error.Network(IOException("boom"))

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileCreate(
            buildJsonObject { put("title", JsonPrimitive("X")) },
            userId = "user-1"
        )

        assertNull(ack)
    }

    @Test
    fun dispatchTileCreate_returnsNullOnV1Error_auth() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles",
                commandKind = "CreateTile",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } throws V1Error.Auth()

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileCreate(
            buildJsonObject { put("title", JsonPrimitive("X")) },
            userId = "user-1"
        )

        assertNull(ack)
    }

    // --- DeleteTile ----------------------------------------------------

    @Test
    fun dispatchTileDelete_callsDeleteEndpoint() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.deleteCommand(path = "/v1/tiles/t-123", responseSerializer = any<KSerializer<Any>>())
        } returns okResponse(tileId = "t-123")

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileDelete("t-123")

        assertNotNull(ack)
        coVerify(exactly = 1) {
            apiClient.deleteCommand(path = "/v1/tiles/t-123", responseSerializer = any<KSerializer<Any>>())
        }
    }

    // --- UpdateTile ----------------------------------------------------

    @Test
    fun dispatchTileUpdate_mapsFields() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/update",
                commandKind = "UpdateTile",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns okResponse("t-123")

        val dispatcher = V1CommandDispatcher(apiClient)
        val payload = buildJsonObject {
            put("title", JsonPrimitive("Renamed"))
            put("color", JsonPrimitive("#ff0000"))
        }
        val ack = dispatcher.dispatchTileUpdate("t-123", payload)

        assertNotNull(ack)
        coVerify(exactly = 1) {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/update",
                commandKind = "UpdateTile",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
    }

    // --- SetTileLifecycle: complete / defer / extend ------------------

    @Test
    fun dispatchTileDefer_buildsSetTileLifecycleWithState1AndDeferredUntil() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/defer",
                commandKind = "SetTileLifecycle",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns okResponse("t-123")

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileDefer("t-123", reason = null, minutes = 15)

        assertNotNull(ack)
        coVerify(exactly = 1) {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/defer",
                commandKind = "SetTileLifecycle",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
    }

    @Test
    fun dispatchTileComplete_buildsSetTileLifecycleWithState2() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/complete",
                commandKind = "SetTileLifecycle",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns okResponse("t-123")

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileComplete(
            effectiveTileId = "t-123",
            nextTileId = null,
            scope = null
        )

        assertNotNull(ack)
        coVerify(exactly = 1) {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/complete",
                commandKind = "SetTileLifecycle",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
    }

    @Test
    fun dispatchTileExtend_fallsThroughToNullBecauseV0HasNoTileId() = runTest {
        // v0 `tile.extend` payload was `{ delta_min }` with no tile_id, so the
        // dispatcher can't address /v1/tiles/{id}/extend-phase. Step 4 leaves
        // extend on the v0 fallback path (returns null, never calls v1).
        val apiClient = newApiClient()
        val dispatcher = V1CommandDispatcher(apiClient)

        val ack = dispatcher.dispatchTileExtend(deltaMin = 5)

        assertNull(ack)
        coVerify(exactly = 0) {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/extend-phase",
                commandKind = "SetTileLifecycle",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
    }

    // --- AttachMemo ----------------------------------------------------

    @Test
    fun dispatchMemoAttach_buildsAttachMemoPayload() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/memos",
                commandKind = "AttachMemo",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns okResponse("t-123")

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchMemoAttach("t-123", body = "remember this")

        assertNotNull(ack)
        coVerify(exactly = 1) {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/memos",
                commandKind = "AttachMemo",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
    }

    // --- Mapping of CommandResponse → CoreCommandAck ------------------

    @Test
    fun dispatcher_mapsAcceptedAsFalseWhenResultIsAcquiredButNotApplied() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles",
                commandKind = "CreateTile",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns CommandResponse(
            commandId = "cmd-1",
            acceptedAt = Instant.now().toString(),
            aggregate = AggregateRef(kind = V1NumericConstants.AggregateKind.PLACEMENT, id = "t-x"),
            revision = 1L,
            result = V1NumericConstants.CommandResult.ACCEPTED, // 2 → not yet applied
            pending = emptyList()
        )
        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileCreate(
            buildJsonObject { put("title", JsonPrimitive("X")) },
            userId = "user-1"
        )
        assertNotNull(ack)
        assertFalse(ack!!.accepted)
    }

    @Test
    fun dispatcher_includesPendingInMetadata() = runTest {
        val apiClient = newApiClient()
        val now = Instant.now()
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles",
                commandKind = "CreateTile",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns CommandResponse(
            commandId = "cmd-1",
            acceptedAt = now.toString(),
            aggregate = AggregateRef(kind = V1NumericConstants.AggregateKind.PLACEMENT, id = "t-x"),
            revision = 1L,
            result = V1NumericConstants.CommandResult.APPLIED,
            pending = listOf(PendingWork(kind = "schedule_tick", scheduledAt = now.toString()))
        )
        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileCreate(
            buildJsonObject { put("title", JsonPrimitive("X")) },
            userId = "user-1"
        )
        assertNotNull(ack)
        assertEquals("t-x", ack!!.generatedTileId())
    }
}
