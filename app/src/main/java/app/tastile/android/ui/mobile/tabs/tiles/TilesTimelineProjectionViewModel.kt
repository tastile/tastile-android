package app.tastile.android.ui.mobile.tabs.tiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.timeline.TimelinePageRepository
import app.tastile.android.data.timeline.TimelineProjectionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Local read-only projection for the compact Tiles timeline and changes
 * panels. Its upstream is active only while a visible panel collects
 * [items], so entering/leaving the tab does not start a network refresh.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TilesTimelineProjectionViewModel @Inject constructor(
    private val pageRepository: TimelinePageRepository,
) : ViewModel() {
    private val request = MutableStateFlow<TimelineProjectionRequest?>(null)

    val projectionRequest: StateFlow<TimelineProjectionRequest?> = request.asStateFlow()

    val items: StateFlow<List<CoreTimelineItem>> = request
        .flatMapLatest { current ->
            current?.let(::observeProjection) ?: flowOf(emptyList())
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    fun setRequest(next: TimelineProjectionRequest?) {
        val normalized = next?.normalized()
        if (request.value != normalized) request.value = normalized
    }

    private fun observeProjection(request: TimelineProjectionRequest): Flow<List<CoreTimelineItem>> =
        pageRepository.observeProjection(request).distinctUntilChanged()
}
