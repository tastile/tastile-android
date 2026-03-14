package app.tastile.android.ui.now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.TileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowViewModel @Inject constructor(
    private val tileRepository: TileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _tiles = MutableStateFlow<List<Tile>>(emptyList())
    val tiles: StateFlow<List<Tile>> = _tiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTiles()
    }

    fun loadTiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val session = authRepository.currentSession
                val userId = session?.user?.id
                if (userId != null) {
                    _tiles.value = tileRepository.getTiles(userId)
                }
            } catch (e: Exception) {
                _error.value = e.message
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createTile(title: String) {
        if (title.isBlank()) return
        
        viewModelScope.launch {
            try {
                val session = authRepository.currentSession
                val userId = session?.user?.id
                if (userId != null) {
                    tileRepository.createTile(userId, title)
                    loadTiles() // Refresh list
                }
            } catch (e: Exception) {
                _error.value = e.message
                e.printStackTrace()
            }
        }
    }

    fun startTile(tileId: String) {
        viewModelScope.launch {
            try {
                tileRepository.startTile(tileId)
                loadTiles() // Refresh list
            } catch (e: Exception) {
                _error.value = e.message
                e.printStackTrace()
            }
        }
    }

    fun completeTile(tileId: String) {
        viewModelScope.launch {
            try {
                tileRepository.completeTile(tileId)
                loadTiles() // Refresh list
            } catch (e: Exception) {
                _error.value = e.message
                e.printStackTrace()
            }
        }
    }

    fun deleteTile(tileId: String) {
        viewModelScope.launch {
            try {
                tileRepository.deleteTile(tileId)
                loadTiles() // Refresh list
            } catch (e: Exception) {
                _error.value = e.message
                e.printStackTrace()
            }
        }
    }
}
