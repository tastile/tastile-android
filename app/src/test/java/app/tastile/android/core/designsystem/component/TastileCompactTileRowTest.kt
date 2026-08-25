package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.data.model.TileLifecycle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TastileCompactTileRowTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `renders title and status glyph`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileCompactTileRow(
                    title = "Read inbox",
                    lifecycle = TileLifecycle.READY,
                )
            }
        }
        composeTestRule.onNodeWithText("Read inbox").assertIsDisplayed()
        composeTestRule.onNodeWithText("○").assertIsDisplayed()
    }

    @Test fun `click handler fires when onClick provided`() {
        var clicked = 0
        composeTestRule.setContent {
            TastileTheme {
                TastileCompactTileRow(
                    title = "Read inbox",
                    lifecycle = TileLifecycle.READY,
                    onClick = { clicked++ },
                    modifier = Modifier.testTag("compact_tile_row"),
                )
            }
        }
        composeTestRule.onNodeWithTag("compact_tile_row").performClick()
        composeTestRule.runOnIdle { assertEquals(1, clicked) }
    }

    @Test fun `trailing slot renders when supplied`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileCompactTileRow(
                    title = "Read inbox",
                    lifecycle = TileLifecycle.READY,
                    trailing = { Box(Modifier.testTag("trailing_slot")) },
                )
            }
        }
        composeTestRule.onNodeWithTag("trailing_slot").assertIsDisplayed()
    }
}
