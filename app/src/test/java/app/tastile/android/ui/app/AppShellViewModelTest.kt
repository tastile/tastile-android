package app.tastile.android.ui.app

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShellViewModelTest {

    @Test
    fun `uiState starts with defaults`() = runTest {
        val vm = AppShellViewModel()
        val state = vm.uiState.first()

        assertTrue(state.isTopBarVisible)
        assertFalse(state.isSystemBarsContrastEnforced)
    }

    @Test
    fun `setTopBarVisible false updates isTopBarVisible`() = runTest {
        val vm = AppShellViewModel()
        vm.setTopBarVisible(false)

        val state = vm.uiState.first()
        assertFalse(state.isTopBarVisible)
    }

    @Test
    fun `setSystemBarsContrastEnforced true updates isSystemBarsContrastEnforced`() = runTest {
        val vm = AppShellViewModel()
        vm.setSystemBarsContrastEnforced(true)

        val state = vm.uiState.first()
        assertTrue(state.isSystemBarsContrastEnforced)
    }

    @Test
    fun `setTopBarVisible with the same value is idempotent`() = runTest {
        val vm = AppShellViewModel()
        // Initial state already has isTopBarVisible = true.
        vm.setTopBarVisible(true)

        assertTrue(vm.uiState.value.isTopBarVisible)
        // Boolean.compareTo returns 0 for equal values — no-op assignment is
        // verified by the state still being true.
        assertEquals(0, vm.uiState.value.isTopBarVisible.compareTo(true))
    }

    @Test
    fun `setSystemBarsContrastEnforced with the same value is idempotent`() = runTest {
        val vm = AppShellViewModel()
        // Initial state already has isSystemBarsContrastEnforced = false.
        vm.setSystemBarsContrastEnforced(false)

        assertFalse(vm.uiState.value.isSystemBarsContrastEnforced)
    }
}