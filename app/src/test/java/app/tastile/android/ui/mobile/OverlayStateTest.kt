package app.tastile.android.ui.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayStateTest {

    @Test
    fun `Hidden is the initial state`() {
        val state: Overlay = Overlay.Hidden
        assertTrue(state is Overlay.Hidden)
    }

    @Test
    fun `TileEdit carries tileId`() {
        val state = Overlay.TileEdit(tileId = "tile-abc")
        assertEquals("tile-abc", (state as Overlay.TileEdit).tileId)
    }

    @Test
    fun `SidePanel carries a section`() {
        val state = Overlay.SidePanel(SidePanelSection.Calendar)
        assertEquals(SidePanelSection.Calendar, (state as Overlay.SidePanel).section)
    }

    @Test
    fun `SidePanelSection has 5 values`() {
        assertEquals(5, SidePanelSection.entries.size)
    }
}
