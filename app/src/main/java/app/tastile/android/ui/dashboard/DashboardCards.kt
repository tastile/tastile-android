package app.tastile.android.ui.dashboard

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed interface CardAction {
    data class TriggerPrompt(val tileId: String) : CardAction
    data class StartTile(val tileId: String) : CardAction
    data class CompleteTile(val tileId: String) : CardAction
    data class DeferTile(val tileId: String) : CardAction
    data class DeleteTile(val tileId: String) : CardAction
}

enum class CardStatus {
    READY,
    STARTED,
    DONE,
    ARCHIVED
}

sealed interface DashboardCardModel {
    val id: String
    val title: String
    val status: CardStatus

    data class BaseCard(
        override val id: String,
        override val title: String,
        override val status: CardStatus,
        val subtitle: String? = null,
        val doneDefinition: String? = null
    ) : DashboardCardModel

    data class TimePriorityCard(
        override val id: String,
        override val title: String,
        override val status: CardStatus,
        val durationMinutes: Int? = null,
        val startAtIso: String? = null,
        val endAtIso: String? = null
    ) : DashboardCardModel

    data class TimelineCard(
        override val id: String,
        override val title: String,
        override val status: CardStatus,
        val items: List<TimelineItem>
    ) : DashboardCardModel
}

data class TimelineItem(
    val tileId: String,
    val title: String,
    val status: CardStatus,
    val timestampIso: String
)

class DashboardCardMapper {
    fun buildExecuteCards(tiles: List<Tile>): List<DashboardCardModel> {
        val active = tiles.firstOrNull { it.isStarted() }
        val next = tiles.firstOrNull { !it.isDone() && !it.isStarted() } ?: active
        val timeSource = active ?: next
        val timelineItems = tiles
            .mapNotNull { tile ->
                val ts = tile.absoluteTimestampIso() ?: return@mapNotNull null
                TimelineItem(
                    tileId = tile.id,
                    title = tile.title,
                    status = tile.lifecycle.toCardStatus(),
                    timestampIso = ts
                )
            }
            .sortedByDescending { it.timestampIso }
            .take(8)

        val result = mutableListOf<DashboardCardModel>()
        if (timeSource != null) {
            result += timeSource.toTimePriorityCard()
        }
        if (next != null && next.id != timeSource?.id) {
            result += next.toBaseCard()
        }
        result += DashboardCardModel.TimelineCard(
            id = "timeline",
            title = "Timeline",
            status = CardStatus.READY,
            items = timelineItems
        )
        return result
    }

    fun buildTileCards(tiles: List<Tile>): List<DashboardCardModel> {
        return tiles.map { tile ->
            val hasTime = tile.objectiveConditions.extractTargetWorkMin() != null || tile.updatedAt != null
            if (hasTime) tile.toTimePriorityCard() else tile.toBaseCard()
        }
    }

    private fun Tile.toBaseCard(): DashboardCardModel.BaseCard {
        return DashboardCardModel.BaseCard(
            id = id,
            title = title,
            status = lifecycle.toCardStatus(),
            subtitle = nextAction,
            doneDefinition = doneDefinition
        )
    }

private fun Tile.toTimePriorityCard(): DashboardCardModel.TimePriorityCard {
    val temporal = temporalConditions
    return DashboardCardModel.TimePriorityCard(
        id = id,
        title = title,
        status = lifecycle.toCardStatus(),
        durationMinutes = objectiveConditions.extractTargetWorkMin(),
        startAtIso = temporal.extractString("fixed_start")
            ?: temporal.extractString("active_start")
            ?: updatedAt
            ?: createdAt,
        endAtIso = temporal.extractString("fixed_end") ?: temporal.extractString("active_end")
    )
}
}

private fun String.toCardStatus(): CardStatus = when (TileLifecycle.fromString(this)) {
    TileLifecycle.READY -> CardStatus.READY
    TileLifecycle.STARTED -> CardStatus.STARTED
    TileLifecycle.DONE -> CardStatus.DONE
    TileLifecycle.ARCHIVED -> CardStatus.ARCHIVED
}

private fun JsonObject?.extractTargetWorkMin(): Int? {
    val value: JsonElement = this?.get("target_work_min") ?: return null
    return value.jsonPrimitive.intOrNull
}

private fun JsonObject?.extractString(key: String): String? {
    return this?.get(key)?.jsonPrimitive?.contentOrNull
}

private fun Tile.absoluteTimestampIso(): String? {
    val temporal = temporalConditions
    return temporal.extractString("fixed_start")
        ?: temporal.extractString("active_start")
        ?: updatedAt
        ?: createdAt
}

