package app.tastile.android.ui.mobile.sheets.quickcreate

import app.tastile.android.data.api.AggregateMeta
import app.tastile.android.data.api.AggregateRef
import app.tastile.android.data.api.CommandResponse
import app.tastile.android.data.api.SourceTileWritePayload
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateIntervalUnit
import app.tastile.android.ui.mobile.sheets.QuickCreatePlacementEffect
import app.tastile.android.ui.mobile.sheets.QuickCreatePlacementRule
import app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode
import app.tastile.android.ui.mobile.sheets.QuickCreateSpan
import app.tastile.android.ui.mobile.sheets.QuickCreateTileKind
import app.tastile.android.ui.mobile.sheets.QuickCreateTimeOfDayMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickCreateSubmissionTest {
    @Test fun `validation requires title and placement span`() {
        assertFalse(quickCreateSubmissionValidation(QuickCreateDraftState()).isValid)
        assertFalse(quickCreateSubmissionValidation(QuickCreateDraftState(identity = QuickCreateDraftState().identity.copy(title = "A"))).isValid)
    }

    @Test fun `all day normalizes missing end to next midnight`() {
        val result = quickCreateSubmissionValidation(
            QuickCreateDraftState(identity = QuickCreateDraftState().identity.copy(title = "A"), time = QuickCreateDraftState().time.copy(timeOfDayMode = QuickCreateTimeOfDayMode.AllDay, span = QuickCreateSpan("2026-07-16", ""))),
        )
        assertTrue(result.isValid)
        assertEquals("2026-07-17T00:00:00Z", result.normalizedEnd)
    }

    @Test fun `placement submission posts to v1 source-tiles first`() = runTest {
        val gateway = FakeGateway()
        val draft = QuickCreateDraftState(
            identity = QuickCreateDraftState().identity.copy(title = "Focus"),
            time = QuickCreateDraftState().time.copy(span = QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T10:00:00Z")),
        )
        val result = QuickCreateSubmissionDispatcher(gateway).submit(draft)
        assertTrue(result is QuickCreateSubmitResult.Success)
        // v1 source-tile path runs first; v0 fallback only fires on NOT_FOUND.
        assertEquals(listOf("sourceTile"), gateway.calls)
    }

    @Test fun `recurring submission posts to v1 source-tiles first`() = runTest {
        val gateway = FakeGateway()
        val draft = QuickCreateDraftState(
            identity = QuickCreateDraftState().identity.copy(kind = QuickCreateTileKind.Recurring, title = "Daily"),
            time = QuickCreateDraftState().time.copy(span = QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T10:00:00Z")),
        )
        val result = QuickCreateSubmissionDispatcher(gateway).submit(draft)
        assertTrue(result is QuickCreateSubmitResult.Success)
        assertEquals(listOf("sourceTile"), gateway.calls)
    }

    @Test fun `recurring identity and placement rules reach canonical source payload`() = runTest {
        val gateway = FakeGateway()
        val base = QuickCreateDraftState()
        val draft = base.copy(
            identity = base.identity.copy(
                kind = QuickCreateTileKind.Recurring,
                title = "Weekly review",
                description = "Review progress",
                visual = base.identity.visual.copy(color = "#8b5cf6", icon = "repeat"),
            ),
            time = base.time.copy(span = QuickCreateSpan("2026-07-16T09:00:00Z", "2026-08-16T10:00:00Z")),
            recurring = base.recurring.copy(
                repeatMode = QuickCreateRepeatMode.Weekly,
                weekdayMask = 0b0101010,
                endDate = "2026-08-15",
                intervalValue = 2,
                intervalUnit = QuickCreateIntervalUnit.Day,
            ),
            plan = base.plan.copy(
                planning = base.plan.planning.copy(
                    placementRules = listOf(
                        QuickCreatePlacementRule(
                            id = "rule-1",
                            rank = 4,
                            effect = QuickCreatePlacementEffect(kind = 3, score = 25),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(QuickCreateSubmissionDispatcher(gateway).submit(draft) is QuickCreateSubmitResult.Success)
        val payload = requireNotNull(gateway.lastSourceTilePayload)
        assertEquals("Review progress", payload.tile.description)
        assertEquals("#8b5cf6", payload.tile.color)
        assertEquals("repeat", payload.tile.icon)
        assertEquals(1, payload.schedule.generation.kind.toInt())
        assertEquals(0b0101010, payload.schedule.generation.weekdayMask?.toInt())
        assertEquals("2026-08-15T23:59:59Z", payload.schedule.generation.endsAt)
        assertEquals(1, payload.plan.planning.placement_rules.size)
        assertEquals(3, payload.plan.planning.placement_rules.single().effect.kind)
        assertEquals(25, payload.plan.planning.placement_rules.single().effect.score)
    }
    @Test fun `v0 fallback fires when v1 endpoint returns NOT_FOUND`() = runTest {
        val gateway = FakeGateway(sourceTileNotFound = true)
        val draft = QuickCreateDraftState(
            identity = QuickCreateDraftState().identity.copy(title = "Focus"),
            time = QuickCreateDraftState().time.copy(span = QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T10:00:00Z")),
        )
        val result = QuickCreateSubmissionDispatcher(gateway).submit(draft)
        assertTrue(result is QuickCreateSubmitResult.Success)
        assertEquals(listOf("sourceTile", "tile:1", "placement", "plan"), gateway.calls)
    }

    @Test fun `failure never reports successful submission`() = runTest {
        // v1 source-tiles path embeds the plan in the create payload and does
        // NOT call setPlan; the only way to exercise setPlan is to force the
        // v0 fallback by having v1 return NOT_FOUND.
        val gateway = FakeGateway(sourceTileNotFound = true, failPlan = true)
        val draft = QuickCreateDraftState(identity = QuickCreateDraftState().identity.copy(title = "Focus"), time = QuickCreateDraftState().time.copy(span = QuickCreateSpan("2026-07-16T09:00:00Z", "2026-07-16T10:00:00Z")))
        assertTrue(QuickCreateSubmissionDispatcher(gateway).submit(draft) is QuickCreateSubmitResult.Failure)
    }

    private class FakeGateway(
        private val failPlan: Boolean = false,
        private val sourceTileNotFound: Boolean = false,
    ) : QuickCreateCommandGateway {
        val calls = mutableListOf<String>()
        val lastSourceTilePayload: SourceTileWritePayload? get() = sourcePayload
        private var sourcePayload: SourceTileWritePayload? = null
        private fun response(id: String, meta: AggregateMeta? = null) = CommandResponse(
            commandId = "cmd", acceptedAt = "2026-07-16T00:00:00Z", aggregate = AggregateRef(1, id),
            aggregateMeta = meta, result = 1,
        )
        override suspend fun createSourceTile(payload: SourceTileWritePayload): CommandResponse {
            calls += "sourceTile"
            sourcePayload = payload
            if (sourceTileNotFound) {
                throw app.tastile.android.data.api.V1Error.Api(
                    kindValue = app.tastile.android.data.api.V1NumericConstants.ApiErrorKind.NOT_FOUND,
                    kindName = "NOT_FOUND",
                    message = "no source-tile handler",
                )
            }
            return response("source-tile")
        }
        override suspend fun createTile(payload: app.tastile.android.data.api.CreateTilePayload): CommandResponse { calls += "tile:${payload.kind}"; return response("tile", AggregateMeta(planId = "plan", frameRuleId = "frame")) }
        override suspend fun createPlacement(payload: app.tastile.android.data.api.CreatePlacementPayload): CommandResponse { calls += "placement"; return response("placement") }
        override suspend fun materializeRecurring(payload: app.tastile.android.data.api.MaterializeRecurringPayload): CommandResponse { calls += "materialize"; return response("placement") }
        override suspend fun setPlan(tileId: String, payload: app.tastile.android.data.api.SetPlanPayload): CommandResponse { calls += "plan"; if (failPlan) throw IllegalStateException("plan unavailable"); return response(tileId) }
        override suspend fun updateSourceTile(sourceTileId: String, payload: app.tastile.android.data.api.SourceTileWritePayload, expectedRevision: Long): app.tastile.android.data.api.CommandResponse { calls += "update-source-tile"; return response(sourceTileId) }
    }
}
