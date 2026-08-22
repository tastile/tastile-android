package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-Kotlin coverage for the hex/Compose-Color helpers used by every
 * QuickCreate workflow panel. The previous per-panel implementations
 * constructed `Color(cleaned.toLong(16))` for six-digit hex like `#3b82f6`,
 * which Compose interprets as a packed ARGB long with the high byte set
 * to zero — producing a fully transparent color. As a result, the
 * `selected == swatch` comparison inside `ColorSwatchRow` was always
 * false and the swatch row never reflected the active selection.
 *
 * These tests pin the round-trip behavior so the swatch indicator can
 * only regress through a deliberate change.
 */
class QuickCreateColorUtilsTest {

    @Test
    fun `six-digit hex parses as opaque rgb with full alpha`() {
        val parsed = parseHexColor("#3b82f6")
        val expected = Color(0xFF3B82F6)
        assertEquals(
            "six-digit hex must produce an opaque color matching 0xFF prefix",
            expected.toArgb(),
            parsed.toArgb(),
        )
        assertTrue("alpha must be 0xFF", parsed.alpha == 1f)
    }

    @Test
    fun `six-digit hex without hash parses identically`() {
        assertEquals(
            parseHexColor("#3b82f6").toArgb(),
            parseHexColor("3b82f6").toArgb(),
        )
    }

    @Test
    fun `eight-digit hex parses argb verbatim`() {
        val parsed = parseHexColor("#803b82f6")
        // 0x803b82f6 = alpha 0x80, rgb 0x3b82f6
        assertEquals(0x803b82f6, parsed.toArgb())
    }

    @Test
    fun `web swatches round-trip exactly through toHexString`() {
        // The exact list rendered by ProjectColorRow.WebColorSwatches. Each
        // swatch must round-trip without alpha bleed so the swatch row's
        // `selected == swatch` check matches the actually-selected color.
        val swatches = listOf(
            Color(0xFF3B82F6),
            Color(0xFF10B981),
            Color(0xFFA855F7),
            Color(0xFFF59E0B),
            Color(0xFFEF4444),
            Color(0xFF6B7280),
        )
        swatches.forEach { swatch ->
            val hex = swatch.toHexString()
            assertEquals(
                "swatch $swatch must round-trip through toHexString -> parseHexColor",
                swatch.toArgb(),
                parseHexColor(hex).toArgb(),
            )
            assertEquals(
                "round-tripped hex must be six digits with a hash",
                "#" + (swatch.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0'),
                hex,
            )
        }
    }

    @Test
    fun `blank and malformed hex fall back to the default tile color`() {
        val fallback = parseHexColor("").toArgb()
        assertEquals(DefaultTileColor.toArgb(), fallback)
        assertEquals(DefaultTileColor.toArgb(), parseHexColor("#").toArgb())
        assertEquals(DefaultTileColor.toArgb(), parseHexColor("#zzzzzz").toArgb())
        assertEquals(DefaultTileColor.toArgb(), parseHexColor("#12").toArgb())
    }

    @Test
    fun `default identity color parses to an opaque swatch`() {
        // The store seeds QuickCreateVisual(color = "#3b82f6"). Without the
        // fix, parseHexColor("#3b82f6") returned a transparent color and the
        // swatch indicator never lit up.
        val parsed = parseHexColor("#3b82f6")
        assertNotEquals(0, parsed.toArgb())
        assertTrue(parsed.alpha == 1f)
    }

    @Test
    fun `toHexString drops the alpha byte and pads to six digits`() {
        assertEquals("#000000", Color(0xFF000000).toHexString())
        assertEquals("#ffffff", Color(0xFFFFFFFF).toHexString())
        assertEquals("#3b82f6", Color(0xFF3B82F6).toHexString())
    }

    @Test
    fun `toSwatchId returns the six-digit rgb hex without color-space noise`() {
        assertEquals("3b82f6", Color(0xFF3B82F6).toSwatchId())
        assertEquals("000000", Color(0xFF000000).toSwatchId())
        assertEquals("ffffff", Color(0xFFFFFFFF).toSwatchId())
        // ARGB-only input (0x80 alpha) should still produce the RGB-only id.
        assertEquals("3b82f6", Color(0x803B82F6).toSwatchId())
    }
}