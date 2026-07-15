package app.tastile.android.data.di

import app.tastile.android.data.api.CognitoAccountApi
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.command.V1CommandDispatcher
import app.tastile.android.data.repository.ApiTokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    /**
     * The [AuthTokenProvider] returns the Tastile API token (minted on first
     * authenticated use and cached in encrypted storage), not the Cognito
     * `id_token`. The Cognito token is the *bootstrap* credential used only
     * to mint the Tastile token. See `PROJECT-TRUTH.md` ("Authentication").
     *
     * Returns `null` when no token has been minted yet, so callers that
     * require auth surface as a recoverable retry rather than sending the
     * wrong credential. The mint path is triggered lazily inside
     * `V1ApiClient` consumers; the manager is invoked here purely for the
     * read-side cache lookup. First-use semantics are owned by
     * [ApiTokenManager] and exercised in
     * `app/src/test/java/.../ApiTokenManagerTest.kt`.
     */
    @Provides
    @Singleton
    fun provideV1ApiClient(apiTokenManager: ApiTokenManager): V1ApiClient =
        V1ApiClient { apiTokenManager.getOrMint() }

    @Provides
    @Singleton
    fun provideCognitoAccountApi(apiTokenManager: ApiTokenManager): CognitoAccountApi =
        CognitoAccountApi { apiTokenManager.getOrMint() }

    @Provides
    @Singleton
    fun provideV1CommandDispatcher(v1ApiClient: V1ApiClient): V1CommandDispatcher =
        V1CommandDispatcher(v1ApiClient)
}
