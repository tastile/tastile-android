package app.tastile.android.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.timeline.TimelinePageKey
import app.tastile.android.data.timeline.TimelinePageRepository
import app.tastile.android.data.timeline.TimelinePageSnapshot
import app.tastile.android.data.timeline.TimelineRefreshDirection
import app.tastile.android.data.timeline.normalizeTimelineAnchor
import app.tastile.android.ui.dashboard.TimelineScale
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Boundary used by the screen state holder to send a refresh intent. */
fun interface TimelinePageRefreshRequester {
    fun requestRefresh(
        keys: Collection<TimelinePageKey>,
        direction: TimelineRefreshDirection,
    )
}

/**
 * Owns the page-scoped timeline state consumed by Compose.
 *
 * This ViewModel observes only [TimelinePageRepository]. Synchronization is
 * an intent sent to [TimelinePageRefreshRequester]; API responses and network
 * jobs never enter the UI state. At most three page observations are active:
 * the previous, current, and next page for the selected scale.
 */
@HiltViewModel
class TimelinePageViewModel private constructor(
    private val pageRepository: TimelinePageRepository,
    private val refreshRequester: TimelinePageRefreshRequester,
    initialKey: TimelinePageKey?,
    initialReadOnly: Boolean,
    @Suppress("UNUSED_PARAMETER") constructionMarker: Unit,
) : ViewModel() {
    /** Hilt production constructor. */
    @Inject
    constructor(
        pageRepository: TimelinePageRepository,
        refreshCoordinator: app.tastile.android.data.timeline.TimelineRefreshCoordinator,
    ) : this(
        pageRepository = pageRepository,
        refreshRequester = TimelinePageRefreshRequester { keys, direction ->
            refreshCoordinator.requestRefresh(keys, direction)
        },
        initialKey = null,
        initialReadOnly = false,
        constructionMarker = Unit,
    )

    /** Constructor for tests and non-Hilt screen hosts. */
    constructor(
        pageRepository: TimelinePageRepository,
        refreshRequester: TimelinePageRefreshRequester,
        initialKey: TimelinePageKey,
        isReadOnly: Boolean = false,
    ) : this(
        pageRepository = pageRepository,
        refreshRequester = refreshRequester,
        initialKey = initialKey,
        initialReadOnly = isReadOnly,
        constructionMarker = Unit,
    )

    /** Constructor for hosts that configure the page context later. */
    constructor(
        pageRepository: TimelinePageRepository,
        refreshRequester: TimelinePageRefreshRequester,
    ) : this(
        pageRepository = pageRepository,
        refreshRequester = refreshRequester,
        initialKey = null,
        initialReadOnly = false,
        constructionMarker = Unit,
    )

    /** Convenience constructor for callers that already have the concrete coordinator. */
    constructor(
        pageRepository: TimelinePageRepository,
        refreshCoordinator: app.tastile.android.data.timeline.TimelineRefreshCoordinator,
        initialKey: TimelinePageKey,
        isReadOnly: Boolean = false,
    ) : this(
        pageRepository = pageRepository,
        refreshRequester = TimelinePageRefreshRequester { keys, direction ->
            refreshCoordinator.requestRefresh(keys, direction)
        },
        initialKey = initialKey,
        initialReadOnly = isReadOnly,
        constructionMarker = Unit,
    )

    private val _uiState = MutableStateFlow(TimelinePageUiState.initial())
    val uiState: StateFlow<TimelinePageUiState> = _uiState.asStateFlow()

    /** Alias used by screens that name their collected state simply `state`. */
    val state: StateFlow<TimelinePageUiState> = uiState

    private var activeKey: TimelinePageKey? = null
    private var observationGeneration = 0L
    private val observationJobs = mutableListOf<Job>()

    init {
        initialKey?.let { setPageKey(it, initialReadOnly) }
    }

    /** The canonical key for the currently selected page, when configured. */
    val currentPageKey: TimelinePageKey?
        get() = activeKeyForState()

    /** Canonical key immediately before [currentPageKey]. */
    val previousPageKey: TimelinePageKey?
        get() = currentPageKey?.let { shiftPage(it, -1) }

    /** Canonical key immediately after [currentPageKey]. */
    val nextPageKey: TimelinePageKey?
        get() = currentPageKey?.let { shiftPage(it, 1) }

    /** Visible page order is previous, current, next. */
    val visiblePageKeys: List<TimelinePageKey>
        get() = listOfNotNull(previousPageKey, currentPageKey, nextPageKey)

    private fun refreshPageKeys(): List<TimelinePageKey> =
        listOfNotNull(currentPageKey, previousPageKey, nextPageKey)

    val previousPage: TimelinePageSnapshot?
        get() = previousPageKey?.let { uiState.value.pages[it] }

    val currentPage: TimelinePageSnapshot?
        get() = currentPageKey?.let { uiState.value.pages[it] }

    val nextPage: TimelinePageSnapshot?
        get() = nextPageKey?.let { uiState.value.pages[it] }

    /**
     * Starts observing a page context. Existing observations are cancelled
     * before the new three page set is installed.
     */
    fun setPageKey(key: TimelinePageKey, isReadOnly: Boolean = uiState.value.isReadOnly) {
        val normalizedKey = key.normalized()
        activeKey = normalizedKey
        _uiState.value = uiState.value.copy(
            scale = normalizedKey.scale,
            anchors = uiState.value.anchors.toPersistentMap().putting(normalizedKey.scale, normalizedKey.anchor),
            pages = persistentMapOf(),
            isReadOnly = isReadOnly,
        )
        restartPageObservations()
    }

    /** Semantic alias for hosts that configure the ViewModel once at entry. */
    fun configure(key: TimelinePageKey, isReadOnly: Boolean = uiState.value.isReadOnly) {
        setPageKey(key, isReadOnly)
    }

    /** Updates account/scope/zone context while retaining the selected scale. */
    fun setContext(
        accountId: String,
        scopeKey: String,
        zoneId: java.time.ZoneId,
        anchor: java.time.LocalDate = uiState.value.currentAnchor,
        scale: TimelineScale = uiState.value.scale,
        isReadOnly: Boolean = uiState.value.isReadOnly,
    ) {
        setPageKey(
            TimelinePageKey(
                accountId = accountId,
                scopeKey = scopeKey,
                zoneId = zoneId,
                scale = scale,
                anchor = anchor,
            ),
            isReadOnly,
        )
    }

    /** Switches scale without discarding that scale's anchor or zoom. */
    fun setScale(scale: TimelineScale) {
        val currentState = uiState.value
        val anchor = normalizeTimelineAnchor(
            scale = scale,
            anchor = currentState.anchors[scale] ?: currentState.currentAnchor,
        )
        if (currentState.scale == scale && activeKey != null) return

        _uiState.value = currentState.copy(
            scale = scale,
            anchors = currentState.anchors.toPersistentMap().putting(scale, anchor),
            pages = persistentMapOf(),
        )
        activeKey = activeKey?.copy(scale = scale, anchor = anchor)?.normalized()
        restartPageObservations()
    }

    /** Sets the anchor for the selected scale, normalizing Week/Month keys. */
    fun setAnchor(anchor: java.time.LocalDate) {
        setAnchor(uiState.value.scale, anchor)
    }

    /** Sets an anchor without changing the selected scale. */
    fun setAnchor(scale: TimelineScale, anchor: java.time.LocalDate) {
        val normalizedAnchor = normalizeTimelineAnchor(scale, anchor)
        val currentState = uiState.value
        if (currentState.anchors[scale] == normalizedAnchor) {
            return
        }

        _uiState.value = currentState.copy(
            anchors = currentState.anchors.toPersistentMap().putting(scale, normalizedAnchor),
            pages = if (scale == currentState.scale) persistentMapOf() else currentState.pages,
        )
        if (scale == currentState.scale) {
            activeKey = activeKey?.copy(anchor = normalizedAnchor)?.normalized()
            restartPageObservations()
        }
    }

    /** Sets the current scale's pinch zoom. */
    fun setZoom(zoom: Float) {
        setZoom(uiState.value.scale, zoom)
    }

    /** Sets one scale's pinch zoom without affecting other scales. */
    fun setZoom(scale: TimelineScale, zoom: Float) {
        val clampedZoom = zoom.coerceIn(MIN_TIMELINE_ZOOM, MAX_TIMELINE_ZOOM)
        if (uiState.value.zooms[scale] == clampedZoom) return
        _uiState.update { it.copy(zooms = it.zooms.toPersistentMap().putting(scale, clampedZoom)) }
    }

    fun setReadOnly(isReadOnly: Boolean) {
        if (uiState.value.isReadOnly == isReadOnly) return
        _uiState.update { it.copy(isReadOnly = isReadOnly) }
    }

    /** Requests coverage refresh for current/adjacent pages in [direction]. */
    fun requestRefresh(direction: TimelineRefreshDirection = TimelineRefreshDirection.None) {
        val keys = refreshPageKeys()
        if (keys.isNotEmpty()) refreshRequester.requestRefresh(keys, direction)
    }

    /** Called by pager settle/drag callbacks to preserve directional priority. */
    fun onSwipe(direction: TimelineRefreshDirection) {
        requestRefresh(direction)
    }

    /** Updates the selected page and sends one directional refresh intent. */
    fun onPageSettled(
        anchor: java.time.LocalDate,
        direction: TimelineRefreshDirection = TimelineRefreshDirection.None,
    ) {
        setAnchor(anchor)
        requestRefresh(direction)
    }

    private fun activeKeyForState(): TimelinePageKey? {
        val key = activeKey ?: return null
        val state = uiState.value
        return key.copy(
            scale = state.scale,
            anchor = state.anchors[state.scale] ?: key.anchor,
        ).normalized()
    }

    private fun restartPageObservations() {
        observationGeneration += 1
        val generation = observationGeneration
        observationJobs.forEach(Job::cancel)
        observationJobs.clear()

        val keys = visiblePageKeys
        if (keys.isEmpty()) return

        keys.forEach { key ->
            observationJobs += viewModelScope.launch {
                pageRepository.observePage(key).collect { snapshot ->
                    // A repository implementation must key its Flow, but the
                    // guard also protects the UI if a shared upstream emits a
                    // different page while this collector is active.
                    if (generation != observationGeneration || snapshot.key.normalized() != key) {
                        return@collect
                    }
                    val canonicalSnapshot = if (snapshot.key == key) {
                        snapshot
                    } else {
                        snapshot.copy(key = key)
                    }
                    _uiState.update { currentState ->
                        if (generation != observationGeneration || currentState.scale != key.scale) {
                            currentState
                        } else {
                            currentState.copy(
                                pages = currentState.pages.toPersistentMap().putting(key, canonicalSnapshot),
                            )
                        }
                    }
                }
            }
        }
        refreshRequester.requestRefresh(refreshPageKeys(), TimelineRefreshDirection.None)
    }

    private fun shiftPage(key: TimelinePageKey, amount: Long): TimelinePageKey {
        val anchor = when (key.scale) {
            TimelineScale.Day,
            TimelineScale.List,
            -> key.anchor.plusDays(amount)

            TimelineScale.Week -> key.anchor.plusWeeks(amount)
            TimelineScale.Month -> key.anchor.plusMonths(amount)
        }
        return key.copy(anchor = anchor).normalized()
    }

    private companion object {
        const val MIN_TIMELINE_ZOOM = 1f
        const val MAX_TIMELINE_ZOOM = 6f
    }
}
