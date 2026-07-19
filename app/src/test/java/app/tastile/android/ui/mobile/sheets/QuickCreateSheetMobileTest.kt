package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Profile
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.ProfileRepository
import app.tastile.android.data.repository.ReferenceOverlayStore
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.data.repository.TileRepository
import app.tastile.android.data.repository.TilesResponse
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.repository.WorkspaceRepository
import app.tastile.android.ui.mobile.panels.ProjectsViewModel
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSubmissionViewModel
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickCreateSheetMobileTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val viewModels = mutableListOf<ViewModel>()

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
    }

    private fun newProjectsViewModel(): ProjectsViewModel {
        val workspaceRepository = mockk<WorkspaceRepository>()
        coEvery { workspaceRepository.list() } returns emptyList()
        return ProjectsViewModel(workspaceRepository).also { viewModels.add(it) }
    }

    private fun newSubmissionViewModel(): QuickCreateSubmissionViewModel =
        QuickCreateSubmissionViewModel(mockk<V1ApiClient>(relaxed = true))
            .also { viewModels.add(it) }

    private fun newDashboardViewModel(): DashboardViewModel {
        val authRepo = mockk<AuthRepository>(relaxed = true)
        val profileRepo = mockk<ProfileRepository>(relaxed = true)
        val tileRepo = mockk<TileRepository>(relaxed = true)
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
    fun `QuickCreateSheetMobile shows Quick Create sheet when overlay is QuickCreate`() {
        val overlay = OverlayViewModel()
        val vm = newDashboardViewModel()
        val projectsVm = newProjectsViewModel()
        val submissionVm = newSubmissionViewModel()

        rule.setContent {
            QuickCreateSheetMobile(
                overlay = overlay,
                dashboardViewModel = vm,
                projectsViewModel = projectsVm,
                submissionViewModel = submissionVm,
            )
        }
        rule.waitForIdle()
        // The submit button (Create) and the close button are part of the
        // header chrome; before the sheet opens they must not exist.
        rule.onAllNodesWithTag("quick-create-handle-submit").assertCountEquals(0)
        rule.onAllNodesWithTag("quick-create-close").assertCountEquals(0)

        rule.runOnUiThread {
            overlay.show(Overlay.QuickCreate)
        }
        rule.waitForIdle()
        // After opening, the sheet chrome is visible: Close icon on the left,
        // Create submit on the right. (The legacy "Quick Create" title text
        // was removed when the sheet header was redesigned to icon-only.)
        rule.onNodeWithTag("quick-create-handle-submit").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-close").assertIsDisplayed()
    }

    @Test
    fun `QuickCreateSheetMobile does not show Quick Create when overlay is Hidden`() {
        val overlay = OverlayViewModel() // starts Hidden
        val dashboardVm = newDashboardViewModel()
        val projectsVm = newProjectsViewModel()
        val submissionVm = newSubmissionViewModel()

        rule.setContent {
            QuickCreateSheetMobile(
                overlay = overlay,
                dashboardViewModel = dashboardVm,
                projectsViewModel = projectsVm,
                submissionViewModel = submissionVm,
            )
        }

        // Sheet chrome (close + submit) is only rendered while the overlay is
        // showing QuickCreate; with the default Hidden overlay both must be absent.
        rule.onAllNodesWithTag("quick-create-handle-submit").assertCountEquals(0)
        rule.onAllNodesWithTag("quick-create-close").assertCountEquals(0)
    }
}
