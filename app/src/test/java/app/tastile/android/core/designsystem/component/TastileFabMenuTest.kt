package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import app.tastile.android.core.designsystem.theme.TastileTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TastileFabMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `main fab is visible at all times with contentDescription equal to mainLabel`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TastileFabMenu(
                        mainIcon = Icons.Outlined.Add,
                        mainLabel = "Add",
                        expanded = false,
                        onExpandedChange = {},
                        items = listOf(
                            FabMenuItem.Action(Icons.Outlined.Add, "Item A", onClick = {}),
                        ),
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()
    }

    @Test
    fun `menu items render with tag fab-menu-item-N when expanded`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TastileFabMenu(
                        mainIcon = Icons.Outlined.Add,
                        mainLabel = "Add",
                        expanded = true,
                        onExpandedChange = {},
                        items = listOf(
                            FabMenuItem.Action(Icons.Outlined.Add, "Item A", onClick = {}),
                            FabMenuItem.Action(Icons.Outlined.Add, "Item B", onClick = {}),
                        ),
                    )
                }
            }
        }
        composeTestRule
            .onAllNodesWithTag("fab-menu-item-0", useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag("fab-menu-item-1", useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `clicking an item invokes its onClick`() {
        var clicks = 0
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TastileFabMenu(
                        mainIcon = Icons.Outlined.Add,
                        mainLabel = "Add",
                        expanded = true,
                        onExpandedChange = {},
                        items = listOf(
                            FabMenuItem.Action(Icons.Outlined.Add, "Item A", onClick = { clicks += 1 }),
                        ),
                    )
                }
            }
        }
        composeTestRule
            .onAllNodesWithTag("fab-menu-item-0", useUnmergedTree = true)
            .onFirst()
            .performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `main fab click invokes onExpandedChange with true when collapsed`() {
        var lastExpanded: Boolean? = null
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TastileFabMenu(
                        mainIcon = Icons.Outlined.Add,
                        mainLabel = "Add",
                        expanded = false,
                        onExpandedChange = { lastExpanded = it },
                        items = listOf(
                            FabMenuItem.Action(Icons.Outlined.Add, "Item A", onClick = {}),
                        ),
                    )
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        assertEquals(true, lastExpanded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty items throws IllegalArgumentException`() {
        composeTestRule.setContent {
            TastileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TastileFabMenu(
                        mainIcon = Icons.Outlined.Add,
                        mainLabel = "Add",
                        expanded = false,
                        onExpandedChange = {},
                        items = emptyList(),
                    )
                }
            }
        }
    }
}
