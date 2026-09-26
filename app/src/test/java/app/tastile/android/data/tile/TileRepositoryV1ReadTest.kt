package app.tastile.android.data.tile

import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1Error
import app.tastile.android.data.api.V1ListTilesResponse
import app.tastile.android.data.api.V1NumericConstants
import app.tastile.android.data.api.TileListView
import app.tastile.android.data.api.TileTemporalView
import app.tastile.android.data.auth.CurrentUserProvider
import app.tastile.android.data.command.V1CommandDispatcher
import app.tastile.android.data.execution.EventRepository
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileRepositoryV1ReadTest {

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
    fun getTiles_returnsEmptyWhenSessionTokenMissing_andDoesNotCallV1() = runTest {
        val apiClient = mockk<V1ApiClient>(relaxed = true)
        val repository = newRepository(apiClient, sessionToken = null)

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

    @Test
    fun getTimeline_treatsCanonicalEmptyResponseAsAuthoritative() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.getTiles(any()) } returns V1ListTilesResponse(
            tiles = listOf(
                TileListView(
                    id = "fallback-tile",
                    title = "Should not be used",
                    lifecycle = V1NumericConstants.LifecycleCode.READY,
                    temporal = TileTemporalView(
                        fixedStart = "2026-09-16T09:00:00Z",
                        fixedEnd = "2026-09-16T10:00:00Z",
                    ),
                ),
            ),
        )
        coEvery { apiClient.getTimeline(any(), any(), any()) } returns emptyList()
        val repository = newRepository(apiClient)
        val start = Instant.parse("2026-09-16T00:00:00Z")
        val end = Instant.parse("2026-09-17T00:00:00Z")

        // Seed the compatibility fallback so a successful empty canonical
        // response cannot accidentally look equivalent to an empty fallback.
        repository.getTiles()
        val result = repository.getTimeline(start, end)

        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { apiClient.getTiles(TileFilter.DEFAULT) }
        coVerify(exactly = 1) { apiClient.getTimeline(start, end, emptyList()) }
    }

    @Test
    fun getTimelineCanonical_returnsTypedFailureWithoutFallback() = runTest {
        val apiClient = mockk<V1ApiClient>()
        val failure = V1Error.Network(IllegalStateException("offline"))
        coEvery { apiClient.getTimeline(any(), any(), any()) } throws failure
        val repository = newRepository(apiClient)
        val start = Instant.parse("2026-09-16T00:00:00Z")
        val end = Instant.parse("2026-09-17T00:00:00Z")

        val result = repository.getTimelineCanonical(start, end)

        assertTrue(result is TimelineFetchResult.Failure)
        assertEquals(failure, (result as TimelineFetchResult.Failure).error)
        coVerify(exactly = 0) { apiClient.getTiles(any()) }
    }

    @Test
    fun getTimeline_keepsCompatibilityFallbackForCanonicalFailure() = runTest {
        val apiClient = mockk<V1ApiClient>()
        coEvery { apiClient.getTiles(any()) } returns V1ListTilesResponse(
            tiles = listOf(
                TileListView(
                    id = "fallback-tile",
                    title = "Cached fallback",
                    lifecycle = V1NumericConstants.LifecycleCode.READY,
                    temporal = TileTemporalView(
                        fixedStart = "2026-09-16T09:00:00Z",
                        fixedEnd = "2026-09-16T10:00:00Z",
                    ),
                ),
            ),
        )
        coEvery { apiClient.getTimeline(any(), any(), any()) } throws
            V1Error.Network(IllegalStateException("offline"))
        val repository = newRepository(apiClient)
        val start = Instant.parse("2026-09-16T00:00:00Z")
        val end = Instant.parse("2026-09-17T00:00:00Z")

        repository.getTiles()
        val result = repository.getTimeline(start, end)

        assertTrue(result.isEmpty())
        assertTrue(repository.latestReadDiagnostics().contains("timeline_source=cloud_fallback"))
        coVerify(exactly = 1) { apiClient.getTiles(TileFilter.DEFAULT) }
    }
}
