package app.tastile.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Tile(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("local_tile_id") val localTileId: String = "",
    val title: String = "",
    @SerialName("next_action") val nextAction: String? = null,
    @SerialName("done_definition") val doneDefinition: String? = null,
    @SerialName("temporal_conditions") val temporalConditions: JsonObject? = null,
    @SerialName("objective_conditions") val objectiveConditions: JsonObject? = null,
    @SerialName("interruption_conditions") val interruptionConditions: JsonObject? = null,
    @SerialName("automation_conditions") val automationConditions: JsonObject? = null,
    val lifecycle: String = "Ready",
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
