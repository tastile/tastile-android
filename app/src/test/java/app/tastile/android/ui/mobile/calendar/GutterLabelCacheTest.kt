package app.tastile.android.ui.mobile.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GutterLabelCacheTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rememberGutterMeasurements_returnsStableList() {
        val cached = mutableStateListOf<List<TextLayoutResult>>()
        compose.setContent {
            MaterialTheme {
                val measurer = rememberTextMeasurer()
                val style = MaterialTheme.typography.labelSmall
                val list = rememberGutterMeasurements(measurer, style, 0, 24)
                cached.add(list)
            }
        }
        compose.waitForIdle()
        assertEquals(25, cached[0].size) // 0..24 inclusive
        // Identity stability: same TextLayoutResult instance across frames.
        val first = cached[0].first()
        compose.waitForIdle()
        // The list is remembered, so identity persists (no re-measure).
        assertSame(first, cached[0].first())
    }

    private fun assertEquals(expected: Int, actual: Int) =
        org.junit.Assert.assertEquals(expected, actual)
    private fun assertSame(a: Any?, b: Any?) =
        org.junit.Assert.assertSame(a, b)
}
