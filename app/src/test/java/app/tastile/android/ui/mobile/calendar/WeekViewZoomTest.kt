package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeekViewZoomTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pinchOut_updatesZoomAndAnchorsScroll() {
        val scrollState = ScrollState(0)
        var observedZoom = 1.5f
        compose.setContent {
            var zoom by remember { mutableFloatStateOf(1.5f) }
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 1200.dp)) {
                    WeekView(
                        items = emptyList(),
                        weekStart = LocalDate.of(2026, 7, 13),
                        zone = ZoneId.of("UTC"),
                        onOpenDay = {},
                        zoom = zoom,
                        onZoomChange = {
                            observedZoom = it
                            zoom = it
                        },
                        onEditEvent = {},
                        scrollState = scrollState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("week-view-frame-grid-lines").performTouchInput {
            pinch(
                start0 = Offset(200f, 400f),
                end0 = Offset(100f, 400f),
                start1 = Offset(400f, 400f),
                end1 = Offset(500f, 400f),
                durationMillis = 300L,
            )
        }

        compose.runOnIdle {
            assertTrue("expected pinch-out to increase Week zoom", observedZoom > 1.5f)
            assertTrue("expected pinch centroid to anchor Week scroll", scrollState.value > 0)
        }
    }

    @Test fun oneFingerSwipe_scrollsWithoutChangingZoom() {
        val scrollState = ScrollState(0)
        var observedZoom = 1.5f
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 1200.dp)) {
                    WeekView(
                        items = emptyList(),
                        weekStart = LocalDate.of(2026, 7, 13),
                        zone = ZoneId.of("UTC"),
                        onOpenDay = {},
                        zoom = observedZoom,
                        onZoomChange = { observedZoom = it },
                        onEditEvent = {},
                        scrollState = scrollState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("week-view-frame-grid-lines").performTouchInput {
            swipeUp(startY = 800f, endY = 200f, durationMillis = 300L)
        }

        compose.runOnIdle {
            assertTrue("expected one-finger swipe to scroll Week", scrollState.value > 0)
            assertEquals(1.5f, observedZoom, 0.0001f)
        }
    }
}
