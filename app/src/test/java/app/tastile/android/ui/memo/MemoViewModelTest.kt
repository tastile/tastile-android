package app.tastile.android.ui.memo

import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.CurrentUserProvider
import app.tastile.android.data.repository.MemoTileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsRecentTilesViaRepository_andSelectsFirstTile() {
        val tiles = listOf(Tile(id = "tile-1"), Tile(id = "tile-2"))
        val repository = FakeMemoTileRepository(recentTiles = tiles)
        val currentUserProvider = FakeCurrentUserProvider(userId = "user-1")

        val viewModel = MemoViewModel(repository, currentUserProvider)

        assertEquals("user-1", repository.lastRecentTilesUserId)
        assertEquals(5, repository.lastRecentTilesLimit)
        assertEquals(tiles, viewModel.recentTiles.value)
        assertEquals("tile-1", viewModel.selectedTileId.value)
    }

    @Test
    fun selectTile_thenSaveMemo_callsRepositorySaveMemo() {
        val repository = FakeMemoTileRepository(recentTiles = listOf(Tile(id = "tile-1")))
        val currentUserProvider = FakeCurrentUserProvider(userId = "user-1")
        val viewModel = MemoViewModel(repository, currentUserProvider)
        viewModel.selectTile("tile-99")

        viewModel.saveMemo("deep focus")

        assertEquals(listOf("tile-99" to "deep focus"), repository.savedMemos)
        assertEquals(true, viewModel.saveSuccess.value)
    }

    private class FakeCurrentUserProvider(
        private val userId: String?
    ) : CurrentUserProvider {
        override fun currentUserId(): String? = userId
    }

    private class FakeMemoTileRepository(
        private val recentTiles: List<Tile>
    ) : MemoTileRepository {
        var lastRecentTilesUserId: String? = null
        var lastRecentTilesLimit: Int? = null
        val savedMemos = mutableListOf<Pair<String, String>>()

        override suspend fun getRecentTiles(userId: String, limit: Int): List<Tile> {
            lastRecentTilesUserId = userId
            lastRecentTilesLimit = limit
            return recentTiles
        }

        override suspend fun saveMemo(tileId: String, note: String) {
            savedMemos += tileId to note
        }
    }
}

