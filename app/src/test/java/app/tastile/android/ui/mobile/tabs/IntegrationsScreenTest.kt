package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Integration
import app.tastile.android.ui.dashboard.DashboardViewModel
import io.mockk.every
import io.mockk.mockk
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

    @Test
    fun `connected integration renders with name and filled glyph`() {
        val item = Integration(id = "google_calendar", name = "Calendar", connected = true)
        rule.setContent { IntegrationsScreen(viewModel = stubVm(listOf(item))) }

        rule.onAllNodesWithText("Calendar", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("●", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `available integration renders with empty glyph and plus action`() {
        val item = Integration(id = "slack", name = "Slack", connected = false)
        rule.setContent { IntegrationsScreen(viewModel = stubVm(listOf(item))) }

        rule.onAllNodesWithText("Slack", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("○", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("+", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `loading with no integrations shows indeterminate progress`() {
        val vm = stubVm(integrations = emptyList())
        every { vm.loading } returns MutableStateFlow(true)
        rule.setContent { IntegrationsScreen(viewModel = vm) }

        rule.onAllNodes(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            ),
        ).onFirst().assertIsDisplayed()
    }
}