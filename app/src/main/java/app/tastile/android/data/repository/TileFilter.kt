package app.tastile.android.data.repository

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Query-string filter for `GET /v1/tiles`, matching the web client's
 * `useTileList` hook (`tastile-web/src/lib/hooks/use-tile-list.ts`).
 *
 * Defaults mirror the web hook's initial state: `view_mode = "list"` and
 * `limit = 20` are always emitted; optional fields are emitted only when
 * non-blank / non-empty so the backend sees the same shape the web client
 * produces.
 */
data class TileFilter(
    val viewMode: String = "list",
    val lifecycle: String? = null,
    val limit: Int = 20,
    val search: String? = null,
    val excludeFuture: Boolean = false,
    val range: String? = null,
    val granularity: String? = null,
    val ownerIds: List<String> = emptyList(),
) {
    /**
     * Render the filter as a `Map<String, String>` ready for HTTP query
     * encoding. Keys are snake_case to match the v1 endpoint contract.
     * `view_mode` and `limit` are always present; nullable strings are
     * included only when non-blank; `owner_ids` is the comma-joined
     * `ownerIds` list, present only when at least one id is supplied.
     */
    fun toQueryParameters(): Map<String, String> {
        val params = linkedMapOf<String, String>()
        params["view_mode"] = viewMode
        if (limit > 0) params["limit"] = limit.toString()
        if (!lifecycle.isNullOrBlank()) params["lifecycle"] = lifecycle
        if (!search.isNullOrBlank()) params["search"] = search
        if (excludeFuture) params["exclude_future"] = "true"
        if (!range.isNullOrBlank()) params["range"] = range
        if (!granularity.isNullOrBlank()) params["granularity"] = granularity
        if (ownerIds.isNotEmpty()) {
            params["owner_ids"] = ownerIds.joinToString(",")
        }
        return params
    }

    companion object {
        val DEFAULT: TileFilter = TileFilter()
    }
}

/**
 * Encode each value of [TileFilter.toQueryParameters] as `application/x-www-form-urlencoded`.
 * Kept as a top-level helper so callers (e.g. `V1ApiClient.getTiles`) can build the
 * final query string without re-importing `URLEncoder`.
 */
internal fun TileFilter.toQueryString(): String =
    toQueryParameters()
        .entries
        .joinToString("&") { (key, value) ->
            val encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.name())
            val encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            "$encodedKey=$encodedValue"
        }