/*
 * ReferencesSubpanel.kt
 *
 * Authoring UI for the [QuickCreatePlanReference] list: per-row target
 * kind / relation chips, moment interval stepper and a "remove reference"
 * affordance. The default reference template used for the "+ add" button
 * lives at the bottom.
 *
 * Rows use the 24dp icon column + 16dp gap reservation provided
 * structurally by `FormFieldLayout`, or bypass it entirely via
 * `ScrollableChipRow` for chrome-less chip batches. The 16dp outer
 * horizontal padding is applied by `FormFieldRow` / `ScrollableChipRow`.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Timer
// m2-allow: m3-component
import androidx.compose.material3.FilledTonalButton
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePlanReference
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

@Composable
internal fun ReferencesPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    FormFieldLayout {
        FilledTonalButton(
            onClick = { store.updatePlan(draft.plan.copy(references = draft.plan.references + defaultPlanReference())) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-add-reference"),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.quickcreate_panel_add_reference))
        }
    }
    draft.plan.references.forEachIndexed { index, reference ->
        val target = reference.target.jsonObjectOrEmpty()
        val pick = reference.pick.jsonObjectOrEmpty()
        FormFieldLayout(icon = Icons.Outlined.Key) {
            UnderlineTextField(
                value = reference.id,
                onValueChange = { value -> updateReference(draft, store, index, reference.copy(id = value)) },
                placeholder = stringResource(R.string.quickcreate_panel_reference_record_id),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick-create-reference-record-id-$index"),
            )
        }
        FormFieldLayout(icon = Icons.Outlined.Link) {
            UnderlineTextField(
                value = target.string("referenceId"),
                onValueChange = { value -> updateReference(draft, store, index, reference.copy(target = target.with("referenceId", value.ifBlank { null }))) },
                placeholder = stringResource(R.string.quickcreate_panel_target_reference),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick-create-reference-id-$index"),
            )
        }
        val targetKinds = listOf(0, 1, 2)
        val targetKindIcons = remember {
            mapOf(
                0 to Icons.Outlined.Tag,
                1 to Icons.Outlined.Link,
                2 to Icons.Outlined.Link,
            )
        }
        ScrollableChipRow {
            targetKinds.forEach { kind ->
                FilterChip(
                    selected = target["kind"]?.jsonPrimitive?.content?.toIntOrNull() == kind,
                    onClick = { updateReference(draft, store, index, reference.copy(target = target.with("kind", kind))) },
                    label = {
                        val labelRes = when (kind) {
                            0 -> R.string.quickcreate_panel_kind_exact
                            1 -> R.string.quickcreate_panel_kind_series
                            else -> R.string.quickcreate_panel_kind_filter
                        }
                        Text(stringResource(labelRes))
                    },
                    leadingIcon = { Icon(targetKindIcons.getValue(kind), contentDescription = null) },
                    modifier = Modifier.testTag("quick-create-reference-record-$index-target-kind-$kind"),
                )
            }
        }
        LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_relation_header))
        val relations = listOf(4, 3, 1, 2, 0)
        ScrollableChipRow {
            relations.forEach { relation ->
                FilterChip(
                    selected = pick["kind"]?.jsonPrimitive?.content?.toIntOrNull() == relation,
                    onClick = { updateReference(draft, store, index, reference.copy(pick = pick.with("kind", relation))) },
                    label = {
                        val labelRes = when (relation) {
                            0 -> R.string.quickcreate_panel_relation_touch
                            1 -> R.string.quickcreate_panel_relation_inside
                            2 -> R.string.quickcreate_panel_relation_overlap
                            3 -> R.string.quickcreate_panel_relation_before
                            else -> R.string.quickcreate_panel_relation_after
                        }
                        Text(stringResource(labelRes))
                    },
                    leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                    modifier = Modifier.testTag("quick-create-reference-record-$index-relation-$relation"),
                )
            }
        }
        FormFieldLayout(icon = Icons.Outlined.Timer) {
            LocalNumberField(
                value = pick.string("momentId", "10"),
                onValueChange = { value -> value.toIntOrNull()?.coerceIn(5, 120)?.let { minutes -> updateReference(draft, store, index, reference.copy(pick = pick.with("momentId", minutes.toString()))) } },
                label = stringResource(R.string.quickcreate_panel_field_interval),
                suffix = stringResource(R.string.quickcreate_panel_field_interval_suffix),
                min = 5,
                max = 120,
                step = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick-create-reference-record-$index-interval"),
            )
        }
        FormFieldLayout {
            FilledTonalButton(
                onClick = { store.updatePlan(draft.plan.copy(references = draft.plan.references.filterIndexed { item, _ -> item != index })) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick-create-reference-record-$index-remove"),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.quickcreate_panel_remove_reference))
            }
        }
    }
}

internal fun defaultPlanReference() = QuickCreatePlanReference(
    id = "",
    target = JsonObject(mapOf("kind" to JsonPrimitive(0), "contextKind" to JsonNull, "referenceId" to JsonNull, "conditionId" to JsonNull)),
    pick = JsonObject(mapOf("kind" to JsonPrimitive(4), "momentId" to JsonPrimitive("10"))),
)

internal fun updateReference(draft: QuickCreateDraftState, store: QuickCreateStateStore, index: Int, reference: QuickCreatePlanReference) {
    store.updatePlan(draft.plan.copy(references = draft.plan.references.replace(index, reference)))
}
