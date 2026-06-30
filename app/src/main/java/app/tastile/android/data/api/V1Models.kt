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

// --- Step 5 lookup models -----------------------------------------------
//
// The dispatcher uses these to translate v0 `tile.start / pause / continue /
// reschedule` into v1 IDs.  Each is a minimal projection of the matching
// server response; fields we don't read are dropped so the wire decoder can
// stay terse.
//
// Wire shape notes (from `crates/v1/api/src/handlers/read.rs`):
//   - `GET /v1/tiles/{id}` (`read_tile`) returns a *flat* TileView with
//     `title`, `description`, `color`, `icon`, `plan_id`, `archived_at` at
//     the top level. NOT the nested `content`/`visual` shape that the
//     legacy `TileView` (used by listTiles) pretends the server sends.
//   - `GET /v1/placements` (`list_placements`) returns `PlacementListItem`.
//   - `GET /v1/executions/{id}` (`read_execution`) returns `ExecutionView`.

@Serializable
data class TileDetailView(
    val id: String,
    val kind: Byte,
    @SerialName("owner_id") val ownerId: String,
    val revision: Long,
    val title: String,
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
    @SerialName("external_id") val externalId: String? = null,
    @SerialName("plan_id") val planId: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null
)

@Serializable
data class V1PlacementListItem(
    @SerialName("placement_id") val placementId: String,
    @SerialName("tile_id") val tileId: String,
    @SerialName("plan_id") val planId: String? = null,
    val title: String = "",
    @SerialName("span_start") val spanStart: String? = null,
    @SerialName("span_end") val spanEnd: String? = null
)

@Serializable
data class V1ExecutionView(
    val id: String,
    @SerialName("tile_id") val tileId: String,
    @SerialName("owner_id") val ownerId: String,
    val revision: Long,
    val state: Byte,
    @SerialName("placement_id") val placementId: String? = null
)