package app.tastile.android.ui.util

import app.tastile.android.data.repository.AppLocale
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun parseDateTime(datePart: String, timePart: String): LocalDateTime? {
    if (datePart.isBlank() || timePart.isBlank()) return null
    return try {
        val date = LocalDate.parse(datePart, dateFormatter)
        val time = LocalTime.parse(timePart, timeFormatter)
        LocalDateTime.of(date, time)
    } catch (_: DateTimeParseException) {
        null
    }
}

fun combineDateTimeToUtcIso(datePart: String, timePart: String): String? {
    val dateTime = parseDateTime(datePart, timePart) ?: return null
    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toString()
}

fun formatDateShort(dateTime: LocalDateTime, locale: AppLocale): String {
    val pattern = if (locale == AppLocale.JA) "M/d HH:mm" else "M/d h:mm a"
    val formatter = DateTimeFormatter.ofPattern(pattern, if (locale == AppLocale.JA) Locale.JAPAN else Locale.US)
    return dateTime.format(formatter)
}

fun localDateFromEpochMillis(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(dateFormatter)
}

fun getCurrentLocalDate(): String = LocalDate.now().format(dateFormatter)

fun getCurrentLocalTime(): String = LocalTime.now().truncatedTo(ChronoUnit.MINUTES).format(timeFormatter)

fun getLocalTimeAfterMinutes(minutes: Long): String {
    return LocalTime.now().plusMinutes(minutes).truncatedTo(ChronoUnit.MINUTES).format(timeFormatter)
}
