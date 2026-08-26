/*
 * TimeSubpanel.kt
 *
 * Authoring UI for `when` (None / Day / Range / Reference), time-of-day
 * window (Unspecified / Moment / Range) and the [QuickCreateWindow] list.
 *
 * Rows use the 24dp icon column + 16dp gap reservation provided
 * structurally by `FormFieldLayout`, or bypass it entirely via
 * `ScrollableChipRow` for chrome-less chip batches. The 16dp outer
 * horizontal padding is applied by `FormFieldRow` / `ScrollableChipRow`.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Anchor
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WbSunny
// m2-allow: m3-component
import androidx.compose.material3.FilledTonalButton
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
import app.tastile.android.ui.mobile.components.picker.DatePickerSheet
import app.tastile.android.ui.mobile.components.picker.ReferenceOption
import app.tastile.android.ui.mobile.components.picker.ReferencePickerSheet
import app.tastile.android.ui.mobile.components.picker.TimePickerSheet
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateSpan
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateTimeOfDayMode
import app.tastile.android.ui.mobile.sheets.QuickCreateWhenMode
import app.tastile.android.ui.mobile.sheets.QuickCreateWindow
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
internal fun TimePanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    fun setWhen(mode: QuickCreateWhenMode) {
        val time = draft.time
        store.updateTime(when (mode) {
            QuickCreateWhenMode.None -> time.copy(whenMode = mode, span = QuickCreateSpan(), timeOfDayMode = QuickCreateTimeOfDayMode.Unspecified, timeOfDayStart = "", timeOfDayEnd = "")
            QuickCreateWhenMode.Day -> time.copy(whenMode = mode, span = time.span.copy(end = ""))
            QuickCreateWhenMode.Range -> time.copy(whenMode = mode)
            QuickCreateWhenMode.Reference -> time.copy(whenMode = mode, span = QuickCreateSpan())
        })
    }
    var showStartTime by remember { mutableStateOf(false) }
    var showEndTime by remember { mutableStateOf(false) }
    var showReferencePicker by remember { mutableStateOf(false) }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val referenceOptions = remember(draft.plan.references) {
        draft.plan.references.map { ref ->
            val targetObj = ref.target.jsonObjectOrEmpty()
            val refId = targetObj["referenceId"]?.jsonPrimitive?.content?.takeUnless { it == "null" } ?: ref.id
            ReferenceOption(id = refId, label = ref.id.ifBlank { refId })
        }
    }
    FormFieldLayout {
        FilterChip(
            selected = draft.time.whenMode == QuickCreateWhenMode.None,
            onClick = { setWhen(QuickCreateWhenMode.None) },
            modifier = Modifier.fillMaxWidth().testTag("quick-create-when-none"),
            label = { Text(stringResource(R.string.quickcreate_panel_when_none)) },
            leadingIcon = { Icon(Icons.Outlined.EventBusy, contentDescription = null) },
        )
    }
    LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_when_header))
    val whenModes = listOf(QuickCreateWhenMode.Day, QuickCreateWhenMode.Range, QuickCreateWhenMode.Reference)
    val whenIcon: (QuickCreateWhenMode) -> androidx.compose.ui.graphics.vector.ImageVector = { mode ->
        when (mode) {
            QuickCreateWhenMode.None -> Icons.Outlined.EventBusy
            QuickCreateWhenMode.Day -> Icons.Outlined.Today
            QuickCreateWhenMode.Range -> Icons.Outlined.DateRange
            QuickCreateWhenMode.Reference -> Icons.Outlined.Tag
        }
    }
    ScrollableChipRow {
        whenModes.forEach { mode ->
            FilterChip(
                selected = draft.time.whenMode == mode,
                onClick = { setWhen(mode) },
                label = { Text(mode.name) },
                leadingIcon = { Icon(whenIcon(mode), contentDescription = null) },
                modifier = Modifier.testTag("quick-create-when-${mode.name.lowercase()}"),
            )
        }
    }
    if (draft.time.whenMode == QuickCreateWhenMode.Day || draft.time.whenMode == QuickCreateWhenMode.Range) Column(Modifier.testTag("quick-create-calendar")) {
        NativeDateField(stringResource(R.string.picker_date_start), draft.time.span.start, "quick-create-start") { value -> store.updateTime(draft.time.copy(span = draft.time.span.copy(start = value))) }
        if (draft.time.whenMode == QuickCreateWhenMode.Range) NativeDateField(stringResource(R.string.picker_date_end), draft.time.span.end, "quick-create-end") { value -> store.updateTime(draft.time.copy(span = draft.time.span.copy(end = value))) }
    }
    if (draft.time.whenMode == QuickCreateWhenMode.Reference) Column(Modifier.testTag("quick-create-reference-catalog")) {
        LocalSectionHeader(title = stringResource(R.string.quickcreate_summary_reference_range))
        FormFieldLayout(icon = Icons.Outlined.Tag) {
            LocalPickerField(
                label = stringResource(R.string.picker_reference_label),
                value = draft.time.referenceId.orEmpty().ifBlank { "—" },
                onClick = { showReferencePicker = true },
                modifier = Modifier.fillMaxWidth().testTag("quick-create-reference-id"),
            )
        }
        FormFieldLayout(icon = Icons.Outlined.Tag) {
            UnderlineTextField(
                value = draft.time.referenceLabel,
                onValueChange = { value -> store.updateTime(draft.time.copy(referenceLabel = value)) },
                placeholder = stringResource(R.string.quickcreate_panel_reference_label),
                modifier = Modifier.fillMaxWidth().testTag("quick-create-reference-label"),
            )
        }
    }
    if (draft.time.whenMode != QuickCreateWhenMode.None) {
        LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_time_of_day_header))
        val timeOfDayModes = QuickCreateTimeOfDayMode.entries.toList()
        ScrollableChipRow {
                timeOfDayModes.forEach { mode ->
                    FilterChip(
                        selected = draft.time.timeOfDayMode == mode,
                        onClick = {
                            store.updateTime(
                                if (mode == QuickCreateTimeOfDayMode.Range) draft.time.copy(
                                    timeOfDayMode = mode,
                                    timeOfDayStart = draft.time.timeOfDayStart.ifBlank { "09:00" },
                                    timeOfDayEnd = draft.time.timeOfDayEnd.ifBlank { "18:00" },
                                ) else draft.time.copy(
                                    timeOfDayMode = mode,
                                    timeOfDayStart = "",
                                    timeOfDayEnd = "",
                                ),
                            )
                        },
                        label = { Text(mode.name) },
                        leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                        modifier = Modifier.testTag("quick-create-time-of-day-${mode.name.lowercase()}"),
                    )
                }
            }
        if (draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.Range) {
            FormFieldLayout(icon = Icons.Outlined.AccessTime) {
                LocalPickerField(
                    label = stringResource(R.string.picker_time_start),
                    value = draft.time.timeOfDayStart.ifBlank { "—" },
                    onClick = { showStartTime = true },
                    modifier = Modifier.fillMaxWidth().testTag("quick-create-time-of-day-start"),
                )
            }
            if (showStartTime) {
                TimePickerSheet(
                    initial = runCatching { LocalTime.parse(draft.time.timeOfDayStart, timeFmt) }.getOrElse { LocalTime.of(9, 0) },
                    onConfirm = { time ->
                        store.updateTime(draft.time.copy(timeOfDayStart = time.format(timeFmt)))
                        showStartTime = false
                    },
                    onDismiss = { showStartTime = false },
                    titleRes = R.string.picker_time_start,
                )
            }
            FormFieldLayout(icon = Icons.Outlined.AccessTime) {
                LocalPickerField(
                    label = stringResource(R.string.picker_time_end),
                    value = draft.time.timeOfDayEnd.ifBlank { "—" },
                    onClick = { showEndTime = true },
                    modifier = Modifier.fillMaxWidth().testTag("quick-create-time-of-day-end"),
                )
            }
            if (showEndTime) {
                TimePickerSheet(
                    initial = runCatching { LocalTime.parse(draft.time.timeOfDayEnd, timeFmt) }.getOrElse { LocalTime.of(18, 0) },
                    onConfirm = { time ->
                        store.updateTime(draft.time.copy(timeOfDayEnd = time.format(timeFmt)))
                        showEndTime = false
                    },
                    onDismiss = { showEndTime = false },
                    titleRes = R.string.picker_time_end,
                )
            }
            val quickRanges = listOf(
                Triple(stringResource(R.string.quickcreate_quickrange_morning), "06:00" to "10:00", Icons.Outlined.WbSunny),
                Triple(stringResource(R.string.quickcreate_quickrange_midday), "09:00" to "18:00", Icons.Outlined.LightMode),
                Triple(stringResource(R.string.quickcreate_quickrange_night), "18:00" to "24:00", Icons.Outlined.DarkMode),
            )
            ScrollableChipRow {
                    quickRanges.forEach { (name, range, icon) ->
                        FilterChip(
                            selected = draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.Range &&
                                draft.time.timeOfDayStart == range.first &&
                                draft.time.timeOfDayEnd == range.second,
                            onClick = {
                                store.updateTime(
                                    draft.time.copy(
                                        timeOfDayMode = QuickCreateTimeOfDayMode.Range,
                                        timeOfDayStart = range.first,
                                        timeOfDayEnd = range.second,
                                    ),
                                )
                            },
                            label = { Text(name) },
                            leadingIcon = { Icon(icon, contentDescription = null) },
                            modifier = Modifier.testTag("quick-create-time-quick-$name"),
                        )
                    }
                }
        }
    }
    FormFieldLayout {
        FilledTonalButton(
            onClick = { store.updateWindows(draft.windows + QuickCreateWindow(UUID.randomUUID().toString(), "self", 0, QuickCreateSpan())) },
            modifier = Modifier.fillMaxWidth().testTag("quick-create-add-window"),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.quickcreate_panel_add_window))
        }
    }
    draft.windows.forEachIndexed { index, window ->
        var showWindowStartDate by remember(index) { mutableStateOf(false) }
        var showWindowEndDate by remember(index) { mutableStateOf(false) }
        var showWindowReferencePicker by remember(index) { mutableStateOf(false) }
        LocalSectionHeader(title = stringResource(R.string.quickcreate_panel_window_header, index + 1))
        val windowKinds = listOf(0, 1, 2, 3)
        val windowKindIcons = remember {
            mapOf(
                0 to Icons.Outlined.Anchor,
                1 to Icons.Outlined.Link,
                2 to Icons.Outlined.Schedule,
                3 to Icons.Outlined.Repeat,
            )
        }
        ScrollableChipRow {
                windowKinds.forEach { kind ->
                    FilterChip(
                        selected = window.kind == kind,
                        onClick = { store.updateWindows(draft.windows.replace(index, window.copy(kind = kind))) },
                        label = {
                            val labelRes = when (kind) {
                                0 -> R.string.quickcreate_panel_window_kind_calendar
                                1 -> R.string.quickcreate_panel_window_kind_label_span
                                2 -> R.string.quickcreate_panel_window_kind_parent_span
                                else -> R.string.quickcreate_panel_window_kind_gap
                            }
                            Text(stringResource(labelRes))
                        },
                        leadingIcon = { Icon(windowKindIcons.getValue(kind), contentDescription = null) },
                        modifier = Modifier.testTag("quick-create-window-$index-kind-$kind"),
                    )
                }
            }
        FormFieldLayout(icon = Icons.Outlined.CalendarToday) {
            LocalPickerField(
                label = stringResource(R.string.picker_date_start),
                value = window.bounds.start.ifBlank { "—" },
                onClick = { showWindowStartDate = true },
                modifier = Modifier.fillMaxWidth().testTag("quick-create-window-$index-start"),
            )
        }
        if (showWindowStartDate) {
            DatePickerSheet(
                initial = runCatching { LocalDate.parse(window.bounds.start, dateFmt) }.getOrElse { LocalDate.now() },
                onConfirm = { date ->
                    store.updateWindows(draft.windows.replace(index, window.copy(bounds = window.bounds.copy(start = date.format(dateFmt)))))
                    showWindowStartDate = false
                },
                onDismiss = { showWindowStartDate = false },
                titleRes = R.string.picker_date_start,
            )
        }
        FormFieldLayout(icon = Icons.Outlined.CalendarToday) {
            LocalPickerField(
                label = stringResource(R.string.picker_date_end),
                value = window.bounds.end.ifBlank { "—" },
                onClick = { showWindowEndDate = true },
                modifier = Modifier.fillMaxWidth().testTag("quick-create-window-$index-end"),
            )
        }
        if (showWindowEndDate) {
            DatePickerSheet(
                initial = runCatching { LocalDate.parse(window.bounds.end, dateFmt) }.getOrElse { LocalDate.now() },
                onConfirm = { date ->
                    store.updateWindows(draft.windows.replace(index, window.copy(bounds = window.bounds.copy(end = date.format(dateFmt)))))
                    showWindowEndDate = false
                },
                onDismiss = { showWindowEndDate = false },
                titleRes = R.string.picker_date_end,
            )
        }
        if (window.kind in 1..3) {
            FormFieldLayout(icon = Icons.Outlined.Tag) {
                LocalPickerField(
                    label = stringResource(R.string.picker_reference_label),
                    value = window.referenceId.orEmpty().ifBlank { "—" },
                    onClick = { showWindowReferencePicker = true },
                    modifier = Modifier.fillMaxWidth().testTag("quick-create-window-$index-reference"),
                )
            }
            if (showWindowReferencePicker) {
                ReferencePickerSheet(
                    references = referenceOptions,
                    onSelect = { option ->
                        store.updateWindows(draft.windows.replace(index, window.copy(referenceId = option.id)))
                        showWindowReferencePicker = false
                    },
                    onDismiss = { showWindowReferencePicker = false },
                )
            }
        }
        FormFieldLayout {
            FilledTonalButton(
                onClick = { store.updateWindows(draft.windows.filterIndexed { item, _ -> item != index }) },
                modifier = Modifier.fillMaxWidth().testTag("quick-create-window-$index-remove"),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.quickcreate_panel_remove_window))
            }
        }
    }
    if (showReferencePicker) {
        ReferencePickerSheet(
            references = referenceOptions,
            onSelect = { option ->
                store.updateTime(draft.time.copy(referenceId = option.id))
                showReferencePicker = false
            },
            onDismiss = { showReferencePicker = false },
        )
    }
}
