package app.tastile.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.model.Integration
import app.tastile.android.data.model.Profile
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.CalendarProjectionResponse
import app.tastile.android.data.repository.CalendarSyncPlanPreviewResponse
import app.tastile.android.data.repository.GoogleCalendarIntegrationSettings
import app.tastile.android.data.repository.IntegrationRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

enum class TimelineScale { Day, Week, Month }

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
    private val integrationRepository: IntegrationRepository
) : ViewModel() {
    private val cardMapper = DashboardCardMapper()
    private val _tiles = MutableStateFlow<List<Tile>>(emptyList())
    val tiles: StateFlow<List<Tile>> = _tiles.asStateFlow()

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

    private val _timelineRange = MutableStateFlow(
        computeTimelineRange(LocalDate.now(), TimelineScale.Day)
    )
    val timelineRange: StateFlow<Pair<Instant, Instant>> = _timelineRange.asStateFlow()
    private val _googleCalendarIntegration = MutableStateFlow<GoogleCalendarIntegrationSettings?>(null)
    val googleCalendarIntegration: StateFlow<GoogleCalendarIntegrationSettings?> = _googleCalendarIntegration.asStateFlow()

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

    val integrations: StateFlow<List<Integration>> = googleCalendarIntegration
        .map { gc ->
            listOf(
                Integration(
                    id = "google_calendar",
                    name = "Calendar",
                    connected = gc?.connected == true,
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _statsDiagnostics = MutableStateFlow("n/a")
    val statsDiagnostics: StateFlow<String> = _statsDiagnostics.asStateFlow()
    private val _daemonStatusSummary = MutableStateFlow("daemon=n/a")
    val daemonStatusSummary: StateFlow<String> = _daemonStatusSummary.asStateFlow()
    private val _calendarMonthProjection = MutableStateFlow<CalendarProjectionResponse?>(null)
    val calendarMonthProjection: StateFlow<CalendarProjectionResponse?> = _calendarMonthProjection.asStateFlow()
    private val _calendarSyncPlanPreview = MutableStateFlow<CalendarSyncPlanPreviewResponse?>(null)
    val calendarSyncPlanPreview: StateFlow<CalendarSyncPlanPreviewResponse?> = _calendarSyncPlanPreview.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_selectedDay, _scale) { d, s -> computeTimelineRange(d, s) }
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
        refreshAll()
    }

    private fun refreshTimeline() {
        val (start, end) = _timelineRange.value
        viewModelScope.launch {
            try {
                _timeline.value = tileRepository.getTimeline(start, end)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load timeline"
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
                    _tiles.value = tileRepository.getTiles(userId)
                    _profile.value = profileRepository.getProfile(userId)
                    _avatarUrl.value = metadataAvatar ?: _profile.value?.avatarUrl
                    val (tlStart, tlEnd) = _timelineRange.value
                    _timeline.value = tileRepository.getTimeline(tlStart, tlEnd)
                    _googleCalendarIntegration.value = integrationRepository.getSettings().googleCalendar
                    runCatching {
                        integrationRepository.getCalendarMonthProjection()
                    }.onSuccess { projection ->
                        _calendarMonthProjection.value = projection
                    }.onFailure { calendarError ->
                        _calendarMonthProjection.value = null
                        _error.value = "Calendar sync unavailable showing fallback timeline"
                        _statsDiagnostics.value = "calendar_projection_error=${calendarError.javaClass.simpleName}"
                    }
                    _calendarSyncPlanPreview.value = runCatching {
                        integrationRepository.getCalendarSyncPlanPreview()
                    }.getOrNull()
                    val daemon = integrationRepository.lastSuccessfulDaemonBaseUrl() ?: "unresolved"
                    _statsDiagnostics.value = "${tileRepository.latestReadDiagnostics()} daemon=$daemon"
                    refreshDaemonStatusInternal()
                } else {
                    _tiles.value = emptyList()
                    _timeline.value = emptyList()
                    _profile.value = null
                    _avatarUrl.value = null
                    _googleCalendarIntegration.value = null
                    _calendarMonthProjection.value = null
                    _calendarSyncPlanPreview.value = null
                    _statsDiagnostics.value = "source=none reason=unauthenticated"
                    _daemonStatusSummary.value = "daemon=unauthenticated"
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

    fun connectGoogleCalendar() {
        viewModelScope.launch {
            try {
                _googleCalendarIntegration.value =
                    integrationRepository.updateGoogleCalendarConnected(true).googleCalendar
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to connect google calendar"
            }
        }
    }

    fun disconnectGoogleCalendar() {
        viewModelScope.launch {
            try {
                _googleCalendarIntegration.value =
                    integrationRepository.updateGoogleCalendarConnected(false).googleCalendar
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to disconnect google calendar"
            }
        }
    }

    fun syncGoogleCalendarNow() {
        viewModelScope.launch {
            try {
                integrationRepository.triggerSync()
                _googleCalendarIntegration.value =
                    integrationRepository.markGoogleCalendarSyncedNow().googleCalendar
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to sync google calendar"
            }
        }
    }

    fun updateGoogleCalendarPolicy(syncMode: String, selectedCalendarId: String?) {
        viewModelScope.launch {
            try {
                _googleCalendarIntegration.value = integrationRepository.updateGoogleCalendarIntegration(
                    syncMode = syncMode,
                    selectedCalendarId = selectedCalendarId
                ).googleCalendar
                _calendarSyncPlanPreview.value = integrationRepository.getCalendarSyncPlanPreview()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update google calendar policy"
            }
        }
    }

    fun triggerDaemonTick() {
        viewModelScope.launch {
            try {
                integrationRepository.triggerTick()
                refreshDaemonStatusInternal()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to trigger daemon tick"
            }
        }
    }

    fun resetLocalSyncData() {
        viewModelScope.launch {
            try {
                val res = integrationRepository.resetLocalSyncData()
                _daemonStatusSummary.value = "recovery=reset-local applied=${res.applied} ok=${res.ok}"
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to reset local sync data"
            }
        }
    }

    fun redownloadRemoteSyncData() {
        viewModelScope.launch {
            try {
                val res = integrationRepository.redownloadRemoteSyncData()
                _daemonStatusSummary.value = "recovery=redownload-remote applied=${res.applied} ok=${res.ok}"
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to redownload remote sync data"
            }
        }
    }

    fun refreshDaemonStatus() {
        viewModelScope.launch {
            try {
                refreshDaemonStatusInternal()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to refresh daemon status"
            }
        }
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
            CardAction.StartBreak -> startBreak()
            CardAction.EndBreak -> endBreak()
            is CardAction.ExtendTile -> extendTile(action.minutes)
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

    private fun startBreak() {
        viewModelScope.launch {
            try {
                tileRepository.startBreak(breakMin = 5)
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start break"
            }
        }
    }

    private fun endBreak() {
        viewModelScope.launch {
            try {
                tileRepository.endBreak()
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to end break"
            }
        }
    }

    private fun extendTile(minutes: Int) {
        viewModelScope.launch {
            try {
                tileRepository.extendTile(minutes)
                refreshAll()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to extend tile"
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

    private suspend fun refreshDaemonStatusInternal() {
        val syncStatus = integrationRepository.getSyncStatus()
        val runtimePaths = integrationRepository.getRuntimePaths()
        val quota = integrationRepository.getTileQuota()
        val syncSummary = if (syncStatus.running) "running" else "idle"
        _daemonStatusSummary.value = buildString {
            append("sync=$syncSummary")
            syncStatus.lastSuccessAt?.let { append(" last_success=$it") }
            if (!syncStatus.lastError.isNullOrBlank()) append(" last_error=${syncStatus.lastError}")
            append(" quota=${quota.tileCount}/${quota.maxTiles}")
            append(" profile=${runtimePaths.profileName}")
        }
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
    val zone = ZoneId.systemDefault()
    return when (scale) {
        TimelineScale.Day -> {
            val start = day.atStartOfDay(zone).toInstant()
            start to start.plusSeconds(24L * 3600L)
        }
        TimelineScale.Week -> {
            val monday = day.minusDays((day.dayOfWeek.value - 1).toLong())
            val start = monday.atStartOfDay(zone).toInstant()
            start to start.plusSeconds(7L * 24L * 3600L)
        }
        TimelineScale.Month -> {
            val ym = YearMonth.from(day)
            val start = ym.atDay(1).atStartOfDay(zone).toInstant()
            start to ym.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant()
        }
    }
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
