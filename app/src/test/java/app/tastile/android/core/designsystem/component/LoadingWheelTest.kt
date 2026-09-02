package app.tastile.android.core.designsystem.component

import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.tastile.android.core.designsystem.theme.TastileTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoadingWheelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `NiaLoadingWheel exposes contentDescription`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface { NiaLoadingWheel(contentDesc = "loading-data") }
            }
        }
        composeTestRule
            .onNodeWithTag("loadingWheel")
            .assertContentDescriptionEquals("loading-data")
    }

    @Test
    fun `NiaLoadingWheel reports indeterminate progress`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface { NiaLoadingWheel(contentDesc = "loading-data") }
            }
        }
        composeTestRule
            .onNodeWithTag("loadingWheel")
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            ))
    }

    @Test
    fun `NiaOverlayLoadingWheel renders with contentDescription`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface { NiaOverlayLoadingWheel(contentDesc = "overlay-loading") }
            }
        }
        composeTestRule
            .onNodeWithTag("loadingWheel", useUnmergedTree = true)
            .assertContentDescriptionEquals("overlay-loading")
    }
}
