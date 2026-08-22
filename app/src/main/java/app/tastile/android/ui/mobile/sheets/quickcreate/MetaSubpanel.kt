/*
 * MetaSubpanel.kt
 *
 * Authoring UI for owner project, free-form tags and memo. Provides the
 * apply / cancel buttons that close the QuickCreate sheet.
 *
 * Rows use the 24dp icon column + 16dp gap reservation provided
 * structurally by `FormFieldLayout`, or bypass it entirely via
 * `ScrollableChipRow` for chrome-less chip batches. The 16dp outer
 * horizontal padding is applied by `FormFieldRow` / `ScrollableChipRow`.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Tag
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateProject
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MetaPanel(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    projects: List<QuickCreateProject>,
    knownTags: List<String>,
    onBack: () -> Unit,
) {
    LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_meta_project))
    ScrollableChipRow(modifier = Modifier.testTag("meta-project-catalog")) {
        FilterChip(
            selected = draft.meta.ownerSubjectId == null,
            onClick = { store.updateMeta(draft.meta.copy(ownerSubjectId = null)) },
            label = { Text(stringResource(R.string.quickcreate_panel_meta_no_project)) },
            leadingIcon = { Icon(Icons.Outlined.FolderOff, contentDescription = null) },
            modifier = Modifier.testTag("meta-project-none"),
        )
        projects.forEach { project ->
            FilterChip(
                selected = draft.meta.ownerSubjectId == project.id,
                onClick = { store.updateMeta(draft.meta.copy(ownerSubjectId = project.id)) },
                label = { Text(project.displayName) },
                leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                modifier = Modifier.testTag("meta-project-${project.id}"),
            )
        }
    }
    LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_meta_tags))
    ScrollableChipRow(modifier = Modifier.testTag("meta-tag-chips")) {
        knownTags.filterNot { it in draft.meta.tags }.forEach { tag ->
            FilterChip(
                selected = false,
                onClick = { store.updateMeta(draft.meta.copy(tags = draft.meta.tags + tag)) },
                label = { Text(stringResource(R.string.quickcreate_panel_meta_tag_chip, tag)) },
                leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
                modifier = Modifier.testTag("meta-tag-suggestion-$tag"),
            )
        }
        draft.meta.tags.forEach { tag ->
            FilterChip(
                selected = true,
                onClick = { store.updateMeta(draft.meta.copy(tags = draft.meta.tags - tag)) },
                label = { Text(stringResource(R.string.quickcreate_panel_meta_tag_chip_remove, tag)) },
                leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
                modifier = Modifier.testTag("meta-tag-selected-$tag"),
            )
        }
    }
    var tagDraft by remember { mutableStateOf("") }
    FormFieldLayout(icon = Icons.Outlined.Tag) {
        OutlinedTextField(
            value = tagDraft,
            onValueChange = { tagDraft = it },
            label = { Text(stringResource(R.string.quickcreate_panel_meta_add_tag)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("meta-tag-input"),
        )
    }
    FormFieldLayout {
        NiaFilledTonalButton(
            onClick = {
                val tag = tagDraft.trim().removePrefix("#")
                if (tag.isNotBlank() && tag !in draft.meta.tags) store.updateMeta(draft.meta.copy(tags = draft.meta.tags + tag))
                tagDraft = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("meta-tag-add"),
            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.quickcreate_panel_meta_add_tag)) },
        )
    }
    FormFieldLayout(icon = Icons.Outlined.Description) {
        OutlinedTextField(
            value = draft.meta.memo,
            onValueChange = { value -> store.updateMeta(draft.meta.copy(memo = value)) },
            label = { Text(stringResource(R.string.quickcreate_panel_meta_memo)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("meta-memo"),
        )
    }
    FormFieldLayout {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            NiaTextButton(
                onClick = { store.updateMeta(draft.meta.copy(ownerSubjectId = null, tags = emptyList(), memo = "")) },
                modifier = Modifier.testTag("meta-clear"),
                leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                text = { Text(stringResource(R.string.quickcreate_panel_meta_clear)) },
            )
            NiaTextButton(
                onClick = onBack,
                modifier = Modifier.testTag("meta-cancel"),
                leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                text = { Text(stringResource(R.string.quickcreate_panel_meta_cancel)) },
            )
            NiaButton(
                onClick = onBack,
                modifier = Modifier.testTag("meta-apply"),
                leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
                text = { Text(stringResource(R.string.quickcreate_panel_meta_apply)) },
            )
        }
    }
}
