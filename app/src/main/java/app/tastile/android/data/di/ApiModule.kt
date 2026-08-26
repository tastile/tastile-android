package app.tastile.android.data.di

import app.tastile.android.data.api.BetterAuthAccountApi
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.auth.ApiTokenCache
import app.tastile.android.data.auth.AuthRepositoryContract
import app.tastile.android.data.auth.BetterAuthHttpClient
import app.tastile.android.data.command.V1CommandDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    /**
     * The [V1ApiClient] `tokenProvider` returns the Tastile API token (minted
     * on first authenticated use and cached in encrypted storage), not the
     * BetterAuth session token. The session token is the *bootstrap*
     * credential used only to mint the Tastile token. See `PROJECT-TRUTH.md`
     * ("Authentication").
     *
     * Returns `null` when no token has been minted yet, so callers that
     * require auth surface as a recoverable retry rather than sending the
     * wrong credential. The mint path is triggered lazily inside
     * `V1ApiClient` consumers; the cache is invoked here purely for the
     * read-side cache lookup. First-use semantics are owned by
     * [ApiTokenCache] and exercised in
     * `app/src/test/java/.../ApiTokenCacheTest.kt`.
     */
    @Provides
    @Singleton
    fun provideV1ApiClient(apiTokenCache: ApiTokenCache): V1ApiClient =
        V1ApiClient { apiTokenCache.getOrMint() }

    @Provides
    @Singleton
    fun provideBetterAuthHttpClient(): BetterAuthHttpClient = BetterAuthHttpClient()

    /**
     * Wires the BetterAuth account API with two distinct token providers.
     * The session token authorizes `/api/account/...` (the Next.js proxy
     * routes); the v1 API token authorizes `/v1/api-tokens`. Each call
     * site in [BetterAuthAccountApi] picks the right provider so a stale
     * session never reaches the v1 daemon and vice versa.
     */
    @Provides
    @Singleton
    fun provideBetterAuthAccountApi(
        authRepository: AuthRepositoryContract,
        apiTokenCache: ApiTokenCache,
    ): BetterAuthAccountApi = BetterAuthAccountApi(
        sessionTokenProvider = { authRepository.currentSessionToken() },
        v1ApiTokenProvider = { apiTokenCache.getOrMint() },
    )

    @Provides
    @Singleton
    fun provideV1CommandDispatcher(v1ApiClient: V1ApiClient): V1CommandDispatcher =
        V1CommandDispatcher(v1ApiClient)
}
