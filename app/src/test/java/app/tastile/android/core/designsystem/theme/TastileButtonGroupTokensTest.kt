package app.tastile.android.core.designsystem.theme

import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.component.ButtonGroupSize
import org.junit.Assert.assertEquals
import org.junit.Test

class TastileButtonGroupTokensTest {

    private val tokens = TastileButtonGroupTokens.Default

    @Test
    fun `Xs size is 32dp height with 8dp horizontal padding`() {
        assertEquals(32.dp, tokens.height(ButtonGroupSize.Xs))
        assertEquals(8.dp, tokens.horizontalPadding(ButtonGroupSize.Xs))
    }

    @Test
    fun `M size is 48dp height with 16dp horizontal padding`() {
        assertEquals(48.dp, tokens.height(ButtonGroupSize.M))
        assertEquals(16.dp, tokens.horizontalPadding(ButtonGroupSize.M))
    }

    @Test
    fun `Xl size is 64dp height with 24dp horizontal padding`() {
        assertEquals(64.dp, tokens.height(ButtonGroupSize.Xl))
        assertEquals(24.dp, tokens.horizontalPadding(ButtonGroupSize.Xl))
    }

    @Test
    fun `icon size scales with button size`() {
        assertEquals(16.dp, tokens.iconSize(ButtonGroupSize.Xs))
        assertEquals(20.dp, tokens.iconSize(ButtonGroupSize.M))
        assertEquals(28.dp, tokens.iconSize(ButtonGroupSize.Xl))
    }
}
