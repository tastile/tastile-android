package app.tastile.android.data.repository

import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.data.api.TileContentView
import app.tastile.android.data.api.TileVisualView
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import app.tastile.android.data.api.V1ListTilesResponse
import app.tastile.android.data.api.V1NumericConstants
import app.tastile.android.data.api.TileView
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileRepositoryV1ReadTest {

    @Test
    fun getTiles_callsV1ApiClientAndMapsExecutionKindToStarted() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listTiles() } returns V1ListTilesResponse(
            tiles = listOf(
                TileView(
                    id = "t-exec",
                    kind = V1NumericConstants.TileKind.EXECUTION,
                    ownerId = "user-1",
                    content = TileContentView(title = "In flight"),
                    visual = TileVisualView(),
                    revision = 1L
                ),
                TileView(
                    id = "t-place",
                    kind = V1NumericConstants.TileKind.PLACEMENT,
                    ownerId = "user-1",
                    content = TileContentView(title = "Scheduled"),
                    visual = TileVisualView(),
                    revision = 1L
                )
            )
        )
        val repository = TileRepository(
            coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true),
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
                every { currentIdToken() } returns "token-abc"
            },
            v1ApiClient = apiClient
        )

        val tiles = repository.getTiles(userId = "user-1")

        coVerify(exactly = 1) { apiClient.listTiles() }
        assertEquals(2, tiles.size)
        val execTile = tiles.firstOrNull { it.id == "t-exec" }
        assertEquals(TileLifecycle.STARTED.value, execTile?.lifecycle)
        val placeTile = tiles.firstOrNull { it.id == "t-place" }
        assertEquals(TileLifecycle.READY.value, placeTile?.lifecycle)
        // getTiles(userId) preserves the v1 diagnostic on success.
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1 "))
    }

    @Test
    fun getTiles_returnsEmptyWhenV1Throws_authError() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listTiles() } throws V1Error.Auth()
        val repository = TileRepository(
            coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true),
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
                every { currentIdToken() } returns "token-abc"
            },
            v1ApiClient = apiClient
        )

        val tiles = repository.getTiles(userId = "user-1")

        assertTrue(tiles.isEmpty())
        // readCloudTiles failed -> v1_unavailable diagnostic is preserved (no snapshot fallback).
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1_unavailable "))
    }

    @Test
    fun getTiles_returnsEmptyWhenV1Throws_networkError() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.listTiles() } throws V1Error.Network(RuntimeException("boom"))
        val repository = TileRepository(
            coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true),
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
                every { currentIdToken() } returns "token-abc"
            },
            v1ApiClient = apiClient
        )

        val tiles = repository.getTiles(userId = "user-1")

        assertTrue(tiles.isEmpty())
        // Network error -> v1_unavailable diagnostic is preserved (no snapshot fallback).
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1_unavailable "))
    }

    @Test
    fun getTiles_returnsEmptyWhenIdTokenMissing_andDoesNotCallV1() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val repository = TileRepository(
            coreRuntimeService = mockk<CoreRuntimeService>(relaxed = true),
            executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
            eventRepository = mockk<EventRepository>(relaxed = true),
            currentUserProvider = mockk<CurrentUserProvider> {
                every { currentUserId() } returns "user-1"
                every { currentIdToken() } returns null
            },
            v1ApiClient = apiClient
        )

        val tiles = repository.getTiles(userId = "user-1")

        assertTrue(tiles.isEmpty())
        coVerify(exactly = 0) { apiClient.listTiles() }
    }
}