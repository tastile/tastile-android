package app.tastile.android.ui.mobile.panels.timeline

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.core.CoreTimelineItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TimelineBlockListTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `TimelineBlockList renders empty state when no blocks`() {
        rule.setContent {
            TimelineBlockList(blocks = emptyList(), onBlockClick = {})
        }

        rule.onNodeWithText("No blocks", substring = true).assertIsDisplayed()
    }

    @Test
    fun `TimelineBlockList renders one row per block`() {
        val blocks = listOf(
            CoreTimelineItem(
                id = "blk-1",
                tileId = "tile-1",
                title = "Focus block",
                type = "work",
                status = "scheduled",
                startAt = "2026-07-15T09:00:00Z",
                endAt = "2026-07-15T10:00:00Z",
            ),
            CoreTimelineItem(
                id = "blk-2",
                tileId = "tile-2",
                title = "Coffee break",
                type = "break",
                status = "scheduled",
                startAt = "2026-07-15T10:00:00Z",
                endAt = "2026-07-15T10:15:00Z",
            ),
        )

        rule.setContent {
            TimelineBlockList(blocks = blocks, onBlockClick = {})
        }

        rule.onNodeWithText("Focus block").assertIsDisplayed()
        rule.onNodeWithText("Coffee break").assertIsDisplayed()
        rule.onAllNodesWithText("work", substring = true).onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("break", substring = true).onFirst().assertIsDisplayed()
    }
}