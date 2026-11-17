package app.tastile.android.data.api

import app.tastile.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin HTTP client for the account-management endpoints exposed by the
 * Next.js web app and the v1 daemon.
 *
 * Two distinct credential paths live here:
 *   * `/api/account/...` Next.js routes live on `WEB_BASE_URL` and require
 *     the BetterAuth session token (`Authorization: Bearer <session>`). The
 *     session token is provided by [app.tastile.android.data.auth.AuthRepository].
 *   * `/v1/api-tokens` lives on `TASTILE_CORE_URL` and uses the minted
 *     Tastile API token (`v1ApiTokenProvider`).
 *
 * Routing the two credentials through separate providers keeps the
 * bootstrap chain (`session_token -> mint -> v1 token`) explicit at the
 * Wiring boundary in [app.tastile.android.data.di.ApiModule].
 */
@Singleton
class BetterAuthAccountApi @Inject constructor(
    private val sessionTokenProvider: suspend () -> String?,
    private val v1ApiTokenProvider: suspend () -> String?,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun webBaseUrl(): String =
        BuildConfig.WEB_BASE_URL.trim().trimEnd('/')

    private fun v1BaseUrl(): String =
        BuildConfig.TASTILE_CORE_URL.trim().trimEnd('/')

    suspend fun getProfile(): AccountProfileDto = withContext(Dispatchers.IO) {
        val token = sessionTokenProvider()
        val url = URL("${webBaseUrl()}/api/account/profile")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            doInput = true
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        val body = connection.readResponseBody()
        json.decodeFromString<AccountProfileEnvelope>(body).profile
    }

    suspend fun startEmailChange(email: String) {
        val token = sessionTokenProvider()
        val url = URL("${webBaseUrl()}/api/account/email/start")
        val body = "email=${URLEncoder.encode(email, Charsets.UTF_8.name())}"
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        connection.discardResponseBody()
    }

    suspend fun verifyEmailChange(code: String) {
        val token = sessionTokenProvider()
        val url = URL("${webBaseUrl()}/api/account/email/verify")
        val body = "code=${URLEncoder.encode(code, Charsets.UTF_8.name())}"
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        connection.discardResponseBody()
    }

    /**
     * `GET /v1/api-tokens` answers with the canonical §15 list envelope
     * (`{items, count, next_cursor}`; tastile-core#334). All cursor pages
     * are followed so the account screen sees every token, matching the
     * full-list semantics the pre-envelope bare array provided.
     */
    suspend fun listTokens(): List<AccountTokenDto> = withContext(Dispatchers.IO) {
        val items = mutableListOf<AccountTokenDto>()
        var cursor: String? = null
        var pages = 0
        do {
            pages += 1
            if (pages > MAX_PAGE_COUNT) {
                throw IOException("api-tokens paging did not terminate")
            }
            val query = buildString {
                append("limit=").append(PAGE_SIZE_LIMIT)
                cursor?.let {
                    append("&cursor=")
                        .append(URLEncoder.encode(it, Charsets.UTF_8.name()))
                }
            }
            val page = json.decodeFromString<AccountTokenListResponse>(
                v1Get("/v1/api-tokens?$query"),
            )
            items += page.items
            cursor = page.nextCursor
        } while (cursor != null)
        items
    }

    suspend fun createToken(label: String?): AccountTokenWithSecretDto = withContext(Dispatchers.IO) {
        val token = v1ApiTokenProvider()
        val url = URL("${v1BaseUrl()}/v1/api-tokens")
        val body = buildJsonObject {
            put("label", label?.let { JsonPrimitive(it) } ?: JsonNull)
            // Core requires `expires_at` on every mint (ADR-0010 D-1.1) and
            // `scopes` when the caller authenticates with a bearer token
            // (v1/14 §14.5 §A.2). Defaults mirror the CLI token contract:
            // 30-day TTL and `tastile.read tastile.write` without OwnerAdmin.
            put("expires_at", JsonPrimitive(Instant.now().plusSeconds(TOKEN_TTL_SECONDS).toString()))
            put("scopes", JsonPrimitive(TOKEN_SCOPES))
        }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val responseBody = connection.readResponseBody()
        json.decodeFromString<AccountTokenWithSecretDto>(responseBody)
    }

    suspend fun revokeToken(id: String) {
        val token = v1ApiTokenProvider()
        val url = URL("${v1BaseUrl()}/v1/api-tokens/$id")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            doInput = true
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        connection.discardResponseBody()
    }

    /** Authenticated GET against the v1 core base URL; returns the body. */
    private suspend fun v1Get(path: String): String {
        val token = v1ApiTokenProvider()
        val url = URL("${v1BaseUrl()}$path")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            doInput = true
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        return connection.readResponseBody()
    }

    private fun HttpURLConnection.readResponseBody(): String {
        val status = responseCode
        val stream = if (status in 200..299) inputStream else errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            throw IOException("http $status: ${text.take(200)}")
        }
        return text
    }

    private fun HttpURLConnection.discardResponseBody() {
        val status = responseCode
        val stream = if (status in 200..299) inputStream else errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            throw IOException("http $status: ${text.take(200)}")
        }
    }

    @Serializable
    data class AccountProfileEnvelope(
        val profile: AccountProfileDto,
    )

    @Serializable
    data class AccountProfileDto(
        val username: String = "",
        val sub: String? = null,
        val email: String? = null,
        val emailVerified: Boolean = false,
        val preferredUsername: String? = null,
    )

    /**
     * §15 list envelope (`{items, count, next_cursor}`) returned by
     * `GET /v1/api-tokens` (tastile-core#334).
     */
    @Serializable
    data class AccountTokenListResponse(
        val items: List<AccountTokenDto> = emptyList(),
        val count: Int = 0,
        @SerialName("next_cursor") val nextCursor: String? = null,
    )

    @Serializable
    data class AccountTokenDto(
        @SerialName("id") val id: String = "",
        @SerialName("token_id") val tokenId: String = "",
        val name: String? = null,
        val label: String? = null,
        @SerialName("token_prefix") val tokenPrefix: String? = null,
        /**
         * v1 `ApiTokenView` sends the display prefix as `prefix`; the web
         * proxy normalizes it to `token_prefix`. Either may be populated.
         */
        @SerialName("prefix") val prefix: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("last_used_at") val lastUsedAt: String? = null,
        @SerialName("last_used_path") val lastUsedPath: String? = null,
        @SerialName("revoked_at") val revokedAt: String? = null,
    ) {
        /** Web returns `name`, the v1 daemon returns `label`. */
        val displayName: String get() = (label ?: name ?: "").trim()

        /** `id` (v1) or `token_id` (web); whichever is populated wins. */
        val canonicalId: String get() = id.ifBlank { tokenId }

        /** `token_prefix` (web) or `prefix` (v1); whichever is populated wins. */
        val resolvedTokenPrefix: String? get() = tokenPrefix ?: prefix
    }

    @Serializable
    data class AccountTokenWithSecretDto(
        val token: String = "",
        @SerialName("id") val id: String = "",
        @SerialName("token_id") val tokenId: String = "",
        @SerialName("access_token") val accessToken: String = "",
        val name: String? = null,
        val label: String? = null,
        @SerialName("token_prefix") val tokenPrefix: String? = null,
        /** v1 `ApiTokenCreatedView` sends the display prefix as `prefix`. */
        @SerialName("prefix") val prefix: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
    ) {
        /** The raw token, returned at most once; either `access_token` or `token`. */
        val secret: String get() = accessToken.ifBlank { token }

        /** `token_id` (web) or `id` (v1); whichever is populated wins. */
        val canonicalId: String get() = tokenId.ifBlank { id }

        /** `token_prefix` (web) or `prefix` (v1); whichever is populated wins. */
        val resolvedTokenPrefix: String? get() = tokenPrefix ?: prefix
    }

    private companion object {
        /** Page size for `GET /v1/api-tokens`; the spec accepts 1..=1000. */
        const val PAGE_SIZE_LIMIT = 1000

        /** Guards against a buggy cursor that never terminates. */
        const val MAX_PAGE_COUNT = 100

        /** Matches the CLI token lifetime (web `core-mint.ts`: 30 days). */
        const val TOKEN_TTL_SECONDS = 30L * 24 * 60 * 60

        /** CLI public-client scope set; excludes OwnerAdmin (v1/14 §14.5). */
        const val TOKEN_SCOPES = "tastile.read tastile.write"
    }
}
