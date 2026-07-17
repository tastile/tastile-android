package app.tastile.android.ui.mobile.calendar

import java.time.Instant

internal fun parseInstantOrNull(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        Instant.parse(value)
    } catch (_: Exception) {
        try {
            java.time.OffsetDateTime.parse(value).toInstant()
        } catch (_: Exception) {
            null
        }
    }
}
