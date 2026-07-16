package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
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
}
