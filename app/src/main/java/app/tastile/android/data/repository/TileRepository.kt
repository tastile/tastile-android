package app.tastile.android.data.repository

import app.tastile.android.data.model.Tile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileRepository @Inject constructor(
    private val client: SupabaseClient
) {
    companion object {
        private const val TABLE_TILES = "tiles"
    }

    suspend fun getTiles(userId: String): List<Tile> {
        return client.from(TABLE_TILES)
            .select {
                filter {
                    eq("user_id", userId)
                    exact("deleted_at", null)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Tile>()
    }

    suspend fun createTile(userId: String, title: String): Tile {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        val tile = Tile(
            userId = userId,
            localTileId = UUID.randomUUID().toString(),
            title = title,
            lifecycle = "Ready",
            localCreatedAt = now,
            localUpdatedAt = now
        )
        
        return client.from(TABLE_TILES)
            .insert(tile) {
                select()
            }
            .decodeSingle<Tile>()
    }

    suspend fun startTile(tileId: String): Tile {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        
        return client.from(TABLE_TILES)
            .update({
                set("lifecycle", "Started")
                set("updated_at", now)
            }) {
                filter {
                    eq("id", tileId)
                }
                select()
            }
            .decodeSingle<Tile>()
    }

    suspend fun completeTile(tileId: String): Tile {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        
        return client.from(TABLE_TILES)
            .update({
                set("lifecycle", "Done")
                set("updated_at", now)
            }) {
                filter {
                    eq("id", tileId)
                }
                select()
            }
            .decodeSingle<Tile>()
    }

    suspend fun deleteTile(tileId: String) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        
        client.from(TABLE_TILES)
            .update({
                set("deleted_at", now)
            }) {
                filter {
                    eq("id", tileId)
                }
            }
    }

    suspend fun pauseTile(tileId: String) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        
        client.from(TABLE_TILES)
            .update({
                set("lifecycle", "Ready")
                set("updated_at", now)
            }) {
                filter {
                    eq("id", tileId)
                }
            }
    }
}
