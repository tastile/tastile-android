package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.api.Workspace
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.panels.ProjectsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose-level coverage for the mobile Projects tab. Stubs both the
 * `DashboardViewModel` (for `tiles`, `tileCountByOwnerId`) and the
 * `ProjectsViewModel` (for `state.workspaces`, `selectedOwnerId`),
 * so the screen renders a deterministic grid of cards + tile list.
 */
@RunWith(AndroidJUnit4::class)
class ProjectsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun stubDashVm(
        tiles: List<Tile> = emptyList(),
        tileCountByOwner: Map<String, Int> = emptyMap(),
    ): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.tiles } returns MutableStateFlow(tiles)
        every { vm.tileCountByOwnerId } returns MutableStateFlow(tileCountByOwner)
        every { vm.loading } returns MutableStateFlow(false)
        every { vm.error } returns MutableStateFlow(null)
        every { vm.locale } returns MutableStateFlow(AppLocale.EN)
        every { vm.requestDeleteTileId } returns MutableStateFlow(null)
        every { vm.requestDeferTileId } returns MutableStateFlow(null)
        every { vm.requestPromptTileId } returns MutableStateFlow(null)
        every { vm.lastActionMessage } returns MutableStateFlow(null)
        every { vm.executionControlStates } returns MutableStateFlow(emptyMap())
        every { vm.executionControlInFlightTileIds } returns MutableStateFlow(emptySet())
        return vm
    }

    private fun stubProjectsVm(
        workspaces: List<Workspace> = emptyList(),
        loading: Boolean = false,
        selectedOwnerId: String? = null,
    ): ProjectsViewModel {
        val vm = mockk<ProjectsViewModel>(relaxed = true)
        every { vm.state } returns MutableStateFlow(
            ProjectsViewModel.State(workspaces = workspaces, loading = loading),
        )
        every { vm.selectedOwnerId } returns MutableStateFlow(selectedOwnerId)
        every { vm.creating } returns MutableStateFlow(false)
        every { vm.deleteCandidate } returns MutableStateFlow(null)
        every { vm.editCandidate } returns MutableStateFlow(null)
        return vm
    }

    private fun stubOverlay(): OverlayViewModel = mockk<OverlayViewModel>(relaxed = true)

    @Test
    fun `renders a card for each workspace plus a tile row for each tile`() {
        val wsA = Workspace(id = "ws-a", displayName = "Project A", color = "#10B981")
        val wsB = Workspace(id = "ws-b", displayName = "Project B", slug = "project-b", color = null)
        val tile1 = Tile(id = "t1", title = "Tile one", lifecycle = TileLifecycle.READY.value)
        val tile2 = Tile(id = "t2", title = "Tile two", lifecycle = TileLifecycle.STARTED.value)
        val dashVm = stubDashVm(
            tiles = listOf(tile1, tile2),
            tileCountByOwner = mapOf("ws-a" to 1, "ws-b" to 1),
        )
        val projectsVm = stubProjectsVm(workspaces = listOf(wsA, wsB))

        rule.setContent {
            ProjectsScreen(
                viewModel = dashVm,
                overlay = stubOverlay(),
                projectsViewModel = projectsVm,
            )
        }

        rule.onNodeWithTag("projects-screen-body").assertIsDisplayed()
        rule.onNodeWithTag("projects-card-ws-a").assertIsDisplayed()
        rule.onNodeWithTag("projects-card-ws-b").assertIsDisplayed()
        rule.onNodeWithTag("projects-card-open-ws-a").assertIsDisplayed()
        rule.onNodeWithTag("projects-card-open-ws-b").assertIsDisplayed()
        rule.onNodeWithTag("projects-grid").performScrollToIndex(6)
        rule.onNodeWithTag("projects-tile-row-t1").assertIsDisplayed()
        rule.onNodeWithTag("projects-grid").performScrollToIndex(7)
        rule.onNodeWithTag("projects-tile-row-t2").assertIsDisplayed()
    }

    @Test
    fun `renders empty projects state when no workspaces and not loading`() {
        val dashVm = stubDashVm(tiles = emptyList())
        val projectsVm = stubProjectsVm(workspaces = emptyList(), loading = false)

        rule.setContent {
            ProjectsScreen(
                viewModel = dashVm,
                overlay = stubOverlay(),
                projectsViewModel = projectsVm,
            )
        }

        rule.onNodeWithTag("projects-screen-body").assertIsDisplayed()
    }
}
