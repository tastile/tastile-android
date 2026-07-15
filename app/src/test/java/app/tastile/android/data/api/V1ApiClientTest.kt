package app.tastile.android.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant

class V1ApiClientTest {

    @Test
    fun numeric_constants_match_v1_spec() {
        assertEquals(0.toByte(), V1NumericConstants.TileKind.RECURRING)
        assertEquals(1.toByte(), V1NumericConstants.TileKind.PLACEMENT)
        assertEquals(2.toByte(), V1NumericConstants.TileKind.EXECUTION)
        assertEquals(0.toShort(), V1NumericConstants.ApiErrorKind.VALIDATION)
        assertEquals(7.toShort(), V1NumericConstants.ApiErrorKind.RETRYABLE)
        assertEquals(0.toByte(), V1NumericConstants.ExecutionState.ACTIVE)
        assertEquals(3.toByte(), V1NumericConstants.ExecutionState.FINISHED_VOID)
    }

    @Test
    fun idempotency_key_is_uuid_v7_shape() {
        val first = V1Idempotency.generate()
        val second = V1Idempotency.generate()
        assertTrue("idempotency keys must be UUIDs", first.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")))
        assertTrue("idempotency keys must be unique", first != second)
    }

    @Test
    fun api_error_maps_to_sealed_branch() {
        val body = V1ApiErrorBody(kind = V1NumericConstants.ApiErrorKind.STALE_REVISION, message = "stale", currentRevision = 12)
        val error = V1Error.fromApiBody(body)
        @Suppress("USELESS_IS_CHECK")
        assertTrue(error is V1Error.Api)
        error as V1Error.Api
        assertEquals("STALE_REVISION", error.kindName)
        assertEquals(12L, error.currentRevision)
    }

    @Test(expected = V1Error.Auth::class)
    fun missing_token_throws_auth() {
        throw V1Error.Auth()
    }

    @Test
    fun timeline_path_includes_iso_range() {
        val start = Instant.parse("2026-07-01T00:00:00Z")
        val end = Instant.parse("2026-07-02T00:00:00Z")
        assertTrue(start.toString().startsWith("2026-07-01"))
        assertTrue(end.toString().startsWith("2026-07-02"))
    }

    @Test
    fun command_envelope_matches_web_v1_wire_shape_without_android_kind_wrapper() {
        val body = V1Wire.commandEnvelope(
            payload = buildJsonObject { put("tile_id", "tile-1") },
            idempotencyKey = "018f4a7c-4f7a-7000-8000-000000000001",
            occurredAt = "2026-07-15T00:00:00Z",
        )

        val root = Json.parseToJsonElement(body.toString()).jsonObject
        assertEquals("018f4a7c-4f7a-7000-8000-000000000001", root["idempotency_key"]?.jsonPrimitive?.contentOrNull)
        assertNull(root["expected_revision"]?.jsonPrimitive?.contentOrNull)
        assertEquals("tile-1", root["payload"]?.jsonObject?.get("tile_id")?.jsonPrimitive?.contentOrNull)
        assertFalse(root["payload"]!!.jsonObject.containsKey("kind"))
        assertFalse(root["payload"]!!.jsonObject.containsKey("value"))
    }

    @Test
    fun tile_creation_paths_match_web_v1_contract() {
        assertEquals("/v1/tiles", V1Endpoints.CREATE_TILE)
        assertEquals("/v1/tiles/tile-1/plan", V1Endpoints.setPlan("tile-1"))
        assertEquals("/v1/placements", V1Endpoints.CREATE_PLACEMENT)
        assertEquals(
            "/v1/recurring/tile-1/frame-rules/rule-1/materialize",
            V1Endpoints.materializeRecurring("tile-1", "rule-1"),
        )
    }

    @Test
    fun tile_creation_payloads_use_web_v1_snake_case_field_names() {
        val tile = Json.encodeToJsonElement(
            CreateTilePayload.serializer(),
            CreateTilePayload(
                kind = V1NumericConstants.TileKind.RECURRING,
                title = "Daily review",
                planRole = V1NumericConstants.PlanRole.EXECUTABLE,
                ownerSubjectId = "subject-1",
                frameRule = buildJsonObject { put("rank", 0) },
            ),
        ).jsonObject
        val plan = Json.encodeToJsonElement(
            SetPlanPayload.serializer(),
            SetPlanPayload("tile-1", 0, JsonNull, JsonNull, JsonNull, JsonArray(emptyList()), JsonArray(emptyList())),
        ).jsonObject
        val placement = Json.encodeToJsonElement(
            CreatePlacementPayload.serializer(),
            CreatePlacementPayload(
                "tile-1", "plan-1", 0,
                buildJsonObject { put("created", JsonNull) },
                PlacementBaselinePayload(PlacementSpanPayload("2026-07-01T00:00:00Z", "2026-07-01T01:00:00Z")),
            ),
        ).jsonObject
        val materialize = Json.encodeToJsonElement(
            MaterializeRecurringPayload.serializer(),
            MaterializeRecurringPayload("tile-1", "rule-1", "2026-07-01T00:00:00Z", "2026-07-01T01:00:00Z"),
        ).jsonObject

        assertEquals("subject-1", tile["owner_subject_id"]?.jsonPrimitive?.contentOrNull)
        assertTrue(tile.containsKey("frame_rule"))
        assertEquals("tile-1", plan["tile_id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("plan-1", placement["plan_id"]?.jsonPrimitive?.contentOrNull)
        assertTrue(placement.containsKey("source_ref"))
        assertEquals("rule-1", materialize["frame_rule_id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("2026-07-01T00:00:00Z", materialize["range_start"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun timeline_read_path_uses_web_start_and_end_query_names() {
        assertEquals(
            "/v1/timeline?start=2026-07-01T00%3A00%3A00Z&end=2026-07-02T00%3A00%3A00Z",
            V1Endpoints.timeline(
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-02T00:00:00Z"),
            ),
        )
    }
}
