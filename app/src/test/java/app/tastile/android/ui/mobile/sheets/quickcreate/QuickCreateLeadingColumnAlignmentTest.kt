package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.NiaTheme
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pins `EditableTitleField` and the first NiaListItem row (`Time`) headline
 * to the same x-coordinate (`PanelTokens.LeadingColumnWidth = 56.dp`), which
 * is the M3 ListItem `leadingContent` slot (16 ListItemStartPadding + 24
 * IconSize + 16 gap).
 *
 * Task 1 (commit `9db7b67`) added `PanelTokens`. This test is the first
 * consumer; it is part of the Case C fix that shifts the EditableTitleField
 * from `padding(horizontal = 16.dp)` to `padding(start =
 * PanelTokens.LeadingColumnWidth, end = 16.dp)`.
 *
 * Why `useUnmergedTree = true` for the Time headline: M3 `ListItem`
 * internally merges its `headlineContent` into the row's semantics node for
 * accessibility, so the merged-tree bounds of the headline collapse to the
 * row's left edge (0.dp). The unmerged tree reports the headline's actual
 * layout bounds (56.dp from row start), which is what we want to assert.
 */
@RunWith(RobolectricTestRunner::class)
class QuickCreateLeadingColumnAlignmentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `title text and ListItem headline share the same x-coordinate`() {
        composeTestRule.setContent {
            NiaTheme {
                QuickCreatePanelContent(
                    store = QuickCreateStateStore(),
                    onClose = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("quick-create-title")
            .assertLeftPositionInRootIsEqualTo(56.dp)
        composeTestRule.onNodeWithText("Time", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(56.dp)
    }
}
