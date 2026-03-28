package app.tastile.android.di

import app.tastile.android.data.SupabaseClientFactory
import app.tastile.android.core.CoreCommandStore
import app.tastile.android.core.CoreRuntimeService
import app.tastile.android.core.PersistentCoreRuntimeService
import app.tastile.android.core.SharedPreferencesCoreCommandStore
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.AuthRepositoryContract
import app.tastile.android.data.repository.CurrentUserProvider
import app.tastile.android.data.repository.MemoTileRepository
import app.tastile.android.data.repository.PromptTileRepository
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.sync.CoreEventSyncService
import app.tastile.android.sync.DefaultCoreEventSyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClientFactory.create()

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
    fun provideCurrentUserProvider(authRepository: AuthRepository): CurrentUserProvider = authRepository

    @Provides
    @Singleton
    fun provideAuthRepositoryContract(authRepository: AuthRepository): AuthRepositoryContract = authRepository

    @Provides
    @Singleton
    fun providePromptTileRepository(tileRepository: TileRepository): PromptTileRepository = tileRepository

    @Provides
    @Singleton
    fun provideMemoTileRepository(tileRepository: TileRepository): MemoTileRepository = tileRepository
}
