package app.tastile.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Domain tile model. Populated by `V1Mappers.toTile()` from
 * `TileListView` (the wire shape of `GET /v1/tiles`).
 *
 * Fields added by the 2026-07-07 web→android parity sweep (`C1`) sit
 * alongside the legacy `*Conditions` slots; the legacy slots are kept
 * temporarily for in-flight callers that still read them (e.g. legacy
 * snapshot JSON rows). They will be retired in a follow-up chunk once
 * every consumer migrates to the new fields.
 *
 * The model deliberately stays serializable so existing snapshot JSON
 * caches keep decoding — new fields default to null/empty.
 */
@Serializable
data class Tile(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("local_tile_id") val localTileId: String = "",
    val title: String = "",
    @SerialName("next_action") val nextAction: String? = null,
    @SerialName("done_definition") val doneDefinition: String? = null,
    val lifecycle: String = "Ready",
    val labels: List<String> = emptyList(),
    @SerialName("worked_minutes") val workedMinutes: Long? = null,
    @SerialName("break_minutes") val breakMinutes: Long? = null,
    @SerialName("objective_mode_code") val objectiveModeCode: Int? = null,
    @SerialName("target_work_min") val targetWorkMin: Long? = null,
    @SerialName("target_rest_min") val targetRestMin: Long? = null,
    @SerialName("done_rule_code") val doneRuleCode: Int? = null,
    @SerialName("resume_note") val resumeNote: String? = null,
    @SerialName("projected_next_start_at") val projectedNextStartAt: String? = null,
    @SerialName("release_at") val releaseAt: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("fixed_start") val fixedStart: String? = null,
    @SerialName("fixed_end") val fixedEnd: String? = null,
    @SerialName("active_start") val activeStart: String? = null,
    @SerialName("active_end") val activeEnd: String? = null,
    @SerialName("plan_id") val planId: String? = null,
    @SerialName("is_recurring") val isRecurring: Boolean = false,

    // Legacy v0 fields retained for snapshot-row decoding. Will be retired
    // once the legacy snapshot consumers in `TileRepository` are removed.
    @SerialName("temporal_conditions") val temporalConditions: JsonObject? = null,
    @SerialName("objective_conditions") val objectiveConditions: JsonObject? = null,
    @SerialName("interruption_conditions") val interruptionConditions: JsonObject? = null,
    @SerialName("automation_conditions") val automationConditions: JsonObject? = null,
    @SerialName("annotation_conditions") val annotationConditions: JsonObject? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("local_created_at") val localCreatedAt: String? = null,
    @SerialName("local_updated_at") val localUpdatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

enum class TileLifecycle(val value: String) {
    READY("Ready"),
    STARTED("Started"),
    DONE("Done"),
    ARCHIVED("Archived");

    companion object {
        fun fromString(value: String): TileLifecycle = values().find { it.value == value } ?: READY
    }
}
