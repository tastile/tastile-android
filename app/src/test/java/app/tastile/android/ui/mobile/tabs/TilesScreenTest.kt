package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TilesScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun stubVm(tiles: List<Tile> = emptyList()): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.tiles } returns MutableStateFlow(tiles)
        every { vm.loading } returns MutableStateFlow(false)
        every { vm.locale } returns MutableStateFlow(app.tastile.android.data.repository.AppLocale.EN)
        return vm
    }

    private fun stubOverlay(): OverlayViewModel = mockk<OverlayViewModel>(relaxed = true)

    @Test
    fun `row shows tile title with lifecycle glyph`() {
        val tile = Tile(id = "t1", title = "Review PR", lifecycle = TileLifecycle.READY.value)
        rule.setContent { TilesScreen(viewModel = stubVm(listOf(tile)), overlay = stubOverlay()) }

        rule.onAllNodesWithText("Review PR", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `row tap selects tile and shows TileEdit overlay`() {
        val tile = Tile(id = "t1", title = "Review PR", lifecycle = TileLifecycle.READY.value)
        val vm = stubVm(listOf(tile))
        val overlay = stubOverlay()

        rule.setContent { TilesScreen(viewModel = vm, overlay = overlay) }
        rule.onAllNodesWithText("Review PR", substring = true).onFirst().performClick()

        verify { vm.selectTile("t1") }
        verify { overlay.show(Overlay.TileEdit("t1")) }
    }

    @Test
    fun `loading with no tiles shows indeterminate progress`() {
        val vm = stubVm(tiles = emptyList())
        every { vm.loading } returns MutableStateFlow(true)
        rule.setContent { TilesScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onAllNodes(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            ),
        ).onFirst().assertIsDisplayed()
    }
}
