package app.tastile.android.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import app.tastile.android.data.repository.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    @get:Rule val rule = createComposeRule()

    /** API 28 device qualifier: dynamic color unavailable, brand fallback. */
    @Test @Config(sdk = [28])
    fun `BRAND + dynamicColor=false + dark=false resolves to brandLight primary`() {
        rule.setContent {
            TastileTheme(themeMode = ThemeMode.BRAND, dynamicColor = false) {
                val got = MaterialTheme.colorScheme.primary
                val want = BrandColors.light().primary
                assert(got == want) { "got $got want $want" }
            }
        }
    }

    /** API 33 device qualifier: dynamic color path is taken. */
    @Test @Config(sdk = [33])
    fun `BRAND + dynamicColor=true + dark=false on API33 resolves to dynamicLight`() {
        rule.setContent {
            TastileTheme(themeMode = ThemeMode.BRAND, dynamicColor = true) {
                val got = MaterialTheme.colorScheme.primary
                assert(got != BrandColors.light().primary) {
                    "expected dynamic, got brand"
                }
            }
        }
    }
    // See Appendix A for the remaining 6 cells. They are added by this same PR.
}
