package app.tastile.android.data.api

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that attaches a Tastile API bearer token to every
 * outgoing request routed through the generated Retrofit client.
 *
 * The token is fetched lazily via the suspend [AuthTokenProvider] (backed
 * by [app.tastile.android.data.auth.ApiTokenCache]). The bridge to
 * OkHttp's sync `intercept` callback uses `runBlocking` because OkHttp runs
 * the interceptor on its dispatcher thread; the suspending call is short
 * (cache hit → instant return, mint → single HTTPS call) and the resulting
 * blocking is no worse than what the existing hand-rolled `V1ApiClient` does.
 *
 * If no token has been minted yet, the request proceeds without an
 * `Authorization` header — the server will return 401, surfaced as
 * `V1Error.Auth` by the generated client's converter.
 */
@Singleton
class V1AuthInterceptor @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenProvider() }
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
