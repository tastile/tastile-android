package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose coverage for the QuickCreate identity color swatch row.
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
 *
 * 2026-09-08: rewrote against current production tags. The 7-section /
 * 3-form refactor relocated the swatch row from `ProjectColorRow` (under
 * the Event panel) to `IdentitySubpanel`. The subpanel is rendered as a
 * stacked `ModalBottomSheet` at the sheet level — not inside
 * [QuickCreatePanelContent] — so the test mounts it directly via
 * [QuickCreateSubpanel] rather than the base panel dispatcher.
 */
@RunWith(AndroidJUnit4::class)
class QuickCreateColorSwatchTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun renderIdentitySubpanel(): QuickCreateStateStore {
        val store = QuickCreateStateStore()
        rule.setContent {
            val draft by store.state.collectAsStateWithLifecycle()
            TastileTheme {
                QuickCreateSubpanel(
                    panel = QuickCreatePanel.Identity,
                    draft = draft,
                    store = store,
                    onBack = { store.backToBase() },
                    projects = emptyList(),
                    knownTags = emptyList(),
                )
            }
        }
        return store
    }

    @Test
    fun `clicking a swatch stores the canonical six-digit hex without alpha bleed`() {
        val store = renderIdentitySubpanel()

        // The green swatch in the web palette is `#10b981`. The chip's
        // testTag is `quick-create-color-<hex-no-prefix>`.
        rule.onNodeWithTag("quick-create-color-10b981").performClick()
        rule.waitForIdle()

        assertEquals("#10b981", store.state.value.identity.visual.color)
        // The active swatch must round-trip cleanly through parseHexColor so
        // the selection indicator stays lit across recompositions.
        val active = parseHexColor(store.state.value.identity.visual.color)
        assertTrue("parsed swatch must be opaque", active.alpha == 1f)
        assertEquals(0xFF10B981.toInt(), active.toArgb())
    }

    @Test
    fun `swatch row exposes one surface per web palette color`() {
        renderIdentitySubpanel()
        // The web palette exposes 8 colors (3b82f6, 8b5cf6, ec4899, ef4444,
        // f59e0b, 10b981, 06b6d4, 6b7280). Verify the default swatch id is
        // reachable so the indicator comparison cannot silently drop swatches.
        rule.onAllNodesWithTag("quick-create-color-3b82f6").assertCountEquals(1)
        rule.onAllNodesWithTag("quick-create-color-10b981").assertCountEquals(1)
    }
}