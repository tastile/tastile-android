package app.tastile.android.ui.mobile.tabs

import app.tastile.android.data.timeline.TimelinePageKey
import app.tastile.android.ui.dashboard.TimelineScale
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun pagerSettleGuard_rejectsStaleScaleGenerationAndPageKey() {
        val dayKey = TimelinePageKey.forScope(
            accountId = "account-a",
            ownerIds = listOf("owner-a"),
            zoneId = ZoneId.of("UTC"),
            scale = TimelineScale.Day,
            anchor = LocalDate.of(2026, 9, 16),
        ).normalized()
        val token = TimelinePagerSettleToken(
            scale = TimelineScale.Day,
            generation = 7L,
            key = dayKey,
        )

        assertTrue(shouldApplyTimelinePagerSettle(token, token))
        assertFalse(
            shouldApplyTimelinePagerSettle(
                token,
                token.copy(scale = TimelineScale.Week),
            ),
        )
        assertFalse(
            shouldApplyTimelinePagerSettle(
                token,
                token.copy(generation = 8L),
            ),
        )
        assertFalse(
            shouldApplyTimelinePagerSettle(
                token,
                token.copy(key = dayKey.copy(anchor = dayKey.anchor.plusDays(1))),
            ),
        )
    }
}
