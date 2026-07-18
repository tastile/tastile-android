package app.tastile.android.data.api

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceTileApiContractTest {
    @Test
    fun source_tile_paths_match_the_five_canonical_endpoints() {
        assertEquals("/v1/source-tiles", V1Endpoints.SOURCE_TILES)
        assertEquals("/v1/source-tiles/source-1", V1Endpoints.sourceTile("source-1"))
        assertEquals("/v1/source-tiles/source-1/reflow", V1Endpoints.reflowSourceTile("source-1"))
        assertEquals("/v1/source-tiles/source-1/placements", V1Endpoints.sourceTilePlacements("source-1"))
    }

    @Test
    fun source_tile_generation_keeps_utc_instants_as_given() {
        val generation = SourceGenerationPayload(
            kind = 0,
            at = "2026-07-19T09:00:00Z",
            startsAt = null,
            intervalMs = null,
            endsAt = null,
            weekdayMask = 31,
            dateRangeStart = "2026-07-01",
            dateRangeEnd = "2026-08-01",
        )
        assertEquals("2026-07-19T09:00:00Z", generation.at)
        assertEquals(31, generation.weekdayMask?.toInt())
        assertEquals("2026-07-01", generation.dateRangeStart)
        assertEquals("2026-08-01", generation.dateRangeEnd)
    }
}
