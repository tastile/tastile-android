package app.tastile.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.model.Profile
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
    private val tileRepository: TileRepository,
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

    init {
        refreshAll()
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
                    _tiles.value = tileRepository.getTiles(userId)
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
                tileRepository.pauseTile(tileId)
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
