package app.tastile.android.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TastileShapesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `TastileShapes populates largeIncreased with M3 Expressive 20dp spec`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalTastileShapeTokens provides TastileShapeTokens.Default,
            ) {
                val shapes = TastileShapes
                assert(shapes.largeIncreased == RoundedCornerShape(20.dp)) {
                    "Expected RoundedCornerShape(20.dp), got ${shapes.largeIncreased}"
                }
            }
            Surface {}
        }
    }

    @Test
    fun `TastileShapes populates extraLargeIncreased with M3 Expressive 32dp spec`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalTastileShapeTokens provides TastileShapeTokens.Default,
            ) {
                val shapes = TastileShapes
                assert(shapes.extraLargeIncreased == RoundedCornerShape(32.dp)) {
                    "Expected RoundedCornerShape(32.dp), got ${shapes.extraLargeIncreased}"
                }
            }
            Surface {}
        }
    }

    @Test
    fun `TastileShapes populates extraExtraLarge with M3 Expressive 48dp spec`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalTastileShapeTokens provides TastileShapeTokens.Default,
            ) {
                val shapes = TastileShapes
                assert(shapes.extraExtraLarge == RoundedCornerShape(48.dp)) {
                    "Expected RoundedCornerShape(48.dp), got ${shapes.extraExtraLarge}"
                }
            }
            Surface {}
        }
    }
}
