package app.tastile.android.core.designsystem.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TastileShapeTokensTest {

    @Test fun `Default exposes xs=4dp`() {
        assertEquals(4.dp, TastileShapeTokens.Default.xs)
    }

    @Test fun `Default exposes s=8dp m=16dp large=20dp xl=28dp`() {
        assertEquals(8.dp, TastileShapeTokens.Default.s)
        assertEquals(16.dp, TastileShapeTokens.Default.m)
        assertEquals(20.dp, TastileShapeTokens.Default.large)
        assertEquals(28.dp, TastileShapeTokens.Default.xl)
    }

    @Test fun `data class equality holds`() {
        val a = TastileShapeTokens(4.dp, 8.dp, 16.dp, 20.dp, 28.dp)
        val b = TastileShapeTokens(4.dp, 8.dp, 16.dp, 20.dp, 28.dp)
        assertEquals(a, b)
    }
}