package app.tastile.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.model.Profile
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.model.projectLabels
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.ReferenceOverlayStore
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.data.repository.TileFilter
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.data.util.formatIsoDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

enum class TimelineScale { Day, Week, Month, List }

/**
 * Sub-tab selector for the mobile Tiles tab. Independent from
 * [TimelineScale] (which drives the main `/timeline` route) so
 * C4 can ship a self-contained scale selector without colliding
 * with the existing timeline screen.
 */
enum class TilesTab { LIST, TIMELINE, CHANGES }

enum class TileRange { ALL, TODAY, RECENT, EXCLUDE_FUTURE }
enum class TileGranularity { ALL, NO_BREAKS, MIN_5M, MIN_15M, MIN_30M }
enum class ListGroupingMode { STATE, PROJECT, TAG }
enum class ListViewMode { COMPACT, COMFORTABLE, DETAILED }
enum class TimelineSubScale { DAY, WEEK, MONTH, CUSTOM }

/**
 * One labelled grouping emitted by [DashboardViewModel.groupedTiles].
 * `groupId` is stable across recompositions so it can drive both
 * section headers and `testTag`s.
 */
data class TileSection(
    val groupId: String,
    val labelKey: String,
    val tiles: List<Tile>,
)

data class CreateTileDraft(
    val title: String,
    val nextAction: String? = null,
    val doneDefinition: String? = null,
    val tileKind: String = "work",
    val objectiveMode: String = "finish_once",
    val useStartAt: Boolean = false,
    val useEndAt: Boolean = false,
    val startAtIso: String? = null,
    val endAtIso: String? = null,
    val recurrenceFrequency: String = "daily",
    val recurrenceInterval: Int = 1,
    val recurrenceWeekdays: List<Int> = emptyList(),
    val recurrenceMonthlyWeek: Int = 1,
    val recurrenceMonthlyWeekday: Int = 0,
    val recurrenceUseStartAt: Boolean = true,
    val recurrenceUseEndAt: Boolean = true,
    val recurrenceStartTime: String? = null,
    val recurrenceEndTime: String? = null,
    val recurrenceValidFromIso: String? = null,
    val recurrenceValidToIso: String? = null,
    val breakSplitsWork: Boolean = true,
    val project: String? = null,
    val labels: List<String> = emptyList(),
    val memo: String? = null,
    val targetWorkMin: Int? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val tileRepository: TileRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val referenceOverlayStore: ReferenceOverlayStore,
) : ViewModel() {
    private val cardMapper = DashboardCardMapper()
    private val _tiles = MutableStateFlow<List<Tile>>(emptyList())
    val tiles: StateFlow<List<Tile>> = _tiles.asStateFlow()

    private val _tileFilter = MutableStateFlow(TileFilter.DEFAULT)
    val tileFilter: StateFlow<TileFilter> = _tileFilter.asStateFlow()

    private val _nextActionableTileId = MutableStateFlow<String?>(null)
    val nextActionableTileId: StateFlow<String?> = _nextActionableTileId.asStateFlow()

    private val _nextActionableStartAt = MutableStateFlow<String?>(null)
    val nextActionableStartAt: StateFlow<String?> = _nextActionableStartAt.asStateFlow()

    fun setTileFilter(filter: TileFilter) {
        _tileFilter.value = filter
    }

    /** Applies the same owner_ids selection to tile lists and the calendar. */
    fun setOwnerFilter(ownerId: String?) {
        setOwnerFilters(ownerId?.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty())
    }

    /** Applies Calendar's checked workspace tree as the v1 `owner_ids` selection. */
    fun setOwnerFilters(ownerIds: Collection<String>) {
        _tileFilter.value = _tileFilter.value.copy(ownerIds = ownerIds.filter { it.isNotBlank() }.distinct())
        refreshTimeline()
    }

    private val _selectedTileId = MutableStateFlow<String?>(null)

    val selectedTile: StateFlow<Tile?> = combine(tiles, _selectedTileId) { list, id ->
        id?.let { tid -> list.firstOrNull { it.id == tid } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectTile(id: String) {
        _selectedTileId.value = id
    }

    fun clearSelectedTile() {
        _selectedTileId.value = null
    }

    internal fun replaceTilesForTest(list: List<Tile>) {
        _tiles.value = list
    }

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _themeMode = MutableStateFlow(userSettingsRepository.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _locale = MutableStateFlow(userSettingsRepository.getLocale())
    val locale: StateFlow<AppLocale> = _locale.asStateFlow()
    private val _securityLockEnabled = MutableStateFlow(userSettingsRepository.getSecurityLockEnabled())
    val securityLockEnabled: StateFlow<Boolean> = _securityLockEnabled.asStateFlow()
    private val _securityLockTimeoutMinutes = MutableStateFlow(userSettingsRepository.getSecurityLockTimeoutMinutes())
    val securityLockTimeoutMinutes: StateFlow<Int> = _securityLockTimeoutMinutes.asStateFlow()

    private val _timeline = MutableStateFlow<List<CoreTimelineItem>>(emptyList())
    val timeline: StateFlow<List<CoreTimelineItem>> = _timeline.asStateFlow()

    private val _isLoadingTimeline = MutableStateFlow(false)
    val isLoadingTimeline: StateFlow<Boolean> = _isLoadingTimeline.asStateFlow()

    private val _timelineRange = MutableStateFlow(
        computeTimelineRange(LocalDate.now(), TimelineScale.Day)
    )
    val timelineRange: StateFlow<Pair<Instant, Instant>> = _timelineRange.asStateFlow()

    private val _selectedDay = MutableStateFlow(java.time.LocalDate.now())
    val selectedDay: StateFlow<java.time.LocalDate> = _selectedDay.asStateFlow()
    fun setSelectedDay(day: java.time.LocalDate) {
        _selectedDay.value = day
    }

    private val _scale = MutableStateFlow(TimelineScale.Day)
    val scale: StateFlow<TimelineScale> = _scale.asStateFlow()
    fun setScale(scale: TimelineScale) {
        _scale.value = scale
    }

    private val _calendarMode = MutableStateFlow(CalendarMode.Scope)
    val calendarMode: StateFlow<CalendarMode> = _calendarMode.asStateFlow()

    private val _calendarMinimumDurationMinutes = MutableStateFlow(0)
    val calendarMinimumDurationMinutes: StateFlow<Int> = _calendarMinimumDurationMinutes.asStateFlow()

    fun setCalendarMode(mode: CalendarMode) {
        _calendarMode.value = mode
        if (mode != CalendarMode.Scope) _selectedDay.value = LocalDate.now()
    }

    fun setCalendarMinimumDuration(minutes: Int) {
        _calendarMinimumDurationMinutes.value = minutes.coerceAtLeast(0)
        refreshTimeline()
    }

    fun moveCalendar(delta: Long) {
        if (!canNavigateCalendar(_calendarMode.value)) return
        _selectedDay.value = shiftCalendarAnchor(_selectedDay.value, _scale.value, delta)
    }

    fun goToCalendarToday() {
        _calendarMode.value = CalendarMode.Scope
        _selectedDay.value = LocalDate.now()
    }

    private val _statsDiagnostics = MutableStateFlow("n/a")
    val statsDiagnostics: StateFlow<String> = _statsDiagnostics.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────
    // Tiles-tab sub-tab + filter + grouping + view-mode state (C4)
    // ─────────────────────────────────────────────────────────────────────

    private val _activeTilesTab = MutableStateFlow(TilesTab.LIST)
    val activeTilesTab: StateFlow<TilesTab> = _activeTilesTab.asStateFlow()

    private val _searchTerm = MutableStateFlow("")
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()

    private val _filterRange = MutableStateFlow(TileRange.ALL)
    val filterRange: StateFlow<TileRange> = _filterRange.asStateFlow()

    private val _filterGranularity = MutableStateFlow(TileGranularity.MIN_5M)
    val filterGranularity: StateFlow<TileGranularity> = _filterGranularity.asStateFlow()

    private val _filterLimit = MutableStateFlow(50)
    val filterLimit: StateFlow<Int> = _filterLimit.asStateFlow()

    private val _listGroupingMode = MutableStateFlow(ListGroupingMode.STATE)
    val listGroupingMode: StateFlow<ListGroupingMode> = _listGroupingMode.asStateFlow()

    private val _listViewMode = MutableStateFlow(ListViewMode.COMFORTABLE)
    val listViewMode: StateFlow<ListViewMode> = _listViewMode.asStateFlow()

    private val _expandedSections = MutableStateFlow<Set<String>>(emptySet())
    val expandedSections: StateFlow<Set<String>> = _expandedSections.asStateFlow()

    private val _sectionLimits = MutableStateFlow<Map<String, Int>>(emptyMap())
    val sectionLimits: StateFlow<Map<String, Int>> = _sectionLimits.asStateFlow()

    private val _timelineScale = MutableStateFlow(TimelineSubScale.DAY)
    val timelineScale: StateFlow<TimelineSubScale> = _timelineScale.asStateFlow()

    private val _customStartIso = MutableStateFlow<String?>(null)
    val customStartIso: StateFlow<String?> = _customStartIso.asStateFlow()

    private val _customEndIso = MutableStateFlow<String?>(null)
    val customEndIso: StateFlow<String?> = _customEndIso.asStateFlow()

    /**
     * Schedule right-pane view mode. Mirrors the `?view=` URL parameter
     * on `tastile-web/src/components/panels/ScheduleSidePanel.tsx`;
     * persisted via [UserSettingsRepository] so the toggle survives
     * process death. C11 ships the two-button toggle + filter logic.
     * Values are unconstrained strings (`"recurring"` / `"upcoming"`)
     * intentionally — see
     * `app.tastile.android.ui.mobile.panels.schedule.VIEW_RECURRING` /
     * `VIEW_UPCOMING` constants.
     */
    private val _scheduleView = MutableStateFlow(userSettingsRepository.getScheduleView())
    val scheduleView: StateFlow<String> = _scheduleView.asStateFlow()

    private val _requestDeleteTileId = MutableStateFlow<String?>(null)
    val requestDeleteTileId: StateFlow<String?> = _requestDeleteTileId.asStateFlow()

    /**
     * Labels the user has enabled as overlays from the References side panel.
     * Mirrors `tastile-web/src/lib/stores/reference-overlay-store.ts`. C6
     * binds this to a `Switch` row per unique label inside the mobile panel.
     */
    val referenceOverlayEnabled: StateFlow<Set<String>> = referenceOverlayStore.enabled

    fun toggleReference(label: String) {
        viewModelScope.launch { referenceOverlayStore.toggle(label) }
    }

    /**
     * Tiles partitioned by the active [listGroupingMode]. STATE groups by
     * `Tile.lifecycle` (4 buckets), PROJECT groups by the first label
     * matching `"project:*"` (Unassigned fallback), TAG groups by every
     * other label (Untagged fallback). Each [TileSection] carries a
     * stable [TileSection.groupId] and a [TileSection.labelKey] the UI
     * can resolve via `stringResource(...)`.
     */
    val groupedTiles: StateFlow<List<TileSection>> = combine(_tiles, _listGroupingMode) { tiles, mode ->
        when (mode) {
            ListGroupingMode.STATE -> tiles
                .groupBy { TileLifecycle.fromString(it.lifecycle).name }
                .toSortedMap()
                .map { (key, group) ->
                    TileSection(groupId = key.lowercase(), labelKey = key.lowercase(), tiles = group)
                }
            ListGroupingMode.PROJECT -> {
                val sections = linkedMapOf<String, MutableList<Tile>>()
                tiles.forEach { tile ->
                    val projectLabel = tile.projectLabels().firstOrNull()
                    val key = projectLabel?.let { "project:$it" } ?: "__unassigned"
                    sections.getOrPut(key) { mutableListOf() }.add(tile)
                }
                sections.map { (key, group) ->
                    TileSection(
                        groupId = key,
                        labelKey = key.removePrefix("project:").takeIf { key.startsWith("project:") }
                            ?: "unassigned",
                        tiles = group,
                    )
                }
            }
            ListGroupingMode.TAG -> {
                val sections = linkedMapOf<String, MutableList<Tile>>()
                tiles.forEach { tile ->
                    val tags = tile.labels.filter { !it.startsWith("project:") }
                    if (tags.isEmpty()) {
                        sections.getOrPut("__untagged") { mutableListOf() }.add(tile)
                    } else {
                        tags.forEach { tag ->
                            sections.getOrPut("tag:$tag") { mutableListOf() }.add(tile)
                        }
                    }
                }
                sections.map { (key, group) ->
                    TileSection(
                        groupId = key,
                        labelKey = key.removePrefix("tag:").takeIf { key.startsWith("tag:") } ?: "untagged",
                        tiles = group,
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setActiveTilesTab(tab: TilesTab) {
        _activeTilesTab.value = tab
    }

    fun setSearchTerm(term: String) {
        _searchTerm.value = term
        rebuildTileFilter()
    }

    fun setFilterRange(range: TileRange) {
        _filterRange.value = range
        rebuildTileFilter()
    }

    fun setFilterGranularity(granularity: TileGranularity) {
        _filterGranularity.value = granularity
        rebuildTileFilter()
    }

    fun setFilterLimit(limit: Int) {
        _filterLimit.value = limit
        rebuildTileFilter()
    }

    fun setListGroupingMode(mode: ListGroupingMode) {
        _listGroupingMode.value = mode
    }

    fun setListViewMode(mode: ListViewMode) {
        _listViewMode.value = mode
    }

    fun toggleSectionExpanded(groupId: String) {
        _expandedSections.value = _expandedSections.value.toMutableSet().apply {
            if (!add(groupId)) remove(groupId)
        }
    }

    /**
     * Per-section visible-tile limit that doubles 8 → 16 → 32 → 60 then
     * resets (matching web's `nextTileSectionLimit` in
     * `tastile-web/src/app/dashboard/tiles/page.tsx`). Capped at 60 so
     * a pathological section never explodes the LazyColumn row budget.
     */
    fun bumpSectionLimit(groupId: String, totalCount: Int) {
        val current = _sectionLimits.value[groupId] ?: INITIAL_SECTION_LIMIT
        val next = nextSectionLimit(current).coerceAtMost(maxOf(INITIAL_SECTION_LIMIT, totalCount))
        _sectionLimits.value = _sectionLimits.value + (groupId to next)
    }

    private fun nextSectionLimit(current: Int): Int {
        if (current >= MAX_SECTION_LIMIT) return INITIAL_SECTION_LIMIT
        return when (current) {
            8 -> 16
            16 -> 32
            32 -> 60
            else -> INITIAL_SECTION_LIMIT
        }
    }

    companion object {
        private const val INITIAL_SECTION_LIMIT = 8
        private const val MAX_SECTION_LIMIT = 60
    }

    fun setTimelineScale(scale: TimelineSubScale) {
        _timelineScale.value = scale
    }

    fun setScheduleView(view: String) {
        if (_scheduleView.value == view) return
        _scheduleView.value = view
        userSettingsRepository.setScheduleView(view)
    }

    fun setCustomRange(startIso: String?, endIso: String?) {
        _customStartIso.value = startIso
        _customEndIso.value = endIso
    }

    fun setDeleteTileCandidate(id: String?) {
        _requestDeleteTileId.value = id
    }

    fun confirmDeleteTile() {
        val id = _requestDeleteTileId.value ?: return
        _requestDeleteTileId.value = null
        deleteTile(id)
    }

    /**
     * Format an ISO instant as a short, locale-aware date-time string.
     * Exposed so the [app.tastile.android.ui.mobile.tabs.tiles] sub-tab
     * bodies can render the same labels the rest of the app uses without
     * re-importing the underlying [formatIsoDateTime] helper.
     */
    fun formatIsoDateTime(iso: String?, locale: AppLocale): String =
        formatIsoDateTime(iso, locale, zone = ZoneId.systemDefault())

    private fun rebuildTileFilter() {
        val current = _tileFilter.value
        val range = when (_filterRange.value) {
            TileRange.ALL -> null
            TileRange.TODAY -> "today"
            TileRange.RECENT -> "recent"
            TileRange.EXCLUDE_FUTURE -> "exclude_future"
        }
        val granularity = when (_filterGranularity.value) {
            TileGranularity.ALL -> null
            TileGranularity.NO_BREAKS -> "no_breaks"
            TileGranularity.MIN_5M -> "min_5m"
            TileGranularity.MIN_15M -> "min_15m"
            TileGranularity.MIN_30M -> "min_30m"
        }
        val search = _searchTerm.value.trim().takeIf { it.isNotBlank() }
        val excludeFuture = _filterRange.value == TileRange.EXCLUDE_FUTURE
        setTileFilter(
            current.copy(
                viewMode = "list",
                limit = _filterLimit.value.coerceAtLeast(0),
                search = search ?: current.search,
                excludeFuture = excludeFuture,
                range = range ?: current.range,
                granularity = granularity ?: current.granularity,
            )
        )
    }

    init {
        viewModelScope.launch {
            combine(_selectedDay, _scale, _calendarMode) { d, s, m ->
                calendarRange(d, s, m)
            }
                .distinctUntilChanged()
                .collect { range ->
                    _timelineRange.value = range
                    refreshTimeline()
                }
        }
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is TastileAuthState.Authenticated) {
                    refreshTimeline()
                }
            }
        }
        viewModelScope.launch {
            combine(authRepository.authState, _tileFilter) { state, filter -> state to filter }
                .distinctUntilChanged()
                .collect { (state, filter) ->
                    val userId = (state as? TastileAuthState.Authenticated)?.userId
                    if (userId != null) {
                        val response = tileRepository.getTiles(filter)
                        _tiles.value = response.tiles
                        _nextActionableTileId.value = response.nextActionableTileId
                        _nextActionableStartAt.value = response.nextActionableStartAt
                        _statsDiagnostics.value = tileRepository.latestReadDiagnostics()
                    } else {
                        _tiles.value = emptyList()
                        _nextActionableTileId.value = null
                        _nextActionableStartAt.value = null
                        _statsDiagnostics.value = "source=none reason=unauthenticated"
                    }
                }
        }
        refreshAll()
    }

    private fun refreshTimeline() {
        val (start, end) = _timelineRange.value
        viewModelScope.launch {
            _isLoadingTimeline.value = true
            try {
                _timeline.value = filterCalendarByMinimumDuration(
                    tileRepository.getTimeline(start, end, _tileFilter.value.ownerIds),
                    _calendarMinimumDurationMinutes.value,
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load timeline"
            } finally {
                _isLoadingTimeline.value = false
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val authState = authRepository.authState.value as? TastileAuthState.Authenticated
                val legacySession = authRepository.currentSession
                val userId = authState?.userId ?: legacySession.readNestedString("user", "id")
                _email.value = authState?.email ?: legacySession.readNestedString("user", "email").orEmpty()
                val metadataAvatar: String? = null
                if (userId != null) {
                    _profile.value = profileRepository.getProfile(userId)
                    _avatarUrl.value = metadataAvatar ?: _profile.value?.avatarUrl
                    val (tlStart, tlEnd) = _timelineRange.value
                    _timeline.value = filterCalendarByMinimumDuration(
                        tileRepository.getTimeline(tlStart, tlEnd, _tileFilter.value.ownerIds),
                        _calendarMinimumDurationMinutes.value,
                    )
                } else {
                    _timeline.value = emptyList()
                    _profile.value = null
                    _avatarUrl.value = null
                }
            } catch (e: Exception) {
                _statsDiagnostics.value = "source=error reason=${e.javaClass.simpleName}"
                _error.value = e.message ?: "Failed to load dashboard data"
            } finally {
                _loading.value = false
            }
        }
    }

    fun createTile(draft: CreateTileDraft) {
        if (draft.title.isBlank()) return
        viewModelScope.launch {
            val userId = authRepository.currentUserId() ?: return@launch
            try {
                tileRepository.createTile(
                    userId = userId,
                    payload = buildCreatePayload(
                        draft.copy(
                            title = draft.title.trim(),
                            nextAction = draft.nextAction?.ifBlank { null },
                            doneDefinition = draft.doneDefinition?.ifBlank { null },
                            labels = draft.labels.filter { it.isNotBlank() },
                            project = draft.project?.ifBlank { null },
                            memo = draft.memo?.ifBlank { null }
                        )
                    )
                )
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create tile"
            }
        }
    }

    fun startTile(tileId: String) {
        viewModelScope.launch {
            try {
                tileRepository.startTile(tileId)
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start tile"
            }
        }
    }

    fun completeTile(tileId: String) {
        viewModelScope.launch {
            try {
                tileRepository.completeTile(tileId)
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to complete tile"
            }
        }
    }

    fun pauseTile(tileId: String) {
        viewModelScope.launch {
            try {
                tileRepository.pauseTile(tileId)
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to pause execution"
            }
        }
    }

    fun resumeTile(tileId: String) {
        viewModelScope.launch {
            try {
                tileRepository.continueTile(tileId)
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to resume execution"
            }
        }
    }

    fun deferTile(tileId: String) {
        viewModelScope.launch {
            try {
                tileRepository.deferTile(tileId)
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to defer tile"
            }
        }
    }

    fun deleteTile(tileId: String) {
        viewModelScope.launch {
            try {
                tileRepository.deleteTile(tileId)
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete tile"
            }
        }
    }

    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId() ?: return@launch
                _profile.value = profileRepository.updateDisplayName(userId, trimmed)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update profile"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                _error.value = null
                authRepository.signOut()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to sign out"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun buildExecuteCards(): List<DashboardCardModel> = cardMapper.buildExecuteCards(_tiles.value)

    fun buildTileCards(): List<DashboardCardModel> = cardMapper.buildTileCards(_tiles.value)

    fun handleCardAction(action: CardAction) {
        when (action) {
            is CardAction.TriggerPrompt -> triggerPrompt(action.tileId)
            is CardAction.StartTile -> startTile(action.tileId)
            is CardAction.CompleteTile -> completeTile(action.tileId)
            is CardAction.DeferTile -> deferTile(action.tileId)
            is CardAction.DeleteTile -> deleteTile(action.tileId)
        }
    }

    private fun triggerPrompt(tileId: String) {
        viewModelScope.launch {
            try {
                tileRepository.requestPrompt(tileId)
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to trigger prompt"
            }
        }
    }

    fun rescheduleTimelineItem(item: CoreTimelineItem, minuteOffset: Int, zoomScale: Float) {
        viewModelScope.launch {
            try {
                val snapMin = snapByZoom(zoomScale)
                val start = parseIsoOrNull(item.startAt) ?: return@launch
                val end = parseIsoOrNull(item.endAt ?: item.startAt)?.let { parsed ->
                    if (parsed.isAfter(start)) parsed else start.plusSeconds(60)
                } ?: start.plusSeconds(30 * 60)
                val durationSeconds = Duration.between(start, end).seconds
                val movedStart = start.plusSeconds(minuteOffset.toLong() * 60L)
                val movedStartMin = movedStart.epochSecond / 60L
                val snappedStartMin = (movedStartMin / snapMin) * snapMin
                val snappedStart = Instant.ofEpochSecond(snappedStartMin * 60L)
                val snappedEnd = snappedStart.plusSeconds(durationSeconds)
                val tileId = item.tileId ?: return@launch
                tileRepository.rescheduleTile(
                    tileId = tileId,
                    startAtIso = snappedStart.toString(),
                    endAtIso = snappedEnd.toString()
                )
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to reschedule timeline item"
            }
        }
    }

    fun createTimelineTile(title: String, startAtIso: String, endAtIso: String) {
        createTile(
            CreateTileDraft(
                title = title,
                useStartAt = true,
                useEndAt = true,
                startAtIso = startAtIso,
                endAtIso = endAtIso
            )
        )
    }

    fun deleteTimelineTile(tileId: String) {
        deleteTile(tileId)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        userSettingsRepository.setThemeMode(mode)
    }

    fun setLocale(locale: AppLocale) {
        _locale.value = locale
        userSettingsRepository.setLocale(locale)
    }

    fun setSecurityLockEnabled(enabled: Boolean) {
        _securityLockEnabled.value = enabled
        userSettingsRepository.setSecurityLockEnabled(enabled)
    }

    fun setSecurityLockTimeoutMinutes(minutes: Int) {
        val normalized = minutes.coerceIn(1, 240)
        _securityLockTimeoutMinutes.value = normalized
        userSettingsRepository.setSecurityLockTimeoutMinutes(normalized)
    }

    private fun buildCreatePayload(draft: CreateTileDraft) = buildJsonObject {
        val recurrenceObject = if (draft.objectiveMode == "recurring") {
            buildJsonObject {
                put("generator", buildJsonObject {
                    put(
                        "step_min",
                        JsonPrimitive(
                            when (draft.recurrenceFrequency) {
                                "weekly" -> 7 * 24 * 60
                                "monthly" -> 30 * 24 * 60
                                else -> 24 * 60
                            }
                        )
                    )
                    put("anchor_epoch_min", JsonNull)
                })
                put("window", buildJsonObject {
                    put("start_offset_min", JsonPrimitive(parseTimeToMin(draft.recurrenceStartTime)))
                    put("end_offset_min", JsonPrimitive(parseTimeToMin(draft.recurrenceEndTime)))
                })
                put("selector", buildJsonObject {
                    put(
                        "expression",
                        JsonPrimitive(
                            buildRecurrenceExpression(
                                frequency = draft.recurrenceFrequency,
                                interval = draft.recurrenceInterval,
                                weekdays = draft.recurrenceWeekdays,
                                monthlyWeek = draft.recurrenceMonthlyWeek,
                                monthlyWeekday = draft.recurrenceMonthlyWeekday
                            )
                        )
                    )
                })
            }
        } else {
            JsonNull
        }

        val mergedLabels = buildList {
            draft.project?.takeIf { it.isNotBlank() }?.let { add("project:$it") }
            addAll(draft.labels.filter { it.isNotBlank() })
        }

        put("title", JsonPrimitive(draft.title))
        put("next_action", (draft.memo ?: draft.nextAction)?.let { JsonPrimitive(it) } ?: JsonNull)
        put("done_definition", draft.doneDefinition?.let { JsonPrimitive(it) } ?: JsonNull)
        put("temporal", buildJsonObject {
            put("release_at", draft.recurrenceValidFromIso?.let { JsonPrimitive(it) } ?: JsonNull)
            put("due_at", draft.recurrenceValidToIso?.let { JsonPrimitive(it) } ?: JsonNull)
            put("fixed_start", if (draft.useStartAt) draft.startAtIso?.let { JsonPrimitive(it) } ?: JsonNull else JsonNull)
            put("fixed_end", if (draft.useEndAt) draft.endAtIso?.let { JsonPrimitive(it) } ?: JsonNull else JsonNull)
            put("active_start", if (draft.useStartAt) draft.startAtIso?.let { JsonPrimitive(it) } ?: JsonNull else JsonNull)
            put("active_end", if (draft.useEndAt) draft.endAtIso?.let { JsonPrimitive(it) } ?: JsonNull else JsonNull)
        })
        put("objective", buildJsonObject {
            put("objective_mode", JsonPrimitive(draft.objectiveMode))
            put("target_work_min", draft.targetWorkMin?.let { JsonPrimitive(it) } ?: JsonNull)
            put("target_rest_min", JsonNull)
            put("done_rule", JsonPrimitive("manual"))
            put("recurrence", recurrenceObject)
        })
        put("interruption", buildJsonObject {
            put("interrupt_penalty", JsonPrimitive(3))
            put("resume_penalty", JsonPrimitive(3))
            put("break_splits_work", JsonPrimitive(draft.breakSplitsWork))
            put("external_interrupt_only", JsonPrimitive(false))
        })
        put("automation", buildJsonObject {
            put("prompt_on_start", JsonPrimitive(false))
            put("prompt_on_end", JsonPrimitive(true))
            put("auto_start_allowed", JsonPrimitive(false))
            put("auto_end_allowed", JsonPrimitive(false))
        })
        put("annotation", buildJsonObject {
            put("semantic_role", JsonPrimitive(draft.tileKind))
            put("labels", kotlinx.serialization.json.buildJsonArray { mergedLabels.forEach { add(JsonPrimitive(it)) } })
            put("timed_labels", kotlinx.serialization.json.buildJsonArray { })
        })
    }

}

private fun extractAvatarUrlFromMetadata(metadata: JsonObject): String? {
    val direct = listOf("avatar_url", "picture", "photo_url", "avatar")
        .firstNotNullOfOrNull { key -> metadata[key]?.jsonPrimitive?.contentOrNull }
    if (!direct.isNullOrBlank()) return direct

    val identities = metadata["identities"]?.jsonArray ?: return null
    return identities.firstNotNullOfOrNull { identityElement ->
        val identity = runCatching { identityElement.jsonObject }.getOrNull() ?: return@firstNotNullOfOrNull null
        val identityData = identity["identity_data"]?.jsonObject ?: return@firstNotNullOfOrNull null
        listOf("avatar_url", "picture", "photo_url")
            .firstNotNullOfOrNull { key -> identityData[key]?.jsonPrimitive?.contentOrNull }
    }
}

private fun Any?.readNestedString(vararg propertyNames: String): String? {
    var current: Any? = this
    for (propertyName in propertyNames) {
        current = current?.javaClass?.methods
            ?.firstOrNull { method ->
                method.parameterCount == 0 &&
                    method.name.equals(
                        "get${propertyName.replaceFirstChar { it.uppercase() }}",
                        ignoreCase = true
                    )
            }
            ?.invoke(current)
    }
    return current as? String
}

private fun parseIsoOrNull(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        Instant.parse(value)
    } catch (_: Exception) {
        null
    }
}

internal fun computeTimelineRange(day: LocalDate, scale: TimelineScale): Pair<Instant, Instant> {
    return calendarRange(day, scale, CalendarMode.Scope)
}

private fun snapByZoom(zoomScale: Float): Long {
    return when {
        zoomScale >= 2.5f -> 1L
        zoomScale >= 1.4f -> 5L
        else -> 15L
    }
}

private fun parseTimeToMin(value: String?): Int {
    if (value.isNullOrBlank() || !value.contains(":")) return 0
    val parts = value.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return (h * 60 + m).coerceIn(0, 24 * 60)
}

private fun buildRecurrenceExpression(
    frequency: String,
    interval: Int,
    weekdays: List<Int>,
    monthlyWeek: Int,
    monthlyWeekday: Int
): String {
    return when (frequency) {
        "weekly" -> "weekly:${interval.coerceAtLeast(1)}:${weekdays.sorted().joinToString(",")}"
        "monthly" -> "monthly:${interval.coerceAtLeast(1)}:${monthlyWeek.coerceAtLeast(1)}:${monthlyWeekday.coerceIn(0, 6)}"
        else -> "daily:${interval.coerceAtLeast(1)}"
    }
}
