package app.tastile.android.ui.util

import app.tastile.android.data.repository.AppLocale
import java.time.Duration

fun parseNonNegativeInt(value: String): Int? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val parsed = trimmed.toIntOrNull() ?: return null
    if (parsed < 0) return null
    return parsed
}

fun sanitizeNumericInput(value: String): String = value.filter(Char::isDigit)

fun parseDurationToMinutes(hoursValue: String, minutesValue: String): Int? {
    val hours = parseNonNegativeInt(hoursValue)
    val minutes = parseNonNegativeInt(minutesValue)
    if (hours == null && minutes == null) return null
    val total = (hours ?: 0) * 60 + (minutes ?: 0)
    if (total <= 0) return null
    return total
}

fun parseBoundedDurationMinutes(startDate: String, startTime: String, endDate: String, endTime: String): Int? {
    val start = parseDateTime(startDate, startTime) ?: return null
    val end = parseDateTime(endDate, endTime) ?: return null
    val diff = Duration.between(start, end).toMinutes().toInt()
    if (diff <= 0) return null
    return diff
}

fun parseTimeToMinutes(time: String): Int? {
    val match = Regex("""^(\d{2}):(\d{2})$""").matchEntire(time) ?: return null
    val h = match.groupValues[1].toIntOrNull() ?: return null
    val m = match.groupValues[2].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

fun formatDuration(totalMinutes: Int, locale: AppLocale): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (locale == AppLocale.JA) {
        when {
            hours > 0 && minutes > 0 -> "${hours}時間${minutes}分"
            hours > 0 -> "${hours}時間"
            else -> "${minutes}分"
        }
    } else {
        when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
