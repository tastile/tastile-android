package app.tastile.android.data.auth

import app.tastile.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thin HTTP wrapper for the BetterAuth endpoints exposed by the Tastile Web
 * Next.js app (`POST /api/auth/sign-in/email`, `POST /api/auth/sign-up/email`,
 * `POST /api/auth/sign-out`, `GET /api/auth/session`).
 *
 * The client is built around `HttpURLConnection` to match the rest of the
 * project's networking layer (`V1ApiClient`, `CognitoAccountApi`'s successor).
 * Cookie handling: the BetterAuth session is delivered as
 * `Set-Cookie: better-auth.session_token=<value>; ...`. We capture the raw
 * token value into [BetterAuthSession.sessionToken] so the data layer can
 * persist it to the Keystore-backed encrypted SharedPreferences.
 *
 * Concurrency: this class is stateless apart from the base URL and JSON
 * codec. Callers may invoke it concurrently from multiple coroutines.
 */
class BetterAuthHttpClient(
    private val baseUrl: String = BuildConfig.WEB_BASE_URL.trim().trimEnd('/'),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Result of a sign-in / sign-up / session lookup. Mirrors BetterAuth's
     * `Session` payload — `sub` is the user id, `email` is the primary
     * contact, `expiresAt` is an epoch-seconds timestamp carried by the
     * session cookie's `Max-Age`.
     */
    data class BetterAuthSession(
        val sessionToken: String,
        val userId: String,
        val email: String?,
        val expiresAtEpochSeconds: Long?,
    )

    /** Parses a `Set-Cookie` value for the BetterAuth session token. */
    internal fun extractSessionToken(setCookieHeaders: List<String>?): String? {
        if (setCookieHeaders.isNullOrEmpty()) return null
        for (header in setCookieHeaders) {
            // BetterAuth emits the cookie with attributes like
            //   better-auth.session_token=abc123; Path=/; HttpOnly; SameSite=Lax
            // We only need the name=value pair.
            val firstPair = header.substringBefore(';').trim()
            if (firstPair.startsWith(SESSION_COOKIE_NAME + "=")) {
                val value = firstPair.substring(SESSION_COOKIE_NAME.length + 1)
                if (value.isNotBlank()) return value
            }
        }
        return null
    }

    suspend fun signIn(email: String, password: String): BetterAuthSession = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("email", JsonPrimitive(email))
            put("password", JsonPrimitive(password))
        }
        val response = postJson(SIGN_IN_PATH, body)
        val sessionToken = response.cookies.firstNotNullOfOrNull { extractSessionToken(listOf(it)) }
            ?: throw BetterAuthException("Sign-in response missing session cookie")
        decodeSession(sessionToken, response.body)
    }

    suspend fun signUp(email: String, password: String, name: String): BetterAuthSession = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("email", JsonPrimitive(email))
            put("password", JsonPrimitive(password))
            put("name", JsonPrimitive(name))
        }
        val response = postJson(SIGN_UP_PATH, body)
        val sessionToken = response.cookies.firstNotNullOfOrNull { extractSessionToken(listOf(it)) }
            ?: throw BetterAuthException("Sign-up response missing session cookie")
        decodeSession(sessionToken, response.body)
    }

    suspend fun signOut(sessionToken: String) = withContext(Dispatchers.IO) {
        // Fire and forget — a 401 here is acceptable (the session is already
        // gone client-side). Surface any non-2xx network errors so the
        // /api/auth/sign-out caller can decide whether to retry.
        val connection = openConnection(SIGN_OUT_PATH, "POST", authenticated = true, sessionToken = sessionToken)
        connection.readSetCookieHeaders()
        connection.discard()
    }

    suspend fun getSession(sessionToken: String): BetterAuthSession? = withContext(Dispatchers.IO) {
        val connection = openConnection(SESSION_PATH, "GET", authenticated = true, sessionToken = sessionToken)
        val status = connection.responseCode
        val body = connection.readBody()
        connection.disconnect()
        if (status == 401) return@withContext null
        if (status !in 200..299) {
            throw BetterAuthException("session lookup failed: HTTP $status ${body.take(200)}")
        }
        decodeSession(sessionToken, body)
    }

    private data class JsonResponse(val body: String, val cookies: List<String>)

    private fun postJson(path: String, body: JsonElement): JsonResponse {
        val connection = openConnection(path, "POST", authenticated = false, sessionToken = null)
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val responseBody = connection.readBody()
        val cookies = connection.readSetCookieHeaders()
        connection.disconnect()
        if (status !in 200..299) {
            throw BetterAuthException("POST $path failed: HTTP $status ${responseBody.take(200)}")
        }
        return JsonResponse(responseBody, cookies)
    }

    private fun openConnection(
        path: String,
        method: String,
        authenticated: Boolean,
        sessionToken: String?,
    ): HttpURLConnection {
        val url = URL("$baseUrl$path")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            doInput = true
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
            if (authenticated && !sessionToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $sessionToken")
            }
        }
        return connection
    }

    private fun HttpURLConnection.readBody(): String {
        val status = responseCode
        val stream = if (status in 200..299) inputStream else errorStream
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private fun HttpURLConnection.readSetCookieHeaders(): List<String> {
        val headers = getHeaderFields() ?: return emptyList()
        // The first entry is keyed by null (the status line) — strip it before
        // collecting the `Set-Cookie` values.
        return headers.entries
            .filter { (key, _) -> key != null && key.equals("Set-Cookie", ignoreCase = true) }
            .mapNotNull { (_, values) -> values?.filterNotNull() }
            .flatten()
    }

    private fun HttpURLConnection.discard() {
        try {
            inputStream.use { it.readBytes() }
        } catch (_: IOException) {
            errorStream?.use { it.readBytes() }
        }
    }

    private fun decodeSession(sessionToken: String, body: String): BetterAuthSession {
        if (body.isBlank()) {
            return BetterAuthSession(sessionToken, userId = "", email = null, expiresAtEpochSeconds = null)
        }
        return try {
            val obj = json.parseToJsonElement(body).jsonObject
            // BetterAuth nests session-level fields under `session` (e.g.
            //   {"user":{...},"session":{"expiresAt":1700000000,"token":"..."}})
            // while user-level fields (id, email) live under `user`. Read
            // the session object first and fall back to the top level for
            // older / custom payloads.
            val sessionObj = obj["session"]?.jsonObject
            val userId = obj["user"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                ?: obj["sub"]?.jsonPrimitive?.contentOrNull
                ?: ""
            val email = obj["user"]?.jsonObject?.get("email")?.jsonPrimitive?.contentOrNull
                ?: obj["email"]?.jsonPrimitive?.contentOrNull
            val expiresAt = sessionObj?.get("expiresAt")?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: sessionObj?.get("exp")?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: obj["expiresAt"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: obj["exp"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            BetterAuthSession(
                sessionToken = sessionToken,
                userId = userId,
                email = email,
                expiresAtEpochSeconds = expiresAt,
            )
        } catch (e: Exception) {
            throw BetterAuthException("Malformed BetterAuth response: ${e.message}")
        }
    }

    companion object {
        private const val SESSION_COOKIE_NAME = "better-auth.session_token"
        private const val SIGN_IN_PATH = "/api/auth/sign-in/email"
        private const val SIGN_UP_PATH = "/api/auth/sign-up/email"
        private const val SIGN_OUT_PATH = "/api/auth/sign-out"
        private const val SESSION_PATH = "/api/auth/session"
    }
}

class BetterAuthException(message: String) : RuntimeException(message)
