package app.tastile.android.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: m3-component
import androidx.compose.material3.Button
// m2-allow: m3-component
import androidx.compose.material3.DatePicker
// m2-allow: m3-component
import androidx.compose.material3.DatePickerDialog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: m3-component
import androidx.compose.material3.SegmentedButtonDefaults
// m2-allow: m3-component
import androidx.compose.material3.SegmentedButton
// m2-allow: m3-component
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
// m2-allow: m3-component
import androidx.compose.material3.Switch
// m2-allow: m3-component
import androidx.compose.material3.Tab
// m2-allow: m3-component
import androidx.compose.material3.PrimaryTabRow
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.TextButton
// m2-allow: m3-component
import androidx.compose.material3.TimePicker
// m2-allow: m3-component
import androidx.compose.material3.rememberDatePickerState
// m2-allow: m3-component
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.model.Tile
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.ui.dashboard.components.AutoCompleteTextField
import app.tastile.android.ui.dashboard.components.DurationInput
import app.tastile.android.ui.dashboard.components.SectionBlock
import app.tastile.android.ui.util.combineDateTimeToUtcIso
import app.tastile.android.ui.util.formatDateShort
import app.tastile.android.ui.util.formatDuration
import app.tastile.android.ui.util.getCurrentLocalDate
import app.tastile.android.ui.util.getCurrentLocalTime
import app.tastile.android.ui.util.getLocalTimeAfterMinutes
import app.tastile.android.ui.util.localDateFromEpochMillis
import app.tastile.android.ui.util.parseBoundedDurationMinutes
import app.tastile.android.ui.util.parseDateTime
import app.tastile.android.ui.util.parseDurationToMinutes
import app.tastile.android.ui.util.parseNonNegativeInt
import app.tastile.android.ui.util.parseTimeToMinutes
import app.tastile.android.ui.util.sanitizeNumericInput
import app.tastile.android.ui.util.validateQuickCreate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.max
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickCreateSheet(
    viewModel: DashboardViewModel,
    onClose: () -> Unit
) {
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf("") }
    var titleEdited by rememberSaveable { mutableStateOf(false) }
    var tileKind by rememberSaveable { mutableStateOf("work") }
    var objectiveMode by rememberSaveable { mutableStateOf("finish_once") }

    var useStartAt by rememberSaveable { mutableStateOf(false) }
    var useEndAt by rememberSaveable { mutableStateOf(false) }
    var startDate by rememberSaveable { mutableStateOf(getCurrentLocalDate()) }
    var startTime by rememberSaveable { mutableStateOf(getCurrentLocalTime()) }
    var endDate by rememberSaveable { mutableStateOf(getCurrentLocalDate()) }
    var endTime by rememberSaveable { mutableStateOf(getLocalTimeAfterMinutes(60)) }

    var recurrenceFrequency by rememberSaveable { mutableStateOf("daily") }
    var recurrenceIntervalInput by rememberSaveable { mutableStateOf("1") }
    var recurrenceWeekdaysCsv by rememberSaveable { mutableStateOf("1") }
    var recurrenceMonthlyWeekInput by rememberSaveable { mutableStateOf("1") }
    var recurrenceMonthlyWeekdayInput by rememberSaveable { mutableStateOf("1") }
    var recurrenceStartTime by rememberSaveable { mutableStateOf(getCurrentLocalTime()) }
    var recurrenceEndTime by rememberSaveable { mutableStateOf(getLocalTimeAfterMinutes(60)) }
    var recurrenceValidFromEnabled by rememberSaveable { mutableStateOf(false) }
    var recurrenceValidToEnabled by rememberSaveable { mutableStateOf(false) }
    var recurrenceValidFromDate by rememberSaveable { mutableStateOf(getCurrentLocalDate()) }
    var recurrenceValidToDate by rememberSaveable { mutableStateOf(getCurrentLocalDate()) }

    var workHours by rememberSaveable { mutableStateOf("0") }
    var workMinutes by rememberSaveable { mutableStateOf("25") }
    var durationManuallyEdited by rememberSaveable { mutableStateOf(false) }
    var breakSplitsWork by rememberSaveable { mutableStateOf(true) }

    var project by rememberSaveable { mutableStateOf("") }
    var tagDraft by rememberSaveable { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    var memo by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var submitting by rememberSaveable { mutableStateOf(false) }

    val errTemporalOrder = stringResource(R.string.quick_create_error_temporal_order)
    val errWorkDuration = stringResource(R.string.quick_create_error_work_duration)
    val errRecurrence = stringResource(R.string.quick_create_error_recurrence_interval)
    val errTitleRequired = stringResource(R.string.quick_create_error_title_required)
    val nextActionLabel = stringResource(R.string.quick_create_next_action_label)
    val nextActionWork = stringResource(R.string.quick_create_next_action_work)

    val workTargetMin = parseDurationToMinutes(workHours, workMinutes)
    val boundedDurationMin = parseBoundedDurationMinutes(startDate, startTime, endDate, endTime)
    val recurrenceStartOffsetMin = parseTimeToMinutes(recurrenceStartTime)
    val recurrenceEndOffsetMin = parseTimeToMinutes(recurrenceEndTime)
    val recurringWindowDurationMin =
        if (recurrenceStartOffsetMin != null && recurrenceEndOffsetMin != null && recurrenceEndOffsetMin > recurrenceStartOffsetMin) {
            recurrenceEndOffsetMin - recurrenceStartOffsetMin
        } else {
            null
        }
    val effectiveDurationMin =
        if (!durationManuallyEdited && recurringWindowDurationMin != null) {
            recurringWindowDurationMin
        } else if (!durationManuallyEdited && boundedDurationMin != null) {
            boundedDurationMin
        } else {
            workTargetMin ?: boundedDurationMin
        }
    val workTargetText = effectiveDurationMin?.let { formatDuration(it, locale, context) }

    val startDateTime = if (useStartAt) parseDateTime(startDate, startTime) else null
    val endDateTime = if (useEndAt) parseDateTime(endDate, endTime) else null
    val hasAnyTemporalConstraint = startDateTime != null || endDateTime != null
    val isRecurring = objectiveMode == "recurring"
    val recurrenceInterval = parseNonNegativeInt(recurrenceIntervalInput) ?: 0
    val recurrenceWindowValid =
        recurrenceStartOffsetMin == null || recurrenceEndOffsetMin == null || recurrenceEndOffsetMin > recurrenceStartOffsetMin
    val temporalOrderValid =
        if (isRecurring) recurrenceWindowValid else (startDateTime == null || endDateTime == null || endDateTime.isAfter(startDateTime))
    val validation = validateQuickCreate(
        tileKind = tileKind,
        objectiveMode = objectiveMode,
        hasAnyTemporalConstraint = hasAnyTemporalConstraint,
        workTargetMin = workTargetMin,
        temporalOrderValid = temporalOrderValid,
        recurrenceInterval = recurrenceInterval
    )

    val suggestedTitle = when {
        tileKind == "label" -> stringResource(R.string.quick_create_period_label)
        objectiveMode == "recurring" && workTargetText != null ->
            stringResource(R.string.quick_create_suggested_recurring_with_duration, workTargetText)
        objectiveMode == "recurring" ->
            stringResource(R.string.quick_create_suggested_recurring)
        objectiveMode == "maximize_within_interval" && startDateTime != null && endDateTime != null ->
            stringResource(
                R.string.quick_create_suggested_maximize_range,
                formatDateShort(startDateTime, locale),
                formatDateShort(endDateTime, locale),
            )
        objectiveMode == "maximize_within_interval" ->
            stringResource(R.string.quick_create_suggested_maximize)
        workTargetText != null ->
            stringResource(R.string.quick_create_suggested_task_with_duration, workTargetText)
        else -> stringResource(R.string.quick_create_suggested_task)
    }

    val doneDefinition = when {
        tileKind == "label" -> stringResource(R.string.quick_create_done_label_period)
        objectiveMode == "recurring" -> stringResource(R.string.quick_create_done_recurring)
        objectiveMode == "maximize_within_interval" && startDateTime != null && endDateTime != null ->
            stringResource(
                R.string.quick_create_done_maximize_range,
                formatDateShort(startDateTime, locale),
                formatDateShort(endDateTime, locale),
            )
        objectiveMode == "maximize_within_interval" ->
            stringResource(R.string.quick_create_suggested_maximize)
        workTargetText != null ->
            stringResource(R.string.quick_create_done_with_duration, workTargetText)
        else -> stringResource(R.string.quick_create_done_one_run)
    }

    val (existingProjects, existingTags) = remember(tiles) { deriveProjectAndTags(tiles) }
    val projectSuggestions = remember(existingProjects, project) {
        existingProjects
            .filter { it.contains(project.trim(), ignoreCase = true) }
            .take(8)
    }
    val tagSuggestions = remember(existingTags, tagDraft, selectedTags) {
        existingTags
            .filter { it.contains(tagDraft.trim(), ignoreCase = true) }
            .filter { suggestion -> selectedTags.none { it.equals(suggestion, ignoreCase = true) } }
            .take(8)
    }

    LaunchedEffect(suggestedTitle, titleEdited) {
        if (!titleEdited) {
            title = suggestedTitle
        }
    }

    LaunchedEffect(boundedDurationMin, recurringWindowDurationMin, durationManuallyEdited) {
        val auto = recurringWindowDurationMin ?: boundedDurationMin
        if (!durationManuallyEdited && auto != null && auto > 0) {
            workHours = (auto / 60).toString()
            workMinutes = (auto % 60).toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.quick_create_title),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.quick_create_close))
            }
        }

        SectionBlock(
            title = stringResource(R.string.quick_create_section_title),
            helpText = stringResource(R.string.quick_create_section_title_help)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleEdited = true
                },
                placeholder = { Text(suggestedTitle) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        SectionBlock {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = tileKind == "work",
                    onClick = { tileKind = "work" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text(stringResource(R.string.quick_create_kind_work)) }
                SegmentedButton(
                    selected = tileKind == "label",
                    onClick = { tileKind = "label" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text(stringResource(R.string.quick_create_kind_label)) }
            }
        }

        SectionBlock {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = objectiveMode == "finish_once",
                    onClick = { objectiveMode = "finish_once" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Text(stringResource(R.string.quick_create_mode_finish_once)) }
                SegmentedButton(
                    selected = objectiveMode == "recurring",
                    onClick = { objectiveMode = "recurring" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Text(stringResource(R.string.quick_create_mode_recurring)) }
                SegmentedButton(
                    selected = objectiveMode == "maximize_within_interval",
                    onClick = { objectiveMode = "maximize_within_interval" },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Text(stringResource(R.string.quick_create_mode_maximize)) }
            }
        }

        if (isRecurring) {
            SectionBlock(
                title = stringResource(R.string.quick_create_recurrence_section_title),
                helpText = stringResource(R.string.quick_create_recurrence_section_help)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = when (recurrenceFrequency) {
                        "daily" -> 0
                        "weekly" -> 1
                        "monthly" -> 2
                        else -> 0
                    }
                ) {
                    Tab(
                        selected = recurrenceFrequency == "daily",
                        onClick = { recurrenceFrequency = "daily" },
                        text = { Text(stringResource(R.string.quick_create_recurrence_daily)) }
                    )
                    Tab(
                        selected = recurrenceFrequency == "weekly",
                        onClick = { recurrenceFrequency = "weekly" },
                        text = { Text(stringResource(R.string.quick_create_recurrence_weekly)) }
                    )
                    Tab(
                        selected = recurrenceFrequency == "monthly",
                        onClick = { recurrenceFrequency = "monthly" },
                        text = { Text(stringResource(R.string.quick_create_recurrence_monthly)) }
                    )
                }

                OutlinedTextField(
                    value = recurrenceIntervalInput,
                    onValueChange = { recurrenceIntervalInput = sanitizeNumericInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.quick_create_recurrence_interval_label)) },
                    singleLine = true
                )

                if (recurrenceFrequency == "weekly") {
                    OutlinedTextField(
                        value = recurrenceWeekdaysCsv,
                        onValueChange = { recurrenceWeekdaysCsv = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.quick_create_recurrence_weekdays_label)) },
                        singleLine = true
                    )
                }

                if (recurrenceFrequency == "monthly") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = recurrenceMonthlyWeekInput,
                            onValueChange = { recurrenceMonthlyWeekInput = sanitizeNumericInput(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.quick_create_recurrence_monthly_week_label)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = recurrenceMonthlyWeekdayInput,
                            onValueChange = { recurrenceMonthlyWeekdayInput = sanitizeNumericInput(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.quick_create_recurrence_monthly_weekday_label)) },
                            singleLine = true
                        )
                    }
                }

                OutlinedTextField(
                    value = recurrenceStartTime,
                    onValueChange = { recurrenceStartTime = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.quick_create_recurrence_start_time_label)) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = recurrenceEndTime,
                    onValueChange = { recurrenceEndTime = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.quick_create_recurrence_end_time_label)) },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.quick_create_recurrence_valid_from_label))
                    Switch(
                        checked = recurrenceValidFromEnabled,
                        onCheckedChange = { recurrenceValidFromEnabled = it }
                    )
                }

                if (recurrenceValidFromEnabled) {
                    DateTimeField(
                        label = stringResource(R.string.quick_create_recurrence_valid_from_date_label),
                        date = recurrenceValidFromDate,
                        time = "00:00",
                        onDateChange = { recurrenceValidFromDate = it },
                        onTimeChange = {},
                        locale = locale,
                        timeEditable = false
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.quick_create_recurrence_valid_to_label))
                    Switch(
                        checked = recurrenceValidToEnabled,
                        onCheckedChange = { recurrenceValidToEnabled = it }
                    )
                }

                if (recurrenceValidToEnabled) {
                    DateTimeField(
                        label = stringResource(R.string.quick_create_recurrence_valid_to_date_label),
                        date = recurrenceValidToDate,
                        time = "23:59",
                        onDateChange = { recurrenceValidToDate = it },
                        onTimeChange = {},
                        locale = locale,
                        timeEditable = false
                    )
                }
            }
        }

        SectionBlock(
            title = stringResource(R.string.quick_create_schedule_section_title),
            helpText = stringResource(R.string.quick_create_schedule_section_help)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.quick_create_schedule_start_label))
                Switch(checked = useStartAt, onCheckedChange = { useStartAt = it })
            }
            if (useStartAt) {
                DateTimeField(
                    label = stringResource(R.string.quick_create_schedule_start_label),
                    date = startDate,
                    time = startTime,
                    onDateChange = { startDate = it },
                    onTimeChange = { startTime = it },
                    locale = locale
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.quick_create_schedule_end_label))
                Switch(checked = useEndAt, onCheckedChange = { useEndAt = it })
            }
            if (useEndAt) {
                DateTimeField(
                    label = stringResource(R.string.quick_create_schedule_end_label),
                    date = endDate,
                    time = endTime,
                    onDateChange = { endDate = it },
                    onTimeChange = { endTime = it },
                    locale = locale
                )
            }
        }

        if (tileKind == "work") {
            SectionBlock(
                title = stringResource(R.string.quick_create_work_duration_section_title),
                helpText = stringResource(R.string.quick_create_work_duration_section_help)
            ) {
                DurationInput(
                    hours = workHours,
                    minutes = workMinutes,
                    title = stringResource(R.string.quick_create_work_duration_section_title),
                    onHoursChange = { workHours = it },
                    onMinutesChange = { workMinutes = it },
                    onManualEdit = { durationManuallyEdited = true }
                )
            }
        }

        if (tileKind == "work") {
            SectionBlock(
                title = stringResource(R.string.quick_create_break_section_title),
                helpText = stringResource(R.string.quick_create_break_section_help)
            ) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = breakSplitsWork,
                        onClick = { breakSplitsWork = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text(stringResource(R.string.quick_create_break_allow_split)) }
                    SegmentedButton(
                        selected = !breakSplitsWork,
                        onClick = { breakSplitsWork = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text(stringResource(R.string.quick_create_break_keep_continuous)) }
                }
            }
        }

        SectionBlock(
            title = stringResource(R.string.quick_create_meta_section_title),
            helpText = stringResource(R.string.quick_create_meta_section_help)
        ) {
            AutoCompleteTextField(
                value = project,
                onValueChange = { project = it },
                suggestions = projectSuggestions,
                placeholder = stringResource(R.string.quick_create_project_placeholder),
                onSuggestionSelected = { project = it }
            )

            AutoCompleteTextField(
                value = tagDraft,
                onValueChange = { tagDraft = it },
                suggestions = tagSuggestions,
                placeholder = stringResource(R.string.quick_create_tag_placeholder),
                onSuggestionSelected = { suggestion ->
                    if (selectedTags.none { it.equals(suggestion, ignoreCase = true) }) {
                        selectedTags.add(suggestion)
                    }
                    tagDraft = ""
                }
            )

            if (selectedTags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedTags.forEach { tag ->
                        FilterChip(
                            selected = true,
                            onClick = { selectedTags.remove(tag) },
                            label = { Text("#$tag ×") }
                        )
                    }
                }
            }
        }

        SectionBlock(
            title = stringResource(R.string.quick_create_memo_section_title),
            helpText = stringResource(R.string.quick_create_memo_section_help)
        ) {
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                placeholder = { Text(stringResource(R.string.quick_create_memo_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        SectionBlock(
            title = stringResource(R.string.quick_create_done_section_title),
            helpText = stringResource(R.string.quick_create_done_section_help)
        ) {
            OutlinedTextField(
                value = doneDefinition,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true
            )
        }

        if (error != null) {
            Text(
                text = error ?: "",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.quick_create_cancel))
            }
            Button(
                onClick = {
                    error = null
                    if (!validation.temporalOrderValid) {
                        error = errTemporalOrder
                        return@Button
                    }
                    if (!validation.durationReady) {
                        error = errWorkDuration
                        return@Button
                    }
                    if (!validation.recurrenceReady) {
                        error = errRecurrence
                        return@Button
                    }
                    if (title.trim().isBlank()) {
                        error = errTitleRequired
                        return@Button
                    }

                    submitting = true
                    viewModel.createTile(
                        CreateTileDraft(
                            title = title.trim(),
                            nextAction = memo.trim().ifBlank {
                                if (tileKind == "label")
                                    nextActionLabel
                                else
                                    nextActionWork
                            },
                            doneDefinition = doneDefinition,
                            tileKind = tileKind,
                            objectiveMode = objectiveMode,
                            useStartAt = useStartAt,
                            useEndAt = useEndAt,
                            startAtIso = if (useStartAt) combineDateTimeToUtcIso(startDate, startTime) else null,
                            endAtIso = if (useEndAt) combineDateTimeToUtcIso(endDate, endTime) else null,
                            recurrenceFrequency = recurrenceFrequency,
                            recurrenceInterval = max(1, recurrenceInterval),
                            recurrenceWeekdays = recurrenceWeekdaysCsv
                                .split(",")
                                .mapNotNull { it.trim().toIntOrNull() }
                                .map { it.coerceIn(0, 6) },
                            recurrenceMonthlyWeek = max(1, recurrenceMonthlyWeekInput.toIntOrNull() ?: 1),
                            recurrenceMonthlyWeekday = (recurrenceMonthlyWeekdayInput.toIntOrNull() ?: 0).coerceIn(0, 6),
                            recurrenceStartTime = recurrenceStartTime,
                            recurrenceEndTime = recurrenceEndTime,
                            recurrenceValidFromIso = if (recurrenceValidFromEnabled) combineDateTimeToUtcIso(recurrenceValidFromDate, "00:00") else null,
                            recurrenceValidToIso = if (recurrenceValidToEnabled) combineDateTimeToUtcIso(recurrenceValidToDate, "23:59") else null,
                            breakSplitsWork = breakSplitsWork,
                            project = project.trim().ifBlank { null },
                            labels = selectedTags.toList(),
                            memo = memo.trim().ifBlank { null },
                            targetWorkMin = if (tileKind == "work") effectiveDurationMin else null
                        )
                    )
                    submitting = false
                    onClose()
                },
                enabled = validation.canSubmit && title.trim().isNotEmpty() && !submitting
            ) {
                Text(stringResource(R.string.quick_create_create))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeField(
    label: String,
    date: String,
    time: String,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    locale: AppLocale,
    timeEditable: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val parsedHour = time.split(":").getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val parsedMinute = time.split(":").getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = date,
                onValueChange = {},
                modifier = Modifier.weight(1f),
                readOnly = true,
                singleLine = true
            )
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.quick_create_select_date))
            }
        }
        if (timeEditable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = time,
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    singleLine = true
                )
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = stringResource(R.string.quick_create_select_time))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            onDateChange(localDateFromEpochMillis(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.date_picker_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = parsedHour,
            initialMinute = parsedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.date_picker_cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

private fun deriveProjectAndTags(tiles: List<Tile>): Pair<List<String>, List<String>> {
    val projectSet = linkedSetOf<String>()
    val tagSet = linkedSetOf<String>()
    tiles.forEach { tile ->
        extractLabels(tile.annotationConditions).forEach { label ->
            if (label.startsWith("project:")) {
                val project = label.removePrefix("project:").trim()
                if (project.isNotBlank()) projectSet.add(project)
            } else if (label.isNotBlank()) {
                tagSet.add(label.trim())
            }
        }
    }
    return projectSet.sortedWith(String.CASE_INSENSITIVE_ORDER) to tagSet.sortedWith(String.CASE_INSENSITIVE_ORDER)
}

private fun extractLabels(annotationConditions: JsonObject?): List<String> {
    val labels = annotationConditions?.get("labels")?.jsonArray ?: JsonArray(emptyList())
    return labels.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() }.filter { it.isNotEmpty() }
}
