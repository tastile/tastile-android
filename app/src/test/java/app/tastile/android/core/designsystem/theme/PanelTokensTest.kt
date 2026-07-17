package app.tastile.android.core.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class PanelTokensTest {
    @Test fun `LeadingColumnWidth equals 56dp - M3 ListItem slot`() {
        assertEquals(56f, PanelTokens.LeadingColumnWidth.value)
    }

    @Test fun `LeadingIconSize equals 24dp - M3 ListItem leading icon`() {
        assertEquals(24f, PanelTokens.LeadingIconSize.value)
    }

    @Test fun `LeadingColumnGap equals 16dp - M3 ListItem leading-to-text gap`() {
        assertEquals(16f, PanelTokens.LeadingColumnGap.value)
    }

    @Test fun `LeadingColumnWidth is the sum of icon size plus gap plus 16dp start padding`() {
        val expected = PanelTokens.LeadingIconSize.value +
            PanelTokens.LeadingColumnGap.value +
            16f
        assertEquals(expected, PanelTokens.LeadingColumnWidth.value)
    }
}
