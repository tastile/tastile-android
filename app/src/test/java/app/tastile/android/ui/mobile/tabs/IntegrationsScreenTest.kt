package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.ui.dashboard.DashboardViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntegrationsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun stubVm(): DashboardViewModel = mockk<DashboardViewModel>(relaxed = true)

    @Test
    fun `screen renders title sub-title and Google Calendar scope notice`() {
        rule.setContent { IntegrationsScreen(viewModel = stubVm()) }

        rule.onAllNodesWithText("Integrations", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Manage Google Calendar connections and sync.")
            .onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Google Calendar", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("This integration is outside the current Tastile v1 scope.")
            .onFirst().assertIsDisplayed()
    }
}
