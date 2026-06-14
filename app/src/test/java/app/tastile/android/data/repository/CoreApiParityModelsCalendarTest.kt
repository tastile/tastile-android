package app.tastile.android.data.repository

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreApiParityModelsCalendarTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun integrationSettings_parsesCalendarExtendedFields() {
        val payload = """
            {
              "google_calendar": {
                "connected": true,
                "provider_status": "connected",
                "can_read": true,
                "can_write": true,
                "account_email": "user@example.com",
                "selected_calendar_id": "primary",
                "granted_scopes": ["calendar.read", "calendar.write"],
                "sync_mode": "bidirectional",
                "read_policy": "import_only",
                "write_policy": "all_editable",
                "last_synced_at": "2026-04-08T00:00:00Z",
                "last_full_sync_at": "2026-04-08T00:00:00Z"
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<IntegrationSettingsResponse>(payload)

        assertTrue(response.googleCalendar.connected)
        assertEquals("connected", response.googleCalendar.providerStatus)
        assertEquals("primary", response.googleCalendar.selectedCalendarId)
        assertEquals("bidirectional", response.googleCalendar.syncMode)
        assertEquals("all_editable", response.googleCalendar.writePolicy)
    }

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
