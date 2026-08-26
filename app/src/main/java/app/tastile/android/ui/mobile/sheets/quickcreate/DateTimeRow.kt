package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
// m2-allow: m3-component
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
// m2-allow: primitive
import androidx.compose.material3.Text
import app.tastile.android.core.designsystem.theme.LocalTastileCardRoleTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.component.NiaDatePicker
import app.tastile.android.core.designsystem.component.NiaDatePickerDialog
import app.tastile.android.core.designsystem.component.NiaTimePicker
import app.tastile.android.core.designsystem.component.NiaTimePickerDialog
import app.tastile.android.core.designsystem.component.rememberNiaDatePickerState
import app.tastile.android.core.designsystem.component.rememberNiaTimePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Date + time row — directly mirrors
 * `tastile-web/src/features/create-tile/ui/sections/DateTimeRow.tsx`.
 *
 * The icon column reservation (24dp icon + 16dp gap) is provided
 * structurally by the host `FormFieldLayout`, which also enforces the
 * panel-wide 56dp minimum row height. This composable therefore no
 * longer wraps itself in a `FormRow` — it just lays out the date and
 * time triggers at x=0 of the content cell the host gave it, and its
 * content centers vertically within the host's row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeRow(
    dateIso: String,
    timeIso: String?,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    datePlaceholder: String,
    timePlaceholder: String,
    modifier: Modifier = Modifier,
    dateTestTag: String? = null,
    timeTestTag: String? = null,
    showTime: Boolean = true,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateValue = remember(dateIso) { isoToLocalDate(dateIso) }
    val timeValue = remember(timeIso) { isoToLocalTime(timeIso) }

    val pendingDate = remember(dateIso) { mutableStateOf(dateValue) }
    val pendingTime = remember(timeIso) { mutableStateOf(timeValue) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        DateTrigger(
            value = dateValue,
            placeholder = datePlaceholder,
            onClick = { showDatePicker = true },
            onClear = { onDateChange("") },
            testTag = dateTestTag,
            modifier = Modifier.weight(1f),
        )
        if (showTime) {
            Spacer(modifier = Modifier.width(12.dp))
            TimeTrigger(
                value = timeValue,
                placeholder = timePlaceholder,
                onClick = { showTimePicker = true },
                onClear = { onTimeChange("") },
                testTag = timeTestTag,
                modifier = Modifier.width(96.dp),
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberNiaDatePickerState(
            initialSelectedDateMillis = pendingDate.value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        NiaDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis
                        if (millis != null) {
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC).toLocalDate()
                            pendingDate.value = picked
                            onDateChange(combineIso(picked, pendingTime.value))
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        ) {
            NiaDatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberNiaTimePickerState(
            initialHour = pendingTime.value?.hour ?: 9,
            initialMinute = pendingTime.value?.minute ?: 0,
            is24Hour = true,
        )
        NiaTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val pickedTime = LocalTime.of(timeState.hour, timeState.minute)
                        pendingTime.value = pickedTime
                        onTimeChange(
                            pickedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        )
                        showTimePicker = false
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        ) {
            NiaTimePicker(state = timeState)
        }
    }
}

@Composable
private fun DateTrigger(
    value: LocalDate?,
    placeholder: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    testTag: String?,
    modifier: Modifier = Modifier,
) {
    FieldTrigger(
        value = value?.format(DateTimeFormatter.ofPattern("M/d")),
        placeholder = placeholder,
        onClick = onClick,
        onClear = onClear,
        modifier = modifier.then(if (testTag != null) Modifier.testTag("$testTag-date") else Modifier),
    )
}

@Composable
private fun TimeTrigger(
    value: LocalTime?,
    placeholder: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    testTag: String?,
    modifier: Modifier = Modifier,
) {
    FieldTrigger(
        value = value?.format(DateTimeFormatter.ofPattern("HH:mm")),
        placeholder = placeholder,
        onClick = onClick,
        onClear = onClear,
        modifier = modifier.then(if (testTag != null) Modifier.testTag("$testTag-time") else Modifier),
        trailing = {
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

/**
 * Bordered field-style trigger (web parity with Mantine `DateInput` /
 * `TimeSuggestionInput`). Renders as an outlined control with a trailing
 * clear affordance when a value is set, not as bare text.
 *
 * Height matches [RowMinHeight] (56dp) so the trigger sits flush with the
 * surrounding form rows — the previous 48dp minimum left an 8dp vertical
 * gap between the trigger and adjacent ListItem-style rows, which read as
 * inconsistent row heights across the QuickCreate panel.
 */
@Composable
private fun FieldTrigger(
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = LocalTastileCardRoleTokens.current.neutral.container,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalTastileCardRoleTokens.current.completed.border),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value != null) LocalContentColor.current else LocalContentColor.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (value != null && onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

private fun isoToLocalDate(iso: String): LocalDate? = when {
    iso.isBlank() -> null
    iso.length >= 10 && iso[4] == '-' -> runCatching { LocalDate.parse(iso.substring(0, 10)) }.getOrNull()
    else -> runCatching { java.time.OffsetDateTime.parse(iso).toLocalDate() }.getOrNull()
}

private fun isoToLocalTime(iso: String?): LocalTime? = if (iso.isNullOrBlank()) {
    null
} else {
    runCatching { LocalTime.parse(iso) }.getOrNull()
}

private fun combineIso(date: LocalDate?, time: LocalTime?): String {
    if (date == null) return ""
    val t = time ?: LocalTime.MIDNIGHT
    val ldt = LocalDateTime.of(date, t)
    return runCatching {
        ldt.atZone(ZoneId.systemDefault()).toInstant().toString()
    }.getOrElse { ldt.atZone(ZoneOffset.UTC).toInstant().toString() }
}
