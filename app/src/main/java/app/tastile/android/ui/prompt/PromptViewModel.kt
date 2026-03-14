package app.tastile.android.ui.prompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.TileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class PromptViewModel @Inject constructor(
    private val tileRepository: TileRepository,
    private val authRepository: AuthRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _activeTile = MutableStateFlow<Tile?>(null)
    val activeTile: StateFlow<Tile?> = _activeTile.asStateFlow()

    private val _elapsedMinutes = MutableStateFlow(0)
    val elapsedMinutes: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Threshold in minutes to show prompt (25 minutes)
    companion object {
        private const val PROMPT_THRESHOLD_MINUTES = 25
    }

    init {
        loadActiveTile()
        startElapsedTimeCounter()
    }

    private fun loadActiveTile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val session = authRepository.currentSession
                val userId = session?.user?.id
                if (userId != null) {
                    // Get the most recent Started tile
                    val tiles = supabaseClient.from("tiles")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("lifecycle", TileLifecycle.STARTED.value)
                                exact("deleted_at", null)
                            }
                        }
                        .decodeList<Tile>()
                    
                    _activeTile.value = tiles.firstOrNull()
                    updateElapsedTime()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load active tile"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startElapsedTimeCounter() {
        viewModelScope.launch {
            while (isActive) {
                delay(60000) // Update every minute
                updateElapsedTime()
            }
        }
    }

    private fun updateElapsedTime() {
        val tile = _activeTile.value ?: return
        val updatedAt = tile.updatedAt ?: return
        
        val updatedTime = try {
            Instant.parse(updatedAt)
        } catch (e: Exception) {
            // Skip unparseable tiles
            return
        }
        
        try {
            val now = Clock.System.now()
            val elapsed = now - updatedTime
            _elapsedMinutes.value = elapsed.inWholeMinutes.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun continueTile() {
        // Just update the timestamp to reset the prompt timer
        viewModelScope.launch {
            try {
                _error.value = null
                val tileId = _activeTile.value?.id ?: return@launch
                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
                
                supabaseClient.from("tiles")
                    .update({
                        set("updated_at", now)
                    }) {
                        filter {
                            eq("id", tileId)
                        }
                    }
                
                loadActiveTile()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to continue tile"
                e.printStackTrace()
            }
        }
    }

    fun takeBreak() {
        // Set lifecycle back to Ready (Take a Break = pause the tile)
        viewModelScope.launch {
            try {
                _error.value = null
                val tileId = _activeTile.value?.id ?: return@launch
                tileRepository.pauseTile(tileId) // lifecycle -> Ready
                _activeTile.value = null
                _elapsedMinutes.value = 0
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to pause tile"
                e.printStackTrace()
            }
        }
    }

    fun completeTile() {
        viewModelScope.launch {
            try {
                _error.value = null
                val tileId = _activeTile.value?.id ?: return@launch
                tileRepository.completeTile(tileId)
                _activeTile.value = null
                _elapsedMinutes.value = 0
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to complete tile"
                e.printStackTrace()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun refresh() {
        loadActiveTile()
    }
}
