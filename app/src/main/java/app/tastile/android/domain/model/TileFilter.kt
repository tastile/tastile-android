package app.tastile.android.domain.model

/**
 * Pure-Kotlin filter for the tile list query. Mirrors the data-layer
 * [app.tastile.android.data.repository.TileFilter] shape so the Hilt
 * adapter can pass values through without re-implementing them.
 *
 * Kept distinct from the data-layer type so use cases and ViewModels
 * remain independent of v1 HTTP/query-string concerns.
 */
data class TileFilter(
    /** View mode passed to `GET /v1/tiles` (`list`/`kanban`/etc.). */
    val viewMode: String = "list",
    /** Optional lifecycle filter (`Ready`/`Started`/`Done`/`Archived`). */
    val lifecycle: String? = null,
    /** Maximum number of tiles to return; 0 / negative = no limit. */
    val limit: Int = 20,
    /** Free-text search string (matches title / next action). */
    val search: String? = null,
    /** When true, hide tiles scheduled to start after `now`. */
    val excludeFuture: Boolean = false,
    /** Optional range token (`day`/`week`/`month`) for time-bounded reads. */
    val range: String? = null,
    /** Optional granularity token for timeline-style reads. */
    val granularity: String? = null,
    /** Optional owner-id allow-list; empty = current user only. */
    val ownerIds: List<String> = emptyList(),
) {
    companion object {
        val DEFAULT: TileFilter = TileFilter()
    }
}
