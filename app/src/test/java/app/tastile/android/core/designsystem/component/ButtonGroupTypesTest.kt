package app.tastile.android.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonGroupTypesTest {

    @Test
    fun `ButtonGroupItem defaults enabled to true`() {
        val item = ButtonGroupItem(icon = null, label = "Tap")
        assertEquals("Tap", item.label)
        assertTrue(item.enabled)
    }

    @Test
    fun `ButtonGroupItem can be disabled`() {
        val item = ButtonGroupItem(icon = Icons.Outlined.Add, label = "Tap", enabled = false)
        assertFalse(item.enabled)
    }

    @Test
    fun `ButtonGroupSize has five members in increasing order`() {
        assertEquals(
            listOf(ButtonGroupSize.Xs, ButtonGroupSize.S, ButtonGroupSize.M, ButtonGroupSize.L, ButtonGroupSize.Xl),
            ButtonGroupSize.values().toList(),
        )
    }
}
