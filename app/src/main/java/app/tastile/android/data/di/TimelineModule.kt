package app.tastile.android.data.di

import android.content.Context
import androidx.room.Room
import app.tastile.android.data.tile.TileRepository
import app.tastile.android.data.timeline.DefaultTimelinePageRepository
import app.tastile.android.data.timeline.DefaultTimelineSyncRepository
import app.tastile.android.data.timeline.TimelineApplicationScope
import app.tastile.android.data.timeline.TimelinePageRepository
import app.tastile.android.data.timeline.TimelineSyncRepository
import app.tastile.android.data.timeline.local.TimelineCacheDao
import app.tastile.android.data.timeline.local.TimelineCacheDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Timeline-local infrastructure shared by production and instrumented tests.
 *
 * This module is intentionally separate from [ApiModule]. Canary tests replace
 * the API/network edge only; cache, refresh coordination and clock bindings
 * must keep the production graph shape instead of being duplicated in the
 * canary replacement module.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimelineModule {
    @Provides
    @Singleton
    fun provideTimelineCacheDatabase(
        @ApplicationContext context: Context,
    ): TimelineCacheDatabase = Room.databaseBuilder(
        context,
        TimelineCacheDatabase::class.java,
        TimelineCacheDatabase.DATABASE_NAME,
    ).build()

    @Provides
    @Singleton
    fun provideTimelineCacheDao(database: TimelineCacheDatabase): TimelineCacheDao =
        database.timelineCacheDao()

    @Provides
    @Singleton
    fun provideTimelinePageRepository(dao: TimelineCacheDao): TimelinePageRepository =
        DefaultTimelinePageRepository(dao)

    @Provides
    @Singleton
    fun provideTimelineSyncRepository(
        tileRepository: TileRepository,
        dao: TimelineCacheDao,
    ): TimelineSyncRepository = DefaultTimelineSyncRepository(tileRepository, dao)

    @Provides
    @Singleton
    @TimelineApplicationScope
    fun provideTimelineApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideTimelineClock(): Clock = Clock.systemUTC()
}
