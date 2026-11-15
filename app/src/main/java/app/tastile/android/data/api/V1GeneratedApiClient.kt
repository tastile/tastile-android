package app.tastile.android.data.api

import app.tastile.android.data.api.generated.v1.apis.ReadApi
import app.tastile.android.data.api.generated.v1.apis.SourceTileApi
import app.tastile.android.data.api.generated.v1.models.CancelSourceTileRequest
import app.tastile.android.data.api.generated.v1.models.CommandResponse
import app.tastile.android.data.api.generated.v1.models.CreateSourceTileRequest
import app.tastile.android.data.api.generated.v1.models.PlacementTileListResponse
import app.tastile.android.data.api.generated.v1.models.PublishScheduleDefinitionRequest
import app.tastile.android.data.api.generated.v1.models.ReflowSourceTileRequest
import app.tastile.android.data.api.generated.v1.models.SourceTileDetailRead
import app.tastile.android.data.api.generated.v1.models.SourceTileListResponse
import app.tastile.android.data.api.generated.v1.models.TileListResponse
import app.tastile.android.data.api.generated.v1.models.UpdateSourceTileRequest
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

/**
 * Type-safe wrapper over the openapi-generator-produced Retrofit interfaces
 * (`ReadApi`, `SourceTileApi`) declared in the canonical OpenAPI 3.1 spec
 * at the cross-repo submodule `../../openapi/openapi.yaml` (resolved by
 * `openapi.input` in `gradle.properties`).
 *
 * Why a wrapper:
 * 1. The generated methods return `retrofit2.Response<T>`. Callers want `T`
 *    on success and a domain-shaped `V1Error` on failure.
 * 2. Single entry point for the v1 spec surface; consumers don't import
 *    two separate generated interfaces.
 * 3. The 10 documented operations (`getSourceTile`, `cancelSourceTile`,
 *    `getSourceTileCompletion`, `listSourceTilePlacements`, `listSourceTiles`,
 *    `reflowSourceTile`, `updateSourceTile`, `createSourceTile`,
 *    `publishScheduleDefinition`, `listTiles`) are exposed with spec-correct
 *    signatures.
 *
 * The DTO types are the Moshi-annotated classes from the
 * `app.tastile.android.data.api.generated.v1.models` package. They are
 * NOT mapped to the hand-written `V1Models.kt` types because the wire shape
 * diverged (UUID type, additional `relations` field, sealed `oneOf` for
 * discriminated unions). For the existing hand-rolled paths, the
 * `V1ApiClient` facade remains the source of truth.
 */
@Singleton
class V1GeneratedApiClient @Inject constructor(
    private val readApi: ReadApi,
    private val sourceTileApi: SourceTileApi,
) {
    // ---- Read API (v1/14 §9) ----------------------------------------------

    /**
     * Contract v3.0.0: `GET /v1/tiles` returns the keyset-paged
     * `TileListResponse` envelope (`{items, count, next_cursor}`); `offset`
     * was retired in favor of `cursor` and is intentionally not exposed.
     */
    suspend fun listTiles(
        ownerIds: String? = null,
        limit: Long? = null,
        cursor: String? = null,
        viewMode: String? = null,
        lifecycle: String? = null,
        range: String? = null,
        granularity: String? = null,
        search: String? = null,
        excludeFuture: Boolean? = null,
    ): TileListResponse = readApi.listTiles(
        ownerIds = ownerIds,
        limit = limit,
        cursor = cursor,
        viewMode = viewMode,
        lifecycle = lifecycle,
        range = range,
        granularity = granularity,
        search = search,
        excludeFuture = excludeFuture,
    ).unwrap()

    // ---- SourceTile API (canonical surface) -------------------------------

    /**
     * Contract v2.0.0: `GET /v1/source-tiles` returns a keyset-paged
     * envelope (`{items, count, next_cursor}`); the `offset` parameter was
     * removed in favor of `cursor`.
     */
    suspend fun listSourceTiles(
        ownerId: java.util.UUID? = null,
        limit: Long? = null,
        cursor: String? = null,
    ): SourceTileListResponse = sourceTileApi.listSourceTiles(
        ownerId = ownerId,
        limit = limit,
        cursor = cursor,
    ).unwrap()

    suspend fun createSourceTile(request: CreateSourceTileRequest): CommandResponse =
        sourceTileApi.createSourceTile(request).unwrap()

    /**
     * Contract v2.0.0: `GET /v1/source-tiles/{id}` pages its `occurrences`
     * and `placements` collections independently via `occurrence_*` /
     * `placement_*` limit+cursor parameter pairs; the detail's collections
     * are paged envelopes, not bare arrays.
     */
    suspend fun getSourceTile(
        id: java.util.UUID,
        ownerId: java.util.UUID? = null,
        occurrenceLimit: Long? = null,
        occurrenceCursor: String? = null,
        placementLimit: Long? = null,
        placementCursor: String? = null,
    ): SourceTileDetailRead = sourceTileApi.getSourceTile(
        id = id,
        ownerId = ownerId,
        occurrenceLimit = occurrenceLimit,
        occurrenceCursor = occurrenceCursor,
        placementLimit = placementLimit,
        placementCursor = placementCursor,
    ).unwrap()

    suspend fun updateSourceTile(
        id: java.util.UUID,
        request: UpdateSourceTileRequest,
    ): CommandResponse = sourceTileApi.updateSourceTile(id, request).unwrap()

    suspend fun cancelSourceTile(
        id: java.util.UUID,
        request: CancelSourceTileRequest,
    ): CommandResponse = sourceTileApi.cancelSourceTile(id, request).unwrap()

    suspend fun reflowSourceTile(
        id: java.util.UUID,
        request: ReflowSourceTileRequest,
    ): CommandResponse = sourceTileApi.reflowSourceTile(id, request).unwrap()

    suspend fun getSourceTileCompletion(
        id: java.util.UUID,
        ownerId: java.util.UUID? = null,
    ): Any = sourceTileApi.getSourceTileCompletion(id, ownerId).unwrap()

    /**
     * Contract v2.0.0: `GET /v1/source-tiles/{id}/placements` returns a
     * keyset-paged envelope; `offset` was removed in favor of `cursor`.
     */
    suspend fun listSourceTilePlacements(
        id: java.util.UUID,
        ownerId: java.util.UUID? = null,
        limit: Long? = null,
        cursor: String? = null,
    ): PlacementTileListResponse = sourceTileApi.listSourceTilePlacements(
        id = id,
        ownerId = ownerId,
        limit = limit,
        cursor = cursor,
    ).unwrap()

    suspend fun publishScheduleDefinition(
        request: PublishScheduleDefinitionRequest,
    ): CommandResponse = sourceTileApi.publishScheduleDefinition(request).unwrap()
}

/**
 * Unwrap a Retrofit `Response<T>` to its body, mapping non-2xx to a
 * [V1Error]. The generated client uses Moshi; we cannot piggy-back on the
 * existing hand-rolled `V1Error.fromApiBody` because it expects a
 * `V1ApiErrorBody` instance — instead, we decode the error body using a
 * fresh [kotlinx.serialization] call here, keeping the wrapper free of
 * the generated layer's JSON codec.
 */
private fun <T> Response<T>.unwrap(): T {
    if (isSuccessful) return body() ?: throw V1Error.Unknown(code(), "<empty body>")
    val raw = errorBody()?.string().orEmpty()
    val parsed = runCatching {
        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString<V1ApiErrorBody>(raw)
    }.getOrNull()
    if (parsed != null) throw V1Error.fromApiBody(parsed)
    throw V1Error.Unknown(code(), raw.take(200))
}
