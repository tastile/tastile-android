package app.tastile.android.data.repository

import app.tastile.android.data.model.Tile

interface MemoTileRepository {
    suspend fun getRecentTiles(userId: String, limit: Int = 5): List<Tile>
    suspend fun saveMemo(tileId: String, note: String)
}

