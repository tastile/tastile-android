package app.tastile.android.ui.mobile

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.R
import app.tastile.android.ui.dashboard.CalendarMode
import app.tastile.android.ui.dashboard.TimelineScale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
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
            )
        }

        rule.onNodeWithContentDescription("Scale: Day").performClick()
        rule.onNodeWithText("Week").performClick()
        rule.onNodeWithContentDescription("Scale: Week").assertIsDisplayed()

        assertEquals(TimelineScale.Week.ordinal, selected.get())
    }

    @Test
    fun `dropdown exposes nav, mode, and minimum-duration sections when configured`() {
        val today = AtomicReference<Unit>()
        val previous = AtomicReference<Unit>()
        val next = AtomicReference<Unit>()
        val modeRef = AtomicReference<CalendarMode?>(null)
        val minRef = AtomicReference<Int?>(null)
        val currentScale = mutableStateOf(TimelineScale.Day)

        rule.setContent {
            MobileTopBar(
                title = "Timeline",
                scale = currentScale.value,
                onScaleChange = { currentScale.value = it },
                onMenu = {},
                onNotifications = {},
                calendarMode = CalendarMode.Scope,
                onCalendarModeChange = { modeRef.set(it) },
                onToday = { today.set(Unit) },
                onPrevious = { previous.set(Unit) },
                onNext = { next.set(Unit) },
                canNavigate = true,
                minimumDuration = 0,
                onMinimumDurationChange = { minRef.set(it) },
            )
        }

        rule.onNodeWithContentDescription("Scale: Day").performClick()

        // DropdownMenu renders in a popup window, so the items exist in the
        // semantics tree but fail the screen-bounds check used by
        // assertIsDisplayed. assertExists is the right check for popup items.
        rule.onNodeWithTag("dropdown-today").assertExists()
        rule.onNodeWithTag("dropdown-nav-prev").assertExists()
        rule.onNodeWithTag("dropdown-nav-next").assertExists()
        rule.onNodeWithTag("dropdown-mode-scope").assertExists()
        rule.onNodeWithTag("dropdown-mode-around").assertExists()
        rule.onNodeWithTag("dropdown-mode-future").assertExists()
        rule.onNodeWithTag("dropdown-min-0").assertExists()
        rule.onNodeWithTag("dropdown-min-5").assertExists()
        rule.onNodeWithTag("dropdown-min-15").assertExists()
        rule.onNodeWithTag("dropdown-min-30").assertExists()

        rule.onNodeWithTag("dropdown-today").performClick()
        rule.onNodeWithContentDescription("Scale: Day").performClick()
        rule.onNodeWithTag("dropdown-mode-future").performClick()
        rule.onNodeWithContentDescription("Scale: Day").performClick()
        rule.onNodeWithTag("dropdown-min-15").performClick()

        assertEquals(Unit, today.get())
        assertEquals(CalendarMode.Future, modeRef.get())
        assertEquals(15, minRef.get())
    }

    @Test
    fun `dropdown hides nav, mode, and minimum sections when no callbacks are configured`() {
        val currentScale = mutableStateOf(TimelineScale.Day)

        rule.setContent {
            MobileTopBar(
                title = "Execute",
                scale = currentScale.value,
                onScaleChange = { currentScale.value = it },
                onMenu = {},
                onNotifications = {},
            )
        }

        rule.onNodeWithContentDescription("Scale: Day").performClick()

        assert(rule.onAllNodesWithTag("dropdown-today").fetchSemanticsNodes().isEmpty())
        assert(rule.onAllNodesWithTag("dropdown-nav-prev").fetchSemanticsNodes().isEmpty())
        assert(rule.onAllNodesWithTag("dropdown-nav-next").fetchSemanticsNodes().isEmpty())
        assert(rule.onAllNodesWithTag("dropdown-mode-scope").fetchSemanticsNodes().isEmpty())
        assert(rule.onAllNodesWithTag("dropdown-min-0").fetchSemanticsNodes().isEmpty())
    }
}
