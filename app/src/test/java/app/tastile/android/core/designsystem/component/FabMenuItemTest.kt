package app.tastile.android.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import org.junit.Assert.assertEquals
import org.junit.Test

class FabMenuItemTest {

    @Test
    fun `Action exposes icon and label`() {
        val action = FabMenuItem.Action(
            icon = Icons.Outlined.Add,
            label = "Add event",
            onClick = {},
        )
        assertEquals(Icons.Outlined.Add, action.icon)
        assertEquals("Add event", action.label)
    }

    @Test
    fun `Action invokes onClick when triggered`() {
        var captured = 0
        val action = FabMenuItem.Action(
            icon = Icons.Outlined.Add,
            label = "tap",
            onClick = { captured += 1 },
        )
        action.onClick()
        assertEquals(1, captured)
    }
}