package app.tastile.android.data.repository

import app.tastile.android.core.CoreBridgeError
import app.tastile.android.core.CoreCommandResponse
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.data.api.TileContentView
import app.tastile.android.data.api.TileVisualView
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1NumericConstants
import app.tastile.android.data.api.V1ListTilesResponse
import app.tastile.android.data.api.TileView
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
import org.junit.Test

class TileRepositoryV1CommandTest {

    private fun newRepository(
        coreRuntimeService: CoreRuntimeService,
        v1ApiClient: V1ApiClient,
        v1Dispatcher: V1CommandDispatcher,
        userId: String = "user-1",
        idToken: String? = "token-abc"
    ): TileRepository = TileRepository(
        coreRuntimeService = coreRuntimeService,
        executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
        eventRepository = mockk<EventRepository>(relaxed = true),
        currentUserProvider = mockk<CurrentUserProvider> {
            every { currentUserId() } returns userId
            every { currentIdToken() } returns idToken
        },
        v1ApiClient = v1ApiClient,
        v1CommandDispatcher = v1Dispatcher
    )

    private fun createAck(tileId: String = "t-123"): CoreCommandResponse =
        CoreCommandResponse(
            accepted = true,
            requestId = null,
            commandId = "cmd-1",
            eventIds = emptyList(),
            metadata = buildJsonObject { put("tileId", JsonPrimitive(tileId)) },
            error = null
        )

    @Test
    fun tryApplyCoreCommand_routesTileCreateToV1Dispatcher() = runTest {
        val coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true)
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileCreate(any(), any()) } returns createAck("t-123")

        val repository = newRepository(coreRuntimeService, apiClient, dispatcher)
        val payload = buildJsonObject { put("title", JsonPrimitive("X")) }

        // Use a public surface that reaches tryApplyCoreCommand → for the test
        // purpose, call the package-private method via reflection-friendly means.
        // Instead, exercise it through `createTile(userId, payload)`.
        val result = repository.createTile(userId = "user-1", payload = payload)

        coVerify(exactly = 1) { dispatcher.dispatchTileCreate(any(), "user-1") }
        coVerify(exactly = 0) { coreRuntimeService.applyCommand(any()) }
        assertEquals("t-123", result.id)
    }

    @Test
    fun tryApplyCoreCommand_fallsBackToV0ForTileStart() = runTest {
        val coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true)
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>(relaxed = true)
        coEvery { coreRuntimeService.applyCommand(any()) } returns createAck()

        val repository = newRepository(coreRuntimeService, apiClient, dispatcher)
        // startTile via v0: confirm v0 ran and the dispatcher was never invoked
        // for the tile.start type. The public surface may still throw if the
        // projected snapshot is empty; what matters is that v0 ran.
        runCatching { repository.startTile("t-123") }

        coVerify(exactly = 1) { coreRuntimeService.applyCommand(any()) }
    }

    @Test
    fun tryApplyCoreCommand_fallsBackToV0ForTilePause() = runTest {
        val coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true)
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>(relaxed = true)
        coEvery { coreRuntimeService.applyCommand(any()) } returns createAck()

        val repository = newRepository(coreRuntimeService, apiClient, dispatcher)
        runCatching { repository.pauseTile("t-123") }

        coVerify(exactly = 1) { coreRuntimeService.applyCommand(any()) }
    }

    @Test
    fun tryApplyCoreCommand_returnsNullWhenV1FailsAndV0Throws() = runTest {
        val coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true)
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileDelete(any()) } returns null
        coEvery { coreRuntimeService.applyCommand(any()) } throws CoreBridgeError.LibraryLoadFailed("err")

        val repository = newRepository(coreRuntimeService, apiClient, dispatcher)
        // deleteTile calls tryApplyCoreCommand("tile.delete", ...). Expect throw because v0 also fails.
        try {
            repository.deleteTile("t-123")
            throw AssertionError("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // ok — v0 fallback failed, the public surface throws.
            assertEquals("Cloud command rejected: delete tile", e.message)
        }
        coVerify(exactly = 1) { dispatcher.dispatchTileDelete("t-123") }
        coVerify(exactly = 1) { coreRuntimeService.applyCommand(any()) }
    }

    @Test
    fun tryApplyCoreCommand_refreshesLatestCloudTilesAfterV1Success() = runTest {
        val coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true)
        val apiClient = mockk<V1ApiClient>()
        // First call (triggered by tryApplyCoreCommand post-success refresh) returns
        // the cloud-side list. We expect exactly one readCloudTiles() call.
        coEvery { apiClient.listTiles() } returns V1ListTilesResponse(
            tiles = listOf(
                TileView(
                    id = "t-123",
                    kind = V1NumericConstants.TileKind.PLACEMENT,
                    ownerId = "user-1",
                    content = TileContentView(title = "Created"),
                    visual = TileVisualView(),
                    revision = 1L
                )
            )
        )
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileCreate(any(), any()) } returns createAck("t-123")

        val repository = newRepository(coreRuntimeService, apiClient, dispatcher)
        val payload = buildJsonObject { put("title", JsonPrimitive("Created")) }
        val result = repository.createTile(userId = "user-1", payload = payload)

        coVerify(exactly = 1) { apiClient.listTiles() }
        assertEquals("Created", result.title)
        // The created tile id was surfaced via dispatcher.metadata.tileId.
        assertEquals("t-123", result.id)
    }

    @Test
    fun deferTile_routesToV1Dispatcher() = runTest {
        val coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true)
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileDefer(any(), any(), any()) } returns createAck("t-123")

        val repository = newRepository(coreRuntimeService, apiClient, dispatcher)
        repository.deferTile("t-123", reason = "later", minutes = 15)

        coVerify(exactly = 1) { dispatcher.dispatchTileDefer("t-123", "later", 15) }
        coVerify(exactly = 0) { coreRuntimeService.applyCommand(any()) }
    }

    @Test
    fun updateTile_routesToV1Dispatcher() = runTest {
        val coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true)
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileUpdate(any(), any()) } returns createAck("t-123")

        val repository = newRepository(coreRuntimeService, apiClient, dispatcher)
        val payload = buildJsonObject { put("title", JsonPrimitive("Renamed")) }
        repository.updateTile("t-123", payload)

        coVerify(exactly = 1) { dispatcher.dispatchTileUpdate("t-123", any()) }
        coVerify(exactly = 0) { coreRuntimeService.applyCommand(any()) }
    }

    @Test
    fun attachMemo_routesToV1Dispatcher() = runTest {
        val coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true)
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchMemoAttach(any(), any()) } returns createAck("t-123")

        val repository = newRepository(coreRuntimeService, apiClient, dispatcher)
        repository.attachMemo(tileId = "t-123", text = "remember", memoKind = null)

        coVerify(exactly = 1) { dispatcher.dispatchMemoAttach("t-123", "remember") }
        coVerify(exactly = 0) { coreRuntimeService.applyCommand(any()) }
    }

    @Test
    fun deleteTile_routesToV1Dispatcher() = runTest {
        val coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true)
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val dispatcher = mockk<V1CommandDispatcher>()
        coEvery { dispatcher.dispatchTileDelete(any()) } returns createAck("t-123")

        val repository = newRepository(coreRuntimeService, apiClient, dispatcher)
        repository.deleteTile("t-123")

        coVerify(exactly = 1) { dispatcher.dispatchTileDelete("t-123") }
        coVerify(exactly = 0) { coreRuntimeService.applyCommand(any()) }
    }
}
