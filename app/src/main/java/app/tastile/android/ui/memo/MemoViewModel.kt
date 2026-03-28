package app.tastile.android.ui.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.CurrentUserProvider
import app.tastile.android.data.repository.MemoTileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoViewModel @Inject constructor(
    private val tileRepository: MemoTileRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _recentTiles = MutableStateFlow<List<Tile>>(emptyList())
    val recentTiles: StateFlow<List<Tile>> = _recentTiles.asStateFlow()

    private val _selectedTileId = MutableStateFlow<String?>(null)
    val selectedTileId: StateFlow<String?> = _selectedTileId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadRecentTiles()
    }

    fun loadRecentTiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userId = currentUserProvider.currentUserId()
                if (userId != null) {
                    val tiles = tileRepository.getRecentTiles(userId, limit = 5)
                    _recentTiles.value = tiles
                    
                    // Select first tile by default
                    if (_selectedTileId.value == null && tiles.isNotEmpty()) {
                        _selectedTileId.value = tiles.first().id
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load tiles"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectTile(tileId: String) {
        _selectedTileId.value = tileId
    }

    fun saveMemo(note: String) {
        val tileId = _selectedTileId.value ?: return
        if (note.isBlank()) return

        viewModelScope.launch {
            try {
                _error.value = null
                tileRepository.saveMemo(tileId, note)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save memo"
                e.printStackTrace()
            }
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    fun clearError() {
        _error.value = null
    }
}
