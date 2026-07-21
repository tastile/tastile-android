package app.tastile.android.data.api

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

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
        val json = Json.encodeToString(SourceTileWritePayload.serializer(), sourceWritePayload())
        assertEquals(
            "{\"tile\":{\"title\":\"授業\",\"description\":null,\"color\":null,\"icon\":null,\"external_id\":null},\"plan\":{\"role\":0,\"references\":[],\"completion\":{\"root\":{\"Any\":[]},\"time_requirements\":[],\"tasks\":[]},\"planning\":{\"placement_rules\":[],\"nesting_rules\":[]},\"metrics\":[],\"decisions\":[]},\"flows\":[],\"schedule\":{\"required_duration_ms\":5400000,\"generation\":{\"kind\":1,\"starts_at\":\"2026-06-10T00:00:00Z\",\"interval_ms\":604800000,\"weekday_mask\":1,\"excluded_dates\":[\"2026-07-16\"]},\"window\":{\"start_offset_ms\":0,\"end_offset_ms\":7200000},\"split_policy\":{\"kind\":0},\"priority\":0},\"horizon\":{\"start\":\"2026-06-10T00:00:00Z\",\"end\":\"2026-08-10T00:00:00Z\"}}",
            json,
        )
    }

    @Test
    fun source_tile_client_sends_canonical_write_envelopes_and_retains_read_reflow_paths() = runBlocking {
        val requests = CopyOnWriteArrayList<RecordedRequest>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            requests += RecordedRequest(exchange.requestMethod, exchange.requestURI.path, exchange.requestHeaders, body)
            val response = when (exchange.requestURI.path) {
                "/v1/source-tiles/source-1" -> if (exchange.requestMethod == "GET") sourceDetailJson else commandResponseJson
                else -> commandResponseJson
            }
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
        server.start()
        try {
            val client = V1ApiClient({ "bearer-token" }, "http://127.0.0.1:${server.address.port}")
            client.createSourceTile(sourceWritePayload())
            client.updateSourceTile("source-1", sourceWritePayload(), expectedRevision = 7)
            client.readSourceTile("source-1")
            client.reflowSourceTile(
                "source-1",
                ReflowSourceTilePayload(PlacementSpanPayload("2026-06-10T00:00:00Z", "2026-06-11T00:00:00Z")),
                expectedRevision = 8,
            )
        } finally {
            server.stop(0)
        }

        assertEquals(listOf("POST", "PUT", "GET", "POST"), requests.map { it.method })
        assertEquals(
            listOf("/v1/source-tiles", "/v1/source-tiles/source-1", "/v1/source-tiles/source-1", "/v1/source-tiles/source-1/reflow"),
            requests.map { it.path },
        )
        requests.filter { it.method != "GET" }.forEach { request ->
            assertEquals("Bearer bearer-token", request.headers.getFirst("Authorization"))
            val envelope = Json.parseToJsonElement(request.body).jsonObject
            assertNotNull(UUID.fromString(envelope.getValue("idempotency_key").toString().trim('"')))
            assertNotNull(envelope["occurred_at"])
        }
        assertEquals(7, Json.parseToJsonElement(requests[1].body).jsonObject["expected_revision"].toString().toInt())
        assertEquals(8, Json.parseToJsonElement(requests[3].body).jsonObject["expected_revision"].toString().toInt())
    }

    private fun sourceWritePayload() = SourceTileWritePayload(
            tile = SourceTileDefinitionPayload(title = "授業"),
            plan = SchedulePlanDefinitionPayloadTyped(
                role = 0,
                references = emptyList(),
                completion = CompletionSchema(
                    root = Json.parseToJsonElement("{\"Any\":[]}"),
                    time_requirements = emptyList(),
                    tasks = emptyList(),
                ),
                planning = SchedulingPlanningDefinitionSchema(
                    placement_rules = emptyList(),
                    nesting_rules = emptyList(),
                ),
                metrics = emptyList(),
                decisions = emptyList(),
            ),
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

    private data class RecordedRequest(
        val method: String,
        val path: String,
        val headers: com.sun.net.httpserver.Headers,
        val body: String,
    )

    private companion object {
        const val commandResponseJson = "{\"command_id\":\"019ef8d5-354a-7bd2-b22a-b4bd372ea0d1\",\"accepted_at\":\"2026-07-19T00:00:00Z\",\"result\":0,\"pending\":[]}"
        const val sourceDetailJson = "{\"source\":{\"source_tile_id\":\"source-1\",\"plan_id\":\"plan-1\",\"owner_id\":\"owner-1\",\"revision\":1,\"title\":\"授業\",\"plan_role\":0,\"schedule\":{\"required_duration_ms\":1,\"generation\":{\"kind\":2,\"excluded_dates\":[]},\"window\":{\"start_offset_ms\":0,\"end_offset_ms\":1},\"split_policy\":{\"kind\":0},\"priority\":0},\"created_at\":\"2026-07-19T00:00:00Z\",\"updated_at\":\"2026-07-19T00:00:00Z\"},\"occurrences\":[],\"placements\":[]}"
    }
}
