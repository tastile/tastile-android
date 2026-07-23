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
import kotlinx.serialization.json.long
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
    fun delete_archive_envelope_matches_web_contract() {
        val body = V1Wire.commandEnvelope(
            payload = buildJsonObject { put("tile_id", "tile-1") },
            idempotencyKey = "018f4a7c-4f7a-7000-8000-000000000001",
            occurredAt = "2026-07-15T00:00:00Z",
        )

        assertTrue(body["expected_revision"] is JsonNull)
        assertEquals("tile-1", body["payload"]!!.jsonObject["tile_id"]!!.jsonPrimitive.content)
        assertEquals("2026-07-15T00:00:00Z", body["occurred_at"]!!.jsonPrimitive.content)
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
                frameRule = FrameRulePayload(
                    id = "rule-1",
                    rank = 0,
                    generator = FrameRuleGeneratorPayload(FrameRuleStepPayload(86_400_000)),
                ),
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
                SourceRefPayload.empty(),
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
    fun standard_tile_create_includes_web_null_fields_and_default_visuals() {
        val tile = Json.encodeToJsonElement(
            CreateTilePayload.serializer(),
            CreateTilePayload(
                kind = V1NumericConstants.TileKind.PLACEMENT,
                title = "Inbox item",
                planRole = V1NumericConstants.PlanRole.EXECUTABLE,
            ),
        ).jsonObject

        assertNull(tile["description"]?.jsonPrimitive?.contentOrNull)
        assertNull(tile["external_id"]?.jsonPrimitive?.contentOrNull)
        assertNull(tile["owner_subject_id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("#3b82f6", tile["color"]?.jsonPrimitive?.contentOrNull)
        assertEquals("check-circle", tile["icon"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun v1_list_tiles_response_decodes_core_bare_array_shape() {
        // Core's `GET /v1/tiles` returns a bare `[TileListView, ...]` array,
        // not the `{tiles: [...]}` envelope the original Android serializer
        // matched. `V1ApiClient.getTiles` wraps the bare-array wire into the
        // envelope shape before passing it to the kotlinx decoder; this test
        // pins the wrapping logic so a future "fix" can't silently drop it.
        val body = """
            [
              {"id":"019f8a33-4825-7c13-9c2c-000000000001","title":"Daily review","plan_id":"plan-1"},
              {"id":"019f8a33-4825-7c13-9c2c-000000000002","title":"Inbox item"}
            ]
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val trimmed = body.trimStart()
        val wrapped = if (trimmed.startsWith("[")) "{\"tiles\":$body}" else body
        val parsed = json.decodeFromString<V1ListTilesResponse>(wrapped)

        assertEquals(2, parsed.tiles.size)
        assertEquals("Daily review", parsed.tiles[0].title)
        assertEquals("plan-1", parsed.tiles[0].planId)
        assertEquals("Inbox item", parsed.tiles[1].title)
        assertNull(parsed.nextActionableTileId)
        assertNull(parsed.nextActionableStartAt)
    }

    @Test
    fun recurring_frame_rule_and_placement_source_ref_match_web_full_wire_shapes() {
        val frameRule = FrameRulePayload(
            id = "00000000-0000-0000-0000-000000000000",
            active = null,
            rank = 0,
            generator = FrameRuleGeneratorPayload(
                step = FrameRuleStepPayload(step = 86_400_000, origin = null, bounds = null),
            ),
        )
        val sourceRef = SourceRefPayload.empty()
        val tile = Json.encodeToJsonElement(
            CreateTilePayload.serializer(),
            CreateTilePayload(0, "Daily", planRole = 0, frameRule = frameRule),
        ).jsonObject
        val placement = Json.encodeToJsonElement(
            CreatePlacementPayload.serializer(),
            CreatePlacementPayload(
                "tile-1", "plan-1", 0, sourceRef,
                PlacementBaselinePayload(PlacementSpanPayload("2026-07-01T00:00:00Z", "2026-07-01T01:00:00Z")),
            ),
        ).jsonObject

        val generator = tile["frame_rule"]!!.jsonObject["generator"]!!.jsonObject
        assertEquals(86_400_000L, generator["Step"]!!.jsonObject["step"]!!.jsonPrimitive.long)
        assertNull(generator["Step"]!!.jsonObject["origin"]?.jsonPrimitive?.contentOrNull)
        assertNull(generator["Step"]!!.jsonObject["bounds"]?.jsonPrimitive?.contentOrNull)
        val wireSourceRef = placement["source_ref"]!!.jsonObject
        assertEquals(setOf("created", "recurring", "flow", "frame", "proposal", "source_text", "external_id"), wireSourceRef.keys)
        wireSourceRef.values.forEach { assertNull(it.jsonPrimitive.contentOrNull) }
    }

    @Test
    fun start_tile_and_execution_lifecycle_match_web_v1_payloads() {
        val start = Json.encodeToJsonElement(
            StartTilePayload.serializer(),
            StartTilePayload(
                "tile-1", "plan-1", 0, SourceRefPayload.empty(),
                StartTileBaseline(PlacementSpanPayload("2026-07-01T00:00:00Z", "2026-07-01T01:00:00Z")),
            ),
        ).jsonObject
        val pauseEnvelope = V1Wire.commandEnvelope(payload = JsonNull)

        assertEquals(setOf("span", "inside"), start["baseline"]!!.jsonObject.keys)
        assertEquals("2026-07-01T00:00:00Z", start["baseline"]!!.jsonObject["span"]!!.jsonObject["start"]!!.jsonPrimitive.content)
        assertEquals(setOf("created", "recurring", "flow", "frame", "proposal", "source_text", "external_id"), start["source_ref"]!!.jsonObject.keys)
        assertTrue(pauseEnvelope["payload"] is JsonNull)
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

    @Test
    fun timeline_item_decodes_the_core_span_inside_source_shapes() {
        val payload = """
            [{
              "placement_id":"placement-1", "tile_id":"tile-1", "revision":1,
              "content":{"title":"Planning"},
              "visual":{"color":"#3b82f6"},
              "role":0,
              "span":{"start":"2026-07-01T09:00:00Z","end":"2026-07-01T10:00:00Z"},
              "inside":null,
              "source":{"kind":1,"detail":"recurring:f3aa"},
              "resolution":{"state":0,"resolved_at":"2026-07-01T09:00:00Z","resolution_hash":"00000000-0000-0000-0000-000000000000","violations":[]},
              "source_tile_id":null,"occurrence_id":null,
              "split_index":null,"split_count":null,"split_group_id":null
            }]
        """.trimIndent()

        val items = Json { ignoreUnknownKeys = true }.decodeFromString<List<TimelineItem>>(payload)

        val only = items.single()
        assertEquals("tile-1", only.tileId)
        assertEquals("placement-1", only.placementId)
        assertEquals("2026-07-01T09:00:00Z", only.span.start)
        assertEquals("2026-07-01T10:00:00Z", only.span.end)
        assertNull(only.inside)
        assertEquals(1.toShort(), only.source.kind)
        assertEquals("recurring:f3aa", only.source.detail)
    }

    @Test
    fun timeline_item_decodes_inside_with_parent_and_scope_when_present() {
        val payload = """
            [{
              "placement_id":"placement-child", "tile_id":"tile-1", "revision":1,
              "content":{"title":"Nested"},
              "visual":{"color":"#000000"},
              "role":0,
              "span":{"start":"2026-07-01T09:00:00Z","end":"2026-07-01T10:00:00Z"},
              "inside":{"parent":"placement-parent","scope":2},
              "source":{"kind":4,"detail":"source:f3aa"},
              "resolution":{"state":0,"resolved_at":"2026-07-01T09:00:00Z","resolution_hash":"00000000-0000-0000-0000-000000000000","violations":[]}
            }]
        """.trimIndent()

        val items = Json { ignoreUnknownKeys = true }.decodeFromString<List<TimelineItem>>(payload)

        val only = items.single()
        val inside = only.inside
        assertEquals("placement-parent", inside?.parent)
        assertEquals(2.toShort(), inside?.scope)
    }

    @Test
    fun workspace_patch_wire_matches_web_edit_fields_including_parent() {
        val body = V1WorkspaceWire.updateBody(
            UpdateWorkspaceInput(
                displayName = "Mobile launch",
                slug = "mobile-launch",
                color = "#aabbcc",
                parentSubjectId = "parent-1",
            ),
        )
        assertEquals("Mobile launch", body["display_name"]?.jsonPrimitive?.content)
        assertEquals("mobile-launch", body["slug"]?.jsonPrimitive?.content)
        assertEquals("#aabbcc", body["color"]?.jsonPrimitive?.content)
        assertEquals("parent-1", body["parent_subject_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun timeline_path_includes_owner_ids_when_project_selected() {
        assertEquals(
            "/v1/timeline?start=2026-07-01T00%3A00%3A00Z&end=2026-07-02T00%3A00%3A00Z&owner_ids=project-1",
            V1Endpoints.timeline(
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-02T00:00:00Z"),
                listOf("project-1"),
            ),
        )
    }
}
