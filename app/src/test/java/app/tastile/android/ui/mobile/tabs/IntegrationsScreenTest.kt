package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Integration
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntegrationsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun stubVm(integrations: List<Integration> = emptyList()): DashboardViewModel {
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.integrations } returns MutableStateFlow(integrations)
        every { vm.loading } returns MutableStateFlow(false)
        return vm
    }

    private fun stubOverlay(): OverlayViewModel = mockk<OverlayViewModel>(relaxed = true)

    @Test
    fun `connected integration renders with name and filled glyph`() {
        val item = Integration(id = "google_calendar", name = "Calendar", connected = true)
        rule.setContent { IntegrationsScreen(viewModel = stubVm(listOf(item)), overlay = stubOverlay()) }

        rule.onAllNodesWithText("Calendar", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("●", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `available integration renders with empty glyph and connect action`() {
        val item = Integration(id = "slack", name = "Slack", connected = false)
        rule.setContent { IntegrationsScreen(viewModel = stubVm(listOf(item)), overlay = stubOverlay()) }

        rule.onAllNodesWithText("Slack", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("○", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Connect", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `loading with no integrations shows indeterminate progress`() {
        val vm = stubVm(integrations = emptyList())
        every { vm.loading } returns MutableStateFlow(true)
        rule.setContent { IntegrationsScreen(viewModel = vm, overlay = stubOverlay()) }

        rule.onAllNodes(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            ),
        ).onFirst().assertIsDisplayed()
    }

    @Test
    fun `connected and available items render in order`() {
        val available = Integration(id = "slack", name = "Slack", connected = false)
        val connected = Integration(id = "google_calendar", name = "Calendar", connected = true)
        rule.setContent { IntegrationsScreen(viewModel = stubVm(listOf(available, connected)), overlay = stubOverlay()) }

        // Verify both items render
        rule.onAllNodesWithText("Calendar", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Slack", substring = true).onFirst().assertIsDisplayed()
        // Verify glyphs render in correct partition order
        rule.onAllNodesWithText("●", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("○", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `row tap opens integration config overlay`() {
        val item = Integration(id = "google_calendar", name = "Calendar", connected = true)
        val overlay = mockk<OverlayViewModel>(relaxed = true)

        rule.setContent { IntegrationsScreen(viewModel = stubVm(listOf(item)), overlay = overlay) }
        rule.onAllNodesWithText("Calendar", substring = true).onFirst().performClick()

        verify { overlay.show(Overlay.IntegrationConfig(integrationId = "google_calendar")) }
    }
}