package app.tastile.android.data.tile

import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1ListTilesResponse
import app.tastile.android.data.api.V1NumericConstants
import app.tastile.android.data.api.TileListView
import app.tastile.android.data.auth.CurrentUserProvider
import app.tastile.android.data.command.V1CommandDispatcher
import app.tastile.android.data.execution.EventRepository
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C3 read-path coverage: `TileRepository.getTiles(filter)` must pass the
 * filter through to `V1ApiClient.getTiles(filter)` and map the tiles into
 * [TilesResponse].
 */
class TileRepositoryGetTilesTest {

    private fun newRepository(
        apiClient: V1ApiClient,
        userId: String = "user-1",
        sessionToken: String? = "session-abc"
    ): TileRepository = TileRepository(
        executionNotificationCoordinator = mockk<ExecutionNotificationCoordinator>(relaxed = true),
        eventRepository = mockk<EventRepository>(relaxed = true),
        currentUserProvider = mockk<CurrentUserProvider> {
            every { currentUserId() } returns userId
            every { currentSessionToken() } returns sessionToken
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
    fun getTiles_mapsResponseTiles() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.getTiles(any()) } returns V1ListTilesResponse(
            tiles = listOf(
                TileListView(id = "t-1", title = "Now", lifecycle = V1NumericConstants.LifecycleCode.STARTED)
            ),
        )
        val repository = newRepository(apiClient)

        val response = repository.getTiles(TileFilter.DEFAULT)

        assertEquals(1, response.tiles.size)
        assertEquals("Now", response.tiles.first().title)
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1 "))
    }

    @Test
    fun getTiles_skipsApiAndReturnsEmptyWhenNoToken() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val repository = newRepository(apiClient, sessionToken = null)

        val response = repository.getTiles()

        coVerify(exactly = 0) { apiClient.getTiles(any()) }
        assertEquals(0, response.tiles.size)
        assertTrue(repository.latestReadDiagnostics().startsWith("source=v1_skipped "))
    }
}
