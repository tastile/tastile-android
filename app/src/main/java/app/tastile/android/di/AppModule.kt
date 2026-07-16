package app.tastile.android.di

import app.tastile.android.core.CoreCommandStore
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.core.PersistentCoreRuntimeService
import app.tastile.android.core.SharedPreferencesCoreCommandStore
import app.tastile.android.data.repository.AuthRepositoryContract
import app.tastile.android.data.repository.CurrentUserProvider
import app.tastile.android.data.repository.DefaultAuthRepository
import app.tastile.android.data.repository.MemoTileRepository
import app.tastile.android.data.repository.PromptTileRepository
import app.tastile.android.data.repository.TileFilter
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.domain.repository.AuthRepository as DomainAuthRepository
import app.tastile.android.domain.repository.TileRepository as DomainTileRepository
import app.tastile.android.domain.repository.TilesResult
import app.tastile.android.sync.CoreEventSyncService
import app.tastile.android.sync.DefaultCoreEventSyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideCoreCommandStore(store: SharedPreferencesCoreCommandStore): CoreCommandStore = store

    @Provides
    @Singleton
    fun provideCoreRuntimeService(commandStore: CoreCommandStore): CoreRuntimeService =
        PersistentCoreRuntimeService(commandStore = commandStore)

    @Provides
    @Singleton
    fun provideCoreEventSyncService(service: DefaultCoreEventSyncService): CoreEventSyncService = service

    @Provides
    @Singleton
    fun provideCurrentUserProvider(authRepository: DefaultAuthRepository): CurrentUserProvider = authRepository

    @Provides
    @Singleton
    fun provideAuthRepositoryContract(authRepository: DefaultAuthRepository): AuthRepositoryContract = authRepository

    @Provides
    @Singleton
    fun providePromptTileRepository(tileRepository: TileRepository): PromptTileRepository = tileRepository

    @Provides
    @Singleton
    fun provideMemoTileRepository(tileRepository: TileRepository): MemoTileRepository = tileRepository

    // ─────────────────────────────────────────────────────────────────
    // Domain-layer adapters (R23): narrow the data-layer repositories
    // to the contracts the domain use cases need. Keeps the dependency
    // arrow data → domain.
    // ─────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDomainAuthRepository(authRepository: DefaultAuthRepository): DomainAuthRepository =
        object : DomainAuthRepository {
            override val getAuthStateStream = authRepository.getAuthStateStream
            override fun currentUserId(): String? = authRepository.currentUserId()
        }

    @Provides
    @Singleton
    fun provideDomainTileRepository(tileRepository: TileRepository): DomainTileRepository =
        object : DomainTileRepository {
            override suspend fun getTiles(filter: TileFilter): TilesResult {
                val response = tileRepository.getTiles(filter)
                return TilesResult(
                    tiles = response.tiles,
                    nextActionableTileId = response.nextActionableTileId,
                    nextActionableStartAt = response.nextActionableStartAt,
                )
            }

            override suspend fun createTile(userId: String, title: String) =
                tileRepository.createTile(userId = userId, title = title)

            override suspend fun startTile(tileId: String) = tileRepository.startTile(tileId)

            override suspend fun completeTile(tileId: String) = tileRepository.completeTile(tileId)

            override suspend fun deleteTile(tileId: String) = tileRepository.deleteTile(tileId)
        }
}
