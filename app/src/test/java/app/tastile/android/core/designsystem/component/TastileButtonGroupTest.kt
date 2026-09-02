package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.TastileTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TastileButtonGroupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `selected item exposes Role Tab and selected true`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    TastileButtonGroup(
                        items = listOf(
                            ButtonGroupItem(icon = null, label = "One"),
                            ButtonGroupItem(icon = null, label = "Two"),
                        ),
                        selectedIndex = 1,
                        onItemSelected = {},
                        size = ButtonGroupSize.M,
                    )
                }
            }
        }
        composeTestRule
            .onAllNodesWithTag("button-group-item-1", useUnmergedTree = true)
            .onFirst()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test
    fun `clicking an item invokes onItemSelected with its index`() {
        var captured = -1
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    TastileButtonGroup(
                        items = listOf(
                            ButtonGroupItem(label = "One"),
                            ButtonGroupItem(label = "Two"),
                        ),
                        selectedIndex = 0,
                        onItemSelected = { captured = it },
                        size = ButtonGroupSize.M,
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("button-group-item-1").performClick()
        assertEquals(1, captured)
    }

    @Test
    fun `M size button is 48dp tall`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    TastileButtonGroup(
                        items = listOf(ButtonGroupItem(label = "X")),
                        selectedIndex = 0,
                        onItemSelected = {},
                        size = ButtonGroupSize.M,
                    )
                }
            }
        }
        composeTestRule
            .onNodeWithTag("button-group-item-0")
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun `Xs size has minimum 48dp touch target even when height is 32dp`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    TastileButtonGroup(
                        items = listOf(ButtonGroupItem(label = "X")),
                        selectedIndex = 0,
                        onItemSelected = {},
                        size = ButtonGroupSize.Xs,
                    )
                }
            }
        }
        composeTestRule
            .onNodeWithTag("button-group-item-0-touch")
            .assertHeightIsEqualTo(48.dp)
    }
}
