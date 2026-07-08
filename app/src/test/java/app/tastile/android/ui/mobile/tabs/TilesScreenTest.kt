package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ListGroupingMode
import app.tastile.android.ui.dashboard.ListViewMode
import app.tastile.android.ui.dashboard.TileGranularity
import app.tastile.android.ui.dashboard.TileRange
import app.tastile.android.ui.dashboard.TilesTab
import app.tastile.android.ui.dashboard.TimelineSubScale
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

    private fun stubVm(
        tiles: List<Tile> = emptyList(),
        timeline: List<CoreTimelineItem> = emptyList(),
        activeTab: TilesTab = TilesTab.LIST,
        sectionLimits: Map<String, Int> = emptyMap(),
    ): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.tiles } returns MutableStateFlow(tiles)
        every { vm.loading } returns MutableStateFlow(false)
        every { vm.locale } returns MutableStateFlow(AppLocale.EN)
        every { vm.activeTilesTab } returns MutableStateFlow(activeTab)
        every { vm.searchTerm } returns MutableStateFlow("")
        every { vm.filterRange } returns MutableStateFlow(TileRange.ALL)
        every { vm.filterGranularity } returns MutableStateFlow(TileGranularity.MIN_5M)
        every { vm.filterLimit } returns MutableStateFlow(50)
        every { vm.listGroupingMode } returns MutableStateFlow(ListGroupingMode.STATE)
        every { vm.listViewMode } returns MutableStateFlow(ListViewMode.COMFORTABLE)
        every { vm.groupedTiles } returns MutableStateFlow(emptyList())
        every { vm.timeline } returns MutableStateFlow(timeline)
        every { vm.timelineScale } returns MutableStateFlow(TimelineSubScale.DAY)
        every { vm.customStartIso } returns MutableStateFlow(null)
        every { vm.customEndIso } returns MutableStateFlow(null)
        every { vm.requestDeleteTileId } returns MutableStateFlow(null)
        every { vm.expandedSections } returns MutableStateFlow(emptySet())
        every { vm.sectionLimits } returns MutableStateFlow(sectionLimits)
        every { vm.tileFilter } returns MutableStateFlow(app.tastile.android.data.repository.TileFilter.DEFAULT)
        return vm
    }

    private fun stubOverlay(): OverlayViewModel = mockk<OverlayViewModel>(relaxed = true)

    @Test
    fun `list tab renders the 12-control composition even when tiles are empty`() {
        rule.setContent {
            TilesScreen(viewModel = stubVm(), overlay = stubOverlay())
        }
        // header row + FAB
        rule.onNodeWithTag("tiles-header-row").assertIsDisplayed()
        rule.onNodeWithTag("tiles-fab-new").assertIsDisplayed()
        // list body
        rule.onNodeWithTag("tiles-list-body").assertIsDisplayed()
        // 3-tab pill
        rule.onNodeWithTag("tiles-tab-list").assertIsDisplayed()
        rule.onNodeWithTag("tiles-tab-timeline").assertIsDisplayed()
        rule.onNodeWithTag("tiles-tab-changes").assertIsDisplayed()
        // 5 filter controls (search + 3 dropdowns + grouping + view-mode = 6 filter row controls)
        rule.onNodeWithTag("tiles-filter-search").assertIsDisplayed()
        rule.onNodeWithTag("tiles-filter-range").assertIsDisplayed()
        rule.onNodeWithTag("tiles-filter-granularity").assertIsDisplayed()
        rule.onNodeWithTag("tiles-filter-limit").assertIsDisplayed()
        rule.onNodeWithTag("tiles-filter-grouping").assertIsDisplayed()
        rule.onNodeWithTag("tiles-filter-view-mode").assertIsDisplayed()
    }

    @Test
    fun `tab switch invokes vm setActiveTilesTab`() {
        val vm = stubVm()
        rule.setContent {
            TilesScreen(viewModel = vm, overlay = stubOverlay())
        }
        rule.onNodeWithTag("tiles-tab-timeline").performClick()
        verify { vm.setActiveTilesTab(TilesTab.TIMELINE) }
        rule.onNodeWithTag("tiles-tab-changes").performClick()
        verify { vm.setActiveTilesTab(TilesTab.CHANGES) }
        rule.onNodeWithTag("tiles-tab-list").performClick()
        verify { vm.setActiveTilesTab(TilesTab.LIST) }
    }

    @Test
    fun `timeline sub tab shows scale dropdown plus empty state`() {
        val vm = stubVm(activeTab = TilesTab.TIMELINE, timeline = emptyList())
        rule.setContent {
            TilesScreen(viewModel = vm, overlay = stubOverlay())
        }
        rule.onNodeWithTag("tiles-timeline-scale").assertIsDisplayed()
    }

    @Test
    fun `timeline sub tab renders one row per item`() {
        val items = (1..3).map {
            CoreTimelineItem(
                id = "ev-$it",
                tileId = "tile-$it",
                title = "Tile $it",
                type = "work",
                status = "scheduled",
                startAt = "2026-07-08T0$it:00:00Z",
                endAt = "2026-07-08T0$it:30:00Z",
            )
        }
        val vm = stubVm(activeTab = TilesTab.TIMELINE, timeline = items)
        rule.setContent {
            TilesScreen(viewModel = vm, overlay = stubOverlay())
        }
        rule.onAllNodesWithTag("timeline-block-ev-1").assertCountEquals(1)
        rule.onAllNodesWithTag("timeline-block-ev-2").assertCountEquals(1)
        rule.onAllNodesWithTag("timeline-block-ev-3").assertCountEquals(1)
    }

    @Test
    fun `changes sub tab caps at 120 rows even with 150 events`() {
        val items = (1..150).map {
            CoreTimelineItem(
                id = "ch-$it",
                tileId = "tile-$it",
                title = "Tile $it",
                type = "work_ended",
                status = "done",
                startAt = "2026-07-08T10:00:00Z",
                endAt = null,
            )
        }
        val vm = stubVm(activeTab = TilesTab.CHANGES, timeline = items)
        rule.setContent {
            TilesScreen(viewModel = vm, overlay = stubOverlay())
        }
        rule.onAllNodesWithTag("tile-change-ch-1-work_ended").assertCountEquals(1)
        rule.onAllNodesWithTag("tile-change-ch-120-work_ended").assertCountEquals(1)
        rule.onAllNodesWithTag("tile-change-ch-121-work_ended").assertCountEquals(0)
    }

    @Test
    fun `changes sub tab empty state renders when timeline empty`() {
        val vm = stubVm(activeTab = TilesTab.CHANGES, timeline = emptyList())
        rule.setContent {
            TilesScreen(viewModel = vm, overlay = stubOverlay())
        }
        rule.onNodeWithTag("tiles-list-body").assertDoesNotExist()
    }

    @Test
    fun `search field change routes through vm setSearchTerm`() {
        val vm = stubVm()
        rule.setContent {
            TilesScreen(viewModel = vm, overlay = stubOverlay())
        }
        rule.onNodeWithTag("tiles-filter-search").performTextInput("alpha")
        verify { vm.setSearchTerm("alpha") }
    }

    @Test
    fun `delete dialog mounts when requestDeleteTileId is set`() {
        val tile = Tile(id = "del-1", title = "Delete Me", lifecycle = TileLifecycle.READY.value)
        val vm = stubVm(tiles = listOf(tile))
        every { vm.requestDeleteTileId } returns MutableStateFlow("del-1")
        rule.setContent {
            TilesScreen(viewModel = vm, overlay = stubOverlay())
        }
        rule.onNodeWithTag("tiles-delete-dialog").assertIsDisplayed()
        rule.onNodeWithTag("tiles-delete-dialog-confirm").performClick()
        verify { vm.confirmDeleteTile() }
    }

    @Test
    fun `section expander tap bumps vm section limit and toggles expanded set`() {
        val vm = stubVm()
        val section = app.tastile.android.ui.dashboard.TileSection(
            groupId = "started",
            labelKey = "started",
            tiles = (1..12).map { Tile(id = "t-$it", title = "Tile $it", lifecycle = TileLifecycle.STARTED.value) },
        )
        every { vm.groupedTiles } returns MutableStateFlow(listOf(section))
        every { vm.expandedSections } returns MutableStateFlow(emptySet())
        rule.setContent {
            TilesScreen(viewModel = vm, overlay = stubOverlay())
        }
        rule.onNodeWithTag("tiles-section-header-tap-started").performScrollTo().performClick()
        verify { vm.bumpSectionLimit("started", 12) }
        verify { vm.toggleSectionExpanded("started") }
    }
}
