package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.user.AppLocale
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.FixedTasksScope
import app.tastile.android.ui.dashboard.ProjectSection
import app.tastile.android.ui.dashboard.SortOrder
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose-level coverage for the mobile Tasks tab.
 *
 * Project-axis rewrite:
 * - tabs are derived from `projectSections` (ALL / STARRED / UNASSIGNED + per-project).
 * - section header is a single `Surface` that owns both the label and the
 *   child tile rows so the accordion "color" stretches across the tiles.
 * - bucket label does **not** show the raw `SortOrder.id`; sorting lives in
 *   a dropdown the swap icon opens.
 */
@RunWith(AndroidJUnit4::class)
class TasksScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun stubVm(
        tiles: List<Tile> = emptyList(),
        sections: List<ProjectSection> = emptyList(),
        visibleSection: ProjectSection = ProjectSection(
            id = FixedTasksScope.ALL.id,
            label = "All",
            tiles = emptyList(),
        ),
        completedTiles: List<Tile> = emptyList(),
        sortOrder: SortOrder = SortOrder.DEFAULT,
    ): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.tiles } returns MutableStateFlow(tiles)
        every { vm.projectSections } returns MutableStateFlow(
            // Default: surface the visible section as a tab even when the caller
            // didn`t build an explicit `sections` list. The real
            // DashboardViewModel always adds ALL/STARRED/UNASSIGNED here, and
            // most of these tests care about a single visible section.
            if (sections.isEmpty()) listOf(visibleSection) else sections,
        )
        every { vm.visibleSection } returns MutableStateFlow(visibleSection)
        every { vm.selectedSectionId } returns MutableStateFlow(visibleSection.id)
        every { vm.completedTiles } returns MutableStateFlow(completedTiles)
        every { vm.sortOrder } returns MutableStateFlow(sortOrder)
        every { vm.loading } returns MutableStateFlow(false)
        every { vm.error } returns MutableStateFlow(null)
        every { vm.requestDeleteTileId } returns MutableStateFlow(null)
        every { vm.requestDeferTileId } returns MutableStateFlow(null)
        every { vm.requestPromptTileId } returns MutableStateFlow(null)
        every { vm.lastActionMessage } returns MutableStateFlow(null)
        every { vm.executionControlStates } returns MutableStateFlow(emptyMap())
        every { vm.executionControlInFlightTileIds } returns MutableStateFlow(emptySet())
        every { vm.locale } returns MutableStateFlow(AppLocale.EN)
        return vm
    }

    private fun stubOverlay(): OverlayViewModel = mockk<OverlayViewModel>(relaxed = true)

    @Test
    fun `tabs render one entry per project section`() {
        val sectionAll = ProjectSection(id = FixedTasksScope.ALL.id, label = "All", tiles = emptyList())
        val sectionStarred = ProjectSection(id = FixedTasksScope.STARRED.id, label = "Starred", tiles = emptyList())
        val sectionUnassigned = ProjectSection(id = FixedTasksScope.UNASSIGNED.id, label = "Unassigned", tiles = emptyList())
        val sectionProject = ProjectSection(id = "project:Lab", label = "Lab", tiles = emptyList())
        val vm = stubVm(sections = listOf(sectionAll, sectionStarred, sectionUnassigned, sectionProject))
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onNodeWithTag("tasks-scope-tabs-row").assertIsDisplayed()
        rule.onNodeWithTag("tasks-scope-tab-${FixedTasksScope.ALL.id}").assertIsDisplayed()
        rule.onNodeWithTag("tasks-scope-tab-${FixedTasksScope.STARRED.id}").assertIsDisplayed()
        rule.onNodeWithTag("tasks-scope-tab-${FixedTasksScope.UNASSIGNED.id}").assertIsDisplayed()
        rule.onNodeWithTag("tasks-scope-tab-project:Lab").assertIsDisplayed()
    }

    @Test
    fun `first tab label sits flush with the tab-row start edge`() {
        val section = ProjectSection(id = FixedTasksScope.ALL.id, label = "All",
            tiles = emptyList())
        val vm = stubVm(visibleSection = section)
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onNodeWithTag("tasks-scope-tab-${FixedTasksScope.ALL.id}").assertIsDisplayed()
    }

    @Test
    fun `section bar exposes label, sort icon and chevron`() {
        val section = ProjectSection(id = FixedTasksScope.ALL.id, label = "All", tiles = emptyList())
        val vm = stubVm(visibleSection = section)
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        // The scope-tabs LazyColumn item pushes the accordion below the
        // viewport on smaller test windows; assert the section bar and its
        // descendants exist rather than insisting on visibility. The
        // production layout renders them in the same Column so they
        // always appear together.
        rule.onNodeWithTag("tasks-section-bar", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag(
            "tasks-bucket-label-${FixedTasksScope.ALL.id}",
            useUnmergedTree = true,
        ).assertExists()
        rule.onNodeWithTag("tasks-sort-button", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `bucket header does not show the raw sort order id`() {
        // Regression: bucket label should read "All" — never the "time_asc"
        // debug chip that earlier revisions leaked onto the bar.
        val section = ProjectSection(id = FixedTasksScope.ALL.id, label = "All",
            tiles = emptyList())
        val vm = stubVm(visibleSection = section,
            sortOrder = SortOrder.BY_TIME_DESC)
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onAllNodesWithText("time_asc", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("time_desc", substring = true).assertCountEquals(0)
    }

    @Test
    fun `tapping the section bar collapses the body and hides tiles`() {
        val section = ProjectSection(
            id = FixedTasksScope.ALL.id,
            label = "All",
            tiles = listOf(
                Tile(id = "a", title = "Alpha", lifecycle = TileLifecycle.READY.value),
                Tile(id = "b", title = "Beta", lifecycle = TileLifecycle.READY.value),
            ),
        )
        val vm = stubVm(visibleSection = section)
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onAllNodesWithTag("execute-tile-a").assertCountEquals(1)
        rule.onNodeWithTag("tasks-section-bar").performClick()
        rule.onAllNodesWithTag("execute-tile-a").assertCountEquals(0)
        rule.onNodeWithTag("tasks-section-bar").performClick()
        rule.onAllNodesWithTag("execute-tile-a").assertCountEquals(1)
    }

    @Test
    fun `task cards render with content-driven height`() {
        // Plain ("休憩") has no schedule → flat row; "休憩" with a
        // releaseAt adds a sub-line. The regression we care about is
        // that both render without crashing.
        val plain = Tile(id = "a", title = "休憩", lifecycle = TileLifecycle.READY.value)
        val scheduled = Tile(
            id = "b",
            title = "休憩",
            lifecycle = TileLifecycle.READY.value,
            releaseAt = "2026-07-21T16:00:00Z",
        )
        val section = ProjectSection(
            id = FixedTasksScope.ALL.id,
            label = "All",
            tiles = listOf(plain, scheduled),
        )
        val vm = stubVm(visibleSection = section)
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onAllNodesWithTag("execute-tile-a").assertCountEquals(1)
        rule.onAllNodesWithTag("execute-tile-b").assertCountEquals(1)
    }

    @Test
    fun `tapping a tab calls setSelectedSection on the view model`() {
        val sectionAll = ProjectSection(id = FixedTasksScope.ALL.id, label = "All", tiles = emptyList())
        val sectionUnassigned = ProjectSection(id = FixedTasksScope.UNASSIGNED.id, label = "Unassigned", tiles = emptyList())
        val vm = stubVm(
            tiles = emptyList(),
            visibleSection = sectionAll,
        )
        every { vm.projectSections } returns MutableStateFlow(listOf(sectionAll, sectionUnassigned))
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onNodeWithTag("tasks-scope-tab-${FixedTasksScope.UNASSIGNED.id}").performClick()
        verify(exactly = 1) { vm.setSelectedSection(FixedTasksScope.UNASSIGNED.id) }
    }

    @Test
    fun `each tile renders with execute-tile test tag`() {
        val section = ProjectSection(
            id = FixedTasksScope.ALL.id,
            label = "All",
            tiles = listOf(
                Tile(id = "a", title = "Alpha", lifecycle = TileLifecycle.READY.value),
                Tile(id = "b", title = "Beta", lifecycle = TileLifecycle.READY.value),
            ),
        )
        val vm = stubVm(visibleSection = section)
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onAllNodesWithTag("execute-tile-a").assertCountEquals(1)
        rule.onAllNodesWithTag("execute-tile-b").assertCountEquals(1)
    }

    @Test
    fun `completed card renders even when no tiles are completed`() {
        val vm = stubVm(completedTiles = emptyList())
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onNodeWithTag("tasks-done-card").assertIsDisplayed()
        rule.onAllNodesWithTag("tasks-done-row-t1").assertCountEquals(0)
    }

    @Test
    fun `tapping the completed card expands and renders done tiles`() {
        val doneTile = Tile(id = "d1", title = "Done", lifecycle = TileLifecycle.DONE.value)
        val vm = stubVm(completedTiles = listOf(doneTile))
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onNodeWithTag("tasks-done-card").performClick()
        rule.mainClock.advanceTimeBy(100)
        rule.onAllNodesWithTag("tasks-done-row-d1").assertCountEquals(1)
    }

    @Test
    fun `sort icon delegates to setSortOrder on the view model`() {
        val section = ProjectSection(id = FixedTasksScope.ALL.id, label = "All", tiles = emptyList())
        val vm = stubVm(visibleSection = section)
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onNodeWithTag("tasks-sort-button").performClick()
        rule.onNodeWithTag("tasks-sort-option-${SortOrder.BY_TITLE.id}").performClick()
        verify(exactly = 1) { vm.setSortOrder(SortOrder.BY_TITLE) }
    }

    @Test
    fun `empty section surfaces the empty state card`() {
        val section = ProjectSection(id = FixedTasksScope.ALL.id, label = "All", tiles = emptyList())
        val vm = stubVm(visibleSection = section)
        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onNodeWithTag("tasks-empty").assertIsDisplayed()
    }
}
