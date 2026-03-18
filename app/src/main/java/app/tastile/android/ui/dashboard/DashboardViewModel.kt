package app.tastile.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.model.Profile
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.EventRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject

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
    private val eventRepository: EventRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {
    private val _tiles = MutableStateFlow<List<Tile>>(emptyList())
    val tiles: StateFlow<List<Tile>> = _tiles.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _themeMode = MutableStateFlow(userSettingsRepository.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _locale = MutableStateFlow(userSettingsRepository.getLocale())
    val locale: StateFlow<AppLocale> = _locale.asStateFlow()

    private val openSegments = mutableMapOf<String, String>()
    private var activeTileId: String? = null

    init {
        refreshAll()
        startPollingSync()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val session = authRepository.currentSession
                val userId = session?.user?.id
                _email.value = session?.user?.email.orEmpty()
                if (userId != null) {
                    rebuildStateFromEvents(userId)
                    _profile.value = profileRepository.getProfile(userId)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load dashboard data"
            } finally {
                _loading.value = false
            }
        }
    }

    fun createTile(draft: CreateTileDraft) {
        if (draft.title.isBlank()) return
        viewModelScope.launch {
            val userId = authRepository.currentSession?.user?.id ?: return@launch
            try {
                val tileId = UUID.randomUUID().toString()
                val tilePayload = buildTilePayload(
                    tileId = tileId,
                    draft = draft.copy(
                        title = draft.title.trim(),
                        nextAction = draft.nextAction?.ifBlank { null },
                        doneDefinition = draft.doneDefinition?.ifBlank { null },
                        labels = draft.labels.filter { it.isNotBlank() },
                        project = draft.project?.ifBlank { null },
                        memo = draft.memo?.ifBlank { null }
                    )
                )
                appendEvent(userId, "tile:$tileId", "tile_created", buildJsonObject { put("tile", tilePayload) })
                rebuildStateFromEvents(userId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create tile"
            }
        }
    }

    fun startTile(tileId: String) {
        viewModelScope.launch {
            val userId = authRepository.currentSession?.user?.id ?: return@launch
            try {
                val now = nowIso()
                val segmentId = UUID.randomUUID().toString()
                appendEvent(userId, "tile:$tileId", "tile_started", buildJsonObject {
                    put("tile_id", JsonPrimitive(tileId))
                    put("started_at", JsonPrimitive(now))
                })
                appendEvent(userId, "tile:$tileId", "segment_started", buildJsonObject {
                    put("segment_id", JsonPrimitive(segmentId))
                    put("tile_id", JsonPrimitive(tileId))
                    put("mode", JsonPrimitive("work"))
                    put("started_at", JsonPrimitive(now))
                })
                rebuildStateFromEvents(userId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start tile"
            }
        }
    }

    fun completeTile(tileId: String) {
        viewModelScope.launch {
            val userId = authRepository.currentSession?.user?.id ?: return@launch
            try {
                val now = nowIso()
                val openSegmentId = openSegments[tileId]
                if (!openSegmentId.isNullOrBlank()) {
                    appendEvent(userId, "tile:$tileId", "segment_ended", buildJsonObject {
                        put("segment_id", JsonPrimitive(openSegmentId))
                        put("tile_id", JsonPrimitive(tileId))
                        put("mode", JsonPrimitive("work"))
                        put("ended_at", JsonPrimitive(now))
                    })
                }
                appendEvent(userId, "tile:$tileId", "tile_completed", buildJsonObject {
                    put("tile_id", JsonPrimitive(tileId))
                    put("completed_at", JsonPrimitive(now))
                })
                rebuildStateFromEvents(userId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to complete tile"
            }
        }
    }

    fun deferTile(tileId: String) {
        viewModelScope.launch {
            val userId = authRepository.currentSession?.user?.id ?: return@launch
            try {
                appendEvent(userId, "tile:$tileId", "tile_deferred", buildJsonObject {
                    put("tile_id", JsonPrimitive(tileId))
                    put("deferred_at", JsonPrimitive(nowIso()))
                    put("next_start_at", JsonNull)
                })
                rebuildStateFromEvents(userId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to defer tile"
            }
        }
    }

    fun deleteTile(tileId: String) {
        viewModelScope.launch {
            val userId = authRepository.currentSession?.user?.id ?: return@launch
            try {
                appendEvent(userId, "tile:$tileId", "tile_deleted", buildJsonObject {
                    put("tile_id", JsonPrimitive(tileId))
                    put("deleted_at", JsonPrimitive(nowIso()))
                })
                rebuildStateFromEvents(userId)
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
                val userId = authRepository.currentSession?.user?.id ?: return@launch
                _profile.value = profileRepository.updateDisplayName(userId, trimmed)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update profile"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        userSettingsRepository.setThemeMode(mode)
    }

    fun setLocale(locale: AppLocale) {
        _locale.value = locale
        userSettingsRepository.setLocale(locale)
    }

    private fun startPollingSync() {
        viewModelScope.launch {
            while (isActive) {
                val userId = authRepository.currentSession?.user?.id
                if (userId != null) runCatching { rebuildStateFromEvents(userId) }
                delay(5000)
            }
        }
    }

    private suspend fun appendEvent(userId: String, aggregateId: String, eventType: String, payload: JsonObject) {
        eventRepository.append(
            userId = userId,
            aggregateId = aggregateId,
            eventType = eventType,
            payload = payload,
            occurredAtIso = nowIso()
        )
    }

    private suspend fun rebuildStateFromEvents(userId: String) {
        val rows = eventRepository.loadAll(userId)
        val map = linkedMapOf<String, Tile>()
        openSegments.clear()
        activeTileId = null

        for (row in rows) {
            val payload = row.eventPayload ?: row.payloadJson ?: continue
            when (row.eventType) {
                "tile_created" -> {
                    parseTileFromPayload(payload)?.let { map[it.id] = it }
                }
                "tile_started" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    val tile = map[tileId] ?: continue
                    map[tileId] = tile.copy(lifecycle = TileLifecycle.STARTED.value)
                    activeTileId = tileId
                }
                "segment_started" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    val segmentId = payload.string("segment_id") ?: continue
                    openSegments[tileId] = segmentId
                }
                "segment_ended" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    openSegments.remove(tileId)
                }
                "tile_completed" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    val tile = map[tileId] ?: continue
                    map[tileId] = tile.copy(lifecycle = TileLifecycle.DONE.value)
                    if (activeTileId == tileId) activeTileId = null
                }
                "tile_deferred" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    val tile = map[tileId] ?: continue
                    map[tileId] = tile.copy(lifecycle = TileLifecycle.READY.value)
                    if (activeTileId == tileId) activeTileId = null
                }
                "tile_deleted" -> {
                    val tileId = payload.string("tile_id") ?: continue
                    map.remove(tileId)
                    openSegments.remove(tileId)
                    if (activeTileId == tileId) activeTileId = null
                }
            }
        }

        _tiles.value = map.values.reversed()
    }

    private fun parseTileFromPayload(payload: JsonObject): Tile? {
        val tile = payload.obj("tile") ?: return null
        val core = tile.obj("core")
        val annotation = tile.obj("annotation")
        val objective = tile.obj("objective")
        val temporal = tile.obj("temporal")
        val interruption = tile.obj("interruption")
        val automation = tile.obj("automation")
        val work = tile.obj("work")

        val tileId = core?.string("id") ?: return null
        val title = core.string("title") ?: return null
        val labels = annotation?.array("labels") ?: JsonArray(emptyList())

        return Tile(
            id = tileId,
            userId = "",
            localTileId = tileId,
            title = title,
            nextAction = core.string("nextAction"),
            doneDefinition = core.string("doneDefinition"),
            temporalConditions = temporal,
            objectiveConditions = objective,
            interruptionConditions = interruption,
            automationConditions = automation,
            lifecycle = TileLifecycle.READY.value,
            annotationConditions = buildJsonObject {
                put("semanticRole", annotation?.string("semanticRole")?.let { JsonPrimitive(it) } ?: JsonPrimitive("work"))
                put("labels", labels)
                put("timedLabels", annotation?.array("timedLabels") ?: JsonArray(emptyList()))
                put("segments", work?.array("segments") ?: JsonArray(emptyList()))
            },
            createdAt = nowIso(),
            updatedAt = nowIso(),
            localCreatedAt = nowIso(),
            localUpdatedAt = nowIso(),
            deletedAt = null
        )
    }

    private fun buildTilePayload(tileId: String, draft: CreateTileDraft): JsonObject {
        val recurrenceObject = if (draft.objectiveMode == "recurring") {
            buildJsonObject {
                put("generator", buildJsonObject {
                    put(
                        "stepMin",
                        JsonPrimitive(
                            when (draft.recurrenceFrequency) {
                                "weekly" -> 7 * 24 * 60
                                "monthly" -> 30 * 24 * 60
                                else -> 24 * 60
                            }
                        )
                    )
                    put("anchorEpochMin", JsonNull)
                })
                put("window", buildJsonObject {
                    put("startOffsetMin", JsonPrimitive(parseTimeToMin(draft.recurrenceStartTime)))
                    put("endOffsetMin", JsonPrimitive(parseTimeToMin(draft.recurrenceEndTime)))
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

        return buildJsonObject {
            put("core", buildJsonObject {
                put("id", JsonPrimitive(tileId))
                put("title", JsonPrimitive(draft.title))
                put("nextAction", (draft.memo ?: draft.nextAction)?.let { JsonPrimitive(it) } ?: JsonNull)
                put("doneDefinition", draft.doneDefinition?.let { JsonPrimitive(it) } ?: JsonNull)
                put("startedAt", JsonNull)
                put("completedAt", JsonNull)
            })
            put("work", buildJsonObject { put("segments", buildJsonArray {}) })
            put("temporal", buildJsonObject {
                put("releaseAt", draft.recurrenceValidFromIso?.let { JsonPrimitive(it) } ?: JsonNull)
                put("dueAt", draft.recurrenceValidToIso?.let { JsonPrimitive(it) } ?: JsonNull)
                put("fixedStart", if (draft.useStartAt) draft.startAtIso?.let { JsonPrimitive(it) } ?: JsonNull else JsonNull)
                put("fixedEnd", if (draft.useEndAt) draft.endAtIso?.let { JsonPrimitive(it) } ?: JsonNull else JsonNull)
                put("activeStart", if (draft.useStartAt) draft.startAtIso?.let { JsonPrimitive(it) } ?: JsonNull else JsonNull)
                put("activeEnd", if (draft.useEndAt) draft.endAtIso?.let { JsonPrimitive(it) } ?: JsonNull else JsonNull)
            })
            put("objective", buildJsonObject {
                put("objectiveMode", JsonPrimitive(draft.objectiveMode))
                put("targetWorkMin", draft.targetWorkMin?.let { JsonPrimitive(it) } ?: JsonNull)
                put("targetRestMin", JsonNull)
                put("doneRule", JsonPrimitive("manual"))
                put("recurrence", recurrenceObject)
            })
            put("interruption", buildJsonObject {
                put("interruptPenalty", JsonPrimitive(3)); put("resumePenalty", JsonPrimitive(3))
                put("breakSplitsWork", JsonPrimitive(draft.breakSplitsWork)); put("externalInterruptOnly", JsonPrimitive(false))
            })
            put("automation", buildJsonObject {
                put("promptOnStart", JsonPrimitive(false)); put("promptOnEnd", JsonPrimitive(true))
                put("autoStartAllowed", JsonPrimitive(false)); put("autoEndAllowed", JsonPrimitive(false))
            })
            put("annotation", buildJsonObject {
                put("semanticRole", JsonPrimitive(draft.tileKind))
                put("labels", buildJsonArray { mergedLabels.forEach { add(JsonPrimitive(it)) } })
                put("timedLabels", buildJsonArray {})
            })
        }
    }

    private fun nowIso(): String = Clock.System.now().toString()
}

private fun JsonObject?.string(key: String): String? {
    return this?.get(key)?.jsonPrimitive?.contentOrNull
}

private fun JsonObject?.obj(key: String): JsonObject? {
    return this?.get(key) as? JsonObject
}

private fun JsonObject?.array(key: String): JsonArray? {
    return this?.get(key) as? JsonArray
}

fun Tile.isStarted(): Boolean = lifecycle == TileLifecycle.STARTED.value
fun Tile.isDone(): Boolean = lifecycle == TileLifecycle.DONE.value

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
