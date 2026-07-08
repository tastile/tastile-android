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
    val pattern = if (locale == AppLocale.JA) "M/d HH:mm" else "M/d h:mm a"
    val zoneId = zone ?: ZoneId.systemDefault()
    val javaLocale = if (locale == AppLocale.JA) Locale.JAPAN else Locale.US
    return instant.atZone(zoneId).format(DateTimeFormatter.ofPattern(pattern, javaLocale))
}