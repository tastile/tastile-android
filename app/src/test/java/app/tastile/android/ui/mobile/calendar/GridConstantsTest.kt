package app.tastile.android.ui.mobile.calendar

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Test

class GridConstantsTest {
    @Test fun timeGutterWidth_isPositive() {
        assertTrue(GridConstants.TIME_GUTTER_WIDTH > 0.dp)
    }

    @Test fun dayZoomBounds_areOrdered() {
        assertTrue(GridConstants.ZOOM_MIN < GridConstants.ZOOM_MAX)
        assertTrue(GridConstants.ZOOM_DEFAULT in GridConstants.ZOOM_MIN..GridConstants.ZOOM_MAX)
    }

    @Test fun dayRange_isFull24Hours() {
        assertEquals(0, GridConstants.DAY_START_HOUR)
        assertEquals(24, GridConstants.DAY_END_HOUR)
    }

    @Test fun scrollBuffer_isPositive() {
        assertTrue(GridConstants.SCROLL_BUFFER_MIN > 0)
    }

    private fun assertEquals(expected: Int, actual: Int) =
        org.junit.Assert.assertEquals(expected, actual)
}
