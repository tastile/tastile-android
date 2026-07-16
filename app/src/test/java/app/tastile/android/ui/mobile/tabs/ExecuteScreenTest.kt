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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExecuteScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun stubVm(
        tiles: List<Tile>,
        loading: Boolean = false,
    ): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.tiles } returns MutableStateFlow(tiles)
        every { vm.loading } returns MutableStateFlow(loading)
        every { vm.error } returns MutableStateFlow(null)
        every { vm.locale } returns MutableStateFlow(app.tastile.android.data.repository.AppLocale.EN)
        return vm
    }

    private fun stubOverlay(): OverlayViewModel = mockk<OverlayViewModel>(relaxed = true)

    @Test
    fun `renders active tile title when one exists`() {
        val active = Tile(
            id = "t1",
            title = "Code review",
            lifecycle = TileLifecycle.STARTED.value,
        )
        val vm = stubVm(listOf(active))

        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }
        rule.onAllNodesWithText("Code review", substring = true, useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `shows indeterminate progress while loading with no tiles`() {
        val vm = stubVm(tiles = emptyList(), loading = true)

        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }
        rule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate),
        )
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `does not render active section when no started tile exists`() {
        val ready = Tile(
            id = "t2",
            title = "Standup",
            lifecycle = TileLifecycle.READY.value,
        )
        val vm = stubVm(listOf(ready))

        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }
        rule.onAllNodesWithText("▶").assertCountEquals(0)
    }

    @Test
    fun `active execution exposes pause and complete but not another start`() {
        val active = Tile(id = "t1", title = "Code review", lifecycle = TileLifecycle.STARTED.value)
        val vm = stubVm(listOf(active))

        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onNodeWithText("Pause").performClick()
        rule.onNodeWithText("Complete").performClick()
        rule.onAllNodesWithText("Start").assertCountEquals(0)

        verify(exactly = 1) { vm.pauseTile("t1") }
        verify(exactly = 1) { vm.completeTile("t1") }
    }
}
