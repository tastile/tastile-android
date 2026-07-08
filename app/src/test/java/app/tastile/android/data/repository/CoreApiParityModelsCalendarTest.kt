package app.tastile.android.data.repository

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreApiParityModelsCalendarTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun calendarProjection_parsesMonthShape() {
        val payload = """
            {
              "view": "month",
              "range_start": "2026-04-01T00:00:00Z",
              "range_end": "2026-05-01T00:00:00Z",
              "grid_start": "2026-03-30T00:00:00Z",
              "grid_end": "2026-05-11T00:00:00Z",
              "blocks": [],
              "all_day_spans": [],
              "overflow_counters": {"2026-04-08": 2},
              "month_summaries": []
            }
        """.trimIndent()

        val response = json.decodeFromString<CalendarProjectionResponse>(payload)

        assertEquals("month", response.view)
        assertEquals("2026-03-30T00:00:00Z", response.gridStart)
        assertEquals(2, response.overflowCounters["2026-04-08"])
    }
}
