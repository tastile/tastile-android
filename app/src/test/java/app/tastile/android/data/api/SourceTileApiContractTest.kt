package app.tastile.android.data.api

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    @Test
    fun source_tile_create_and_update_share_the_canonical_payload_shape() {
        val payload = SourceTileWritePayload(
            tile = SourceTileDefinitionPayload(title = "授業"),
            plan = SourcePlanDefinitionPayload(),
            flows = emptyList(),
            schedule = SourceSchedulePayload(
                requiredDurationMs = 90 * 60 * 1000,
                generation = SourceGenerationPayload(
                    kind = 1,
                    startsAt = "2026-06-10T00:00:00Z",
                    intervalMs = 7 * 24 * 60 * 60 * 1000L,
                    weekdayMask = 1,
                    excludedDates = listOf("2026-07-16"),
                ),
                window = SourceWindowPayload(0, 2 * 60 * 60 * 1000),
                splitPolicy = SourceSplitPolicyPayload(kind = 0),
                priority = 0,
            ),
            horizon = PlacementSpanPayload(
                start = "2026-06-10T00:00:00Z",
                end = "2026-08-10T00:00:00Z",
            ),
        )

        val json = Json.encodeToString(SourceTileWritePayload.serializer(), payload)
        assertEquals(
            "{\"tile\":{\"title\":\"授業\",\"description\":null,\"color\":null,\"icon\":null,\"external_id\":null},\"plan\":{\"role\":0,\"references\":[],\"completion\":{\"root\":{\"All\":[]},\"time_requirements\":[],\"tasks\":[]},\"planning\":{\"placement_rules\":[],\"nesting_rules\":[]},\"metrics\":[],\"decisions\":[]},\"flows\":[],\"schedule\":{\"required_duration_ms\":5400000,\"generation\":{\"kind\":1,\"starts_at\":\"2026-06-10T00:00:00Z\",\"interval_ms\":604800000,\"weekday_mask\":1,\"excluded_dates\":[\"2026-07-16\"]},\"window\":{\"start_offset_ms\":0,\"end_offset_ms\":7200000},\"split_policy\":{\"kind\":0},\"priority\":0},\"horizon\":{\"start\":\"2026-06-10T00:00:00Z\",\"end\":\"2026-08-10T00:00:00Z\"}}",
            json,
        )
    }
}
