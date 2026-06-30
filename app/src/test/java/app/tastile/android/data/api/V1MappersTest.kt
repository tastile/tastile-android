package app.tastile.android.data.api

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import org.junit.Assert.assertEquals
import org.junit.Test

class V1MappersTest {

    private fun v1Tile(
        id: String = "tile-1",
        kind: Byte = V1NumericConstants.TileKind.RECURRING,
        ownerId: String = "owner-1",
        externalId: String? = null,
        title: String = "Hello",
        revision: Long = 1L
    ): TileView = TileView(
        id = id,
        kind = kind,
        ownerId = ownerId,
        externalId = externalId,
        content = TileContentView(title = title, note = null),
        visual = TileVisualView(color = null, icon = null),
        revision = revision
    )

    @Test
    fun execution_kind_maps_to_started_lifecycle() {
        val view = v1Tile(kind = V1NumericConstants.TileKind.EXECUTION, title = "In flight")

        val tile = view.toTile(userId = "owner-1")

        assertEquals(TileLifecycle.STARTED.value, tile.lifecycle)
    }

    @Test
    fun placement_kind_maps_to_ready_lifecycle() {
        val view = v1Tile(kind = V1NumericConstants.TileKind.PLACEMENT, title = "Scheduled")

        val tile = view.toTile(userId = "owner-1")

        assertEquals(TileLifecycle.READY.value, tile.lifecycle)
    }

    @Test
    fun recurring_kind_maps_to_ready_lifecycle() {
        val view = v1Tile(kind = V1NumericConstants.TileKind.RECURRING, title = "Recurring")

        val tile = view.toTile(userId = "owner-1")

        assertEquals(TileLifecycle.READY.value, tile.lifecycle)
    }

    @Test
    fun unknown_kind_defaults_to_ready_lifecycle() {
        val view = v1Tile(kind = 99.toByte(), title = "Future kind")

        val tile = view.toTile(userId = "owner-1")

        assertEquals(TileLifecycle.READY.value, tile.lifecycle)
    }

    @Test
    fun mapper_preserves_id_and_title() {
        val view = v1Tile(id = "tile-xyz", title = "Buy milk")

        val tile = view.toTile(userId = "owner-1")

        assertEquals("tile-xyz", tile.id)
        assertEquals("Buy milk", tile.title)
    }

    @Test
    fun mapper_copies_userId_from_argument() {
        val view = v1Tile(ownerId = "owner-from-v1")

        val tile = view.toTile(userId = "owner-from-args")

        assertEquals("owner-from-args", tile.userId)
    }

    @Test
    fun mapper_populates_localTileId_from_id() {
        val view = v1Tile(id = "tile-abc")

        val tile = view.toTile(userId = "owner-1")

        assertEquals("tile-abc", tile.localTileId)
    }

    @Test
    fun list_response_maps_each_tile_independently() {
        val response = V1ListTilesResponse(
            tiles = listOf(
                v1Tile(id = "t-1", kind = V1NumericConstants.TileKind.EXECUTION, title = "Exec"),
                v1Tile(id = "t-2", kind = V1NumericConstants.TileKind.PLACEMENT, title = "Place"),
                v1Tile(id = "t-3", kind = V1NumericConstants.TileKind.RECURRING, title = "Recur")
            )
        )

        val tiles: List<Tile> = response.toTiles(userId = "owner-1")

        assertEquals(3, tiles.size)
        assertEquals("Exec", tiles[0].title)
        assertEquals(TileLifecycle.STARTED.value, tiles[0].lifecycle)
        assertEquals(TileLifecycle.READY.value, tiles[1].lifecycle)
        assertEquals(TileLifecycle.READY.value, tiles[2].lifecycle)
    }
}