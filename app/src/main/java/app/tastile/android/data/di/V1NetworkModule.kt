package app.tastile.android.data.di

import app.tastile.android.BuildConfig
import app.tastile.android.data.api.V1AuthInterceptor
import app.tastile.android.data.api.generated.v1.apis.ReadApi
import app.tastile.android.data.api.generated.v1.apis.SourceTileApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Hilt module that wires the openapi-generator-produced Retrofit + Moshi
 * client for the v1 surface documented in `app/openapi/v1.json`.
 *
 * Provides:
 * - [V1Moshi] (Moshi with Kotlin reflect adapter — generated DTOs use
 *   `@JsonClass(generateAdapter = true)` via KSP, so the reflect factory
 *   is only a fallback for ad-hoc decoding in tests).
 * - [V1OkHttpClient] (15s connect/read timeouts to match the hand-rolled
 *   `V1ApiClient`; logging interceptor in debug builds only).
 * - [V1Retrofit] (Moshi converter, base URL from
 *   `BuildConfig.TASTILE_CORE_URL`).
 * - [ReadApi] and [SourceTileApi] — the two generated Retrofit interfaces.
 *
 * Consumers inject the Retrofit interfaces directly (not the generated
 * `ApiClient` factory) and call `Response<T>` methods. The
 * `V1GeneratedApiClient` wrapper in `app.tastile.android.data.api` provides
 * a flattened, type-safe facade that unwraps the `Response<T>` and bridges
 * the generated Moshi DTOs to the call sites.
 */
@Module
@InstallIn(SingletonComponent::class)
object V1NetworkModule {
    private const val TIMEOUT_SECONDS: Long = 15

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class V1Moshi

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class V1OkHttpClient

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class V1Retrofit

    @Provides
    @Singleton
    @V1Moshi
    fun provideV1Moshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    @V1OkHttpClient
    fun provideV1OkHttpClient(authInterceptor: V1AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BASIC
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @V1Retrofit
    fun provideV1Retrofit(
        @V1Moshi moshi: Moshi,
        @V1OkHttpClient client: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ensureTrailingSlash(BuildConfig.TASTILE_CORE_URL))
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideReadApi(@V1Retrofit retrofit: Retrofit): ReadApi =
        retrofit.create(ReadApi::class.java)

    @Provides
    @Singleton
    fun provideSourceTileApi(@V1Retrofit retrofit: Retrofit): SourceTileApi =
        retrofit.create(SourceTileApi::class.java)

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
