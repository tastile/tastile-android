package app.tastile.android.ui.dashboard.components

// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DurationPickerDialog(
    initialHours: Int,
    initialMinutes: Int,
    title: String,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var hours by remember { mutableIntStateOf(initialHours.coerceIn(0, 99)) }
    var minutes by remember { mutableIntStateOf(initialMinutes.coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            DurationPickerContent(
                hours = hours,
                minutes = minutes,
                onHoursChange = { hours = it },
                onMinutesChange = { minutes = it },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hours, minutes) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
