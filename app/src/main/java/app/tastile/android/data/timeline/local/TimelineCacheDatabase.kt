package app.tastile.android.data.timeline.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TimelineItemEntity::class,
        TimelineDayMembershipEntity::class,
        TimelineCoverageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TimelineCacheDatabase : RoomDatabase() {
    abstract fun timelineCacheDao(): TimelineCacheDao

    companion object {
        const val DATABASE_NAME = "timeline_cache.db"
    }
}
