package app.tastile.android.domain.repository

import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.TileFilter

/**
 * Domain-layer read/write contract for tiles.
 *
 * Concrete implementation lives in
 * [app.tastile.android.data.repository.TileRepository]; the domain layer
 * only sees this narrowed interface so use cases remain free of v1 API
 * details (cache invalidation, dispatcher selection, etc.).
 *
 * Kept in `domain/` (not `data/`) per the R23 architecture recommendation
 * so the dependency arrow points data → domain, not the reverse.
 */
interface TileRepository {
    suspend fun getTiles(filter: TileFilter = TileFilter.DEFAULT): TilesResult

    suspend fun createTile(userId: String, title: String): Tile

    suspend fun startTile(tileId: String): Tile

    suspend fun completeTile(tileId: String): Tile

    suspend fun deleteTile(tileId: String)
}

/**
 * Trimmed mirror of [app.tastile.android.data.repository.TileRepository.TilesResponse].
 * Domain callers don't need the raw v1 diagnostics strings, only the
 * surface list plus the two `next_actionable_*` hints the dashboard
 * surfaces.
 */
data class TilesResult(
    val tiles: List<Tile>,
    val nextActionableTileId: String?,
    val nextActionableStartAt: String?,
)
