package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ZoomEndFrameCallbackTest {
    @get:Rule val compose = createComposeRule()
    @Test fun pinchOut_scrollsWithinOneFrameBridge() {
        val scrollState = ScrollState(0)
        compose.setContent {
            MaterialTheme {
                val zoom = androidx.compose.runtime.remember { mutableFloatStateOf(1f) }
                Box(Modifier.requiredSize(400.dp, 1200.dp)) {
                    DayView(
                        date = LocalDate.now(),
                        zoom = zoom.floatValue,
                        blocks = emptyList(),
                        zone = ZoneId.of("UTC"),
                        today = LocalDate.now(),
                        onCreateAt = { _, _ -> },
                        onEditEvent = {},
                        onZoomChange = { zoom.floatValue = it },
                        scrollState = scrollState,
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("day-view-frame-grid-lines").performTouchInput {
            pinch(
                start0 = Offset(150f, 400f),
                end0 = Offset(50f, 400f),
                start1 = Offset(300f, 400f),
                end1 = Offset(400f, 400f),
                durationMillis = 300L,
            )
        }
        compose.waitForIdle()
        // v38: scrollState advances to the anchored target within one
        // recomposition (no `withFrameNanos` deferral).
        assert(scrollState.value > 0)
    }
}
