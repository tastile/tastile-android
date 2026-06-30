package app.tastile.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AggregateRef(
    val kind: Byte,
    val id: String
)

@Serializable
data class CommandResponse(
    @SerialName("command_id") val commandId: String,
    @SerialName("accepted_at") val acceptedAt: String,
    val aggregate: AggregateRef? = null,
    val revision: Long? = null,
    val result: Byte,
    val pending: List<PendingWork> = emptyList()
)

@Serializable
data class PendingWork(
    val kind: String,
    @SerialName("scheduled_at") val scheduledAt: String? = null
)

@Serializable
data class TileVisualView(
    val color: String? = null,
    val icon: String? = null
)

@Serializable
data class TileContentView(
    val title: String = "",
    val note: String? = null
)

@Serializable
data class Span(
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String
)

@Serializable
data class PlacementInsideView(
    @SerialName("placement_id") val placementId: String
)

@Serializable
data class PlacementSourceView(
    val value: Byte
)

@Serializable
data class ResolutionInfoView(
    val state: Byte,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("resolution_hash") val resolutionHash: String? = null,
    val violations: List<ResolutionViolationBody> = emptyList()
)

@Serializable
data class TimelineItem(
    @SerialName("placement_id") val placementId: String,
    val revision: Long,
    val content: TileContentView,
    val visual: TileVisualView,
    val role: Byte,
    val span: Span,
    val inside: PlacementInsideView? = null,
    val source: PlacementSourceView,
    val resolution: ResolutionInfoView
)

@Serializable
data class TileView(
    val id: String,
    val kind: Byte,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("external_id") val externalId: String? = null,
    val content: TileContentView,
    val visual: TileVisualView,
    val revision: Long
)

@Serializable
data class V1ListTilesResponse(
    val tiles: List<TileView> = emptyList()
)

@Serializable
data class V1TimelineResponse(
    val items: List<TimelineItem> = emptyList()
)

@Serializable
data class RuntimePathView(
    @SerialName("id") val id: String,
    @SerialName("profile_name") val profileName: String,
    @SerialName("app_data_dir") val appDataDir: String,
    @SerialName("db_path") val dbPath: String,
    @SerialName("session_path") val sessionPath: String,
    @SerialName("daemon_startup_log_path") val daemonStartupLogPath: String = "",
    @SerialName("daemon_executable_path") val daemonExecutablePath: String = ""
)

@Serializable
data class V1ListRuntimePathsResponse(
    val paths: List<RuntimePathView> = emptyList()
)