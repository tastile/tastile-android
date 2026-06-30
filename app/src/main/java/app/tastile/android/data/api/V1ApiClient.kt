package app.tastile.android.data.api

import app.tastile.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
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

typealias AuthTokenProvider = () -> String?

@Singleton
class V1ApiClient @Inject constructor(
    private val tokenProvider: AuthTokenProvider
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun baseUrl(): String =
        BuildConfig.TASTILE_CORE_URL.trim().trimEnd('/')

    private suspend inline fun <reified T> get(path: String): T = withContext(Dispatchers.IO) {
        try {
            val token = tokenProvider()
            if (token.isNullOrBlank()) throw V1Error.Auth()
            val url = URL("${baseUrl()}$path")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                doInput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                val err = runCatching { json.decodeFromString<V1ApiErrorBody>(body) }.getOrNull()
                if (err != null) throw V1Error.fromApiBody(err)
                throw V1Error.Unknown(status, body.take(200))
            }
            json.decodeFromString<T>(body)
        } catch (e: IOException) {
            throw V1Error.Network(e)
        }
    }

    suspend fun listTiles(): V1ListTilesResponse =
        get("/v1/tiles")

    suspend fun readTile(tileId: String): TileDetailView =
        get("/v1/tiles/$tileId")

    suspend fun listPlacements(): List<V1PlacementListItem> =
        get("/v1/placements")

    suspend fun getExecution(executionId: String): V1ExecutionView =
        get("/v1/executions/$executionId")

    suspend fun getTimeline(start: Instant, end: Instant): V1TimelineResponse {
        val startIso = URLEncoder.encode(start.toString(), Charsets.UTF_8.name())
        val endIso = URLEncoder.encode(end.toString(), Charsets.UTF_8.name())
        return get("/v1/timeline?start=$startIso&end=$endIso")
    }

    suspend fun listRuntimePaths(): V1ListRuntimePathsResponse =
        get("/v1/runtime/paths")

    suspend fun <Req, Resp> postCommand(
        path: String,
        commandKind: String,
        payload: Req,
        payloadSerializer: KSerializer<Req>,
        responseSerializer: KSerializer<Resp>,
        expectedRevision: Long? = null
    ): Resp = withContext(Dispatchers.IO) {
        try {
            val token = tokenProvider()
            if (token.isNullOrBlank()) throw V1Error.Auth()
            val envelope = buildJsonObject {
                put("expectedRevision", expectedRevision?.let { JsonPrimitive(it) } ?: JsonNull)
                put("idempotencyKey", V1Idempotency.generate())
                put("occurredAt", Instant.now().toString())
                put("payload", buildJsonObject {
                    put("kind", commandKind)
                    put("value", json.encodeToJsonElement(payloadSerializer, payload))
                })
            }
            val url = URL("${baseUrl()}$path")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            connection.outputStream.use { it.write(envelope.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                val err = runCatching { json.decodeFromString<V1ApiErrorBody>(body) }.getOrNull()
                if (err != null) throw V1Error.fromApiBody(err)
                throw V1Error.Unknown(status, body.take(200))
            }
            json.decodeFromString(responseSerializer, body)
        } catch (e: IOException) {
            throw V1Error.Network(e)
        }
    }

    /**
     * Issues a POST request whose body is sent verbatim (no `CommandRequest`
     * envelope wrapping).  Used by Macro Step 5 for endpoints that bypass the
     * standard envelope: `POST /v1/prompts` and
     * `POST /v1/prompts/startup-recovery`.  Idempotency keys are still added
     * via the `Idempotency-Key` header so retry dedup still works.
     */
    suspend fun <Resp> postRawJson(
        path: String,
        body: JsonObject,
        responseSerializer: KSerializer<Resp>
    ): Resp = withContext(Dispatchers.IO) {
        try {
            val token = tokenProvider()
            if (token.isNullOrBlank()) throw V1Error.Auth()
            val url = URL("${baseUrl()}$path")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Idempotency-Key", V1Idempotency.generate())
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                val err = runCatching { json.decodeFromString<V1ApiErrorBody>(responseBody) }.getOrNull()
                if (err != null) throw V1Error.fromApiBody(err)
                throw V1Error.Unknown(status, responseBody.take(200))
            }
            json.decodeFromString(responseSerializer, responseBody)
        } catch (e: IOException) {
            throw V1Error.Network(e)
        }
    }

    /**
     * Issues a DELETE request to the v1 endpoint. Mirrors [postCommand]'s shape but
     * with no body — used for `tile.delete` (`DELETE /v1/tiles/{id}`). The
     * endpoint still returns a `CommandResponse` envelope, which is decoded via
     * the supplied [responseSerializer].
     */
    suspend fun <Resp> deleteCommand(
        path: String,
        responseSerializer: KSerializer<Resp>
    ): Resp = withContext(Dispatchers.IO) {
        try {
            val token = tokenProvider()
            if (token.isNullOrBlank()) throw V1Error.Auth()
            val url = URL("${baseUrl()}$path")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                doInput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                val err = runCatching { json.decodeFromString<V1ApiErrorBody>(body) }.getOrNull()
                if (err != null) throw V1Error.fromApiBody(err)
                throw V1Error.Unknown(status, body.take(200))
            }
            json.decodeFromString(responseSerializer, body)
        } catch (e: IOException) {
            throw V1Error.Network(e)
        }
    }
}