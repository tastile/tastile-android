package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TaskBucket
import app.tastile.android.ui.dashboard.TaskBucketGroup
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose-level coverage for the mobile Tasks tab grouping.
 *
 * `ExecuteScreen.kt` reads the [DashboardViewModel.tasksByBucket] derivation
 * (added alongside the rewrite). These tests stub the derivation explicitly
 * so the screen renders a deterministic list of bucket headers + rows.
 *
 * They live alongside `ExecuteScreenTest.kt` rather than replacing it:
 * that file still exercises the legacy active-tile hero paths and
 * execution-control plumbing, which the new screen shares.
 */
@RunWith(AndroidJUnit4::class)
class TasksScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun stubVm(
        tiles: List<Tile> = emptyList(),
        buckets: List<TaskBucketGroup> = emptyList(),
    ): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.tiles } returns MutableStateFlow(tiles)
        every { vm.tasksByBucket } returns MutableStateFlow(buckets)
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
    fun `renders bucket header for each non-empty group in derivation order`() {
        val todayTile = Tile(id = "today-1", title = "Today tile", lifecycle = TileLifecycle.READY.value)
        val weekTile = Tile(id = "week-1", title = "Week tile", lifecycle = TileLifecycle.READY.value)
        val laterTile = Tile(id = "later-1", title = "Later tile", lifecycle = TileLifecycle.READY.value)
        val noDateTile = Tile(id = "nodate-1", title = "No date tile", lifecycle = TileLifecycle.READY.value)
        val vm = stubVm(
            tiles = listOf(todayTile, weekTile, laterTile, noDateTile),
            buckets = TaskBucket.entries.map { bucket ->
                val tiles = when (bucket) {
                    TaskBucket.TODAY -> listOf(todayTile)
                    TaskBucket.THIS_WEEK -> listOf(weekTile)
                    TaskBucket.LATER -> listOf(laterTile)
                    TaskBucket.NO_DATE -> listOf(noDateTile)
                }
                TaskBucketGroup(bucket = bucket, tiles = tiles)
            },
        )

        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        TaskBucket.entries.forEach { bucket ->
            rule.onNodeWithTag("tasks-bucket-header-${bucket.groupId}").assertIsDisplayed()
        }
        rule.onNodeWithTag("tasks-list").performScrollToIndex(1)
        rule.onNodeWithTag("execute-tile-today-1").assertIsDisplayed()
        rule.onNodeWithTag("tasks-list").performScrollToIndex(3)
        rule.onNodeWithTag("execute-tile-week-1").assertIsDisplayed()
        rule.onNodeWithTag("tasks-list").performScrollToIndex(5)
        rule.onNodeWithTag("execute-tile-later-1").assertIsDisplayed()
        rule.onNodeWithTag("tasks-list").performScrollToIndex(7)
        rule.onNodeWithTag("execute-tile-nodate-1").assertIsDisplayed()
    }

    @Test
    fun `skips bucket headers when derivation returns no groups`() {
        val vm = stubVm(tiles = emptyList(), buckets = emptyList())

        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        TaskBucket.entries.forEach { bucket ->
            rule.onNodeWithTag("tasks-bucket-header-${bucket.groupId}").assertDoesNotExist()
        }
    }

    @Test
    fun `renders only the buckets the derivation emits`() {
        val todayTile = Tile(id = "today-1", title = "Today only", lifecycle = TileLifecycle.READY.value)
        val vm = stubVm(
            tiles = listOf(todayTile),
            buckets = listOf(TaskBucketGroup(bucket = TaskBucket.TODAY, tiles = listOf(todayTile))),
        )

        rule.setContent { ExecuteScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onNodeWithTag("tasks-bucket-header-today").assertIsDisplayed()
        rule.onNodeWithTag("tasks-bucket-header-this_week").assertDoesNotExist()
        rule.onNodeWithTag("tasks-bucket-header-later").assertDoesNotExist()
        rule.onNodeWithTag("tasks-bucket-header-no_date").assertDoesNotExist()
        rule.onAllNodesWithTag("execute-tile-today-1").assertCountEquals(1)
    }
}
