package app.tastile.android.ui.mobile.sheets

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.data.access.AccessRepository
import app.tastile.android.data.api.AggregateRef
import app.tastile.android.data.api.CommandResponse
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.auth.AuthRepository
import app.tastile.android.data.auth.TastileAuthState
import app.tastile.android.data.model.Profile
import app.tastile.android.data.repository.TilesResponse
import app.tastile.android.data.tile.TileRepository
import app.tastile.android.data.user.AppLocale
import app.tastile.android.data.user.ProfileRepository
import app.tastile.android.data.user.UserSettingsRepository
import app.tastile.android.data.workspace.ReferenceOverlayStore
import app.tastile.android.data.workspace.WorkspaceRepository
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.panels.ProjectsViewModel
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSubmissionViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Canary for the 2026-09-15 signed-AAB failure: QuickCreate Create button
 * renders `enabled=true / clickable=true`, yet physical pointer input never
 * fires `onClick` (no POST), while the same sheet's close(X) works.
 *
 * Production wiring under test (see [QuickCreateSheetMobile]):
 * `real Composable -> real ViewModel -> real dispatcher -> V1ApiClient boundary`.
 * Only the network client is faked. The ViewModel itself is real on purpose:
 * mocking it would hide exactly this class of wiring failure.
 *
 * Why two click tests:
 * - `semanticsClick_submitsWrite` drives `performClick()` (semantics action —
 *   bypasses pointer routing / gesture arbitration entirely).
 * - `pointerClick_submitsWrite` drives `performTouchInput { click() }`
 *   (Compose pointer path, so `dragHandle` drag-vs-tap arbitration is in play).
 *
 * Honest limitation (verified 2026-09-15): on JVM/Robolectric BOTH tests go
 * green even with the suspect `ModalBottomSheet(dragHandle = { ... Button })
 * ` structure intact, because Robolectric does not reproduce the device's
 * MotionEvent / anchored-drag gesture arbitration. The true RED for this
 * failure mode requires an emulator test that sends platform-level pointer
 * input (UiAutomator click at the node's bounds) — see the proposed
 * `QuickCreateCreateJourneyTest` in `src/androidTest` (Q3/Q6 in the strategy
 * report). These two JVM tests pin the wiring below the gesture layer so that
 * once the emulator test exists, a divergence between them is itself evidence
 * pointing at the gesture layer rather than the ViewModel/dispatcher.
 */
@RunWith(AndroidJUnit4::class)
class QuickCreateCreateJourneyCanaryTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val viewModels = mutableListOf<ViewModel>()

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
    }

    private fun successResponse(id: String = "tile-1") = CommandResponse(
        commandId = "cmd",
        acceptedAt = "2026-09-15T00:00:00Z",
        aggregate = AggregateRef(1, id),
        aggregateMeta = null,
        result = 1,
    )

    private data class Fixture(
        val overlay: OverlayViewModel,
        val submissionVm: QuickCreateSubmissionViewModel,
        val client: V1ApiClient,
    )

    private fun setUp(): Fixture {
        val client = mockk<V1ApiClient>(relaxed = true)
        coEvery { client.createSourceTile(any()) } returns successResponse()

        val authRepo = mockk<AuthRepository>(relaxed = true)
        val accessRepo = mockk<AccessRepository>(relaxed = true)
        val profileRepo = mockk<ProfileRepository>(relaxed = true)
        val tileRepo = mockk<TileRepository>(relaxed = true)
        val userSettingsRepo = mockk<UserSettingsRepository>(relaxed = true)
        val referenceOverlayStore = mockk<ReferenceOverlayStore>(relaxed = true)
        every { userSettingsRepo.getLocale() } returns AppLocale.EN
        every { authRepo.authState } returns MutableStateFlow(TastileAuthState.Unauthenticated)
        coEvery { tileRepo.getTimeline(any(), any()) } returns emptyList()
        coEvery { tileRepo.getTiles(any()) } returns TilesResponse(emptyList(), null, null)
        coEvery { profileRepo.getProfile(any()) } returns Profile(id = "user-1")
        val dashboardVm = DashboardViewModel(
            authRepository = authRepo,
            accessRepository = accessRepo,
            profileRepository = profileRepo,
            tileRepository = tileRepo,
            userSettingsRepository = userSettingsRepo,
            referenceOverlayStore = referenceOverlayStore,
        ).also { viewModels.add(it) }

        val workspaceRepo = mockk<WorkspaceRepository>()
        coEvery { workspaceRepo.list() } returns emptyList()
        val projectsVm = ProjectsViewModel(workspaceRepo, mockk<Context>(relaxed = true))
            .also { viewModels.add(it) }

        val submissionVm = QuickCreateSubmissionViewModel(client)
            .also { viewModels.add(it) }
        val overlay = OverlayViewModel()

        rule.setContent {
            TastileTheme {
                QuickCreateSheetMobile(
                    overlay = overlay,
                    dashboardViewModel = dashboardVm,
                    projectsViewModel = projectsVm,
                    submissionViewModel = submissionVm,
                )
            }
        }
        rule.runOnUiThread { overlay.show(Overlay.QuickCreate) }
        rule.waitForIdle()
        rule.onNodeWithTag("quick-create-handle-submit").assertIsDisplayed()
        return Fixture(overlay, submissionVm, client)
    }

    private fun enterTitleMakingCreateValid() {
        // The base sheet seeds a valid next-15-min span via
        // QuickCreateStateStore.openCreate(Event); a non-blank title is the
        // remaining precondition for validation to pass and Create to enable.
        rule.onNodeWithTag("quick-create-title").performTextInput("Canary tile")
        rule.waitForIdle()
        rule.onNodeWithTag("quick-create-handle-submit").assertIsEnabled()
    }

    private fun awaitSubmission(fixture: Fixture) {
        rule.waitUntil(timeoutMillis = 10_000) {
            fixture.submissionVm.state.value.createdTileId != null ||
                fixture.submissionVm.state.value.error != null
        }
    }

    @Test
    fun semanticsClick_submitsWrite() {
        val fixture = setUp()
        enterTitleMakingCreateValid()

        rule.onNodeWithTag("quick-create-handle-submit").performClick()
        awaitSubmission(fixture)

        coVerify(exactly = 1) { fixture.client.createSourceTile(any()) }
    }

    @Test
    fun pointerClick_submitsWrite() {
        val fixture = setUp()
        enterTitleMakingCreateValid()

        // Compose pointer path (not the semantics shortcut): if the sheet's
        // drag-handle gesture claims the tap before the Button's press
        // detector, this is where submission silently never fires on device.
        rule.onNodeWithTag("quick-create-handle-submit").performTouchInput { click() }
        awaitSubmission(fixture)

        coVerify(exactly = 1) { fixture.client.createSourceTile(any()) }
    }
}
