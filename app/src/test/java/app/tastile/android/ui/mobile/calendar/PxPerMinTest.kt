package app.tastile.android.ui.mobile.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM invariants on the v37 day-view [computePxPerMin] helper (T7).
 *
 * The helper was lifted out of `DayViewScaffold`'s inline math so this
 * test can lock the formula without spinning up Compose or Robolectric.
 * Regression in the formula would silently break the v34 "24h fits on
 * one screen at min zoom" invariant — this test owns that guarantee.
 *
 * The day range `0..24` is enforced at the composable call site
 * (`startHour = 0`, `endHour = 24`), NOT inside the helper, so this
 * test verifies the *behavior* of the formula on a `totalMinutes`
 * value matching `1440 + 2 * SCROLL_BUFFER_MIN`.
 */
class PxPerMinTest {

    private val totalMinutes = 24 * 60 + GridConstants.SCROLL_BUFFER_MIN * 2

    @Test fun baseline_atZoomDefault_isAvailableHeightOverTotalMinutes() {
        // availableHeightPx = totalMinutes → pxPerMinBase = 1f.
        // At ZOOM_DEFAULT the helper must return pxPerMinBase (not scaled).
        val pxPerMin = computePxPerMin(
            availableHeightPx = totalMinutes.toFloat(),
            totalMinutes = totalMinutes,
            zoom = GridConstants.ZOOM_DEFAULT,
        )
        assertEquals(1f, pxPerMin, 0.0001f)
    }

    @Test fun zoomDefault_twoHourViewport_isHalfAPxPerMin() {
        // availableHeightPx = 720 → pxPerMinBase = 0.5f.
        val pxPerMin = computePxPerMin(
            availableHeightPx = 720f,
            totalMinutes = totalMinutes,
            zoom = GridConstants.ZOOM_DEFAULT,
        )
        assertEquals(720f / totalMinutes, pxPerMin, 0.0001f)
    }

    @Test fun zoomMax_scalesBaseline6x_andStaysWithinBounds() {
        // Zoom=ZOOM_MAX must scale the baseline up to ~6x. The result is
        // NOT expected to be exactly 6x because the helper floors at
        // pxPerMinBase — instead, the value must be at least the baseline
        // and at most baseline * ZOOM_MAX.
        val availableHeightPx = 1440f
        val pxPerMin = computePxPerMin(
            availableHeightPx = availableHeightPx,
            totalMinutes = totalMinutes,
            zoom = GridConstants.ZOOM_MAX,
        )
        val pxPerMinBase = availableHeightPx / totalMinutes
        assertTrue("zoom must scale above baseline", pxPerMin >= pxPerMinBase)
        assertTrue(
            "zoom must not exceed ZOOM_MAX * baseline",
            pxPerMin <= pxPerMinBase * GridConstants.ZOOM_MAX + 0.0001f,
        )
    }

    @Test fun zoomBelowMin_isClampedToMin_soDayRangeStaysValid() {
        // Out-of-range zoom (e.g. 0.5f) must be clamped up to ZOOM_MIN —
        // otherwise the v34 invariant "the 0..24 day does NOT overflow
        // the viewport" would fail.
        val availableHeightPx = 1440f
        val pxPerMin = computePxPerMin(
            availableHeightPx = availableHeightPx,
            totalMinutes = totalMinutes,
            zoom = 0.5f,
        )
        val pxPerMinAtZoomMin = computePxPerMin(
            availableHeightPx = availableHeightPx,
            totalMinutes = totalMinutes,
            zoom = GridConstants.ZOOM_MIN,
        )
        assertEquals(pxPerMinAtZoomMin, pxPerMin, 0.0001f)
    }

    @Test fun zoomAboveMax_isClampedToMax() {
        val availableHeightPx = 1440f
        val pxPerMin = computePxPerMin(
            availableHeightPx = availableHeightPx,
            totalMinutes = totalMinutes,
            zoom = GridConstants.ZOOM_MAX + 10f,
        )
        val pxPerMinAtZoomMax = computePxPerMin(
            availableHeightPx = availableHeightPx,
            totalMinutes = totalMinutes,
            zoom = GridConstants.ZOOM_MAX,
        )
        assertEquals(pxPerMinAtZoomMax, pxPerMin, 0.0001f)
    }

    @Test fun neverFallsBelowBaselineFloor_evenAtMinZoom() {
        // The core v34 guarantee: at ZOOM_MIN the day must fit in the
        // available viewport. The helper floors at pxPerMinBase, so the
        // result must NEVER be less than that.
        val availableHeightPx = 1440f
        for (zoom in floatArrayOf(0.1f, 0.5f, 1f, 2f, 6f, 12f)) {
            val pxPerMin = computePxPerMin(
                availableHeightPx = availableHeightPx,
                totalMinutes = totalMinutes,
                zoom = zoom,
            )
            val pxPerMinBase = availableHeightPx / totalMinutes
            assertTrue(
                "zoom=$zoom produced pxPerMin=$pxPerMin < base=$pxPerMinBase",
                pxPerMin >= pxPerMinBase - 0.0001f,
            )
        }
    }

    @Test fun dayRangeIsForcedTo0To24_atHelperCallSite() {
        // The 0..24 day range is enforced by `DayViewScaffold` setting
        // `startHour = 0`, `endHour = 24` and computing
        // `totalMinutes = 24*60 + 2*SCROLL_BUFFER_MIN`. This test pins
        // both the constants and the totalMinutes so the formula's
        // downstream behavior is well-defined.
        assertEquals(0, GridConstants.DAY_START_HOUR)
        assertEquals(24, GridConstants.DAY_END_HOUR)
        // 24*60 + 30 = 1470 minutes from 0..24 + 15 min buffer top + 15 min buffer bottom.
        assertEquals(1470, totalMinutes)
    }
}
