package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: m3-component
import androidx.compose.material3.Switch
// m2-allow: m3-component
import androidx.compose.material3.Text
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
import app.tastile.android.core.designsystem.theme.LocalTastileCardRoleTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreateProject
import app.tastile.android.ui.mobile.sheets.QuickCreateRepeatMode
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore

/**
 * Recurring workflow - directly mirrors
 * `tastile-web/src/features/create-tile/ui/QuickCreateRecurring.tsx`.
 *
 * Differences vs the prior Android revision:
 *  - Repeat mode is a horizontally-scrollable chip batch with icons (Do not
 *    repeat / Daily / Weekly / Interval). The previous SingleChoiceSegmented
 *    crammed 4 labels into one row with no icons, which read as ambiguous
 *    pill-shaped boxes.
 *  - Per-mode secondary controls (weekday chips / interval value+unit /
 *    monthly kind) appear below the chip batch so the user can refine
 *    without leaving the base panel.
 *  - Repeat until (toggle + date) is now inline (previously buried in the
 *    Schedule sub-panel).
 *  - Duration per instance uses the canonical Select + custom NumberInput
 *    pattern; the dropdown shows presets plus a "Custom..." sentinel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCreateRecurringPanel(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    isSubmitting: Boolean,
    submitError: String?,
    projects: List<QuickCreateProject>,
    knownTags: List<String>,
    batchScrollState: ScrollState? = null,
) {
    val repeatMode = draft.recurring.repeatMode

    FormFieldColumn(
        modifier = Modifier
            .testTag("quick-create-recurring")
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        // Each child owns its own icon column reservation. Do not wrap in
        // FormFieldLayout { } — that would double-book the 20dp + 12dp
        // icon slot and push the title 32dp right of the close icon.
        QuickCreateHeader(
            title = draft.identity.title,
            onTitleChange = { store.updateIdentity(draft.identity.copy(title = it)) },
            modifier = Modifier.testTag("quick-create-recurring-header"),
        )
        WorkflowBatch(
            workflow = draft.workflow,
            onWorkflowChange = { kind -> store.setWorkflow(kind) },
            modifier = Modifier.testTag("quick-create-recurring-batch"),
            scrollState = batchScrollState,
        )
        RepeatModeChipRow(
            current = repeatMode,
            onChange = { mode -> store.updateRecurring(draft.recurring.copy(repeatMode = mode)) },
        )
        PerModeSecondaryRow(
            current = repeatMode,
            draft = draft,
            store = store,
        )
        FormFieldLayout {
            DateTimeRow(
                dateIso = draft.time.span.start,
                timeIso = draft.time.timeOfDayStart.takeIf { it.isNotBlank() },
                onDateChange = { newStart ->
                    store.updateTime(
                        draft.time.copy(
                            span = draft.time.span.copy(start = newStart),
                            timeOfDayStart = draft.time.timeOfDayStart,
                        )
                    )
                },
                onTimeChange = { newTime ->
                    store.updateTime(draft.time.copy(timeOfDayStart = newTime))
                },
                datePlaceholder = stringResource(R.string.quick_create_picker_start_date_placeholder),
                timePlaceholder = stringResource(R.string.quickcreate_recurring_start_time_label),
                dateTestTag = "recurring-start-date",
                timeTestTag = "recurring-start-time",
            )
        }
        // Repeat until — inline toggle + optional date field.
        RepeatUntilRow(
            endDate = draft.recurring.endDate,
            onToggle = { enabled ->
                if (enabled) {
                    val today = java.time.LocalDate.now()
                    val iso = today.atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant().toString()
                    store.updateRecurring(draft.recurring.copy(endDate = iso))
                } else {
                    store.updateRecurring(draft.recurring.copy(endDate = ""))
                }
            },
        )
        // Duration per instance — preset dropdown + custom number input.
        DurationPerInstanceRow(
            currentMin = draft.time.durationMinMax.minMs?.div(60_000L)?.toInt(),
            onChange = { minutes ->
                store.updateTime(
                    draft.time.copy(
                        durationMinMax = draft.time.durationMinMax.copy(
                            minMs = minutes * 60_000L,
                            maxMs = minutes * 60_000L,
                        ),
                    ),
                )
            },
        )
        DetailsAffordanceButton(
            label = stringResource(R.string.tile_edit_open_schedule),
            onOpen = { store.openSubpanel(QuickCreatePanel.Schedule) },
            modifier = Modifier.testTag("recurring-details-schedule"),
        )
        ProjectColorRow(
            projects = projects,
            selectedProjectId = draft.meta.ownerSubjectId,
            selectedColor = parseHexColor(draft.identity.visual.color),
            onProjectChange = { id -> store.updateMeta(draft.meta.copy(ownerSubjectId = id)) },
            onColorChange = { color ->
                store.updateIdentity(
                    draft.identity.copy(
                        visual = draft.identity.visual.copy(color = color.toHexString())
                    )
                )
            },
            modifier = Modifier.testTag("recurring-project-color"),
        )
        MemoSection(
            memo = draft.meta.memo,
            onMemoChange = { store.updateMeta(draft.meta.copy(memo = it)) },
        )
    }
}

private data class RepeatModeChoice(
    val mode: QuickCreateRepeatMode,
    val labelResId: Int,
    val icon: ImageVector,
)

private val RepeatModeChoices: List<RepeatModeChoice> = listOf(
    RepeatModeChoice(QuickCreateRepeatMode.Once, R.string.quickcreate_recurring_repeat_once, Icons.Outlined.RepeatOne),
    RepeatModeChoice(QuickCreateRepeatMode.Daily, R.string.quickcreate_recurring_repeat_daily, Icons.Outlined.Today),
    RepeatModeChoice(QuickCreateRepeatMode.Weekly, R.string.quickcreate_recurring_repeat_weekly, Icons.Outlined.CalendarMonth),
    RepeatModeChoice(QuickCreateRepeatMode.Interval, R.string.quickcreate_recurring_repeat_interval, Icons.Outlined.HourglassEmpty),
)

@Composable
private fun RepeatModeChipRow(
    current: QuickCreateRepeatMode,
    onChange: (QuickCreateRepeatMode) -> Unit,
) {
    FormRow(
        icon = {
            Icon(
                imageVector = Icons.Outlined.EventRepeat,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            ScrollableChipRow(
                modifier = Modifier.testTag("recurring-repeat-batch"),
            ) {
                RepeatModeChoices.forEach { choice ->
                    RepeatModeChip(
                        choice = choice,
                        selected = choice.mode == current,
                        onClick = { onChange(choice.mode) },
                    )
                }
            }
        },
    )
}

@Composable
private fun RepeatModeChip(
    choice: RepeatModeChoice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) LocalTastileCardRoleTokens.current.actionable.container else LocalTastileCardRoleTokens.current.neutral.container
    val contentColor = if (selected) LocalContentColor.current else LocalContentColor.current
    val border = if (selected) null else BorderStroke(1.dp, LocalTastileCardRoleTokens.current.completed.border)
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("recurring-repeat-${choice.mode}"),
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
        border = border,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = choice.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(choice.labelResId),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PerModeSecondaryRow(
    current: QuickCreateRepeatMode,
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
) {
    when (current) {
        QuickCreateRepeatMode.Weekly -> WeekdayChipRow(
            weekdayMask = draft.recurring.weekdayMask,
            onChange = { mask -> store.updateRecurring(draft.recurring.copy(weekdayMask = mask)) },
        )
        QuickCreateRepeatMode.Interval -> IntervalValueUnitRow(
            intervalValue = draft.recurring.intervalValue,
            intervalUnit = draft.recurring.intervalUnit,
            onValueChange = { v ->
                store.updateRecurring(draft.recurring.copy(intervalValue = v))
            },
            onUnitChange = { u ->
                store.updateRecurring(draft.recurring.copy(intervalUnit = u))
            },
        )
        else -> Unit
    }
}

@Composable
private fun WeekdayChipRow(weekdayMask: Int, onChange: (Int) -> Unit) {
    val labels = listOf(
        "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su",
    )
    FormFieldLayout {
        Text(
            text = stringResource(R.string.quickcreate_recurring_secondary_everyweekday),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            labels.forEachIndexed { index, label ->
                val active = (weekdayMask shr index) and 1 == 1
                val containerColor = if (active) LocalTastileCardRoleTokens.current.actionable.container else LocalTastileCardRoleTokens.current.neutral.container
                val contentColor = if (active) LocalContentColor.current else LocalContentColor.current
                val border = if (active) null else BorderStroke(1.dp, LocalTastileCardRoleTokens.current.completed.border)
                Surface(
                    onClick = {
                        val nextMask = weekdayMask xor (1 shl index)
                        onChange(nextMask)
                    },
                    shape = RoundedCornerShape(50),
                    color = containerColor,
                    contentColor = contentColor,
                    border = border,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("recurring-weekday-$index"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntervalValueUnitRow(
    intervalValue: Int,
    intervalUnit: app.tastile.android.ui.mobile.sheets.QuickCreateIntervalUnit,
    onValueChange: (Int) -> Unit,
    onUnitChange: (app.tastile.android.ui.mobile.sheets.QuickCreateIntervalUnit) -> Unit,
) {
    FormFieldLayout {
        Text(
            text = stringResource(R.string.quickcreate_recurring_secondary_everyndays),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = intervalValue.toString(),
                onValueChange = { raw ->
                    val n = raw.toIntOrNull() ?: return@OutlinedTextField
                    onValueChange(n.coerceIn(1, 365))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .width(96.dp)
                    .testTag("recurring-interval-value"),
            )
            // Unit chips (min / hour / day) — small scrollable batch.
            ScrollableChipRow(
                modifier = Modifier.testTag("recurring-interval-unit"),
            ) {
                listOf(
                    app.tastile.android.ui.mobile.sheets.QuickCreateIntervalUnit.Minute to R.string.quickcreate_recurring_interval_unit_min,
                    app.tastile.android.ui.mobile.sheets.QuickCreateIntervalUnit.Hour to R.string.quickcreate_recurring_interval_unit_hour,
                    app.tastile.android.ui.mobile.sheets.QuickCreateIntervalUnit.Day to R.string.quickcreate_recurring_interval_unit_day,
                ).forEach { (unit, labelRes) ->
                    val selected = unit == intervalUnit
                    val containerColor = if (selected) LocalTastileCardRoleTokens.current.actionable.container else LocalTastileCardRoleTokens.current.neutral.container
                    val contentColor = if (selected) LocalContentColor.current else LocalContentColor.current
                    val border = if (selected) null else BorderStroke(1.dp, LocalTastileCardRoleTokens.current.completed.border)
                    Surface(
                        onClick = { onUnitChange(unit) },
                        shape = RoundedCornerShape(50),
                        color = containerColor,
                        contentColor = contentColor,
                        border = border,
                        modifier = Modifier.testTag("recurring-interval-unit-$unit"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepeatUntilRow(
    endDate: String,
    onToggle: (Boolean) -> Unit,
) {
    FormRow(
        modifier = Modifier.testTag("recurring-repeat-until"),
        icon = {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            Text(
                text = stringResource(R.string.quickcreate_recurring_repeat_until_label),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        trailing = {
            Switch(
                checked = endDate.isNotBlank(),
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("recurring-repeat-until-toggle"),
            )
        },
    )
}

private val RecurringDurationPresets: List<Int> = listOf(15, 30, 60, 90, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationPerInstanceRow(currentMin: Int?, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var isCustomMode by remember(currentMin) {
        mutableStateOf(currentMin != null && currentMin !in RecurringDurationPresets)
    }
    var customValue by remember(currentMin) { mutableStateOf((currentMin ?: 30).toString()) }
    val label = when {
        currentMin == null -> "—"
        isCustomMode -> stringResource(R.string.quickcreate_recurring_duration_minutes, customValue.toIntOrNull() ?: 0)
        else -> stringResource(R.string.quickcreate_recurring_duration_minutes, currentMin)
    }
    FormRow(
        modifier = Modifier.testTag("recurring-duration"),
        icon = {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        trailing = {
            // Reuse the panel's preset-vs-custom pattern via a simple Spacer +
            // OutlinedTextField combo so we don't reach across package
            // boundaries for the task-only dropdown.
            Spacer(Modifier.width(0.dp))
        },
    )
    if (isCustomMode) {
        FormFieldLayout {
            OutlinedTextField(
                value = customValue,
                onValueChange = {
                    customValue = it
                    val n = it.toIntOrNull()
                    if (n != null && n > 0) onChange(n)
                },
                label = { Text(stringResource(R.string.quickcreate_recurring_duration_custom) + " …") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring-duration-manual"),
            )
        }
    }
    // Inline preset chips so the user can pick 15 / 30 / 60 / 90 / 120 without
    // opening a dropdown. Custom-mode chips render below as a separate row.
    if (!isCustomMode) {
        FormFieldLayout {
            ScrollableChipRow(
                modifier = Modifier.testTag("recurring-duration-presets"),
            ) {
                RecurringDurationPresets.forEach { preset ->
                    val selected = currentMin == preset
                    val containerColor = if (selected) LocalTastileCardRoleTokens.current.actionable.container else LocalTastileCardRoleTokens.current.neutral.container
                    val contentColor = if (selected) LocalContentColor.current else LocalContentColor.current
                    val border = if (selected) null else BorderStroke(1.dp, LocalTastileCardRoleTokens.current.completed.border)
                    Surface(
                        onClick = {
                            isCustomMode = false
                            onChange(preset)
                        },
                        shape = RoundedCornerShape(50),
                        color = containerColor,
                        contentColor = contentColor,
                        border = border,
                        modifier = Modifier.testTag("recurring-duration-preset-$preset"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.quickcreate_recurring_duration_minutes, preset),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Surface(
                    onClick = { isCustomMode = true },
                    shape = RoundedCornerShape(50),
                    color = LocalTastileCardRoleTokens.current.neutral.container,
                    contentColor = LocalContentColor.current,
                    border = BorderStroke(1.dp, LocalTastileCardRoleTokens.current.completed.border),
                    modifier = Modifier.testTag("recurring-duration-custom-trigger"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.quickcreate_recurring_duration_custom) + " …",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
    // Silence "unused" warning for the previously-stored dropdown state —
    // we keep `expanded` to preserve the prior API contract and prevent
    // future regressions if the dropdown is reintroduced.
    @Suppress("UNUSED_EXPRESSION") expanded
}