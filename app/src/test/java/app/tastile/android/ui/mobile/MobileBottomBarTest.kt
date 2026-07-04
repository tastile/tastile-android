package app.tastile.android.ui.mobile

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MobileBottomBarTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `bottom bar renders 5 slots with role Button`() {
        var timeline: String = ""
        var execute: String = ""
        var tiles: String = ""
        var quickCreate: String = ""
        var settings: String = ""

        rule.setContent {
            val context = LocalContext.current
            timeline = context.getString(R.string.mobile_bottom_timeline)
            execute = context.getString(R.string.mobile_bottom_execute)
            tiles = context.getString(R.string.mobile_bottom_tiles)
            quickCreate = context.getString(R.string.mobile_bottom_quick_create)
            settings = context.getString(R.string.mobile_bottom_settings)

            MobileBottomBar(
                currentRoute = "execute",
                onSelect = {},
                onQuickCreate = {},
            )
        }

        rule.onNodeWithContentDescription(timeline)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithContentDescription(execute)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithContentDescription(tiles)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithContentDescription(quickCreate)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithContentDescription(settings)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }
}
