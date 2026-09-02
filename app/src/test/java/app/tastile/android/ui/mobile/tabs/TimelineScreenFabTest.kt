package app.tastile.android.ui.mobile.tabs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TimelineScreenFabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun stubVm(): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.timeline } returns MutableStateFlow<List<CoreTimelineItem>>(emptyList())
        every { vm.selectedDay } returns MutableStateFlow(LocalDate.now())
        every { vm.scale } returns MutableStateFlow(TimelineScale.Day)
        return vm
    }

    @Test
    fun `fab click shows Overlay QuickCreate`() {
        val viewModel = stubVm()
        val overlay: OverlayViewModel = mockk(relaxed = true)

        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimelineScreen(viewModel = viewModel, overlay = overlay)
                }
            }
        }
        composeTestRule.onNodeWithTag("quick-create-fab").performClick()
        verify { overlay.show(Overlay.QuickCreate) }
    }

    @Test
    fun `quick-create-fab is rendered`() {
        val viewModel = stubVm()
        val overlay: OverlayViewModel = mockk(relaxed = true)

        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimelineScreen(viewModel = viewModel, overlay = overlay)
                }
            }
        }
        composeTestRule.onNodeWithTag("quick-create-fab").assertIsDisplayed()
    }
}
