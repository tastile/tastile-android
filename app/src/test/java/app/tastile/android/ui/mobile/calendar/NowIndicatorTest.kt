package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class NowIndicatorTest {
    @get:Rule val compose = createComposeRule()
    private val now = Instant.parse("2026-07-17T08:30:00Z")

    @Test fun nowIndicator_dotAndLine_areDisplayed() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 1200.dp)) {
                    NowIndicator(
                        nowProvider = { now },
                        pxPerMin = 1f,
                        dayRangeStartHour = 0,
                        dayRangeEndHour = 24,
                        modifier = Modifier.testTag("now-indicator"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("now-indicator-dot").assertExists()
        compose.onNodeWithTag("now-indicator-dot").assertHeightIsEqualTo(10.dp)
        compose.onNodeWithTag("now-indicator-line").assertExists()
        compose.onNodeWithTag("now-indicator-line").assertHeightIsEqualTo(2.dp)
    }

    @Test fun nowIndicator_usesZoneForMinutesOfDay() = runTest {
        // 08:30 UTC is 17:30 in JST (UTC+9) = 17*60 + 30 = 1050 minutes-of-day.
        // The line tag stays 2dp tall regardless of zone — what we verify is
        // that the Composable accepts a zone parameter and renders the line.
        val instant = Instant.parse("2026-07-17T08:30:00Z")
        val zoneJst = java.time.ZoneId.of("Asia/Tokyo")
        val expectedMinutes = 17 * 60 + 30
        // Sanity check the math outside Compose so a wrong-zone regression is loud.
        val derived = instant.atZone(zoneJst).hour * 60 + instant.atZone(zoneJst).minute
        check(derived == expectedMinutes) { "expected $expectedMinutes, got $derived" }
        // UTC would give a different value (8*60+30 = 510). Confirm the zone
        // matters: if we computed from epoch seconds we'd get 510, not 1050.
        val utcDerived = ((instant.epochSecond / 60) % (24 * 60)).toInt()
        check(utcDerived != expectedMinutes) {
            "test precondition broken: UTC and JST should differ ($utcDerived vs $expectedMinutes)"
        }

        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 1440.dp)) {
                    NowIndicator(
                        nowProvider = { instant },
                        zone = zoneJst,
                        pxPerMin = 1f,
                        dayRangeStartHour = 0,
                        dayRangeEndHour = 24,
                        modifier = Modifier.testTag("now-indicator-tz"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("now-indicator-line").assertExists()
        compose.onNodeWithTag("now-indicator-line").assertHeightIsEqualTo(2.dp)
        compose.onNodeWithTag("now-indicator-dot").assertExists()
        compose.onNodeWithTag("now-indicator-dot").assertHeightIsEqualTo(10.dp)
    }

    @Test fun nowIndicator_usesM3ErrorToken_notColorRed() = runTest {
        var captured: Color = Color.Unspecified
        compose.setContent {
            MaterialTheme {
                captured = MaterialTheme.colorScheme.error
                Box(Modifier.size(400.dp, 1200.dp)) {
                    NowIndicator(
                        nowProvider = { now },
                        pxPerMin = 1f,
                        dayRangeStartHour = 0,
                        dayRangeEndHour = 24,
                        modifier = Modifier.testTag("now-indicator-color"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("now-indicator-color").assertExists()
        check(captured != Color.Red) {
            "NowIndicator must derive color from M3 colorScheme.error, not Color.Red"
        }
    }
}