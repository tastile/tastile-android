package app.tastile.android.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.tastile.android.core.designsystem.theme.TastileTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TastileCardActionRowTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `Ready branch exposes Start and Delete buttons`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileCardActionRow(
                    actions = TastileCardActions.Ready,
                    onStart = {},
                    onComplete = {},
                    onDefer = {},
                    onDelete = {},
                    startLabel = { Text("Start") },
                    completeLabel = { Text("Complete") },
                    deferLabel = { Text("Defer") },
                    deleteLabel = { Text("Delete") },
                )
            }
        }
        composeTestRule.onNodeWithTag("card_action_start").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_action_delete").assertIsDisplayed()
    }

    @Test fun `Started branch exposes Complete and Defer buttons`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileCardActionRow(
                    actions = TastileCardActions.Started,
                    onStart = {},
                    onComplete = {},
                    onDefer = {},
                    onDelete = {},
                    startLabel = { Text("Start") },
                    completeLabel = { Text("Complete") },
                    deferLabel = { Text("Defer") },
                    deleteLabel = { Text("Delete") },
                )
            }
        }
        composeTestRule.onNodeWithTag("card_action_complete").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_action_defer").assertIsDisplayed()
    }

    @Test fun `DoneOrArchived branch exposes only Delete`() {
        composeTestRule.setContent {
            TastileTheme {
                TastileCardActionRow(
                    actions = TastileCardActions.DoneOrArchived,
                    onStart = {},
                    onComplete = {},
                    onDefer = {},
                    onDelete = {},
                    startLabel = { Text("Start") },
                    completeLabel = { Text("Complete") },
                    deferLabel = { Text("Defer") },
                    deleteLabel = { Text("Delete") },
                )
            }
        }
        composeTestRule.onNodeWithTag("card_action_delete").assertIsDisplayed()
    }

    @Test fun `clicking the Delete button on Ready fires onDelete`() {
        var deletes = 0
        composeTestRule.setContent {
            TastileTheme {
                TastileCardActionRow(
                    actions = TastileCardActions.Ready,
                    onStart = {},
                    onComplete = {},
                    onDefer = {},
                    onDelete = { deletes++ },
                    startLabel = { Text("Start") },
                    completeLabel = { Text("Complete") },
                    deferLabel = { Text("Defer") },
                    deleteLabel = { Text("Delete") },
                )
            }
        }
        composeTestRule.onNodeWithTag("card_action_delete").performClick()
        composeTestRule.runOnIdle { assertEquals(1, deletes) }
    }
}
