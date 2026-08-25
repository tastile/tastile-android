package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.tastile.android.core.designsystem.theme.TastileTheme
import org.junit.Rule
import org.junit.Test

class TastileDashboardCardShellTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `renders header and content slots`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileDashboardCardShell(
                    header = {
                        Box(Modifier.testTag("shell_header")) { Text("Header") }
                    },
                    content = {
                        Box(Modifier.testTag("shell_content")) { Text("Content") }
                    },
                )
            }
        }
        composeTestRule.onNodeWithTag("shell_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shell_content").assertIsDisplayed()
    }
}