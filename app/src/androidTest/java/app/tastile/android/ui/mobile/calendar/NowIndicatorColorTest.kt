package app.tastile.android.ui.mobile.calendar

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class NowIndicatorColorTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun nowIndicator_lineRendersM3ErrorToken_notColorRed() {
        var expectedColor = Color.Unspecified
        compose.setContent {
            MaterialTheme {
                expectedColor = MaterialTheme.colorScheme.error
                Box(Modifier.size(400.dp, 1200.dp)) {
                    NowIndicator(
                        nowProvider = { Instant.parse("2026-07-17T08:30:00Z") },
                        zone = ZoneOffset.UTC,
                        pxPerMin = 1f,
                        dayRangeStartHour = 0,
                        dayRangeEndHour = 24,
                        modifier = Modifier.testTag("now-indicator-color"),
                    )
                }
            }
        }
        compose.waitForIdle()

        val lineImage = compose.onNodeWithTag("now-indicator-line").captureToImage()
        val linePixel = lineImage.toPixelMap()[lineImage.width / 2, lineImage.height / 2]
        assertEquals(expectedColor, linePixel)
        assertNotEquals(Color.Red, linePixel)
    }

    @Test
    fun nowIndicator_dotCenterAlignsWithGridLeftEdge() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 1200.dp)) {
                    NowIndicator(
                        nowProvider = { Instant.parse("2026-07-17T08:30:00Z") },
                        zone = ZoneOffset.UTC,
                        pxPerMin = 1f,
                        dayRangeStartHour = 0,
                        dayRangeEndHour = 24,
                    )
                }
            }
        }

        compose.onNodeWithTag("now-indicator-dot")
            .assertLeftPositionInRootIsEqualTo((-5).dp)
    }
}
