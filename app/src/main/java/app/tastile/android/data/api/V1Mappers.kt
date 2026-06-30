package app.tastile.android.data.api

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle

/**
 * Interim mapper from v1 [TileView] (numeric [kind]) to the existing v0 [Tile]
 * data class (string [Tile.lifecycle]). Lets the existing UI keep rendering
 * while v1 data flows through.
 *
 * Lifecycle derivation:
 *  - [V1NumericConstants.TileKind.EXECUTION] -> [TileLifecycle.STARTED] (in-flight execution)
 *  - [V1NumericConstants.TileKind.PLACEMENT] -> [TileLifecycle.READY]
 *  - [V1NumericConstants.TileKind.RECURRING] -> [TileLifecycle.READY]
 *  - unknown kind                            -> [TileLifecycle.READY] (defensive)
 */
fun TileView.toTile(userId: String): Tile {
    val lifecycle = when (kind) {
        V1NumericConstants.TileKind.EXECUTION -> TileLifecycle.STARTED.value
        V1NumericConstants.TileKind.PLACEMENT,
        V1NumericConstants.TileKind.RECURRING -> TileLifecycle.READY.value
        else -> TileLifecycle.READY.value
    }
    return Tile(
        id = id,
        userId = userId,
        localTileId = id,
        title = content.title,
        lifecycle = lifecycle
    )
}

fun V1ListTilesResponse.toTiles(userId: String): List<Tile> =
    tiles.map { it.toTile(userId = userId) }