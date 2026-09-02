package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.data.model.Tile
import app.tastile.android.data.user.AppLocale
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ListGroupingMode
import app.tastile.android.ui.dashboard.ListViewMode
import app.tastile.android.ui.dashboard.TileGranularity
import app.tastile.android.ui.dashboard.TileRange
import app.tastile.android.ui.dashboard.TilesTab
import app.tastile.android.ui.dashboard.TimelineSubScale
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TilesScreenFabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Stub all StateFlows TilesScreen reads at first composition. Without
    // this, the relaxed mock's `Object` value gets unboxed by Compose's
    // by-delegate as the StateFlow's element type and ClassCastExceptions
    // before the test can assert anything. Mirrors TilesScreenTest.kt:40-71.
    private fun stubVm(): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.tiles } returns MutableStateFlow<List<Tile>>(emptyList())
        every { vm.loading } returns MutableStateFlow(false)
        every { vm.locale } returns MutableStateFlow(AppLocale.EN)
        every { vm.activeTilesTab } returns MutableStateFlow(TilesTab.LIST)
        every { vm.searchTerm } returns MutableStateFlow("")
        every { vm.filterRange } returns MutableStateFlow(TileRange.ALL)
        every { vm.filterGranularity } returns MutableStateFlow(TileGranularity.MIN_5M)
        every { vm.filterLimit } returns MutableStateFlow(50)
        every { vm.listGroupingMode } returns MutableStateFlow(ListGroupingMode.STATE)
        every { vm.listViewMode } returns MutableStateFlow(ListViewMode.COMFORTABLE)
        every { vm.groupedTiles } returns MutableStateFlow(emptyList())
        every { vm.timeline } returns MutableStateFlow<List<CoreTimelineItem>>(emptyList())
        every { vm.timelineScale } returns MutableStateFlow(TimelineSubScale.DAY)
        every { vm.customStartIso } returns MutableStateFlow(null)
        every { vm.customEndIso } returns MutableStateFlow(null)
        every { vm.requestDeleteTileId } returns MutableStateFlow(null)
        every { vm.requestDeferTileId } returns MutableStateFlow(null)
        every { vm.requestPromptTileId } returns MutableStateFlow(null)
        every { vm.expandedSections } returns MutableStateFlow(emptySet())
        every { vm.sectionLimits } returns MutableStateFlow(emptyMap())
        every { vm.tileFilter } returns MutableStateFlow(app.tastile.android.data.tile.TileFilter.DEFAULT)
        return vm
    }

    @Test
    fun `fab click shows Overlay QuickCreate`() {
        val viewModel = stubVm()
        val overlay: OverlayViewModel = mockk(relaxed = true)

        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TilesScreen(viewModel = viewModel, overlay = overlay)
                }
            }
        }
        composeTestRule.onNodeWithTag("tiles-fab-new").performClick()
        verify { overlay.show(Overlay.QuickCreate) }
    }

    @Test
    fun `tiles-fab-new is rendered`() {
        val viewModel = stubVm()
        val overlay: OverlayViewModel = mockk(relaxed = true)

        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TilesScreen(viewModel = viewModel, overlay = overlay)
                }
            }
        }
        composeTestRule.onNodeWithTag("tiles-fab-new").assertIsDisplayed()
    }
}
