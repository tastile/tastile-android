package app.tastile.android.ui.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [MonthEventIndicator] (Phase v37 / Task 5).
 *
 * MonthEventIndicator visualises the per-cell event count. Contract:
 *   * `count == 0`       → no node at all (zero-state suppresses the dot).
 *   * `count == 1`       → a 6dp dot.
 *   * `count in 2..3`    → a pill with the count text ("2" / "3").
 *   * `count > 3`        → an overflow pill with text "+N".
 *
 * The component lives under the static [MonthViewFrame] in the same overlay
 * layer (see [MonthView]). It must NOT pull `itemsByDate` itself; the parent
 * supplies the resolved `count`.
 *
 * Note: A `count == 0` box should resolve to no semantics at all. The
 * "indicator" root tag only renders when count > 0.
 */
@RunWith(RobolectricTestRunner::class)
class MonthEventIndicatorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun indicator_zeroCount_rendersNothing() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(60.dp, 60.dp)) {
                    MonthEventIndicator(count = 0)
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("month-event-indicator").assertDoesNotExist()
    }

    @Test fun indicator_oneCount_rendersDot() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(60.dp, 60.dp)) {
                    MonthEventIndicator(
                        count = 1,
                        modifier = Modifier.testTag("month-event-indicator"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("month-event-indicator").assertIsDisplayed()
        // Dot variant carries no text — verify by absence.
        compose.onAllNodesWithText("1").assertCountEquals(0)
    }

    @Test fun indicator_threeCount_rendersPillWithText() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(60.dp, 60.dp)) {
                    MonthEventIndicator(
                        count = 3,
                        modifier = Modifier.testTag("month-event-indicator"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("month-event-indicator").assertIsDisplayed()
        compose.onNodeWithText("3").assertIsDisplayed()
    }

    @Test fun indicator_overflowCount_rendersPlusN() = runTest {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(60.dp, 60.dp)) {
                    MonthEventIndicator(
                        count = 5,
                        modifier = Modifier.testTag("month-event-indicator"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("month-event-indicator").assertIsDisplayed()
        compose.onNodeWithText("+5").assertIsDisplayed()
    }
}
