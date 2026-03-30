package app.tastile.android.ui.dashboard

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardCardMapperTest {

    @Test
    fun buildExecuteCards_includesBaseTimeAndTimelineVariants() {
        val active = Tile(
            id = "t-active",
            title = "Active Task",
            lifecycle = TileLifecycle.STARTED.value,
            updatedAt = "2026-03-29T10:00:00Z",
            temporalConditions = buildJsonObject {
                put("fixed_start", JsonPrimitive("2026-03-29T10:00:00Z"))
                put("fixed_end", JsonPrimitive("2026-03-29T10:50:00Z"))
            },
            objectiveConditions = buildJsonObject {
                put("target_work_min", JsonPrimitive(50))
            }
        )
        val next = Tile(
            id = "t-next",
            title = "Next Task",
            lifecycle = TileLifecycle.READY.value
        )
        val cards = DashboardCardMapper().buildExecuteCards(listOf(active, next))

        assertEquals(3, cards.size)
        assertTrue(cards.any { it is DashboardCardModel.TimePriorityCard })
        assertTrue(cards.any { it is DashboardCardModel.BaseCard })
        assertTrue(cards.any { it is DashboardCardModel.TimelineCard })
        val timeCard = cards.filterIsInstance<DashboardCardModel.TimePriorityCard>().first()
        assertEquals(50, timeCard.durationMinutes)
        assertEquals("2026-03-29T10:00:00Z", timeCard.startAtIso)
        assertEquals("2026-03-29T10:50:00Z", timeCard.endAtIso)
        val timeline = cards.filterIsInstance<DashboardCardModel.TimelineCard>().first()
        assertEquals(1, timeline.items.size)
        assertEquals("t-active", timeline.items.first().tileId)
        assertEquals(CardStatus.STARTED, timeline.items.first().status)
        assertNotNull(timeline.items.first().timestampIso)
    }

    @Test
    fun buildTileCards_mapsTilesToRenderableCards() {
        val tiles = listOf(
            Tile(id = "t1", title = "A", lifecycle = TileLifecycle.READY.value),
            Tile(id = "t2", title = "B", lifecycle = TileLifecycle.STARTED.value)
        )

        val cards = DashboardCardMapper().buildTileCards(tiles)

        assertEquals(2, cards.size)
        assertTrue(cards.all { it is DashboardCardModel.BaseCard || it is DashboardCardModel.TimePriorityCard })
    }
}

