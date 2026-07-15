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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin HTTP client for the account-management endpoints exposed by the
 * Next.js web app and the v1 daemon. The Cognito id/access tokens are
 * the bearer credential for the Next routes (`/api/account/*`); the
 * Tastile API token is the bearer for the v1 routes (`/v1/api-tokens`).
 *
 * Distinct from [V1ApiClient] because:
 *   * these endpoints are not in the v1 Command envelope shape,
 *   * account/profile reads use the Cognito id_token claims, not the v1
 *     API token,
 *   * tokens (`/v1/api-tokens`) share the v1 base URL but follow the
 *     web flat JSON shape (the v1 envelope decoder would reject them).
 *
 * Decoding is JSON; no signature verification happens client-side.
 *
 * The "/api/account/profile" route is currently a Next.js web proxy.
 * When [getProfile] fails the repository falls back to the cached
 * Cognito id_token claims (see `AccountRepository.loadProfile`).
 */
@Singleton
class CognitoAccountApi @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun webBaseUrl(): String =
        BuildConfig.COGNITO_WEB_AUTH_BASE_URL.trim().trimEnd('/')

    private fun v1BaseUrl(): String =
        BuildConfig.TASTILE_CORE_URL.trim().trimEnd('/')

    suspend fun getProfile(): AccountProfileDto = withContext(Dispatchers.IO) {
        val token = tokenProvider()
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
        val token = tokenProvider()
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
        val token = tokenProvider()
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

    suspend fun listTokens(): List<AccountTokenDto> = withContext(Dispatchers.IO) {
        val token = tokenProvider()
        val url = URL("${v1BaseUrl()}/v1/api-tokens")
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
        json.decodeFromString<AccountTokenListResponse>(body).tokens
    }

    suspend fun createToken(label: String?): AccountTokenWithSecretDto = withContext(Dispatchers.IO) {
        val token = tokenProvider()
        val url = URL("${v1BaseUrl()}/v1/api-tokens")
        val body = buildJsonObject {
            put("label", label?.let { JsonPrimitive(it) } ?: JsonNull)
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
        val token = tokenProvider()
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

    @Serializable
    data class AccountTokenListResponse(
        val tokens: List<AccountTokenDto> = emptyList(),
    )

    @Serializable
    data class AccountTokenDto(
        @SerialName("id") val id: String = "",
        @SerialName("token_id") val tokenId: String = "",
        val name: String? = null,
        val label: String? = null,
        @SerialName("token_prefix") val tokenPrefix: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("last_used_at") val lastUsedAt: String? = null,
        @SerialName("last_used_path") val lastUsedPath: String? = null,
        @SerialName("revoked_at") val revokedAt: String? = null,
    ) {
        /** Web returns `name`, the v1 daemon returns `label`. */
        val displayName: String get() = (label ?: name ?: "").trim()

        /** `id` (v1) or `token_id` (web); whichever is populated wins. */
        val canonicalId: String get() = id.ifBlank { tokenId }
    }

    @Serializable
    data class AccountTokenWithSecretDto(
        val token: String = "",
        @SerialName("token_id") val tokenId: String = "",
        @SerialName("access_token") val accessToken: String = "",
        val name: String? = null,
        val label: String? = null,
        @SerialName("token_prefix") val tokenPrefix: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
    ) {
        /** The raw token, returned at most once; either `access_token` or `token`. */
        val secret: String get() = accessToken.ifBlank { token }
    }
}
