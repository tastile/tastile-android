package app.tastile.android.data.repository

import app.tastile.android.data.model.Tile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TilesResponse(
    val tiles: List<Tile>,
    @SerialName("next_actionable_tile_id") val nextActionableTileId: String? = null,
    @SerialName("next_actionable_start_at") val nextActionableStartAt: String? = null
)

@Serializable
data class ExecutionViewResponse(
    @SerialName("tiles_in_progress") val tilesInProgress: List<Tile>,
    @SerialName("main_tile") val mainTile: Tile? = null,
    @SerialName("is_working") val isWorking: Boolean = false,
    @SerialName("is_on_break") val isOnBreak: Boolean = false,
    @SerialName("is_idle") val isIdle: Boolean = true,
    @SerialName("main_tile_started_at") val mainTileStartedAt: String? = null,
    @SerialName("main_tile_ends_at") val mainTileEndsAt: String? = null,
    @SerialName("pending_prompt_id") val pendingPromptId: String? = null,
    @SerialName("tile_count") val tileCount: Int = 0,
    @SerialName("event_count") val eventCount: Int = 0
)

@Serializable
data class TilesInProgressResponse(
    val tiles: List<Tile>,
    val count: Int
)

@Serializable
data class ActiveTileResponse(
    val tile: Tile? = null,
    val phase: String = "idle",
    @SerialName("phase_started_at") val phaseStartedAt: String? = null,
    @SerialName("phase_ends_at") val phaseEndsAt: String? = null,
    @SerialName("resume_note") val resumeNote: String? = null,
    @SerialName("next_visible_action") val nextVisibleAction: String? = null
)

@Serializable
data class ExecutionResponse(
    @SerialName("active_tile_id") val activeTileId: String? = null,
    @SerialName("phase_kind") val phaseKind: String = "idle",
    @SerialName("phase_started_at") val phaseStartedAt: String? = null,
    @SerialName("phase_ends_at") val phaseEndsAt: String? = null,
    @SerialName("pending_prompt_id") val pendingPromptId: String? = null,
    @SerialName("tile_count") val tileCount: Int = 0,
    @SerialName("event_count") val eventCount: Int = 0
)

@Serializable
data class PendingPromptResponse(
    val prompt: PromptViewResponse? = null
)

@Serializable
data class PromptViewResponse(
    @SerialName("prompt_id") val promptId: String,
    val kind: String,
    val severity: String? = null,
    @SerialName("tile_id") val tileId: String? = null,
    val title: String,
    val body: String,
    val why: String,
    @SerialName("suggested_minutes") val suggestedMinutes: Int? = null,
    val actions: List<PromptActionViewResponse> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val stale: Boolean = false
)

@Serializable
data class PromptActionViewResponse(
    val id: String,
    val label: String
)

@Serializable
data class TimelineTodayResponse(
    val items: List<TimelineItemViewResponse>
)

@Serializable
data class TimelineItemViewResponse(
    val kind: String,
    @SerialName("tile_id") val tileId: String? = null,
    @SerialName("semantic_role") val semanticRole: String? = null,
    val title: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("duration_min") val durationMin: Long = 0,
    @SerialName("is_active") val isActive: Boolean = false
)

@Serializable
data class RequestPromptResponse(
    val ok: Boolean,
    val prompt: PromptViewResponse? = null,
    val error: String? = null
)
