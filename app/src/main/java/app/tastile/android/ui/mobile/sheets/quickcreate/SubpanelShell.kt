/*
 * SubpanelShell.kt
 *
 * Top-level subpanel dispatcher. Selects the appropriate private panel
 * composable based on the current [QuickCreatePanel] selection. The
 * individual subpanels live in their own files for readability.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreateProject
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore

@Composable
internal fun QuickCreateSubpanel(
    panel: QuickCreatePanel,
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    onBack: () -> Unit,
    projects: List<QuickCreateProject>,
    knownTags: List<String>,
) {
    // `FormFieldColumn` already applies the 8dp row gap (`RowVerticalSpacing`),
    // so no explicit arrangement is needed — identical grid rhythm to the
    // main panels. The outer 16dp horizontal padding is applied by the sheet.
    FormFieldColumn(
        Modifier
            .testTag("quick-create-subpanel-${panel.name}")
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        when (panel) {
            QuickCreatePanel.Intent -> IntentPanel(store)
            QuickCreatePanel.Identity -> IdentityPanel(draft, store)
            QuickCreatePanel.Time -> TimePanel(draft, store)
            QuickCreatePanel.Duration -> DurationPanel(draft, store)
            QuickCreatePanel.Recurring -> RecurringPanel(draft, store)
            QuickCreatePanel.References -> ReferencesPanel(draft, store)
            QuickCreatePanel.Completion -> CompletionPanel(draft, store)
            QuickCreatePanel.PlacementRules -> PlacementRulesPanel(draft, store)
            QuickCreatePanel.Meta -> MetaPanel(draft, store, projects, knownTags, onBack)
            QuickCreatePanel.Schedule -> SchedulePanel(draft, store)
            QuickCreatePanel.Base -> Unit
        }
    }
}
