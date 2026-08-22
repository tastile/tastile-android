/*
 * RecurringSubpanel.kt
 *
 * Authoring UI for the recurring tile mode: weekly weekday mask,
 * interval cadence and condition-deferred fallback.
 *
 * Rows use the 24dp icon column + 16dp gap reservation provided
 * structurally by `FormFieldLayout`, or bypass it entirely via
 * `ScrollableChipRow` for chrome-less chip batches. The 16dp outer
 * horizontal padding is applied by `FormFieldRow` / `ScrollableChipRow`.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
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
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.dashboard.components.DatePickerField
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateIntervalUnit
import app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateTileKind

@Composable
internal fun RecurringPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val recurring = draft.recurring
    val repeatModeIcons = remember {
        mapOf(
            QuickCreateRepeatMode.Once to Icons.Outlined.Today,
            QuickCreateRepeatMode.Daily to Icons.Outlined.EventRepeat,
            QuickCreateRepeatMode.Weekly to Icons.Outlined.CalendarViewWeek,
            QuickCreateRepeatMode.Interval to Icons.Outlined.Timer,
            QuickCreateRepeatMode.Condition to Icons.Outlined.Tune,
        )
    }
    LocalSectionHeader(title = stringResource(R.string.quick_create_recurring_title))
    ScrollableChipRow {
        QuickCreateRepeatMode.entries.forEach { mode ->
            FilterChip(
                selected = recurring.repeatMode == mode,
                onClick = {
                    store.updateRecurring(recurring.copy(repeatMode = mode))
                    store.updateIdentity(draft.identity.copy(kind = if (mode == QuickCreateRepeatMode.Once) draft.identity.kind else QuickCreateTileKind.Recurring))
                },
                label = { Text(mode.name) },
                leadingIcon = { Icon(repeatModeIcons.getValue(mode), contentDescription = null) },
                modifier = Modifier.testTag("quick-create-repeat-${mode.name.lowercase()}"),
            )
        }
    }
    if (recurring.repeatMode == QuickCreateRepeatMode.Weekly) {
        LocalSectionHeader(title = stringResource(R.string.quick_create_weekdays))
        val days = listOf("S", "M", "T", "W", "T", "F", "S")
        ScrollableChipRow(spacing = 8.dp) {
            days.forEachIndexed { bit, label ->
                val selected = recurring.weekdayMask and (1 shl bit) != 0
                FilterChip(
                    selected = selected,
                    onClick = { store.updateRecurring(recurring.copy(weekdayMask = recurring.weekdayMask xor (1 shl bit))) },
                    label = { Text(label) },
                    modifier = Modifier.testTag("quick-create-weekday-$bit"),
                )
            }
        }
    }
    if (recurring.repeatMode == QuickCreateRepeatMode.Interval) {
        FormFieldLayout(icon = Icons.Outlined.Timer) {
            LocalNumberField(
                value = recurring.intervalValue.toString(),
                onValueChange = { value -> value.toIntOrNull()?.let { store.updateRecurring(recurring.copy(intervalValue = it.coerceAtLeast(1))) } },
                label = stringResource(R.string.quick_create_interval_value),
                suffix = recurring.intervalUnit.name.lowercase(),
                min = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick-create-interval-value"),
            )
        }
        val intervalUnitIcons = remember {
            mapOf(
                QuickCreateIntervalUnit.Minute to Icons.Outlined.AccessTime,
                QuickCreateIntervalUnit.Hour to Icons.Outlined.Schedule,
                QuickCreateIntervalUnit.Day to Icons.Outlined.Today,
            )
        }
        ScrollableChipRow {
            QuickCreateIntervalUnit.entries.forEach { unit ->
                FilterChip(
                    selected = recurring.intervalUnit == unit,
                    onClick = { store.updateRecurring(recurring.copy(intervalUnit = unit)) },
                    label = { Text(unit.name) },
                    leadingIcon = { Icon(intervalUnitIcons.getValue(unit), contentDescription = null) },
                    modifier = Modifier.testTag("quick-create-interval-${unit.name.lowercase()}"),
                )
            }
        }
    }
    if (recurring.repeatMode == QuickCreateRepeatMode.Condition) {
        FormFieldLayout(icon = Icons.Outlined.Tune) {
            Text(
                text = stringResource(R.string.quick_create_condition_deferred),
            )
        }
    }
    FormFieldLayout(icon = Icons.Outlined.CalendarMonth) {
        DatePickerField(
            value = recurring.endDate,
            label = stringResource(R.string.quick_create_end_date),
            onValueChange = { store.updateRecurring(recurring.copy(endDate = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-recurring-end-date"),
        )
    }
    if (recurring.endDate.isNotBlank()) {
        FormFieldLayout {
            NiaTextButton(
                onClick = { store.updateRecurring(recurring.copy(endDate = "")) },
                leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                text = { Text(stringResource(R.string.quick_create_no_end_date)) },
            )
        }
    }
}
