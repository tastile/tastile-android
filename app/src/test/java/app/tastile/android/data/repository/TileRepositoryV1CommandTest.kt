package app.tastile.android.data.repository

import app.tastile.android.core.CoreCommandResponse
import app.tastile.android.data.api.CommandResponse
import app.tastile.android.data.api.AggregateRef
import app.tastile.android.data.api.TileContentView
import app.tastile.android.data.api.TileDetailView
import app.tastile.android.data.api.TileVisualView
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1NumericConstants
import app.tastile.android.data.api.V1ListTilesResponse
import app.tastile.android.data.api.TileView
import app.tastile.android.data.api.V1PlacementListItem
import app.tastile.android.data.command.V1CommandDispatcher
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Macro Step 5: every command in TileRepository now routes to v1 through
 * `V1CommandDispatcher`. No more `coreRuntimeService.applyCommand(...)`
 * fallback. These tests assert the new routing and the lookup-failure
 * throws for tile.start / tile.pause / tile.continue / tile.reschedule.
 */
class TileRepositoryV1CommandTest {

    private fun newRepository(
        v1ApiClient: V1ApiClient,
        v1Dispatcher: V1CommandDispatcher,
        userId: String = "user-1",
        idToken: String? = "token-abc"
    ): TileRepository = TileRepository(
        executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
        eventRepository = mockk<EventRepository>(relaxed = true),
        currentUserProvider = mockk<CurrentUserProvider> {
            every { currentUserId() } returns userId
            every { currentIdToken() } returns idToken
        },
        v1ApiClient = v1ApiClient,
        v1CommandDispatcher = v1Dispatcher
    )

    private fun okAck(tileId: String = "t-123"): CoreCommandResponse =
        CoreCommandResponse(
            accepted = true,
            requestId = null,
            commandId = "cmd-1",
            eventIds = emptyList(),
            metadata = buildJsonObject { put("tileId", JsonPrimitive(tileId)) },
            error = null
        )

    private fun okCommandResponse(tileId: String = "t-123"): CommandResponse =
        CommandResponse(
            commandId = "cmd-1",
            acceptedAt = "2026-07-01T00:00:00Z",
            aggregate = AggregateRef(kind = V1NumericConstants.AggregateKind.PLACEMENT, id = tileId),
            revision = 1L,
            result = V1NumericConstants.CommandResult.APPLIED,
            pending = emptyList()
        )

    private fun tileDetail(tileId: String = "t-123", planId: String? = "p-1"): TileDetailView =
        TileDetailView(
            id = tileId,
            kind = V1NumericConstants.TileKind.PLACEMENT,
            ownerId = "user-1",
            revision = 1L,
            title = "Walk dog",
            planId = planId
        )

    private fun placementList(tileId: String = "t-123", placementId: String = "pl-1"): List<V1PlacementListItem> =
        listOf(
            V1PlacementListItem(
                placementId = placementId,
                tileId = tileId,
                planId = "p-1",
                title = "Walk dog"
            )
        )

    private fun cloudTileListResponse(tileId: String = "t-123"): V1ListTilesResponse =
        V1ListTilesResponse(
            tiles = listOf(
                TileView(
                    id = tileId,
                    kind = V1NumericConstants.TileKind.PLACEMENT,
                    ownerId = "user-1",
                    content = TileContentView(title = "Walk dog"),
                    visual = TileVisualView(),
                    revision = 1L
                )
            )
        )

    // --- createTile (Step 4 territory, ensure still wired) -------------

    @Test
    fun createTile_routesToV1Dispatcher() = runTest {
        val apiClient = mockk<V1ApiClient>()
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileCreate(any(), any()) } returns okAck("t-123")
        // The dispatcher is mocked, but `createTile` calls
        // `refreshCloudCacheAfterCommand` only on non-create flows (we
        // removed that for create). Confirm no spurious reads.

        val repository = newRepository(apiClient, dispatcher)
        val payload = buildJsonObject { put("title", JsonPrimitive("Walk dog")) }
        val result = repository.createTile(userId = "user-1", payload = payload)

        coVerify(exactly = 1) { dispatcher.dispatchTileCreate(any(), "user-1") }
        assertEquals("t-123", result.id)
    }

    // --- deleteTile -----------------------------------------------------

    @Test
    fun deleteTile_routesToV1Dispatcher() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileDelete(any()) } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        repository.deleteTile("t-123")

        coVerify(exactly = 1) { dispatcher.dispatchTileDelete("t-123") }
    }

    @Test
    fun deleteTile_throwsWhenDispatcherReturnsNull() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileDelete(any()) } returns null

        val repository = newRepository(apiClient, dispatcher)
        try {
            repository.deleteTile("t-123")
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Cloud command rejected: delete tile", e.message)
        }
    }

    // --- startTile (lookup-failure throw) -------------------------------

    @Test
    fun startTile_routesToV1DispatcherAndRefreshesCloudCache() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listTiles() } returns cloudTileListResponse("t-123")
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileStart("t-123") } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        val tile = repository.startTile("t-123")

        coVerify(exactly = 1) { dispatcher.dispatchTileStart("t-123") }
        coVerify(exactly = 1) { apiClient.listTiles() }
        assertEquals("t-123", tile.id)
    }

    @Test
    fun startTile_propagatesIllegalStateExceptionWhenPlanMissing() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileStart("t-123") } throws
            IllegalStateException("tile.start requires plan_id; v1 backend did not return one for tile t-123")

        val repository = newRepository(apiClient, dispatcher)
        val ex = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repository.startTile("t-123") }
        }
        assertTrue(
            "expected plan_id error message, got: ${ex.message}",
            ex.message!!.contains("plan_id")
        )
    }

    @Test
    fun startTile_throwsWhenDispatcherReturnsNull() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileStart(any()) } returns null

        val repository = newRepository(apiClient, dispatcher)
        try {
            repository.startTile("t-123")
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Cloud command rejected: start tile", e.message)
        }
    }

    // --- pauseTile (lookup-failure throw) --------------------------------

    @Test
    fun pauseTile_routesToV1Dispatcher() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTilePause("t-123") } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        repository.pauseTile("t-123")

        coVerify(exactly = 1) { dispatcher.dispatchTilePause("t-123") }
    }

    @Test
    fun pauseTile_propagatesIllegalStateExceptionWhenNoActiveExecution() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTilePause("t-123") } throws
            IllegalStateException("tile.pause: no active execution for tile t-123")

        val repository = newRepository(apiClient, dispatcher)
        val ex = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repository.pauseTile("t-123") }
        }
        assertTrue(ex.message!!.contains("no active execution"))
    }

    @Test
    fun pauseTile_throwsWhenDispatcherReturnsNull() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTilePause(any()) } returns null

        val repository = newRepository(apiClient, dispatcher)
        try {
            repository.pauseTile("t-123")
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Cloud command rejected: pause tile", e.message)
        }
    }

    // --- continueTile ---------------------------------------------------

    @Test
    fun continueTile_routesToV1Dispatcher() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileContinue("t-123") } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        repository.continueTile("t-123")

        coVerify(exactly = 1) { dispatcher.dispatchTileContinue("t-123") }
    }

    @Test
    fun continueTile_propagatesIllegalStateExceptionWhenNoActiveExecution() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileContinue("t-123") } throws
            IllegalStateException("tile.continue: no active execution for tile t-123")

        val repository = newRepository(apiClient, dispatcher)
        val ex = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repository.continueTile("t-123") }
        }
        assertTrue(ex.message!!.contains("no active execution"))
    }

    // --- rescheduleTile -------------------------------------------------

    @Test
    fun rescheduleTile_routesToV1Dispatcher() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileReschedule(any(), any(), any()) } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        repository.rescheduleTile("t-123", "2026-07-01T09:00:00Z", "2026-07-01T10:00:00Z")

        coVerify(exactly = 1) {
            dispatcher.dispatchTileReschedule("t-123", "2026-07-01T09:00:00Z", "2026-07-01T10:00:00Z")
        }
    }

    @Test
    fun rescheduleTile_propagatesIllegalStateExceptionWhenNoPlacement() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileReschedule(any(), any(), any()) } throws
            IllegalStateException("tile.reschedule: no placement for tile t-123")

        val repository = newRepository(apiClient, dispatcher)
        val ex = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.rescheduleTile("t-123", "2026-07-01T09:00:00Z", "2026-07-01T10:00:00Z")
            }
        }
        assertTrue(ex.message!!.contains("no placement"))
    }

    // --- deferTile ------------------------------------------------------

    @Test
    fun deferTile_routesToV1Dispatcher() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileDefer(any(), any(), any()) } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        repository.deferTile("t-123", reason = "later", minutes = 15)

        coVerify(exactly = 1) { dispatcher.dispatchTileDefer("t-123", "later", 15) }
    }

    @Test
    fun deferTile_throwsWhenDispatcherReturnsNull() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileDefer(any(), any(), any()) } returns null

        val repository = newRepository(apiClient, dispatcher)
        try {
            repository.deferTile("t-123", reason = null, minutes = 15)
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Cloud command rejected: defer tile", e.message)
        }
    }

    // --- updateTile -----------------------------------------------------

    @Test
    fun updateTile_routesToV1Dispatcher() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileUpdate(any(), any()) } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        val payload = buildJsonObject { put("title", JsonPrimitive("Renamed")) }
        repository.updateTile("t-123", payload)

        coVerify(exactly = 1) { dispatcher.dispatchTileUpdate("t-123", any()) }
    }

    // --- attachMemo -----------------------------------------------------

    @Test
    fun attachMemo_routesToV1Dispatcher() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchMemoAttach(any(), any()) } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        repository.attachMemo(tileId = "t-123", text = "remember", memoKind = null)

        coVerify(exactly = 1) { dispatcher.dispatchMemoAttach("t-123", "remember") }
    }

    @Test
    fun attachMemo_silentlyReturnsWhenTileIdNullAndDispatcherReturnsNull() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchMemoAttach(any(), any()) } returns null

        val repository = newRepository(apiClient, dispatcher)
        // No tile_id → silently no-op (matches v0 contract for "memo with
        // no associated tile")
        repository.attachMemo(tileId = null, text = "remember", memoKind = null)
    }

    // --- break.start / break.end (UnsupportedOperationException) -------

    @Test
    fun startBreak_throwsUnsupportedOperationException() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>(relaxed = true)

        val repository = newRepository(apiClient, dispatcher)
        val ex = assertThrows(UnsupportedOperationException::class.java) {
            kotlinx.coroutines.runBlocking { repository.startBreak(breakMin = 5) }
        }
        assertTrue(ex.message!!.contains("v1"))
    }

    @Test
    fun endBreak_throwsUnsupportedOperationException() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>(relaxed = true)

        val repository = newRepository(apiClient, dispatcher)
        val ex = assertThrows(UnsupportedOperationException::class.java) {
            kotlinx.coroutines.runBlocking { repository.endBreak() }
        }
        assertTrue(ex.message!!.contains("v1"))
    }

    // --- extendTile (no tile_id, must throw) ----------------------------

    @Test
    fun extendTile_throwsUnsupportedOperationException() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>(relaxed = true)

        val repository = newRepository(apiClient, dispatcher)
        val ex = assertThrows(UnsupportedOperationException::class.java) {
            kotlinx.coroutines.runBlocking { repository.extendTile(extendMin = 5) }
        }
        assertTrue(ex.message!!.contains("tile_id"))
    }

    // --- prompt.request -------------------------------------------------

    @Test
    fun requestPrompt_routesToV1DispatcherAndReturnsTrue() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchPromptRequest("t-123") } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        val ok = repository.requestPrompt("t-123")

        coVerify(exactly = 1) { dispatcher.dispatchPromptRequest("t-123") }
        assertEquals(true, ok)
    }

    @Test
    fun requestPrompt_returnsFalseWhenDispatcherReturnsNull() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchPromptRequest(any()) } returns null

        val repository = newRepository(apiClient, dispatcher)
        val ok = repository.requestPrompt("t-123")
        assertEquals(false, ok)
    }

    // --- prompt.respond_startup_recovery --------------------------------

    @Test
    fun respondStartupRecoveryPrompt_routesToV1DispatcherAndReturnsTrue() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery {
            dispatcher.dispatchStartupRecoveryPrompt(any(), any(), any(), any())
        } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        val ok = repository.respondStartupRecoveryPrompt(
            promptId = "p-1", tileId = "t-123", actionId = "retry"
        )

        coVerify(exactly = 1) {
            dispatcher.dispatchStartupRecoveryPrompt("p-1", "t-123", "retry", null)
        }
        assertEquals(true, ok)
    }

    @Test
    fun respondStartupRecoveryPrompt_returnsFalseWhenDispatcherReturnsNull() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery {
            dispatcher.dispatchStartupRecoveryPrompt(any(), any(), any(), any())
        } returns null

        val repository = newRepository(apiClient, dispatcher)
        val ok = repository.respondStartupRecoveryPrompt(
            promptId = "p-1", tileId = "t-123", actionId = "retry"
        )
        assertEquals(false, ok)
    }

    // --- refreshCloudCacheAfterCommand behavior -------------------------

    @Test
    fun deleteTile_refreshesLatestCloudTilesAfterV1Success() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listTiles() } returns cloudTileListResponse("t-123")
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileDelete(any()) } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        repository.deleteTile("t-123")

        coVerify(exactly = 1) { apiClient.listTiles() }
    }

    @Test
    fun startTile_refreshesLatestCloudTilesAfterV1Success() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listTiles() } returns cloudTileListResponse("t-123")
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileStart(any()) } returns okAck("t-123")

        val repository = newRepository(apiClient, dispatcher)
        repository.startTile("t-123")

        coVerify(exactly = 1) { apiClient.listTiles() }
    }
}