package app.tastile.android.ui.mobile.tabs

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineZoomMathTest {

    @Test
    fun anchoredZoomScrollTarget_keepsMinuteUnderAnchorStable() {
        val currentScroll = 300
        val anchorY = 250f
        val oldPxPerMin = 2f
        val newPxPerMin = 3f

        val target = anchoredZoomScrollTarget(
            currentScrollPx = currentScroll,
            anchorYpx = anchorY,
            oldPxPerMin = oldPxPerMin,
            newPxPerMin = newPxPerMin,
            totalMinutes = 1470,
            viewportPx = 900f,
        )

        val oldMinute = (currentScroll + anchorY) / oldPxPerMin
        val newMinute = (target + anchorY) / newPxPerMin
        assertEquals(oldMinute, newMinute, 0.5f)
    }

    @Test
    fun anchoredZoomScrollTarget_chainsFromPreviousTargetDuringRapidPinch() {
        val anchorY = 260f
        val totalMinutes = 1470
        val viewportPx = 900f
        val startScroll = 420
        val oldPxPerMin = 2f
        val midPxPerMin = 3f
        val newPxPerMin = 4f

        val midTarget = anchoredZoomScrollTarget(
            currentScrollPx = startScroll,
            anchorYpx = anchorY,
            oldPxPerMin = oldPxPerMin,
            newPxPerMin = midPxPerMin,
            totalMinutes = totalMinutes,
            viewportPx = viewportPx,
        )
        val finalTarget = anchoredZoomScrollTarget(
            currentScrollPx = midTarget,
            anchorYpx = anchorY,
            oldPxPerMin = midPxPerMin,
            newPxPerMin = newPxPerMin,
            totalMinutes = totalMinutes,
            viewportPx = viewportPx,
        )

        val oldMinute = (startScroll + anchorY) / oldPxPerMin
        val finalMinute = (finalTarget + anchorY) / newPxPerMin
        assertEquals(oldMinute, finalMinute, 0.5f)
    }

    @Test
    fun anchoredZoomScrollTarget_clampsToNewContentBounds() {
        val target = anchoredZoomScrollTarget(
            currentScrollPx = 5_000,
            anchorYpx = 600f,
            oldPxPerMin = 3f,
            newPxPerMin = 1f,
            totalMinutes = 1470,
            viewportPx = 1_000f,
        )

        assertEquals(470, target)
    }
}
