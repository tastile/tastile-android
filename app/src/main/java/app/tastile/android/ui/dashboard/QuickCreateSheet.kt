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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun QuickCreateSheet(
    viewModel: DashboardViewModel,
    onClose: () -> Unit
) {
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    fun t(ja: String, en: String): String = if (locale == AppLocale.JA) ja else en

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
    val workTargetText = effectiveDurationMin?.let { formatDuration(it, locale) }

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

    val suggestedTitle = remember(
        tileKind,
        objectiveMode,
        startDateTime,
        endDateTime,
        workTargetText,
        locale
    ) {
        when {
            tileKind == "label" -> t("期間ラベル", "Period label")
            objectiveMode == "recurring" && workTargetText != null -> t("定期タスク $workTargetText", "Recurring task $workTargetText")
            objectiveMode == "recurring" -> t("定期タスク", "Recurring task")
            objectiveMode == "maximize_within_interval" && startDateTime != null && endDateTime != null ->
                if (locale == AppLocale.JA) {
                    "${formatDateShort(startDateTime, locale)} - ${formatDateShort(endDateTime, locale)} で最大化"
                } else {
                    "Maximize in ${formatDateShort(startDateTime, locale)} - ${formatDateShort(endDateTime, locale)}"
                }
            objectiveMode == "maximize_within_interval" -> t("できる限り進める", "Maximize progress")
            workTargetText != null -> t("作業 $workTargetText", "Task $workTargetText")
            else -> t("作業タスク", "Task")
        }
    }

    val doneDefinition = remember(
        tileKind,
        objectiveMode,
        workTargetText,
        startDateTime,
        endDateTime,
        locale
    ) {
        when {
            tileKind == "label" -> t("指定した期間のラベル付けを完了", "Complete labeling for the selected period")
            objectiveMode == "recurring" -> t("1サイクル実行したら完了（定期）", "Complete one cycle (recurring)")
            objectiveMode == "maximize_within_interval" && startDateTime != null && endDateTime != null ->
                if (locale == AppLocale.JA) {
                    "${formatDateShort(startDateTime, locale)} から ${formatDateShort(endDateTime, locale)} の間で最大化"
                } else {
                    "Maximize progress from ${formatDateShort(startDateTime, locale)} to ${formatDateShort(endDateTime, locale)}"
                }
            objectiveMode == "maximize_within_interval" -> t("できる限り進める", "Maximize progress")
            workTargetText != null -> t("${workTargetText}の実行を完了", "Complete $workTargetText of work")
            else -> t("1回の実行を完了", "Complete one run")
        }
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
                t("クイック作成", "Quick Create"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = t("閉じる", "Close"))
            }
        }

        SectionBlock(
            title = t("タイトル", "Title"),
            helpText = t("タスク名を入力してください", "Enter task title")
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
                ) { Text(t("作業", "Work")) }
                SegmentedButton(
                    selected = tileKind == "label",
                    onClick = { tileKind = "label" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text(t("ラベル", "Label")) }
            }
        }

        SectionBlock {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = objectiveMode == "finish_once",
                    onClick = { objectiveMode = "finish_once" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Text(t("単発", "Finish once")) }
                SegmentedButton(
                    selected = objectiveMode == "recurring",
                    onClick = { objectiveMode = "recurring" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Text(t("繰返し", "Recurring")) }
                SegmentedButton(
                    selected = objectiveMode == "maximize_within_interval",
                    onClick = { objectiveMode = "maximize_within_interval" },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Text(t("最大化", "Maximize")) }
            }
        }

        if (isRecurring) {
            SectionBlock(
                title = t("繰り返し設定", "Recurrence"),
                helpText = t("繰り返しの頻度と時間を設定", "Configure frequency and recurrence window")
            ) {
                TabRow(
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
                        text = { Text(t("毎日", "Daily")) }
                    )
                    Tab(
                        selected = recurrenceFrequency == "weekly",
                        onClick = { recurrenceFrequency = "weekly" },
                        text = { Text(t("毎週", "Weekly")) }
                    )
                    Tab(
                        selected = recurrenceFrequency == "monthly",
                        onClick = { recurrenceFrequency = "monthly" },
                        text = { Text(t("毎月", "Monthly")) }
                    )
                }

                OutlinedTextField(
                    value = recurrenceIntervalInput,
                    onValueChange = { recurrenceIntervalInput = sanitizeNumericInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t("間隔", "Interval")) },
                    singleLine = true
                )

                if (recurrenceFrequency == "weekly") {
                    OutlinedTextField(
                        value = recurrenceWeekdaysCsv,
                        onValueChange = { recurrenceWeekdaysCsv = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(t("曜日 (0-6, カンマ区切り)", "Weekdays (0-6, comma separated)")) },
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
                            label = { Text(t("第何週", "Week")) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = recurrenceMonthlyWeekdayInput,
                            onValueChange = { recurrenceMonthlyWeekdayInput = sanitizeNumericInput(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(t("曜日 0-6", "Weekday 0-6")) },
                            singleLine = true
                        )
                    }
                }

                OutlinedTextField(
                    value = recurrenceStartTime,
                    onValueChange = { recurrenceStartTime = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t("繰り返し開始時刻 (HH:mm)", "Recurrence start time (HH:mm)")) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = recurrenceEndTime,
                    onValueChange = { recurrenceEndTime = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t("繰り返し終了時刻 (HH:mm)", "Recurrence end time (HH:mm)")) },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t("有効開始日", "Valid from"))
                    Switch(
                        checked = recurrenceValidFromEnabled,
                        onCheckedChange = { recurrenceValidFromEnabled = it }
                    )
                }

                if (recurrenceValidFromEnabled) {
                    DateTimeField(
                        label = t("有効開始日", "Valid from date"),
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
                    Text(t("有効終了日", "Valid to"))
                    Switch(
                        checked = recurrenceValidToEnabled,
                        onCheckedChange = { recurrenceValidToEnabled = it }
                    )
                }

                if (recurrenceValidToEnabled) {
                    DateTimeField(
                        label = t("有効終了日", "Valid to date"),
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
            title = t("スケジュール", "Schedule"),
            helpText = t("開始・終了日時を設定", "Set start/end date and time")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(t("開始日時", "Start at"))
                Switch(checked = useStartAt, onCheckedChange = { useStartAt = it })
            }
            if (useStartAt) {
                DateTimeField(
                    label = t("開始日時", "Start at"),
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
                Text(t("終了日時", "End at"))
                Switch(checked = useEndAt, onCheckedChange = { useEndAt = it })
            }
            if (useEndAt) {
                DateTimeField(
                    label = t("終了日時", "End at"),
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
                title = t("作業時間", "Work duration"),
                helpText = t("目標作業時間を設定", "Set target work duration")
            ) {
                DurationInput(
                    hours = workHours,
                    minutes = workMinutes,
                    title = t("作業時間", "Work duration"),
                    onHoursChange = { workHours = it },
                    onMinutesChange = { workMinutes = it },
                    onManualEdit = { durationManuallyEdited = true }
                )
            }
        }

        if (tileKind == "work") {
            SectionBlock(
                title = t("休憩の扱い", "Break handling"),
                helpText = t("休憩で作業を分割するか", "Split work on break")
            ) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = breakSplitsWork,
                        onClick = { breakSplitsWork = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text(t("分割する", "Allow split")) }
                    SegmentedButton(
                        selected = !breakSplitsWork,
                        onClick = { breakSplitsWork = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text(t("継続", "Keep continuous")) }
                }
            }
        }

        SectionBlock(
            title = t("メタ情報", "Meta"),
            helpText = t("プロジェクトとタグ", "Project and tags")
        ) {
            AutoCompleteTextField(
                value = project,
                onValueChange = { project = it },
                suggestions = projectSuggestions,
                placeholder = t("プロジェクト", "Project"),
                onSuggestionSelected = { project = it }
            )

            AutoCompleteTextField(
                value = tagDraft,
                onValueChange = { tagDraft = it },
                suggestions = tagSuggestions,
                placeholder = t("タグ", "Tag"),
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
            title = t("メモ", "Memo"),
            helpText = t("次のアクションを記載", "Describe the next action")
        ) {
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                placeholder = { Text(t("メモを入力", "Enter memo")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        SectionBlock(
            title = t("完了条件", "Done definition"),
            helpText = t("自動生成された完了条件", "Auto-generated done definition")
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
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onClose) {
                Text(t("キャンセル", "Cancel"))
            }
            Button(
                onClick = {
                    error = null
                    if (!validation.temporalOrderValid) {
                        error = t("日時の前後関係が不正です", "Temporal order is invalid")
                        return@Button
                    }
                    if (!validation.durationReady) {
                        error = t("作業時間を設定してください", "Work duration is required")
                        return@Button
                    }
                    if (!validation.recurrenceReady) {
                        error = t("繰り返し間隔を設定してください", "Recurrence interval is required")
                        return@Button
                    }
                    if (title.trim().isBlank()) {
                        error = t("タイトルを入力してください", "Title is required")
                        return@Button
                    }

                    submitting = true
                    viewModel.createTile(
                        CreateTileDraft(
                            title = title.trim(),
                            nextAction = memo.trim().ifBlank {
                                if (tileKind == "label") t("この期間にラベルを適用", "Apply this label within the selected period")
                                else t("開始して最初の1手を実行", "Start and execute the first step")
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
                Text(t("作成", "Create"))
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
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = date,
                onValueChange = {},
                modifier = Modifier.weight(1f),
                readOnly = true,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = if (locale == AppLocale.JA) "日付選択" else "Select date")
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
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = if (locale == AppLocale.JA) "時刻選択" else "Select time")
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
                    Text(if (locale == AppLocale.JA) "OK" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(if (locale == AppLocale.JA) "キャンセル" else "Cancel")
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
                        onTimeChange(String.format("%02d:%02d", timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    }
                ) {
                    Text(if (locale == AppLocale.JA) "OK" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(if (locale == AppLocale.JA) "キャンセル" else "Cancel")
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
