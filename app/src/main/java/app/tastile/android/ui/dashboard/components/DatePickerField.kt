package app.tastile.android.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
// m2-allow: primitive
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaDatePicker
import app.tastile.android.core.designsystem.component.NiaDatePickerDialog
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.core.designsystem.component.rememberNiaDatePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Read-only date text field paired with an icon button that opens the
 * dashboard's M3 [NiaDatePicker] dialog. Writes back the chosen date as the
 * canonical `yyyy-MM-dd` string (matching the wire format the rest of the
 * dashboard composer expects). Wraps the same `OutlinedTextField` +
 * `IconButton` shape as [TimePickerField] so the two pickers stay visually
 * consistent.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val initialMillis = runCatching { LocalDate.parse(value) }
        .getOrNull()
        ?.atStartOfDay(ZoneId.systemDefault())
        ?.toInstant()
        ?.toEpochMilli()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        IconButton(onClick = { showPicker = true }) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = stringResource(R.string.quick_create_select_date)
            )
        }
    }

    if (showPicker) {
        val dateState = rememberNiaDatePickerState(initialSelectedDateMillis = initialMillis)
        NiaDatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                NiaButton(
                    onClick = {
                        val millis = dateState.selectedDateMillis
                        if (millis != null) {
                            val selected = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onValueChange(selected.toString())
                        }
                        showPicker = false
                    },
                    text = { Text(stringResource(R.string.date_picker_confirm)) },
                )
            },
            dismissButton = {
                NiaTextButton(
                    onClick = { showPicker = false },
                    text = { Text(stringResource(R.string.date_picker_cancel)) },
                )
            },
        ) {
            NiaDatePicker(state = dateState)
        }
    }
}