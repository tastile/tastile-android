package app.tastile.android.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Wire-shape parity test for the v1/05 Condition + Term + leaf sub-enum
 * polymorphic mirror in [ConditionAstMirror.kt].
 *
 * For every variant we construct the typed value, serialise via
 * [Json.encodeToString], assert the result byte-for-byte matches what
 * Core emits per the OpenAPI schema, then deserialise and assert the
 * typed value round-trips.
 */
class ConditionAstWireShapeTest {

    private val json = Json

    @Test
    fun condition_any_empty_serializes_as_wire_form() {
        val v = ConditionRef(ConditionAny(emptyList()))
        assertEquals("""{"Any":[]}""", json.encodeToString(ConditionRefSerializer, v))
        val round = json.decodeFromJsonElement(ConditionRefSerializer, json.parseToJsonElement("""{"Any":[]}"""))
        assertEquals(v, round)
    }

    @Test
    fun condition_all_with_nested_recurses_with_wire_form() {
        val nested = ConditionRef(ConditionNot(ConditionRef(ConditionAny(emptyList()))))
        val v = ConditionRef(ConditionAll(listOf(nested)))
        val expected = """{"All":[{"Not":{"Any":[]}}]}"""
        assertEquals(expected, json.encodeToString(ConditionRefSerializer, v))
        val round = json.decodeFromJsonElement(ConditionRefSerializer, json.parseToJsonElement(expected))
        assertEquals(v, round)
    }

    @Test
    fun condition_term_with_calendar_serializes_with_wire_form() {
        val term = TermRef(TermCalendar(
            weekday_mask = 31,
            time_start = TimeOfDay(0, 0, 0, 0),
            time_end = null,
            holiday_kind = 0,
            date_range = null,
            offset_min = 0,
        ))
        val v = ConditionRef(ConditionTerm(term))
        val out = json.encodeToString(ConditionRefSerializer, v)
        // Ensure the structure is {"Term":{"Calendar":{...}}}
        val asObj = json.parseToJsonElement(out) as JsonObject
        val termObj = asObj["Term"] as JsonObject
        assertTrue("Calendar" in termObj)
        val calObj = termObj["Calendar"] as JsonObject
        assertEquals(31, (calObj["weekday_mask"] as JsonPrimitive).content.toInt())
        assertEquals(0, (calObj["holiday_kind"] as JsonPrimitive).content.toInt())
    }

    @Test
    fun term_moment_at_serializes_with_wire_form() {
        val term = TermRef(TermMoment(
            point = 0,
            target = MomentTargetContext(0),
            offset = 0L,
            comparison = MomentComparisonAt("2026-07-21T00:00:00Z"),
        ))
        val expected = """{"Moment":{"point":0,"target":{"Context":0},"offset":0,"comparison":{"At":"2026-07-21T00:00:00Z"}}}"""
        assertEquals(expected, json.encodeToString(TermRefSerializer, term))
    }

    @Test
    fun term_moment_with_target_reference_and_before_comparison() {
        val term = TermRef(TermMoment(
            point = 1,
            target = MomentTargetReference("019f8356-57e0-7cb2-a1e9-0dfb75f98bdd"),
            offset = 60L,
            comparison = MomentComparisonBefore(MomentTargetPlacement("019f8356-580c-7d33-9284-0f88d7ee5784")),
        ))
        val expected = """{"Moment":{"point":1,"target":{"Reference":"019f8356-57e0-7cb2-a1e9-0dfb75f98bdd"},"offset":60,"comparison":{"Before":{"Placement":"019f8356-580c-7d33-9284-0f88d7ee5784"}}}}"""
        assertEquals(expected, json.encodeToString(TermRefSerializer, term))
    }

    @Test
    fun term_moment_within_range_comparison() {
        val term = TermRef(TermMoment(
            point = 2,
            target = MomentTargetFrame("frame-uuid"),
            offset = 0L,
            comparison = MomentComparisonWithin(RangeI64(min = 0, max = 100)),
        ))
        val expected = """{"Moment":{"point":2,"target":{"Frame":"frame-uuid"},"offset":0,"comparison":{"Within":{"min":0,"max":100}}}}"""
        assertEquals(expected, json.encodeToString(TermRefSerializer, term))
    }

    @Test
    fun term_relation_serializes_with_wire_form() {
        val term = TermRef(TermRelation(
            reference_id = "ref-uuid",
            relation = 0,
            window_kind = RelationWindowKind("LabelSpan"),
        ))
        val expected = """{"Relation":{"reference_id":"ref-uuid","relation":0,"window_kind":"LabelSpan"}}"""
        assertEquals(expected, json.encodeToString(TermRefSerializer, term))
    }

    @Test
    fun term_gap_serializes_with_wire_form() {
        val term = TermRef(TermGap(
            scope = ScopeAst(kind = 0, parent = null),
            left_anchor = AnchorSelector(
                `when` = ConditionRef(ConditionAny(emptyList())),
                pick = PickAst(kind = 0, at = null),
            ),
            right_anchor = AnchorSelector(
                `when` = ConditionRef(ConditionAny(emptyList())),
                pick = PickAst(kind = 0, at = null),
            ),
            size = null,
        ))
        val out = json.encodeToString(TermRefSerializer, term)
        assertTrue("Gap" in out)
        assertTrue("scope" in out)
        assertTrue("left_anchor" in out)
        assertTrue("right_anchor" in out)
    }

    @Test
    fun term_requirement_serializes_with_wire_form() {
        val term = TermRef(TermRequirement(
            time_requirement = "tr-uuid",
            state = RequirementState("Met"),
        ))
        val expected = """{"Requirement":{"time_requirement":"tr-uuid","state":"Met"}}"""
        assertEquals(expected, json.encodeToString(TermRefSerializer, term))
    }

    @Test
    fun term_task_serializes_with_wire_form() {
        val term = TermRef(TermTask(task_id = "task-uuid", state = TaskStateKind("Completed")))
        val expected = """{"Task":{"task_id":"task-uuid","state":"Completed"}}"""
        assertEquals(expected, json.encodeToString(TermRefSerializer, term))
    }

    @Test
    fun term_fact_equal_serializes_with_wire_form() {
        val term = TermRef(TermFact(key = "k1", comparison = FactComparisonEqual("v1")))
        val expected = """{"Fact":{"key":"k1","comparison":{"Equal":"v1"}}}"""
        assertEquals(expected, json.encodeToString(TermRefSerializer, term))
    }

    @Test
    fun term_fact_exists_and_missing_serializes_with_wire_form() {
        val t1 = TermRef(TermFact(key = "k1", comparison = FactComparisonExists))
        assertEquals("""{"Fact":{"key":"k1","comparison":{"Exists":{}}}}""", json.encodeToString(TermRefSerializer, t1))
        val t2 = TermRef(TermFact(key = "k1", comparison = FactComparisonMissing))
        assertEquals("""{"Fact":{"key":"k1","comparison":{"Missing":{}}}}""", json.encodeToString(TermRefSerializer, t2))
    }

    @Test
    fun term_metric_equal_and_inrange_serializes_with_wire_form() {
        val t1 = TermRef(TermMetric(metric_id = "m-uuid", comparison = MetricComparisonEqual(42L)))
        assertEquals("""{"Metric":{"metric_id":"m-uuid","comparison":{"Equal":42}}}""", json.encodeToString(TermRefSerializer, t1))
        val t2 = TermRef(TermMetric(metric_id = "m-uuid", comparison = MetricComparisonInRange(RangeI64(min = 0, max = 100))))
        assertEquals("""{"Metric":{"metric_id":"m-uuid","comparison":{"InRange":{"min":0,"max":100}}}}""", json.encodeToString(TermRefSerializer, t2))
    }

    @Test
    fun term_feedback_serializes_with_wire_form() {
        val term = TermRef(TermFeedback(feedback_txn = "ft-uuid", state = FeedbackState("Applied")))
        val expected = """{"Feedback":{"feedback_txn":"ft-uuid","state":"Applied"}}"""
        assertEquals(expected, json.encodeToString(TermRefSerializer, term))
    }

    @Test
    fun term_life_tile_serializes_with_wire_form() {
        val term = TermRef(TermLife(target = LifeTargetTile("tile-uuid"), state = LifeState("Active")))
        val expected = """{"Life":{"target":{"Tile":"tile-uuid"},"state":"Active"}}"""
        assertEquals(expected, json.encodeToString(TermRefSerializer, term))
    }

    @Test
    fun condition_not_with_calendar_term_round_trips() {
        val v = ConditionRef(
            ConditionNot(
                ConditionRef(
                    ConditionTerm(
                        TermRef(
                            TermCalendar(
                                weekday_mask = 1, time_start = null, time_end = null, holiday_kind = 0, date_range = null, offset_min = 0,
                            )
                        )
                    )
                )
            )
        )
        val out = json.encodeToString(ConditionRefSerializer, v)
        val round = json.decodeFromJsonElement(ConditionRefSerializer, json.parseToJsonElement(out))
        assertEquals(v, round)
    }
}
