package app.tastile.android.ui.time

import app.tastile.android.data.user.AppLocale
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
    // 5-language gate: pick a pattern + java.util.Locale per AppLocale so
    // each supported language renders the date with its own conventions
    // (e.g. zh-CN day-first, ja year-month-day hour, ko AM/PM marker).
    val pattern = when (locale) {
        AppLocale.EN -> "M/d h:mm a"
        AppLocale.JA -> "M/d HH:mm"
        AppLocale.ZH_CN -> "M/d HH:mm"
        AppLocale.KO -> "M/d a h:mm"
        AppLocale.ES -> "d/M H:mm"
    }
    val javaLocale = when (locale) {
        AppLocale.EN -> Locale.US
        AppLocale.JA -> Locale.JAPAN
        AppLocale.ZH_CN -> Locale.SIMPLIFIED_CHINESE
        AppLocale.KO -> Locale.KOREAN
        AppLocale.ES -> Locale.forLanguageTag("es-ES")
    }
    return dateTime.format(DateTimeFormatter.ofPattern(pattern, javaLocale))
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
