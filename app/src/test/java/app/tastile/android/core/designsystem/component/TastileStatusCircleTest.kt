package app.tastile.android.core.designsystem.component

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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TastileStatusCircleTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `renders the correct glyph for each lifecycle`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileStatusCircle(lifecycle = TileLifecycle.READY)
                TastileStatusCircle(lifecycle = TileLifecycle.STARTED)
                TastileStatusCircle(lifecycle = TileLifecycle.DONE)
                TastileStatusCircle(lifecycle = TileLifecycle.ARCHIVED)
            }
        }
        composeTestRule.onNodeWithText("○").assertIsDisplayed()
        composeTestRule.onNodeWithText("▶").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
        composeTestRule.onNodeWithText("·").assertIsDisplayed()
    }

    @Test fun `click handler fires when onClick is non-null`() {
        var clicked = 0
        composeTestRule.setContent {
            TastileTheme {
                TastileStatusCircle(
                    lifecycle = TileLifecycle.READY,
                    onClick = { clicked++ },
                )
            }
        }
        composeTestRule.onNodeWithTag("tastile_status_circle").performClick()
        composeTestRule.runOnIdle { assertEquals(1, clicked) }
    }

    @Test fun `no click handler leaves the indicator non-interactive`() {
        var clicked = 0
        composeTestRule.setContent {
            TastileTheme {
                TastileStatusCircle(lifecycle = TileLifecycle.DONE)
            }
        }
        composeTestRule.onNodeWithTag("tastile_status_circle").performClick()
        composeTestRule.runOnIdle { assertEquals(0, clicked) }
    }
}
