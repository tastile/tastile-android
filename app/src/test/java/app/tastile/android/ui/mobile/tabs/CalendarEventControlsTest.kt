package app.tastile.android.ui.mobile.tabs

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.api.Workspace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarEventControlsTest {
    @Test
    fun calendar_event_target_uses_tile_for_recurring_placement_and_placement_otherwise() {
        assertEquals(
            CalendarEventTarget.RecurringTile("tile-1"),
            calendarEventTarget(CoreTimelineItem("placement-1", "tile-1", 1, "Daily", "work", "scheduled", "2026-07-16T09:00:00Z")),
        )
        assertEquals(
            CalendarEventTarget.Placement("event-1", "tile-2"),
            calendarEventTarget(CoreTimelineItem("event-1", "tile-2", 0, "One-off", "work", "scheduled", "2026-07-16T09:00:00Z")),
        )
    }

    @Test
    fun project_toggle_cascades_to_descendants_and_reports_partial_parent() {
        val workspaces = listOf(
            workspace("root", "Root"),
            workspace("child", "Child", "root"),
            workspace("grandchild", "Grandchild", "child"),
            workspace("other", "Other"),
        )
        val descendants = projectDescendantMap(workspaces)

        val selected = toggleProjectCascade(setOf("other"), "root", descendants)
        assertEquals(setOf("root", "child", "grandchild", "other"), selected)
        assertTrue(projectCheckState("root", setOf("child"), descendants).indeterminate)
        assertFalse(projectCheckState("root", selected, descendants).indeterminate)
        assertTrue(projectCheckState("root", selected, descendants).checked)
        assertEquals(setOf("other"), toggleProjectCascade(selected, "root", descendants))
        assertEquals(emptySet<String>(), normalizeOwnerSelection(selected, setOf("root", "child", "grandchild", "other")))
    }

    private fun workspace(id: String, name: String, parent: String? = null) = Workspace(
        id = id,
        displayName = name,
        parentSubjectId = parent,
    )
}
