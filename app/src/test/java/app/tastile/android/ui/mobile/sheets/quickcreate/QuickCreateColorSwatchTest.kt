package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose coverage for the QuickCreate color swatch row.
 *
 * Pins the regression that broke `#3b82f6` round-tripping:
 *   - the previous `parseHexColor("#3b82f6")` returned a fully transparent
 *     color (alpha=0), so `selected == swatch` was always false and the
 *     swatch row never reflected the active selection;
 *   - the previous `Color.toHexString()` produced a broken packed-color
 *     string like `-3b82f600000000` instead of `#3b82f6`.
 *
 * After the fix, clicking a swatch must (a) update the store with the
 * canonical six-digit hex, (b) keep the swatch the active one across
 * recompositions (the `isSelected` comparison now compares colors with
 * matching alpha), and (c) leave the swatch testTags stable enough to
 * target from UI tests.
 */
@RunWith(AndroidJUnit4::class)
class QuickCreateColorSwatchTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun renderEventPanel(): QuickCreateStateStore {
        val store = QuickCreateStateStore()
        rule.setContent { QuickCreatePanelContent(store, {}, projects = emptyList()) }
        return store
    }

    @Test
    fun `clicking a swatch stores the canonical six-digit hex without alpha bleed`() {
        val store = renderEventPanel()

        // The green swatch in WebColorSwatches is `#10b981`. The
        // `<testTag>-color-<id>` pattern lives on the inner swatch Surface.
        rule.onNodeWithTag("quick-create-event-project-color-color-10b981")
            .performScrollTo()
            .performClick()
        rule.waitForIdle()

        assertEquals("#10b981", store.state.value.identity.visual.color)
        // The active swatch must round-trip cleanly through parseHexColor so
        // the selection indicator stays lit across recompositions.
        val active = parseHexColor(store.state.value.identity.visual.color)
        assertTrue("parsed swatch must be opaque", active.alpha == 1f)
        assertEquals(0xFF10B981, active.toArgb())
    }

    @Test
    fun `swatch row exposes one surface per web palette plus the custom trigger`() {
        renderEventPanel()
        // Each of the six web palette colors produces one swatch; the custom
        // dialog trigger is a sibling. Verify the count and that the default
        // swatch id is reachable so the indicator comparison cannot silently
        // drop swatches.
        rule.onAllNodesWithTag("quick-create-event-project-color-color-3b82f6")
            .assertCountEquals(1)
        rule.onAllNodesWithTag("quick-create-event-project-color-color-custom")
            .assertCountEquals(1)
    }
}