/*
 * PlacementRulesSubpanel.kt
 *
 * Authoring UI for the placement-rule list under
 * [QuickCreatePlanningDefinition]. Each rule carries an effect kind, rank and
 * an optional `when` condition encoded as JSON.
 *
 * Rows use the 24dp icon column + 16dp gap reservation provided
 * structurally by `FormFieldLayout`, or bypass it entirely via
 * `ScrollableChipRow` for chrome-less chip batches. The 16dp outer
 * horizontal padding is applied by `FormFieldRow` / `ScrollableChipRow`.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateDurationRange
import app.tastile.android.ui.mobile.sheets.QuickCreatePlacementRule
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

@Composable
internal fun PlacementRulesPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val rules = draft.plan.planning.placementRules
    LocalSectionHeader(title = stringResource(R.string.quick_create_placement_rules))
    FormFieldLayout(icon = Icons.Outlined.Tune) {
        Text(
            text = stringResource(R.string.quick_create_placement_rules_hint),
        )
    }
    rules.forEachIndexed { index, rule ->
        FormFieldLayout {
            Surface(modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-placement-rule-$index")) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.quick_create_rule_number, index + 1))
                    val effectIcons = remember {
                        mapOf(
                            "Permit" to Icons.Outlined.Check,
                            "Deny" to Icons.Outlined.Block,
                            "Limit" to Icons.Outlined.Timer,
                            "Score" to Icons.Outlined.Star,
                            "Record" to Icons.Outlined.Bookmarks,
                        )
                    }
                    ScrollableChipRow {
                        listOf("Permit", "Deny", "Limit", "Score", "Record").forEachIndexed { kind, label ->
                            FilterChip(
                                selected = rule.effect.kind == kind,
                                onClick = { updatePlacementRule(draft, store, index, rule.copy(effect = rule.effect.copy(kind = kind))) },
                                label = { Text(label) },
                                leadingIcon = { Icon(effectIcons.getValue(label), contentDescription = null) },
                                modifier = Modifier.testTag("quick-create-placement-rule-$index-effect-$kind"),
                            )
                        }
                    }
                    FormFieldLayout(icon = Icons.Outlined.FormatListBulleted) {
                        LocalNumberField(
                            value = rule.rank.toString(),
                            onValueChange = { it.toIntOrNull()?.let { rank -> updatePlacementRule(draft, store, index, rule.copy(rank = rank)) } },
                            label = stringResource(R.string.quick_create_rank),
                            suffix = "",
                            min = 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick-create-placement-rule-$index-rank"),
                        )
                    }
                    if (rule.`when` == null) {
                        NiaFilledTonalButton(
                            onClick = { updatePlacementRule(draft, store, index, rule.copy(`when` = defaultPlacementCondition())) },
                            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                            text = { Text(stringResource(R.string.quick_create_add_when_condition)) },
                        )
                    } else {
                        NiaTextButton(
                            onClick = { updatePlacementRule(draft, store, index, rule.copy(`when` = null)) },
                            leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                            text = { Text(stringResource(R.string.quick_create_remove_when_condition)) },
                        )
                    }
                    when (rule.effect.kind) {
                        2 -> {
                            FormFieldLayout(icon = Icons.Outlined.Timer) {
                                LocalNumberField(
                                    value = (rule.effect.span?.minMs ?: 0L).div(60_000L).toString(),
                                    onValueChange = { input -> input.toLongOrNull()?.let { min -> updatePlacementRule(draft, store, index, rule.copy(effect = rule.effect.copy(span = (rule.effect.span ?: QuickCreateDurationRange()).copy(minMs = min * 60_000L)))) } },
                                    label = stringResource(R.string.quick_create_min_minutes), suffix = "min",
                                    min = 0,
                                    step = 5,
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                )
                            }
                            FormFieldLayout(icon = Icons.Outlined.Timer) {
                                LocalNumberField(
                                    value = (rule.effect.span?.maxMs ?: 0L).div(60_000L).toString(),
                                    onValueChange = { input -> input.toLongOrNull()?.let { max -> updatePlacementRule(draft, store, index, rule.copy(effect = rule.effect.copy(span = (rule.effect.span ?: QuickCreateDurationRange()).copy(maxMs = max * 60_000L)))) } },
                                    label = stringResource(R.string.quick_create_max_minutes), suffix = "min",
                                    min = 0,
                                    step = 5,
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                )
                            }
                        }
                        3 -> FormFieldLayout(icon = Icons.Outlined.Star) {
                            LocalNumberField(
                                value = (rule.effect.score ?: 0).toString(),
                                onValueChange = { input -> input.toIntOrNull()?.let { score -> updatePlacementRule(draft, store, index, rule.copy(effect = rule.effect.copy(score = score))) } },
                                label = stringResource(R.string.quick_create_score), suffix = "",
                                modifier = Modifier
                                    .fillMaxWidth(),
                            )
                        }
                        4 -> ScrollableChipRow {
                            listOf(0 to stringResource(R.string.quick_create_record_optional), 1 to stringResource(R.string.quick_create_record_required)).forEach { (record, label) ->
                                FilterChip(
                                    selected = (rule.effect.record ?: 0) == record,
                                    onClick = { updatePlacementRule(draft, store, index, rule.copy(effect = rule.effect.copy(record = record))) },
                                    label = { Text(label) },
                                    leadingIcon = { Icon(if (record == 0) Icons.Outlined.RadioButtonUnchecked else Icons.Outlined.CheckCircle, contentDescription = null) },
                                )
                            }
                        }
                    }
                    NiaTextButton(
                        onClick = { store.updatePlan(draft.plan.copy(planning = draft.plan.planning.copy(placementRules = rules.filterIndexed { item, _ -> item != index }))) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        text = { Text(stringResource(R.string.quick_create_remove_rule)) },
                    )
                }
            }
        }
    }
    FormFieldLayout {
        NiaFilledTonalButton(
            onClick = { store.updatePlan(draft.plan.copy(planning = draft.plan.planning.copy(placementRules = rules + QuickCreatePlacementRule(UUID.randomUUID().toString())))) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-add-placement-rule"),
            text = { Text(stringResource(R.string.quick_create_add_placement_rule)) },
        )
    }
}

private fun updatePlacementRule(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    index: Int,
    rule: QuickCreatePlacementRule,
) {
    store.updatePlan(draft.plan.copy(planning = draft.plan.planning.copy(placementRules = draft.plan.planning.placementRules.replace(index, rule))))
}

private fun defaultPlacementCondition(): JsonElement = buildJsonObject {
    put("Term", buildJsonObject {
        put("Calendar", buildJsonObject {
            put("weekday_mask", JsonPrimitive(127))
            put("time_start", JsonNull)
            put("time_end", JsonNull)
            put("holiday_kind", JsonPrimitive(0))
            put("date_range", JsonNull)
            put("offset_min", JsonPrimitive(0))
        })
    })
}
