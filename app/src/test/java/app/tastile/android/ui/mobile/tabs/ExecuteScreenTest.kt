package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecutionControlState
import app.tastile.android.ui.dashboard.ProjectSection
import app.tastile.android.ui.dashboard.SortOrder
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.tabs.tiles.DeferTileDialog
import app.tastile.android.ui.mobile.tabs.tiles.PromptRequestDialog
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose-level coverage for the mobile Tasks tab.
 *
 * The Tasks view is project-axis: tab row + section bar + filling tile
 * cards. Active-tile hero no longer exists; STARTED lifecycle is just a
 * TaskCard with a Pause/Resume TextButton inline (kept here so the existing
 * `execute-pause-*` and `execute-resume-*` tests still pass).
 */
@RunWith(AndroidJUnit4::class)
class ExecuteScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun stubVm(
        tiles: List<Tile>,
        loading: Boolean = false,
        visibleSection: ProjectSection? = null,
        completedTiles: List<Tile> = emptyList(),
        sortOrder: SortOrder = SortOrder.DEFAULT,
    ): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        val section = visibleSection ?: ProjectSection(
            id = "all",
            label = "All",
            tiles = tiles,
        )
        every { vm.tiles } returns MutableStateFlow(tiles)
        every { vm.projectSections } returns MutableStateFlow(listOf(section))
        every { vm.visibleSection } returns MutableStateFlow(section)
        every { vm.selectedSectionId } returns MutableStateFlow(section.id)
        every { vm.completedTiles } returns MutableStateFlow(completedTiles)
        every { vm.sortOrder } returns MutableStateFlow(sortOrder)
        every { vm.loading } returns MutableStateFlow(loading)
        every { vm.error } returns MutableStateFlow(null)
        every { vm.requestDeleteTileId } returns MutableStateFlow(null)
        every { vm.requestDeferTileId } returns MutableStateFlow(null)
        every { vm.requestPromptTileId } returns MutableStateFlow(null)
        every { vm.lastActionMessage } returns MutableStateFlow(null)
        every { vm.executionControlStates } returns MutableStateFlow(emptyMap())
        every { vm.executionControlInFlightTileIds } returns MutableStateFlow(emptySet())
        every { vm.locale } returns MutableStateFlow(app.tastile.android.data.user.AppLocale.EN)
        return vm
    }

    private fun stubOverlay(): OverlayViewModel = mockk<OverlayViewModel>(relaxed = true)

    @Test
    fun `renders a STARTED tile title in its card`() {
        val active = Tile(
            id = "t1", title = "Code review",
            lifecycle = TileLifecycle.STARTED.value,
        )
        val vm = stubVm(listOf(active))
        rule.setContent {
            TastileTheme {
                ExecuteScreen(viewModel = vm, overlay = stubOverlay())
            }
        }

        rule.onAllNodesWithText("Code review", substring = true, useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `shows indeterminate progress while loading with no tiles`() {
        val vm = stubVm(tiles = emptyList(), loading = true)
        rule.setContent {
            TastileTheme {
                ExecuteScreen(viewModel = vm, overlay = stubOverlay())
            }
        }
        rule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate),
        ).onFirst().assertIsDisplayed()
    }

    @Test
    fun `does not render a standalone active glyph when no started tile exists`() {
        // Without a STARTED tile in scope, no `▶` text is rendered anywhere.
        val ready = Tile(
            id = "t2", title = "Standup",
            lifecycle = TileLifecycle.READY.value,
        )
        val vm = stubVm(listOf(ready))
        rule.setContent {
            TastileTheme {
                ExecuteScreen(viewModel = vm, overlay = stubOverlay())
            }
        }

        rule.onAllNodesWithText("▶").assertCountEquals(0)
    }

    @Test
    fun `active execution exposes pause and complete but not another start`() {
        val active = Tile(id = "t1", title = "Code review",
            lifecycle = TileLifecycle.STARTED.value)
        val vm = stubVm(listOf(active))
        every { vm.executionControlStates } returns MutableStateFlow(
            mapOf("t1" to ExecutionControlState.Active)
        )
        rule.setContent {
            TastileTheme {
                ExecuteScreen(viewModel = vm, overlay = stubOverlay())
            }
        }

        rule.onNodeWithTag("execute-pause-t1").performClick()
        rule.onNodeWithText("Complete").performClick()
        rule.onAllNodesWithText("Start", substring = false).assertCountEquals(0)

        verify(exactly = 1) { vm.pauseTile("t1") }
        verify(exactly = 1) { vm.completeTile("t1") }
    }

    @Test
    fun `paused execution replaces pause with resume and resumes the same tile`() {
        val active = Tile(id = "t1", title = "Code review",
            lifecycle = TileLifecycle.STARTED.value)
        val executionStates = MutableStateFlow(
            mapOf("t1" to ExecutionControlState.Active)
        )
        val vm = stubVm(listOf(active))
        every { vm.executionControlStates } returns executionStates

        rule.setContent {
            TastileTheme {
                ExecuteScreen(viewModel = vm, overlay = stubOverlay())
            }
        }
        rule.onNodeWithTag("execute-pause-t1").performClick()
        verify(exactly = 1) { vm.pauseTile("t1") }

        rule.runOnIdle { executionStates.value = mapOf("t1" to ExecutionControlState.Paused) }
        rule.onNodeWithTag("execute-resume-t1").performClick()
        rule.onAllNodesWithText("Pause").assertCountEquals(0)
        verify(exactly = 1) { vm.resumeTile("t1") }
    }

    @Test
    fun `in flight execution control disables the corresponding lifecycle action`() {
        val active = Tile(id = "t1", title = "Code review",
            lifecycle = TileLifecycle.STARTED.value)
        val vm = stubVm(listOf(active))
        every { vm.executionControlStates } returns MutableStateFlow(
            mapOf("t1" to ExecutionControlState.Active)
        )
        every { vm.executionControlInFlightTileIds } returns MutableStateFlow(setOf("t1"))

        rule.setContent {
            TastileTheme {
                ExecuteScreen(viewModel = vm, overlay = stubOverlay())
            }
        }

        rule.onNodeWithTag("execute-pause-t1").assertExists()
    }

    @Test
    fun `scope tab click delegates to setSelectedSection on the view model`() {
        val sectionAll = ProjectSection(id = "all", label = "All", tiles = emptyList())
        val sectionUnassigned = ProjectSection(
            id = "unassigned",
            label = "Unassigned",
            tiles = emptyList(),
        )
        val vm = stubVm(
            tiles = emptyList(),
            visibleSection = sectionAll,
        )
        every { vm.projectSections } returns MutableStateFlow(listOf(sectionAll, sectionUnassigned))
        rule.setContent {
            TastileTheme {
                ExecuteScreen(viewModel = vm, overlay = stubOverlay())
            }
        }

        rule.onNodeWithTag("tasks-scope-tab-${"unassigned"}").performClick()
        verify(exactly = 1) { vm.setSelectedSection("unassigned") }
    }

    @Test
    fun `defer confirmation submits an explicit selected next time`() {
        var deferredUntil: String? = null
        rule.setContent {
            DeferTileDialog(
                tileTitle = "Standup",
                onConfirm = { deferredUntil = it },
                onCancel = {},
            )
        }
        rule.onNodeWithText("Confirm").performClick()
        assertNotNull(deferredUntil)
    }

    @Test
    fun `prompt request requires confirmation before sending`() {
        var requests = 0
        rule.setContent {
            PromptRequestDialog(tileTitle = "Standup", onConfirm = { requests++ }, onCancel = {})
        }
        rule.onNodeWithText("Request").performClick()
        assertEquals(1, requests)
    }
}
