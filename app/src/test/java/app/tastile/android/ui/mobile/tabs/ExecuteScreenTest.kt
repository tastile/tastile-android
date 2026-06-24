package app.tastile.android.ui.mobile.tabs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle
import app.tastile.android.ui.dashboard.DashboardViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExecuteScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders active tile title when one exists`() {
        val active = Tile(
            id = "t1",
            title = "Code review",
            lifecycle = TileLifecycle.STARTED.value,
        )
        val vm = mockk<DashboardViewModel>(relaxed = true)
        every { vm.tiles } returns MutableStateFlow(listOf(active))
        every { vm.loading } returns MutableStateFlow(false)
        every { vm.locale } returns MutableStateFlow(app.tastile.android.data.repository.AppLocale.EN)

        rule.setContent { ExecuteScreen(viewModel = vm) }
        rule.onAllNodesWithText("Code review", substring = true, useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
    }
}
