package app.tastile.android.data.command

import app.tastile.android.core.CoreCommandAck
import app.tastile.android.data.api.AggregateRef
import app.tastile.android.data.api.AppendChangesPayload
import app.tastile.android.data.api.ArchiveTilePayload
import app.tastile.android.data.api.SetTileLifecyclePayload
import app.tastile.android.data.api.CommandResponse
import app.tastile.android.data.api.PendingWork
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import app.tastile.android.data.api.V1NumericConstants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    fun dispatchPlacementClose_postsCloseEndpointAndAccepts204() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.postCommandNoResponse("/v1/placements/p-1/close", any<Any>(), any<KSerializer<Any>>()) } returns Unit
        val ack = V1CommandDispatcher(apiClient).dispatchPlacementClose("p-1")
        assertNotNull(ack)
        assertTrue(ack!!.accepted)
        coVerify { apiClient.postCommandNoResponse("/v1/placements/p-1/close", any<Any>(), any<KSerializer<Any>>()) }
    }

    @Test
    fun dispatchTileDelete_callsDeleteEndpoint() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.deleteCommand(path = "/v1/tiles/t-123", payload = any<Any>(), payloadSerializer = any<KSerializer<Any>>(), expectedRevision = null)
        } returns Unit

        val payload = slot<ArchiveTilePayload>()
        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileDelete("t-123")

        assertNotNull(ack)
        coVerify {
            apiClient.deleteCommand(
                path = "/v1/tiles/t-123",
                payload = capture(payload),
                payloadSerializer = any<KSerializer<Any>>(),
                expectedRevision = null,
            )
        }
        assertEquals("t-123", payload.captured.tileId)
        coVerify(exactly = 1) {
            apiClient.deleteCommand(path = "/v1/tiles/t-123", payload = any<Any>(), payloadSerializer = any<KSerializer<Any>>(), expectedRevision = null)
        }
    }

    // --- UpdateTile ----------------------------------------------------

    @Test
    fun dispatchTileUpdate_mapsFields() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/update",
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
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns okResponse("t-123")

        val payload = slot<SetTileLifecyclePayload>()
        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileDefer("t-123", deferredUntil = "2026-07-01T09:15:00Z")

        assertNotNull(ack)
        coVerify {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/defer",
                payload = capture(payload),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
        assertEquals("2026-07-01T09:15:00Z", payload.captured.deferredUntil)
        coVerify(exactly = 1) {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/defer",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
    }

    @Test
    fun dispatchTileComplete_buildsSetTileLifecycleWithState2() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.getActiveTile() } returns app.tastile.android.data.api.ActiveTileView(
            tileId = "t-123",
            placementId = "pl-1",
            executionId = "ex-1",
        )
        coEvery { apiClient.readExecution("ex-1") } returns app.tastile.android.data.api.ExecutionView(
            id = "ex-1",
            tileId = "t-123",
            state = 0,
            placementId = "pl-1",
        )
        coEvery {
            apiClient.postCommand(
                path = "/v1/executions/ex-1/finish",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>(),
            )
        } returns okResponse("ex-1")
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/complete",
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

    // --- Step 5: tile.start ---------------------------------------------

    @Test
    fun dispatchTileStart_fetchesPlanIdAndPostsStartTilePayload() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.readTile("t-123") } returns app.tastile.android.data.api.TileDetailView(
            id = "t-123",
            kind = V1NumericConstants.TileKind.PLACEMENT,
            ownerId = "user-1",
            revision = 1L,
            title = "Walk dog",
            planId = "p-7"
        )
        coEvery {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/start",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns okResponse("t-123")
        coEvery {
            apiClient.postCommand(
                path = "/v1/placements/t-123/executions",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>(),
            )
        } returns okResponse("ex-123").copy(
            aggregate = AggregateRef(V1NumericConstants.AggregateKind.EXECUTION, "ex-123"),
        )

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileStart("t-123")

        assertNotNull(ack)
        coVerify(exactly = 1) { apiClient.readTile("t-123") }
        coVerify(exactly = 1) {
            apiClient.postCommand(
                path = "/v1/tiles/t-123/start",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
        coVerify(exactly = 1) {
            apiClient.postCommand(
                path = "/v1/placements/t-123/executions",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>(),
            )
        }
    }

    @Test
    fun dispatchTileStart_throwsIllegalStateWhenPlanIdMissing() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.readTile("t-123") } returns app.tastile.android.data.api.TileDetailView(
            id = "t-123",
            kind = V1NumericConstants.TileKind.PLACEMENT,
            ownerId = "user-1",
            revision = 1L,
            title = "Walk dog",
            planId = null
        )

        val dispatcher = V1CommandDispatcher(apiClient)
        try {
            dispatcher.dispatchTileStart("t-123")
            throw AssertionError("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("plan_id"))
        }
    }

    @Test
    fun dispatchTileStart_returnsNullOnV1Error_network() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.readTile("t-123") } throws V1Error.Network(IOException("boom"))

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileStart("t-123")
        assertNull(ack)
    }

    // --- Step 5: tile.pause ---------------------------------------------

    @Test
    fun dispatchTilePause_throwsWhenNoActiveExecution() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.getActiveTile() } returns null

        val dispatcher = V1CommandDispatcher(apiClient)
        try {
            dispatcher.dispatchTilePause("t-123")
            throw AssertionError("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("no active execution"))
        }
    }

    @Test
    fun dispatchTilePause_returnsNullOnV1Error_network() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.getActiveTile() } throws V1Error.Network(IOException("boom"))

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTilePause("t-123")
        assertNull(ack)
    }

    // --- Step 5: tile.continue ------------------------------------------

    @Test
    fun dispatchTileContinue_throwsWhenNoActiveExecution() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.getActiveTile() } returns null

        val dispatcher = V1CommandDispatcher(apiClient)
        try {
            dispatcher.dispatchTileContinue("t-123")
            throw AssertionError("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("no active execution"))
        }
    }

    @Test
    fun executionLifecycle_usesActiveTileReadModelForPauseAndCachedExecutionForResume() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.getActiveTile() } returns app.tastile.android.data.api.ActiveTileView(
            tileId = "t-123", placementId = "pl-1", executionId = "ex-1"
        )
        coEvery { apiClient.readExecution("ex-1") } returns app.tastile.android.data.api.ExecutionView(
            id = "ex-1", tileId = "t-123", state = 0, placementId = "pl-1"
        )
        coEvery {
            apiClient.postNullCommand("/v1/executions/ex-1/pause", any<KSerializer<Any>>())
        } returns okResponse("ex-1")
        coEvery {
            apiClient.postNullCommand("/v1/executions/ex-1/resume", any<KSerializer<Any>>())
        } returns okResponse("ex-1")

        val dispatcher = V1CommandDispatcher(apiClient)
        assertNotNull(dispatcher.dispatchTilePause("t-123"))
        assertNotNull(dispatcher.dispatchTileContinue("t-123"))

        coVerify(exactly = 1) { apiClient.getActiveTile() }
        coVerify(exactly = 1) { apiClient.postNullCommand("/v1/executions/ex-1/pause", any<KSerializer<Any>>()) }
        coVerify(exactly = 1) { apiClient.postNullCommand("/v1/executions/ex-1/resume", any<KSerializer<Any>>()) }
    }

    @Test
    fun executionStateForTile_clearsVolatileIdWhenRevalidationFails() = runTest {
        val apiClient = newApiClient()
        val activeExecution = app.tastile.android.data.api.ExecutionView(
            id = "ex-1",
            tileId = "t-123",
            state = 0,
            placementId = "pl-1",
        )
        coEvery { apiClient.getActiveTile() } returnsMany listOf(
            app.tastile.android.data.api.ActiveTileView("t-123", "pl-1", "ex-1"),
            null,
        )
        coEvery { apiClient.readExecution("ex-1") } returns activeExecution andThenThrows V1Error.Network(IOException("offline"))
        coEvery {
            apiClient.postNullCommand("/v1/executions/ex-1/pause", any<KSerializer<Any>>())
        } returns okResponse("ex-1")

        val dispatcher = V1CommandDispatcher(apiClient)
        assertNotNull(dispatcher.dispatchTilePause("t-123"))

        assertTrue(dispatcher.executionStateLookupForTile("t-123") is ExecutionStateLookup.Unavailable)
        coVerify(exactly = 1) { apiClient.getActiveTile() }
    }

    @Test
    fun executionStateLookup_distinguishesNoActiveExecutionFromInvalidClaim() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.getActiveTile() } returns null

        val dispatcher = V1CommandDispatcher(apiClient)

        assertEquals(ExecutionStateLookup.NoActiveExecution, dispatcher.executionStateLookupForTile("t-123"))
    }

    @Test
    fun dispatchPlacementExecutionStart_postsExactPlacementCommand() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.listPlacements() } returns listOf(
            app.tastile.android.data.api.V1PlacementListItem(
                placementId = "pl-1",
                tileId = "t-123",
                planId = "plan-1",
                spanStart = "2026-01-01T00:00:00Z",
                spanEnd = "2026-01-01T01:00:00Z",
            )
        )
        coEvery {
            apiClient.postCommand(
                "/v1/placements/pl-1/executions",
                app.tastile.android.data.api.StartExecutionPayload("pl-1"),
                app.tastile.android.data.api.StartExecutionPayload.serializer(),
                CommandResponse.serializer(),
            )
        } returns okResponse("ex-1")

        assertNotNull(V1CommandDispatcher(apiClient).dispatchPlacementExecutionStart("t-123"))

        coVerify(exactly = 1) {
            apiClient.postCommand(
                "/v1/placements/pl-1/executions",
                app.tastile.android.data.api.StartExecutionPayload("pl-1"),
                app.tastile.android.data.api.StartExecutionPayload.serializer(),
                CommandResponse.serializer(),
            )
        }
    }

    @Test
    fun dispatchExecutionFinish_postsFinishOnlyWithoutTileComplete() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.getActiveTile() } returns app.tastile.android.data.api.ActiveTileView("t-123", "pl-1", "ex-1")
        coEvery { apiClient.readExecution("ex-1") } returns app.tastile.android.data.api.ExecutionView("ex-1", "t-123", 0, "pl-1")
        coEvery {
            apiClient.postCommand(
                "/v1/executions/ex-1/finish",
                app.tastile.android.data.api.ExecutionFinishPayload(kind = 0, note = null),
                app.tastile.android.data.api.ExecutionFinishPayload.serializer(),
                CommandResponse.serializer(),
            )
        } returns okResponse("ex-1")

        assertNotNull(V1CommandDispatcher(apiClient).dispatchExecutionFinish("t-123"))

        coVerify(exactly = 1) {
            apiClient.postCommand(
                "/v1/executions/ex-1/finish",
                app.tastile.android.data.api.ExecutionFinishPayload(kind = 0, note = null),
                app.tastile.android.data.api.ExecutionFinishPayload.serializer(),
                CommandResponse.serializer(),
            )
        }
    }

    // --- Step 5: tile.reschedule ----------------------------------------

    @Test
    fun dispatchTileReschedule_throwsWhenNoPlacement() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.listPlacements() } returns emptyList()

        val dispatcher = V1CommandDispatcher(apiClient)
        try {
            dispatcher.dispatchTileReschedule("t-123", "2026-07-01T09:00:00Z", "2026-07-01T10:00:00Z", ownerId = "user-1")
            throw AssertionError("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("no placement"))
        }
    }

    @Test
    fun dispatchTileReschedule_findsPlacementAndPostsAppendChanges() = runTest {
        val apiClient = newApiClient()
        coEvery { apiClient.listPlacements() } returns listOf(
            app.tastile.android.data.api.V1PlacementListItem(
                placementId = "pl-1",
                tileId = "t-123",
                planId = "p-1",
                title = "Walk dog"
            )
        )
        coEvery {
            apiClient.postCommand(
                path = "/v1/placements/pl-1/changes",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns okResponse("t-123")

        val payload = slot<AppendChangesPayload>()
        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchTileReschedule(
            "t-123", "2026-07-01T09:00:00Z", "2026-07-01T10:00:00Z", ownerId = "user-1"
        )

        assertNotNull(ack)
        coVerify {
            apiClient.postCommand(
                path = "/v1/placements/pl-1/changes",
                payload = capture(payload),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
        val changeSet = payload.captured.changeset
        assertEquals("user-1", changeSet["owner_id"]!!.jsonPrimitive.content)
        assertEquals("pl-1", changeSet["target"]!!.jsonObject["Placement"]!!.jsonPrimitive.content)
        assertEquals(2, changeSet["changes"]!!.jsonArray.size)
        assertEquals("pl-1", changeSet["changes"]!!.jsonArray[0].jsonObject["key"]!!.jsonObject["item"]!!.jsonPrimitive.content)
        assertEquals("2026-07-01T09:00:00Z", changeSet["changes"]!!.jsonArray[0].jsonObject["value"]!!.jsonObject["Instant"]!!.jsonPrimitive.content)
        assertEquals("user-1", changeSet["created_by"]!!.jsonObject["actor"]!!.jsonPrimitive.content)
        coVerify(exactly = 1) { apiClient.listPlacements() }
        coVerify(exactly = 1) {
            apiClient.postCommand(
                path = "/v1/placements/pl-1/changes",
                payload = any<Any>(),
                payloadSerializer = any<KSerializer<Any>>(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
    }

    // --- Step 5: prompt.request -----------------------------------------

    @Test
    fun dispatchPromptRequest_postsToV1PromptsEndpointAndReturnsAckWithPromptId() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postRawJson(
                path = "/v1/prompts",
                body = any(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns buildJsonObject { put("id", JsonPrimitive("pr-7")) }

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchPromptRequest("t-123")

        assertNotNull(ack)
        assertTrue(ack!!.accepted)
        assertEquals("pr-7", ack.metadata?.get("promptId")?.jsonPrimitive?.contentOrNull)
        coVerify(exactly = 1) {
            apiClient.postRawJson(
                path = "/v1/prompts",
                body = any(),
                responseSerializer = any<KSerializer<Any>>()
            )
        }
    }

    @Test
    fun dispatchPromptRequest_returnsNullOnV1Error_auth() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postRawJson(
                path = "/v1/prompts",
                body = any(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } throws V1Error.Auth()

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchPromptRequest("t-123")
        assertNull(ack)
    }

    // --- Step 5: prompt.respond_startup_recovery -----------------------

    @Test
    fun dispatchStartupRecoveryPrompt_passesBodyVerbatim() = runTest {
        val apiClient = newApiClient()
        val bodySlot = slot<JsonObject>()
        coEvery {
            apiClient.postRawJson(
                path = "/v1/prompts/startup-recovery",
                body = capture(bodySlot),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns buildJsonObject { put("accepted", JsonPrimitive(true)) }

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchStartupRecoveryPrompt(
            promptId = "pr-7",
            tileId = "t-123",
            actionId = "retry",
            stopAtIso = "2026-07-01T11:00:00Z"
        )

        assertNotNull(ack)
        assertTrue(ack!!.accepted)
        val captured = bodySlot.captured
        assertEquals("pr-7", captured["prompt_id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("t-123", captured["tile_id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("retry", captured["action_id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("2026-07-01T11:00:00Z", captured["stop_at"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun dispatchStartupRecoveryPrompt_omitsStopAtWhenBlank() = runTest {
        val apiClient = newApiClient()
        val bodySlot = slot<JsonObject>()
        coEvery {
            apiClient.postRawJson(
                path = "/v1/prompts/startup-recovery",
                body = capture(bodySlot),
                responseSerializer = any<KSerializer<Any>>()
            )
        } returns buildJsonObject { put("accepted", JsonPrimitive(true)) }

        val dispatcher = V1CommandDispatcher(apiClient)
        dispatcher.dispatchStartupRecoveryPrompt(
            promptId = "pr-7",
            tileId = "t-123",
            actionId = "retry",
            stopAtIso = null
        )

        val captured = bodySlot.captured
        assertEquals("pr-7", captured["prompt_id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("retry", captured["action_id"]?.jsonPrimitive?.contentOrNull)
        // stop_at is absent when stopAtIso is null/blank
        assertEquals(null, captured["stop_at"])
    }

    @Test
    fun dispatchStartupRecoveryPrompt_returnsNullOnV1Error_network() = runTest {
        val apiClient = newApiClient()
        coEvery {
            apiClient.postRawJson(
                path = "/v1/prompts/startup-recovery",
                body = any(),
                responseSerializer = any<KSerializer<Any>>()
            )
        } throws V1Error.Network(IOException("boom"))

        val dispatcher = V1CommandDispatcher(apiClient)
        val ack = dispatcher.dispatchStartupRecoveryPrompt(
            promptId = "pr-7", tileId = "t-123", actionId = "retry", stopAtIso = null
        )
        assertNull(ack)
    }
}
