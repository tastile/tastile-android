/*
 * ScheduleSubpanel.kt
 *
 * Schedule-authoring subpanel for the four new `SourceScheduleDefinition`
 * fields wired in the v1 source-tile envelope:
 *
 * 1. [QuickCreateSchedule.priority] (i32, 0..10) — numeric stepper.
 * 2. [QuickCreateSchedule.splitPolicyKind] (0=unsplit / 1=split) +
 *    min/max_segment_ms + max_segments — FilterChip kind + numeric inputs.
 * 3. [QuickCreateSchedule.offsetMin] (i32 UTC minutes) — numeric stepper.
 * 4. [QuickCreateSchedule.excludedDates] (list of ISO-8601 dates) — date picker
 *    add + remove chip row.
 *
 * Rows use the 24dp icon column + 16dp gap reservation provided
 * structurally by `FormFieldLayout`, or bypass it entirely via
 * `ScrollableChipRow` for chrome-less chip batches. The 16dp outer
 * horizontal padding is applied by `FormFieldRow` / `ScrollableChipRow`.
 *
 * See `docs/ux-fix-v1-source-tile-wiring.md` for the wire mapping.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Timer
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.ui.mobile.components.picker.DatePickerSheet
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun SchedulePanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val schedule = draft.schedule
    val dateFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // 1. Priority (0..10).
    LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_schedule_priority_header))
    FormFieldLayout(icon = Icons.Outlined.Star) {
        LocalNumberField(
            value = schedule.priority.toString(),
            onValueChange = { value ->
                val coerced = value.toIntOrNull()?.coerceIn(0, 10) ?: schedule.priority
                store.updateSchedule(schedule.copy(priority = coerced))
            },
            label = stringResource(R.string.quickcreate_panel_schedule_priority_label),
            suffix = stringResource(R.string.quickcreate_panel_schedule_priority_suffix),
            min = 0,
            max = 10,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("schedule-priority"),
        )
    }

    // 2. Split policy.
    LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_schedule_split_header))
    val splitPolicyKinds = listOf(
        0 to stringResource(R.string.quickcreate_panel_schedule_split_unsplit),
        1 to stringResource(R.string.quickcreate_panel_schedule_split_split),
    )
    val splitPolicyIcons = remember {
        mapOf(0 to Icons.Outlined.Layers, 1 to Icons.Outlined.Timeline)
    }
    ScrollableChipRow {
        splitPolicyKinds.forEach { (kind, label) ->
            FilterChip(
                selected = schedule.splitPolicyKind.toInt() == kind,
                onClick = { store.updateSchedule(schedule.copy(splitPolicyKind = kind.toShort())) },
                label = { Text(label) },
                leadingIcon = { Icon(splitPolicyIcons.getValue(kind), contentDescription = null) },
                modifier = Modifier.testTag("schedule-split-kind-$kind"),
            )
        }
    }
    if (schedule.splitPolicyKind.toInt() == 1) {
        FormFieldLayout(icon = Icons.Outlined.Timer) {
            LocalNumberField(
                value = schedule.splitPolicyMinSegmentMs.toString(),
                onValueChange = { value ->
                    val next = value.toLongOrNull()?.coerceAtLeast(0L) ?: schedule.splitPolicyMinSegmentMs
                    store.updateSchedule(schedule.copy(splitPolicyMinSegmentMs = next))
                },
                label = stringResource(R.string.quickcreate_panel_schedule_min_segment),
                suffix = stringResource(R.string.quickcreate_panel_schedule_min_segment_suffix),
                min = 0,
                step = 60_000,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule-split-min-segment"),
            )
        }
        FormFieldLayout(icon = Icons.Outlined.Timer) {
            LocalNumberField(
                value = if (schedule.splitPolicyMaxSegmentMs == Long.MAX_VALUE) "" else schedule.splitPolicyMaxSegmentMs.toString(),
                onValueChange = { value ->
                    val next = value.toLongOrNull()?.coerceAtLeast(0L) ?: Long.MAX_VALUE
                    store.updateSchedule(schedule.copy(splitPolicyMaxSegmentMs = next))
                },
                label = stringResource(R.string.quickcreate_panel_schedule_max_segment),
                suffix = stringResource(R.string.quickcreate_panel_schedule_min_segment_suffix),
                min = 0,
                step = 60_000,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule-split-max-segment"),
            )
        }
        FormFieldLayout(icon = Icons.Outlined.FormatListBulleted) {
            LocalNumberField(
                value = schedule.splitPolicyMaxSegments.toString(),
                onValueChange = { value ->
                    val next = value.toIntOrNull()?.coerceAtLeast(1) ?: schedule.splitPolicyMaxSegments
                    store.updateSchedule(schedule.copy(splitPolicyMaxSegments = next))
                },
                label = stringResource(R.string.quickcreate_panel_schedule_max_segments),
                suffix = stringResource(R.string.quickcreate_panel_schedule_max_segments_suffix),
                min = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule-split-max-segments"),
            )
        }
    }

    // 3. Offset minutes (UTC offset east of UTC; default 0 = UTC).
    LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_schedule_offset_header))
    FormFieldLayout(icon = Icons.Outlined.Schedule) {
        LocalNumberField(
            value = schedule.offsetMin.toString(),
            onValueChange = { value ->
                // Accept integer; clamp to ±12h for safety; default to 0 if blank/invalid.
                val next = value.toIntOrNull()?.coerceIn(-720, 720) ?: 0
                store.updateSchedule(schedule.copy(offsetMin = next))
            },
            label = stringResource(R.string.quickcreate_panel_schedule_offset_label),
            suffix = stringResource(R.string.quickcreate_panel_schedule_offset_suffix),
            min = -720,
            max = 720,
            step = 15,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("schedule-offset-min"),
        )
    }

    // 4. Excluded dates (ISO yyyy-MM-dd).
    LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_schedule_excluded_header))
    ScrollableChipRow {
        schedule.excludedDates.forEach { isoDate ->
            FilterChip(
                selected = true,
                onClick = {
                    store.updateSchedule(schedule.copy(excludedDates = schedule.excludedDates - isoDate))
                },
                label = { Text(stringResource(R.string.quickcreate_panel_schedule_excluded_chip_remove, isoDate)) },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                modifier = Modifier.testTag("schedule-excluded-date-$isoDate"),
            )
        }
    }
    var showExcludedDatePicker by remember { mutableStateOf(false) }
    FormFieldLayout {
        NiaFilledTonalButton(
            onClick = { showExcludedDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("schedule-add-excluded-date"),
            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.quickcreate_panel_schedule_add_excluded)) },
        )
    }
    if (showExcludedDatePicker) {
        DatePickerSheet(
            initial = LocalDate.now(),
            onConfirm = { date ->
                val iso = date.format(dateFmt)
                if (iso !in schedule.excludedDates) {
                    store.updateSchedule(schedule.copy(excludedDates = schedule.excludedDates + iso))
                }
                showExcludedDatePicker = false
            },
            onDismiss = { showExcludedDatePicker = false },
            titleRes = R.string.picker_date_start,
        )
    }
}
