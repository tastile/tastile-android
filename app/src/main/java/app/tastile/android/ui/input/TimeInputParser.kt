package app.tastile.android.ui.input

import android.annotation.SuppressLint
import android.content.Context
import app.tastile.android.R
import app.tastile.android.data.user.AppLocale
import app.tastile.android.ui.time.parseDateTime
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

/**
 * Format a duration as a localized human-readable string.
 *
 * Pass a [Context] so the helper resolves the per-locale pattern through
 * `R.string.duration_format_*`. When [context] is null (rare, used by callers
 * with no Android dependency), the helper falls back to the canonical
 * "<hours>h <minutes>m" representation.
 */
@SuppressLint("StringFormatInvalid")
fun formatDuration(totalMinutes: Int, locale: AppLocale, context: Context? = null): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    if (context != null) {
        return when {
            hours > 0 && minutes > 0 ->
                context.getString(R.string.duration_format_hours_minutes, hours, minutes)
            hours > 0 ->
                context.getString(R.string.duration_format_hours, hours)
            else ->
                context.getString(R.string.duration_format_minutes, minutes)
        }
    }
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
