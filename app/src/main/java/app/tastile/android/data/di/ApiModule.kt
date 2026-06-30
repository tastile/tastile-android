package app.tastile.android.data.di

import app.tastile.android.data.api.AuthTokenProvider
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.command.V1CommandDispatcher
import app.tastile.android.data.repository.CurrentUserProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideAuthTokenProvider(currentUser: CurrentUserProvider): AuthTokenProvider =
        { currentUser.currentIdToken() }

    @Provides
    @Singleton
    fun provideV1ApiClient(tokenProvider: AuthTokenProvider): V1ApiClient =
        V1ApiClient(tokenProvider)

    @Provides
    @Singleton
    fun provideV1CommandDispatcher(v1ApiClient: V1ApiClient): V1CommandDispatcher =
        V1CommandDispatcher(v1ApiClient)
}