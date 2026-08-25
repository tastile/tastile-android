package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.data.model.TileLifecycle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TastileTileCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `expanded false hides expandedContent`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileTileCard(
                    title = "Sample",
                    lifecycle = TileLifecycle.READY,
                    expanded = false,
                    onToggleExpanded = {},
                    expandedContent = {
                        Box(Modifier.testTag("expanded_body")) { Text("body") }
                    },
                )
            }
        }
        composeTestRule.onAllNodesWithTag("expanded_body").assertCountEquals(0)
    }

    @Test fun `expanded true shows expandedContent`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileTileCard(
                    title = "Sample",
                    lifecycle = TileLifecycle.READY,
                    expanded = true,
                    onToggleExpanded = {},
                    expandedContent = {
                        Box(Modifier.testTag("expanded_body")) { Text("body") }
                    },
                )
            }
        }
        composeTestRule.onNodeWithTag("expanded_body").assertIsDisplayed()
    }

    @Test fun `clicking the header fires onToggleExpanded`() {
        var toggles = 0
        composeTestRule.setContent {
            TastileTheme {
                TastileTileCard(
                    title = "Sample",
                    lifecycle = TileLifecycle.READY,
                    expanded = false,
                    onToggleExpanded = { toggles++ },
                )
            }
        }
        composeTestRule.onNodeWithText("Sample").performClick()
        composeTestRule.runOnIdle { assertEquals(1, toggles) }
    }
}