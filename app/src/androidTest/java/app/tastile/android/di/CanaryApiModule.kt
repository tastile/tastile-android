package app.tastile.android.di

import app.tastile.android.data.api.BetterAuthAccountApi
import app.tastile.android.data.auth.BetterAuthHttpClient
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.command.V1CommandDispatcher
import app.tastile.android.data.di.ApiModule
import app.tastile.android.util.QuickCreateCanaryBackend
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces production [ApiModule] for canary interaction tests.
 *
 * Only the network edge is faked ([QuickCreateCanaryBackend.client]);
 * every other binding is re-provided with its production construction so
 * the graph shape under test stays identical to release. If [ApiModule]
 * gains a new `@Provides`, this module MUST mirror it — a missing binding
 * fails the test build fast at Hilt codegen time, which is the desired
 * behaviour (silent graph drift is worse than a compile error).
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ApiModule::class],
)
object CanaryApiModule {

    @Provides
    @Singleton
    fun provideV1ApiClient(): V1ApiClient = QuickCreateCanaryBackend.client

    @Provides
    @Singleton
    fun provideBetterAuthHttpClient(): BetterAuthHttpClient = BetterAuthHttpClient()

    @Provides
    @Singleton
    fun provideBetterAuthAccountApi(): BetterAuthAccountApi =
        BetterAuthAccountApi(
            sessionTokenProvider = { null },
            v1ApiTokenProvider = { null },
        )

    @Provides
    @Singleton
    fun provideV1CommandDispatcher(client: V1ApiClient): V1CommandDispatcher =
        V1CommandDispatcher(client)
}
