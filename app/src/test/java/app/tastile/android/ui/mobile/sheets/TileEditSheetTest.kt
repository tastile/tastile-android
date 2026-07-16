package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Profile
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.ReferenceOverlayStore
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.TilesResponse
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.ExecutionControlState
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TileEditSheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val viewModels = mutableListOf<DashboardViewModel>()
    private val tileRepositories = mutableListOf<TileRepository>()

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
        tileRepositories.clear()
    }

    private fun newDashboardViewModel(): DashboardViewModel {
        val authRepo = mockk<AuthRepository>(relaxed = true)
        val profileRepo = mockk<ProfileRepository>(relaxed = true)
        val tileRepo = mockk<TileRepository>(relaxed = true)
        tileRepositories.add(tileRepo)
        val userSettingsRepo = mockk<UserSettingsRepository>(relaxed = true)
        val referenceOverlayStore = mockk<ReferenceOverlayStore>(relaxed = true)
        every { userSettingsRepo.getLocale() } returns AppLocale.EN
        // Unauthenticated keeps refreshAll in the empty-state branch so it never
        // touches the mock repositories; refreshTimeline (fired by the
        // selectedDay/scale combine) still runs, so stub getTimeline explicitly.
        every { authRepo.authState } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        coEvery { tileRepo.getTimeline(any(), any()) } returns emptyList()
        coEvery { tileRepo.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        coEvery { profileRepo.getProfile(any()) } returns Profile(id = "user-1")
        return DashboardViewModel(
            authRepository = authRepo,
            profileRepository = profileRepo,
            tileRepository = tileRepo,
            userSettingsRepository = userSettingsRepo,
            referenceOverlayStore = referenceOverlayStore,
        ).also { viewModels.add(it) }
    }

    @Test
    fun `TileEditSheet renders tile title for the requested id`() {
        val overlay = OverlayViewModel()
        val vm = newDashboardViewModel()
        val tile = Tile(id = "abc", title = "Write spec", lifecycle = "Ready")
        vm.replaceTilesForTest(listOf(tile))
        vm.selectTile("abc")

        rule.setContent {
            TileEditSheet(overlay = overlay, viewModel = vm)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Write spec").assertDoesNotExist()

        rule.runOnUiThread {
            overlay.show(Overlay.TileEdit(tileId = "abc"))
        }
        rule.waitForIdle()
        rule.onAllNodesWithText("Write spec").assertCountEquals(2)
    }

    @Test
    fun `TileEditSheet does not render content when overlay is Hidden`() {
        val overlay = OverlayViewModel() // starts Hidden
        val vm = newDashboardViewModel()
        val tile = Tile(id = "abc", title = "Write spec", lifecycle = "Ready")
        vm.replaceTilesForTest(listOf(tile))
        vm.selectTile("abc")

        rule.setContent {
            TileEditSheet(overlay = overlay, viewModel = vm)
        }

        rule.onNodeWithText("Write spec").assertDoesNotExist()
    }

    @Test
    fun `TileEditSheet composes delete confirmation for its selected tile`() {
        val overlay = OverlayViewModel()
        val vm = newDashboardViewModel()
        vm.replaceTilesForTest(listOf(Tile(id = "abc", title = "Write spec", lifecycle = "Ready")))
        vm.selectTile("abc")

        rule.setContent { TileEditSheet(overlay = overlay, viewModel = vm) }
        rule.runOnUiThread { overlay.show(Overlay.TileEdit(tileId = "abc")) }
        rule.waitForIdle()
        rule.runOnUiThread { vm.setDeleteTileCandidate("abc") }
        rule.waitForIdle()

        rule.onNodeWithTag("tiles-delete-dialog").assertIsDisplayed()
    }

    @Test
    fun `TileEditSheet saves a prefilled title through update flow rather than Quick Create`() {
        val overlay = OverlayViewModel()
        val vm = newDashboardViewModel()
        vm.replaceTilesForTest(listOf(Tile(id = "abc", title = "Write spec", lifecycle = "Ready")))
        vm.selectTile("abc")

        rule.setContent { TileEditSheet(overlay = overlay, viewModel = vm) }
        rule.runOnUiThread { overlay.show(Overlay.TileEdit(tileId = "abc")) }
        rule.waitForIdle()

        rule.onAllNodesWithTag("tile-edit-save-details").assertCountEquals(1)
        rule.onNodeWithText("Quick Create").assertDoesNotExist()
    }

    @Test
    fun `TileEditSheet closing a calendar occurrence closes only its placement`() {
        val overlay = OverlayViewModel()
        val vm = newDashboardViewModel()
        vm.replaceTilesForTest(listOf(Tile(id = "tile-1", title = "Standup", lifecycle = "Ready")))
        vm.selectTile("tile-1")

        rule.setContent { TileEditSheet(overlay = overlay, viewModel = vm) }
        rule.runOnUiThread { overlay.show(Overlay.TileEdit(tileId = "tile-1", placementId = "placement-1")) }
        rule.waitForIdle()

        rule.onNodeWithText("Delete occurrence").performSemanticsAction(SemanticsActions.OnClick) { action ->
            action?.invoke()
        }
        rule.waitForIdle()
        rule.onNodeWithText("Delete occurrence?").assertIsDisplayed()
        rule.onNodeWithText("Delete").performClick()
        rule.waitForIdle()

        coVerify(exactly = 1) { tileRepositories.last().closePlacement("placement-1") }
        coVerify(exactly = 0) { tileRepositories.last().deleteTile(any()) }
    }

    @Test
    fun `TileEditSheet started tile shows Pause instead of Resume`() {
        val overlay = OverlayViewModel()
        val vm = newDashboardViewModel()
        vm.replaceTilesForTest(listOf(Tile(id = "tile-1", title = "Focus", lifecycle = "Started")))
        vm.replaceExecutionControlStatesForTest(mapOf("tile-1" to ExecutionControlState.Active))
        vm.selectTile("tile-1")

        rule.setContent { TileEditSheet(overlay = overlay, viewModel = vm) }
        rule.runOnUiThread { overlay.show(Overlay.TileEdit(tileId = "tile-1")) }
        rule.waitForIdle()

        rule.onNodeWithText("Pause").assertIsDisplayed()
        rule.onNodeWithText("Resume").assertDoesNotExist()
    }
}
