package app.tastile.android.data.repository

import app.tastile.android.data.model.Tile

interface PromptTileRepository {
    suspend fun getActiveStartedTile(userId: String): Tile?
    suspend fun continueTile(tileId: String)
    suspend fun pauseTile(tileId: String)
    suspend fun completeTile(tileId: String): Tile
    suspend fun getPendingPrompt(): PromptViewResponse? = null
    suspend fun requestPrompt(tileId: String): Boolean = false
}
