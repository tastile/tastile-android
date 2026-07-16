package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.R
import app.tastile.android.notifications.NotificationItem
import app.tastile.android.notifications.NotificationRepository
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NotificationsSheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders empty state when no notifications`() {
        val repo = mockk<NotificationRepository>()
        every { repo.pending } returns MutableStateFlow(emptyList())
        val overlay = OverlayViewModel()

        rule.setContent {
            NotificationsSheet(overlay = overlay, repository = repo)
        }
        rule.runOnUiThread { overlay.show(Overlay.Notifications) }
        rule.waitForIdle()
        rule.onNodeWithText(rule.activity.getString(R.string.empty_tiles_title)).assertIsDisplayed()
    }

    @Test
    fun `renders notification labels when present`() {
        val repo = mockk<NotificationRepository>()
        every { repo.pending } returns MutableStateFlow(
            listOf(NotificationItem(label = "First"), NotificationItem(label = "Second"))
        )
        val overlay = OverlayViewModel()

        rule.setContent {
            NotificationsSheet(overlay = overlay, repository = repo)
        }
        rule.runOnUiThread { overlay.show(Overlay.Notifications) }
        rule.waitForIdle()
        rule.onNodeWithText("First").assertIsDisplayed()
        rule.onNodeWithText("Second").assertIsDisplayed()
    }
}