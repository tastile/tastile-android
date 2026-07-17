package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit tests for the dynamic Day-view tile layer (Phase v37 / Task 3).
 *
 * DayViewTile composes the event blocks for the day (the data-bound layer)
 * and the NowIndicator overlay. With `blocks = emptyList()` we exercise the
 * happy-path early-return: the tile root Box is rendered and no EventChip
 * traffic is needed.
 *
 * The companion Frame test covers the static grid; we keep this test focused
 * on the existence + tag-identification of the tile Box.
 */
@RunWith(RobolectricTestRunner::class)
class DayViewTileTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tile_emptyBlocks_rootIsDisplayed() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 1200.dp)) {
                    DayViewTile(
                        blocks = emptyList(),
                        date = LocalDate.parse("2026-07-17"),
                        pxPerMin = 1f,
                        zone = ZoneId.of("UTC"),
                        scrollState = rememberScrollState(),
                        onEditEvent = {},
                        modifier = Modifier.testTag("day-view-tile"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("day-view-tile").assertIsDisplayed()
    }
}
