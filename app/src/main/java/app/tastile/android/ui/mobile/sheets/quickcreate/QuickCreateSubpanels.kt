package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
// m2-allow: m3-component
import androidx.compose.material3.DatePicker
// m2-allow: m3-component
import androidx.compose.material3.DatePickerDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.FilledTonalButton
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.ListItem
// m2-allow: m3-component
import androidx.compose.material3.ListItemDefaults
// m2-allow: m3-component
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedButton
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: m3-component
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.AppPrimaryButton
import app.tastile.android.core.designsystem.component.AppSecondaryButton
import app.tastile.android.core.designsystem.component.AppTertiaryButton
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.core.designsystem.component.NiaOutlinedButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.mobile.components.picker.DatePickerSheet
import app.tastile.android.ui.mobile.components.picker.ReferenceOption
import app.tastile.android.ui.mobile.components.picker.ReferencePickerSheet
import app.tastile.android.ui.mobile.components.picker.TimePickerSheet
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
        Modifier
            .testTag("quick-create-subpanel-${panel.name}")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NiaTextButton(
            onClick = onBack,
            text = { Text("Back") },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) },
        )
        Text(
            text = panel.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalDivider()
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
    NiaOutlinedButton(
        onClick = { setWhen(QuickCreateWhenMode.None) },
        modifier = Modifier.fillMaxWidth().testTag("quick-create-when-none"),
        text = { Text("No date or time") },
        leadingIcon = { Icon(Icons.Outlined.EventBusy, contentDescription = null) },
    )
    LocalSectionHeader(title = "When")
    val whenModes = listOf(QuickCreateWhenMode.Day, QuickCreateWhenMode.Range, QuickCreateWhenMode.Reference)
    val whenIcon: (QuickCreateWhenMode) -> androidx.compose.ui.graphics.vector.ImageVector = { mode ->
        when (mode) {
            QuickCreateWhenMode.None -> Icons.Outlined.EventBusy
            QuickCreateWhenMode.Day -> Icons.Outlined.Today
            QuickCreateWhenMode.Range -> Icons.Outlined.DateRange
            QuickCreateWhenMode.Reference -> Icons.Outlined.Tag
        }
    }
    LocalSelectList(
        options = whenModes,
        selected = draft.time.whenMode.takeIf { it in whenModes },
        label = { it.name },
        leading = whenIcon,
        onSelect = { setWhen(it) },
        testTag = { "quick-create-when-${it.name.lowercase()}" },
    )
    if (draft.time.whenMode == QuickCreateWhenMode.Day || draft.time.whenMode == QuickCreateWhenMode.Range) Column(Modifier.testTag("quick-create-calendar")) {
        NativeDateField("Date", draft.time.span.start, "quick-create-start") { value -> store.updateTime(draft.time.copy(span = draft.time.span.copy(start = value))) }
        if (draft.time.whenMode == QuickCreateWhenMode.Range) NativeDateField("End date", draft.time.span.end, "quick-create-end") { value -> store.updateTime(draft.time.copy(span = draft.time.span.copy(end = value))) }
    }
    if (draft.time.whenMode == QuickCreateWhenMode.Reference) Column(Modifier.testTag("quick-create-reference-catalog")) {
        LocalSectionHeader(title = "Reference range")
        LocalPickerField(
            label = stringResource(R.string.picker_reference_label),
            value = draft.time.referenceId.orEmpty().ifBlank { "—" },
            onClick = { showReferencePicker = true },
            leadingIcon = Icons.Outlined.Tag,
            modifier = Modifier.fillMaxWidth().testTag("quick-create-reference-id"),
        )
        OutlinedTextField(draft.time.referenceLabel, { value -> store.updateTime(draft.time.copy(referenceLabel = value)) }, label = { Text("Reference label") }, modifier = Modifier.fillMaxWidth().testTag("quick-create-reference-label"))
    }
    if (draft.time.whenMode != QuickCreateWhenMode.None) {
        LocalSectionHeader(title = "Time of day")
        val timeOfDayModes = QuickCreateTimeOfDayMode.entries.toList()
        LocalSelectList(
            options = timeOfDayModes,
            selected = draft.time.timeOfDayMode,
            label = { it.name },
            leading = { Icons.Outlined.Schedule },
            onSelect = { mode ->
                store.updateTime(if (mode == QuickCreateTimeOfDayMode.Range) draft.time.copy(timeOfDayMode = mode, timeOfDayStart = draft.time.timeOfDayStart.ifBlank { "09:00" }, timeOfDayEnd = draft.time.timeOfDayEnd.ifBlank { "18:00" }) else draft.time.copy(timeOfDayMode = mode, timeOfDayStart = "", timeOfDayEnd = ""))
            },
            testTag = { "quick-create-time-of-day-${it.name.lowercase()}" },
        )
        if (draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.Range) {
            LocalPickerField(
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
            LocalPickerField(
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
            LocalSelectList(
                options = quickRanges,
                selected = quickRanges.firstOrNull { (_, range, _) ->
                    draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.Range &&
                        draft.time.timeOfDayStart == range.first &&
                        draft.time.timeOfDayEnd == range.second
                },
                label = { it.first },
                leading = { it.third },
                onSelect = { (_, range, _) ->
                    store.updateTime(
                        draft.time.copy(
                            timeOfDayMode = QuickCreateTimeOfDayMode.Range,
                            timeOfDayStart = range.first,
                            timeOfDayEnd = range.second,
                        ),
                    )
                },
                testTag = { "quick-create-time-quick-${it.first}" },
            )
        }
    }
    FilledTonalButton(
        onClick = { store.updateWindows(draft.windows + QuickCreateWindow(UUID.randomUUID().toString(), "self", 0, app.tastile.android.ui.mobile.sheets.QuickCreateSpan())) },
        modifier = Modifier.testTag("quick-create-add-window"),
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Add window")
    }
    draft.windows.forEachIndexed { index, window ->
        var showWindowStartDate by remember(index) { mutableStateOf(false) }
        var showWindowEndDate by remember(index) { mutableStateOf(false) }
        var showWindowReferencePicker by remember(index) { mutableStateOf(false) }
        LocalSectionHeader(title = "Window ${index + 1}")
        val windowKinds = listOf(0, 1, 2, 3)
        val windowKindLabel: (Int) -> String = { kind -> when (kind) {
            0 -> "Calendar"
            1 -> "Label span"
            2 -> "Parent span"
            else -> "Gap"
        } }
        val windowKindIcon: (Int) -> androidx.compose.ui.graphics.vector.ImageVector = { kind -> when (kind) {
            0 -> Icons.Outlined.Anchor
            1 -> Icons.Outlined.Link
            2 -> Icons.Outlined.Schedule
            else -> Icons.Outlined.Repeat
        } }
        LocalSelectList(
            options = windowKinds,
            selected = window.kind,
            label = windowKindLabel,
            leading = windowKindIcon,
            onSelect = { kind -> store.updateWindows(draft.windows.replace(index, window.copy(kind = kind))) },
            testTag = { "quick-create-window-$index-kind-$it" },
        )
        LocalPickerField(
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
        LocalPickerField(
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
            LocalPickerField(
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
        FilledTonalButton(
            onClick = { store.updateWindows(draft.windows.filterIndexed { item, _ -> item != index }) },
            modifier = Modifier.testTag("quick-create-window-$index-remove"),
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Remove window")
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

@Composable
private fun DurationPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val duration = draft.time.durationMinMax
    NiaOutlinedButton(
        onClick = { store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(null, null))) },
        modifier = Modifier.fillMaxWidth().testTag("quick-create-duration-none"),
        text = { Text("No duration") },
        leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
    )
    LocalNumberField(
        value = duration.minMs?.div(60_000L)?.toString() ?: "90",
        onValueChange = { value -> value.toLongOrNull()?.let { minutes ->
            val milliseconds = minutes.coerceIn(10L, 720L) * 60_000L
            store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(milliseconds, milliseconds)))
        } },
        label = "Duration",
        suffix = "min",
        modifier = Modifier.fillMaxWidth().testTag("quick-create-duration-minutes"),
    )
    NiaOutlinedButton(
        onClick = { /* wired by caller if needed */ },
        modifier = Modifier.fillMaxWidth().testTag("quick-create-duration-completion-link"),
        text = { Text("Use for completion") },
        leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
    )
}

@Composable
private fun RecurringPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    Column(Modifier.testTag("quick-create-recurring-controls")) {
        val repeatModes = QuickCreateRepeatMode.entries.toList()
        val repeatIcon: (QuickCreateRepeatMode) -> androidx.compose.ui.graphics.vector.ImageVector = { mode ->
            when (mode) {
                QuickCreateRepeatMode.Once -> Icons.Outlined.Event
                QuickCreateRepeatMode.Daily -> Icons.Outlined.Today
                QuickCreateRepeatMode.Weekly -> Icons.Outlined.DateRange
                QuickCreateRepeatMode.Interval -> Icons.Outlined.Repeat
                QuickCreateRepeatMode.Condition -> Icons.Outlined.Autorenew
            }
        }
        LocalSelectList(
            options = repeatModes,
            selected = draft.recurring.repeatMode,
            label = { it.name },
            leading = repeatIcon,
            onSelect = { value ->
                store.updateRecurring(draft.recurring.copy(repeatMode = value))
                if (value != QuickCreateRepeatMode.Once) store.updateIdentity(draft.identity.copy(kind = QuickCreateTileKind.Recurring))
            },
            testTag = { "quick-create-repeat-${it.name.lowercase()}" },
        )
        LocalSectionHeader(
            title = "Weekdays",
            subtitle = if (draft.recurring.repeatMode == QuickCreateRepeatMode.Weekly) null else "Weekly only",
        )
        LocalWeekdayPicker(
            selectedMask = draft.recurring.weekdayMask,
            onToggle = { bit -> store.updateRecurring(draft.recurring.copy(weekdayMask = draft.recurring.weekdayMask xor (1 shl bit))) },
            enabled = draft.recurring.repeatMode == QuickCreateRepeatMode.Weekly,
            testTag = { "quick-create-weekday-$it" },
        )
        NiaOutlinedButton(
            onClick = { store.updateRecurring(draft.recurring.copy(endDate = if (draft.recurring.endDate.isBlank()) LocalDate.now().toString() else "")) },
            modifier = Modifier.testTag("quick-create-recurring-end-switch"),
            text = { Text("End date") },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
        )
        if (draft.recurring.endDate.isNotBlank()) NativeDateField("End date", draft.recurring.endDate, "quick-create-recurring-end-date") { value -> store.updateRecurring(draft.recurring.copy(endDate = value)) }
    }
}

@Composable
private fun ReferencesPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    FilledTonalButton(
        onClick = { store.updatePlan(draft.plan.copy(references = draft.plan.references + defaultPlanReference())) },
        modifier = Modifier.testTag("quick-create-add-reference"),
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Add reference")
    }
    draft.plan.references.forEachIndexed { index, reference ->
        val target = reference.target.jsonObjectOrEmpty()
        val pick = reference.pick.jsonObjectOrEmpty()
        OutlinedTextField(reference.id, { value -> updateReference(draft, store, index, reference.copy(id = value)) }, label = { Text("Reference ID") }, modifier = Modifier.testTag("quick-create-reference-record-id-$index"))
        OutlinedTextField(target.string("referenceId"), { value -> updateReference(draft, store, index, reference.copy(target = target.with("referenceId", value.ifBlank { null }))) }, label = { Text("Target reference") }, modifier = Modifier.testTag("quick-create-reference-id-$index"))
        val targetKinds = listOf(0, 1, 2)
        val targetKindLabel: (Int) -> String = { kind -> when (kind) {
            0 -> "Exact"
            1 -> "Series"
            else -> "Filter"
        } }
        val targetKindIcon: (Int) -> androidx.compose.ui.graphics.vector.ImageVector = { kind -> if (kind == 0) Icons.Outlined.Tag else Icons.Outlined.Link }
        LocalSelectList(
            options = targetKinds,
            selected = target["kind"]?.jsonPrimitive?.content?.toIntOrNull(),
            label = targetKindLabel,
            leading = targetKindIcon,
            onSelect = { kind -> updateReference(draft, store, index, reference.copy(target = target.with("kind", kind))) },
            testTag = { "quick-create-reference-record-$index-target-kind-$it" },
        )
        LocalSectionHeader(title = "Relation")
        val relations = listOf(4, 3, 1, 2, 0)
        val relationLabel: (Int) -> String = { relation -> when (relation) {
            0 -> "Touch"
            1 -> "Inside"
            2 -> "Overlap"
            3 -> "Before"
            else -> "After"
        } }
        val relationIcon: (Int) -> androidx.compose.ui.graphics.vector.ImageVector = { Icons.Outlined.Link }
        LocalSelectList(
            options = relations,
            selected = pick["kind"]?.jsonPrimitive?.content?.toIntOrNull(),
            label = relationLabel,
            leading = relationIcon,
            onSelect = { relation -> updateReference(draft, store, index, reference.copy(pick = pick.with("kind", relation))) },
            testTag = { "quick-create-reference-record-$index-relation-$it" },
        )
        LocalNumberField(
            value = pick.string("momentId", "10"),
            onValueChange = { value -> value.toIntOrNull()?.coerceIn(5, 120)?.let { minutes -> updateReference(draft, store, index, reference.copy(pick = pick.with("momentId", minutes.toString()))) } },
            label = "Interval",
            suffix = "min",
            modifier = Modifier.fillMaxWidth().testTag("quick-create-reference-record-$index-interval"),
        )
        FilledTonalButton(
            onClick = { store.updatePlan(draft.plan.copy(references = draft.plan.references.filterIndexed { item, _ -> item != index })) },
            modifier = Modifier.testTag("quick-create-reference-record-$index-remove"),
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Remove reference")
        }
    }
}

private fun defaultPlanReference() = QuickCreatePlanReference(
    id = "",
    target = JsonObject(mapOf("kind" to JsonPrimitive(0), "contextKind" to JsonNull, "referenceId" to JsonNull, "conditionId" to JsonNull)),
    pick = JsonObject(mapOf("kind" to JsonPrimitive(4), "momentId" to JsonPrimitive("10"))),
)

@Composable
private fun IntentPanel(store: QuickCreateStateStore) {
    LocalSectionHeader(title = "Add condition or group")
    val intentTargets = listOf(
        Triple("Time", QuickCreatePanel.Time, Icons.Outlined.Schedule),
        Triple("References", QuickCreatePanel.References, Icons.Outlined.Link),
        Triple("Recurring", QuickCreatePanel.Recurring, Icons.Outlined.Repeat),
        Triple("Meta", QuickCreatePanel.Meta, Icons.Outlined.Tag),
        Triple("Completion", QuickCreatePanel.Completion, Icons.Outlined.Check),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        intentTargets.forEach { (label, panel, icon) ->
            NiaButton(
                onClick = { store.openSubpanel(panel) },
                modifier = Modifier.fillMaxWidth().testTag("quick-create-intent-${label.lowercase()}"),
                leadingIcon = { Icon(icon, contentDescription = null) },
                text = { Text(label) },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NativeDateField(label: String, value: String, tag: String, onSelected: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    LocalPickerField(
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
                NiaButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis -> onSelected(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toString()) }
                        open = false
                    },
                    leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
                    text = { Text("OK") },
                )
            },
            dismissButton = {
                NiaTextButton(
                    onClick = { open = false },
                    leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                    text = { Text("Cancel") },
                )
            },
        ) { DatePicker(state = state) }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CompletionPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    LocalSectionHeader(title = "Logic")
    val logicKinds = listOf(0 to "ALL", 1 to "ANY", 2 to "NOT")
    val logicIcon: (Int) -> androidx.compose.ui.graphics.vector.ImageVector = { kind -> when (kind) {
        0 -> Icons.Outlined.PlaylistAddCheck
        1 -> Icons.Outlined.PlaylistAdd
        else -> Icons.Outlined.Block
    } }
    LocalSelectList(
        options = logicKinds,
        selected = logicKinds.firstOrNull { it.first == draft.plan.completion.root.kind },
        label = { it.second },
        leading = { logicIcon(it.first) },
        onSelect = { (kind, _) -> store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = draft.plan.completion.root.copy(kind = kind, term = null)))) },
        testTag = { "quick-create-completion-logic-${it.second.lowercase()}" },
    )
    ConditionControls(draft.plan.completion.root, onChange = { root -> store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = root))) }, allowTermKind = false)
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NiaFilledTonalButton(
            onClick = { addCompletionTerm(draft, store, "task") },
            modifier = Modifier.testTag("quick-create-completion-add-task"),
            leadingIcon = { Icon(Icons.Outlined.Task, contentDescription = null) },
            text = { Text("Task") },
        )
        NiaFilledTonalButton(
            onClick = { addCompletionTerm(draft, store, "relation") },
            modifier = Modifier.testTag("quick-create-completion-add-relation"),
            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
            text = { Text("Relation") },
        )
        NiaFilledTonalButton(
            onClick = { addCompletionTerm(draft, store, "metric") },
            modifier = Modifier.testTag("quick-create-completion-add-metric"),
            leadingIcon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
            text = { Text("Metric") },
        )
    }
    NiaFilledTonalButton(
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
        leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
        text = { Text("Add time requirement") },
    )
    draft.plan.completion.timeRequirements.forEachIndexed { index, requirement ->
        val required = requirement.required.jsonObjectOrEmpty()
        val minimumMinutes = required.long("minMs")?.div(60_000L)?.toString().orEmpty()
        LocalNumberField(
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
            label = "Minutes",
            suffix = "min",
            modifier = Modifier.fillMaxWidth().testTag("time-requirement-$index-required-minutes"),
        )
        NiaFilledTonalButton(
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
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            text = { Text("Remove time requirement") },
        )
    }
    NiaTextButton(
        onClick = {
            store.updatePlan(draft.plan.copy(completion = draft.plan.completion.copy(root = QuickCreateConditionNode(0), timeRequirements = emptyList(), tasks = emptyList())))
        },
        modifier = Modifier.testTag("quick-create-completion-clear"),
        leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
        text = { Text("Clear completion") },
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
    val logicIcon: (Int) -> androidx.compose.ui.graphics.vector.ImageVector = { kind -> when (kind) {
        0 -> Icons.Outlined.PlaylistAddCheck
        1 -> Icons.Outlined.PlaylistAdd
        2 -> Icons.Outlined.Block
        else -> Icons.Outlined.TextFields
    } }
    LocalSelectList(
        options = logicKinds,
        selected = logicKinds.firstOrNull { it.first == node.kind },
        label = { it.second },
        leading = { logicIcon(it.first) },
        onSelect = { (kind, _) -> onChange(node.copy(kind = kind, children = if (kind == 3) emptyList() else node.children, term = if (kind == 3) defaultTermValue("calendar") else null)) },
        testTag = { "condition-$path-logic-${it.second.lowercase()}" },
    )
    if (node.kind == 3) {
        val termTypes = listOf("calendar", "moment", "relation", "gap", "requirement", "task", "fact", "metric", "life")
        val termIcon: (String) -> androidx.compose.ui.graphics.vector.ImageVector = { type -> when (type) {
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
        } }
        LocalSelectList(
            options = termTypes,
            selected = node.term?.jsonObjectOrEmpty()?.string("kind"),
            label = { it },
            leading = termIcon,
            onSelect = { type -> onChange(node.copy(term = defaultTermValue(type))) },
            testTag = { "condition-$path-term-$it" },
        )
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
            NiaFilledTonalButton(
                onClick = { onChange(node.copy(children = node.children.filterIndexed { item, _ -> item != index })) },
                modifier = Modifier.testTag("condition-$path-child-$index-remove"),
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                text = { Text("Remove") },
            )
        }
        NiaFilledTonalButton(
            onClick = { onChange(node.copy(children = node.children + QuickCreateConditionNode(3, term = defaultTermValue("calendar")))) },
            modifier = Modifier.testTag("condition-$path-add-child"),
            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("Add condition") },
        )
    }
}

@Composable private fun CalendarTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    LocalNumberField(
        value = value.string("weekdayMask", "0"),
        onValueChange = { input -> onChange(term.withValue("weekdayMask", input.toIntOrNull() ?: 0)) },
        label = "Weekday mask",
        suffix = "0–127",
        modifier = Modifier.fillMaxWidth().testTag("condition-$path-calendar-weekday-mask"),
    )
    LocalNumberField(
        value = value.string("offsetMin", "0"),
        onValueChange = { input -> onChange(term.withValue("offsetMin", input.toIntOrNull() ?: 0)) },
        label = "Offset minutes",
        suffix = "min",
        modifier = Modifier.fillMaxWidth().testTag("condition-$path-calendar-offset"),
    )
    OutlinedTextField(value.string("timeStart"), { input -> onChange(term.withValue("timeStart", input.ifBlank { null })) }, label = { Text("Time start") }, modifier = Modifier.fillMaxWidth().testTag("condition-calendar-start"))
    OutlinedTextField(value.string("timeEnd"), { input -> onChange(term.withValue("timeEnd", input.ifBlank { null })) }, label = { Text("Time end") }, modifier = Modifier.fillMaxWidth().testTag("condition-calendar-end"))
}

@Composable private fun MomentTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("referenceId"), { input -> onChange(term.withValue("referenceId", input.ifBlank { null })) }, label = { Text("Reference ID") }, modifier = Modifier.fillMaxWidth().testTag("condition-moment-reference"))
    LocalNumberField(
        value = value.string("offsetMs", "0"),
        onValueChange = { input -> onChange(term.withValue("offsetMs", input.toLongOrNull() ?: 0L)) },
        label = "Offset ms",
        suffix = "ms",
        modifier = Modifier.fillMaxWidth().testTag("condition-moment-offset"),
    )
}

@Composable private fun RelationTermFields(term: JsonObject, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("referenceId"), { input -> onChange(term.withValue("referenceId", input)) }, label = { Text("Reference ID") }, modifier = Modifier.fillMaxWidth().testTag("condition-relation-reference"))
    val relations = listOf(0, 1, 2, 3, 4)
    val relationLabel: (Int) -> String = { r -> when (r) {
        0 -> "Touch"
        1 -> "Inside"
        2 -> "Overlap"
        3 -> "Before"
        else -> "After"
    } }
    LocalSelectList(
        options = relations,
        selected = value.string("relation", "0").toIntOrNull(),
        label = relationLabel,
        leading = { Icons.Outlined.Link },
        onSelect = { relation -> onChange(term.withValue("relation", relation)) },
        testTag = { "condition-relation-kind-$it" },
    )
    val windowKinds = listOf(0, 1, 2, 3)
    val windowKindLabel: (Int) -> String = { k -> when (k) {
        0 -> "Calendar"
        1 -> "Label span"
        2 -> "Parent span"
        else -> "Gap"
    } }
    LocalSelectList(
        options = windowKinds,
        selected = value.string("windowKind", "0").toIntOrNull(),
        label = windowKindLabel,
        leading = { Icons.Outlined.Schedule },
        onSelect = { kind -> onChange(term.withValue("windowKind", kind)) },
        testTag = { "condition-relation-window-$it" },
    )
}

@Composable private fun TaskTermFields(term: JsonObject, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("taskId"), { input -> onChange(term.withValue("taskId", input)) }, label = { Text("Task ID") }, modifier = Modifier.fillMaxWidth().testTag("condition-task-id"))
    LocalNumberField(
        value = value.string("state", "0"),
        onValueChange = { input -> onChange(term.withValue("state", input.toIntOrNull() ?: 0)) },
        label = "State",
        suffix = "0–N",
        modifier = Modifier.fillMaxWidth().testTag("condition-task-state"),
    )
}

@Composable private fun GapTermFields(path: String) {
    // Current Web TermFields intentionally exposes Gap as an informational placeholder only.
    Text("Gap configuration is preserved when supplied by the API.", modifier = Modifier.testTag("condition-$path-gap-placeholder"))
}

@Composable private fun RequirementTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("requirementId"), { input ->
        onChange(term.withValue("requirementId", input))
    }, label = { Text("Requirement ID") }, modifier = Modifier.fillMaxWidth().testTag("condition-$path-requirement-id"))
    LocalNumberField(
        value = value.string("state", "0"),
        onValueChange = { input -> onChange(term.withValue("state", input.toIntOrNull() ?: 0)) },
        label = "State",
        suffix = "0–N",
        modifier = Modifier.fillMaxWidth().testTag("condition-$path-requirement-state"),
    )
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
    }, label = { Text("ID") }, modifier = Modifier.fillMaxWidth().testTag("condition-$path-$kind-id"))
    LocalNumberField(
        value = value.string("op", "0"),
        onValueChange = { input -> onChange(term.withValue("op", input.toIntOrNull() ?: 0)) },
        label = "Operator",
        modifier = Modifier.fillMaxWidth().testTag("condition-$path-$kind-op"),
    )
    OutlinedTextField(value.string("value"), { input ->
        onChange(term.withValue("value", scalarValue(input)))
    }, label = { Text("Value") }, modifier = Modifier.fillMaxWidth().testTag("condition-$path-$kind-value"))
}

@Composable private fun LifeTermFields(term: JsonObject, path: String, onChange: (JsonObject) -> Unit) {
    val value = term.valueObject()
    OutlinedTextField(value.string("target"), { input ->
        onChange(term.withValue("target", input))
    }, label = { Text("Target") }, modifier = Modifier.fillMaxWidth().testTag("condition-$path-life-target"))
    LocalNumberField(
        value = value.string("state", "0"),
        onValueChange = { input -> onChange(term.withValue("state", input.toIntOrNull() ?: 0)) },
        label = "State",
        suffix = "0–N",
        modifier = Modifier.fillMaxWidth().testTag("condition-$path-life-state"),
    )
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
    LocalSectionHeader(title = "Project")
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
    LocalSectionHeader(title = "Tags")
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
    NiaFilledTonalButton(
        onClick = {
            val tag = tagDraft.trim().removePrefix("#")
            if (tag.isNotBlank() && tag !in draft.meta.tags) store.updateMeta(draft.meta.copy(tags = draft.meta.tags + tag))
            tagDraft = ""
        },
        modifier = Modifier.testTag("meta-tag-add"),
        leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
        text = { Text("Add tag") },
    )
    OutlinedTextField(draft.meta.memo, { value -> store.updateMeta(draft.meta.copy(memo = value)) }, label = { Text("Memo") }, modifier = Modifier.fillMaxWidth().testTag("meta-memo"))
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NiaTextButton(
            onClick = { store.updateMeta(draft.meta.copy(ownerSubjectId = null, tags = emptyList(), memo = "")) },
            modifier = Modifier.testTag("meta-clear"),
            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
            text = { Text("Clear") },
        )
        NiaTextButton(
            onClick = onBack,
            modifier = Modifier.testTag("meta-cancel"),
            leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
            text = { Text("Cancel") },
        )
        NiaButton(
            onClick = onBack,
            modifier = Modifier.testTag("meta-apply"),
            leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
            text = { Text("Apply") },
        )
    }
}

@Composable
private fun BehaviorPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val planRoles = QuickCreatePlanRole.entries.toList()
    val roleIcon: (QuickCreatePlanRole) -> androidx.compose.ui.graphics.vector.ImageVector = { role ->
        when (role) {
            QuickCreatePlanRole.Label -> Icons.Outlined.Flag
            QuickCreatePlanRole.Executable -> Icons.Outlined.PlayArrow
        }
    }
    LocalSelectList(
        options = planRoles,
        selected = draft.plan.role,
        label = { it.name },
        leading = roleIcon,
        onSelect = { store.updateBehavior(it) },
        testTag = { "behavior-role-${it.name.lowercase()}" },
    )
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

// ── Local design system helpers (NiA equivalents of the deleted App* primitives) ──

@Composable
internal fun LocalSectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> LocalSelectList(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    leading: ((T) -> androidx.compose.ui.graphics.vector.ImageVector)? = null,
    onSelect: (T) -> Unit,
    testTag: ((T) -> String)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        options.forEach { option ->
            val isSelected = option == selected
            ListItem(
                headlineContent = { Text(label(option)) },
                leadingContent = if (leading != null) {
                    { Icon(leading(option), contentDescription = null) }
                } else {
                    null
                },
                trailingContent = if (isSelected) {
                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                } else {
                    null
                },
                colors = if (isSelected) {
                    ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    ListItemDefaults.colors()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (testTag != null) Modifier.testTag(testTag(option)) else Modifier,
                    ),
            )
        }
    }
}

@Composable
internal fun LocalNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        suffix = if (suffix != null) { { Text(suffix) } } else null,
    )
}

@Composable
internal fun LocalPickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LocalWeekdayPicker(
    selectedMask: Int,
    onToggle: (Int) -> Unit,
    enabled: Boolean,
    testTag: (Int) -> String,
) {
    val days = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEachIndexed { index, name ->
            val bit = 1 shl index
            FilterChip(
                selected = (selectedMask and bit) != 0,
                onClick = { if (enabled) onToggle(index) },
                label = { Text(name) },
                enabled = enabled,
                modifier = Modifier.testTag(testTag(index)),
            )
        }
    }
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
