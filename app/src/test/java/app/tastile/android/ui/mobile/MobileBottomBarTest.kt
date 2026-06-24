package app.tastile.android.ui.mobile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import app.tastile.android.ui.mobile.designsystem.MobileTokens
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class MobileBottomBarTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `bottom bar renders 5 slots with role Button`() {
        rule.setContent {
            MobileBottomBar(
                currentRoute = "execute",
                onSelect = {},
                onQuickCreate = {},
            )
        }
        rule.onNodeWithContentDescription("Execute").assertIsDisplayed()
        rule.onNodeWithContentDescription("Tiles").assertIsDisplayed()
        rule.onNodeWithContentDescription("Quick create").assertIsDisplayed()
        rule.onNodeWithContentDescription("Integrations").assertIsDisplayed()
        rule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }
}