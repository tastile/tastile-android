package app.tastile.android.ui.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.TileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

@HiltViewModel
class MemoViewModel @Inject constructor(
    private val tileRepository: TileRepository,
    private val authRepository: AuthRepository,
    private val supabaseClient: SupabaseClient
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
                val session = authRepository.currentSession
                val userId = session?.user?.id
                if (userId != null) {
                    // Get 5 most recent non-deleted tiles
                    val tiles = supabaseClient.from("tiles")
                        .select {
                            filter {
                                eq("user_id", userId)
                                exact("deleted_at", null)
                            }
                            order("updated_at", Order.DESCENDING)
                            limit(5)
                        }
                        .decodeList<Tile>()
                    
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
                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
                
                // Get current annotation_conditions
                val tile = supabaseClient.from("tiles")
                    .select {
                        filter {
                            eq("id", tileId)
                        }
                    }
                    .decodeSingle<Tile>()
                
                // Build updated annotation_conditions with note
                val currentAnnotations = tile.annotationConditions ?: buildJsonObject { }
                val updatedAnnotations = buildJsonObject {
                    currentAnnotations.entries.forEach { (key, value) ->
                        put(key, value)
                    }
                    put("note", JsonPrimitive(note))
                }
                
                // Update tile
                supabaseClient.from("tiles")
                    .update({
                        set("annotation_conditions", updatedAnnotations)
                        set("updated_at", now)
                    }) {
                        filter {
                            eq("id", tileId)
                        }
                    }
                
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
