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
    @SerialName("aggregate_meta") val aggregateMeta: AggregateMeta? = null,
    val revision: Long? = null,
    val result: Byte,
    val pending: List<PendingWork> = emptyList()
)

@Serializable
data class AggregateMeta(
    @SerialName("tile_id") val tileId: String? = null,
    @SerialName("plan_id") val planId: String? = null,
    @SerialName("recurring_id") val recurringId: String? = null,
    @SerialName("frame_rule_id") val frameRuleId: String? = null,
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

// --- TileListView (GET /v1/tiles) -------------------------------------
//
// Mirrors `tastile-web/src/lib/hooks/use-tile-list.ts` (TileListView shape)
// and the OpenAPI spec wired by `GET /v1/tiles`.
//
// Wire fields:
//   - id, plan_id, title, lifecycle (i16), next_action, done_definition
//   - worked_minutes, break_minutes, labels[]
//   - objective_mode (i16), target_work_min, target_rest_min, done_rule (i16|null)
//   - resume_note, projected_next_start_at
//   - temporal { release_at, due_at, fixed_start, fixed_end, active_start, active_end }
//   - recurrence { step_min, window_start_min, window_end_min, expression }
//
// `ignoreUnknownKeys = true` on the Json instance absorbs additional fields
// the backend may add without breaking the deserialization.

@Serializable
data class TileListView(
    val id: String,
    @SerialName("plan_id") val planId: String? = null,
    val title: String = "",
    val lifecycle: Int? = null,
    @SerialName("next_action") val nextAction: String? = null,
    @SerialName("done_definition") val doneDefinition: String? = null,
    @SerialName("worked_minutes") val workedMinutes: Long? = null,
    @SerialName("break_minutes") val breakMinutes: Long? = null,
    val labels: List<String> = emptyList(),
    @SerialName("objective_mode") val objectiveMode: Int? = null,
    @SerialName("target_work_min") val targetWorkMin: Long? = null,
    @SerialName("target_rest_min") val targetRestMin: Long? = null,
    @SerialName("done_rule") val doneRule: Int? = null,
    @SerialName("resume_note") val resumeNote: String? = null,
    @SerialName("projected_next_start_at") val projectedNextStartAt: String? = null,
    val temporal: TileTemporalView? = null,
    val recurrence: TileRecurrenceView? = null
)

@Serializable
data class TileTemporalView(
    @SerialName("release_at") val releaseAt: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("fixed_start") val fixedStart: String? = null,
    @SerialName("fixed_end") val fixedEnd: String? = null,
    @SerialName("active_start") val activeStart: String? = null,
    @SerialName("active_end") val activeEnd: String? = null
)

@Serializable
data class TileRecurrenceView(
    @SerialName("step_min") val stepMin: Long? = null,
    @SerialName("window_start_min") val windowStartMin: Long? = null,
    @SerialName("window_end_min") val windowEndMin: Long? = null,
    val expression: String? = null
)

// Legacy `TileView` shape (still used by callers that decoded
// `GET /v1/tiles` into the old nested content/visual form before C1).
// `readTile()` (`GET /v1/tiles/{id}`) returns the *flat* `TileDetailView`
// documented below; this class survives for that one internal call site.
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
    val tiles: List<TileListView> = emptyList(),
    @SerialName("next_actionable_tile_id") val nextActionableTileId: String? = null,
    @SerialName("next_actionable_start_at") val nextActionableStartAt: String? = null
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
//     legacy `TileView` (used by listTiles) pretended the server sends.
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

// --- C5 Projects (workspaces) -------------------------------------------
//
// Wire shape for `GET /v1/access/subjects?kind=1` and friends.
// Mirrors the web `Workspace` interface (see tastile-web
// `src/lib/hooks/use-projects.ts`). We use only the fields Android reads; the
// list endpoint returns SubjectRow items as opaque Workspace summaries.

@Serializable
data class Workspace(
    val id: String,
    val kind: Short = 1,
    @SerialName("display_name") val displayName: String,
    val slug: String? = null,
    val email: String? = null,
    @SerialName("parent_subject_id") val parentSubjectId: String? = null,
    val color: String? = null,
    @SerialName("owner_user_id") val ownerUserId: String? = null,
    @SerialName("disabled_at") val disabledAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

data class CreateWorkspaceInput(
    @SerialName("display_name") val displayName: String,
    val slug: String? = null,
    val color: String? = null,
    @SerialName("parent_subject_id") val parentSubjectId: String? = null,
)

@Serializable
data class V1ListWorkspacesResponse(
    val items: List<Workspace> = emptyList(),
    val count: Int = 0,
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
