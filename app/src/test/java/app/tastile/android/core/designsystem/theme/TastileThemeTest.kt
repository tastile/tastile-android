package app.tastile.android.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TastileThemeTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `LocalTastileShapeTokens default inside TastileTheme`() {
        composeTestRule.setContent {
            var captured: TastileShapeTokens? = null
            TastileTheme {
                captured = LocalTastileShapeTokens.current
                Text("probe", Modifier.testTag("probe"))
            }
            assertEquals(TastileShapeTokens.Default, captured)
        }
        composeTestRule.onNodeWithTag("probe").assertIsDisplayed()
    }

    @Test fun `MaterialTheme shapes medium is RoundedCornerShape 16dp inside TastileTheme`() {
        composeTestRule.setContent {
            TastileTheme {
                Text("probe", Modifier.testTag("probe"))
                assertEquals(
                    RoundedCornerShape(16.dp),
                    MaterialTheme.shapes.medium,
                )
            }
        }
        composeTestRule.onNodeWithTag("probe").assertIsDisplayed()
    }

    @Test fun `MaterialTheme colorScheme is non-null inside TastileTheme`() {
        composeTestRule.setContent {
            TastileTheme {
                Text("probe", Modifier.testTag("probe"))
                // MaterialTheme.colorScheme is non-null when TastileTheme provides one
                MaterialTheme.colorScheme.primary
            }
        }
        composeTestRule.onNodeWithTag("probe").assertIsDisplayed()
    }
}