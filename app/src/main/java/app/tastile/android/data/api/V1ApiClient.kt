package app.tastile.android.data.api

import app.tastile.android.BuildConfig
import app.tastile.android.data.repository.TileFilter
import app.tastile.android.data.repository.toQueryString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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

typealias AuthTokenProvider = suspend () -> String?

/** Canonical endpoint paths shared with tastile-web's v1 command client. */
internal object V1Endpoints {
    const val CREATE_TILE = "/v1/tiles"
    const val CREATE_PLACEMENT = "/v1/placements"

    fun setPlan(tileId: String) = "/v1/tiles/$tileId/plan"
    fun materializeRecurring(tileId: String, frameRuleId: String) =
        "/v1/recurring/$tileId/frame-rules/$frameRuleId/materialize"

    fun timeline(start: Instant, end: Instant): String {
        val startIso = URLEncoder.encode(start.toString(), Charsets.UTF_8.name())
        val endIso = URLEncoder.encode(end.toString(), Charsets.UTF_8.name())
        return "/v1/timeline?start=$startIso&end=$endIso"
    }
}

/** Serializes the v1 `CommandRequest<T>` wire format used by tastile-web. */
internal object V1Wire {
    fun commandEnvelope(
        payload: JsonElement,
        idempotencyKey: String = V1Idempotency.generate(),
        occurredAt: String = Instant.now().toString(),
        expectedRevision: Long? = null,
    ): JsonObject = buildJsonObject {
        put("expected_revision", expectedRevision?.let(::JsonPrimitive) ?: JsonNull)
        put("idempotency_key", idempotencyKey)
        put("occurred_at", occurredAt)
        put("payload", payload)
    }
}

@Singleton
class V1ApiClient @Inject constructor(
    private val tokenProvider: AuthTokenProvider
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun baseUrl(): String =
        BuildConfig.TASTILE_CORE_URL.trim().trimEnd('/')

    private fun webAuthBaseUrl(): String =
        BuildConfig.COGNITO_WEB_AUTH_BASE_URL.trim().trimEnd('/')

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

    suspend fun getTiles(filter: TileFilter = TileFilter.DEFAULT): V1ListTilesResponse {
        val query = filter.toQueryString()
        return if (query.isEmpty()) {
            get("/v1/tiles")
        } else {
            get("/v1/tiles?$query")
        }
    }

    suspend fun readTile(tileId: String): TileDetailView =
        get("/v1/tiles/$tileId")

    suspend fun listPlacements(): List<V1PlacementListItem> =
        get("/v1/placements")

    suspend fun getTimeline(start: Instant, end: Instant): V1TimelineResponse {
        return get(V1Endpoints.timeline(start, end))
    }

    suspend fun createTile(payload: CreateTilePayload): CommandResponse =
        postCommand(V1Endpoints.CREATE_TILE, payload, CreateTilePayload.serializer(), CommandResponse.serializer())

    suspend fun setPlan(tileId: String, payload: SetPlanPayload): CommandResponse =
        postCommand(V1Endpoints.setPlan(tileId), payload, SetPlanPayload.serializer(), CommandResponse.serializer())

    suspend fun createPlacement(payload: CreatePlacementPayload): CommandResponse =
        postCommand(V1Endpoints.CREATE_PLACEMENT, payload, CreatePlacementPayload.serializer(), CommandResponse.serializer())

    suspend fun materializeRecurring(payload: MaterializeRecurringPayload): CommandResponse =
        postCommand(
            V1Endpoints.materializeRecurring(payload.recurringId, payload.frameRuleId),
            payload,
            MaterializeRecurringPayload.serializer(),
            CommandResponse.serializer(),
        )

    suspend fun listRuntimePaths(): V1ListRuntimePathsResponse =
        get("/v1/runtime/paths")

    // --- C5 Projects (workspaces) ---------------------------------------
    //
    // All three endpoints are *raw JSON* (no CommandRequest envelope) — the
    // web hooks call them through `getCoreClient().call("listMyWorkspaces")`
    // with no envelope. The Rust handlers at
    // `crates/v1/api/src/handlers/access.rs` accept a plain JSON body, so
    // we follow suit.

    suspend fun listWorkspaces(): V1ListWorkspacesResponse =
        get("/v1/access/subjects?kind=1")

    /**
     * `POST /v1/access/workspaces` body `{ display_name, slug?, color?,
     * parent_subject_id? }`. Returns the created `Workspace` row per the
     * 201 CREATED wire shape.
     */
    suspend fun createWorkspace(input: CreateWorkspaceInput): Workspace {
        val body = buildJsonObject {
            put("display_name", input.displayName)
            put("slug", input.slug?.let { JsonPrimitive(it) } ?: JsonNull)
            put("color", input.color?.let { JsonPrimitive(it) } ?: JsonNull)
            put("parent_subject_id", input.parentSubjectId?.let { JsonPrimitive(it) } ?: JsonNull)
        }
        return postRawJson(
            path = "/v1/access/workspaces",
            body = body,
            responseSerializer = Workspace.serializer(),
        )
    }

    /**
     * `DELETE /v1/access/subjects/{id}` returns 204 NO CONTENT with no body.
     * Surface that as a unit (`Unit`) and let the caller's `Response` chain
     * collapse it.
     */
    suspend fun deleteWorkspace(id: String) {
        deleteRaw(path = "/v1/access/subjects/$id")
    }

    suspend fun <Req, Resp> postCommand(
        path: String,
        payload: Req,
        payloadSerializer: KSerializer<Req>,
        responseSerializer: KSerializer<Resp>,
        expectedRevision: Long? = null,
        @Suppress("UNUSED_PARAMETER") commandKind: String? = null,
    ): Resp = withContext(Dispatchers.IO) {
        try {
            val token = tokenProvider()
            if (token.isNullOrBlank()) throw V1Error.Auth()
            val encodedPayload = json.encodeToJsonElement(payloadSerializer, payload)
            val envelope = V1Wire.commandEnvelope(
                payload = encodedPayload,
                expectedRevision = expectedRevision,
            )
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

    suspend fun <Resp> postNullCommand(
        path: String,
        responseSerializer: KSerializer<Resp>,
    ): Resp = postCommand(
        path = path,
        payload = JsonNull,
        payloadSerializer = JsonElement.serializer(),
        responseSerializer = responseSerializer,
    )

    /**
     * Issues a POST request whose body is sent verbatim (no CommandRequest
     * envelope). Used for endpoints that take a plain JSON payload — C5
     * projects (`POST /v1/access/workspaces`).
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

    /**
     * Issues a DELETE request whose response body is unused. Used by
     * C5 projects (`DELETE /v1/access/subjects/{id}` returns
     * 204 NO CONTENT) where the [deleteCommand] envelope-decoding
     * path is not appropriate.
     */
    suspend fun deleteRaw(path: String) = withContext(Dispatchers.IO) {
        try {
            val token = tokenProvider()
            if (token.isNullOrBlank()) throw V1Error.Auth()
            val url = URL("${baseUrl()}$path")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val err = runCatching { json.decodeFromString<V1ApiErrorBody>(body) }.getOrNull()
                if (err != null) throw V1Error.fromApiBody(err)
                throw V1Error.Unknown(status, body.take(200))
            }
        } catch (e: IOException) {
            throw V1Error.Network(e)
        }
    }

    /**
     * Mints the first Tastile API token through the web public-client boundary.
     * The Cognito access token is verified server-side; the Core bridge secret
     * never leaves the web host or enters the Android artifact.
     */
    suspend fun mintApiTokenViaWeb(
        accessToken: String,
        request: V1ApiTokenCreateRequest
    ): V1ApiTokenCreateResponse = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("label", request.label?.let { JsonPrimitive(it) } ?: JsonNull)
            }
            val url = URL("${webAuthBaseUrl()}/api/mobile/api-token")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
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
            json.decodeFromString<V1ApiTokenCreateResponse>(responseBody)
        } catch (e: IOException) {
            throw V1Error.Network(e)
        }
    }
}
