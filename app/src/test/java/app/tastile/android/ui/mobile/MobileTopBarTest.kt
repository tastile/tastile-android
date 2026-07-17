package app.tastile.android.ui.mobile

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.R
import app.tastile.android.ui.dashboard.TimelineScale
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MobileTopBarTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `top bar renders menu, scale, notifications, avatar controls`() {
        var menu: String = ""
        var notifications: String = ""
        var avatar: String = ""

        rule.setContent {
            val context = LocalContext.current
            menu = context.getString(R.string.mobile_top_menu)
            notifications = context.getString(R.string.mobile_top_notifications)
            avatar = context.getString(R.string.mobile_top_avatar)

            MobileTopBar(
                title = "Execute",
                scale = TimelineScale.Day,
                onScaleChange = {},
                onMenu = {},
                onNotifications = {},
                onAvatar = {},
            )
        }

        rule.onNodeWithContentDescription(menu)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithContentDescription("Scale: Day")
            .assertIsDisplayed()
        rule.onNodeWithContentDescription(notifications)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithContentDescription(avatar)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `scale picker opens and selects new scale`() {
        val selected = AtomicInteger(-1)
        val currentScale = mutableStateOf(TimelineScale.Day)

        rule.setContent {
            MobileTopBar(
                title = "Execute",
                scale = currentScale.value,
                onScaleChange = {
                    selected.set(it.ordinal)
                    currentScale.value = it
                },
                onMenu = {},
                onNotifications = {},
                onAvatar = {},
            )
        }

        rule.onNodeWithContentDescription("Scale: Day").performClick()
        rule.onNodeWithText("Week").performClick()
        rule.onNodeWithContentDescription("Scale: Week").assertIsDisplayed()

        assertEquals(TimelineScale.Week.ordinal, selected.get())
    }
}
