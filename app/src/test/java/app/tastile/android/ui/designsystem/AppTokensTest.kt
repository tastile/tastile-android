package app.tastile.android.ui.designsystem

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppTokensTest {

    @Test
    fun flatDesignTokens_haveExpectedRadiiAndButtonHeight() {
        assertEquals(6.dp, AppCorner.small)
        assertEquals(8.dp, AppCorner.medium)
        assertEquals(48.dp, AppComponentSize.buttonMinHeight)
    }
}

