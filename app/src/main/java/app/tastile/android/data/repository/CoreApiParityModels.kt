package app.tastile.android.data.repository

import app.tastile.android.data.model.Tile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TilesResponse(
    val tiles: List<Tile>,
    @SerialName("next_actionable_tile_id") val nextActionableTileId: String? = null,
    @SerialName("next_actionable_start_at") val nextActionableStartAt: String? = null
)

@Serializable
data class CalendarProjectionResponse(
    val view: String,
    @SerialName("range_start") val rangeStart: String,
    @SerialName("range_end") val rangeEnd: String,
    @SerialName("grid_start") val gridStart: String,
    @SerialName("grid_end") val gridEnd: String,
    val blocks: List<CalendarProjectionBlockResponse> = emptyList(),
    @SerialName("all_day_spans") val allDaySpans: List<CalendarProjectionBlockResponse> = emptyList(),
    @SerialName("overflow_counters") val overflowCounters: Map<String, Int> = emptyMap(),
    @SerialName("month_summaries") val monthSummaries: List<CalendarMonthSummaryResponse> = emptyList()
)

@Serializable
data class CalendarProjectionBlockResponse(
    @SerialName("tile_id") val tileId: String? = null,
    val title: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("semantic_role") val semanticRole: String? = null,
    @SerialName("all_day") val allDay: Boolean = false,
    val ownership: String = "tastile_owned",
    val editable: Boolean = true,
    @SerialName("source_label") val sourceLabel: String = "tastile"
)

@Serializable
data class CalendarMonthSummaryResponse(
    val month: String,
    @SerialName("item_count") val itemCount: Int
)