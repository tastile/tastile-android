package app.tastile.android.ui.prompt

import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.CurrentUserProvider
import app.tastile.android.data.repository.PromptTileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PromptViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsActiveStartedTileViaRepository() {
        val repository = FakePromptTileRepository(
            activeTile = Tile(id = "tile-1", updatedAt = "2025-01-01T00:00:00Z")
        )
        val currentUserProvider = FakeCurrentUserProvider(userId = "user-1")

        val viewModel = PromptViewModel(repository, currentUserProvider)

        assertEquals("user-1", repository.lastActiveUserId)
        assertEquals("tile-1", viewModel.activeTile.value?.id)
    }

    @Test
    fun continueTile_callsRepositoryContinueAndReloadsActiveTile() {
        val repository = FakePromptTileRepository(
            activeTile = Tile(id = "tile-1", updatedAt = "2025-01-01T00:00:00Z"),
            afterContinueTile = Tile(id = "tile-1", updatedAt = "2025-01-01T00:05:00Z")
        )
        val currentUserProvider = FakeCurrentUserProvider(userId = "user-1")
        val viewModel = PromptViewModel(repository, currentUserProvider)

        viewModel.continueTile()

        assertEquals(listOf("tile-1"), repository.continuedTileIds)
        assertEquals(2, repository.activeTileRequestCount)
    }

    @Test
    fun takeBreak_callsRepositoryPauseAndClearsState() {
        val repository = FakePromptTileRepository(
            activeTile = Tile(id = "tile-2", updatedAt = "2025-01-01T00:00:00Z")
        )
        val currentUserProvider = FakeCurrentUserProvider(userId = "user-1")
        val viewModel = PromptViewModel(repository, currentUserProvider)

        viewModel.takeBreak()

        assertEquals(listOf("tile-2"), repository.pausedTileIds)
        assertNull(viewModel.activeTile.value)
        assertEquals(0, viewModel.elapsedMinutes.value)
    }

    @Test
    fun completeTile_callsRepositoryCompleteAndClearsState() {
        val repository = FakePromptTileRepository(
            activeTile = Tile(id = "tile-3", updatedAt = "2025-01-01T00:00:00Z")
        )
        val currentUserProvider = FakeCurrentUserProvider(userId = "user-1")
        val viewModel = PromptViewModel(repository, currentUserProvider)

        viewModel.completeTile()

        assertEquals(listOf("tile-3"), repository.completedTileIds)
        assertNull(viewModel.activeTile.value)
        assertEquals(0, viewModel.elapsedMinutes.value)
    }

    private class FakeCurrentUserProvider(
        private val userId: String?
    ) : CurrentUserProvider {
        override fun currentUserId(): String? = userId
    }

    private class FakePromptTileRepository(
        activeTile: Tile? = null,
        private val afterContinueTile: Tile? = activeTile
    ) : PromptTileRepository {
        private var activeTileResponse: Tile? = activeTile
        var lastActiveUserId: String? = null
        var activeTileRequestCount: Int = 0
        val continuedTileIds = mutableListOf<String>()
        val pausedTileIds = mutableListOf<String>()
        val completedTileIds = mutableListOf<String>()

        override suspend fun getActiveStartedTile(userId: String): Tile? {
            lastActiveUserId = userId
            activeTileRequestCount += 1
            return activeTileResponse
        }

        override suspend fun continueTile(tileId: String) {
            continuedTileIds += tileId
            activeTileResponse = afterContinueTile
        }

        override suspend fun pauseTile(tileId: String) {
            pausedTileIds += tileId
        }

        override suspend fun completeTile(tileId: String): Tile {
            completedTileIds += tileId
            return Tile(id = tileId)
        }
    }
}

