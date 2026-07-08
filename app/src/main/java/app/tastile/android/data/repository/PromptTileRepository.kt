package app.tastile.android.data.repository

import app.tastile.android.data.model.Tile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Prompt view projection returned by `TileRepository.getPendingPrompt()`
 * and consumed by `PromptViewModel`.
 *
 * Kept in this file (rather than the wire-shape DTOs in
 * `CoreApiParityModels.kt`) because the prompt surface survives the C3
 * TileRepository rework — only the tile-list read path was rewritten.
 */
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

interface PromptTileRepository {
    suspend fun getActiveStartedTile(userId: String): Tile?
    suspend fun continueTile(tileId: String)
    suspend fun pauseTile(tileId: String)
    suspend fun completeTile(tileId: String): Tile
    suspend fun getPendingPrompt(): PromptViewResponse? = null
    suspend fun requestPrompt(tileId: String): Boolean = false
}