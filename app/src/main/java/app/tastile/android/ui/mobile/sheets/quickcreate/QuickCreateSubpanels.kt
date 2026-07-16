package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Anchor
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import app.tastile.android.ui.mobile.designsystem.AppDismissButton
import app.tastile.android.ui.mobile.designsystem.AppPickerButton
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppSecondaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
import app.tastile.android.ui.mobile.designsystem.SectionHeader
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateDurationRange
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreatePlanRole
import app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateTileKind
import app.tastile.android.ui.mobile.sheets.QuickCreatePlanReference
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskContent
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskDefinition
import app.tastile.android.ui.mobile.sheets.QuickCreateTimeRequirement
import app.tastile.android.ui.mobile.sheets.QuickCreateTimeOfDayMode
import app.tastile.android.ui.mobile.sheets.QuickCreateWhenMode
import app.tastile.android.ui.mobile.sheets.QuickCreateWindow
import app.tastile.android.ui.mobile.sheets.QuickCreateFrameGenerator
import app.tastile.android.ui.mobile.sheets.QuickCreateFrameRule
import app.tastile.android.ui.mobile.sheets.QuickCreateRecurringRule
import app.tastile.android.ui.mobile.sheets.QuickCreateConditionNode
import app.tastile.android.ui.mobile.sheets.QuickCreateWindowRule
import app.tastile.android.ui.mobile.sheets.QuickCreateDateRange
import app.tastile.android.ui.mobile.sheets.QuickCreateProject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
internal fun QuickCreateSubpanel(
    panel: QuickCreatePanel,
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    onBack: () -> Unit,
    projects: List<QuickCreateProject>,
    knownTags: List<String>,
) {
    Column(
        Modifier.testTag("quick-create-subpanel-${panel.name}").verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
    ) {
        BackHeader(onBack)
        SectionHeader(title = panel.name)
        when (panel) {
            QuickCreatePanel.Intent -> IntentPanel(store)
            QuickCreatePanel.Time -> TimePanel(draft, store)
            QuickCreatePanel.Duration -> DurationPanel(draft, store)
            QuickCreatePanel.Recurring -> RecurringPanel(draft, store)
            QuickCreatePanel.References -> ReferencesPanel(draft, store)
            QuickCreatePanel.Completion -> CompletionPanel(draft, store)
            QuickCreatePanel.Meta -> MetaPanel(draft, store, projects, knownTags, onBack)
            QuickCreatePanel.Behavior -> BehaviorPanel(draft, store)
            QuickCreatePanel.Base -> Unit
        }
    }
}

@Composable
private fun TimePanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    fun setWhen(mode: QuickCreateWhenMode) {
        val time = draft.time
        store.updateTime(when (mode) {
            QuickCreateWhenMode.None -> time.copy(whenMode = mode, span = app.tastile.android.ui.mobile.sheets.QuickCreateSpan(), timeOfDayMode = QuickCreateTimeOfDayMode.Unspecified, timeOfDayStart = "", timeOfDayEnd = "")
            QuickCreateWhenMode.Day -> time.copy(whenMode = mode, span = time.span.copy(end = ""))
            QuickCreateWhenMode.Range -> time.copy(whenMode = mode)
            QuickCreateWhenMode.Reference -> time.copy(whenMode = mode, span = app.tastile.android.ui.mobile.sheets.QuickCreateSpan())
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
    AppTertiaryButton(
        text = "No date or time",
        onClick = { setWhen(QuickCreateWhenMode.None) },
        modifier = Modifier.fillMaxWidth().testTag("quick-create-when-none"),
        leadingIcon = Icons.Outlined.EventBusy,
    )
    SectionHeader(title = "When")
    val whenModes = listOf(QuickCreateWhenMode.Day, QuickCreateWhenMode.Range, QuickCreateWhenMode.Reference)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        whenModes.forEachIndexed { index, mode ->
            val icon = when (mode) {
                QuickCreateWhenMode.None -> Icons.Outlined.EventBusy
                QuickCreateWhenMode.Day -> Icons.Outlined.Today
                QuickCreateWhenMode.Range -> Icons.Outlined.DateRange
                QuickCreateWhenMode.Reference -> Icons.Outlined.Tag
            }
            SegmentedButton(
                selected = draft.time.whenMode == mode,
                onClick = { setWhen(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = whenModes.size),
                modifier = Modifier.testTag("quick-create-when-${mode.name.lowercase()}"),
                icon = {
                    Icon(
                        imageVector = if (draft.time.whenMode == mode) Icons.Outlined.Check else icon,
                        contentDescription = null,
                    )
                },
                label = { Text(mode.name) },
            )
        }
    }
    if (draft.time.whenMode == QuickCreateWhenMode.Day || draft.time.whenMode == QuickCreateWhenMode.Range) Column(Modifier.testTag("quick-create-calendar")) {
        NativeDateField("Date", draft.time.span.start, "quick-create-start") { value -> store.updateTime(draft.time.copy(span = draft.time.span.copy(start = value))) }
        if (draft.time.whenMode == QuickCreateWhenMode.Range) NativeDateField("End date", draft.time.span.end, "quick-create-end") { value -> store.updateTime(draft.time.copy(span = draft.time.span.copy(end = value))) }
    }
    if (draft.time.whenMode == QuickCreateWhenMode.Reference) Column(Modifier.testTag("quick-create-reference-catalog")) {
        SectionHeader(title = "Reference range")
        AppPickerButton(
            label = stringResource(R.string.picker_reference_label),
            value = draft.time.referenceId.orEmpty().ifBlank { "—" },
            onClick = { showReferencePicker = true },
            leadingIcon = Icons.Outlined.Tag,
            modifier = Modifier.fillMaxWidth().testTag("quick-create-reference-id"),
        )
        OutlinedTextField(draft.time.referenceLabel, { value -> store.updateTime(draft.time.copy(referenceLabel = value)) }, label = { Text("Reference label") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-reference-label"))
    }
    if (draft.time.whenMode != QuickCreateWhenMode.None) {
        SectionHeader(title = "Time of day")
        val timeOfDayModes = QuickCreateTimeOfDayMode.entries.toList()
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            timeOfDayModes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = draft.time.timeOfDayMode == mode,
                    onClick = {
                        store.updateTime(if (mode == QuickCreateTimeOfDayMode.Range) draft.time.copy(timeOfDayMode = mode, timeOfDayStart = draft.time.timeOfDayStart.ifBlank { "09:00" }, timeOfDayEnd = draft.time.timeOfDayEnd.ifBlank { "18:00" }) else draft.time.copy(timeOfDayMode = mode, timeOfDayStart = "", timeOfDayEnd = ""))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = timeOfDayModes.size),
                    modifier = Modifier.testTag("quick-create-time-of-day-${mode.name.lowercase()}"),
                    icon = {
                        Icon(
                            imageVector = if (draft.time.timeOfDayMode == mode) Icons.Outlined.Check else Icons.Outlined.Schedule,
                            contentDescription = null,
                        )
                    },
                    label = { Text(mode.name) },
                )
            }
        }
        if (draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.Range) {
            AppPickerButton(
                label = stringResource(R.string.picker_time_start),
                value = draft.time.timeOfDayStart.ifBlank { "—" },
                onClick = { showStartTime = true },
                leadingIcon = Icons.Outlined.AccessTime,
                modifier = Modifier.fillMaxWidth().testTag("quick-create-time-of-day-start"),
            )
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
            AppPickerButton(
                label = stringResource(R.string.picker_time_end),
                value = draft.time.timeOfDayEnd.ifBlank { "—" },
                onClick = { showEndTime = true },
                leadingIcon = Icons.Outlined.AccessTime,
                modifier = Modifier.fillMaxWidth().testTag("quick-create-time-of-day-end"),
            )
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
                Triple("morning", "06:00" to "10:00", Icons.Outlined.WbSunny),
                Triple("midday", "09:00" to "18:00", Icons.Outlined.LightMode),
                Triple("night", "18:00" to "24:00", Icons.Outlined.DarkMode),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                quickRanges.forEachIndexed { index, (label, range, icon) ->
                    SegmentedButton(
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
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = quickRanges.size),
                        modifier = Modifier.testTag("quick-create-time-quick-$label"),
                        icon = {
                            Icon(
                                imageVector = if (
                                    draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.Range &&
                                    draft.time.timeOfDayStart == range.first &&
                                    draft.time.timeOfDayEnd == range.second
                                ) Icons.Outlined.Check else icon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
    AppSecondaryButton(
        text = "Add window",
        onClick = { store.updateWindows(draft.windows + QuickCreateWindow(UUID.randomUUID().toString(), "self", 0, app.tastile.android.ui.mobile.sheets.QuickCreateSpan())) },
        modifier = Modifier.testTag("quick-create-add-window"),
        leadingIcon = Icons.Outlined.Add,
    )
    draft.windows.forEachIndexed { index, window ->
        var showWindowStartDate by remember(index) { mutableStateOf(false) }
        var showWindowEndDate by remember(index) { mutableStateOf(false) }
        var showWindowReferencePicker by remember(index) { mutableStateOf(false) }
        SectionHeader(title = "Window ${index + 1}")
        val windowKinds = listOf(0, 1, 2, 3)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            windowKinds.forEachIndexed { kindIndex, kind ->
                val icon = when (kind) {
                    0 -> Icons.Outlined.Anchor
                    1 -> Icons.Outlined.Link
                    2 -> Icons.Outlined.Schedule
                    else -> Icons.Outlined.Repeat
                }
                SegmentedButton(
                    selected = window.kind == kind,
                    onClick = { store.updateWindows(draft.windows.replace(index, window.copy(kind = kind))) },
                    shape = SegmentedButtonDefaults.itemShape(index = kindIndex, count = windowKinds.size),
                    modifier = Modifier.testTag("quick-create-window-$index-kind-$kind"),
                    icon = {
                        Icon(
                            imageVector = if (window.kind == kind) Icons.Outlined.Check else icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Kind $kind") },
                )
            }
        }
        AppPickerButton(
            label = stringResource(R.string.picker_date_start),
            value = window.bounds.start.ifBlank { "—" },
            onClick = { showWindowStartDate = true },
            leadingIcon = Icons.Outlined.CalendarToday,
            modifier = Modifier.fillMaxWidth().testTag("quick-create-window-$index-start"),
        )
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
        AppPickerButton(
            label = stringResource(R.string.picker_date_end),
            value = window.bounds.end.ifBlank { "—" },
            onClick = { showWindowEndDate = true },
            leadingIcon = Icons.Outlined.CalendarToday,
            modifier = Modifier.fillMaxWidth().testTag("quick-create-window-$index-end"),
        )
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
            AppPickerButton(
                label = stringResource(R.string.picker_reference_label),
                value = window.referenceId.orEmpty().ifBlank { "—" },
                onClick = { showWindowReferencePicker = true },
                leadingIcon = Icons.Outlined.Tag,
                modifier = Modifier.fillMaxWidth().testTag("quick-create-window-$index-reference"),
            )
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
        AppSecondaryButton(
            text = "Remove window",
            onClick = { store.updateWindows(draft.windows.filterIndexed { item, _ -> item != index }) },
            modifier = Modifier.testTag("quick-create-window-$index-remove"),
            leadingIcon = Icons.Outlined.Delete,
        )
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

@Composable
private fun DurationPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val duration = draft.time.durationMinMax
    AppTertiaryButton(
        text = "No duration",
        onClick = { store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(null, null))) },
        modifier = Modifier.fillMaxWidth().testTag("quick-create-duration-none"),
        leadingIcon = Icons.Outlined.Close,
    )
    OutlinedTextField(
        value = duration.minMs?.div(60_000L)?.toString() ?: "90",
        onValueChange = { value -> value.toLongOrNull()?.let { minutes ->
            val milliseconds = minutes.coerceIn(10L, 720L) * 60_000L
            store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(milliseconds, milliseconds)))
        } },
        label = { Text("Duration (minutes)") },
        modifier = Modifier.fillMaxWidth().testTag("quick-create-duration-minutes"),
    )
    Text("Use for completion", modifier = Modifier.testTag("quick-create-duration-completion-link"))
}

@Composable
private fun RecurringPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    Column(Modifier.testTag("quick-create-recurring-controls")) {
        val repeatModes = QuickCreateRepeatMode.entries.toList()
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            repeatModes.forEachIndexed { index, value ->
                val icon = when (value) {
                    QuickCreateRepeatMode.Once -> Icons.Outlined.Event
                    QuickCreateRepeatMode.Daily -> Icons.Outlined.Today
                    QuickCreateRepeatMode.Weekly -> Icons.Outlined.DateRange
                    QuickCreateRepeatMode.Interval -> Icons.Outlined.Repeat
                    QuickCreateRepeatMode.Condition -> Icons.Outlined.Autorenew
                }
                SegmentedButton(
                    selected = draft.recurring.repeatMode == value,
                    onClick = {
                        store.updateRecurring(draft.recurring.copy(repeatMode = value))
                        if (value != QuickCreateRepeatMode.Once) store.updateIdentity(draft.identity.copy(kind = QuickCreateTileKind.Recurring))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = repeatModes.size),
                    modifier = Modifier.testTag("quick-create-repeat-${value.name.lowercase()}"),
                    icon = {
                        Icon(
                            imageVector = if (draft.recurring.repeatMode == value) Icons.Outlined.Check else icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(value.name) },
                )
            }
        }
        Text(if (draft.recurring.repeatMode == QuickCreateRepeatMode.Weekly) "Weekdays" else "Weekdays (weekly only)")
        val weekdays = (0..6).toList()
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEachIndexed { index, bit ->
                val selected = draft.recurring.repeatMode == QuickCreateRepeatMode.Weekly && (draft.recurring.weekdayMask shr bit) and 1 == 1
                SegmentedButton(
                    selected = selected,
                    enabled = draft.recurring.repeatMode == QuickCreateRepeatMode.Weekly,
                    onClick = { store.updateRecurring(draft.recurring.copy(weekdayMask = draft.recurring.weekdayMask xor (1 shl bit))) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = weekdays.size),
                    modifier = Modifier.testTag("quick-create-weekday-$bit"),
                    icon = {
                        Icon(
                            imageVector = if (selected) Icons.Outlined.Check else Icons.Outlined.CalendarToday,
                            contentDescription = null,
                        )
                    },
                    label = { Text(bit.toString()) },
                )
            }
        }
        AppTertiaryButton(
            text = "End date",
            onClick = { store.updateRecurring(draft.recurring.copy(endDate = if (draft.recurring.endDate.isBlank()) LocalDate.now().toString() else "")) },
            modifier = Modifier.testTag("quick-create-recurring-end-switch"),
            leadingIcon = Icons.Outlined.CalendarMonth,
        )
        if (draft.recurring.endDate.isNotBlank()) NativeDateField("End date", draft.recurring.endDate, "quick-create-recurring-end-date") { value -> store.updateRecurring(draft.recurring.copy(endDate = value)) }
    }
}

@Composable
private fun ReferencesPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    AppSecondaryButton(
        text = "Add reference",
        onClick = { store.updatePlan(draft.plan.copy(references = draft.plan.references + defaultPlanReference())) },
        modifier = Modifier.testTag("quick-create-add-reference"),
        leadingIcon = Icons.Outlined.Add,
    )
    draft.plan.references.forEachIndexed { index, reference ->
        val target = reference.target.jsonObjectOrEmpty()
        val pick = reference.pick.jsonObjectOrEmpty()
        OutlinedTextField(reference.id, { value -> updateReference(draft, store, index, reference.copy(id = value)) }, label = { Text("Reference ID") }, modifier = Modifier.testTag("quick-create-reference-record-id-$index"))
        OutlinedTextField(target.string("referenceId"), { value -> updateReference(draft, store, index, reference.copy(target = target.with("referenceId", value.ifBlank { null }))) }, label = { Text("Target reference") }, modifier = Modifier.testTag("quick-create-reference-id-$index"))
        val targetKinds = listOf(0, 1, 2)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            targetKinds.forEachIndexed { kindIndex, kind ->
                val currentKind = target["kind"]?.jsonPrimitive?.content?.toIntOrNull()
                val icon = when (kind) {
                    0 -> Icons.Outlined.Tag
                    else -> Icons.Outlined.Link
                }
                SegmentedButton(
                    selected = currentKind == kind,
                    onClick = { updateReference(draft, store, index, reference.copy(target = target.with("kind", kind))) },
                    shape = SegmentedButtonDefaults.itemShape(index = kindIndex, count = targetKinds.size),
                    icon = {
                        Icon(
                            imageVector = if (currentKind == kind) Icons.Outlined.Check else icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Target kind $kind") },
                )
            }
        }
        SectionHeader(title = "Relation")
        val relations = listOf(4, 3, 1, 2, 0)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            relations.forEachIndexed { relIndex, relation ->
                val currentRelation = pick["kind"]?.jsonPrimitive?.content?.toIntOrNull()
                SegmentedButton(
                    selected = currentRelation == relation,
                    onClick = { updateReference(draft, store, index, reference.copy(pick = pick.with("kind", relation))) },
                    shape = SegmentedButtonDefaults.itemShape(index = relIndex, count = relations.size),
                    icon = {
                        Icon(
                            imageVector = if (currentRelation == relation) Icons.Outlined.Check else Icons.Outlined.Link,
                            contentDescription = null,
                        )
                    },
                    label = { Text(relation.toString()) },
                )
            }
        }
        OutlinedTextField(pick.string("momentId", "10"), { value -> value.toIntOrNull()?.coerceIn(5, 120)?.let { minutes -> updateReference(draft, store, index, reference.copy(pick = pick.with("momentId", minutes.toString()))) } }, label = { Text("Interval (minutes)") })
        AppSecondaryButton(
            text = "Remove reference",
            onClick = { store.updatePlan(draft.plan.copy(references = draft.plan.references.filterIndexed { item, _ -> item != index })) },
            modifier = Modifier.testTag("quick-create-reference-record-$index-remove"),
            leadingIcon = Icons.Outlined.Delete,
        )
    }
}

private fun defaultPlanReference() = QuickCreatePlanReference(
    id = "",
    target = JsonObject(mapOf("kind" to JsonPrimitive(0), "contextKind" to JsonNull, "referenceId" to JsonNull, "conditionId" to JsonNull)),
    pick = JsonObject(mapOf("kind" to JsonPrimitive(4), "momentId" to JsonPrimitive("10"))),
)

@Composable
private fun IntentPanel(store: QuickCreateStateStore) {
    SectionHeader(title = "Add condition or group")
    val intentTargets = listOf(
        Triple("Time", QuickCreatePanel.Time, Icons.Outlined.Schedule),
        Triple("References", QuickCreatePanel.References, Icons.Outlined.Link),
        Triple("Recurring", QuickCreatePanel.Recurring, Icons.Outlined.Repeat),
        Triple("Meta", QuickCreatePanel.Meta, Icons.Outlined.Tag),
        Triple("Completion", QuickCreatePanel.Completion, Icons.Outlined.Check),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        intentTargets.forEach { (label, panel, icon) ->
            AppPrimaryButton(
                text = label,
                onClick = { store.openSubpanel(panel) },
                modifier = Modifier.fillMaxWidth().testTag("quick-create-intent-${label.lowercase()}"),
                leadingIcon = icon,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NativeDateField(label: String, value: String, tag: String, onSelected: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    AppPickerButton(
        label = label,
        value = value.ifBlank { "—" },
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth().testTag(tag),
        leadingIcon = Icons.Outlined.CalendarMonth,
    )
    if (open) {
        val state = androidx.compose.material3.rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                AppPrimaryButton(
                    text = "OK",
                    onClick = {
                        state.selectedDateMillis?.let { millis -> onSelected(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toString()) }
                        open = false
                    },
                    leadingIcon = Icons.Outlined.Check,
                )
            },
            dismissButton = {
                AppTertiaryButton(
                    text = "Cancel",
                    onClick = { open = false },
                    leadingIcon = Icons.Outlined.Close,
                )
            },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun CompletionPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    SectionHeader(title = "Logic")
    val logicKinds = listOf(0 to "ALL", 1 to "ANY", 2 to "NOT")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        logicKinds.forEachIndexed { index, (kind, label) ->
            val icon = when (kind) {
                0 -> Icons.Outlined.PlaylistAddCheck
                1 -> Icons.Outlined.PlaylistAdd
                else -> Icons.Outlined.Block
            }
            SegmentedButton(
                selected = draft.plan.completion.root.kind == kind,
                onClick = { store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = draft.plan.completion.root.copy(kind = kind, term = null)))) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = logicKinds.size),
                icon = {
                    Icon(
                        imageVector = if (draft.plan.completion.root.kind == kind) Icons.Outlined.Check else icon,
                        contentDescription = null,
                    )
                },
                label = { Text(label) },
            )
        }
    }
    ConditionControls(draft.plan.completion.root, onChange = { root -> store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = root))) }, allowTermKind = false)
    Row {
        AppSecondaryButton(
            text = "Task",
            onClick = { addCompletionTerm(draft, store, "task") },
            modifier = Modifier.testTag("quick-create-completion-add-task"),
            leadingIcon = Icons.Outlined.Task,
        )
        AppSecondaryButton(
            text = "Relation",
            onClick = { addCompletionTerm(draft, store, "relation") },
            modifier = Modifier.testTag("quick-create-completion-add-relation"),
            leadingIcon = Icons.Outlined.Link,
        )
        AppSecondaryButton(
            text = "Metric",
            onClick = { addCompletionTerm(draft, store, "metric") },
            modifier = Modifier.testTag("quick-create-completion-add-metric"),
            leadingIcon = Icons.Outlined.BarChart,
        )
    }
    AppSecondaryButton(
        text = "Add time requirement",
        onClick = {
            store.updatePlan(
                draft.plan.copy(
                    completion = draft.plan.completion.copy(
                        timeRequirements = draft.plan.completion.timeRequirements + webTimeRequirement(
                            draft.time.durationMinMax.minMs,
                        ),
                    ),
                ),
            )
        },
        modifier = Modifier.testTag("quick-create-completion-add-time"),
        leadingIcon = Icons.Outlined.Add,
    )
    draft.plan.completion.timeRequirements.forEachIndexed { index, requirement ->
        val required = requirement.required.jsonObjectOrEmpty()
        val minimumMinutes = required.long("minMs")?.div(60_000L)?.toString().orEmpty()
        OutlinedTextField(
            value = minimumMinutes,
            onValueChange = { input ->
                val minutes = input.toLongOrNull()
                val nextRequired = when {
                    input.isBlank() -> required.with("minMs", null)
                    minutes == null -> required
                    else -> required.with("minMs", minutes.coerceAtLeast(5L) * 60_000L)
                }
                updateTimeRequirement(draft, store, index, requirement.copy(required = nextRequired))
            },
            label = { Text("Minutes") },
            modifier = Modifier.testTag("time-requirement-$index-required-minutes"),
        )
        Text("minutes")
        AppSecondaryButton(
            text = "Remove time requirement",
            onClick = {
                store.updatePlan(
                    draft.plan.copy(
                        completion = draft.plan.completion.copy(
                            timeRequirements = draft.plan.completion.timeRequirements.filterIndexed { item, _ -> item != index },
                        ),
                    ),
                )
            },
            modifier = Modifier.testTag("time-requirement-$index-remove"),
            leadingIcon = Icons.Outlined.Delete,
        )
    }
    AppTertiaryButton(
        text = "Clear completion",
        onClick = {
            store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = QuickCreateConditionNode(0), timeRequirements = emptyList(), tasks = emptyList())))
        },
        modifier = Modifier.testTag("quick-create-completion-clear"),
        leadingIcon = Icons.Outlined.DeleteSweep,
    )
}

private fun addCompletionTerm(draft: QuickCreateDraftState, store: QuickCreateStateStore, kind: String) {
    val term = if (kind == "task" && draft.plan.completion.tasks.isNotEmpty()) {
        termValue("task", mapOf("taskId" to JsonPrimitive(draft.plan.completion.tasks.first().id), "state" to JsonPrimitive(2)))
    } else defaultTermValue(kind)
    store.appendCompletionTerm(term)
}

private fun webTimeRequirement(durationMinimumMs: Long?): QuickCreateTimeRequirement = QuickCreateTimeRequirement(
    id = UUID.randomUUID().toString(),
    observation = JsonObject(mapOf("scope" to JsonPrimitive(0))),
    required = JsonObject(mapOf("minMs" to JsonPrimitive(durationMinimumMs ?: 60 * 60_000L))),
)

private fun updateTimeRequirement(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    index: Int,
    requirement: QuickCreateTimeRequirement,
) {
    store.updatePlan(
        draft.plan.copy(
            completion = draft.plan.completion.copy(
                timeRequirements = draft.plan.completion.timeRequirements.replace(index, requirement),
            ),
        ),
    )
}

@Composable private fun ConditionControls(node: QuickCreateConditionNode, onChange: (QuickCreateConditionNode) -> Unit, path: String = "root", allowTermKind: Boolean = true) {
    val logicKinds = listOf(0 to "ALL", 1 to "ANY", 2 to "NOT", 3 to "TERM").filter { allowTermKind || it.first != 3 }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        logicKinds.forEachIndexed { index, (kind, label) ->
            val icon = when (kind) {
                0 -> Icons.Outlined.PlaylistAddCheck
                1 -> Icons.Outlined.PlaylistAdd
                2 -> Icons.Outlined.Block
                else -> Icons.Outlined.TextFields
            }
            SegmentedButton(
                selected = node.kind == kind,
                onClick = { onChange(node.copy(kind = kind, children = if (kind == 3) emptyList() else node.children, term = if (kind == 3) defaultTermValue("calendar") else null)) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = logicKinds.size),
                icon = {
                    Icon(
                        imageVector = if (node.kind == kind) Icons.Outlined.Check else icon,
                        contentDescription = null,
                    )
                },
                label = { Text(label) },
            )
        }
    }
    if (node.kind == 3) {
        val termTypes = listOf("calendar", "moment", "relation", "gap", "requirement", "task", "fact", "metric", "life")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            termTypes.forEachIndexed { index, type ->
                val icon = when (type) {
                    "calendar" -> Icons.Outlined.CalendarMonth
                    "moment" -> Icons.Outlined.Schedule
                    "relation" -> Icons.Outlined.Link
                    "gap" -> Icons.Outlined.HorizontalRule
                    "requirement" -> Icons.Outlined.Check
                    "task" -> Icons.Outlined.Task
                    "fact" -> Icons.Filled.Lightbulb
                    "metric" -> Icons.Outlined.BarChart
                    "life" -> Icons.Outlined.Favorite
                    else -> Icons.Outlined.TextFields
                }
                val currentKind = node.term?.jsonObjectOrEmpty()?.string("kind")
                SegmentedButton(
                    selected = currentKind == type,
                    onClick = { onChange(node.copy(term = defaultTermValue(type))) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = termTypes.size),
                    modifier = Modifier.testTag("condition-$path-term-$type"),
                    icon = {
                        Icon(
                            imageVector = if (currentKind == type) Icons.Outlined.Check else icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(type) },
                )
            }
        }
        val term = node.term?.jsonObjectOrEmpty() ?: JsonObject(emptyMap())
        when (term.string("kind")) {
            "calendar" -> CalendarTermFields(term, path) { value -> onChange(node.copy(term = value)) }
            "moment" -> MomentTermFields(term, path) { value -> onChange(node.copy(term = value)) }
            "relation" -> RelationTermFields(term) { value -> onChange(node.copy(term = value)) }
            "task" -> TaskTermFields(term) { value -> onChange(node.copy(term = value)) }
            "gap" -> GapTermFields(path)
            "requirement" -> RequirementTermFields(term, path) { value -> onChange(node.copy(term = value)) }
            "fact" -> ScalarTermFields(term, path, "fact", "factId") { value -> onChange(node.copy(term = value)) }
            "metric" -> ScalarTermFields(term, path, "metric", "metricId") { value -> onChange(node.copy(term = value)) }
            "feedback" -> ScalarTermFields(term, path, "feedback", "feedbackTxnId") { value -> onChange(node.copy(term = value)) }
            "life" -> LifeTermFields(term, path) { value -> onChange(node.copy(term = value)) }
        }
    }
    else {
        node.children.forEachIndexed { index, child ->
            ConditionControls(child, { updated -> onChange(node.copy(children = node.children.replace(index, updated))) }, "$path-$index")
            AppSecondaryButton(
                text = "Remove",
                onClick = { onChange(node.copy(children = node.children.filterIndexed { item, _ -> item != index })) },
                modifier = Modifier.testTag("condition-$path-child-$index-remove"),
                leadingIcon = Icons.Outlined.Delete,
            )
        }
        AppSecondaryButton(
            text = "Add condition",
            onClick = { onChange(node.copy(children = node.children + QuickCreateConditionNode(3, term = defaultTermValue("calendar")))) },
            modifier = Modifier.testTag("condition-$path-add-child"),
            leadingIcon = Icons.Outlined.Add,
        )
    }
}

@Composable private fun CalendarTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("weekdayMask", "0"), { input -> onChange(term.withValue("weekdayMask", input.toIntOrNull() ?: 0)) }, label = { Text("Weekday mask") }, modifier = Modifier.testTag("condition-$path-calendar-weekday-mask"))
    OutlinedTextField(value.string("offsetMin", "0"), { input -> onChange(term.withValue("offsetMin", input.toIntOrNull() ?: 0)) }, label = { Text("Offset minutes") }, modifier = Modifier.testTag("condition-$path-calendar-offset"))
    OutlinedTextField(value.string("timeStart"), { input -> onChange(term.withValue("timeStart", input.ifBlank { null })) }, label = { Text("Time start") }, modifier = Modifier.testTag("condition-calendar-start"))
    OutlinedTextField(value.string("timeEnd"), { input -> onChange(term.withValue("timeEnd", input.ifBlank { null })) }, label = { Text("Time end") }, modifier = Modifier.testTag("condition-calendar-end"))
}

@Composable private fun MomentTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("referenceId"), { input -> onChange(term.withValue("referenceId", input.ifBlank { null })) }, label = { Text("Reference ID") }, modifier = Modifier.testTag("condition-moment-reference"))
    OutlinedTextField(value.string("offsetMs", "0"), { input -> onChange(term.withValue("offsetMs", input.toLongOrNull() ?: 0L)) }, label = { Text("Offset ms") }, modifier = Modifier.testTag("condition-moment-offset"))
}

@Composable private fun RelationTermFields(term: JsonObject, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("referenceId"), { input -> onChange(term.withValue("referenceId", input)) }, label = { Text("Reference ID") }, modifier = Modifier.testTag("condition-relation-reference"))
    OutlinedTextField(value.string("relation", "0"), { input -> onChange(term.withValue("relation", input.toIntOrNull() ?: 0)) }, label = { Text("Relation") }, modifier = Modifier.testTag("condition-relation-kind"))
    OutlinedTextField(value.string("windowKind", "0"), { input -> onChange(term.withValue("windowKind", input.toIntOrNull() ?: 0)) }, label = { Text("Window kind") }, modifier = Modifier.testTag("condition-relation-window"))
}

@Composable private fun TaskTermFields(term: JsonObject, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("taskId"), { input -> onChange(term.withValue("taskId", input)) }, label = { Text("Task ID") }, modifier = Modifier.testTag("condition-task-id"))
    OutlinedTextField(value.string("state", "0"), { input -> onChange(term.withValue("state", input.toIntOrNull() ?: 0)) }, label = { Text("State") }, modifier = Modifier.testTag("condition-task-state"))
}

@Composable private fun GapTermFields(path: String) {
    // Current Web TermFields intentionally exposes Gap as an informational placeholder only.
    Text("Gap configuration is preserved when supplied by the API.", modifier = Modifier.testTag("condition-$path-gap-placeholder"))
}

@Composable private fun RequirementTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("requirementId"), { input ->
        onChange(term.withValue("requirementId", input))
    }, label = { Text("Requirement ID") }, modifier = Modifier.testTag("condition-$path-requirement-id"))
    OutlinedTextField(value.string("state", "0"), { input ->
        onChange(term.withValue("state", input.toIntOrNull() ?: 0))
    }, label = { Text("State") }, modifier = Modifier.testTag("condition-$path-requirement-state"))
}

@Composable private fun ScalarTermFields(
    term: JsonObject,
    path: String,
    kind: String,
    idKey: String,
    onChange: (JsonObject) -> Unit,
) {
    val value = term.valueObject()
    OutlinedTextField(value.string(idKey), { input ->
        onChange(term.withValue(idKey, input))
    }, label = { Text("ID") }, modifier = Modifier.testTag("condition-$path-$kind-id"))
    OutlinedTextField(value.string("op", "0"), { input ->
        onChange(term.withValue("op", input.toIntOrNull() ?: 0))
    }, label = { Text("Operator") }, modifier = Modifier.testTag("condition-$path-$kind-op"))
    OutlinedTextField(value.string("value"), { input ->
        onChange(term.withValue("value", scalarValue(input)))
    }, label = { Text("Value") }, modifier = Modifier.testTag("condition-$path-$kind-value"))
}

@Composable private fun LifeTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("target"), { input ->
        onChange(term.withValue("target", input))
    }, label = { Text("Target") }, modifier = Modifier.testTag("condition-$path-life-target"))
    OutlinedTextField(value.string("state", "0"), { input ->
        onChange(term.withValue("state", input.toIntOrNull() ?: 0))
    }, label = { Text("State") }, modifier = Modifier.testTag("condition-$path-life-state"))
}

private fun defaultTermValue(kind: String): JsonObject = when (kind) {
    "calendar" -> termValue(kind, mapOf("weekdayMask" to JsonPrimitive(0), "timeStart" to JsonNull, "timeEnd" to JsonNull, "holidayKind" to JsonPrimitive(2), "dateRange" to JsonNull, "offsetMin" to JsonPrimitive(0)))
    "moment" -> termValue(kind, mapOf("referenceId" to JsonNull, "point" to JsonNull, "offsetMs" to JsonPrimitive(0)))
    "relation" -> termValue(kind, mapOf("referenceId" to JsonPrimitive(""), "relation" to JsonPrimitive(0), "windowKind" to JsonPrimitive(0)))
    "gap" -> termValue(kind, mapOf(
        "scope" to JsonPrimitive(0),
        "leftAnchor" to JsonObject(mapOf("referenceId" to JsonNull, "point" to JsonNull)),
        "rightAnchor" to JsonObject(mapOf("referenceId" to JsonNull, "point" to JsonNull)),
        "size" to JsonObject(mapOf("minMs" to JsonNull, "maxMs" to JsonNull)),
    ))
    "requirement" -> termValue(kind, mapOf("requirementId" to JsonPrimitive(""), "state" to JsonPrimitive(0)))
    "fact" -> termValue(kind, mapOf("factId" to JsonPrimitive(""), "op" to JsonPrimitive(0), "value" to JsonNull))
    "metric" -> termValue(kind, mapOf("metricId" to JsonPrimitive(""), "op" to JsonPrimitive(0), "value" to JsonNull))
    "feedback" -> termValue(kind, mapOf("feedbackTxnId" to JsonPrimitive(""), "op" to JsonPrimitive(0), "value" to JsonNull))
    "life" -> termValue(kind, mapOf("target" to JsonPrimitive(""), "state" to JsonPrimitive(0)))
    "task" -> termValue(kind, mapOf("taskId" to JsonPrimitive(""), "state" to JsonPrimitive(0)))
    else -> defaultTermValue("calendar")
}

private fun termValue(kind: String, value: Map<String, JsonElement>) = JsonObject(mapOf("kind" to JsonPrimitive(kind), "value" to JsonObject(value)))
private fun JsonObject.valueObject(): JsonObject = this["value"] as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.withValue(key: String, value: Any?): JsonObject = with("value", valueObject().with(key, value))
private fun scalarValue(input: String): Any? = when {
    input.isBlank() -> null
    input.toLongOrNull() != null -> input.toLong()
    input.toDoubleOrNull()?.isFinite() == true -> input.toDouble()
    else -> input
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaPanel(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    projects: List<QuickCreateProject>,
    knownTags: List<String>,
    onBack: () -> Unit,
) {
    SectionHeader(title = "Behavior")
    Row(Modifier.testTag("behavior-role")) {
        QuickCreatePlanRole.entries.forEach { value ->
            FilterChip(
                selected = draft.plan.role == value,
                onClick = { store.updateBehavior(value) },
                label = { Text(value.name) },
                modifier = Modifier.testTag("behavior-role-${value.name.lowercase()}"),
            )
        }
    }
    SectionHeader(title = "Project")
    Column(Modifier.testTag("meta-project-catalog")) {
        FilterChip(
            selected = draft.meta.ownerSubjectId == null,
            onClick = { store.updateMeta(draft.meta.copy(ownerSubjectId = null)) },
            label = { Text("No project") },
            modifier = Modifier.testTag("meta-project-none"),
        )
        projects.forEach { project ->
            FilterChip(
                selected = draft.meta.ownerSubjectId == project.id,
                onClick = { store.updateMeta(draft.meta.copy(ownerSubjectId = project.id)) },
                label = { Text(project.displayName) },
                modifier = Modifier.testTag("meta-project-${project.id}"),
            )
        }
    }
    SectionHeader(title = "Tags")
    FlowRow(Modifier.testTag("meta-tag-chips")) {
        knownTags.filterNot { it in draft.meta.tags }.forEach { tag ->
            FilterChip(false, { store.updateMeta(draft.meta.copy(tags = draft.meta.tags + tag)) }, { Text("#$tag") }, Modifier.testTag("meta-tag-suggestion-$tag"))
        }
        draft.meta.tags.forEach { tag ->
            FilterChip(true, { store.updateMeta(draft.meta.copy(tags = draft.meta.tags - tag)) }, { Text("#$tag ×") }, Modifier.testTag("meta-tag-selected-$tag"))
        }
    }
    var tagDraft by remember { mutableStateOf("") }
    OutlinedTextField(tagDraft, { tagDraft = it }, label = { Text("Add tag") }, modifier = Modifier.fillMaxWidth().testTag("meta-tag-input"))
    AppSecondaryButton(
        text = "Add tag",
        onClick = {
            val tag = tagDraft.trim().removePrefix("#")
            if (tag.isNotBlank() && tag !in draft.meta.tags) store.updateMeta(draft.meta.copy(tags = draft.meta.tags + tag))
            tagDraft = ""
        },
        modifier = Modifier.testTag("meta-tag-add"),
        leadingIcon = Icons.Outlined.Add,
    )
    OutlinedTextField(draft.meta.memo, { value -> store.updateMeta(draft.meta.copy(memo = value)) }, label = { Text("Memo") }, modifier = Modifier.fillMaxWidth().testTag("meta-memo"))
    Row {
        AppTertiaryButton(
            text = "Clear",
            onClick = { store.updateMeta(draft.meta.copy(ownerSubjectId = null, tags = emptyList(), memo = "")) },
            modifier = Modifier.testTag("meta-clear"),
            leadingIcon = Icons.Outlined.DeleteSweep,
        )
        AppTertiaryButton(
            text = "Cancel",
            onClick = onBack,
            modifier = Modifier.testTag("meta-cancel"),
            leadingIcon = Icons.Outlined.Close,
        )
        AppPrimaryButton(
            text = "Apply",
            onClick = onBack,
            modifier = Modifier.testTag("meta-apply"),
            leadingIcon = Icons.Outlined.Check,
        )
    }
}

@Composable
private fun BehaviorPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val planRoles = QuickCreatePlanRole.entries.toList()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        planRoles.forEachIndexed { index, value ->
            val icon = when (index) {
                0 -> Icons.Outlined.Flag
                1 -> Icons.Outlined.Repeat
                2 -> Icons.Outlined.PlayArrow
                else -> Icons.Outlined.HelpOutline
            }
            SegmentedButton(
                selected = draft.plan.role == value,
                onClick = { store.updateBehavior(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = planRoles.size),
                icon = {
                    Icon(
                        imageVector = if (draft.plan.role == value) Icons.Outlined.Check else icon,
                        contentDescription = null,
                    )
                },
                label = { Text(value.name) },
            )
        }
    }
}

@Composable
private fun JsonEditor(label: String, value: JsonElement, tag: String? = null, onValidValue: (JsonElement) -> Unit) {
    val encoded = Json.encodeToString(JsonElement.serializer(), value)
    var text by remember(encoded) { mutableStateOf(encoded) }
    OutlinedTextField(
        value = text,
        onValueChange = { candidate ->
            text = candidate
            runCatching { Json.parseToJsonElement(candidate) }.getOrNull()?.let(onValidValue)
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().then(if (tag == null) Modifier else Modifier.testTag(tag)),
    )
}

private fun <T> List<T>.replace(index: Int, value: T): List<T> = toMutableList().also { it[index] = value }

private fun JsonElement.jsonObjectOrEmpty(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.string(key: String, fallback: String = ""): String = this[key]?.jsonPrimitive?.content?.takeUnless { it == "null" } ?: fallback
private fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
private fun JsonObject.with(key: String, value: Any?): JsonObject = JsonObject(toMutableMap().also { map -> map[key] = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is Int -> JsonPrimitive(value)
    is Long -> JsonPrimitive(value)
    is Double -> JsonPrimitive(value)
    else -> JsonPrimitive(value.toString())
} })
private fun updateReference(draft: QuickCreateDraftState, store: QuickCreateStateStore, index: Int, reference: QuickCreatePlanReference) = store.updatePlan(draft.plan.copy(references = draft.plan.references.replace(index, reference)))

private fun updateTask(draft: QuickCreateDraftState, store: QuickCreateStateStore, index: Int, task: QuickCreateTaskDefinition) {
    store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(tasks = draft.plan.completion.tasks.replace(index, task))))
}

private fun conditionToJson(condition: QuickCreateConditionNode): JsonElement = buildJsonObject {
    put("kind", JsonPrimitive(condition.kind))
    put("children", buildJsonArray { condition.children.forEach { add(conditionToJson(it)) } })
    put("term", condition.term ?: JsonNull)
}

private fun conditionFromJson(value: JsonElement): QuickCreateConditionNode? = runCatching {
    val objectValue = value.jsonObject
    QuickCreateConditionNode(
        kind = objectValue.getValue("kind").jsonPrimitive.content.toInt(),
        children = objectValue["children"]?.jsonArray?.mapNotNull(::conditionFromJson) ?: emptyList(),
        term = objectValue["term"].takeUnless { it == null || it is JsonNull },
    )
}.getOrNull()

private fun windowRulesToJson(rules: List<QuickCreateWindowRule>): JsonElement = buildJsonArray {
    rules.forEach { rule -> add(buildJsonObject {
        put("id", JsonPrimitive(rule.id)); put("weekdayMask", rule.weekdayMask?.let(::JsonPrimitive) ?: JsonNull)
        put("timeStart", rule.timeStart?.let(::JsonPrimitive) ?: JsonNull); put("timeEnd", rule.timeEnd?.let(::JsonPrimitive) ?: JsonNull)
        put("holidayKind", rule.holidayKind?.let(::JsonPrimitive) ?: JsonNull)
        put("dateStart", rule.dateRange?.startDate?.let(::JsonPrimitive) ?: JsonNull); put("dateEnd", rule.dateRange?.endDate?.let(::JsonPrimitive) ?: JsonNull)
        put("when", rule.`when`?.let(::conditionToJson) ?: JsonNull)
    }) }
}

private fun windowRulesFromJson(value: JsonElement): List<QuickCreateWindowRule>? = runCatching {
    value.jsonArray.map { element ->
        val item = element.jsonObject
        fun string(name: String) = item[name]?.jsonPrimitive?.content?.takeUnless { it == "null" }
        QuickCreateWindowRule(string("id") ?: UUID.randomUUID().toString(), string("weekdayMask")?.toIntOrNull(), string("timeStart"), string("timeEnd"), string("holidayKind")?.toIntOrNull(), string("dateStart")?.let { QuickCreateDateRange(it, string("dateEnd").orEmpty()) }, item["when"]?.let(::conditionFromJson))
    }
}.getOrNull()