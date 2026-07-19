package app.tastile.android.ui.mobile

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.R
import app.tastile.android.ui.dashboard.TimelineScale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MobileTopBarTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `top bar renders menu, scale, notifications, avatar controls`() {
        // Resolve string resources against the Activity up-front so the
        // composable doesn't call `context.getString()` (lint
        // LocalContextGetResourceValueCall). The values are stable per test.
        val menu = rule.activity.getString(R.string.mobile_top_menu)
        val notifications = rule.activity.getString(R.string.mobile_top_notifications)
        val avatar = rule.activity.getString(R.string.mobile_top_avatar)

        rule.setContent {
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
    fun `dropdown contains only Day, Week, Month items`() {
        val currentScale = mutableStateOf(TimelineScale.Day)

        rule.setContent {
            MobileTopBar(
                title = "Timeline",
                scale = currentScale.value,
                onScaleChange = { currentScale.value = it },
                onMenu = {},
                onNotifications = {},
            )
        }

        rule.onNodeWithContentDescription("Scale: Day").performClick()

        // DropdownMenu renders in a popup window, so the items exist in the
        // semantics tree. Use onAllNodesWithText and verify count == 2
        // (one for the trigger pill, one for the dropdown item) since the
        // trigger pill also renders the current scale name.
        assertEquals(2, rule.onAllNodesWithText("Day").fetchSemanticsNodes().size)
        assertEquals(1, rule.onAllNodesWithText("Week").fetchSemanticsNodes().size)
        assertEquals(1, rule.onAllNodesWithText("Month").fetchSemanticsNodes().size)
    }

    @Test
    fun `week title wraps small weekdays in parentheses`() {
        val title = formatWeekTitle(
            weekStart = LocalDate.of(2026, 7, 13),
            weekEnd = LocalDate.of(2026, 7, 19),
            locale = Locale.JAPAN,
        )

        assertEquals("7/13(月)–7/19(日)", title.text)
        assertEquals(
            listOf("(月)", "(日)"),
            title.spanStyles.map { title.text.substring(it.start, it.end) },
        )
        assertEquals(listOf(11.sp, 11.sp), title.spanStyles.map { it.item.fontSize })
    }

    @Test
    fun `month picker confirms the selected month`() {
        val selected = AtomicReference<YearMonth?>(null)
        val august = YearMonth.of(2026, 8)
        val augustDescription = august.format(
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()),
        )

        rule.setContent {
            MonthPickerDialog(
                initialMonth = YearMonth.of(2026, 7),
                onDismissRequest = {},
                onMonthSelected = selected::set,
            )
        }

        rule.onNodeWithContentDescription(augustDescription)
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        rule.onNodeWithText(rule.activity.getString(R.string.date_picker_confirm)).performClick()

        rule.runOnIdle { assertEquals(august, selected.get()) }
    }

    @Test
    fun `month picker cancel leaves the selection unchanged`() {
        val selected = AtomicReference<YearMonth?>(null)
        val august = YearMonth.of(2026, 8)
        val augustDescription = august.format(
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()),
        )

        rule.setContent {
            MonthPickerDialog(
                initialMonth = YearMonth.of(2026, 7),
                onDismissRequest = {},
                onMonthSelected = selected::set,
            )
        }

        rule.onNodeWithContentDescription(augustDescription)
            .performScrollTo()
            .performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.date_picker_cancel)).performClick()

        rule.runOnIdle { assertNull(selected.get()) }
    }
}
