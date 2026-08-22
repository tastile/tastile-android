/*
 * IntentSubpanel.kt
 *
 * Entry point of the QuickCreate sheet — a list of "intent" buttons that
 * jump into each authoring subpanel. Keep ordering stable; the layout
 * defines the UX affordances.
 *
 * Rows use the 24dp icon column + 16dp gap reservation provided
 * structurally by `FormFieldLayout`. The 16dp outer horizontal padding
 * is applied by `FormFieldRow`.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore

@Composable
internal fun IntentPanel(store: QuickCreateStateStore) {
    LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_intent_header))
    val intentTargets = listOf(
        Triple(stringResource(R.string.quickcreate_section_identity), QuickCreatePanel.Identity, Icons.Outlined.TextFields),
        Triple(stringResource(R.string.quickcreate_essential_time), QuickCreatePanel.Time, Icons.Outlined.Schedule),
        Triple(stringResource(R.string.quickcreate_section_recurring), QuickCreatePanel.Recurring, Icons.Outlined.Repeat),
        Triple(stringResource(R.string.quickcreate_section_placement_rules), QuickCreatePanel.PlacementRules, Icons.Outlined.Tune),
        Triple(stringResource(R.string.quickcreate_section_references), QuickCreatePanel.References, Icons.Outlined.Link),
        Triple("Schedule", QuickCreatePanel.Schedule, Icons.Outlined.Tune),
        Triple("Meta", QuickCreatePanel.Meta, Icons.Outlined.Tag),
        Triple("Completion", QuickCreatePanel.Completion, Icons.Outlined.Check),
    )
    intentTargets.forEach { (label, panel, icon) ->
        FormFieldLayout {
            NiaButton(
                onClick = { store.openSubpanel(panel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick-create-intent-${label.lowercase()}"),
                leadingIcon = { Icon(icon, contentDescription = null) },
                text = { Text(label) },
            )
        }
    }
}
