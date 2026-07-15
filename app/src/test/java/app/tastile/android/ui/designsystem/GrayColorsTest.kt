package app.tastile.android.ui.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class GrayColorsTest {

    @Test
    fun surface_matchesWebGrayPaletteToken() {
        assertEquals(Color(0xFFF4F4F5), GrayColors.surface)
    }

    @Test
    fun onSurface_isDarkInk() {
        assertEquals(Color(0xFF18181B), GrayColors.onSurface)
    }

    @Test
    fun primary_isZincMidTone() {
        assertEquals(Color(0xFF71717A), GrayColors.primary)
    }
}