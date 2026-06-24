package app.tastile.android.ui.mobile

import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayViewModelTest {

    @Test
    fun `current starts as Hidden`() = runTest {
        val vm = OverlayViewModel()
        assertTrue(vm.current.first() is Overlay.Hidden)
    }

    @Test
    fun `show replaces the current overlay`() = runTest {
        val vm = OverlayViewModel()
        vm.show(Overlay.QuickCreate)
        assertTrue(vm.current.first() is Overlay.QuickCreate)

        vm.show(Overlay.TileEdit(tileId = "x"))
        val state = vm.current.first()
        assertTrue(state is Overlay.TileEdit)
        assertEquals("x", (state as Overlay.TileEdit).tileId)
    }

    @Test
    fun `dismiss returns to Hidden`() = runTest {
        val vm = OverlayViewModel()
        vm.show(Overlay.Notifications)
        vm.dismiss()
        assertTrue(vm.current.first() is Overlay.Hidden)
    }

    @Test
    fun `show SidePanel preserves section`() = runTest {
        val vm = OverlayViewModel()
        vm.show(Overlay.SidePanel(SidePanelSection.Schedule))
        val state = vm.current.first()
        assertTrue(state is Overlay.SidePanel)
        assertEquals(SidePanelSection.Schedule, (state as Overlay.SidePanel).section)
    }
}
