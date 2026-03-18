package app.tastile.android.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DurationInput(
    hours: String,
    minutes: String,
    title: String,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onManualEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val parsedHours = hours.toIntOrNull()?.coerceIn(0, 99) ?: 0
    val parsedMinutes = minutes.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val displayValue = String.format("%02d:%02d", parsedHours, parsedMinutes)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            singleLine = true
        )
        IconButton(onClick = { showPicker = true }) {
            Icon(Icons.Default.Schedule, contentDescription = title)
        }
    }

    if (showPicker) {
        DurationPickerDialog(
            initialHours = parsedHours,
            initialMinutes = parsedMinutes,
            title = title,
            onConfirm = { h, m ->
                onHoursChange(h.toString())
                onMinutesChange(m.toString())
                onManualEdit()
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}
