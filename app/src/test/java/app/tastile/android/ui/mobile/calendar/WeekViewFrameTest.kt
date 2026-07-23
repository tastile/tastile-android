package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeekViewFrameTest {
    @get:Rule val compose = createComposeRule()

    @Test fun frame_root_andCanvas_areDisplayed() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 600.dp)) {
                    WeekViewFrame(
                        modifier = Modifier.fillMaxSize(),
                        pxPerMin = 1f,
                    )
                }
            }
        }
        compose.onNodeWithTag("week-view-frame").assertIsDisplayed()
        compose.onNodeWithTag("week-view-frame-grid-lines").assertIsDisplayed()
    }

}
