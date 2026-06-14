package app.tastile.android.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationModelTest {

    @Test
    fun appNavigationStartRoute_defaultsToCalendarDay() {
        assertEquals(Screen.CalendarDay.route, appNavigationStartRoute)
    }

    @Test
    fun sidePanelSections_groupsDisplayModesAndManagementTabs() {
        val sections = sidePanelSections()
        assertEquals(3, sections.size)

        val display = sections.first { it.id == "display" }
        assertEquals(
            listOf(Screen.CalendarDay.route, Screen.CalendarWeek.route, Screen.CalendarMonth.route),
            display.items.map { it.route }
        )

        val flow = sections.first { it.id == "flow" }
        assertEquals(
            listOf(Screen.Now.route, Screen.Next.route, Screen.Review.route),
            flow.items.map { it.route }
        )

        val management = sections.first { it.id == "management" }
        assertEquals(
            listOf(
                Screen.Tiles.route,
                Screen.Integrations.route,
                Screen.Settings.route,
                Screen.Account.route
            ),
            management.items.map { it.route }
        )
    }

    @Test
    fun sidePanelSections_exposesAllConfiguredRoutesExactlyOnce() {
        val routes = sidePanelSections().flatMap { section -> section.items.map { it.route } }
        assertEquals(routes.size, routes.toSet().size)
        assertTrue(routes.contains(Screen.CalendarDay.route))
        assertTrue(routes.contains(Screen.Account.route))
    }
}

