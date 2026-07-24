package app.tastile.android.ui.mobile.calendar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import org.junit.Assert.assertEquals
import org.junit.Test

class GridPointsBatchingTest {
    @Test fun buildGridPoints_returns25HorizontalLineSegments() {
        val pts = buildGridPoints(width = 800f, pxPerMinPx = 2f, endHour = 24)
        // 25 hours => 25 lines => 50 points in PointMode.Lines.
        assertEquals(50, pts.size)
        // First line goes from (0,0) to (width,0).
        assertEquals(Offset(0f, 0f), pts[0])
        assertEquals(Offset(800f, 0f), pts[1])
    }
}
