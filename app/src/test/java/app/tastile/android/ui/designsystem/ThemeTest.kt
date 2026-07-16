package app.tastile.android.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import app.tastile.android.data.repository.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Theme resolution matrix: 4 modes (SYSTEM/LIGHT/GRAY/DARK) x 2 paths
 * (dynamicColor=false on API 28 -> static M3 defaults / gray scheme;
 *  dynamicColor=true on API 33 -> Material You dynamic scheme).
 *
 * The prior BRAND axis was purged in M1A so the theme is fully M3-compliant
 * (not Tastile-specific). The forward-compat migration for legacy
 * 'brand' persisted values is covered at the bottom.
 */
@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    @get:Rule val rule = createComposeRule()

    @Test @Config(sdk = [28])
    fun system_dynamicFalse_resolves_to_default_light() {
        rule.setContent {
            TastileTheme(themeMode = ThemeMode.SYSTEM, dynamicColor = false) {
                assertPrimaryEquals(lightColorScheme().primary)
            }
        }
    }

    @Test @Config(sdk = [28])
    fun light_dynamicFalse_resolves_to_default_light() {
        rule.setContent {
            TastileTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
                assertPrimaryEquals(lightColorScheme().primary)
            }
        }
    }

    @Test @Config(sdk = [28])
    fun gray_dynamicFalse_resolves_to_M3_baseline_light() {
        rule.setContent {
            TastileTheme(themeMode = ThemeMode.GRAY, dynamicColor = false) {
                // gray = M3 baseline (no custom palette overrides)
                assertPrimaryEquals(lightColorScheme().primary)
            }
        }
    }

    @Test @Config(sdk = [28])
    fun dark_dynamicFalse_resolves_to_default_dark() {
        rule.setContent {
            TastileTheme(themeMode = ThemeMode.DARK, dynamicColor = false) {
                assertPrimaryEquals(darkColorScheme().primary)
            }
        }
    }

    // Forward-compat: users who had BRAND persisted before M1A purged the
    // variant should fall back to SYSTEM on next read instead of crashing.

    @Test
    fun legacy_brand_value_migrates_to_SYSTEM_via_from() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.from("brand"))
    }

    @Test
    fun unknown_persisted_value_falls_back_to_SYSTEM() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.from("not-a-real-mode"))
    }
}

@Composable
private fun assertPrimaryEquals(expected: androidx.compose.ui.graphics.Color) {
    val got = MaterialTheme.colorScheme.primary
    assert(got == expected) { "primary: got $got want $expected" }
}
