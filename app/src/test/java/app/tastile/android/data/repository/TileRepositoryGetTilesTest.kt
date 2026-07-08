package app.tastile.android.data.repository

import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1ListTilesResponse
import app.tastile.android.data.api.V1NumericConstants
import app.tastile.android.data.api.TileListView
import app.tastile.android.data.command.V1CommandDispatcher
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

/**
 * C3 read-path coverage: `TileRepository.getTiles(filter)` must pass the
 * filter through to `V1ApiClient.getTiles(filter)` and surface the
 * `next_actionable_*` fields into [TilesResponse].
 */
class TileRepositoryGetTilesTest {

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
    fun getTiles_passesFilterToApiClient() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.getTiles(any()) } returns V1ListTilesResponse(
            tiles = listOf(
                TileListView(id = "t-1", title = "Walk dog", lifecycle = V1NumericConstants.LifecycleCode.READY)
            )
        )
        val repository = newRepository(apiClient)
        val filter = TileFilter(
            lifecycle = "started",
            limit = 50,
            search = "meeting",
            excludeFuture = true,
            range = "today",
            granularity = "min_5m",
            ownerIds = listOf("u1", "u2"),
        )

        val response = repository.getTiles(filter)

        coVerify(exactly = 1) { apiClient.getTiles(filter) }
        assertEquals(1, response.tiles.size)
        assertEquals("Walk dog", response.tiles.first().title)
    }

    @Test
    fun getTiles_threadsNextActionableFieldsIntoResponse() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.getTiles(any()) } returns V1ListTilesResponse(
            tiles = listOf(
                TileListView(id = "t-1", title = "Now", lifecycle = V1NumericConstants.LifecycleCode.STARTED)
            ),
            nextActionableTileId = "t-1",
            nextActionableStartAt = "2026-07-08T09:00:00Z",
        )
        val repository = newRepository(apiClient)

        val response = repository.getTiles(TileFilter.DEFAULT)

        assertEquals("t-1", response.nextActionableTileId)
        assertEquals("2026-07-08T09:00:00Z", response.nextActionableStartAt)
        assertTrue(repository.latestReadDiagnostics().contains("next_tile=t-1"))
    }

    @Test
    fun getTiles_skipsApiAndReturnsEmptyWhenNoToken() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val repository = newRepository(apiClient, idToken = null)

        val response = repository.getTiles()

        coVerify(exactly = 0) { apiClient.getTiles(any()) }
        assertEquals(0, response.tiles.size)
        assertNull(response.nextActionableTileId)
        assertNull(response.nextActionableStartAt)
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1_skipped "))
    }
}