package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.model.Workspace
import app.tastile.android.data.user.AppLocale
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
 * Compose-level coverage for the mobile Projects tab.
 *
 * The screen reads the workspace list from `DashboardViewModel.workspaces`
 * (not from `ProjectsViewModel.state`), so the test must stub the former
 * for the LazyColumn to materialise any rows. The current production
 * layout uses a flat list of `projects-row-<id>` rows (single column,
 * no card grid / open-button / inline tile rows); assertions target
 * those tags.
 */
@RunWith(AndroidJUnit4::class)
class ProjectsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun stubDashVm(
        workspaces: List<Workspace> = emptyList(),
        tiles: List<Tile> = emptyList(),
        tileCountByOwner: Map<String, Int> = emptyMap(),
    ): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.workspaces } returns MutableStateFlow(workspaces)
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
        selectedOwnerId: String? = null,
    ): ProjectsViewModel {
        val vm = mockk<ProjectsViewModel>(relaxed = true)
        every { vm.state } returns MutableStateFlow(ProjectsViewModel.State(workspaces = emptyList()))
        every { vm.selectedOwnerId } returns MutableStateFlow(selectedOwnerId)
        every { vm.creating } returns MutableStateFlow(false)
        every { vm.deleteCandidate } returns MutableStateFlow(null)
        every { vm.editCandidate } returns MutableStateFlow(null)
        return vm
    }

    private fun stubOverlay(): OverlayViewModel = mockk<OverlayViewModel>(relaxed = true)

    @Test
    fun `renders a row for each workspace and uses the workspace id as test tag`() {
        val wsA = Workspace(
            id = "ws-a",
            kind = Workspace.KIND_WORKSPACE,
            displayName = "Project A",
            slug = null,
            email = null,
            parentSubjectId = null,
            color = "#10B981",
            ownerUserId = null,
            disabledAt = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )
        val wsB = Workspace(
            id = "ws-b",
            kind = Workspace.KIND_WORKSPACE,
            displayName = "Project B",
            slug = "project-b",
            email = null,
            parentSubjectId = null,
            color = null,
            ownerUserId = null,
            disabledAt = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )
        val dashVm = stubDashVm(workspaces = listOf(wsA, wsB))
        val projectsVm = stubProjectsVm()

        rule.setContent {
            TastileTheme {
                ProjectsScreen(
                    viewModel = dashVm,
                    overlay = stubOverlay(),
                    projectsViewModel = projectsVm,
                )
            }
        }

        rule.onNodeWithTag("projects-screen-body").assertIsDisplayed()
        rule.onNodeWithTag("projects-row-ws-a").assertIsDisplayed()
        rule.onNodeWithTag("projects-row-ws-b").assertIsDisplayed()
        // The dot Box has a 10dp size; under the v1 Compose test rule the
        // semantics tree considers it merged into the parent Row, so look
        // it up via the unmerged tree.
        rule.onNodeWithTag("projects-row-dot-ws-a", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("projects-row-dot-ws-b", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `renders empty projects state when no workspaces and not loading`() {
        val dashVm = stubDashVm(workspaces = emptyList())
        val projectsVm = stubProjectsVm()

        rule.setContent {
            TastileTheme {
                ProjectsScreen(
                    viewModel = dashVm,
                    overlay = stubOverlay(),
                    projectsViewModel = projectsVm,
                )
            }
        }

        rule.onNodeWithTag("projects-screen-body").assertIsDisplayed()
    }
}
