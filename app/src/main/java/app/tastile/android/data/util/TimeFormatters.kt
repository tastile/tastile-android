package app.tastile.android.data.util

import app.tastile.android.data.repository.AppLocale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * ISO instant string → short human label honouring the active
 * [AppLocale]. Used by the Changes sub-tab of the mobile Tiles tab.
 *
 * Mirrors the format choice in `ui/util/DateTimeUtils.kt#formatDateShort`
 * (M/d HH:mm in JA, M/d h:mm a in EN) but starting from an ISO instant
 * instead of a [java.time.LocalDateTime]. Falls back to the raw ISO
 * string when parsing fails so the row never collapses to an empty
 * label.
 */
fun formatIsoDateTime(
    iso: String?,
    locale: AppLocale,
    zone: ZoneId? = null,
): String {
    if (iso.isNullOrBlank()) return ""
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return iso
    // 5-language gate: per-locale pattern + java.util.Locale so day-first
    // (zh-CN) / 24h (ja) / AM/PM (en, ko) all render correctly. zh-CN/ko/es
    // fall back to en strings on the resource side; the formatter here is
    // the runtime hint for the date-time format.
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
    val zoneId = zone ?: ZoneId.systemDefault()
    return instant.atZone(zoneId).format(DateTimeFormatter.ofPattern(pattern, javaLocale))
}