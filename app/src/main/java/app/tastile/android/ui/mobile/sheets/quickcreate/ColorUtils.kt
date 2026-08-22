package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Default tile color used when no visual color has been authored yet.
 * Mirrors the v1 source-tile default in tastile-web.
 */
internal val DefaultTileColor: Color = Color(0xFF3B82F6)

/**
 * Parse a `#RRGGBB` or `#AARRGGBB` hex string into a Compose [Color].
 *
 * The previous per-panel implementations constructed `Color(cleaned.toLong(16))`
 * for six-digit hex like `#3b82f6`, which Compose interprets as a packed
 * ARGB long with the high byte set to zero — producing a fully transparent
 * color. As a result `selected == swatch` was always false and the swatch
 * row never reflected the active selection.
 *
 * This helper:
 *  - treats six-digit hex (`#RRGGBB`) as opaque RGB by OR-ing in `0xFF000000`;
 *  - treats eight-digit hex (`#AARRGGBB`) as ARGB;
 *  - falls back to [DefaultTileColor] for blank / malformed input so the
 *    swatch row always has a selected baseline.
 */
internal fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#").trim()
    if (cleaned.isEmpty()) return DefaultTileColor
    return runCatching {
        when (cleaned.length) {
            6 -> Color((0xFF000000u).toInt() or cleaned.toLong(16).toInt())
            8 -> Color(cleaned.toLong(16))
            else -> DefaultTileColor
        }
    }.getOrDefault(DefaultTileColor)
}

/**
 * Round-trip a Compose [Color] back to a `#RRGGBB` string suitable for the
 * v1 source-tile wire.
 *
 * The previous implementation formatted `Color.value.toLong().toULong()`
 * as a hex string. Because Compose packs the color space id into the
 * upper bits of `Color.value`, the resulting string carried both the
 * color-space tag and a re-ordered ARGB layout, which the server
 * consistently rejected on round-trip. Using [Color.toArgb] (and
 * stripping the alpha byte) gives a stable wire format.
 */
internal fun Color.toHexString(): String {
    val rgb = toArgb() and 0xFFFFFF
    return "#" + rgb.toString(16).padStart(6, '0')
}

/**
 * Stable identifier for a swatch used in [androidx.compose.ui.platform.testTag].
 *
 * Drops the alpha byte and the Compose color-space packing so two visually
 * identical swatches share the same id. The previous
 * `value.toLong().toString(16)` produced negative hex strings like
 * `-3b82f600000000` that are awkward to target from UI tests and which
 * also depend on the packed color-space bits.
 */
internal fun Color.toSwatchId(): String {
    val rgb = toArgb() and 0xFFFFFF
    return rgb.toString(16).padStart(6, '0')
}