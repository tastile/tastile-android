package app.tastile.android.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.TextButton
// m2-allow: m3-component
import androidx.compose.material3.TimePicker
// m2-allow: m3-component
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import java.util.Locale

/**
 * Read-only HH:mm text field paired with an icon button that opens an m3
 * [TimePicker] dialog. Writes back the chosen time as a zero-padded `HH:mm`
 * string on confirm (matching the canonical wire format used elsewhere in
 * the dashboard composer).
 *
 * The string format is preserved so callers can keep their `HH:mm` state
 * shape (e.g. `recurrenceStartTime` / `recurrenceEndTime`) without
 * introducing a `LocalTime` dependency in the surrounding code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val parsedHour = value.split(":").getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val parsedMinute = value.split(":").getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val displayValue = String.format(Locale.US, "%02d:%02d", parsedHour, parsedMinute)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        IconButton(onClick = { showPicker = true }) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = stringResource(R.string.quick_create_select_time)
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = parsedHour,
            initialMinute = parsedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(
                            String.format(
                                Locale.US,
                                "%02d:%02d",
                                pickerState.hour,
                                pickerState.minute
                            )
                        )
                        showPicker = false
                    }
                ) {
                    Text(stringResource(R.string.date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.date_picker_cancel))
                }
            },
            text = {
                TimePicker(state = pickerState)
            }
        )
    }
}