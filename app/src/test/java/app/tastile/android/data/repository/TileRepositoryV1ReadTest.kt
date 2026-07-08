package app.tastile.android.data.repository

import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import app.tastile.android.data.api.V1ListTilesResponse
import app.tastile.android.data.api.V1NumericConstants
import app.tastile.android.data.api.TileListView
import app.tastile.android.data.command.V1CommandDispatcher
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileRepositoryV1ReadTest {

    private fun newRepository(
        apiClient: V1ApiClient,
        userId: String = "user-1",
        idToken: String? = "token-abc"
    ): TileRepository = TileRepository(
        executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
        eventRepository = mockk<EventRepository>(relaxed = true),
        currentUserProvider = mockk<CurrentUserProvider> {
            every { currentUserId() } returns userId
            every { currentIdToken() } returns idToken
        },
        v1ApiClient = apiClient,
        v1CommandDispatcher = mockk<V1CommandDispatcher>(relaxed = true)
    )

    @Test
    fun getTiles_callsV1ApiClientAndMapsExecutionKindToStarted() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.getTiles(any()) } returns V1ListTilesResponse(
            tiles = listOf(
                TileListView(
                    id = "t-exec",
                    title = "In flight",
                    lifecycle = V1NumericConstants.LifecycleCode.STARTED,
                ),
                TileListView(
                    id = "t-place",
                    title = "Scheduled",
                    lifecycle = V1NumericConstants.LifecycleCode.READY,
                )
            )
        )
        val repository = newRepository(apiClient)

        val response = repository.getTiles()

        coVerify(exactly = 1) { apiClient.getTiles(TileFilter.DEFAULT) }
        assertEquals(2, response.tiles.size)
        val execTile = response.tiles.firstOrNull { it.id == "t-exec" }
        assertEquals(TileLifecycle.STARTED.value, execTile?.lifecycle)
        val placeTile = response.tiles.firstOrNull { it.id == "t-place" }
        assertEquals(TileLifecycle.READY.value, placeTile?.lifecycle)
        // getTiles(filter) preserves the v1 diagnostic on success.
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1 "))
    }

    @Test
    fun getTiles_returnsEmptyWhenV1Throws_authError() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.getTiles(any()) } throws V1Error.Auth()
        val repository = newRepository(apiClient)

        val response = repository.getTiles()

        assertTrue(response.tiles.isEmpty())
        assertNull(response.nextActionableTileId)
        assertNull(response.nextActionableStartAt)
        // readCloudTiles failed -> v1_unavailable diagnostic is preserved.
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1_unavailable "))
    }

    @Test
    fun getTiles_returnsEmptyWhenV1Throws_networkError() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.getTiles(any()) } throws V1Error.Network(RuntimeException("boom"))
        val repository = newRepository(apiClient)

        val response = repository.getTiles()

        assertTrue(response.tiles.isEmpty())
        // Network error -> v1_unavailable diagnostic is preserved.
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1_unavailable "))
    }

    @Test
    fun getTiles_returnsEmptyWhenIdTokenMissing_andDoesNotCallV1() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val repository = newRepository(apiClient, idToken = null)

        val response = repository.getTiles()

        assertTrue(response.tiles.isEmpty())
        coVerify(exactly = 0) { apiClient.getTiles(any()) }
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1_skipped "))
    }

    @Test
    fun getTiles_threadsNextActionableFieldsIntoResponse() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.getTiles(any()) } returns V1ListTilesResponse(
            tiles = listOf(
                TileListView(
                    id = "t-1",
                    title = "Next",
                    lifecycle = V1NumericConstants.LifecycleCode.READY,
                )
            ),
            nextActionableTileId = "t-1",
            nextActionableStartAt = "2026-07-08T09:00:00Z"
        )
        val repository = newRepository(apiClient)

        val response = repository.getTiles()

        assertEquals("t-1", response.nextActionableTileId)
        assertEquals("2026-07-08T09:00:00Z", response.nextActionableStartAt)
        assertTrue(repository.latestReadDiagnostics().contains("next_tile=t-1"))
        assertTrue(repository.latestReadDiagnostics().contains("next_at=2026-07-08T09:00:00Z"))
    }
}