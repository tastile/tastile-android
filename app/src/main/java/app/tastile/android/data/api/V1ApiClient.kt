package app.tastile.android.data.api

import android.util.Log
import app.tastile.android.BuildConfig
import app.tastile.android.data.tile.TileFilter
import app.tastile.android.data.tile.toQueryString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
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
    const val SOURCE_TILES = "/v1/source-tiles"
    const val ACTIVE_EXECUTION = "/v1/active-tile"
    const val PENDING_SESSIONS = "/v1/sessions"
    const val ENDPOINTS = "/v1/endpoints"

    fun sourceTile(sourceTileId: String) = "$SOURCE_TILES/$sourceTileId"
    fun reflowSourceTile(sourceTileId: String) = "${sourceTile(sourceTileId)}/reflow"
    fun sourceTilePlacements(sourceTileId: String) = "${sourceTile(sourceTileId)}/placements"
    fun execution(executionId: String) = "/v1/executions/$executionId"
    fun startExecution(placementId: String) = "/v1/placements/$placementId/executions"
    fun pauseExecution(executionId: String) = "${execution(executionId)}/pause"
    fun resumeExecution(executionId: String) = "${execution(executionId)}/resume"
    fun finishExecution(executionId: String) = "${execution(executionId)}/finish"
    fun session(sessionId: String) = "/v1/sessions/$sessionId"
    fun sessionFeedback(sessionId: String) = "${session(sessionId)}/feedback"
    fun endpoint(endpointId: String) = "$ENDPOINTS/$endpointId"

    fun setPlan(tileId: String) = "/v1/tiles/$tileId/plan"
    fun materializeRecurring(tileId: String, frameRuleId: String) =
        "/v1/recurring/$tileId/frame-rules/$frameRuleId/materialize"

    fun timeline(start: Instant, end: Instant, ownerIds: List<String> = emptyList()): String {
        val startIso = URLEncoder.encode(start.toString(), Charsets.UTF_8.name())
        val endIso = URLEncoder.encode(end.toString(), Charsets.UTF_8.name())
        val owners = ownerIds.filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(",")
            ?.let { "&owner_ids=${URLEncoder.encode(it, Charsets.UTF_8.name())}" }
            .orEmpty()
        return "/v1/timeline?start=$startIso&end=$endIso$owners"
    }
}
 
 private const val HTTP_UNAUTHORIZED = 401
private const val TAG = "V1ApiClient"

/**
 * Page size for the keyset-paged read endpoints (contract v2.0.0+). The
 * spec accepts 1..1000 (default 100); the client always asks for the maximum so
 * full-list reads need as few round-trips as possible.
 */
private const val PAGED_READ_LIMIT = 1000

/**
 * Safety bound on cursor-following loops. A well-behaved server ends every
 * list with `next_cursor = null`; this only guards against a buggy cursor
 * that never terminates.
 */
private const val MAX_PAGE_COUNT = 100

private fun encodeQueryParam(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name())
 
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

internal object V1WorkspaceWire {
    fun updateBody(input: UpdateWorkspaceInput): JsonObject = buildJsonObject {
        put("display_name", input.displayName?.let(::JsonPrimitive) ?: JsonNull)
        put("slug", input.slug?.let(::JsonPrimitive) ?: JsonNull)
        put("color", input.color?.let(::JsonPrimitive) ?: JsonNull)
        put("parent_subject_id", input.parentSubjectId?.let(::JsonPrimitive) ?: JsonNull)
    }
}

@Singleton
class V1ApiClient @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
    private val tokenRefresher: TokenRefresher,
) {
    private var coreUrlOverride: String? = null

    /**
     * Test-only constructor that points the client at a local server.
     * [tokenRefresher] defaults to a no-op that reports failure, so a test
     * server that answers 401 fails the request with [V1Error.Auth]
     * instead of retrying; retry tests pass a real refresher.
     */
    internal constructor(
        tokenProvider: AuthTokenProvider,
        coreUrl: String,
        tokenRefresher: TokenRefresher = TokenRefresher { null },
    ) : this(tokenProvider, tokenRefresher) {
        coreUrlOverride = coreUrl.trim().trimEnd('/')
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun baseUrl(): String =
        coreUrlOverride ?: BuildConfig.TASTILE_CORE_URL.trim().trimEnd('/')

    private fun webAuthBaseUrl(): String =
        BuildConfig.WEB_BASE_URL.trim().trimEnd('/')

    /**
     * Runs [block] with a bearer token, recovering from a Core 401 exactly
     * once: the rejected [staleToken] is discarded and re-minted through
     * [TokenRefresher.refreshAfterUnauthorized] (single-flight, so
     * parallel 401s share one re-mint), then [block] runs once more with
     * the replacement token.
     *
     * Contracts:
     *  - 401 is the only refresh trigger. The minted token expires at
     *    now + 90 days (tastile-web#199) and time-based renewal is out of
     *    scope here — a non-401 error propagates without any retry.
     *  - a failed re-mint surfaces as [V1Error.Auth]; by then
     *    [app.tastile.android.data.auth.ApiTokenCache] has already
     *    transitioned the auth state to unauthenticated, so callers stop
     *    instead of retrying (bounded: at most one retry per request).
     *  - a second 401 propagates as [V1Error.Unauthorized] — never a
     *    third attempt.
     */
    private suspend fun <T> withBearer(block: suspend (token: String) -> T): T {
        val stale = tokenProvider()
        if (stale.isNullOrBlank()) throw V1Error.Auth()
        try {
            return block(stale)
        } catch (e: V1Error.Unauthorized) {
            Log.w(TAG, "core rejected bearer; re-minting and retrying the request once")
            val fresh = tokenRefresher.refreshAfterUnauthorized(stale)
                ?: throw V1Error.Auth()
            return block(fresh)
        }
    }

    /** Maps a non-2xx Core status onto the sealed [V1Error]; 401 first. */
    private fun errorFor(status: Int, body: String): V1Error =
        if (status == HTTP_UNAUTHORIZED) {
            V1Error.Unauthorized()
        } else {
            runCatching { json.decodeFromString<V1ApiErrorBody>(body) }.getOrNull()
                ?.let { V1Error.fromApiBody(it) }
                ?: V1Error.Unknown(status, body.take(200))
        }

    private suspend inline fun <reified T> get(path: String): T =
        json.decodeFromString(getRaw(path))

    /**
     * Fetches every page of a `{items, count, next_cursor}` list endpoint
     * (contract v2.0.0+, extended to more reads in v3.0.0) and returns the
     * concatenated items. Callers that
     * need the full list — e.g. `findPlacementForTile` picking the freshest
     * placement — must not stop at the first page.
     */
    private suspend inline fun <reified T> getAllPages(path: String): List<T> {
        val items = mutableListOf<T>()
        var cursor: String? = null
        var pages = 0
        do {
            pages += 1
            if (pages > MAX_PAGE_COUNT) {
                throw V1Error.Unknown(0, "paged read did not terminate: $path")
            }
            val query = buildString {
                append("limit=$PAGED_READ_LIMIT")
                cursor?.let { append("&cursor=").append(encodeQueryParam(it)) }
            }
            val page: V1PagedRead<T> = get("$path?$query")
            items += page.items
            cursor = page.nextCursor
        } while (cursor != null)
        return items
    }

    private suspend fun getRaw(path: String): String = withBearer { token ->
        withContext(Dispatchers.IO) {
            try {
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
                    throw errorFor(status, body)
                }
                body
            } catch (e: IOException) {
                throw V1Error.Network(e)
            }
        }
    }

    /**
     * `GET /v1/tiles` under contract v3.0.0 returns the keyset-paged
     * `TileListResponse` envelope (`{items, count, next_cursor}`); `offset`
     * was retired in favor of `cursor`. This reads the first page only —
     * the dashboard list is intentionally page-sized by [TileFilter.limit].
     * Callers that must see every row (e.g. `fetchTileById` resolving a
     * tile outside the first page) use [listAllTiles] to follow
     * `next_cursor`.
     */
    suspend fun getTiles(filter: TileFilter = TileFilter.DEFAULT): V1ListTilesResponse {
        val query = filter.toQueryString()
        val path = if (query.isEmpty()) "/v1/tiles" else "/v1/tiles?$query"
        return get(path)
    }

    /**
     * Follows every `next_cursor` page of `GET /v1/tiles` for [filter] and
     * returns the concatenated items, preserving the pre-v3.0.0 full-list
     * semantics for lookups that must not stop at the first page. The
     * filter's own `limit` stays the page size; `cursor` is appended per
     * page.
     */
    suspend fun listAllTiles(filter: TileFilter): List<TileListView> {
        // TileFilter always emits view_mode + limit, so the base query is
        // never empty and "/v1/tiles?…" is always well-formed.
        val base = filter.toQueryString()
        val items = mutableListOf<TileListView>()
        var cursor: String? = null
        var pages = 0
        do {
            pages += 1
            if (pages > MAX_PAGE_COUNT) {
                throw V1Error.Unknown(0, "tiles paging did not terminate: /v1/tiles")
            }
            val page: V1ListTilesResponse = get(
                "/v1/tiles?$base" +
                    cursor?.let { "&cursor=${encodeQueryParam(it)}" }.orEmpty()
            )
            items += page.tiles
            cursor = page.nextCursor
        } while (cursor != null)
        return items
    }

    suspend fun readTile(tileId: String): TileDetailView =
        get("/v1/tiles/$tileId")

    /**
     * `GET /v1/placements` is keyset-paged under contract v2.0.0
     * (`{items, count, next_cursor}`). The dispatcher scans the whole list to
     * find a tile's freshest placement, so every cursor page is followed.
     */
    suspend fun listPlacements(): List<V1PlacementListItem> =
        getAllPages("/v1/placements")

    /** Returns null when the account has no running execution. */
    suspend fun getActiveTile(): ActiveTileView? =
        get(V1Endpoints.ACTIVE_EXECUTION)

    suspend fun readExecution(executionId: String): ExecutionView =
        get(V1Endpoints.execution(executionId))

    suspend fun startExecution(placementId: String): CommandResponse =
        postCommand(V1Endpoints.startExecution(placementId), StartExecutionPayload(placementId), StartExecutionPayload.serializer(), CommandResponse.serializer())

    suspend fun pauseExecution(executionId: String): CommandResponse =
        postNullCommand(V1Endpoints.pauseExecution(executionId), CommandResponse.serializer())

    suspend fun resumeExecution(executionId: String): CommandResponse =
        postNullCommand(V1Endpoints.resumeExecution(executionId), CommandResponse.serializer())

    suspend fun finishExecution(executionId: String, kind: Int, note: String?): CommandResponse =
        postCommand(V1Endpoints.finishExecution(executionId), FinishExecutionPayload(kind, note), FinishExecutionPayload.serializer(), CommandResponse.serializer())

    /**
     * `GET /v1/sessions` is keyset-paged under contract v3.0.0
     * (`{items, count, next_cursor}`); all cursor pages are followed to
     * preserve the pre-v3.0.0 full-list semantics (the old bare array was
     * capped at LIMIT 200).
     */
    suspend fun listPendingSessions(): List<PendingSessionView> =
        getAllPages(V1Endpoints.PENDING_SESSIONS)

    suspend fun listEndpoints(): List<EndpointView> = get(V1Endpoints.ENDPOINTS)

    suspend fun registerEndpoint(payload: EndpointRegistrationPayload): EndpointView =
        postRawJson(
            V1Endpoints.ENDPOINTS,
            json.encodeToJsonElement(EndpointRegistrationPayload.serializer(), payload).jsonObject,
            EndpointView.serializer(),
        )

    suspend fun deleteEndpoint(endpointId: String) = deleteRaw(V1Endpoints.endpoint(endpointId))

    suspend fun readSession(sessionId: String): SessionDetailView = get(V1Endpoints.session(sessionId))

    suspend fun submitSessionFeedback(sessionId: String, payload: ApplyFeedbackPayload): CommandResponse =
        postCommand(
            V1Endpoints.sessionFeedback(sessionId),
            payload,
            ApplyFeedbackPayload.serializer(),
            CommandResponse.serializer(),
            expectedRevision = payload.baseRevision,
        )

    /** `/v1/timeline` returns a bare TimelineItem array, not an `{ items }` envelope. */
    suspend fun getTimeline(start: Instant, end: Instant, ownerIds: List<String> = emptyList()): List<TimelineItem> {
        return get(V1Endpoints.timeline(start, end, ownerIds))
    }

    suspend fun createTile(payload: CreateTilePayload): CommandResponse =
        postCommand(V1Endpoints.CREATE_TILE, payload, CreateTilePayload.serializer(), CommandResponse.serializer())

    suspend fun setPlan(tileId: String, payload: SetPlanPayload): CommandResponse =
        postCommand(V1Endpoints.setPlan(tileId), payload, SetPlanPayload.serializer(), CommandResponse.serializer())

    suspend fun createPlacement(payload: CreatePlacementPayload): CommandResponse =
        postCommand(V1Endpoints.CREATE_PLACEMENT, payload, CreatePlacementPayload.serializer(), CommandResponse.serializer())

    suspend fun createSourceTile(payload: SourceTileWritePayload): CommandResponse =
        postCommand(
            V1Endpoints.SOURCE_TILES,
            payload,
            SourceTileWritePayload.serializer(),
            CommandResponse.serializer(),
        )

    suspend fun updateSourceTile(
        sourceTileId: String,
        payload: SourceTileWritePayload,
        expectedRevision: Long,
    ): CommandResponse = putCommand(
        V1Endpoints.sourceTile(sourceTileId),
        payload,
        SourceTileWritePayload.serializer(),
        CommandResponse.serializer(),
        expectedRevision,
    )

    suspend fun materializeRecurring(payload: MaterializeRecurringPayload): CommandResponse =
        postCommand(
            V1Endpoints.materializeRecurring(payload.recurringId, payload.frameRuleId),
            payload,
            MaterializeRecurringPayload.serializer(),
            CommandResponse.serializer(),
        )

    suspend fun reflowSourceTile(sourceTileId: String, payload: ReflowSourceTilePayload, expectedRevision: Long): CommandResponse =
        postCommand(V1Endpoints.reflowSourceTile(sourceTileId), payload, ReflowSourceTilePayload.serializer(), CommandResponse.serializer(), expectedRevision)

    /**
     * `GET /v1/source-tiles/{id}` pages its `occurrences` and `placements`
     * collections independently (`occurrence_cursor` / `placement_cursor`).
     * Both cursors are followed until exhausted and the pages are merged
     * back into the flat [SourceTileDetailRead] lists, preserving the
     * pre-v2.0.0 full-list semantics for callers.
     */
    suspend fun readSourceTile(sourceTileId: String): SourceTileDetailRead {
        val path = V1Endpoints.sourceTile(sourceTileId)
        val first: SourceTileDetailPage = get(path)
        val occurrences = LinkedHashMap<String, SourceOccurrenceRead>()
        first.occurrences.items.forEach { occurrences[it.occurrenceId] = it }
        val placements = LinkedHashMap<String, SourcePlacementRead>()
        first.placements.items.forEach { placements[it.placementId] = it }
        var occurrenceCursor = first.occurrences.nextCursor
        var placementCursor = first.placements.nextCursor
        var pages = 0
        while (occurrenceCursor != null || placementCursor != null) {
            pages += 1
            if (pages > MAX_PAGE_COUNT) {
                throw V1Error.Unknown(0, "source tile detail paging did not terminate: $path")
            }
            val query = listOfNotNull(
                occurrenceCursor?.let { "occurrence_cursor=${encodeQueryParam(it)}" },
                placementCursor?.let { "placement_cursor=${encodeQueryParam(it)}" },
            ).joinToString("&")
            val page: SourceTileDetailPage = get("$path?$query")
            // A collection whose cursor is exhausted resets to its first
            // page on the next request; dedup by id keeps the merge correct.
            page.occurrences.items.forEach { occurrences.putIfAbsent(it.occurrenceId, it) }
            page.placements.items.forEach { placements.putIfAbsent(it.placementId, it) }
            occurrenceCursor = page.occurrences.nextCursor
            placementCursor = page.placements.nextCursor
        }
        return SourceTileDetailRead(
            source = first.source,
            occurrences = occurrences.values.toList(),
            placements = placements.values.toList(),
        )
    }

    /**
     * `GET /v1/source-tiles/{id}/placements` is keyset-paged under contract
     * v2.0.0; all cursor pages are followed to preserve the full-list
     * contract.
     */
    suspend fun listSourceTilePlacements(sourceTileId: String): List<SourcePlacementRead> =
        getAllPages(V1Endpoints.sourceTilePlacements(sourceTileId))

    suspend fun listRuntimePaths(): V1ListRuntimePathsResponse =
        get("/v1/runtime/paths")

    // --- C5 Projects (workspaces) ---------------------------------------
    //
    // All three endpoints are *raw JSON* (no CommandRequest envelope) — the
    // web hooks call them through `getCoreClient().call("listMyWorkspaces")`
    // with no envelope. The Rust handlers at
    // `crates/v1/api/src/handlers/access.rs` accept a plain JSON body, so
    // we follow suit.

    /**
     * `GET /v1/access/subjects?kind=1` returns only WORKSPACE (=Project)
     * rows. Personal scope (`kind=0`) is synthesized client-side in
     * [app.tastile.android.data.access.AccessRepository] from the
     * authenticated user's id.
     */
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

    /** Plain JSON PATCH, matching the web updateSubject endpoint. */
    suspend fun updateWorkspace(id: String, input: UpdateWorkspaceInput): Workspace {
        val body = V1WorkspaceWire.updateBody(input)
        return patchRawJson(
            path = "/v1/access/subjects/$id",
            body = body,
            responseSerializer = Workspace.serializer(),
        )
    }

    suspend fun <Req, Resp> postCommand(
        path: String,
        payload: Req,
        payloadSerializer: KSerializer<Req>,
        responseSerializer: KSerializer<Resp>,
        expectedRevision: Long? = null,
    ): Resp = withBearer { token ->
        withContext(Dispatchers.IO) {
            try {
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
                    throw errorFor(status, body)
                }
                json.decodeFromString(responseSerializer, body)
            } catch (e: IOException) {
                throw V1Error.Network(e)
            }
        }
    }

    suspend fun <Req, Resp> putCommand(
        path: String,
        payload: Req,
        payloadSerializer: KSerializer<Req>,
        responseSerializer: KSerializer<Resp>,
        expectedRevision: Long? = null,
    ): Resp = withBearer { token ->
        withContext(Dispatchers.IO) {
            try {
                val envelope = V1Wire.commandEnvelope(
                    payload = json.encodeToJsonElement(payloadSerializer, payload),
                    expectedRevision = expectedRevision,
                )
                val connection = (URL("${baseUrl()}$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
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
                    throw errorFor(status, body)
                }
                json.decodeFromString(responseSerializer, body)
            } catch (e: IOException) {
                throw V1Error.Network(e)
            }
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

    suspend fun <Req> postCommandNoResponse(path: String, payload: Req, payloadSerializer: KSerializer<Req>) = withBearer { token ->
        withContext(Dispatchers.IO) {
            val connection = (URL("${baseUrl()}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; doOutput = true; doInput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15_000; readTimeout = 15_000
            }
            val envelope = V1Wire.commandEnvelope(json.encodeToJsonElement(payloadSerializer, payload))
            connection.outputStream.use { it.write(envelope.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw errorFor(status, body)
        }
    }

    /**
     * Issues a POST request whose body is sent verbatim (no CommandRequest
     * envelope). Used for endpoints that take a plain JSON payload — C5
     * projects (`POST /v1/access/workspaces`).
     */
    suspend fun <Resp> postRawJson(
        path: String,
        body: JsonObject,
        responseSerializer: KSerializer<Resp>
    ): Resp = withBearer { token ->
        withContext(Dispatchers.IO) {
            try {
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
                    throw errorFor(status, responseBody)
                }
                json.decodeFromString(responseSerializer, responseBody)
            } catch (e: IOException) {
                throw V1Error.Network(e)
            }
        }
    }

    suspend fun <Resp> patchRawJson(
        path: String,
        body: JsonObject,
        responseSerializer: KSerializer<Resp>,
    ): Resp = withBearer { token ->
        withContext(Dispatchers.IO) {
            try {
                val connection = (URL("${baseUrl()}$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    doOutput = true
                    doInput = true
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    throw errorFor(status, responseBody)
                }
                json.decodeFromString(responseSerializer, responseBody)
            } catch (e: IOException) {
                throw V1Error.Network(e)
            }
        }
    }

    /** Sends a v1 CommandEnvelope DELETE. Tile archive succeeds with 204. */
    suspend fun <Req> deleteCommand(
        path: String,
        payload: Req,
        payloadSerializer: KSerializer<Req>,
        expectedRevision: Long? = null,
    ) = withBearer { token ->
        withContext(Dispatchers.IO) {
            try {
                val url = URL("${baseUrl()}$path")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    doOutput = true
                    doInput = true
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }
                val envelope = V1Wire.commandEnvelope(
                    payload = json.encodeToJsonElement(payloadSerializer, payload),
                    expectedRevision = expectedRevision,
                )
                connection.outputStream.use { it.write(envelope.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }
                    .orEmpty()
                if (status !in 200..299) {
                    throw errorFor(status, body)
                }
                Unit
            } catch (e: IOException) {
                throw V1Error.Network(e)
            }
        }
    }

    /**
     * Issues a DELETE request whose response body is unused. Used by
     * C5 projects (`DELETE /v1/access/subjects/{id}` returns
     * 204 NO CONTENT) where the [deleteCommand] envelope-decoding
     * path is not appropriate.
     */
    suspend fun deleteRaw(path: String) = withBearer { token ->
        withContext(Dispatchers.IO) {
            try {
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
                    throw errorFor(status, body)
                }
            } catch (e: IOException) {
                throw V1Error.Network(e)
            }
        }
    }

    /**
     * Mints the first Tastile API token through the web public-client boundary.
     * The BetterAuth session token is verified server-side; the Core bridge
     * secret never leaves the web host or enters the Android artifact.
     */
    suspend fun mintApiTokenViaWeb(
        sessionToken: String,
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
                setRequestProperty("Authorization", "Bearer $sessionToken")
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
