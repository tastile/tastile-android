package app.tastile.android.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class V1SessionApiTest {
    @Test
    fun workflow_session_wire_shape_uses_generic_ids_and_numeric_state() {
        val pending = Json.decodeFromString<List<PendingSessionView>>(
            """[{"id":"session-1","owner_id":"owner-1","state":0,"revision":7,"created_at":"2026-07-28T00:00:00Z","updated_at":"2026-07-28T00:00:00Z"}]""",
        )
        val detail = Json.decodeFromString<SessionDetailView>(
            """{"id":"session-1","owner_id":"owner-1","state":0,"revision":7,"created_at":"2026-07-28T00:00:00Z","updated_at":"2026-07-28T00:00:00Z","root_node_id":"node-1","runs":[{"id":"run-1","decision_id":"decision-1","subject_kind":4,"subject_id":"subject-1","tree_root_id":"node-1","created_at":"2026-07-28T00:00:00Z"}]}""",
        )
        val change = FeedbackChangeDto(
            target = buildJsonObject { put("InteractionInput", "input-1") },
            key = JsonPrimitive("answer"),
            kind = 0,
            value = JsonPrimitive("candidate-2"),
        )

        assertEquals("session-1", pending.single().id)
        assertEquals("subject-1", detail.runs.single().subjectId)
        assertEquals("input-1", change.target.jsonObject["InteractionInput"].toString().trim('"'))
        assertEquals("/v1/sessions", V1Endpoints.PENDING_SESSIONS)
    }
}
